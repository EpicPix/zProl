package ga.epicpix.zprol.compiled;

import java.util.Arrays;

public class FunctionSignature {

    public Type returnType;
    public Type[] parameters;

    public FunctionSignature(Type returnType, Type... parameters) {
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public boolean validateFunctionSignature(FunctionSignature cmp) {
        if(cmp.returnType == null || returnType == cmp.returnType) {
            if(cmp.parameters.length == parameters.length) {
                boolean success = true;
                for(int i = 0; i<cmp.parameters.length; i++) {
                    if(cmp.parameters[i] != parameters[i]) {
                        success = false;
                        break;
                    }
                }
                return success;
            }
        }
        return false;
    }

    public String toString() {
        return "FunctionSignature(returnType=" + returnType + ", parameters=" + Arrays.toString(parameters) + ')';
    }
}
