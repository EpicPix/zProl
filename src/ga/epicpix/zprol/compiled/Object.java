package ga.epicpix.zprol.compiled;

import java.util.ArrayList;

public class Object {

    public String name;
    public Type ext;
    public ArrayList<ObjectField> fields;
    public ArrayList<Function> functions;

    public Object(String name, Type ext, ArrayList<ObjectField> fields, ArrayList<Function> functions) {
        if(!(ext.type == Types.NONE || ext instanceof TypeObject)) {
            throw new IllegalArgumentException("Type is not instanceof an object");
        }
        this.name = name;
        this.ext = ext;
        this.fields = fields;
        this.functions = functions;
    }

}
