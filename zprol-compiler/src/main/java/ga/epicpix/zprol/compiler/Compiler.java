package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.precompiled.*;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tree.*;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.*;
import ga.epicpix.zprol.utils.SeekIterator;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static ga.epicpix.zprol.compiler.CompilerIdentifierData.accessorToData;
import static ga.epicpix.zprol.compiler.CompilerUtils.*;
import static ga.epicpix.zprol.compiler.FieldCompiler.compileField;

public class Compiler {
    public static IBytecodeStorage parseFunctionCode(CompiledData data, PreClass clazz, SeekIterator<IStatement> tokens, FunctionSignature sig, String[] names, DataParser parser) {
        IBytecodeStorage storage = createStorage();
        LocalScopeManager localsManager = new LocalScopeManager();
        if(clazz != null) {
            localsManager.defineLocalVariable("this", new ClassType(data.namespace, clazz.name));
        }
        for(int i = 0; i<names.length; i++) {
            localsManager.defineLocalVariable(names[i], sig.parameters()[i]);
        }
        boolean hasReturned = parseFunctionCode(data, tokens, sig, storage, new FunctionCodeScope(localsManager, clazz), parser);
        if(!hasReturned) {
            if(!(sig.returnType() instanceof VoidType)) {
                throw new TokenLocatedException("Missing return statement in " + sig);
            }
            storage.pushInstruction(getConstructedSizeInstruction(0, "return"));
        }
        storage.setLocalsSize(localsManager.getLocalVariablesSize());
        return storage;
    }

