package ga.epicpix.zprol.compiler;

import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zpil.bytecode.IBytecodeInstructionGenerator;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.precompiled.*;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.*;
import ga.epicpix.zprol.utils.SeekIterator;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class Compiler {
    public static IBytecodeStorage parseFunctionCode(CompiledData data, SeekIterator<Token> tokens, FunctionSignature sig, String[] names) {
        IBytecodeStorage storage = createStorage();
        LocalScopeManager localsManager = new LocalScopeManager();
        for(int i = 0; i<names.length; i++) {
            localsManager.defineLocalVariable(names[i], sig.parameters()[i]);
        }
        localsManager.newScope();
        boolean hasReturned = parseFunctionCode(data, tokens, sig, names, storage, localsManager);
        if(!hasReturned) {
            if(!(sig.returnType() instanceof VoidType)) {
                throw new TokenLocatedException("Missing return statement in " + sig);
            }
            storage.pushInstruction(getConstructedSizeInstruction(0, "return"));
        }
        localsManager.leaveScope();
        storage.setLocalsSize(localsManager.getLocalVariablesSize());
        return storage;
    }

    public static boolean parseFunctionCode(CompiledData data, SeekIterator<Token> tokens, FunctionSignature sig, String[] names, IBytecodeStorage storage, LocalScopeManager localsManager) {
        localsManager.newScope();
        int opens = 0;
        boolean hasReturned = false;
        Token token;
        while(tokens.hasNext()) {
            token = tokens.next();
            if(token.getType() == TokenType.NAMED) {
                var named = (NamedToken) token;
                if("ReturnStatement".equals(named.name)) {
                    if(!(sig.returnType() instanceof VoidType)) {
                        if(named.getTokenWithName("Expression") == null) {
                            throw new TokenLocatedException("Function is not void, expected a return value", named);
                        }
                        var types = new ArrayDeque<Type>();
                        generateInstructionsFromExpression(named.getTokenWithName("Expression"), sig.returnType(), types, data, localsManager, storage, false);
                    }
                    if(sig.returnType() instanceof VoidType) {
                        if(named.getTokenWithName("Expression") != null) {
                            throw new TokenLocatedException("Function is void, expected no value", named);
                        }
                    }
                    if(sig.returnType() instanceof PrimitiveType primitive) {
                        storage.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "return"));
                    }else if(sig.returnType() instanceof BooleanType) {
                        storage.pushInstruction(getConstructedSizeInstruction(8, "return"));
                    }else if(!(sig.returnType() instanceof VoidType)) {
                        storage.pushInstruction(getConstructedInstruction("areturn"));
                    }
                    hasReturned = true;
                    break;
                } else if("FunctionCallStatement".equals(named.name)) {
                    generateInstructionsFromExpression(named, null, new ArrayDeque<>(), data, localsManager, storage, true);
                } else if("CreateAssignmentStatement".equals(named.name)) {
                    var type = data.resolveType(named.getSingleTokenWithName("Type").asWordToken().getWord());
                    if(type instanceof VoidType) {
                        throw new TokenLocatedException("Cannot create a variable with void type", named);
                    }
                    var name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();
                    var expression = named.getTokenWithName("Expression");
                    var types = new ArrayDeque<Type>();
                    generateInstructionsFromExpression(expression, type, types, data, localsManager, storage, false);
                    var rType = types.pop();
                    doCast(rType, type, false, storage, expression);
                    var local = localsManager.defineLocalVariable(name, rType);
                    if(rType instanceof PrimitiveType primitive) {
                        storage.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_local", local.index()));
                    }else {
                        storage.pushInstruction(getConstructedInstruction("astore_local", local.index()));
                    }
                } else if("AssignmentStatement".equals(named.name)) {
                    var accessors = new ArrayList<NamedToken>();
                    for(Token t : named.getTokenWithName("Accessor").tokens) {
                        if(t instanceof NamedToken tn && tn.name.equals("AccessorElement")) {
                            accessors.add(tn);
                        }
                    }
                    var name = named.getTokenWithName("Accessor").getSingleTokenWithName("Identifier").asWordToken().getWord();
                    var expression = named.getTokenWithName("Expression");
                    var local = localsManager.tryGetLocalVariable(name);
                    Field f = null;
                    Type expected;
                    if(local != null) {
                        expected = local.type();
                    }else {
                        for(var using : data.getUsing()) {
                            for(var field : using.fields) {
                                if(field.name.equals(name)) {
                                    f = new Field(using.namespace, field.name, data.resolveType(field.type));
                                    break;
                                }
                            }
                        }
                        if(f == null) {
                            throw new TokenLocatedException("Unknown variable", named.getTokenWithName("Accessor").getSingleTokenWithName("Identifier"));
                        }else {
                            expected = f.type();
                        }
                    }
                    var types = new ArrayDeque<Type>();
                    var queue = createStorage();
                    if(accessors.size() != 0) {
                        var accessorTypes = new ArrayDeque<Type>();
                        if(local != null) {
                            var useType = local.type();
                            if (useType instanceof PrimitiveType primitive) {
                                queue.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_local", local.index()));
                            } else if (useType instanceof BooleanType) {
                                queue.pushInstruction(getConstructedSizeInstruction(8, "load_local", local.index()));
                            } else if (useType instanceof VoidType) {
                                throw new TokenLocatedException("Cannot store void type");
                            } else {
                                queue.pushInstruction(getConstructedInstruction("aload_local", local.index()));
                            }
                            accessorTypes.push(useType);
                        }else {
                            var useType = f.type();
                            if (useType instanceof PrimitiveType primitive) {
                                queue.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_field", f));
                            } else if (useType instanceof BooleanType) {
                                queue.pushInstruction(getConstructedSizeInstruction(8, "load_field", f));
                            } else if (useType instanceof VoidType) {
                                throw new TokenLocatedException("Cannot load void type");
                            } else {
                                queue.pushInstruction(getConstructedInstruction("aload_field", f));
                            }
                            accessorTypes.push(useType);
                        }
                        for (int i = 0; i < accessors.size() - 1; i++) {
                            getAccessor(accessors.get(i), accessorTypes, data, queue, localsManager);
                        }
                        expected = setAccessor(accessors.get(accessors.size() - 1), accessorTypes, data, queue, localsManager);
                    }
                    generateInstructionsFromExpression(expression, expected, types, data, localsManager, storage, false);
                    var assignmentType = types.pop();
                    if(accessors.size() == 0) {
                        var castType = doCast(assignmentType, expected, false, queue, expression);
                        if(local != null) {
                            if (castType instanceof PrimitiveType primitive) {
                                queue.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_local", local.index()));
                            } else if (castType instanceof BooleanType) {
                                queue.pushInstruction(getConstructedSizeInstruction(8, "store_local", local.index()));
                            } else if (castType instanceof VoidType) {
                                throw new TokenLocatedException("Cannot store void type");
                            } else {
                                queue.pushInstruction(getConstructedInstruction("astore_local", local.index()));
                            }
                        }else {
                            if (castType instanceof PrimitiveType primitive) {
                                queue.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_field", f));
                            } else if (castType instanceof BooleanType) {
                                queue.pushInstruction(getConstructedSizeInstruction(8, "store_field", f));
                            } else if (castType instanceof VoidType) {
                                throw new TokenLocatedException("Cannot store void type");
                            } else {
                                queue.pushInstruction(getConstructedInstruction("astore_field", f));
                            }
                        }
                    }
                    doCast(assignmentType, expected, false, storage, expression);
                    for(var instr : queue.getInstructions()) storage.pushInstruction(instr);
                } else if("IfStatement".equals(named.name)) {
                    var types = new ArrayDeque<Type>();
                    generateInstructionsFromExpression(named.getTokenWithName("Expression"), null, types, data, localsManager, storage, false);
                    var ret = types.pop();
                    if(!(ret instanceof BooleanType)) {
                        throw new TokenLocatedException("Expected boolean expression", named.getTokenWithName("Expression"));
                    }
                    var statements = new ArrayList<Token>();
                    for(var statement : named.getTokenWithName("Code").getTokensWithName("Statement")) {
                        statements.add(statement.tokens[0]);
                    }
                    int preInstr = storage.getInstructions().size();
                    parseFunctionCode(data, new SeekIterator<>(statements), sig, names, storage, localsManager);
                    int postInstr = storage.getInstructions().size();
                    if(named.getTokenWithName("ElseStatement") != null) {
                        var elseStatements = new ArrayList<Token>();
                        for(var statement : named.getTokenWithName("ElseStatement").getTokenWithName("Code").getTokensWithName("Statement")) {
                            elseStatements.add(statement.tokens[0]);
                        }
                        parseFunctionCode(data, new SeekIterator<>(elseStatements), sig, names, storage, localsManager);
                        storage.pushInstruction(postInstr, getConstructedInstruction("jmp", storage.getInstructions().size()-postInstr+1));
                        postInstr++;
                    }
                    storage.pushInstruction(preInstr, getConstructedInstruction("neqjmp", postInstr-preInstr+1));
                } else if("WhileStatement".equals(named.name)) {
                    var types = new ArrayDeque<Type>();
                    int preInstr = storage.getInstructions().size();
                    generateInstructionsFromExpression(named.getTokenWithName("Expression"), null, types, data, localsManager, storage, false);
                    var ret = types.pop();
                    if(!(ret instanceof BooleanType)) {
                        throw new TokenLocatedException("Expected boolean expression", named.getTokenWithName("Expression"));
                    }
                    int addInstr = storage.getInstructions().size();
                    var statements = new ArrayList<Token>();
                    for(var statement : named.getTokenWithName("Code").getTokensWithName("Statement")) {
                        statements.add(statement.tokens[0]);
                    }
                    parseFunctionCode(data, new SeekIterator<>(statements), sig, names, storage, localsManager);
                    int postInstr = storage.getInstructions().size() + 1;

                    storage.pushInstruction(getConstructedInstruction("jmp", preInstr-postInstr));
                    storage.pushInstruction(addInstr, getConstructedInstruction("neqjmp", postInstr-addInstr+1));
                } else {
                    throw new TokenLocatedException("Not implemented language feature: " + named.name + " / " + Arrays.toString(named.tokens), named);
                }
            }
        }
        localsManager.leaveScope();
        return hasReturned;
    }

    public static void generateInstructionsFromExpression(NamedToken token, Type expectedType, ArrayDeque<Type> types, CompiledData data, LocalScopeManager localsManager, IBytecodeStorage bytecode, boolean discardValue) {
        if(token.name.equals("Expression")) {
            generateInstructionsFromExpression(token.tokens[0].asNamedToken(), expectedType, types, data, localsManager, bytecode, expectedType == null && discardValue);
            Type prob = types.size() != 0 ? types.peek() : null;

            if(prob != null && discardValue) {
                if(prob instanceof PrimitiveType primitive) {
                    if(primitive.getSize() != 0) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "pop"));
                    }else {
                        types.push(primitive);
                    }
                }else if(prob instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "pop"));
                }else {
                    bytecode.pushInstruction(getConstructedInstruction("apop"));
                }
            }else if(prob != null) {
                types.push(prob);
            }
        }else if(token.name.equals("MultiplicativeExpression") || token.name.equals("AdditiveExpression") || token.name.equals("InclusiveOrExpression") || token.name.equals("EqualsExpression") || token.name.equals("ShiftExpression") || token.name.equals("InclusiveAndExpression") || token.name.equals("CompareExpression")) {
            types.push(runOperator(token, expectedType, data, localsManager, types, bytecode));
        }else if(token.name.equals("DecimalInteger")) {
           BigInteger number = getDecimalInteger(token.tokens[0]);
           if(expectedType instanceof PrimitiveType primitive) {
               bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "push", number));
               types.push(primitive);
           }else if(expectedType == null) {
               bytecode.pushInstruction(getConstructedSizeInstruction(4, "push", number));
               types.push(data.resolveType("int32"));
           }else {
               throw new TokenLocatedException("Cannot infer size of number from a non-primitive type (" + expectedType.getName() + ")", token);
           }
        }else if(token.name.equals("FunctionCall") || token.name.equals("FunctionCallStatement")) {
            var name = token.getSingleTokenWithName("Identifier").asWordToken().getWord();
            var argumentList = token.getTokenWithName("ArgumentList");
            var arguments = argumentList != null ? argumentList.getTokensWithName("Argument") : new ArrayList<NamedToken>();

            var possibleFunctions = new ArrayList<PreFunction>();
            for(var using : data.getUsing()) {
                for(var func : using.functions) {
                    if(func.name.equals(name)) {
                        if(func.parameters.size() == arguments.size()) {
                            possibleFunctions.add(func);
                        }
                    }
                }
            }

            if(possibleFunctions.size() == 0) {
                throw new TokenLocatedException("Function not defined: " + name, token);
            }else if(possibleFunctions.size() != 1) {

                var garbage = createStorage();
                var argTypes = new ArrayList<Type>();

                for(var arg : arguments) {
                    generateInstructionsFromExpression(arg.tokens[0].asNamedToken(), null, types, data, localsManager, garbage, false);
                    argTypes.add(types.pop());
                }

                ArrayList<String> candidates = new ArrayList<>();
                int closestScore = 0;
                ArrayList<PreFunction> closest = new ArrayList<>();
                for(var a : possibleFunctions) {
                    boolean matches = true;
                    boolean nPrimMatches = true;
                    int score = 0;
                    for(int i = 0; i<a.parameters.size(); i++) {
                        var t = data.resolveType(a.parameters.get(i).type);
                        if(!t.equals(argTypes.get(i))) {
                            if(argTypes.get(i) instanceof PrimitiveType && t instanceof PrimitiveType) {
                                score++;
                            }else if(!(argTypes.get(i) instanceof PrimitiveType) || !(t instanceof PrimitiveType)) {
                                nPrimMatches = false;
                            }
                            matches = false;
                        }else {
                            score++;
                        }
                    }

                    if(closestScore < score) {
                        closest.clear();
                        closestScore = score;
                    }
                    if(closestScore == score && nPrimMatches) closest.add(a);

                    if(matches) {
                        closest.clear();
                        closest.add(a);
                        break;
                    }
                    candidates.add("(" + a.parameters.stream().map(b -> data.resolveType(b.type).getName()).collect(Collectors.joining(", ")) + ")");
                }

                if(closest.size() == 1) {
                    possibleFunctions.clear();
                    possibleFunctions.add(closest.get(0));
                }else {
                    if(closest.size() != 0) {
                        throw new TokenLocatedException(
                            "Ambiguous function call for arguments (" +
                                argTypes.stream()
                                    .map(Type::getName)
                                    .collect(Collectors.joining(", "))
                                + ")\nCandidates are:\n" +
                                closest.stream()
                                    .map(a -> "(" + a.parameters.stream().map(b -> data.resolveType(b.type).getName()).collect(Collectors.joining(", ")) + ")")
                                    .collect(Collectors.joining("\n")
                                ), token);
                    }
                    throw new TokenLocatedException("Cannot find overload for arguments (" + argTypes.stream().map(Type::getName).collect(Collectors.joining(", ")) + ")\nCandidates are:\n" + String.join("\n", candidates), token);
                }
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

            for(int i = 0; i<arguments.size(); i++) {
                generateInstructionsFromExpression(arguments.get(i).getTokenWithName("Expression"), parameters[i], types, data, localsManager, bytecode, false);
                doCast(types.pop(), signature.parameters()[i], false, bytecode, arguments.get(i).getTokenWithName("Expression"));
            }

            bytecode.pushInstruction(getConstructedInstruction("invoke", new Function(data.namespace, modifiers, func.name, signature, null)));

            if(!discardValue && returnType instanceof VoidType) {
                throw new TokenLocatedException("Cannot store a void type", token);
            }

            if(discardValue && returnType instanceof PrimitiveType primitive) {
                bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "pop"));
            }else if(discardValue && returnType instanceof BooleanType) {
                bytecode.pushInstruction(getConstructedSizeInstruction(8, "pop"));
            }else if(discardValue) {
                if(!(returnType instanceof VoidType)) {
                    bytecode.pushInstruction(getConstructedInstruction("apop"));
                }
            }else {
                if (expectedType != null) {
                    types.push(doCast(returnType, expectedType, false, bytecode, token));
                } else {
                    types.push(returnType);
                }
            }
        } else if(token.name.equals("Accessor")) {
            var accessors = new ArrayList<NamedToken>();
            for(Token t : token.tokens) {
                if(t instanceof NamedToken tn && tn.name.equals("AccessorElement")) {
                    accessors.add(tn);
                }
            }
            String firstAccess = token.getSingleTokenWithName("Identifier").asWordToken().getWord();
            var local = localsManager.tryGetLocalVariable(firstAccess);
            if(local == null) {
                Field useField = null;
                for(var using : data.getUsing()) {
                    for(var field : using.fields) {
                        if(field.name.equals(firstAccess)) {
                            useField = new Field(using.namespace, field.name, data.resolveType(field.type));
                            break;
                        }
                    }
                }
                if(useField != null) {
                    if (useField.type() instanceof PrimitiveType primitive)
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_field", useField));
                    else if (useField.type() instanceof BooleanType)
                        bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_field", useField));
                    else
                        bytecode.pushInstruction(getConstructedInstruction("aload_field", useField));

                    types.push(useField.type());
                }else {
                    throw new TokenLocatedException("Unknown variable '" + firstAccess + "'", token.getSingleTokenWithName("Identifier"));
                }
            }else {
                if (local.type() instanceof PrimitiveType primitive)
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_local", local.index()));
                else if (local.type() instanceof BooleanType)
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_local", local.index()));
                else
                    bytecode.pushInstruction(getConstructedInstruction("aload_local", local.index()));

                types.push(local.type());

            }
            for (var accessor : accessors) {
                getAccessor(accessor, types, data, bytecode, localsManager);
            }
        }else if(token.name.equals("ArrayAccessor")) {
            var arrayType = (ArrayType) types.pop();
            if(!arrayType.type.equals(expectedType)) {
                throw new TokenLocatedException("Expected type does not match the array type", token);
            }
            var expr = token.getTokenWithName("Expression");
            generateInstructionsFromExpression(expr, null, types, data, localsManager, bytecode, false);
            var expressionType = types.pop();
            if(!(expressionType instanceof PrimitiveType)) {
                throw new TokenLocatedException("Expected a primitive number", expr);
            }
            var arrayData = arrayType.type;
            if(arrayData instanceof PrimitiveType primitive) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_array"));
            else if(arrayData instanceof BooleanType) bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_array"));
            else if(arrayData instanceof ArrayType) bytecode.pushInstruction(getConstructedInstruction("aload_array"));
            else if(arrayData instanceof ClassType) bytecode.pushInstruction(getConstructedInstruction("aload_array"));
            else throw new TokenLocatedException("Unsupported type " + arrayData.getClass().getSimpleName(), token);
            types.push(arrayType.type);
        }else if(token.name.equals("String")) {
            var strChars = token.getSingleTokenWithName("StringChars");
            bytecode.pushInstruction(getConstructedInstruction("push_string", strChars != null ? strChars.asWordToken().getWord().replace("\\\"", "\"").replace("\\n", "\n").replace("\\0", "\0") : ""));
            types.push(data.resolveType("zprol.lang.String"));
        }else if(token.name.equals("CastExpression")) {
            var hardCast = token.getTokenWithName("HardCastOperator");
            var castType = data.resolveType((hardCast != null ? hardCast : token.getTokenWithName("CastOperator")).getSingleTokenWithName("Type").asWordToken().getWord());
            if(token.tokens[1].getType() == TokenType.OPEN) {
                generateInstructionsFromExpression(token.tokens[2].asNamedToken(), null, types, data, localsManager, bytecode, false);
            }else {
                generateInstructionsFromExpression(token.tokens[1].asNamedToken(), null, types, data, localsManager, bytecode, false);
            }
            var from = types.pop();
            if(hardCast != null) {
                // this will not check sizes of primitive types, this is unsafe
                types.push(castType);
                return;
            }
            types.push(doCast(from, castType, true, bytecode, token.tokens[1]));
        }else if(token.name.equals("Boolean")) {
            bytecode.pushInstruction(getConstructedSizeInstruction(8, "push", Boolean.parseBoolean(token.tokens[0].asWordToken().getWord()) ? 1 : 0));
            types.push(new BooleanType());
        }else if(token.name.equals("Null")) {
            bytecode.pushInstruction(getConstructedSizeInstruction(8, "push", 0));
            types.push(new NullType());
        }else {
            throw new TokenLocatedException("Unknown token " + token.name, token);
        }
    }

    public static Type runOperator(NamedToken token, Type expectedType, CompiledData data, LocalScopeManager localsManager, ArrayDeque<Type> types, IBytecodeStorage bytecode) {
        if (token.tokens[0].getType() == TokenType.OPEN) {
            generateInstructionsFromExpression(token.tokens[1].asNamedToken(), expectedType, types, data, localsManager, bytecode, false);
            return types.pop();
        }
        generateInstructionsFromExpression(token.tokens[0].asNamedToken(), expectedType, types, data, localsManager, bytecode, false);
        var arg1 = types.pop();

        int amtOperators = (token.tokens.length - 1) / 2;
        for (int i = 0; i < amtOperators; i++) {
            var arg2Storage = createStorage();
            generateInstructionsFromExpression(token.tokens[i * 2 + 2].asNamedToken(), expectedType, types, data, localsManager, arg2Storage, false);
            var arg2 = types.pop();

            arg1 = runOperator(arg1, arg2, token.tokens[i * 2 + 1].asNamedToken(), token, bytecode, arg2Storage);
        }

        return arg1;
    }

    public static Type runOperator(Type arg1, Type arg2, NamedToken operator, Token location, IBytecodeStorage bytecode, IBytecodeStorage arg2bytecode) {
        var operatorName = operator.tokens[0].asWordToken().getWord();

        if(!(arg1 instanceof PrimitiveType prim1) || !(arg2 instanceof PrimitiveType prim2)) {
            if((arg1 instanceof ClassType || arg1 instanceof NullType) && (arg2 instanceof ClassType || arg2 instanceof NullType)) {
                if(operatorName.equals("==")) {
                    for(var instr : arg2bytecode.getInstructions()) {
                        bytecode.pushInstruction(instr);
                    }
                    bytecode.pushInstruction(getConstructedInstruction("aeq"));
                    return new BooleanType();
                }else if(operatorName.equals("!=")) {
                    for(var instr : arg2bytecode.getInstructions()) {
                        bytecode.pushInstruction(instr);
                    }
                    bytecode.pushInstruction(getConstructedInstruction("aneq"));
                    return new BooleanType();
                }
            }
            throw new TokenLocatedException("Cannot perform math operation on types " + arg1.getName() + " <-> " + arg2.getName(), location);
        }
        var bigger = prim1.getSize() > prim2.getSize() ? prim1 : prim2;
        var smaller = prim1.getSize() <= prim2.getSize() ? prim1 : prim2;
        if(prim2 != bigger) {
            for(var instr : arg2bytecode.getInstructions()) {
                bytecode.pushInstruction(instr);
            }
        }
        var got = doCast(smaller, bigger, false, bytecode, location);
        if(prim2 == bigger) {
            for(var instr : arg2bytecode.getInstructions()) {
                bytecode.pushInstruction(instr);
            }
        }
        var primitive = (PrimitiveType) got;

        switch (operatorName) {
            case "+" -> bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "add"));
            case "-" -> bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "sub"));
            case "*" -> {
                if (primitive.isUnsigned()) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "mulu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "mul"));
            }
            case "/" -> {
                if (primitive.isUnsigned()) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "divu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "div"));
            }
            case "%" -> {
                if (primitive.isUnsigned()) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "modu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "mod"));
            }
            case "&" -> bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "and"));
            case "<<" -> bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "shift_left"));
            case ">>" -> bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "shift_right"));
            case "|" -> bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "or"));
            case "==" -> {
                bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "eq"));
                got = new BooleanType();
            }
            case "!=" -> {
                bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "neq"));
                got = new BooleanType();
            }
            case "<" -> {
                if(primitive.isUnsigned()) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "ltu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "lt"));
                got = new BooleanType();
            }
            case "<=" -> {
                if(primitive.isUnsigned()) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "leu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "le"));
                got = new BooleanType();
            }
            case ">" -> {
                if(primitive.isUnsigned()) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "gtu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "gt"));
                got = new BooleanType();
            }
            case ">=" -> {
                if(primitive.isUnsigned()) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "geu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "ge"));
                got = new BooleanType();
            }
            default -> throw new TokenLocatedException("Unknown operator '" + operatorName + "'", operator);
        }
        return got;
    }

    public static Type doCast(Type from, Type to, boolean explicit, IBytecodeStorage bytecode, Token location) {
        if(from instanceof NullType && to instanceof ClassType) {
            return to;
        }
        if(!(from instanceof PrimitiveType primitiveFrom) || !(to instanceof PrimitiveType primitiveTo)) {
            if(!from.equals(to)) {
                throw new TokenLocatedException("Unsupported cast from " + from.getName() + " to " + to.getName(), location);
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
        throw new TokenLocatedException("Unsupported implicit cast from " + from.getName() + " to " + to.getName(), location);
    }

    public static void getAccessor(NamedToken accessor, ArrayDeque<Type> types, CompiledData data, IBytecodeStorage storage, LocalScopeManager localsManager) {
        var type = types.pop();
        if(accessor.getTokenWithName("Identifier") != null) {
            String field = accessor.getSingleTokenWithName("Identifier").asWordToken().getWord();

            if (type instanceof PrimitiveType) {
                throw new TokenLocatedException("Cannot access fields of a primitive type", accessor);
            } else if (type instanceof BooleanType) {
                throw new TokenLocatedException("Cannot access fields of a boolean type", accessor);
            } else if (type instanceof ArrayType) {
                throw new TokenLocatedException("Cannot access fields of an array", accessor);
            } else if (type instanceof VoidType) {
                throw new TokenLocatedException("Cannot access fields of a void type", accessor);
            } else if (type instanceof ClassType classType) {
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
                    throw new TokenLocatedException("Unknown type '" + classType + "'", accessor);
                }

                PreField found = null;
                for(var f : clz.fields) {
                    if(f.name.equals(field)) {
                        found = f;
                        break;
                    }
                }
                if(found == null) {
                    throw new TokenLocatedException("Unable to find field '" + field + "' in '" + clz.name + "'", accessor);
                }
                storage.pushInstruction(getConstructedInstruction("class_field_load", new Class(classType.getNamespace(), classType.getName(), null, null), field));
                types.push(data.resolveType(found.type));
            } else {
                throw new TokenLocatedException("Unknown type " + type.getClass().getSimpleName(), accessor);
            }
        }else if(accessor.getTokenWithName("ArrayAccessor") != null) {
            if(!(type instanceof ArrayType)) {
                throw new TokenLocatedException("Expected an array got '" + type.getName() + "'", accessor);
            }
            types.push(type);
            generateInstructionsFromExpression(accessor.getTokenWithName("ArrayAccessor"), ((ArrayType) type).type, types, data, localsManager, storage, false);
            var got = types.peek();
            if(!((ArrayType) type).type.equals(got)) {
                throw new TokenLocatedException("Could not get the value", accessor);
            }
        }else {
            throw new TokenLocatedException("Unknown accessor", accessor);
        }
    }

    public static Type setAccessor(NamedToken accessor, ArrayDeque<Type> types, CompiledData data, IBytecodeStorage storage, LocalScopeManager localsManager) {
        var type = types.pop();
        if(accessor.getTokenWithName("Identifier") != null) {
            String field = accessor.getSingleTokenWithName("Identifier").asWordToken().getWord();

            if (type instanceof PrimitiveType) {
                throw new TokenLocatedException("Cannot access fields of a primitive type", accessor);
            } else if (type instanceof BooleanType) {
                throw new TokenLocatedException("Cannot access fields of a boolean type", accessor);
            } else if (type instanceof ArrayType) {
                throw new TokenLocatedException("Cannot access fields of an array", accessor);
            } else if (type instanceof VoidType) {
                throw new TokenLocatedException("Cannot access fields of a void type", accessor);
            } else if (type instanceof ClassType classType) {
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
                    throw new TokenLocatedException("Unknown type '" + classType + "'", accessor);
                }

                PreField found = null;
                for(var f : clz.fields) {
                    if(f.name.equals(field)) {
                        found = f;
                        break;
                    }
                }
                if(found == null) {
                    throw new TokenLocatedException("Unable to find field '" + field + "' in '" + clz.name + "'", accessor);
                }
                storage.pushInstruction(getConstructedInstruction("class_field_store", new Class(classType.getNamespace(), classType.getName(), null, null), field));
                return data.resolveType(found.type);
            } else {
                throw new TokenLocatedException("Unknown type " + type.getClass().getSimpleName(), accessor);
            }
        }else if(accessor.getTokenWithName("ArrayAccessor") != null) {
            if(!(type instanceof ArrayType arrType)) {
                throw new TokenLocatedException("Expected an array got '" + type.getName() + "'", accessor);
            }
            types.push(type);
            var expr = accessor.getTokenWithName("ArrayAccessor").getTokenWithName("Expression");
            generateInstructionsFromExpression(expr, null, types, data, localsManager, storage, false);
            var got = types.pop();
            if(!(got instanceof PrimitiveType)) {
                throw new TokenLocatedException("Could not get the index", expr);
            }
            var arrayData = arrType.type;
            if(arrayData instanceof PrimitiveType primitive) storage.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_array"));
            else if(arrayData instanceof BooleanType) storage.pushInstruction(getConstructedSizeInstruction(8, "store_array"));
            else if(arrayData instanceof ArrayType) storage.pushInstruction(getConstructedInstruction("astore_array"));
            else if(arrayData instanceof ClassType) storage.pushInstruction(getConstructedInstruction("astore_array"));
            else throw new TokenLocatedException("Unsupported type " + arrayData.getClass().getSimpleName(), accessor);
            return arrayData;
        }else {
            throw new TokenLocatedException("Unknown set accessor", accessor);
        }
    }

    public static void compileFunction(CompiledData data, PreFunction function) {
        var returnType = data.resolveType(function.returnType);
        var parameters = new Type[function.parameters.size()];
        String[] names = new String[function.parameters.size()];
        for(int i = 0; i<function.parameters.size(); i++) {
            PreParameter param = function.parameters.get(i);
            parameters[i] = data.resolveType(param.type);
            if(parameters[i] instanceof VoidType) {
                throw new TokenLocatedException("Cannot use 'void' type for arguments");
            }
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

    public static Method compileMethod(CompiledData data, String className, PreFunction function) {
        var returnType = data.resolveType(function.returnType);
        var parameters = new Type[function.parameters.size()];
        String[] names = new String[function.parameters.size()];
        for(int i = 0; i<function.parameters.size(); i++) {
            PreParameter param = function.parameters.get(i);
            parameters[i] = data.resolveType(param.type);
            if(parameters[i] instanceof VoidType) {
                throw new TokenLocatedException("Cannot use 'void' type for arguments");
            }
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
        return new Method(data.namespace, modifiers, className, function.name, signature, bytecode);
    }

    public static void compileClass(CompiledData data, PreClass clazz) {
        var fields = new ClassField[clazz.fields.size()];
        for (int i = 0; i < clazz.fields.size(); i++) {
            var field = clazz.fields.get(i);
            var type = data.resolveType(field.type);
            if(type instanceof VoidType) {
                throw new TokenLocatedException("Cannot create a field with void type");
            }
            fields[i] = new ClassField(field.name, type);
        }

        var methods = new Method[clazz.methods.size()];
        for (int i = 0; i < clazz.methods.size(); i++) {
            methods[i] = compileMethod(data, clazz.name, clazz.methods.get(i));
        }
        data.addClass(new Class(data.namespace, clazz.name, fields, methods));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other) throws UnknownTypeException {
        CompiledData data = new CompiledData(preCompiled.namespace);

        data.using(preCompiled);
        for(var o : other) if(preCompiled.using.contains(o.namespace)) data.using(o);

        for(var clazz : preCompiled.classes) compileClass(data, clazz);
        for(var function : preCompiled.functions) compileFunction(data, function);
        for(var field : preCompiled.fields) {
            var type = data.resolveType(field.type);
            if(type instanceof VoidType) {
                throw new TokenLocatedException("Cannot create a field with void type");
            }
            data.addField(new Field(data.namespace, field.name, type));
        }
        return data;
    }

    public static BigInteger getDecimalInteger(Token token) {
        try {
            return new BigInteger(token.asWordToken().getWord(), 10);
        }catch(NumberFormatException e) {
            throw new TokenLocatedException("Decimal Integer not a valid integer '" + token.asWordToken().getWord() + "'", token);
        }
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
