package ga.epicpix.zprol.generators;

import ga.epicpix.zprol.compiled.generated.GeneratedData;
import ga.epicpix.zprol.exceptions.NotImplementedException;

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

    public void generate(DataOutputStream out, GeneratedData generated) throws IOException {
        throw new NotImplementedException("Generating " + getGeneratorName() + " is not implemented yet");
    }

}
