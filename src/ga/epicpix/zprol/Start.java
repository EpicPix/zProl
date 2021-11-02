package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.generators.GeneratorAssembly;
import ga.epicpix.zprol.generators.GeneratorPorth;
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

        long gen_x86_64_linux_asm_start = System.currentTimeMillis();
        GeneratorAssembly.generate_x86_64_linux_assembly(compiled, new File(normalName + "_64linux.asm"));
        long gen_x86_64_linux_asm_end = System.currentTimeMillis();
        System.out.printf("Took %d ms to generate x86-64 linux assembly\n", gen_x86_64_linux_asm_end - gen_x86_64_linux_asm_start);

        long gen_porth_start = System.currentTimeMillis();
        GeneratorPorth.generate_porth(compiled, new File(normalName + ".porth"));
        long gen_porth_end = System.currentTimeMillis();
        System.out.printf("Took %d ms to generate porth\n", gen_porth_end - gen_porth_start);
    }

}
