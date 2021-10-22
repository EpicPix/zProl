package ga.epicpix.zprol.compiled;

public class TypeObject extends Type {

    public String name;

    public TypeObject(String name) {
        super(Types.OBJECT);
        this.name = name;
    }
}
