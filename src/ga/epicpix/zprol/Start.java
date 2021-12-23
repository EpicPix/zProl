package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Compiler;
import ga.epicpix.zprol.generators.Generator;
import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.precompiled.PreCompiler;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.parser.Parser;
import ga.epicpix.zprol.parser.tokens.Token;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;

public class Start {

    public static void main(String[] args) throws UnknownTypeException, IOException {
        try {
            Language.load("language.zld");
        }catch(ParserException e) {
            e.printError();
            System.exit(1);
            return;
        }
        Generator.initGenerators();

        boolean p = false;
        ArrayList<String> arguments = new ArrayList<>();
        for(String arg : args) {
            if(arg.equals("-test")) p = true;
            else arguments.add(arg);
        }
        args = arguments.toArray(new String[0]);
        if(p) {
            Scanner sc = new Scanner(System.in);
            while(!sc.nextLine().equals("break")) {
                preMain(args);
            }
        }else {
            preMain(args);
        }
    }

    public static void preMain(String[] args) throws IOException, UnknownTypeException {
        ArrayList<Generator> generators = new ArrayList<>();
        String outputFile = null;

        ArrayList<String> files = new ArrayList<>();
        Iterator<String> argsIterator = Arrays.asList(args).iterator();
        while(argsIterator.hasNext()) {
            String s = argsIterator.next();
            if(s.startsWith("-")) {
                if(s.startsWith("-g")) {
                    String gen = s.substring(2);
                    for(Generator generator : Generator.GENERATORS) {
                        if(gen.equals(generator.getGeneratorCommandLine())) {
                            generators.add(generator);
                            break;
                        }
                    }
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
        if(files.isEmpty()) {
            throw new IllegalArgumentException("Files to compile not specified");
        }
        compileFiles(files, Objects.requireNonNullElse(outputFile, "output.out"), generators);
    }

    public static void compileFiles(ArrayList<String> files, String output, ArrayList<Generator> generators) throws IOException, UnknownTypeException {
        ArrayList<PreCompiledData> preCompiled = new ArrayList<>();
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
//                long loadStart = System.currentTimeMillis();
//                compiled.add(CompiledData.load(new File(file)));
//                long loadEnd = System.currentTimeMillis();
//                System.out.printf("[%s] Took %d ms to load\n", file, loadEnd - loadStart);
                throw new NotImplementedException("Cannot load compiled files yet!");
            }else {
                try {
                    long startToken = System.currentTimeMillis();
                    ArrayList<Token> tokens = Parser.tokenize(file);
                    long endToken = System.currentTimeMillis();
                    System.out.printf("[%s] Took %d ms to tokenize\n", file, endToken - startToken);

                    long startPreCompile = System.currentTimeMillis();
                    PreCompiledData data = PreCompiler.preCompile(tokens);
                    long stopPreCompile = System.currentTimeMillis();
                    System.out.printf("[%s] Took %d ms to precompile\n", file, stopPreCompile - startPreCompile);
                    preCompiled.add(data);
                }catch(ParserException e) {
                    e.printError();
                    System.exit(1);
                    return;
                }
            }
        }

        ArrayList<CompiledData> compiledData = new ArrayList<>();

        for(PreCompiledData data : preCompiled) {
            ArrayList<PreCompiledData> pre = new ArrayList<>(preCompiled);
            pre.remove(data);
            long startCompile = System.currentTimeMillis();
            CompiledData zpil = Compiler.compile(data, pre);
            long stopCompile = System.currentTimeMillis();
            System.out.printf("[%s] Took %d ms to compile\n", zpil.namespace, stopCompile - startCompile);
            compiledData.add(zpil);
        }


        String normalName = output.substring(0, output.lastIndexOf('.') == -1 ? output.length() : output.lastIndexOf('.'));

        System.err.println("Saving is not fully implemented.");

        for(Generator gen : generators) {
            long startGenerator = System.currentTimeMillis();
            gen.generate(new File(normalName + gen.getFileExtension()), null);
            long stopGenerator = System.currentTimeMillis();
            System.out.printf("Took %d ms to generate %s\n", stopGenerator - startGenerator, gen.getGeneratorName());
        }
    }

}
