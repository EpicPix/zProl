package ga.epicpix.zprol.operation;

import ga.epicpix.zprol.Reflection;
import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.PrimitiveType;
import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.operation.Operation.OperationBrackets;
import ga.epicpix.zprol.operation.Operation.OperationCall;
import ga.epicpix.zprol.operation.Operation.OperationCast;
import ga.epicpix.zprol.operation.Operation.OperationField;
import ga.epicpix.zprol.operation.Operation.OperationNumber;
import ga.epicpix.zprol.operation.Operation.OperationString;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.parser.tokens.NumberToken;
import ga.epicpix.zprol.parser.tokens.OperatorToken;
import ga.epicpix.zprol.parser.tokens.StringToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import java.util.ArrayList;
import java.util.Stack;

public class OperationCompiler {

    private int current = 0;

    private Operation compileReference(Token token, SeekIterator<Token> tokens, CompiledData data) {
        ArrayList<Token> ref = new ArrayList<>();
        ref.add(token);
        ArrayList<Operation> params = new ArrayList<>();
        boolean call = false;
        while(tokens.seek().getType() != TokenType.OPERATOR && tokens.seek().getType() != TokenType.END_LINE) {
            token = tokens.next();
            if(token.getType() == TokenType.OPEN) {
                call = true;
                if(tokens.seek().getType() != TokenType.CLOSE) {
                    ArrayList<Operation> op = new ArrayList<>();
                    if(tokens.seek().getType() == TokenType.NAMED) compile0(2, op, new Stack<>(), new SeekIterator<>(tokens.next().asNamedToken().tokens), data);
                    else compile0(2, op, new Stack<>(), tokens, data);
                    params.add(op.get(0));
                }else {
                    tokens.next();
                    return new OperationCall(ref, params);
                }
            }else if(token.getType() == TokenType.COMMA && call) {
                ArrayList<Operation> op = new ArrayList<>();
                if(tokens.seek().getType() == TokenType.NAMED) compile0(2, op, new Stack<>(), new SeekIterator<>(tokens.next().asNamedToken().tokens), data);
                else compile0(2, op, new Stack<>(), tokens, data);
                params.add(op.get(0));
            }else if(token.getType() == TokenType.COMMA) {
                tokens.back();
                if(call) {
                    return new OperationCall(ref, params);
                }else {
                    return new OperationField(ref);
                }
            }else if(token.getType() == TokenType.CLOSE && call) {
                return new OperationCall(ref, params);
            }else if(token.getType() == TokenType.CLOSE && !call) {
                tokens.back();
                return new OperationField(ref);
            }else if(!call) {
                ref.add(token);
            }
        }
        return new OperationField(ref);
    }

