package ga.epicpix.zprol.compiled;

public class TypeFunctionSignature extends Type {

    public Type returnType;
    public Type[] parameters;

    public TypeFunctionSignature(Type returnType, Type... parameters) {
        super(Types.FUNCTION_SIGNATURE);
        this.returnType = returnType;
        this.parameters = parameters;
    }

}
