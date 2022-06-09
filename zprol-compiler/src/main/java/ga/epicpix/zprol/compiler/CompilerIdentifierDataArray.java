package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.structures.IBytecodeStorage;
import ga.epicpix.zprol.types.*;

import java.util.ArrayDeque;

import static ga.epicpix.zprol.compiler.Compiler.generateInstructionsFromExpression;
import static ga.epicpix.zprol.compiler.CompilerUtils.getConstructedInstruction;
import static ga.epicpix.zprol.compiler.CompilerUtils.getConstructedSizeInstruction;

public class CompilerIdentifierDataArray extends CompilerIdentifierData {

    public final NamedToken expression;

    public CompilerIdentifierDataArray(Token location, NamedToken expression) {
        super(location);
        this.expression = expression;
    }

    public Type loadArray(ArrayType arrayType, PreClass thisClass, CompiledData data, LocalScopeManager localsManager, IBytecodeStorage bytecode) {
        var types = new ArrayDeque<Type>();
        generateInstructionsFromExpression(expression, thisClass, null, types, data, localsManager, bytecode, false);
        var expressionType = types.pop();
        if(!(expressionType instanceof PrimitiveType)) {
            throw new TokenLocatedException("Expected a primitive number", location);
        }
        var arrayData = arrayType.type;
        if(arrayData instanceof PrimitiveType primitive) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_array"));
        else if(arrayData instanceof BooleanType) bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_array"));
        else if(arrayData instanceof ArrayType) bytecode.pushInstruction(getConstructedInstruction("aload_array"));
        else if(arrayData instanceof ClassType) bytecode.pushInstruction(getConstructedInstruction("aload_array"));
        else throw new TokenLocatedException("Unsupported type " + arrayData.getClass().getSimpleName(), location);
        return arrayType.type;
    }

    public Type storeArray(ArrayType arrayType, PreClass thisClass, CompiledData data, LocalScopeManager localsManager, IBytecodeStorage bytecode) {
        var types = new ArrayDeque<Type>();
        generateInstructionsFromExpression(expression, thisClass, null, types, data, localsManager, bytecode, false);
        var expressionType = types.pop();
        if(!(expressionType instanceof PrimitiveType)) {
            throw new TokenLocatedException("Expected a primitive number", location);
        }
        var arrayData = arrayType.type;
        if(arrayData instanceof PrimitiveType primitive) bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_array"));
        else if(arrayData instanceof BooleanType) bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_array"));
        else if(arrayData instanceof ArrayType) bytecode.pushInstruction(getConstructedInstruction("astore_array"));
        else if(arrayData instanceof ClassType) bytecode.pushInstruction(getConstructedInstruction("astore_array"));
        else throw new TokenLocatedException("Unsupported type " + arrayData.getClass().getSimpleName(), location);
        return arrayType.type;
    }

}