    public void compile0(int type, ArrayList<Operation> operations, Stack<ArrayList<Operation>> stackOperations, SeekIterator<Token> tokens, CompiledData data) {
        Token token;
        while(tokens.hasNext()) {
            if((tokens.seek().getType() == TokenType.COMMA || tokens.seek().getType() == TokenType.CLOSE) && type == 2) {
                return;
            }
            token = tokens.next();
            if(token.getType() == TokenType.END_LINE && type == 0) {
                return;
            }
            if(token.getType() == TokenType.CLOSE && type == 1) {
                OperationBrackets brackets = new OperationBrackets(operations.get(0));
                operations.clear();
                if(stackOperations.size() != 0) {
                    operations.addAll(stackOperations.pop());
                }
                operations.add(brackets);
                return;
            }

            if(token.getType() == TokenType.NUMBER) {
                operations.add(new OperationNumber((NumberToken) token));
            }else if(token.getType() == TokenType.STRING) {
                operations.add(new OperationString((StringToken) token));
            }else if(token.getType() == TokenType.WORD) {
                operations.add(compileReference(token, tokens, data));
            }else if(token.getType() == TokenType.OPERATOR) {
                OperatorToken operatorToken = (OperatorToken) token;
                String operator = operatorToken.operator;
                int order = OperationOrder.getOrder(operator);
                if(order == -1) throw new RuntimeException("Could not determine the order of operation: '" + operator + "'");
                Class<? extends Operation> operatorClass = OperationOrder.ORDER_TO_CLASS.get(operator);
                if(operatorClass == null) throw new RuntimeException("Could not get the class of operation: '" + operator + "'");
                token = tokens.next();
                Operation op;
                Operation last;
                if(token.getType() == TokenType.OPEN) {
                    stackOperations.push(new ArrayList<>(operations));
                    operations.clear();
                    compile0(1, operations, stackOperations, tokens, data);
                    op = operations.get(operations.size() - 1);
                    operations.remove(operations.size() - 1);
                }else if(token.getType() == TokenType.WORD) {
                    op = compileReference(token, tokens, data);
                }else {
                    op = new OperationNumber((NumberToken) token);
                }
                last = operations.get(operations.size() - 1);
                operations.remove(operations.size() - 1);

                int lOrder = OperationOrder.getOrder(OperationOrder.classToOperation(last.getClass()));

                if(lOrder == -1) {
                    operations.add(Reflection.createInstance(operatorClass, last, op));
                }else {
                    if(order < lOrder) {
                        operations.add(Reflection.createInstance(operatorClass, last, op));
                    }else {
                        operations.add(Reflection.createInstance(last.getClass(), last.left, Reflection.createInstance(operatorClass, last.right, op)));
                    }
                }
            }else if(token.getType() == TokenType.OPEN) {
                int current = tokens.currentIndex();
                try {
                    PrimitiveType t = data.resolveType(tokens.next().asWordHolder().getWord());
                    operations.add(new OperationCast(t));
                    if(tokens.next().getType() != TokenType.CLOSE) {
                        throw new RuntimeException("Missing ')'");
                    }
                } catch(UnknownTypeException | ClassCastException unk) {
                    tokens.setIndex(current);
                    stackOperations.push(new ArrayList<>(operations));
                    operations.clear();
                    compile0(1, operations, stackOperations, tokens, data);
                }
            }
            if(operations.size() >= 2) {
                Operation o = operations.get(operations.size() - 2);
                if(o instanceof OperationCast c) {
                    c.left = operations.get(operations.size() - 1);
                    operations.remove(operations.size() - 1);
                }
            }
        }
    }

    public Operation compile(CompiledData data, SeekIterator<Token> tokens) {
        current = 0;
        ArrayList<Operation> operations = new ArrayList<>();
        Stack<ArrayList<Operation>> stackOperations = new Stack<>();
        compile0(0, operations, stackOperations, tokens, data);
        if(Boolean.parseBoolean(System.getProperty("DEBUG"))) {
            System.out.println(operations);
            printOperations(operations.get(0));
        }
        return operations.get(0);
    }

    private void printOperations(Operation operation) {
        if(operation instanceof OperationNumber) {
            NumberToken number = ((OperationNumber) operation).number;
            System.out.println("push " + number.number);
            return;
        }else if(operation instanceof OperationString) {
            StringToken string = ((OperationString) operation).string;
            System.out.println("push " + string.getData());
            return;
        }else if(operation instanceof OperationBrackets) {
            printOperations(operation.left);
            return;
        }else if(operation instanceof OperationCast) {
            printOperations(operation.left);
            System.out.println("cast " + ((OperationCast) operation).type);
            return;
        }else if(operation instanceof OperationField ref) {
            System.out.println("push field " + Token.toFriendlyString(ref.reference));
            return;
        }else if(operation instanceof OperationCall call) {
            for(Operation op : call.parameters) {
                printOperations(op);
            }
            System.out.println("call " + Token.toFriendlyString(call.reference));
            return;
        }
        printOperations(operation.left);
        printOperations(operation.right);
        String op = OperationOrder.classToOperation(operation.getClass());
        if(op != null) {
            System.out.println(OperationOrder.ORDER_TO_NAME.get(op));
        }
    }

}
