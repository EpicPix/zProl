package ga.epicpix.zprol.compiled.operation;

import ga.epicpix.zprol.Reflection;
import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.operation.Operation.OperationBrackets;
import ga.epicpix.zprol.compiled.operation.Operation.OperationCall;
import ga.epicpix.zprol.compiled.operation.Operation.OperationField;
import ga.epicpix.zprol.compiled.operation.Operation.OperationNumber;
import ga.epicpix.zprol.tokens.NumberToken;
import ga.epicpix.zprol.tokens.OperatorToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.WordToken;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class OperationCompiler {

    private int current = 0;

    public void reset() {
        current = 0;
    }

    private Operation compileReference(Token token, SeekIterator<Token> tokens) {
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
                    compile0(2, op, new Stack<>(), tokens);
                    params.add(op.get(0));
                }else {
                    tokens.next();
                    return new OperationCall(ref, params);
                }
            }else if(token.getType() == TokenType.COMMA && call) {
                ArrayList<Operation> op = new ArrayList<>();
                compile0(2, op, new Stack<>(), tokens);
                params.add(op.get(0));
            }else if(token.getType() == TokenType.COMMA) {
                tokens.previous();
                if(call) {
                    return new OperationCall(ref, params);
                }else {
                    return new OperationField(ref);
                }
            }else if(token.getType() == TokenType.CLOSE && call) {
                return new OperationCall(ref, params);
            }else if(token.getType() == TokenType.CLOSE && !call) {
                tokens.previous();
                return new OperationField(ref);
            }else if(!call) {
                ref.add(token);
            }
        }
        return new OperationField(ref);
    }

    private void compile0(int type, ArrayList<Operation> operations, Stack<ArrayList<Operation>> stackOperations, SeekIterator<Token> tokens) {
        Token token;
        while(true) {
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
                operations.addAll(stackOperations.pop());
                operations.add(brackets);
                return;
            }

            if(token.getType() == TokenType.NUMBER) {
                operations.add(new OperationNumber((NumberToken) token));
            }else if(token.getType() == TokenType.WORD) {
                operations.add(compileReference(token, tokens));
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
                    compile0(1, operations, stackOperations, tokens);
                    op = operations.get(operations.size() - 1);
                    operations.remove(operations.size() - 1);
                }else if(token.getType() == TokenType.WORD) {
                    op = compileReference(token, tokens);
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
                stackOperations.push(new ArrayList<>(operations));
                operations.clear();
                compile0(1, operations, stackOperations, tokens);
            }
        }
    }

    public Operation compile(CompiledData data, Bytecode bytecode, SeekIterator<Token> tokens) {
        ArrayList<Operation> operations = new ArrayList<>();
        Stack<ArrayList<Operation>> stackOperations = new Stack<>();
        compile0(0, operations, stackOperations, tokens);
        if(Boolean.parseBoolean(System.getProperty("DEBUG"))) {
            File dot = new File("math.dot");
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(dot));
                out.write("digraph Math {\n");
                out.write("    root -> op" + current + "\n");
                generateDotFile(operations.get(0), out);
                out.write("}");
                out.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
            printOperations(operations.get(0));
        }
        return operations.get(0);
    }

    private void printOperations(Operation operation) {
        if(operation instanceof OperationNumber) {
            Token number = ((OperationNumber) operation).number;
            if(number instanceof NumberToken) {
                System.out.println("push " + ((NumberToken) number).number);
            }else if(number instanceof WordToken) {
                System.out.println("push " + ((WordToken) number).word);
            }
            return;
        }else if(operation instanceof OperationBrackets) {
            printOperations(operation.left);
            return;
        }else if(operation instanceof OperationField) {
            OperationField ref = (OperationField) operation;
            System.out.println("push field " + Token.toFriendlyString(ref.reference));
            return;
        }else if(operation instanceof OperationCall) {
            OperationCall call = (OperationCall) operation;
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

    private void generateDotFile(Operation operation, BufferedWriter writer) throws IOException {
        if(operation instanceof OperationNumber) {
            Token number = ((OperationNumber) operation).number;
            if(number instanceof NumberToken) {
                writer.write("    op" + current + " [label=" + ((NumberToken) number).number + "]\n");
            }else if(number instanceof WordToken) {
                writer.write("    op" + current + " [label=" + ((WordToken) number).word + "]\n");
            }
            current++;
            return;
        }else if(operation instanceof OperationField) {
            OperationField field = (OperationField) operation;
            writer.write("    op" + current + " [label=\"" + Token.toFriendlyString(field.reference) + "\"]\n");
            current++;
            return;
        }else if(operation instanceof OperationCall) {
            OperationCall call = (OperationCall) operation;
            int c = current;
            writer.write("    op" + current + " [label=\"" + Token.toFriendlyString(call.reference) + "()\"]\n");
            current++;
            for(Operation op : call.parameters) {
                writer.write("    op" + c + " -> op" + current + "\n");
                generateDotFile(op, writer);
                current++;
            }
            return;
        }else if(operation instanceof OperationBrackets) {
            writer.write("    op" + current + " [label=\"()\"]\n");
            int c = current;
            current++;
            writer.write("    op" + c + " -> op" + current + "\n");
            generateDotFile(operation.left, writer);
            return;
        }else {
            writer.write("    op" + current + " [label=\"" + OperationOrder.classToOperation(operation.getClass()) + "\"]\n");
        }
        int c = current;
        current++;
        writer.write("    op" + c + " -> op" + current + "\n");
        generateDotFile(operation.left, writer);
        current++;
        writer.write("    op" + c + " -> op" + current + "\n");
        generateDotFile(operation.right, writer);
    }
}
