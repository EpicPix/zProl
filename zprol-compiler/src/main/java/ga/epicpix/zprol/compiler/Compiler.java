package ga.epicpix.zprol.compiler;

import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zpil.bytecode.IBytecodeInstructionGenerator;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.operation.*;
import ga.epicpix.zprol.compiler.precompiled.*;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zpil.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.BooleanType;
import ga.epicpix.zprol.types.ClassType;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Type;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

public class Compiler {

    public static IBytecodeStorage parseFunctionCode(CompiledData data, SeekIterator<Token> tokens, FunctionSignature sig, String[] names) {
        IBytecodeStorage storage = createStorage();
        LocalScopeManager localsManager = new LocalScopeManager();
        for(int i = 0; i<names.length; i++) {
            localsManager.getCurrentScope().defineLocalVariable(names[i], sig.parameters()[i]);
        }
        localsManager.newScope();
        int opens = 0;
        boolean hasReturned = false;
        Token token;
        while(tokens.hasNext()) {
            token = tokens.next();
            if(token.getType() == TokenType.NAMED) {
                var named = (NamedToken) token;
                if("ReturnStatement".equals(named.name)) {
                    if(!(sig.returnType() instanceof PrimitiveType primitive) || primitive.getSize() != 0) {
                        if(named.getTokenWithName("Expression") == null) {
                            throw new TokenLocatedException("Function is not void, expected a return value", named);
                        }
                        generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(named.getTokenWithName("Expression").tokens)), sig.returnType(), new ArrayDeque<>(), data, localsManager, storage, false);
                    }
                    if(sig.returnType() instanceof PrimitiveType primitive && primitive.getSize() == 0) {
                        if(named.getTokenWithName("Expression") != null) {
                            throw new TokenLocatedException("Function is void, expected no value", named);
                        }
                    }
                    if(sig.returnType() instanceof PrimitiveType primitive) {
                        storage.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "return"));
                    }else {
                        storage.pushInstruction(getConstructedInstruction("areturn"));
                    }
                    if(opens == 0) {
                        hasReturned = true;
                        break;
                    }
                } else if("FunctionCallStatement".equals(named.name)) {
                    generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(named)), null, new ArrayDeque<>(), data, localsManager, storage, true);
                } else if("CreateAssignmentStatement".equals(named.name)) {
                    var type = data.resolveType(named.getSingleTokenWithName("Type").asWordToken().getWord());
                    var name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();
                    var expression = named.getTokenWithName("Expression").tokens;
                    var types = new ArrayDeque<Type>();
                    generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(expression)), type, types, data, localsManager, storage, false);
                    var rType = types.pop();
                    doCast(type, rType, false, storage);
                    var local = localsManager.defineLocalVariable(name, rType);
                    if(rType instanceof PrimitiveType primitive) {
                        storage.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_local", local.index()));
                    }else {
                        storage.pushInstruction(getConstructedInstruction("astore_local", local.index()));
                    }
                } else if("AssignmentStatement".equals(named.name)) {
                    var ids = new ArrayList<String>();
                    for(Token t : named.getTokenWithName("Accessor").tokens) {
                        if(t instanceof NamedToken tn && tn.name.equals("Identifier")) {
                            ids.add(tn.tokens[0].asWordToken().getWord());
                        }
                    }
                    var name = ids.get(0);
                    var expression = named.getTokenWithName("Expression").tokens;
                    var local = localsManager.getLocalVariable(name);
                    if(ids.size() == 1) {
                        var types = new ArrayDeque<Type>();
                        generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(expression)), local.type(), types, data, localsManager, storage, false);
                        if (types.pop() instanceof PrimitiveType primitive) {
                            storage.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_local", local.index()));
                        } else {
                            storage.pushInstruction(getConstructedInstruction("astore_local", local.index()));
                        }
                    }else {
                        ArrayList<IBytecodeInstruction> instructionQueue = new ArrayList<>();
                        if (local.type() instanceof PrimitiveType primitive) {
                            instructionQueue.add(getConstructedSizeInstruction(primitive.getSize(), "load_local", local.index()));
                        } else {
                            instructionQueue.add(getConstructedInstruction("aload_local", local.index()));
                        }
                        var type = local.type();
                        for(int i = 1; i<ids.size() - 1; i++) {
                            var next = getClassFieldType(type, data, ids.get(i));
                            if(type instanceof ClassType classType) {
                                instructionQueue.add(getConstructedInstruction("class_field_load", new Class(classType.getNamespace(), classType.getName(), null), ids.get(i)));
                            }else {
                                throw new TokenLocatedException("Expected class in loadClassField");
                            }
                            type = next;
                        }
                        var types = new ArrayDeque<Type>();
                        var expected = getClassFieldType(type, data, ids.get(ids.size() - 1));
                        generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(expression)), expected, types, data, localsManager, storage, false);
                        var got = types.pop();
                        doCast(got, expected, false, storage);
                        for(var instr : instructionQueue) storage.pushInstruction(instr);

                        if(type instanceof ClassType classType) {
                            storage.pushInstruction(getConstructedInstruction("class_field_store", new Class(classType.getNamespace(), classType.getName(), null), ids.get(ids.size() - 1)));
                        }else {
                            throw new TokenLocatedException("Expected a class");
                        }
                    }
                } else {
                    throw new NotImplementedException("Not implemented language feature: " + named.name + " / " + Arrays.toString(named.tokens));
                }
            }else {
                if(token.getType() == TokenType.OPEN_SCOPE) {
                    opens++;
                    localsManager.newScope();
                }else if(token.getType() == TokenType.CLOSE_SCOPE) {
                    opens--;
                    localsManager.leaveScope();
                    if(opens == 0) {
                        localsManager.leaveScope();
                        break;
                    }
                }else {
                    throw new RuntimeException("Not implemented token type: " + token);
                }
                //TODO: ++x or --x
            }
        }
        if(!hasReturned) {
            if(!(sig.returnType() instanceof PrimitiveType primitive) || primitive.getSize() != 0) throw new TokenLocatedException("Missing return statement", tokens.current());
            storage.pushInstruction(getConstructedSizeInstruction(0, "return"));
        }
        storage.setLocalsSize(localsManager.getLocalVariablesSize());
        return storage;
    }

    public static void generateInstructionsFromEquation(Operation operation, Type expectedType, ArrayDeque<Type> types, CompiledData data, LocalScopeManager localsManager, IBytecodeStorage bytecode, boolean discardValue) {
        if(operation instanceof OperationRoot root) {
            Type prob = expectedType;
            for(var op : root.getOperations()) {
                generateInstructionsFromEquation(op, prob, types, data, localsManager, bytecode, expectedType == null && discardValue);
                if(types.size() != 0) {
                    prob = types.peek();
                }
            }

            if(prob != null && discardValue) {
                if(prob instanceof PrimitiveType primitive) {
                    if(primitive.getSize() != 0) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "pop"));
                    }else {
                        types.push(primitive);
                    }
                }else {
                    bytecode.pushInstruction(getConstructedInstruction("apop"));
                }
            }else if(prob != null) {
                types.push(prob);
            }
        }else if(operation instanceof OperationNumber number) {
            if(expectedType instanceof PrimitiveType primitive) {
                bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "push", number.number));
                types.push(primitive);
            }else if(expectedType == null) {
                bytecode.pushInstruction(getConstructedSizeInstruction(4, "push", number.number));
                Type int32 = data.resolveType("int32");
                types.push(int32);
            }else {
                throw new TokenLocatedException("Cannot infer size of number from a non-primitive type (" + expectedType.getName() + ")");
            }
        }else if(operation instanceof OperationOperator operator) {
            String op = operator.operator.operator();
            var arg1 = types.pop();
            var arg2 = types.pop();
            if(!(arg1 instanceof PrimitiveType prim1) || !(arg2 instanceof PrimitiveType prim2)) {
                throw new TokenLocatedException("Cannot perform math operation on types " + arg1.getName() + " <-> " + arg2.getName());
            }
            var bigger = prim1.getSize() > prim2.getSize() ? prim1 : prim2;
            var smaller = prim1.getSize() <= prim2.getSize() ? prim1 : prim2;
            var got = doCast(smaller, bigger, false, bytecode);
            PrimitiveType primitive = (PrimitiveType) got;
            switch (op) {
                case "+":
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "add"));
                    break;
                case "-":
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "sub"));
                    break;
                case "*":
                    if (primitive.isUnsigned()) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "mulu"));
                    } else {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "mul"));
                    }
                    break;
                case "/":
                    if (primitive.isUnsigned()) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "divu"));
                    } else {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "div"));
                    }
                    break;
                case "|":
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "or"));
                    break;
                case "==":
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "cmp"));
                    types.push(new BooleanType());
                    return;
                default:
                    throw new NotImplementedException("Unknown operator " + op);
            }
            types.push(expectedType);
        }else if(operation instanceof OperationCall call) {
            ArrayList<PreFunction> possibleFunctions = new ArrayList<>();
            for(var using : data.getUsing()) {
                if(data.namespace != null && !data.namespace.equals(using.namespace)) continue; // currently, method name can only be a string without dots
                for(var func : using.functions) {
                    if(func.name.equals(call.getFunctionName())) {
                        if(func.parameters.size() == call.getOperations().size()) {
                            possibleFunctions.add(func);
                        }
                    }
                }
            }

            if(possibleFunctions.size() == 0) {
                throw new FunctionNotDefinedException("Unknown function: " + call.getFunctionName());
            }else if(possibleFunctions.size() != 1) {
                throw new NotImplementedException("Cannot match overloaded parameter types");
            }

            var func = possibleFunctions.get(0);
            var returnType = data.resolveType(func.returnType);
            var parameters = new Type[func.parameters.size()];
            for(int i = 0; i<func.parameters.size(); i++) {
                parameters[i] = data.resolveType(func.parameters.get(i).type);
            }
            FunctionSignature signature = new FunctionSignature(returnType, parameters);
            EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
            for(PreFunctionModifiers modifier : func.modifiers) {
                modifiers.add(modifier.getCompiledModifier());
            }

            for(int i = 0; i<call.getOperations().size(); i++) {
                generateInstructionsFromEquation(call.getOperations().get(i), parameters[i], types, data, localsManager, bytecode, false);
            }

            bytecode.pushInstruction(getConstructedInstruction("invoke", new Function(data.namespace, modifiers, func.name, signature, null)));

            if(discardValue && returnType instanceof PrimitiveType primitive) {
                if(primitive.getSize() != 0) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "pop"));
                }
            }else if(discardValue) {
                bytecode.pushInstruction(getConstructedInstruction("apop"));
            }else {
                if(expectedType != null) {
                    if(!returnType.equals(expectedType)) {
                        var got = doCast(returnType, expectedType, false, bytecode);
                        types.push(got);
                    }else {
                        types.push(returnType);
                    }
                }else {
                    types.push(returnType);
                }
            }
        }else if(operation instanceof OperationAccessor field) {
            var ids = field.getIdentifiers();
            var local = localsManager.tryGetLocalVariable(ids[0]);
            if(local != null) {
                if(local.type() instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_local", local.index()));
                }else {
                    bytecode.pushInstruction(getConstructedInstruction("aload_local", local.index()));
                }

                var type = local.type();
                for(int i = 1; i<ids.length; i++) {
                    type = loadClassField(type, data, ids[i], bytecode);
                }
                types.push(type);
            }else {
                throw new NotImplementedException("Not implemented looking in different scopes");
            }
        }else if(operation instanceof OperationString str) {
            bytecode.pushInstruction(getConstructedInstruction("push_string", str.getString().replace("\\\"", "\"").replace("\\n", "\n").replace("\\0", "\0")));
            types.push(data.resolveType("zprol.lang.String"));
        }else if(operation instanceof OperationAssignment assignment) {
            var local = localsManager.tryGetLocalVariable(assignment.getIdentifier());
            if(local != null) {
                generateInstructionsFromEquation(assignment.getOperation(), local.type(), types, data, localsManager, bytecode, false);
                if(local.type() instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "dup"));
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_local", local.index()));
                }else {
                    bytecode.pushInstruction(getConstructedInstruction("adup"));
                    bytecode.pushInstruction(getConstructedInstruction("astore_local", local.index()));
                }
                types.push(local.type());
            }else {
                throw new NotImplementedException("Not implemented looking in different scopes");
            }
        }else if(operation instanceof OperationCast cast) {
            var castType = data.resolveType(cast.getType());
            generateInstructionsFromEquation(cast.getOperation(), null, types, data, localsManager, bytecode, false);
            var from = types.pop();
            if(cast.isHardCast()) {
                // this will not check sizes of primitive types, this is unsafe
                types.push(castType);
                return;
            }
            types.push(doCast(from, castType, true, bytecode));
        }else if(operation instanceof OperationBoolean bool) {
            bytecode.pushInstruction(getConstructedSizeInstruction(1, "push", bool.getValue() ? 1 : 0));
            types.push(new BooleanType());
        }else {
            throw new NotImplementedException("Unknown operation " + operation.getClass());
        }
    }

    public static Type doCast(Type from, Type to, boolean explicit, IBytecodeStorage bytecode) {
        if(!(from instanceof PrimitiveType primitiveFrom) || !(to instanceof PrimitiveType primitiveTo)) {
            if(!from.equals(to)) {
                throw new TokenLocatedException("Unsupported cast from " + from.getName() + " to " + to.getName());
            }else {
                return to;
            }
        }
        if(primitiveTo.getSize() > primitiveFrom.getSize()) {
            bytecode.pushInstruction(getConstructedSizeInstruction(primitiveFrom.getSize(), "cast" + getInstructionPrefix(primitiveTo.getSize())));
            return to;
        }
        if(primitiveTo.getSize() == primitiveFrom.getSize()) {
            return to;
        }
        if(explicit) {
            if(primitiveTo.getSize() < primitiveFrom.getSize()) {
                bytecode.pushInstruction(getConstructedSizeInstruction(primitiveFrom.getSize(), "cast" + getInstructionPrefix(primitiveTo.getSize())));
                return to;
            }
        }
        throw new TokenLocatedException("Unsupported cast from " + from.getName() + " to " + to.getName());
    }

    public static Type getClassFieldType(Type type, CompiledData data, String field) {
        if(type instanceof PrimitiveType) {
            throw new TokenLocatedException("Cannot access fields of a primitive type");
        }else if(type instanceof ClassType classType) {
            PreClass clz = null;
            for(var p : data.getUsing()) {
                if(classType.getNamespace() != null && !classType.getNamespace().equals(p.namespace)) continue;
                for(var c : p.classes) {
                    if(classType.getName().equals(c.name)) {
                        clz = c;
                        break;
                    }
                }
                if(clz != null) break;
            }
            if(clz == null) {
                throw new TokenLocatedException("Unknown type '" + classType + "'");
            }

            PreField found = null;
            for(var f : clz.fields) {
                if(f.name.equals(field)) {
                    found = f;
                    break;
                }
            }
            if(found == null) {
                throw new TokenLocatedException("Unable to find field '" + field + "' in '" + clz.name + "'");
            }
            return data.resolveType(found.type);
        }else {
            throw new TokenLocatedException("Unknown type " + type.getClass().getSimpleName());
        }
    }

    public static Type loadClassField(Type type, CompiledData data, String field, IBytecodeStorage bytecode) {
        var next = getClassFieldType(type, data, field);
        if(type instanceof ClassType classType) {
            bytecode.pushInstruction(getConstructedInstruction("class_field_load", new Class(classType.getNamespace(), classType.getName(), null), field));
        }else {
            throw new TokenLocatedException("Expected class in loadClassField");
        }
        return next;
    }

    public static void compileFunction(CompiledData data, PreFunction function) {
        var returnType = data.resolveType(function.returnType);
        var parameters = new Type[function.parameters.size()];
        String[] names = new String[function.parameters.size()];
        for(int i = 0; i<function.parameters.size(); i++) {
            PreParameter param = function.parameters.get(i);
            parameters[i] = data.resolveType(param.type);
            names[i] = param.name;
        }
        FunctionSignature signature = new FunctionSignature(returnType, parameters);
        IBytecodeStorage bytecode = null;
        if(function.hasCode()) {
            bytecode = parseFunctionCode(data, new SeekIterator<>(function.code), signature, names);
        }
        EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
        for(PreFunctionModifiers modifier : function.modifiers) {
            modifiers.add(modifier.getCompiledModifier());
        }
        data.addFunction(new Function(data.namespace, modifiers, function.name, signature, bytecode));
    }

    public static void compileClass(CompiledData data, PreClass clazz) {
        ClassField[] fields = new ClassField[clazz.fields.size()];
        for (int i = 0; i < clazz.fields.size(); i++) {
            PreField field = clazz.fields.get(i);
            fields[i] = new ClassField(field.name, data.resolveType(field.type));
        }
        data.addClass(new Class(data.namespace, clazz.name, fields));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other) throws UnknownTypeException {
        CompiledData data = new CompiledData(preCompiled.namespace);

        data.using(preCompiled);
        for(PreCompiledData o : other) if(preCompiled.using.contains(o.namespace)) data.using(o);

        for(PreClass clazz : preCompiled.classes) compileClass(data, clazz);
        for(PreFunction function : preCompiled.functions) compileFunction(data, function);
        return data;
    }

    public static String getInstructionPrefix(int size) {
        return Bytecode.BYTECODE.getInstructionPrefix(size);
    }

    public static IBytecodeInstructionGenerator getInstruction(String name) {
        return Bytecode.BYTECODE.getInstruction(name);
    }

    public static IBytecodeInstruction getConstructedInstruction(int id, Object... args) {
        return Bytecode.BYTECODE.getConstructedInstruction(id, args);
    }

    public static IBytecodeInstruction getConstructedInstruction(String name, Object... args) {
        return Bytecode.BYTECODE.getConstructedInstruction(name, args);
    }

    public static IBytecodeInstruction getConstructedSizeInstruction(int size, String name, Object... args) {
        return getConstructedInstruction(getInstructionPrefix(size) + name, args);
    }

    public static IBytecodeStorage createStorage() {
        return Bytecode.BYTECODE.createStorage();
    }

}