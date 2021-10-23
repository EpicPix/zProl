package ga.epicpix.zprol.compiled;

public class TypeNamed extends Type {

    public Type type;
    public String name;

    public TypeNamed(Type type, String name) {
        super(Types.NAMED);
        this.type = type;
        this.name = name;
    }

}
