package ga.epicpix.zprol.compiler.precompiled;

import java.util.ArrayList;
import java.util.List;

public class PreClass {

    public String namespace;
    public String name;

    public List<PreField> fields = new ArrayList<>();
    public List<PreFunction> methods = new ArrayList<>();

}
