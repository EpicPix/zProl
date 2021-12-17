package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.exceptions.NotImplementedException;
import java.util.ArrayList;

public class Object {

    public String name;
    public Type ext;
    public ArrayList<ObjectField> fields;
    public ArrayList<Function> functions;

    public Object(String name, Type ext, ArrayList<ObjectField> fields, ArrayList<Function> functions) {
        this.name = name;
        this.ext = ext;
        this.fields = fields;
        this.functions = functions;
        throw new NotImplementedException("Not implemented yet");
    }

}
