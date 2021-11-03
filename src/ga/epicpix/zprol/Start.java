package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.generators.GeneratorAssembly;
import ga.epicpix.zprol.generators.GeneratorPorth;
import ga.epicpix.zprol.tokens.Token;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Start {

    public static void main(String[] args) throws IOException, UnknownTypeException {
        boolean load = false;
        String fileName = null;
        for(String s : args) {
            if(s.startsWith("-")) {
                if(s.equals("-load")) {
                    load = true;
                    continue;
                }
                System.err.println("Unknown setting: " + s);
                System.exit(1);
            }else {
                if(fileName != null) {
                    System.err.println("Multiple file names specified, already contains \"" + fileName + "\" but also found \"" + s + "\"");
                    System.exit(1);
                }
                fileName = s;
            }
        }
        if(fileName == null) {
            throw new IllegalArgumentException("File not specified");
        }
        String normalName = fileName.substring(0, fileName.lastIndexOf('.'));

        CompiledData zpil;

        if(load) {
            long loadStart = System.currentTimeMillis();
            zpil = CompiledData.load(new File(fileName));
            long loadEnd = System.currentTimeMillis();
            System.out.printf("Took %d ms to load\n", loadEnd - loadStart);
        }else {
            long startToken = System.currentTimeMillis();
            ArrayList<Token> tokens = Parser.tokenize(fileName);
            long startCompile = System.currentTimeMillis();
            System.out.printf("Took %d ms to tokenize\n", startCompile - startToken);
            zpil = Compiler.compile(tokens);
            long startSave = System.currentTimeMillis();
            System.out.printf("Took %d ms to compile\n", startSave - startCompile);
            zpil.save(new File(normalName + ".zpil"));
            long startConvert = System.currentTimeMillis();
            System.out.printf("Took %d ms to save\n", startConvert - startSave);
        }

        long gen_x86_64_linux_asm_start = System.currentTimeMillis();
        GeneratorAssembly.generate_x86_64_linux_assembly(zpil, new File(normalName + "_64linux.asm"));
        long gen_x86_64_linux_asm_end = System.currentTimeMillis();
        System.out.printf("Took %d ms to generate x86-64 linux assembly\n", gen_x86_64_linux_asm_end - gen_x86_64_linux_asm_start);

        long gen_porth_start = System.currentTimeMillis();
        GeneratorPorth.generate_porth(zpil, new File(normalName + ".porth"));
        long gen_porth_end = System.currentTimeMillis();
        System.out.printf("Took %d ms to generate porth\n", gen_porth_end - gen_porth_start);
    }

}
