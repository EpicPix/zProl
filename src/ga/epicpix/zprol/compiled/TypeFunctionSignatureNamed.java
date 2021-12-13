package ga.epicpix.zprol.compiled;

public class TypeFunctionSignatureNamed extends Type {

    public Type returnType;
    public TypeNamed[] parameters;

    public TypeFunctionSignatureNamed(Type returnType, TypeNamed... parameters) {
        super(Types.FUNCTION_SIGNATURE);
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public TypeFunctionSignature getNormalSignature() {
        Type[] parameters = new Type[this.parameters.length];
        for(int i = 0; i<this.parameters.length; i++) {
            parameters[i] = this.parameters[i].type;
        }
        return new TypeFunctionSignature(returnType, parameters);
    }

}
