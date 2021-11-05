package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.generators.GeneratorAssembly;
import ga.epicpix.zprol.generators.GeneratorPorth;
import ga.epicpix.zprol.tokens.Token;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Start {

    public static void main(String[] args) throws IOException, UnknownTypeException {

        boolean generate_x86_64_linux = false;
        boolean generate_porth = false;

        String fileName = null;
        for(String s : args) {
            if(s.startsWith("-")) {
                if(s.equals("-glinux64")) {
                    generate_x86_64_linux = true;
                    continue;
                }else if(s.equals("-gporth")) {
                    generate_porth = true;
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
        boolean load = false;
        if(new File(fileName).exists()) {
            DataInputStream in = new DataInputStream(new FileInputStream(fileName));
            if(in.readInt() == 0x7a50524c) {
                load = true;
            }
            in.close();
        }
        String normalName = fileName.substring(0, fileName.lastIndexOf('.'));

        CompiledData zpil;

        if(load) {
            long loadStart = System.currentTimeMillis();
            zpil = CompiledData.load(new File(fileName));
            long loadEnd = System.currentTimeMillis();
            System.out.printf("Took %d ms to load\n", loadEnd - loadStart);
        }else {
            ArrayList<Token> tokens;
            try {
                long startToken = System.currentTimeMillis();
                tokens = Parser.tokenize(fileName);
                long endToken = System.currentTimeMillis();
                System.out.printf("Took %d ms to tokenize\n", endToken - startToken);
            }catch(ParserException e) {
                e.printError();
                System.exit(1);
                return;
            }
            long startCompile = System.currentTimeMillis();
            zpil = Compiler.compile(tokens);
            long startSave = System.currentTimeMillis();
            System.out.printf("Took %d ms to compile\n", startSave - startCompile);
            zpil.save(new File(normalName + ".zpil"));
            long startConvert = System.currentTimeMillis();
            System.out.printf("Took %d ms to save\n", startConvert - startSave);
        }

        if(generate_x86_64_linux) {
            long gen_x86_64_linux_asm_start = System.currentTimeMillis();
            GeneratorAssembly.generate_x86_64_linux_assembly(zpil, new File(normalName + "_64linux.asm"));
            long gen_x86_64_linux_asm_end = System.currentTimeMillis();
            System.out.printf("Took %d ms to generate x86-64 linux assembly\n", gen_x86_64_linux_asm_end - gen_x86_64_linux_asm_start);
        }

        if(generate_porth) {
            long gen_porth_start = System.currentTimeMillis();
            GeneratorPorth.generate_porth(zpil, new File(normalName + ".porth"));
            long gen_porth_end = System.currentTimeMillis();
            System.out.printf("Took %d ms to generate porth\n", gen_porth_end - gen_porth_start);
        }
    }

}
