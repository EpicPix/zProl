package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.precompiled.*;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
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
    public static IBytecodeStorage parseFunctionCode(CompiledData data, PreClass clazz, SeekIterator<Token> tokens, FunctionSignature sig, String[] names) {
        IBytecodeStorage storage = createStorage();
        LocalScopeManager localsManager = new LocalScopeManager();
        if(clazz != null) {
            localsManager.defineLocalVariable("this", new ClassType(data.namespace, clazz.name));
        }
        for(int i = 0; i<names.length; i++) {
            localsManager.defineLocalVariable(names[i], sig.parameters()[i]);
        }
        boolean hasReturned = parseFunctionCode(data, tokens, sig, storage, new FunctionCodeScope(localsManager, clazz));
        if(!hasReturned) {
            if(!(sig.returnType() instanceof VoidType)) {
                throw new TokenLocatedException("Missing return statement in " + sig);
            }
            storage.pushInstruction(getConstructedSizeInstruction(0, "return"));
        }
        storage.setLocalsSize(localsManager.getLocalVariablesSize());
        return storage;
    }

    public static boolean parseFunctionCode(CompiledData data, SeekIterator<Token> tokens, FunctionSignature sig, IBytecodeStorage bytecode, FunctionCodeScope scope) {
        scope.start();
        boolean hasReturned = false;
        Token token;
        while(tokens.hasNext()) {
            token = tokens.next();
            if(token instanceof NamedToken named) {
                if("Whitespace".equals(named.name)) continue;
                if("ReturnStatement".equals(named.name)) {
                    if(!(sig.returnType() instanceof VoidType)) {
                        if(named.getTokenWithName("Expression") == null) {
                            throw new TokenLocatedException("Function is not void, expected a return value", named);
                        }
                        var types = new ArrayDeque<Type>();
                        generateInstructionsFromExpression(named.getTokenWithName("Expression"), sig.returnType(), types, data, scope, bytecode, false);
                    }
                    if(sig.returnType() instanceof VoidType) {
                        if(named.getTokenWithName("Expression") != null) {
                            throw new TokenLocatedException("Function is void, expected no value", named);
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
                } else if("AccessorStatement".equals(named.name)) {
                    generateInstructionsFromExpression(named.getTokenWithName("Accessor"), null, new ArrayDeque<>(), data, scope, bytecode, true);
                } else if("CreateAssignmentStatement".equals(named.name)) {
                    var type = data.resolveType(named.getTokenAsString("Type"));
                    if(type instanceof VoidType) {
                        throw new TokenLocatedException("Cannot create a variable with void type", named);
                    }
                    var name = named.getTokenAsString("Identifier");
                    var expression = named.getTokenWithName("Expression");
                    var types = new ArrayDeque<Type>();
                    generateInstructionsFromExpression(expression, type, types, data, scope, bytecode, false);
                    var rType = types.pop();
                    doCast(rType, type, false, bytecode, expression);
                    var local = scope.localsManager.defineLocalVariable(name, type);
                    if(type instanceof PrimitiveType primitive) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_local", local.index()));
                    }else {
                        bytecode.pushInstruction(getConstructedInstruction("astore_local", local.index()));
                    }
                } else if("AssignmentStatement".equals(named.name)) {
                    var tempStorage = createStorage();
                    var accessorData = accessorToData(named.getTokenWithName("Accessor"));

                    ArrayDeque<Type> types = new ArrayDeque<>();
                    for (int i = 0; i < accessorData.length - 1; i++) {
                        var v = accessorData[i];
                        if(v instanceof CompilerIdentifierDataFunction func) {
                            var context = getClassContext(i, scope.thisClass, types, data, v.location);
                            doFunctionCall(func, context, data, null, types, scope, tempStorage, false, i == 0);
                        }else if(v instanceof CompilerIdentifierDataField field) {
                            var context = getClassContext(i, scope.thisClass, types, data, v.location);
                            var ret = field.loadField(context, scope.localsManager, tempStorage, data, i == 0);
                            if(ret == null) {
                                throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location);
                            }
                            types.push(ret);
                        }else if(v instanceof CompilerIdentifierDataArray array) {
                            var currentType = types.pop();
                            if(!(currentType instanceof ArrayType arrType)) throw new TokenLocatedException("Expected an array type", v.location);
                            types.push(array.loadArray(arrType, data, scope, tempStorage));
                        }else {
                            throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location);
                        }
                    }
                    var v = accessorData[accessorData.length - 1];

                    Type expectedType;

                    if(v instanceof CompilerIdentifierDataField field) {
                        var context = getClassContext(accessorData.length - 1, scope.thisClass, types, data, v.location);
                        expectedType = field.storeField(context, scope.localsManager, types.size() != 0 ? types.pop() : null, tempStorage, data, accessorData.length == 1);
                        if(expectedType == null) {
                            throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location);
                        }
                    }else if(v instanceof CompilerIdentifierDataArray array) {
                        var currentType = types.pop();
                        if(!(currentType instanceof ArrayType arrType)) throw new TokenLocatedException("Expected an array type", v.location);
                        expectedType = array.storeArray(arrType, data, scope, tempStorage);
                    }else {
                        throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location);
                    }

                    generateInstructionsFromExpression(named.getTokenWithName("Expression"), expectedType, types, data, scope, bytecode, false);
                    doCast(types.pop(), expectedType, false, bytecode, named.getTokenWithName("Expression"));
                    for(var instr : tempStorage.getInstructions()) bytecode.pushInstruction(instr);
                } else if("IfStatement".equals(named.name)) {
                    var types = new ArrayDeque<Type>();
                    generateInstructionsFromExpression(named.getTokenWithName("Expression"), null, types, data, scope, bytecode, false);
                    var ret = types.pop();
                    if(!(ret instanceof BooleanType)) {
                        throw new TokenLocatedException("Expected boolean expression", named.getTokenWithName("Expression"));
                    }
                    var statements = new ArrayList<Token>();
                    for(var statement : named.getTokenWithName("Code").getTokensWithName("Statement")) {
                        statements.add(statement.tokens[0]);
                    }
                    int preInstr = bytecode.getInstructions().size();
                    bytecode.pushInstruction(getConstructedInstruction("int"));
                    parseFunctionCode(data, new SeekIterator<>(statements), sig, bytecode, new FunctionCodeScope(FunctionCodeScope.ScopeType.IF, scope));
                    int postInstr = bytecode.getInstructions().size();
                    if(named.getTokenWithName("ElseStatement") != null) {
                        var elseStatements = new ArrayList<Token>();
                        for(var statement : named.getTokenWithName("ElseStatement").getTokenWithName("Code").getTokensWithName("Statement")) {
                            elseStatements.add(statement.tokens[0]);
                        }
                        parseFunctionCode(data, new SeekIterator<>(elseStatements), sig, bytecode, new FunctionCodeScope(FunctionCodeScope.ScopeType.ELSE, scope));
                        bytecode.pushInstruction(postInstr, getConstructedInstruction("jmp", bytecode.getInstructions().size()-postInstr+1));
                        postInstr++;
                    }
                    bytecode.replaceInstruction(preInstr, getConstructedInstruction("neqjmp", postInstr-preInstr));
                } else if("WhileStatement".equals(named.name)) {
                    var types = new ArrayDeque<Type>();
                    int preInstr = bytecode.getInstructionsLength();
                    generateInstructionsFromExpression(named.getTokenWithName("Expression"), null, types, data, scope, bytecode, false);
                    var ret = types.pop();
                    if(!(ret instanceof BooleanType)) {
                        throw new TokenLocatedException("Expected boolean expression", named.getTokenWithName("Expression"));
                    }
                    int addInstr = bytecode.getInstructionsLength();
                    bytecode.pushInstruction(getConstructedInstruction("int"));

                    var statements = new ArrayList<Token>();
                    for(var statement : named.getTokenWithName("Code").getTokensWithName("Statement")) {
                        statements.add(statement.tokens[0]);
                    }
                    FunctionCodeScope fscope = new FunctionCodeScope(FunctionCodeScope.ScopeType.WHILE, scope);
                    parseFunctionCode(data, new SeekIterator<>(statements), sig, bytecode, fscope);
                    int postInstr = bytecode.getInstructionsLength();

                    bytecode.pushInstruction(getConstructedInstruction("jmp", preInstr-postInstr));
                    bytecode.replaceInstruction(addInstr, getConstructedInstruction("neqjmp", postInstr-addInstr+1));

                    for(int location : fscope.breakLocations) {
                        bytecode.replaceInstruction(location, getConstructedInstruction("jmp", postInstr-location+1));
                    }

                    for(int location : fscope.continueLocations) {
                        bytecode.replaceInstruction(location, getConstructedInstruction("jmp", preInstr- location));
                    }

                }  else if("BreakStatement".equals(named.name)) {
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
                        throw new TokenLocatedException("Cannot use `break` without a `while` loop", named.getLexerToken("BreakKeyword"));
                    }
                }  else if("ContinueStatement".equals(named.name)) {
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
                        throw new TokenLocatedException("Cannot use `continue` without a `while` loop", named.getLexerToken("ContinueKeyword"));
                    }
                } else {
                    throw new TokenLocatedException("Not implemented language feature: " + named.name + " / " + Arrays.toString(named.tokens), named);
                }
            }
        }
        scope.finish();
        return hasReturned;
    }

    public static PreClass getClassContext(int index, PreClass thisClass, ArrayDeque<Type> types, CompiledData data, Token location) {
        if(index != 0) {
            if(types.size() != 0 && types.peek() instanceof ClassType) {
                return classTypeToPreClass((ClassType) types.pop(), data);
            }else {
                throw new TokenLocatedException("Expected a class type", location);
            }
        }else {
            return thisClass;
        }
    }

    public static void generateInstructionsFromExpression(NamedToken token, Type expectedType, ArrayDeque<Type> types, CompiledData data, FunctionCodeScope scope, IBytecodeStorage bytecode, boolean discardValue) {
        switch(token.name) {
            case "Expression" -> {
                generateInstructionsFromExpression(token.tokens[0].asNamedToken(), expectedType, types, data, scope, bytecode, expectedType == null && discardValue);
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
                } else if(prob != null) {
                    types.push(prob);
                }
            }
            case "MultiplicativeExpression", "AdditiveExpression", "InclusiveOrExpression", "EqualsExpression", "ShiftExpression", "InclusiveAndExpression", "CompareExpression" -> types.push(runOperator(token, expectedType, data, scope, types, bytecode));
            case "DecimalInteger", "HexInteger" -> {
                BigInteger number = switch(token.name) {
                    case "DecimalInteger" -> getDecimalInteger(token.tokens[0]);
                    case "HexInteger" -> getHexInteger(token.tokens[0]);
                    default -> throw new TokenLocatedException("Impossible case", token);
                };
                if(expectedType instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "push", number));
                    types.push(primitive);
                } else if(expectedType == null) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(4, "push", number));
                    types.push(data.resolveType("int32"));
                } else {
                    throw new TokenLocatedException("Cannot infer size of number from a non-primitive type (" + expectedType.getName() + ")", token);
                }
            }
            case "Accessor" -> {
                var accessorData = accessorToData(token);
                for(int i = 0; i < accessorData.length; i++) {
                    var v = accessorData[i];
                    if(v instanceof CompilerIdentifierDataFunction func) {
                        var context = getClassContext(i, scope.thisClass, types, data, v.location);
                        doFunctionCall(func, context, data, null, types, scope, bytecode, discardValue && i == accessorData.length - 1, i == 0);
                    } else if(v instanceof CompilerIdentifierDataField field) {
                        var context = getClassContext(i, scope.thisClass, types, data, v.location);
                        var ret = field.loadField(context, scope.localsManager, bytecode, data, i == 0);
                        if(ret == null) {
                            throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location);
                        }
                        types.push(ret);
                    } else if(v instanceof CompilerIdentifierDataArray array) {
                        var currentType = types.pop();
                        if(!(currentType instanceof ArrayType arrType))
                            throw new TokenLocatedException("Expected an array type", v.location);
                        types.push(array.loadArray(arrType, data, scope, bytecode));
                    } else {
                        throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location);
                    }
                }
            }
            case "String" -> {
                bytecode.pushInstruction(getConstructedInstruction("push_string", convertToLanguageString(token)));
                types.push(data.resolveType("zprol.lang.String"));
            }
            case "CastExpression" -> {
                var hardCast = token.getTokenWithName("HardCastOperator");
                var castType = data.resolveType((hardCast != null ? hardCast : token.getTokenWithName("CastOperator")).getTokenAsString("Type"));
                var tokens = token.getNonWhitespaceTokens();
                if(tokens[1] instanceof LexerToken lex && lex.name.equals("OpenParen")) {
                    generateInstructionsFromExpression(tokens[2].asNamedToken(), null, types, data, scope, bytecode, false);
                } else {
                    generateInstructionsFromExpression(tokens[1].asNamedToken(), null, types, data, scope, bytecode, false);
                }
                var from = types.pop();
                if(hardCast != null) {
                    // this will not check sizes of primitive types, this is unsafe
                    types.push(castType);
                    return;
                }
                types.push(doCast(from, castType, true, bytecode, token.tokens[1]));
            }
            case "Boolean" -> {
                bytecode.pushInstruction(getConstructedInstruction("push_" + token.toStringRaw()));
                types.push(new BooleanType());
            }
            case "Null" -> {
                bytecode.pushInstruction(getConstructedInstruction("null"));
                types.push(new NullType());
            }
            default -> throw new TokenLocatedException("Unknown token " + token.name, token);
        }
    }

    public static Type runOperator(NamedToken token, Type expectedType, CompiledData data, FunctionCodeScope scope, ArrayDeque<Type> types, IBytecodeStorage bytecode) {
        var tokens = token.getNonWhitespaceTokens();
        if (tokens[0] instanceof LexerToken lexer && lexer.name.equals("OpenParen")) {
            generateInstructionsFromExpression(tokens[1].asNamedToken(), expectedType, types, data, scope, bytecode, false);
            return types.pop();
        }
        generateInstructionsFromExpression(tokens[0].asNamedToken(), expectedType, types, data, scope, bytecode, false);
        var arg1 = types.pop();

        int amtOperators = (tokens.length - 1) / 2;
        for (int i = 0; i < amtOperators; i++) {
            var arg2Storage = createStorage();
            generateInstructionsFromExpression(tokens[i * 2 + 2].asNamedToken(), expectedType, types, data, scope, arg2Storage, false);
            var arg2 = types.pop();

            arg1 = runOperator(arg1, arg2, tokens[i * 2 + 1].asLexerToken(), token, bytecode, arg2Storage);
        }

        return arg1;
    }

    public static Type runOperator(Type arg1, Type arg2, LexerToken operator, Token location, IBytecodeStorage bytecode, IBytecodeStorage arg2bytecode) {
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

    public static void doFunctionCall(CompilerIdentifierDataFunction func, PreClass classContext, CompiledData data, Type expectedType, ArrayDeque<Type> types, FunctionCodeScope scope, IBytecodeStorage bytecode, boolean discardValue, boolean searchPublic) {
        var possibleFunctions = func.lookupFunction(data, classContext, searchPublic);
        if(possibleFunctions.size() == 0)
            throw new TokenLocatedException("Function not defined: " + func.getFunctionName(), func.location);

        if(possibleFunctions.size() != 1) {

            var garbage = createStorage();
            var argTypes = new ArrayList<Type>();

            for(var arg : func.arguments) {
                generateInstructionsFromExpression(arg, null, types, data, scope, garbage, false);
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
                                ), func.location);
                }
                throw new TokenLocatedException("Cannot find overload for arguments (" + argTypes.stream().map(Type::getName).collect(Collectors.joining(", ")) + ")\nCandidates are:\n" + String.join("\n", candidates), func.location);
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
        EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
        for(PreFunctionModifiers modifier : f.modifiers) {
            modifiers.add(modifier.getCompiledModifier());
        }

        if(lfunc.isClassMethod() && searchPublic) {
            bytecode.pushInstruction(getConstructedInstruction("aload_local", scope.localsManager.getLocalVariable("this").index()));
        }

        for(int i = 0; i<func.arguments.length; i++) {
            generateInstructionsFromExpression(func.arguments[i], parameters[i], types, data, scope, bytecode, false);
            doCast(types.pop(), signature.parameters()[i], false, bytecode, func.arguments[i]);
        }

        if(lfunc.isClassMethod()) {
            bytecode.pushInstruction(getConstructedInstruction("invoke_class", new Method(classContext.namespace, modifiers, classContext.name, func.getFunctionName(), signature, null)));
        }else {
            bytecode.pushInstruction(getConstructedInstruction("invoke", new Function(data.namespace, modifiers, func.getFunctionName(), signature, null)));
        }

        if(!discardValue && returnType instanceof VoidType) {
            throw new TokenLocatedException("Cannot store a void type", func.location);
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
                types.push(doCast(returnType, expectedType, false, bytecode, func.location));
            } else {
                types.push(returnType);
            }
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
            bytecode = parseFunctionCode(data, null, new SeekIterator<>(function.code), signature, names);
        }
        EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
        for(PreFunctionModifiers modifier : function.modifiers) {
            modifiers.add(modifier.getCompiledModifier());
        }
        data.addFunction(new Function(data.namespace, modifiers, function.name, signature, bytecode));
    }

    public static Method compileMethod(CompiledData data, PreClass clazz, PreFunction function) {
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
            bytecode = parseFunctionCode(data, clazz, new SeekIterator<>(function.code), signature, names);
        }
        EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
        for(PreFunctionModifiers modifier : function.modifiers) {
            modifiers.add(modifier.getCompiledModifier());
        }
        return new Method(data.namespace, modifiers, clazz.name, function.name, signature, bytecode);
    }

    public static void compileClass(CompiledData data, PreClass clazz) {
        var fields = new ClassField[clazz.fields.size()];
        for (int i = 0; i < clazz.fields.size(); i++) {
            fields[i] = (ClassField) compileField(data, clazz.fields.get(i), clazz);
        }

        var methods = new Method[clazz.methods.size()];
        for (int i = 0; i < clazz.methods.size(); i++) {
            methods[i] = compileMethod(data, clazz, clazz.methods.get(i));
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
            data.addField((Field) compileField(data, field, null));
        }
        return data;
    }

}
