package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.tokens.Token;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Start {

    public static void main(String[] args) throws IOException, UnknownTypeException {
        if(args.length == 0) {
            throw new IllegalArgumentException("File not specified");
        }
        String fileName = String.join(" ", args);
        String normalName = fileName.substring(0, fileName.lastIndexOf('.'));
        long startToken = System.currentTimeMillis();
        ArrayList<Token> tokens = Parser.tokenize(fileName);
        long startCompile = System.currentTimeMillis();
        System.out.printf("Took %d ms to tokenize\n", startCompile - startToken);
        CompiledData compiled = Compiler.compile(tokens);
        long startSave = System.currentTimeMillis();
        System.out.printf("Took %d ms to compile\n", startSave - startCompile);
        compiled.save(new File(normalName + ".zpil"));
        long startConvert = System.currentTimeMillis();
        System.out.printf("Took %d ms to save\n", startConvert - startSave);
        Generator.generate_x86_linux_assembly(compiled, new File(normalName + "_64linux.asm"), true);
        long start32BitGen = System.currentTimeMillis();
        System.out.printf("Took %d ms to generate x86-64 linux assembly\n", start32BitGen - startConvert);
        Generator.generate_x86_linux_assembly(compiled, new File(normalName + "_32linux.asm"), false);
        long end = System.currentTimeMillis();
        System.out.printf("Took %d ms to generate x86 linux assembly\n", end - startConvert);
    }

}
