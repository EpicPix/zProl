package ga.epicpix.zprol.compiled.precompiled;

import java.util.ArrayList;
import java.util.HashMap;

public class PreCompiledData {

    public ArrayList<String> using = new ArrayList<>();
    public String namespace;

    public ArrayList<PreFunction> functions = new ArrayList<>();
    public HashMap<String, String> typedef = new HashMap<>();
    public HashMap<String, PreStructure> structures = new HashMap<>();


}
