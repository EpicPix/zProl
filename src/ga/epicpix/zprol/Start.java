package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.exceptions.NotImplementedException;
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
import java.util.Arrays;
import java.util.Iterator;

public class Start {

    public static void main(String[] args) throws IOException, UnknownTypeException {

        boolean generate_x86_64_linux = false;
        boolean generate_porth = false;
        String outputFile = null;

        ArrayList<String> files = new ArrayList<>();
        Iterator<String> argsIterator = Arrays.asList(args).iterator();
        while(argsIterator.hasNext()) {
            String s = argsIterator.next();
            if(s.startsWith("-")) {
                if(s.equals("-glinux64")) {
                    generate_x86_64_linux = true;
                    continue;
                } else if(s.equals("-gporth")) {
                    generate_porth = true;
                    continue;
                } else if(s.equals("-o")) {
                    if(outputFile != null) {
                        throw new IllegalArgumentException("Tried to declare output file multiple times");
                    }
                    if(!argsIterator.hasNext()) {
                        throw new IllegalArgumentException("Missing output filename after -o");
                    }
                    outputFile = argsIterator.next();
                    continue;
                }
                System.err.println("Unknown setting: " + s);
                System.exit(1);
            } else {
                files.add(s);
            }
        }
        if(files.size() == 0) {
            throw new IllegalArgumentException("Files to compile not specified");
        }
        if(outputFile != null) {
            compileFiles(files, outputFile, generate_x86_64_linux, generate_porth);
        }else {
            for(String file : files) {
                CompiledData data = loadFile(file, outputFile, generate_x86_64_linux, generate_porth);
                if(data == null) {
                    System.exit(1);
                    return;
                }
            }
        }
    }

    public static void compileFiles(ArrayList<String> files, String output, boolean generate_x86_64_linux, boolean generate_porth) throws IOException {
        ArrayList<CompiledData> compiled = new ArrayList<>();
        for(String file : files) {
            boolean load = false;
            if(new File(file).exists()) {
                DataInputStream in = new DataInputStream(new FileInputStream(file));
                if(in.readInt() == 0x7a50524c) {
                    load = true;
                }
                in.close();
            }

            if(load) {
                long loadStart = System.currentTimeMillis();
                compiled.add(CompiledData.load(new File(file)));
                long loadEnd = System.currentTimeMillis();
                System.out.printf("[%s] Took %d ms to load\n", file, loadEnd - loadStart);
            }else {
                try {
                    long startToken = System.currentTimeMillis();
                    ArrayList<Token> tokens = Parser.tokenize(file);
                    long endToken = System.currentTimeMillis();
                    System.out.printf("[%s] Took %d ms to tokenize\n", file, endToken - startToken);
                    Compiler.preCompile(tokens);
                    // compiled.add(Compiler.compile(???));
                }catch(ParserException e) {
                    e.printError();
                    System.exit(1);
                    return;
                }
            }
        }

        // BytecodeGenerator.joinFiles(compiled); ???

        if(generate_x86_64_linux) {
            throw new NotImplementedException("Generating x86-64 linux assembly from multiple compiled files is not supported yet!");
        }

        if(generate_porth) {
            throw new NotImplementedException("Generating x86-64 linux assembly from multiple compiled files is not supported yet!");
        }
    }

    public static CompiledData loadFile(String file, String output, boolean generate_x86_64_linux, boolean generate_porth) throws IOException, UnknownTypeException {
        boolean load = false;
        if(new File(file).exists()) {
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            if(in.readInt() == 0x7a50524c) {
                load = true;
            }
            in.close();
        }
        String normalName = output == null ? file.substring(0, file.lastIndexOf('.')) : output.substring(0, output.lastIndexOf('.'));

        CompiledData zpil;

        if(load) {
            long loadStart = System.currentTimeMillis();
            zpil = CompiledData.load(new File(file));
            long loadEnd = System.currentTimeMillis();
            System.out.printf("[%s] Took %d ms to load\n", file, loadEnd - loadStart);
        }else {
            ArrayList<Token> tokens;
            try {
                long startToken = System.currentTimeMillis();
                tokens = Parser.tokenize(file);
                long endToken = System.currentTimeMillis();
                System.out.printf("[%s] Took %d ms to tokenize\n", file, endToken - startToken);
            }catch(ParserException e) {
                e.printError();
                System.exit(1);
                return null;
            }
            long startCompile = System.currentTimeMillis();
            zpil = Compiler.compile(tokens);
            long stopCompile = System.currentTimeMillis();
            System.out.printf("[%s] Took %d ms to compile\n", file, stopCompile - startCompile);

            long startSave = System.currentTimeMillis();
            zpil.save(new File(normalName + ".zpil"));
            long stopSave = System.currentTimeMillis();
            System.out.printf("[%s] Took %d ms to save\n", file, stopSave - startSave);
        }

        if(generate_x86_64_linux) {
            long gen_x86_64_linux_asm_start = System.currentTimeMillis();
            GeneratorAssembly.generate_x86_64_linux_assembly(zpil, new File(normalName + "_64linux.asm"));
            long gen_x86_64_linux_asm_end = System.currentTimeMillis();
            System.out.printf("[%s] Took %d ms to generate x86-64 linux assembly\n", file, gen_x86_64_linux_asm_end - gen_x86_64_linux_asm_start);
        }

        if(generate_porth) {
            long gen_porth_start = System.currentTimeMillis();
            GeneratorPorth.generate_porth(zpil, new File(normalName + ".porth"));
            long gen_porth_end = System.currentTimeMillis();
            System.out.printf("[%s] Took %d ms to generate porth\n", file, gen_porth_end - gen_porth_start);
        }
        return zpil;
    }

}
