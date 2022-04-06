package ga.epicpix.zprol.precompiled;

import java.util.ArrayList;
import java.util.HashMap;

public class PreCompiledData {

    public String sourceFile;

    public final ArrayList<String> using = new ArrayList<>();
    public String namespace;

    public final ArrayList<PreFunction> functions = new ArrayList<>();

}
