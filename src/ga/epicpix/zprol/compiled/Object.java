package ga.epicpix.zprol.compiled;

import java.util.ArrayList;

public class Object {

    public String name;
    public Type ext;
    public ArrayList<ObjectField> fields;

    public Object(String name, Type ext, ArrayList<ObjectField> fields) {
        this.name = name;
        this.ext = ext;
        this.fields = fields;
    }

}
