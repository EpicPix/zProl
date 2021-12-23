package ga.epicpix.zprol.generators;

import ga.epicpix.zprol.exceptions.NotImplementedException;
import java.io.File;
import java.util.ArrayList;

public abstract class Generator {

    public static final ArrayList<Generator> GENERATORS = new ArrayList<>();

    public static void initGenerators() {
        GENERATORS.add(new GeneratorAssembly());
    }

    protected Generator() {}

    public abstract String getGeneratorCommandLine();
    public abstract String getGeneratorName();
    public abstract String getFileExtension();

    // LinkedData was removed to be re-added, Object will be changed to a different object in the version when linking is added back
    public void generate(File outputFile, Object linked) {
        throw new NotImplementedException("Generating " + getGeneratorName() + " is not implemented yet");
    }

}
