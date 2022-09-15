package ga.epicpix.zprol;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.compiler.Compiler;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.precompiled.*;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.generators.Generator;
import ga.epicpix.zprol.parser.Lexer;
import ga.epicpix.zprol.parser.Parser;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tree.FileTree;
import ga.epicpix.zprol.structures.FunctionModifiers;
import ga.epicpix.zprol.utils.SeekIterator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static ga.epicpix.zprol.Loader.*;

public class Start {

    public static final boolean SHOW_TIMINGS = !Boolean.parseBoolean(System.getProperty("HIDE_TIMINGS"));

    public static void main(String[] args) throws UnknownTypeException, IOException {
        registerTypes();
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
        boolean ignoreCompileStdWarning = false;
        boolean unloadStd = false;
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
                } else if(s.equals("--ignore-std-not-found-warning")) {
                    ignoreCompileStdWarning = true;
                    continue;
                } else if(s.equals("--unload-std")) {
                    unloadStd = true;
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

            String output = Objects.requireNonNullElse(outputFile, "output.out");

            compileFiles(files, output, ignoreCompileStdWarning, unloadStd);

            String normalName = output.substring(0, output.lastIndexOf('.') == -1 ? output.length() : output.lastIndexOf('.'));
            var generated = GeneratedData.load(Files.readAllBytes(new File(normalName + ".zpil").toPath()));
            for(Generator gen : generators) {
                long startGenerator = System.currentTimeMillis();
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(normalName + gen.getFileExtension())));
                gen.generate(out, generated);
                out.close();
                long stopGenerator = System.currentTimeMillis();
                if(SHOW_TIMINGS) System.out.printf("Took %d ms to generate %s code\n", stopGenerator - startGenerator, gen.getGeneratorName());
            }
        }

        if(printFile != null) {
            printZpil(GeneratedData.load(Files.readAllBytes(new File(printFile).toPath())));
        }

        if(outputFile != null || printFile != null) {
            return;
        }

        throw new NotImplementedException("When help menu?");
    }

    public static void compileFiles(ArrayList<String> files, String output, boolean ignoreStdWarning, boolean unloadStd) throws IOException, UnknownTypeException {
        ArrayList<PreCompiledData> preCompiled = new ArrayList<>();
        ArrayList<CompiledData> compiled = new ArrayList<>();
        ArrayList<PreCompiledData> included = new ArrayList<>();
        ArrayList<CompiledData> includedCompiled = new ArrayList<>();
        if(!unloadStd) {
            var stdReader = Start.class.getClassLoader().getResourceAsStream("std.zpil");
            if (stdReader != null) {
                loadGenerated(GeneratedData.load(stdReader.readAllBytes()), includedCompiled, included);
            } else {
                if (!ignoreStdWarning) System.err.println("Warning! Standard library not found");
            }
        }
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
                loadGenerated(gen, includedCompiled, included);
            }else {
                try {
                    long startLex = System.currentTimeMillis();
                    ArrayList<LexerToken> lexedTokens = Lexer.lex(new File(file).getName(), Files.readAllLines(new File(file).toPath()).toArray(new String[0]));
                    long endLex = System.currentTimeMillis();
                    if(SHOW_TIMINGS) System.out.printf("[%s] Took %d ms to lex\n", file.substring(file.lastIndexOf('/') + 1), endLex - startLex);

                    long startToken = System.currentTimeMillis();
                    FileTree tree = Parser.parse(new SeekIterator<>(lexedTokens));
                    long endToken = System.currentTimeMillis();
                    if(SHOW_TIMINGS) System.out.printf("[%s] Took %d ms to parse\n", file.substring(file.lastIndexOf('/') + 1), endToken - startToken);

                    long startPreCompile = System.currentTimeMillis();
                    PreCompiledData data = PreCompiler.preCompile(file.substring(file.lastIndexOf('/') + 1), tree, lexedTokens.get(0) != null ? lexedTokens.get(0).parser : null);
                    long stopPreCompile = System.currentTimeMillis();
                    if(SHOW_TIMINGS) System.out.printf("[%s] Took %d ms to precompile\n", data.namespace != null ? data.namespace : data.sourceFile, stopPreCompile - startPreCompile);
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


        try {
            for (PreCompiledData data : preCompiled) {
                ArrayList<PreCompiledData> pre = new ArrayList<>(included);
                pre.addAll(preCompiled);
                pre.remove(data);
                long startCompile = System.currentTimeMillis();
                CompiledData zpil = Compiler.compile(data, pre, data.parser);
                long stopCompile = System.currentTimeMillis();
                if (SHOW_TIMINGS)
                    System.out.printf("[%s] Took %d ms to compile\n", zpil.namespace != null ? zpil.namespace : data.sourceFile, stopCompile - startCompile);
                compiled.add(zpil);
            }
        }catch(TokenLocatedException e) {
            e.printError();
            System.exit(1);
            return;
        }

        GeneratedData linked = new GeneratedData();
        long startLink = System.currentTimeMillis();
        for(CompiledData d : compiled) d.includeToGenerated(linked);
        for(CompiledData d : includedCompiled) d.includeToGenerated(linked);
        long stopLink = System.currentTimeMillis();
        if(SHOW_TIMINGS) System.out.printf("Took %d ms to link everything\n", stopLink - startLink);

        String normalName = output.substring(0, output.lastIndexOf('.') == -1 ? output.length() : output.lastIndexOf('.'));
        {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(normalName + ".zpil"));
            out.write(GeneratedData.save(linked));
            out.close();
        }
    }

    public static void loadGenerated(GeneratedData gen, ArrayList<CompiledData> includedCompiled, ArrayList<PreCompiledData> included) {
        HashMap<String, CompiledData> compiledData = new HashMap<>();
        HashMap<String, PreCompiledData> preCompiledData = new HashMap<>();
        for(var clazz : gen.classes) {
            compiledData.putIfAbsent(clazz.namespace(), new CompiledData(clazz.namespace()));
            preCompiledData.putIfAbsent(clazz.namespace(), new PreCompiledData());

            compiledData.get(clazz.namespace()).addClass(clazz);
            var fields = new ArrayList<PreField>();
            var methods = new ArrayList<PreFunction>();
            for(var f : clazz.fields()) {
                fields.add(new PreField(f.name(), f.type().normalName(), null));
            }
            for(var func : clazz.methods()) {
                var params = new ArrayList<PreParameter>();
                for(var f : func.signature().parameters()) {
                    params.add(new PreParameter(null, f.normalName()));
                }
                var function = new PreFunction();
                function.name = func.name();
                function.returnType = func.signature().returnType().normalName();
                function.parameters.addAll(params);
                function.modifiers.addAll(func.modifiers());
                methods.add(function);
            }
            preCompiledData.get(clazz.namespace()).classes.add(new PreClass(clazz.namespace(), clazz.name(), fields.toArray(new PreField[0]), methods.toArray(new PreFunction[0])));
        }
        for(var func : gen.functions) {
            compiledData.putIfAbsent(func.namespace(), new CompiledData(func.namespace()));
            preCompiledData.putIfAbsent(func.namespace(), new PreCompiledData());

            compiledData.get(func.namespace()).addFunction(func);
            var params = new ArrayList<PreParameter>();
            for(var f : func.signature().parameters()) {
                params.add(new PreParameter(null, f.normalName()));
            }
            var function = new PreFunction();
            function.name = func.name();
            function.returnType = func.signature().returnType().normalName();
            function.parameters.addAll(params);
            function.modifiers.addAll(func.modifiers());
            preCompiledData.get(func.namespace()).functions.add(function);
        }
        for(var fld : gen.fields) {
            compiledData.putIfAbsent(fld.namespace(), new CompiledData(fld.namespace()));
            preCompiledData.putIfAbsent(fld.namespace(), new PreCompiledData());

            compiledData.get(fld.namespace()).addField(fld);
            var field = new PreField(null);
            field.name = fld.name();
            field.type = fld.type().getName();
            for(var v : fld.modifiers()) {
                for(var m : PreFieldModifiers.MODIFIERS) {
                    if(m.getCompiledModifier() == v) {
                        field.modifiers.add(m);
                        break;
                    }
                }
            }
            preCompiledData.get(fld.namespace()).fields.add(field);
        }
        for(var e : compiledData.entrySet()) includedCompiled.add(e.getValue());
        for(var e : preCompiledData.entrySet()) {
            e.getValue().namespace = e.getKey();
            included.add(e.getValue());
        }
    }

    public static void printZpil(GeneratedData data) {
        System.out.println("Functions:");
        for(var func : data.functions) {
            System.out.println("  Function");
            System.out.println("    Namespace: \"" + (func.namespace() != null ? func.namespace() : "") + "\"");
            System.out.println("    Name: \"" + func.name() + "\"");
            System.out.println("    Signature: \"" + func.signature() + "\"");
            System.out.println("    Modifiers (" + func.modifiers().size() + "):");
            for(var modifier : func.modifiers()) {
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

        System.out.println("Fields:");
        for(var fld : data.fields) {
            System.out.println("  Field");
            System.out.println("    Namespace: \"" + (fld.namespace() != null ? fld.namespace() : "") + "\"");
            System.out.println("    Name: \"" + fld.name() + "\"");
            System.out.println("    Type: \"" + fld.type().getDescriptor() + "\"");
            if(fld.defaultValue() != null) {
                if(fld.defaultValue().value() != null) {
                    System.out.println("    Constant Value: " + fld.defaultValue().value().getClass().getSimpleName() + " " + fld.defaultValue().value());
                }else {
                    System.out.println("    Constant Value: null");
                }
            }
            System.out.println("    Modifiers (" + fld.modifiers().size() + "):");
            for(var modifier : fld.modifiers()) {
                System.out.println("      " + modifier);
            }
        }

        System.out.println("Classes:");
        for(var clz : data.classes) {
            System.out.println("  Class");
            System.out.println("    Namespace: \"" + (clz.namespace() != null ? clz.namespace() : "") + "\"");
            System.out.println("    Name: \"" + clz.name() + "\"");
            System.out.println("    Fields:");
            for(var fld : clz.fields()) {
                System.out.println("      Field");
                System.out.println("        Name: \"" + fld.name() + "\"");
                System.out.println("        Type: \"" + fld.type().getDescriptor() + "\"");
            }
            System.out.println("    Methods:");
            for(var func : clz.methods()) {
                System.out.println("      Method");
                System.out.println("        Namespace: \"" + (func.namespace() != null ? func.namespace() : "") + "\"");
                System.out.println("        Class: \"" + func.className() + "\"");
                System.out.println("        Name: \"" + func.name() + "\"");
                System.out.println("        Signature: \"" + func.signature() + "\"");
                System.out.println("        Modifiers (" + func.modifiers().size() + "):");
                for(var modifier : func.modifiers()) {
                    System.out.println("          " + modifier);
                }
                if(!FunctionModifiers.isEmptyCode(func.modifiers())) {
                    System.out.println("        Code");
                    System.out.println("          Locals Size: " + func.code().getLocalsSize());
                    System.out.println("          Instructions");
                    for(var instruction : func.code().getInstructions()) {
                        System.out.println("            " + instruction);
                    }
                }
            }
        }
    }

}
