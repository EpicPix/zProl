package ga.epicpix.zprol.compiled.precompiled;

import java.util.ArrayList;
import java.util.HashMap;

public class PreCompiledData {

    public HashMap<String, String> imported = new HashMap<>();
    public String exportName;

    public ArrayList<PreFunction> functions = new ArrayList<>();
    public HashMap<String, String> typedef = new HashMap<>();
    public HashMap<String, PreStructure> structures = new HashMap<>();


}
