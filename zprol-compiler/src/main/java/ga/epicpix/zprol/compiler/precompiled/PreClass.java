package ga.epicpix.zprol.compiler.precompiled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreClass {

    public String namespace;
    public String name;

    public List<PreField> fields = new ArrayList<>();
    public List<PreFunction> methods = new ArrayList<>();

    public PreClass() {}
    public PreClass(String namespace, String name, PreField[] fields, PreFunction[] methods) {
        this.namespace = namespace;
        this.name = name;
        this.fields = Arrays.asList(fields);
        this.methods = Arrays.asList(methods);
    }

}
