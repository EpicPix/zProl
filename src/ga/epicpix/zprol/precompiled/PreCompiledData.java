package ga.epicpix.zprol.precompiled;

import java.util.ArrayList;
import java.util.HashMap;

public class PreCompiledData {

    public String sourceFile;

    public ArrayList<String> using = new ArrayList<>();
    public String namespace;

    public ArrayList<PreFunction> functions = new ArrayList<>();
    public HashMap<String, String> typedef = new HashMap<>();


}
