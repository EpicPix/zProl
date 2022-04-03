package ga.epicpix.zprol.generators;

import ga.epicpix.zprol.compiled.GeneratedData;

import java.io.DataOutputStream;
import java.io.IOException;

public final class GeneratorAssembly extends Generator {

    public String getGeneratorCommandLine() {
        return "asmlinux64";
    }

    public String getGeneratorName() {
        return "Assembly x86-64 Linux";
    }

    public String getFileExtension() {
        return ".asm";
    }

    public void generate(DataOutputStream outStream, GeneratedData generated) throws IOException {

    }
}
