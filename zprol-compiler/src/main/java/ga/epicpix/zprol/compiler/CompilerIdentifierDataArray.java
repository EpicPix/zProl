package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tree.IExpression;
import ga.epicpix.zprol.parser.tree.ITree;
import ga.epicpix.zprol.structures.IBytecodeStorage;
import ga.epicpix.zprol.types.*;

import java.util.ArrayDeque;

import static ga.epicpix.zprol.compiler.Compiler.doCast;
import static ga.epicpix.zprol.compiler.Compiler.generateInstructionsFromExpression;
import static ga.epicpix.zprol.compiler.CompilerUtils.getConstructedInstruction;
import static ga.epicpix.zprol.compiler.CompilerUtils.getConstructedSizeInstruction;

public class CompilerIdentifierDataArray extends CompilerIdentifierData {

    public final IExpression expression;

    public CompilerIdentifierDataArray(ITree location, IExpression expression) {
        super(location);
        this.expression = expression;
    }

    public Type loadArray(ArrayType arrayType, CompiledData data, FunctionCodeScope scope, IBytecodeStorage bytecode, DataParser parser) {
        ArrayDeque<Type> types = new ArrayDeque<Type>();
        generateInstructionsFromExpression(expression, data.resolveType("uint64"), types, data, scope, bytecode, false, parser);
        types.push(doCast(types.pop(), data.resolveType("uint64"), false, bytecode, expression, parser));
        Type expressionType = types.pop();
        if(!(expressionType instanceof PrimitiveType)) {
            throw new TokenLocatedException("Expected a primitive number", location, parser);
        }
        Type arrayData = arrayType.type;
        if(arrayData instanceof PrimitiveType) bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) arrayData).getSize(), "load_array"));
        else if(arrayData instanceof BooleanType) bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_array"));
        else if(arrayData instanceof ArrayType) bytecode.pushInstruction(getConstructedInstruction("aload_array"));
        else if(arrayData instanceof ClassType) bytecode.pushInstruction(getConstructedInstruction("aload_array"));
        else throw new TokenLocatedException("Unsupported type " + arrayData.getClass().getSimpleName(), location, parser);
        return arrayType.type;
    }

    public Type storeArray(ArrayType arrayType, CompiledData data, FunctionCodeScope scope, IBytecodeStorage bytecode, DataParser parser) {
        ArrayDeque<Type> types = new ArrayDeque<Type>();
        generateInstructionsFromExpression(expression, data.resolveType("uint64"), types, data, scope, bytecode, false, parser);
        types.push(doCast(types.pop(), data.resolveType("uint64"), false, bytecode, expression, parser));
        Type expressionType = types.pop();
        if(!(expressionType instanceof PrimitiveType)) {
            throw new TokenLocatedException("Expected a primitive number", location, parser);
        }
        Type arrayData = arrayType.type;
        if(arrayData instanceof PrimitiveType) bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) arrayData).getSize(), "store_array"));
        else if(arrayData instanceof BooleanType) bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_array"));
        else if(arrayData instanceof ArrayType) bytecode.pushInstruction(getConstructedInstruction("astore_array"));
        else if(arrayData instanceof ClassType) bytecode.pushInstruction(getConstructedInstruction("astore_array"));
        else throw new TokenLocatedException("Unsupported type " + arrayData.getClass().getSimpleName(), location, parser);
        return arrayType.type;
    }

}
