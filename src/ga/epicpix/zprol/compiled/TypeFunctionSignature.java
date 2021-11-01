package ga.epicpix.zprol.compiled;

public class TypeFunctionSignature extends Type {

    public Type returnType;
    public Type[] parameters;

    public TypeFunctionSignature(Type returnType, Type... parameters) {
        super(Types.FUNCTION_SIGNATURE);
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public boolean validateFunctionSignature(TypeFunctionSignature cmp) {
        if(cmp.returnType == null || returnType.type == cmp.returnType.type) {
            if(cmp.parameters.length == parameters.length) {
                boolean success = true;
                for(int i = 0; i<cmp.parameters.length; i++) {
                    if((cmp.parameters[i].type != parameters[i].type) && !(parameters[i].type.isNumberType() && cmp.parameters[i].type == Types.NUMBER)) {
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
