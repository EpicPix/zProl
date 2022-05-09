package ga.epicpix.zprol.compiler.operation;

import java.util.HashMap;

public record LanguageOperator(String operator, int precedence) {

    public static final HashMap<String, LanguageOperator> OPERATORS = new HashMap<>();

}
