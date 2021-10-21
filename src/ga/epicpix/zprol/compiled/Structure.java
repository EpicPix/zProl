package ga.epicpix.zprol.compiled;

import java.util.ArrayList;

public class Structure {

    public String name;
    public ArrayList<StructureField> fields;

    public Structure(String name, ArrayList<StructureField> fields) {
        this.name = name;
        this.fields = fields;
    }

}
