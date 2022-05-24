package ga.epicpix.zprol.compiler.precompiled;

import java.util.ArrayList;

public class PreCompiledData {

    public String sourceFile;

    public final ArrayList<String> using = new ArrayList<>();
    public String namespace;

    public final ArrayList<PreFunction> functions = new ArrayList<>();
    public final ArrayList<PreClass> classes = new ArrayList<>();

}
