package ga.epicpix.zprol.compiler;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.compiled.locals.LocalVariable;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.precompiled.*;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.OperatorExpression;
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
            localsManager.defineLocalVariable(names[i], sig.parameters[i]);
        }
        boolean hasReturned = parseFunctionCode(data, tokens, sig, storage, new FunctionCodeScope(localsManager, clazz), parser);
        if(!hasReturned) {
            if(!(sig.returnType instanceof VoidType)) {
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
            if(token instanceof ReturnStatementTree) {
                ReturnStatementTree retStatement = (ReturnStatementTree) token;
                if(!(sig.returnType instanceof VoidType)) {
                    if(retStatement.expression == null) {
                        throw new TokenLocatedException("Function is not void, expected a return value", token, parser);
                    }
                    ArrayDeque<Type> types = new ArrayDeque<Type>();
                    generateInstructionsFromExpression(retStatement.expression, sig.returnType, types, data, scope, bytecode, false, parser);
                }
                if(sig.returnType instanceof VoidType) {
                    if(retStatement.expression != null) {
                        throw new TokenLocatedException("Function is void, expected no value", token, parser);
                    }
                }
                if(sig.returnType instanceof PrimitiveType) {
                    PrimitiveType primitive = (PrimitiveType) sig.returnType;
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "return"));
                }else if(sig.returnType instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "return"));
                }else if(!(sig.returnType instanceof VoidType)) {
                    bytecode.pushInstruction(getConstructedInstruction("areturn"));
                }
                hasReturned = true;
                break;
            }else if(token instanceof AccessorStatementTree) {
                AccessorStatementTree accessorStatement = (AccessorStatementTree) token;
                generateInstructionsFromExpression(accessorStatement.accessor, null, new ArrayDeque<>(), data, scope, bytecode, true, parser);
            }else if(token instanceof CreateAssignmentStatementTree ) {
                CreateAssignmentStatementTree createAssignmentStatement = (CreateAssignmentStatementTree) token;
                Type type = data.resolveType(createAssignmentStatement.type.toString());
                if(type instanceof VoidType) {
                    throw new TokenLocatedException("Cannot create a variable with void type", token, parser);
                }
                String name = createAssignmentStatement.name.toStringRaw();
                IExpression expression = createAssignmentStatement.expression;
                ArrayDeque<Type> types = new ArrayDeque<Type>();
                generateInstructionsFromExpression(expression, type, types, data, scope, bytecode, false, parser);
                Type rType = types.pop();
                doCast(rType, type, false, bytecode, expression, parser);
                LocalVariable local = scope.localsManager.defineLocalVariable(name, type);
                if(type instanceof PrimitiveType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) type).getSize(), "store_local", local.index));
                }else {
                    bytecode.pushInstruction(getConstructedInstruction("astore_local", local.index));
                }
            }else if(token instanceof AssignmentStatementTree) {
                AssignmentStatementTree assignmentStatement = (AssignmentStatementTree) token;
                IBytecodeStorage tempStorage = createStorage();
                CompilerIdentifierData[] accessorData = accessorToData(assignmentStatement.accessor);
                ArrayDeque<Type> types = new ArrayDeque<>();
                for (int i = 0; i < accessorData.length - 1; i++) {
                    CompilerIdentifierData v = accessorData[i];
                    if(v instanceof CompilerIdentifierDataFunction) {
                        PreClass context = getClassContext(i, scope.thisClass, types, data, v.location, parser);
                        doFunctionCall((CompilerIdentifierDataFunction) v, context, data, null, types, scope, tempStorage, false, i == 0, parser);
                    }else if(v instanceof CompilerIdentifierDataField) {
                        CompilerIdentifierDataField field = (CompilerIdentifierDataField) v;
                        PreClass context = getClassContext(i, scope.thisClass, types, data, v.location, parser);
                        Type ret = field.loadField(context, scope.localsManager, tempStorage, data, i == 0);
                        if(ret == null) {
                            throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location, parser);
                        }
                        types.push(ret);
                    }else if(v instanceof CompilerIdentifierDataArray) {
                        CompilerIdentifierDataArray array = (CompilerIdentifierDataArray) v;
                        Type currentType = types.pop();
                        if(!(currentType instanceof ArrayType)) throw new TokenLocatedException("Expected an array type", v.location, parser);
                        types.push(array.loadArray((ArrayType) currentType, data, scope, tempStorage, parser));
                    }else {
                        throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location, parser);
                    }
                }
                CompilerIdentifierData v = accessorData[accessorData.length - 1];

                Type expectedType;

                if(v instanceof CompilerIdentifierDataField) {
                    CompilerIdentifierDataField field = (CompilerIdentifierDataField) v;
                    PreClass context = getClassContext(accessorData.length - 1, scope.thisClass, types, data, v.location, parser);
                    expectedType = field.storeField(context, scope.localsManager, types.size() != 0 ? types.pop() : null, tempStorage, data, accessorData.length == 1, parser);
                    if(expectedType == null) {
                        throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location, parser);
                    }
                }else if(v instanceof CompilerIdentifierDataArray) {
                    CompilerIdentifierDataArray array = (CompilerIdentifierDataArray) v;
                    Type currentType = types.pop();
                    if(!(currentType instanceof ArrayType)) throw new TokenLocatedException("Expected an array type", v.location, parser);
                    expectedType = array.storeArray((ArrayType) currentType, data, scope, tempStorage, parser);
                }else {
                    throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location, parser);
                }

                generateInstructionsFromExpression(assignmentStatement.expression, expectedType, types, data, scope, bytecode, parser);
                doCast(types.pop(), expectedType, false, bytecode, assignmentStatement.expression, parser);
                for(IBytecodeInstruction instr : tempStorage.getInstructions()) bytecode.pushInstruction(instr);
            }else if(token instanceof IfStatementTree) {
                IfStatementTree ifStatement = (IfStatementTree) token;
                ArrayDeque<Type> types = new ArrayDeque<Type>();
                generateInstructionsFromExpression(ifStatement.expression, null, types, data, scope, bytecode, parser);
                Type ret = types.pop();
                if(!(ret instanceof BooleanType)) {
                    throw new TokenLocatedException("Expected boolean expression", ifStatement.expression, parser);
                }
                ArrayList<IStatement> statements = new ArrayList<>(ifStatement.code.statements);

                int preInstr = bytecode.getInstructions().size();
                bytecode.pushInstruction(getConstructedInstruction("int"));
                parseFunctionCode(data, new SeekIterator<>(statements), sig, bytecode, new FunctionCodeScope(FunctionCodeScope.ScopeType.IF, scope), parser);
                int postInstr = bytecode.getInstructions().size();
                if(ifStatement.elseStatement != null) {
                    ArrayList<IStatement> elseStatements = new ArrayList<>(ifStatement.elseStatement.code.statements);
                    parseFunctionCode(data, new SeekIterator<>(elseStatements), sig, bytecode, new FunctionCodeScope(FunctionCodeScope.ScopeType.ELSE, scope), parser);
                    bytecode.pushInstruction(postInstr, getConstructedInstruction("jmp", bytecode.getInstructions().size()-postInstr+1));
                    postInstr++;
                }
                bytecode.replaceInstruction(preInstr, getConstructedInstruction("neqjmp", postInstr-preInstr));
            }else if(token instanceof WhileStatementTree) {
                WhileStatementTree whileStatement = (WhileStatementTree) token;
                ArrayDeque<Type> types = new ArrayDeque<Type>();
                int preInstr = bytecode.getInstructionsLength();
                generateInstructionsFromExpression(whileStatement.expression, null, types, data, scope, bytecode, false, parser);
                Type ret = types.pop();
                if(!(ret instanceof BooleanType)) {
                    throw new TokenLocatedException("Expected boolean expression", whileStatement.expression, parser);
                }
                int addInstr = bytecode.getInstructionsLength();
                bytecode.pushInstruction(getConstructedInstruction("int"));

                ArrayList<IStatement> statements = new ArrayList<>(whileStatement.code.statements);
                FunctionCodeScope fscope = new FunctionCodeScope(FunctionCodeScope.ScopeType.WHILE, scope);
                parseFunctionCode(data, new SeekIterator<>(statements), sig, bytecode, fscope, parser);
                int postInstr = bytecode.getInstructionsLength();

                bytecode.pushInstruction(getConstructedInstruction("jmp", preInstr-postInstr));
                bytecode.replaceInstruction(addInstr, getConstructedInstruction("neqjmp", postInstr-addInstr+1));

                for(int location : fscope.breakLocations) bytecode.replaceInstruction(location, getConstructedInstruction("jmp", postInstr-location+1));
                for(int location : fscope.continueLocations) bytecode.replaceInstruction(location, getConstructedInstruction("jmp", preInstr- location));
            }else if(token instanceof BreakStatementTree) {
                BreakStatementTree breakStatement = (BreakStatementTree) token;
                FunctionCodeScope whileLoc = scope;
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
            }else if(token instanceof ContinueStatementTree) {
                ContinueStatementTree continueStatement = (ContinueStatementTree) token;
                FunctionCodeScope whileLoc = scope;
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
            if(prob instanceof PrimitiveType) {
                PrimitiveType primitive = (PrimitiveType) prob;
                if(primitive.getSize() != 0) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "pop"));
                } else {
                    types.push(primitive);
                }
            } else if(prob instanceof BooleanType) {
                bytecode.pushInstruction(getConstructedSizeInstruction(8, "pop"));
            } else if(!(prob instanceof VoidType)) {
                bytecode.pushInstruction(getConstructedInstruction("apop"));
            }
        }
    }

    public static void generateInstructionsFromExpression(IExpression token, Type expectedType, ArrayDeque<Type> types, CompiledData data, FunctionCodeScope scope, IBytecodeStorage bytecode, DataParser parser) {
        if(token instanceof OperatorExpressionTree) {
            types.push(runOperator((OperatorExpressionTree) token, expectedType, data, scope, types, bytecode, parser));
        }else if(token instanceof LiteralTree) {
            LiteralTree literal = (LiteralTree) token;
            if(literal.type == LiteralType.INTEGER) {
                BigInteger number = (BigInteger) literal.value;
                if(expectedType instanceof PrimitiveType) {
                    PrimitiveType primitive = (PrimitiveType) expectedType;
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "push", number));
                    types.push(primitive);
                } else if(expectedType == null) {
                    if((long)number.intValue() == number.longValue()) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(4, "push", number));
                        types.push(data.resolveType("int32"));
                    }else {
                        bytecode.pushInstruction(getConstructedSizeInstruction(8, "push", number));
                        types.push(data.resolveType("int64"));
                    }
                } else {
                    throw new TokenLocatedException("Cannot infer size of number from a non-primitive type (" + expectedType.getName() + ")", token, parser);
                }
            }else if(literal.type == LiteralType.BOOLEAN) {
                if(literal.value == Boolean.TRUE) {
                    bytecode.pushInstruction(getConstructedInstruction("push_true"));
                }else {
                    bytecode.pushInstruction(getConstructedInstruction("push_false"));
                }
                types.push(new BooleanType());
            }else if(literal.type == LiteralType.STRING) {
                bytecode.pushInstruction(getConstructedInstruction("push_string", literal.value));
                types.push(data.resolveType("zprol.lang.String"));
            }else if(literal.type == LiteralType.NULL) {
                bytecode.pushInstruction(getConstructedInstruction("null"));
                types.push(new NullType());
            }else {
                throw new TokenLocatedException("Unknown literal", literal, parser);
            }
        }else if(token instanceof NegateTree) {
            NegateTree neg = (NegateTree) token;
            generateInstructionsFromExpression(neg.expression, expectedType, types, data, scope, bytecode, parser);
            Type type = types.peek();
            if(!(type instanceof PrimitiveType)) {
                throw new TokenLocatedException("Expected a primitive type, got " + type.normalName(), neg, parser);
            }else if(((PrimitiveType) type).unsigned) {
                throw new TokenLocatedException("Expected a signed primitive type, got " + type.normalName(), neg, parser);
            }
            bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) type).getSize(), "neg"));
        }else if(token instanceof CastTree) {
            CastTree cast = (CastTree) token;
            TypeTree op = cast.type;
            boolean isHardCast = cast.hardCast;
            Type castType = data.resolveType(op.toString());
            generateInstructionsFromExpression(cast.value, null, types, data, scope, bytecode, parser);
            Type from = types.pop();
            if(isHardCast) {
                // this will not check sizes of primitive types, this is unsafe
                types.push(castType);
                return;
            }
            types.push(doCast(from, castType, true, bytecode, cast.value, parser));
        }else if(token instanceof AccessorTree) {
            CompilerIdentifierData[] accessorData = accessorToData((AccessorTree) token);
            for(int i = 0; i < accessorData.length; i++) {
                CompilerIdentifierData v = accessorData[i];
                if(v instanceof CompilerIdentifierDataFunction) {
                    PreClass context = getClassContext(i, scope.thisClass, types, data, v.location, parser);
                    doFunctionCall((CompilerIdentifierDataFunction) v, context, data, null, types, scope, bytecode, false, i == 0, parser);
                } else if(v instanceof CompilerIdentifierDataField) {
                    CompilerIdentifierDataField field = (CompilerIdentifierDataField) v;
                    PreClass context = getClassContext(i, scope.thisClass, types, data, v.location, parser);
                    Type ret = field.loadField(context, scope.localsManager, bytecode, data, i == 0);
                    if(ret == null) {
                        throw new TokenLocatedException("Unknown field " + field.getFieldName(), field.location, parser);
                    }
                    types.push(ret);
                } else if(v instanceof CompilerIdentifierDataArray) {
                    Type currentType = types.pop();
                    if(!(currentType instanceof ArrayType))
                        throw new TokenLocatedException("Expected an array type", v.location, parser);
                    types.push(((CompilerIdentifierDataArray) v).loadArray((ArrayType) currentType, data, scope, bytecode, parser));
                } else {
                    throw new TokenLocatedException("Unknown compiler identifier data " + v.getClass().getSimpleName(), v.location, parser);
                }
            }
        }else {
            throw new TokenLocatedException("Unknown expression", token, parser);
        }
    }

    public static Type runOperator(OperatorExpressionTree token, Type expectedType, CompiledData data, FunctionCodeScope scope, ArrayDeque<Type> types, IBytecodeStorage bytecode, DataParser parser) {
        generateInstructionsFromExpression(token.left, expectedType, types, data, scope, bytecode, false, parser);
        Type arg1 = types.pop();
        int amtOperators = token.operators.length;
        for (int i = 0; i < amtOperators; i++) {
            OperatorExpression op = token.operators[i];
            IBytecodeStorage arg2Storage = createStorage();
            generateInstructionsFromExpression(op.expression, expectedType, types, data, scope, arg2Storage, false, parser);
            Type arg2 = types.pop();

            arg1 = runOperator(arg1, arg2, op.operator, token, bytecode, arg2Storage, parser);
        }
        return arg1;
    }

    public static Type runOperator(Type arg1, Type arg2, LexerToken operator, ITree location, IBytecodeStorage bytecode, IBytecodeStorage arg2bytecode, DataParser parser) {
        String operatorName = operator.toStringRaw();

        if(!(arg1 instanceof PrimitiveType) || !(arg2 instanceof PrimitiveType)) {
            if((arg1 instanceof ClassType || arg1 instanceof NullType) && (arg2 instanceof ClassType || arg2 instanceof NullType)) {
                if(operatorName.equals("==")) {
                    for(IBytecodeInstruction instr : arg2bytecode.getInstructions()) {
                        bytecode.pushInstruction(instr);
                    }
                    bytecode.pushInstruction(getConstructedInstruction("aeq"));
                    return new BooleanType();
                }else if(operatorName.equals("!=")) {
                    for(IBytecodeInstruction instr : arg2bytecode.getInstructions()) {
                        bytecode.pushInstruction(instr);
                    }
                    bytecode.pushInstruction(getConstructedInstruction("aneq"));
                    return new BooleanType();
                }
            }
            if(arg1 instanceof BooleanType && arg2 instanceof BooleanType) {
                if(operatorName.equals("==")) {
                    for(IBytecodeInstruction instr : arg2bytecode.getInstructions()) {
                        bytecode.pushInstruction(instr);
                    }
                    bytecode.pushInstruction(getConstructedInstruction("leq"));
                    return new BooleanType();
                }else if(operatorName.equals("!=")) {
                    for(IBytecodeInstruction instr : arg2bytecode.getInstructions()) {
                        bytecode.pushInstruction(instr);
                    }
                    bytecode.pushInstruction(getConstructedInstruction("lneq"));
                    return new BooleanType();
                }
            }
            throw new TokenLocatedException("Cannot perform math operation on types " + arg1.getName() + " <-> " + arg2.getName(), location, parser);
        }
        PrimitiveType prim1 = (PrimitiveType) arg1;
        PrimitiveType prim2 = (PrimitiveType) arg2;
        PrimitiveType bigger = prim1.getSize() > prim2.getSize() ? prim1 : prim2;
        PrimitiveType smaller = prim1.getSize() <= prim2.getSize() ? prim1 : prim2;
        if(prim2 != bigger) {
            for(IBytecodeInstruction instr : arg2bytecode.getInstructions()) {
                bytecode.pushInstruction(instr);
            }
        }
        Type got = doCast(smaller, bigger, false, bytecode, location, parser);
        if(prim2 == bigger) {
            for(IBytecodeInstruction instr : arg2bytecode.getInstructions()) {
                bytecode.pushInstruction(instr);
            }
        }
        PrimitiveType primitive = (PrimitiveType) got;

        switch(operatorName) {
            case "+": bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "add"));break;
            case "-": bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "sub"));break;
            case "*":
                if(primitive.isUnsigned())
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "mulu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "mul"));
                break;
            case "/":
                if(primitive.isUnsigned())
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "divu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "div"));
                break;
            case "%":
                if(primitive.isUnsigned())
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "modu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "mod"));
                break;
            case "&": bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "and"));break;
            case "<<": bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "shift_left"));break;
            case ">>": bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "shift_right"));break;
            case "|": bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "or"));break;
            case "==":
                bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "eq"));
                got = new BooleanType();
                break;
            case "!=":
                bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "neq"));
                got = new BooleanType();
                break;
            case "<":
                if(primitive.isUnsigned())
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "ltu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "lt"));
                got = new BooleanType();
                break;
            case "<=":
                if(primitive.isUnsigned())
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "leu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "le"));
                got = new BooleanType();
                break;
            case ">":
                if(primitive.isUnsigned())
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "gtu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "gt"));
                got = new BooleanType();
                break;
            case ">=":
                if(primitive.isUnsigned())
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "geu"));
                else bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "ge"));
                got = new BooleanType();
                break;
            default: throw new TokenLocatedException("Unknown operator '" + operatorName + "'", operator);
        }
        return got;
    }

    public static Type doCast(Type from, Type to, boolean explicit, IBytecodeStorage bytecode, ITree location, DataParser parser) {
        if(from instanceof NullType && to instanceof ClassType) {
            return to;
        }
        if(!(from instanceof PrimitiveType) || !(to instanceof PrimitiveType)) {
            if(!from.equals(to)) {
                throw new TokenLocatedException("Unsupported cast from " + from.getName() + " to " + to.getName(), location, parser);
            }else {
                return to;
            }
        }
        PrimitiveType primitiveFrom = (PrimitiveType) from;
        PrimitiveType primitiveTo = (PrimitiveType) to;
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
        ArrayList<LookupFunction> possibleFunctions = func.lookupFunction(data, classContext, searchPublic);
        if(possibleFunctions.size() == 0)
            throw new TokenLocatedException("Function not defined: " + func.getFunctionName(), func.location, parser);

        if(possibleFunctions.size() != 1) {

            IBytecodeStorage garbage = createStorage();
            ArrayList<Type> argTypes = new ArrayList<Type>();

            for(IExpression arg : func.arguments) {
                generateInstructionsFromExpression(arg, null, types, data, scope, garbage, parser);
                argTypes.add(types.pop());
            }

            ArrayList<String> candidates = new ArrayList<String>();
            int closestScore = 0;
            ArrayList<LookupFunction> closest = new ArrayList<LookupFunction>();
            for(LookupFunction a : possibleFunctions) {
                boolean matches = true;
                boolean nPrimMatches = true;
                int score = 0;
                if(a.func != null) {
                    for(int i = 0; i < a.func.parameters.size(); i++) {
                        Type t = data.resolveType(a.func.parameters.get(i).type);
                        if(!t.equals(argTypes.get(i))) {
                            if(argTypes.get(i) instanceof PrimitiveType && t instanceof PrimitiveType) {
                                score++;
                            } else if(!(argTypes.get(i) instanceof PrimitiveType) || !(t instanceof PrimitiveType)) {
                                nPrimMatches = false;
                            }
                            matches = false;
                        } else {
                            score++;
                        }
                    }
                }else {
                    for(int i = 0; i < a.genFunc.signature.parameters.length; i++) {
                        Type t = a.genFunc.signature.parameters[i];
                        if(!t.equals(argTypes.get(i))) {
                            if(argTypes.get(i) instanceof PrimitiveType && t instanceof PrimitiveType) {
                                score++;
                            } else if(!(argTypes.get(i) instanceof PrimitiveType) || !(t instanceof PrimitiveType)) {
                                nPrimMatches = false;
                            }
                            matches = false;
                        } else {
                            score++;
                        }
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
                if(a.func != null) {
                    candidates.add("(" + a.func.parameters.stream().map(b -> data.resolveType(b.type).getName()).collect(Collectors.joining(", ")) + ")");
                }else {
                    candidates.add("(" + Arrays.stream(a.genFunc.signature.parameters).map(Type::getName).collect(Collectors.joining(", ")) + ")");
                }
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
                                .map(a -> {
                                    if(a.func != null) {
                                        return "(" + a.func.parameters.stream().map(b -> data.resolveType(b.type).getName()).collect(Collectors.joining(", ")) + ")";
                                    }else {
                                        return "(" + Arrays.stream(a.genFunc.signature.parameters).map(Type::getName).collect(Collectors.joining(", ")) + ")";
                                    }
                                })
                                .collect(Collectors.joining("\n")
                                ), func.location, parser);
                }
                throw new TokenLocatedException("Cannot find overload for arguments (" + argTypes.stream().map(Type::getName).collect(Collectors.joining(", ")) + ")\nCandidates are:\n" + String.join("\n", candidates), func.location, parser);
            }
        }

        LookupFunction lfunc = possibleFunctions.get(0);
        Type returnType;
        Type[] parameters;
        FunctionSignature signature;
        EnumSet<FunctionModifiers> fMods = EnumSet.noneOf(FunctionModifiers.class);
        if(lfunc.func != null) {
            PreFunction f = lfunc.func;
            fMods.addAll(f.modifiers);
            returnType = data.resolveType(f.returnType);
            parameters = new Type[f.parameters.size()];
            for(int i = 0; i < f.parameters.size(); i++) {
                parameters[i] = data.resolveType(f.parameters.get(i).type);
            }
            signature = new FunctionSignature(returnType, parameters);
        }else {
            Function f = lfunc.genFunc;
            fMods.addAll(f.modifiers);
            returnType = f.signature.returnType;
            parameters = f.signature.parameters;
            signature = f.signature;
        }

        if(lfunc.isClassMethod && searchPublic) {
            bytecode.pushInstruction(getConstructedInstruction("aload_local", scope.localsManager.getLocalVariable("this").index));
        }

        for(int i = 0; i<func.arguments.length; i++) {
            generateInstructionsFromExpression(func.arguments[i], parameters[i], types, data, scope, bytecode, parser);
            doCast(types.pop(), signature.parameters[i], false, bytecode, func.arguments[i], parser);
        }

        if(lfunc.isClassMethod) {
            bytecode.pushInstruction(getConstructedInstruction("invoke_class", new Method(classContext.namespace, fMods, classContext.name, func.getFunctionName(), signature, null)));
        }else {
            bytecode.pushInstruction(getConstructedInstruction("invoke", new Function(lfunc.namespace, fMods, func.getFunctionName(), signature, null)));
        }

        if(discardValue && returnType instanceof PrimitiveType) {
            bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) returnType).getSize(), "pop"));
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
        Type returnType = data.resolveType(function.returnType);
        Type[] parameters = new Type[function.parameters.size()];
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
        Type returnType = data.resolveType(function.returnType);
        Type[] parameters = new Type[function.parameters.size()];
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
        ClassField[] fields = new ClassField[clazz.fields.size()];
        for (int i = 0; i < clazz.fields.size(); i++) {
            fields[i] = (ClassField) compileField(data, clazz.fields.get(i), clazz, parser);
        }

        Method[] methods = new Method[clazz.methods.size()];
        for (int i = 0; i < clazz.methods.size(); i++) {
            methods[i] = compileMethod(data, clazz, clazz.methods.get(i), parser);
        }
        data.addClass(new Class(data.namespace, clazz.name, fields, methods));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other, ArrayList<GeneratedData> generated, DataParser parser) throws UnknownTypeException {
        CompiledData data = new CompiledData(preCompiled.namespace, preCompiled.using.toArray(new String[0]));

        data.using(preCompiled);
        for(PreCompiledData o : other) if(preCompiled.using.contains(o.namespace)) data.using(o);
        for(GeneratedData o : generated) data.using(o);

        for(PreClass clazz : preCompiled.classes) compileClass(data, clazz, parser);
        for(PreFunction function : preCompiled.functions) compileFunction(data, function, parser);
        for(PreField field : preCompiled.fields) {
            data.addField((Field) compileField(data, field, null, parser));
        }
        return data;
    }

}
