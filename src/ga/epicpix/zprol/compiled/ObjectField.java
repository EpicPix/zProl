package ga.epicpix.zprol.compiled;

import java.util.ArrayList;

public class ObjectField {

    public String name;
    public Type type;
    public ArrayList<Flag> flags;

    public ObjectField(String name, Type type, ArrayList<Flag> flags) {
        this.name = name;
        this.type = type;
        this.flags = flags;
    }

}