    public static boolean parseFunctionCode(CompiledData data, SeekIterator<IStatement> tokens, FunctionSignature sig, IBytecodeStorage bytecode, FunctionCodeScope scope, DataParser parser) {
        scope.start();
        boolean hasReturned = false;
        IStatement token;
        while(tokens.hasNext()) {
            token = tokens.next();
            if(token instanceof ReturnStatementTree retStatement) {
                if(!(sig.returnType() instanceof VoidType)) {
                    if(retStatement.expression() == null) {
                        throw new TokenLocatedException("Function is not void, expected a return value", token, parser);
                    }
                    var types = new ArrayDeque<Type>();
                    generateInstructionsFromExpression(retStatement.expression(), sig.returnType(), types, data, scope, bytecode, false, parser);
                }
                if(sig.returnType() instanceof VoidType) {
                    if(retStatement.expression() != null) {
                        throw new TokenLocatedException("Function is void, expected no value", token, parser);
                    }
                }
                if(sig.returnType() instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "return"));
                }else if(sig.returnType() instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "return"));
                }else if(!(sig.returnType() instanceof VoidType)) {
                    bytecode.pushInstruction(getConstructedInstruction("areturn"));
                }
                hasReturned = true;
                break;
            }else if(token instanceof AccessorStatementTree accessorStatement) {
                generateInstructionsFromExpression(accessorStatement.accessor(), null, new ArrayDeque<>(), data, scope, bytecode, true, parser);
            }else if(token instanceof CreateAssignmentStatementTree createAssignmentStatement) {
                var type = data.resolveType(createAssignmentStatement.type().toString());
                if(type instanceof VoidType) {
                    throw new TokenLocatedException("Cannot create a variable with void type", token, parser);
                }
                var name = createAssignmentStatement.name().toStringRaw();
                var expression = createAssignmentStatement.expression();
                var types = new ArrayDeque<Type>();
                generateInstructionsFromExpression(expression, type, types, data, scope, bytecode, false, parser);
                var rType = types.pop();
                doCast(rType, type, false, bytecode, expression, parser);
                var local = scope.localsManager.defineLocalVariable(name, type);
                if(type instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_local", local.index()));
                }else {
                    bytecode.pushInstruction(getConstructedInstruction("astore_local", local.index()));
                }
            }else if(token instanceof AssignmentStatementTree assignmentStatement) {
                var tempStorage = createStorage();
                var accessorData = accessorToData(assignmentStatement.accessor());

                ArrayDeque<Type> types = new ArrayDeque<>();
                for (int i = 0; i < accessorData.length - 1; i++) {
                    var v = accessorData[i];
                    if(v instanceof CompilerIdentifierDataFunction func) {
                        var context = getClassContext(i, scope.thisClass, types, data, v.location, parser);
                        doFunctionCall(func, context, data, null, types, scope, tempStorage, false, i == 0, parser);
                    }else if(v instanceof CompilerIdentifierDataField field) {
                        var context = getClassContext(i, scope.thisClass, types, data, v.location, parser);
                        var ret = field.loadField(context, scope.localsManager, tempStorage, data, i == 0);
                        if(ret == null) {
                            throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location, parser);
                        }
                        types.push(ret);
                    }else if(v instanceof CompilerIdentifierDataArray array) {
                        var currentType = types.pop();
                        if(!(currentType instanceof ArrayType arrType)) throw new TokenLocatedException("Expected an array type", v.location, parser);
                        types.push(array.loadArray(arrType, data, scope, tempStorage, parser));
                    }else {
                        throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location, parser);
                    }
                }
                var v = accessorData[accessorData.length - 1];

                Type expectedType;

                if(v instanceof CompilerIdentifierDataField field) {
                    var context = getClassContext(accessorData.length - 1, scope.thisClass, types, data, v.location, parser);
                    expectedType = field.storeField(context, scope.localsManager, types.size() != 0 ? types.pop() : null, tempStorage, data, accessorData.length == 1, parser);
                    if(expectedType == null) {
                        throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location, parser);
                    }
                }else if(v instanceof CompilerIdentifierDataArray array) {
                    var currentType = types.pop();
                    if(!(currentType instanceof ArrayType arrType)) throw new TokenLocatedException("Expected an array type", v.location, parser);
                    expectedType = array.storeArray(arrType, data, scope, tempStorage, parser);
                }else {
                    throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location, parser);
                }

                generateInstructionsFromExpression(assignmentStatement.expression(), expectedType, types, data, scope, bytecode, parser);
                doCast(types.pop(), expectedType, false, bytecode, assignmentStatement.expression(), parser);
                for(var instr : tempStorage.getInstructions()) bytecode.pushInstruction(instr);
            }else if(token instanceof IfStatementTree ifStatement) {
                var types = new ArrayDeque<Type>();
                generateInstructionsFromExpression(ifStatement.expression(), null, types, data, scope, bytecode, parser);
                var ret = types.pop();
                if(!(ret instanceof BooleanType)) {
                    throw new TokenLocatedException("Expected boolean expression", ifStatement.expression(), parser);
                }
                var statements = new ArrayList<>(ifStatement.code().statements());

                int preInstr = bytecode.getInstructions().size();
                bytecode.pushInstruction(getConstructedInstruction("int"));
                parseFunctionCode(data, new SeekIterator<>(statements), sig, bytecode, new FunctionCodeScope(FunctionCodeScope.ScopeType.IF, scope), parser);
                int postInstr = bytecode.getInstructions().size();
                if(ifStatement.elseStatement() != null) {
                    var elseStatements = new ArrayList<>(ifStatement.elseStatement().code().statements());
                    parseFunctionCode(data, new SeekIterator<>(elseStatements), sig, bytecode, new FunctionCodeScope(FunctionCodeScope.ScopeType.ELSE, scope), parser);
                    bytecode.pushInstruction(postInstr, getConstructedInstruction("jmp", bytecode.getInstructions().size()-postInstr+1));
                    postInstr++;
                }
                bytecode.replaceInstruction(preInstr, getConstructedInstruction("neqjmp", postInstr-preInstr));
            }else if(token instanceof WhileStatementTree whileStatement) {
                var types = new ArrayDeque<Type>();
                int preInstr = bytecode.getInstructionsLength();
                generateInstructionsFromExpression(whileStatement.expression(), null, types, data, scope, bytecode, false, parser);
                var ret = types.pop();
                if(!(ret instanceof BooleanType)) {
                    throw new TokenLocatedException("Expected boolean expression", whileStatement.expression(), parser);
                }
                int addInstr = bytecode.getInstructionsLength();
                bytecode.pushInstruction(getConstructedInstruction("int"));

                var statements = new ArrayList<>(whileStatement.code().statements());
                FunctionCodeScope fscope = new FunctionCodeScope(FunctionCodeScope.ScopeType.WHILE, scope);
                parseFunctionCode(data, new SeekIterator<>(statements), sig, bytecode, fscope, parser);
                int postInstr = bytecode.getInstructionsLength();

                bytecode.pushInstruction(getConstructedInstruction("jmp", preInstr-postInstr));
                bytecode.replaceInstruction(addInstr, getConstructedInstruction("neqjmp", postInstr-addInstr+1));

                for(int location : fscope.breakLocations) bytecode.replaceInstruction(location, getConstructedInstruction("jmp", postInstr-location+1));
                for(int location : fscope.continueLocations) bytecode.replaceInstruction(location, getConstructedInstruction("jmp", preInstr- location));
            }else if(token instanceof BreakStatementTree breakStatement) {
                var whileLoc = scope;
                while(whileLoc != null) {
                    if(whileLoc.scopeType == FunctionCodeScope.ScopeType.WHILE) {
                        break;
                    }
                    whileLoc = whileLoc.previous;
                }
                if(whileLoc != null) {
                    whileLoc.addBreakLocation(bytecode.getInstructionsLength());
                    bytecode.pushInstruction(getConstructedInstruction("int"));
                }else {
                    throw new TokenLocatedException("Cannot use `break` without a `while` loop", breakStatement, parser);
                }
            }else if(token instanceof ContinueStatementTree continueStatement) {
                var whileLoc = scope;
                while(whileLoc != null) {
                    if(whileLoc.scopeType == FunctionCodeScope.ScopeType.WHILE) {
                        break;
                    }
                    whileLoc = whileLoc.previous;
                }
                if(whileLoc != null) {
                    whileLoc.addContinueLocation(bytecode.getInstructionsLength());
                    bytecode.pushInstruction(getConstructedInstruction("int"));
                }else {
                    throw new TokenLocatedException("Cannot use `continue` without a `while` loop", continueStatement, parser);
                }
            }else {
                throw new TokenLocatedException("Not implemented statement", token, parser);
            }
        }
        scope.finish();
        return hasReturned;
    }

    public static PreClass getClassContext(int index, PreClass thisClass, ArrayDeque<Type> types, CompiledData data, ITree location, DataParser parser) {
        if(index != 0) {
            if(types.size() != 0 && types.peek() instanceof ClassType) {
                return classTypeToPreClass((ClassType) types.pop(), data);
            }else {
                throw new TokenLocatedException("Expected a class type", location, parser);
            }
        }else {
            return thisClass;
        }
    }

    public static void generateInstructionsFromExpression(IExpression token, Type expectedType, ArrayDeque<Type> types, CompiledData data, FunctionCodeScope scope, IBytecodeStorage bytecode, boolean discardValue, DataParser parser) {
        generateInstructionsFromExpression(token, expectedType, types, data, scope, bytecode, parser);
        Type prob = types.size() != 0 ? types.peek() : null;
        if(prob != null && discardValue) {
            if(prob instanceof PrimitiveType primitive) {
                if(primitive.getSize() != 0) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "pop"));
                } else {
                    types.push(primitive);
                }
            } else if(prob instanceof BooleanType) {
                bytecode.pushInstruction(getConstructedSizeInstruction(8, "pop"));
            } else {
                bytecode.pushInstruction(getConstructedInstruction("apop"));
            }
        }
    }

    public static void generateInstructionsFromExpression(IExpression token, Type expectedType, ArrayDeque<Type> types, CompiledData data, FunctionCodeScope scope, IBytecodeStorage bytecode, DataParser parser) {
        if(token instanceof OperatorExpressionTree operatorExpression) {
            types.push(runOperator(operatorExpression, expectedType, data, scope, types, bytecode, parser));
        }else if(token instanceof LiteralTree literal) {
            if(literal.type() == LiteralType.INTEGER) {
                BigInteger number = (BigInteger) literal.value();
                if(expectedType instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "push", number));
                    types.push(primitive);
                } else if(expectedType == null) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(4, "push", number));
                    types.push(data.resolveType("int32"));
                } else {
                    throw new TokenLocatedException("Cannot infer size of number from a non-primitive type (" + expectedType.getName() + ")", token, parser);
                }
            }else if(literal.type() == LiteralType.BOOLEAN) {
                if(literal.value() == Boolean.TRUE) {
                    bytecode.pushInstruction(getConstructedInstruction("push_true"));
                }else {
                    bytecode.pushInstruction(getConstructedInstruction("push_false"));
                }
                types.push(new BooleanType());
            }else if(literal.type() == LiteralType.STRING) {
                bytecode.pushInstruction(getConstructedInstruction("push_string", literal.value()));
                types.push(data.resolveType("zprol.lang.String"));
            }else if(literal.type() == LiteralType.NULL) {
                bytecode.pushInstruction(getConstructedInstruction("null"));
                types.push(new NullType());
            }else {
                throw new TokenLocatedException("Unknown literal", literal, parser);
            }
        }else if(token instanceof CastTree cast) {
            var op = cast.type();
            boolean isHardCast = cast.hardCast();
            var castType = data.resolveType(op.toString());
            generateInstructionsFromExpression(cast.value(), null, types, data, scope, bytecode, parser);
            var from = types.pop();
            if(isHardCast) {
                // this will not check sizes of primitive types, this is unsafe
                types.push(castType);
                return;
            }
            types.push(doCast(from, castType, true, bytecode, cast.value(), parser));
        }else if(token instanceof AccessorTree accessor) {
            var accessorData = accessorToData(accessor);
            for(int i = 0; i < accessorData.length; i++) {
                var v = accessorData[i];
                if(v instanceof CompilerIdentifierDataFunction func) {
                    var context = getClassContext(i, scope.thisClass, types, data, v.location, parser);
                    doFunctionCall(func, context, data, null, types, scope, bytecode, false, i == 0, parser);
                } else if(v instanceof CompilerIdentifierDataField field) {
                    var context = getClassContext(i, scope.thisClass, types, data, v.location, parser);
                    var ret = field.loadField(context, scope.localsManager, bytecode, data, i == 0);
                    if(ret == null) {
                        throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location, parser);
                    }
                    types.push(ret);
                } else if(v instanceof CompilerIdentifierDataArray array) {
                    var currentType = types.pop();
                    if(!(currentType instanceof ArrayType arrType))
                        throw new TokenLocatedException("Expected an array type", v.location, parser);
                    types.push(array.loadArray(arrType, data, scope, bytecode, parser));
                } else {
                    throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location, parser);
                }
            }
        }else {
            throw new TokenLocatedException("Unknown expression", token, parser);
        }
    }

    public static Type runOperator(OperatorExpressionTree token, Type expectedType, CompiledData data, FunctionCodeScope scope, ArrayDeque<Type> types, IBytecodeStorage bytecode, DataParser parser) {

        generateInstructionsFromExpression(token.left(), expectedType, types, data, scope, bytecode, false, parser);
        var arg1 = types.pop();
        var arg2Storage = createStorage();
        generateInstructionsFromExpression(token.right(), expectedType, types, data, scope, arg2Storage, false, parser);
        var arg2 = types.pop();
        return runOperator(arg1, arg2, token.operator(), token, bytecode, arg2Storage, parser);

        // TODO: parsing has wrong 'merge' implementation for operators ._. this would never have more than 1 operators with the issue

//        var tokens = named.tokens;
//        if (tokens[0] instanceof LexerToken lexer && lexer.name.equals("OpenParen")) {
//            generateInstructionsFromExpression(tokens[1], expectedType, types, data, scope, bytecode, false);
//            return types.pop();
//        }
//        generateInstructionsFromExpression(tokens[0], expectedType, types, data, scope, bytecode, false);
//        var arg1 = types.pop();
//        int amtOperators = (tokens.length - 1) / 2;
//        for (int i = 0; i < amtOperators; i++) {
//            var arg2Storage = createStorage();
//            generateInstructionsFromExpression(tokens[i * 2 + 2], expectedType, types, data, scope, arg2Storage, false);
//            var arg2 = types.pop();
//
//            arg1 = runOperator(arg1, arg2, tokens[i * 2 + 1].asLexerToken(), token, bytecode, arg2Storage);
//        }
//        return arg1;
    }

    public static Type runOperator(Type arg1, Type arg2, LexerToken operator, ITree location, IBytecodeStorage bytecode, IBytecodeStorage arg2bytecode, DataParser parser) {
        var operatorName = operator.toStringRaw();

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
            if(arg1 instanceof BooleanType && arg2 instanceof BooleanType) {
                if(operatorName.equals("==")) {
                    for(var instr : arg2bytecode.getInstructions()) {
                        bytecode.pushInstruction(instr);
                    }
                    bytecode.pushInstruction(getConstructedInstruction("beq"));
                    return new BooleanType();
                }else if(operatorName.equals("!=")) {
                    for(var instr : arg2bytecode.getInstructions()) {
                        bytecode.pushInstruction(instr);
                    }
                    bytecode.pushInstruction(getConstructedInstruction("bneq"));
                    return new BooleanType();
                }
            }
            throw new TokenLocatedException("Cannot perform math operation on types " + arg1.getName() + " <-> " + arg2.getName(), location, parser);
        }
        var bigger = prim1.getSize() > prim2.getSize() ? prim1 : prim2;
        var smaller = prim1.getSize() <= prim2.getSize() ? prim1 : prim2;
        if(prim2 != bigger) {
            for(var instr : arg2bytecode.getInstructions()) {
                bytecode.pushInstruction(instr);
            }
        }
        var got = doCast(smaller, bigger, false, bytecode, location, parser);
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

    public static Type doCast(Type from, Type to, boolean explicit, IBytecodeStorage bytecode, ITree location, DataParser parser) {
        if(from instanceof NullType && to instanceof ClassType) {
            return to;
        }
        if(!(from instanceof PrimitiveType primitiveFrom) || !(to instanceof PrimitiveType primitiveTo)) {
            if(!from.equals(to)) {
                throw new TokenLocatedException("Unsupported cast from " + from.getName() + " to " + to.getName(), location, parser);
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
        throw new TokenLocatedException("Unsupported implicit cast from " + from.getName() + " to " + to.getName(), location, parser);
    }

    public static void doFunctionCall(CompilerIdentifierDataFunction func, PreClass classContext, CompiledData data, Type expectedType, ArrayDeque<Type> types, FunctionCodeScope scope, IBytecodeStorage bytecode, boolean discardValue, boolean searchPublic, DataParser parser) {
        var possibleFunctions = func.lookupFunction(data, classContext, searchPublic);
        if(possibleFunctions.size() == 0)
            throw new TokenLocatedException("Function not defined: " + func.getFunctionName(), func.location, parser);

        if(possibleFunctions.size() != 1) {

            var garbage = createStorage();
            var argTypes = new ArrayList<Type>();

            for(var arg : func.arguments) {
                generateInstructionsFromExpression(arg, null, types, data, scope, garbage, parser);
                argTypes.add(types.pop());
            }

            var candidates = new ArrayList<String>();
            var closestScore = 0;
            var closest = new ArrayList<LookupFunction>();
            for(var a : possibleFunctions) {
                boolean matches = true;
                boolean nPrimMatches = true;
                int score = 0;
                for(int i = 0; i<a.func().parameters.size(); i++) {
                    var t = data.resolveType(a.func().parameters.get(i).type);
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
                candidates.add("(" + a.func().parameters.stream().map(b -> data.resolveType(b.type).getName()).collect(Collectors.joining(", ")) + ")");
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
                                .map(a -> "(" + a.func().parameters.stream().map(b -> data.resolveType(b.type).getName()).collect(Collectors.joining(", ")) + ")")
                                .collect(Collectors.joining("\n")
                                ), func.location, parser);
                }
                throw new TokenLocatedException("Cannot find overload for arguments (" + argTypes.stream().map(Type::getName).collect(Collectors.joining(", ")) + ")\nCandidates are:\n" + String.join("\n", candidates), func.location, parser);
            }
        }

        var lfunc = possibleFunctions.get(0);
        var f = lfunc.func();
        var returnType = data.resolveType(f.returnType);
        var parameters = new Type[f.parameters.size()];
        for(int i = 0; i<f.parameters.size(); i++) {
            parameters[i] = data.resolveType(f.parameters.get(i).type);
        }
        FunctionSignature signature = new FunctionSignature(returnType, parameters);

        if(lfunc.isClassMethod() && searchPublic) {
            bytecode.pushInstruction(getConstructedInstruction("aload_local", scope.localsManager.getLocalVariable("this").index()));
        }

        for(int i = 0; i<func.arguments.length; i++) {
            generateInstructionsFromExpression(func.arguments[i], parameters[i], types, data, scope, bytecode, parser);
            doCast(types.pop(), signature.parameters()[i], false, bytecode, func.arguments[i], parser);
        }

        EnumSet<FunctionModifiers> fMods = EnumSet.noneOf(FunctionModifiers.class);
        fMods.addAll(f.modifiers);
        if(lfunc.isClassMethod()) {
            bytecode.pushInstruction(getConstructedInstruction("invoke_class", new Method(classContext.namespace, fMods, classContext.name, func.getFunctionName(), signature, null)));
        }else {
            bytecode.pushInstruction(getConstructedInstruction("invoke", new Function(data.namespace, fMods, func.getFunctionName(), signature, null)));
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
                types.push(doCast(returnType, expectedType, false, bytecode, func.location, parser));
            } else {
                types.push(returnType);
            }
        }
    }

    public static void compileFunction(CompiledData data, PreFunction function, DataParser parser) {
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
            bytecode = parseFunctionCode(data, null, new SeekIterator<>(function.code), signature, names, parser);
        }
        EnumSet<FunctionModifiers> fMods = EnumSet.noneOf(FunctionModifiers.class);
        fMods.addAll(function.modifiers);
        data.addFunction(new Function(data.namespace, fMods, function.name, signature, bytecode));
    }

    public static Method compileMethod(CompiledData data, PreClass clazz, PreFunction function, DataParser parser) {
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
            bytecode = parseFunctionCode(data, clazz, new SeekIterator<>(function.code), signature, names, parser);
        }
        EnumSet<FunctionModifiers> fMods = EnumSet.noneOf(FunctionModifiers.class);
        fMods.addAll(function.modifiers);
        return new Method(data.namespace, fMods, clazz.name, function.name, signature, bytecode);
    }

    public static void compileClass(CompiledData data, PreClass clazz, DataParser parser) {
        var fields = new ClassField[clazz.fields.size()];
        for (int i = 0; i < clazz.fields.size(); i++) {
            fields[i] = (ClassField) compileField(data, clazz.fields.get(i), clazz, parser);
        }

        var methods = new Method[clazz.methods.size()];
        for (int i = 0; i < clazz.methods.size(); i++) {
            methods[i] = compileMethod(data, clazz, clazz.methods.get(i), parser);
        }
        data.addClass(new Class(data.namespace, clazz.name, fields, methods));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other, DataParser parser) throws UnknownTypeException {
        CompiledData data = new CompiledData(preCompiled.namespace);

        data.using(preCompiled);
        for(var o : other) if(preCompiled.using.contains(o.namespace)) data.using(o);

        for(var clazz : preCompiled.classes) compileClass(data, clazz, parser);
        for(var function : preCompiled.functions) compileFunction(data, function, parser);
        for(var field : preCompiled.fields) {
            data.addField((Field) compileField(data, field, null, parser));
        }
        return data;
    }

}
