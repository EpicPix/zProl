package ga.epicpix.zprol;

import static ga.epicpix.zprol.compiler.operation.LanguageOperator.registerOperator;
import static ga.epicpix.zprol.parser.LanguageKeyword.registerKeyword;
import static ga.epicpix.zprol.types.Types.registerPrimitiveType;

public class Loader {

    public static void registerOperators() {
        registerOperator("==", 5);
        registerOperator("|", 10);
        registerOperator(">>", 15);
        registerOperator("<<", 15);
        registerOperator("%", 20);
        registerOperator("+", 20);
        registerOperator("-", 20);
        registerOperator("*", 25);
        registerOperator("/", 25);
        registerOperator("%", 25);
    }

    public static void registerType(int size, boolean unsigned, String descriptor, String... names) {
        registerPrimitiveType(size, unsigned, descriptor, names);
        for(String name : names) {
            registerKeyword(name, "type");
        }
    }

    public static void registerTypes() {
        registerType(0, false, "V", "void");

        registerType(1, false, "B", "int8", "byte");
        registerType(2, false, "S", "int16", "short");
        registerType(4, false, "I", "int32", "int");
        registerType(8, false, "L", "int64", "long");

        registerType(1, true, "uB", "uint8", "ubyte");
        registerType(2, true, "uS", "uint16", "ushort");
        registerType(4, true, "uI", "uint32", "uint");
        registerType(8, true, "uL", "uint64", "ulong");
    }

    public static void registerKeywords() {
        registerKeyword("using", "definition");
        registerKeyword("namespace", "definition");
        registerKeyword("class", "definition");

        registerKeyword("return", "control");

        registerKeyword("native", "function_modifier");
        registerKeyword("inline", "function_modifier");
    }

}
