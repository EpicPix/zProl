package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.TypeNamed;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Generator {

    public static void generate_x86_64_linux_assembly(CompiledData data, File save) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(save));

        for(Function func : data.getFunctions()) {
            if(!func.flags.contains(Flag.NO_IMPLEMENTATION)) {
                StringBuilder funcName = new StringBuilder(func.name + "." + func.signature.returnType);
                for(TypeNamed param : func.signature.parameters) {
                    funcName.append(".").append(param.type.type.toString().toLowerCase());
                }
                writer.write(funcName + ":\n");
            }
        }

//        writer.write();

        writer.close();
    }

}
