package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.*;
import ga.epicpix.zprol.compiled.Compiler;
import ga.epicpix.zprol.exceptions.compilation.CompileException;
import ga.epicpix.zprol.generators.Generator;
import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.precompiled.PreCompiler;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.compilation.ParserException;
import ga.epicpix.zprol.exceptions.compilation.UnknownTypeException;
import ga.epicpix.zprol.parser.Parser;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.zld.Language;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;

public class Start {

    public static final boolean SHOW_TIMINGS = !Boolean.parseBoolean(System.getProperty("HIDE_TIMINGS"));

    public static void main(String[] args) throws UnknownTypeException, IOException {
        try {
            long startLoad = System.currentTimeMillis();
            Language.load("language.zld");
            long endLoad = System.currentTimeMillis();
            if(SHOW_TIMINGS) System.out.printf("Took %d ms load language definition\n", endLoad - startLoad);
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
        String printFile = null;

        ArrayList<String> files = new ArrayList<>();
        Iterator<String> argsIterator = Arrays.asList(args).iterator();
        while(argsIterator.hasNext()) {
            String s = argsIterator.next();
            if(s.startsWith("-")) {
                if(s.startsWith("-g")) {
                    String gen = s.substring(2);
                    boolean found = false;
                    for(Generator generator : Generator.GENERATORS) {
                        if(gen.equals(generator.getGeneratorCommandLine())) {
                            generators.add(generator);
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        throw new IllegalArgumentException("Unable to find generator: " + gen);
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
                } else if(s.equals("-p")) {
                    if(printFile != null) {
                        throw new IllegalArgumentException("Tried to declare input file multiple times");
                    }
                    if(!argsIterator.hasNext()) {
                        if(outputFile != null) {
                            printFile = outputFile;
                            break;
                        }
                        throw new IllegalArgumentException("Missing input filename after -p");
                    }
                    printFile = argsIterator.next();
                    continue;
                }
                System.err.println("Unknown setting: " + s);
                System.exit(1);
            } else {
                files.add(s);
            }
        }
        if(outputFile != null || !files.isEmpty()) {
            if(files.isEmpty()) {
                throw new IllegalArgumentException("Files to compile not specified");
            }

            compileFiles(files, Objects.requireNonNullElse(outputFile, "output.out"), generators);
        }

        if(printFile != null) {
            printZpil(GeneratedData.load(Files.readAllBytes(new File(printFile).toPath())));
        }

        if(outputFile != null || printFile != null) {
            return;
        }

        throw new NotImplementedException("When help menu?");
    }

    public static void compileFiles(ArrayList<String> files, String output, ArrayList<Generator> generators) throws IOException, UnknownTypeException {
        ArrayList<PreCompiledData> preCompiled = new ArrayList<>();
        for(String file : files) {
            boolean load = false;
            if(new File(file).exists() && Files.size(new File(file).toPath()) >= 4) {
                DataInputStream in = new DataInputStream(new FileInputStream(file));
                if(new String(in.readNBytes(4)).equals("zPrl")) {
                    load = true;
                }
                in.close();
            }

            if(load) {
                throw new NotImplementedException("Cannot load compiled files yet!");
            }else {
                String normalName = file.substring(0, file.lastIndexOf('.') == -1 ? file.length() : file.lastIndexOf('.'));
                try {
                    long startToken = System.currentTimeMillis();
                    ArrayList<Token> tokens = Parser.tokenize(new File(file));
                    long endToken = System.currentTimeMillis();
                    if(SHOW_TIMINGS) System.out.printf("[%s] Took %d ms to tokenize\n", file.substring(file.lastIndexOf('/') + 1), endToken - startToken);

                    if(Boolean.parseBoolean(System.getProperty("PARSE_TREE"))) {
                        long startAst = System.currentTimeMillis();
                        DataOutputStream out = new DataOutputStream(new FileOutputStream(normalName + ".dot"));
                        out.write(Parser.generateParseTree(tokens).getBytes(StandardCharsets.UTF_8));
                        out.close();
                        long endAst = System.currentTimeMillis();
                        if(SHOW_TIMINGS) System.out.printf("[%s] Took %d ms to save parser parse tree\n", file.substring(file.lastIndexOf('/') + 1), endAst - startAst);
                    }

                    long startPreCompile = System.currentTimeMillis();
                    PreCompiledData data = PreCompiler.preCompile(file.substring(file.lastIndexOf('/') + 1), tokens);
                    long stopPreCompile = System.currentTimeMillis();
                    if(SHOW_TIMINGS) System.out.printf("[%s] Took %d ms to precompile\n", data.namespace != null ? data.namespace : data.sourceFile, stopPreCompile - startPreCompile);
                    preCompiled.add(data);
                }catch(ParserException e) {
                    e.printError();
                    System.exit(1);
                    return;
                }catch(CompileException e) {
                    e.printError();
                    System.exit(1);
                    return;
                }
            }
        }

        ArrayList<CompiledData> compiledData = new ArrayList<>();

        try {
            for (PreCompiledData data : preCompiled) {
                ArrayList<PreCompiledData> pre = new ArrayList<>(preCompiled);
                pre.remove(data);
                long startCompile = System.currentTimeMillis();
                CompiledData zpil = Compiler.compile(data, pre);
                long stopCompile = System.currentTimeMillis();
                if (SHOW_TIMINGS)
                    System.out.printf("[%s] Took %d ms to compile\n", zpil.namespace != null ? zpil.namespace : data.sourceFile, stopCompile - startCompile);
                compiledData.add(zpil);
            }
        }catch(CompileException e) {
            e.printError();
            System.exit(1);
            return;
        }

        GeneratedData linked = new GeneratedData();
        long startLink = System.currentTimeMillis();
        linked.addCompiled(compiledData.toArray(new CompiledData[0]));
        long stopLink = System.currentTimeMillis();
        if(SHOW_TIMINGS) System.out.printf("Took %d ms to link everything\n", stopLink - startLink);

        String normalName = output.substring(0, output.lastIndexOf('.') == -1 ? output.length() : output.lastIndexOf('.'));
        {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(normalName + ".zpil"));
            out.write(GeneratedData.save(linked));
            out.close();
        }

        for(Generator gen : generators) {
            long startGenerator = System.currentTimeMillis();
            DataOutputStream out = new DataOutputStream(new FileOutputStream(normalName + gen.getFileExtension()));
            gen.generate(out, linked);
            out.close();
            long stopGenerator = System.currentTimeMillis();
            if(SHOW_TIMINGS) System.out.printf("Took %d ms to generate %s code\n", stopGenerator - startGenerator, gen.getGeneratorName());
        }
    }

    public static void printZpil(GeneratedData data) {
        System.out.println("Functions:");
        for(Function func : data.functions) {
            System.out.println("  Function");
            System.out.println("    Namespace: \"" + (func.namespace() != null ? func.namespace() : "") + "\"");
            System.out.println("    Name: \"" + func.name() + "\"");
            System.out.println("    Signature: \"" + func.signature() + "\"");
            System.out.println("    Modifiers (" + func.modifiers().size() + "):");
            for(FunctionModifiers modifier : func.modifiers()) {
                System.out.println("      " + modifier);
            }
            if(!FunctionModifiers.isEmptyCode(func.modifiers())) {
                System.out.println("    Code");
                System.out.println("      Locals Size: " + func.code().getLocalsSize());
                System.out.println("      Instructions");
                for(var instruction : func.code().getInstructions()) {
                    System.out.println("        " + instruction);
                }
            }
        }
    }

}
