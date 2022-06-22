package ga.epicpix.zprol.compiler.precompiled;

import java.util.ArrayList;

public class PreField {

    public PreField() {}
    public PreField(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String type;
    public String name;
    public final ArrayList<PreFieldModifiers> modifiers = new ArrayList<>();

}
