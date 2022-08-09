package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tree.IExpression;
import ga.epicpix.zprol.parser.tree.ITree;
import ga.epicpix.zprol.structures.IBytecodeStorage;
import ga.epicpix.zprol.types.*;

import java.util.ArrayDeque;

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
        var types = new ArrayDeque<Type>();
        generateInstructionsFromExpression(expression, null, types, data, scope, bytecode, false, parser);
        var expressionType = types.pop();
        if(!(expressionType instanceof PrimitiveType)) {
            throw new TokenLocatedException("Expected a primitive number", location, parser);
        }
        var arrayData = arrayType.type;
        if(arrayData instanceof PrimitiveType primitive) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_array"));
        else if(arrayData instanceof BooleanType) bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_array"));
        else if(arrayData instanceof ArrayType) bytecode.pushInstruction(getConstructedInstruction("aload_array"));
        else if(arrayData instanceof ClassType) bytecode.pushInstruction(getConstructedInstruction("aload_array"));
        else throw new TokenLocatedException("Unsupported type " + arrayData.getClass().getSimpleName(), location, parser);
        return arrayType.type;
    }

    public Type storeArray(ArrayType arrayType, CompiledData data, FunctionCodeScope scope, IBytecodeStorage bytecode, DataParser parser) {
        var types = new ArrayDeque<Type>();
        generateInstructionsFromExpression(expression, null, types, data, scope, bytecode, false, parser);
        var expressionType = types.pop();
        if(!(expressionType instanceof PrimitiveType)) {
            throw new TokenLocatedException("Expected a primitive number", location, parser);
        }
        var arrayData = arrayType.type;
        if(arrayData instanceof PrimitiveType primitive) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_array"));
        else if(arrayData instanceof BooleanType) bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_array"));
        else if(arrayData instanceof ArrayType) bytecode.pushInstruction(getConstructedInstruction("astore_array"));
        else if(arrayData instanceof ClassType) bytecode.pushInstruction(getConstructedInstruction("astore_array"));
        else throw new TokenLocatedException("Unsupported type " + arrayData.getClass().getSimpleName(), location, parser);
        return arrayType.type;
    }

}
