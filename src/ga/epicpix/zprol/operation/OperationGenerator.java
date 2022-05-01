package ga.epicpix.zprol.operation;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.NumberToken;
import ga.epicpix.zprol.parser.tokens.OperatorToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.zld.LanguageOperator;

import java.util.ArrayList;
import java.util.Stack;
import java.util.stream.Collectors;

public class OperationGenerator {

    public static ArrayList<Operation> getOperations(SeekIterator<Token> tokens) {
        Stack<LanguageOperator> cachedOperator = new Stack<>();

        ArrayList<Operation> operations = new ArrayList<>();
        loop:
        while(tokens.hasNext()) {
            Token token = tokens.next();
            if(token instanceof NumberToken num) {
                operations.add(new OperationNumber(num));
                while(cachedOperator.size() != 0) {
                    if(tokens.hasNext()) {
                        int a = tokens.seek().asOperatorToken().operator.precedence();
                        int b = cachedOperator.peek().precedence();
                        if(a > b) {
                            continue loop;
                        }
                    }
                    operations.add(new OperationOperator(cachedOperator.pop()));
                }
            }else if(token instanceof OperatorToken operator) {
                if(cachedOperator.isEmpty()) {
                    cachedOperator.push(operator.operator);
                    continue;
                }
                while(!cachedOperator.isEmpty()) {
                    int a = operator.operator.precedence();
                    int b = cachedOperator.peek().precedence();
                    if (a < b) {
                        operations.add(new OperationOperator(cachedOperator.pop()));
                    }else {
                        break;
                    }
                }
                cachedOperator.push(operator.operator);
            }else if(token instanceof NamedToken named) {
                if(named.name.equals("ExpressionParenthesis")) {
                    Token[] t = new Token[named.tokens.length - 2];
                    System.arraycopy(named.tokens, 1, t, 0, t.length);
                    operations.addAll(getOperations(new SeekIterator<>(t)));
                }
            }
        }

        while(!cachedOperator.isEmpty()) {
            operations.add(new OperationOperator(cachedOperator.pop()));
        }

        return operations;
    }

}
