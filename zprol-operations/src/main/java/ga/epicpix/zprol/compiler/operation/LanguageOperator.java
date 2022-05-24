package ga.epicpix.zprol.compiler.operation;

import java.util.HashMap;

public record LanguageOperator(String operator, int precedence) {

    public static final HashMap<String, LanguageOperator> OPERATORS = new HashMap<>();

    public static LanguageOperator registerOperator(String operator, int precedence) {
        var op = new LanguageOperator(operator, precedence);
        OPERATORS.put(operator, op);
        return op;
    }

}
