package ga.epicpix.zprol.compiled;

public record FunctionSignature(PrimitiveType returnType, PrimitiveType... parameters) {

    public boolean validateFunctionSignature(FunctionSignature cmp) {
        if (cmp.returnType == null || returnType == cmp.returnType) {
            if (cmp.parameters.length == parameters.length) {
                boolean success = true;
                for (int i = 0; i < cmp.parameters.length; i++) {
                    if (cmp.parameters[i] != parameters[i]) {
                        success = false;
                        break;
                    }
                }
                return success;
            }
        }
        return false;
    }

}
