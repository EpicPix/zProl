package ga.epicpix.zprol.compiled;

public class TypeFunctionSignatureNamed extends Type {

    public Type returnType;
    public TypeNamed[] parameters;

    public TypeFunctionSignatureNamed(Type returnType, TypeNamed... parameters) {
        super(Types.FUNCTION_SIGNATURE);
        this.returnType = returnType;
        this.parameters = parameters;
    }

}
