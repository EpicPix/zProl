package ga.epicpix.zprol.operation;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.exceptions.compilation.CompileException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.zld.Language;
import ga.epicpix.zprol.zld.LanguageOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class OperationGenerator {

    public static OperationRoot getOperations(SeekIterator<Token> tokens) {
        Stack<LanguageOperator> cachedOperator = new Stack<>();

        ArrayList<Operation> operations = new ArrayList<>();
        loop:
        while(tokens.hasNext()) {
            Token token = tokens.next();
            if(!(token instanceof NamedToken)) {
                throw new CompileException("Expected NamedToken but got " + token.getClass().getSimpleName(), token);
            }
            NamedToken named = token.asNamedToken();
            switch (named.name) {
                case "Operator" -> {
                    String operatorName = named.tokens[0].asWordToken().getWord();
                    LanguageOperator operator = Language.OPERATORS.get(operatorName);
                    if (operator == null) {
                        throw new CompileException("Unknown operator '" + operatorName + "'", named);
                    }
                    if (cachedOperator.isEmpty()) {
                        cachedOperator.push(operator);
                        continue;
                    }
                    while (!cachedOperator.isEmpty()) {
                        int a = operator.precedence();
                        int b = cachedOperator.peek().precedence();
                        if (a < b) {
                            operations.add(new OperationOperator(cachedOperator.pop()));
                        } else {
                            break;
                        }
                    }
                    cachedOperator.push(operator);
                    continue;
                }
                case "ExpressionParenthesis" -> {
                    Token[] t = new Token[named.tokens.length - 2];
                    System.arraycopy(named.tokens, 1, t, 0, t.length);
                    operations.addAll(getOperations(new SeekIterator<>(t)).getOperations());
                }
                case "FunctionCallStatement", "FunctionCall" -> {
                    String name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();
                    ArrayList<OperationRoot> callOp = new ArrayList<>();
                    if (named.getTokenWithName("ArgumentList") != null) {
                        for (Token argument : named.getTokenWithName("ArgumentList").getTokensWithName("Argument")) {
                            callOp.add(getOperations(new SeekIterator<>(argument.asNamedToken().getTokenWithName("Expression").tokens)));
                        }
                    }
                    operations.add(new OperationCall(name, callOp));
                }
                case "DecimalInteger" -> operations.add(new OperationNumber(OperationNumber.getDecimalInteger(named.tokens[0])));
                case "Identifier" -> operations.add(new OperationField(named.tokens[0].asWordToken().getWord()));
                case "String" -> operations.add(new OperationString(named.getSingleTokenWithName("StringChars").asWordToken().getWord()));
                case "Assignment" -> operations.add(new OperationAssignment(named.getSingleTokenWithName("Identifier").asWordToken().getWord(), getOperations(new SeekIterator<>(named.getTokenWithName("Expression").tokens))));
                default -> throw new NotImplementedException("Not implemented named token in expression '" + named.name + "' " + Arrays.toString(named.tokens));
            }

            while (cachedOperator.size() != 0) {
                if (tokens.hasNext()) {
                    NamedToken t = tokens.seek().asNamedToken();
                    if(!t.name.equals("Operator")) {
                        throw new CompileException("Expected Operator but got " + t.name, t);
                    }
                    String operatorName = t.tokens[0].asWordToken().getWord();
                    LanguageOperator operator = Language.OPERATORS.get(operatorName);
                    if (operator == null) {
                        throw new CompileException("Unknown operator '" + operatorName + "'", t);
                    }
                    int a = operator.precedence();
                    int b = cachedOperator.peek().precedence();
                    if (a > b) {
                        continue loop;
                    }
                }
                operations.add(new OperationOperator(cachedOperator.pop()));
            }
        }

        while(!cachedOperator.isEmpty()) {
            operations.add(new OperationOperator(cachedOperator.pop()));
        }

        return new OperationRoot(operations);
    }

}
