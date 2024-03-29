package ga.epicpix.zprol;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.compiler.Compiler;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.precompiled.*;
import ga.epicpix.zprol.errors.ErrorCodes;
import ga.epicpix.zprol.errors.ErrorStorage;
import ga.epicpix.zprol.errors.ErrorType;
import ga.epicpix.zprol.generators.Generator;
import ga.epicpix.zprol.interpreter.Interpreter;
import ga.epicpix.zprol.parser.Lexer;
import ga.epicpix.zprol.parser.LexerResults;
import ga.epicpix.zprol.parser.Parser;
import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tree.FileTree;
import ga.epicpix.zprol.structures.FunctionSignature;
import ga.epicpix.zprol.types.Types;
import ga.epicpix.zprol.utils.SeekIterator;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static ga.epicpix.zprol.generators.Generator.initGenerators;

public class Start {

    public static final boolean HIDE_TIMINGS = Boolean.parseBoolean(System.getProperty("HIDE_TIMINGS"));
    public static final String VERSION = "1.1.2";

    static {
        initGenerators();
    }

    public static void main(String[] args) throws UnknownTypeException, IOException {
        ArrayList<Generator> generators = new ArrayList<>();
        boolean ignoreCompileStdWarning = false;
        boolean unloadStd = false;
        boolean errorInfo = false;
        String interpretFile = null;
        String outputFile = null;
        String printFile = null;

        ArrayList<String> files = new ArrayList<>();
        Iterator<String> argsIterator = Arrays.asList(args).iterator();
        label:
        while(argsIterator.hasNext()) {
            String s = argsIterator.next();
            if(s.startsWith("-")) {
                switch(s) {
                    case "-v", "--version" -> {
                        System.out.println("zProl Version: " + VERSION);
                        return;
                    }
                    case "-g" -> {
                        if(!argsIterator.hasNext()) {
                            throw new IllegalArgumentException("Expected generator after '-g'");
                        }
                        String gen = argsIterator.next();
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
                    }
                    case "-o" -> {
                        if(outputFile != null) {
                            throw new IllegalArgumentException("Tried to declare output file multiple times");
                        }
                        if(!argsIterator.hasNext()) {
                            throw new IllegalArgumentException("Missing output filename after -o");
                        }
                        outputFile = argsIterator.next();
                        continue;
                    }
                    case "--ignore-std-not-found-warning" -> {
                        ignoreCompileStdWarning = true;
                        continue;
                    }
                    case "--unload-std" -> {
                        unloadStd = true;
                        continue;
                    }
                    case "--error-info" -> {
                        errorInfo = true;
                        continue;
                    }
                    case "-i" -> {
                        if(interpretFile != null) {
                            throw new IllegalArgumentException("Tried to declare interpreter file multiple times");
                        }
                        if(!argsIterator.hasNext()) {
                            throw new IllegalArgumentException("Missing filename after -i");
                        }
                        interpretFile = argsIterator.next();
                        continue;
                    }
                    case "-p" -> {
                        if(printFile != null) {
                            throw new IllegalArgumentException("Tried to declare input file multiple times");
                        }
                        if(!argsIterator.hasNext()) {
                            if(outputFile != null) {
                                printFile = outputFile;
                                break label;
                            }
                            throw new IllegalArgumentException("Missing input filename after -p");
                        }
                        printFile = argsIterator.next();
                        continue;
                    }
                }
                System.err.println("Unknown setting: " + s);
                System.exit(1);
            } else {
                files.add(s);
            }
        }

        ErrorStorage errors = new ErrorStorage(errorInfo);
        if(outputFile != null || !files.isEmpty()) {
            if(files.isEmpty()) {
                throw new IllegalArgumentException("Files to compile not specified");
            }

            String output = outputFile != null ? outputFile : "output.out";

            compileFiles(files, output, ignoreCompileStdWarning, unloadStd, errors);

            int warnCount = errors.getErrorCount(ErrorType.WARN);
            int errCount = errors.getErrorCount(ErrorType.ERROR) + errors.getErrorCount(ErrorType.CRITICAL);
            if(warnCount != 0) {
                if(warnCount == 1) {
                    System.out.println("1 warning");
                }else {
                    System.out.println(warnCount + " warnings");
                }
            }
            if(errCount != 0) {
                if(errCount == 1) {
                    System.out.println("1 error");
                }else {
                    System.out.println(errCount + " errors");
                }
                System.exit(1);
                return;
            }

            String normalName = output.substring(0, output.lastIndexOf('.') == -1 ? output.length() : output.lastIndexOf('.'));
            var generated = GeneratedData.load(Files.readAllBytes(new File(normalName + ".zpil").toPath()));
            for(Generator gen : generators) {
                long startGenerator = System.nanoTime();
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(normalName + gen.getFileExtension())));
                gen.generate(out, generated);
                out.close();
                long stopGenerator = System.nanoTime();
                if(!HIDE_TIMINGS) System.out.printf("Took %d μs to generate %s code\n", (stopGenerator - startGenerator)/1000, gen.getGeneratorName());
            }
        }

        if(printFile != null) {
            GeneratedData.load(Files.readAllBytes(new File(printFile).toPath())).printZpil();
        }

        if(interpretFile != null) {
            var file = GeneratedData.load(Files.readAllBytes(new File(interpretFile).toPath()));
            var function = file.getFunction(new FunctionSignature(Types.getTypeFromDescriptor("V")), "main");
            if(function == null) {
                System.err.println("Function 'void main()' not found!");
                System.exit(1);
                return;
            }
            Interpreter.runInterpreter(file, function);
        }

        if(outputFile != null || printFile != null || interpretFile != null) {
            return;
        }

        System.out.println("zProl Help Menu");
        System.out.println("-i <zpil>                        Run the zProl interpreter, first function that has the signature of 'void main()', will be used");
        System.out.println("-g <gen>                         Use a generator for converting zpil bytecode to other formats");
        System.out.println("-o <file>                        File where the generated zpil should be put");
        System.out.println("-p [file]                        Shows compiled zpil code, optional file if the output file is provided");
        System.out.println("-v                               Shows version of the currently running zProl version");
        System.out.println("--error-info                     Shows errors with more info");
        System.out.println("--ignore-std-not-found-warning   Will not show a warning message when the std was not found (used for compiling the std itself)");
        System.out.println("--unload-std                     Will not load the std while compiling (will use other libraries though)");
        System.out.println("--version                        Shows version of the currently running zProl version");
        System.out.println();
        System.out.println("Available generators:");
        for(var gen : Generator.GENERATORS) {
            System.out.println(" - " + gen.getGeneratorName() + " (*" + gen.getFileExtension() + "): -g " + gen.getGeneratorCommandLine());
        }
    }

    public static void compileFiles(ArrayList<String> files, String output, boolean ignoreStdWarning, boolean unloadStd, ErrorStorage errors) throws IOException, UnknownTypeException {
        ArrayList<PreCompiledData> preCompiled = new ArrayList<>();
        ArrayList<CompiledData> compiled = new ArrayList<>();
        ArrayList<GeneratedData> includedCompiled = new ArrayList<>();

        if(!unloadStd) {
            var stdReader = Start.class.getClassLoader().getResourceAsStream("std.zpil");
            if (stdReader != null) {
                var gen = GeneratedData.load(stdReader.readAllBytes());
                includedCompiled.add(gen);
            } else {
                if (!ignoreStdWarning) {
                    errors.addError(ErrorCodes.STD_STANDARD_LIBRARY_NOT_FOUND);
                }
            }
        }

        boolean buildFailed = false;

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
                var gen = GeneratedData.load(Files.readAllBytes(new File(file).toPath()));
                includedCompiled.add(gen);
            }else {
                try {
                    long startLex = System.nanoTime();
                    LexerResults lexedTokens = Lexer.lex(file, Files.readAllLines(new File(file).toPath()).toArray(new String[0]), errors);
                    long endLex = System.nanoTime();
                    if(errors.getErrorCount(ErrorType.ERROR) + errors.getErrorCount(ErrorType.CRITICAL) != 0) {
                        System.out.printf("[%s] Failed to lex due to errors\n",file.substring(file.lastIndexOf('/') + 1));
                        buildFailed = true;
                        continue;
                    }
                    if(!HIDE_TIMINGS) System.out.printf("[%s] Took %d μs to lex\n", file.substring(file.lastIndexOf('/') + 1), (endLex - startLex)/1000);

                    long startToken = System.nanoTime();
                    FileTree tree = Parser.parse(new SeekIterator<>(lexedTokens.tokens), errors);
                    long endToken = System.nanoTime();
                    if(errors.getErrorCount(ErrorType.ERROR) + errors.getErrorCount(ErrorType.CRITICAL) != 0) {
                        System.out.printf("[%s] Failed to parse due to errors\n",file.substring(file.lastIndexOf('/') + 1));
                        buildFailed = true;
                        continue;
                    }

                    if(!HIDE_TIMINGS) System.out.printf("[%s] Took %d μs to parse\n", file.substring(file.lastIndexOf('/') + 1), (endToken - startToken)/1000);

                    long startPreCompile = System.nanoTime();
                    PreCompiledData data = PreCompiler.preCompile(file.substring(file.lastIndexOf('/') + 1), tree, lexedTokens.parser);
                    long stopPreCompile = System.nanoTime();
                    if(!HIDE_TIMINGS) System.out.printf("[%s] Took %d μs to precompile\n", data.namespace != null ? data.namespace : data.sourceFile, (stopPreCompile - startPreCompile)/1000);
                    preCompiled.add(data);
                }catch(ParserException e) {
                    e.printError();
                    System.exit(1);
                    return;
                }catch(TokenLocatedException e) {
                    e.printError();
                    System.exit(1);
                    return;
                }
            }
        }

        if(buildFailed) {
            return;
        }


        try {
            for (PreCompiledData data : preCompiled) {
                ArrayList<PreCompiledData> pre = new ArrayList<>(preCompiled);
                pre.remove(data);
                long startCompile = System.nanoTime();
                CompiledData zpil = Compiler.compile(data, pre, includedCompiled, data.parser);
                long stopCompile = System.nanoTime();
                if (!HIDE_TIMINGS)
                    System.out.printf("[%s] Took %d μs to compile\n", zpil.namespace != null ? zpil.namespace : data.sourceFile, (stopCompile - startCompile)/1000);
                compiled.add(zpil);
            }
        }catch(TokenLocatedException e) {
            e.printError();
            System.exit(1);
            return;
        }

        GeneratedData linked = new GeneratedData();
        long startLink = System.nanoTime();
        for(CompiledData d : compiled) d.includeToGenerated(linked);
        for(GeneratedData d : includedCompiled) d.includeToGenerated(linked);
        long stopLink = System.nanoTime();
        if(!HIDE_TIMINGS) System.out.printf("Took %d μs to link everything\n", (stopLink - startLink)/1000);

        String normalName = output.substring(0, output.lastIndexOf('.') == -1 ? output.length() : output.lastIndexOf('.'));
        {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(normalName + ".zpil"));
            out.write(GeneratedData.save(linked));
            out.close();
        }
    }

}
