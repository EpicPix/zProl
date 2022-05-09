package ga.epicpix.zprol.generators;

import ga.epicpix.zpil.GeneratedData;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class Generator {

    public static final ArrayList<Generator> GENERATORS = new ArrayList<>();

    public static void initGenerators() {
        GENERATORS.add(new GeneratorAssemblyLinux64());
    }

    protected Generator() {}

    public abstract String getGeneratorCommandLine();
    public abstract String getGeneratorName();
    public abstract String getFileExtension();

    public abstract void generate(DataOutputStream out, GeneratedData generated) throws IOException;

}
