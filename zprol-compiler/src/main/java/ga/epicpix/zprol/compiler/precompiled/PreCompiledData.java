package ga.epicpix.zprol.compiler.precompiled;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.tree.NamespaceIdentifierTree;

import java.util.ArrayList;

public class PreCompiledData {

    public DataParser parser;
    public String sourceFile;

    public final ArrayList<String> using = new ArrayList<>();
    public String namespace;

    public final ArrayList<PreFunction> functions = new ArrayList<>();
    public final ArrayList<PreField> fields = new ArrayList<>();
    public final ArrayList<PreClass> classes = new ArrayList<>();

}
