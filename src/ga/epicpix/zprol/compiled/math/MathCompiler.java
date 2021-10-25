package ga.epicpix.zprol.compiled.math;

import ga.epicpix.zprol.Reflection;
import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.compiled.CompiledData;
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

public class MathCompiler {

    private int current = 0;

    public void reset() {
        current = 0;
    }

    private MathOperation compileReference(Token token, SeekIterator<Token> tokens) {
        ArrayList<Token> ref = new ArrayList<>();
        ref.add(token);
        ArrayList<MathOperation> params = new ArrayList<>();
        boolean call = false;
        while(tokens.seek().getType() != TokenType.OPERATOR && tokens.seek().getType() != TokenType.END_LINE) {
            token = tokens.next();
            if(token.getType() == TokenType.OPEN) {
                call = true;
                if(tokens.seek().getType() != TokenType.CLOSE) {
                    ArrayList<MathOperation> op = new ArrayList<>();
                    compile0(2, op, new Stack<>(), tokens);
                    params.add(op.get(0));
                }else {
                    tokens.next();
                    return new MathCall(ref, params);
                }
            }else if(token.getType() == TokenType.COMMA && call) {
                ArrayList<MathOperation> op = new ArrayList<>();
                compile0(2, op, new Stack<>(), tokens);
                params.add(op.get(0));
            }else if(token.getType() == TokenType.COMMA) {
                tokens.previous();
                if(call) {
                    return new MathCall(ref, params);
                }else {
                    return new MathField(ref);
                }
            }else if(token.getType() == TokenType.CLOSE && call) {
                return new MathCall(ref, params);
            }else if(token.getType() == TokenType.CLOSE && !call) {
                tokens.previous();
                return new MathField(ref);
            }else if(!call) {
                ref.add(token);
            }
        }
        return new MathField(ref);
    }

    private void compile0(int type, ArrayList<MathOperation> operations, Stack<ArrayList<MathOperation>> stackOperations, SeekIterator<Token> tokens) {
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
                MathBrackets brackets = new MathBrackets(operations.get(0));
                operations.clear();
                operations.addAll(stackOperations.pop());
                operations.add(brackets);
                return;
            }

            if(token.getType() == TokenType.NUMBER) {
                operations.add(new MathNumber(token));
            }else if(token.getType() == TokenType.WORD) {
                Token next = tokens.seek();
                if(next.getType() == TokenType.ACCESSOR || next.getType() == TokenType.OPEN) {
                    operations.add(compileReference(token, tokens));
                }else {
                    operations.add(new MathNumber(token));
                }
            }else if(token.getType() == TokenType.OPERATOR) {
                OperatorToken operatorToken = (OperatorToken) token;
                String operator = operatorToken.operator;
                int order = MathOrder.getOrder(operator);
                if(order == -1) throw new RuntimeException("Could not determine the order of operation: '" + operator + "'");
                Class<? extends MathOperation> operatorClass = MathOrder.ORDER_TO_CLASS.get(operator);
                if(operatorClass == null) throw new RuntimeException("Could not get the class of operation: '" + operator + "'");
                if(order == 0) {
                    token = tokens.next();
                    MathOperation num = new MathNumber(token);
                    if(token.getType() == TokenType.OPEN) {
                        stackOperations.push(new ArrayList<>(operations));
                        operations.clear();
                        compile0(1, operations, stackOperations, tokens);
                        num = operations.get(operations.size() - 1);
                        operations.remove(operations.size() - 1);
                    }
                    MathOperation last = operations.get(operations.size() - 1);
                    operations.remove(operations.size() - 1);
                    if(last instanceof MathNumber) {
                        operations.add(Reflection.createInstance(operatorClass, new Class[] {MathOperation.class, MathOperation.class}, last, new MathNumber(token)));
                    }else {
                        if(last instanceof MathBrackets) {
                            operations.add(new MathMultiply(last, new MathNumber(token)));
                        }else {
                            operations.add(Reflection.createInstance(last.getClass(), new Class[] {MathOperation.class, MathOperation.class}, last.operation, Reflection.createInstance(operatorClass, new Class[] {MathOperation.class, MathOperation.class}, last.number, num)));
                        }
                    }
                }else if(order == 1) {
                    token = tokens.next();
                    MathOperation op = new MathNumber(token);
                    MathOperation last;
                    if(token.getType() == TokenType.OPEN) {
                        stackOperations.push(new ArrayList<>(operations));
                        operations.clear();
                        compile0(1, operations, stackOperations, tokens);
                        op = operations.get(operations.size() - 1);
                        operations.remove(operations.size() - 1);
                    }else if(token.getType() == TokenType.WORD) {
                        Token after = tokens.seek();
                        if(after.getType() == TokenType.ACCESSOR || after.getType() == TokenType.OPEN) {
                            op = compileReference(token, tokens);
                        }
                    }
                    last = operations.get(operations.size() - 1);
                    operations.remove(operations.size() - 1);
                    operations.add(Reflection.createInstance(operatorClass, new Class[] {MathOperation.class, MathOperation.class}, last, op));
                }
            }else if(token.getType() == TokenType.OPEN) {
                stackOperations.push(new ArrayList<>(operations));
                operations.clear();
                compile0(1, operations, stackOperations, tokens);
            }
        }
    }

    public void compile(CompiledData data, Bytecode bytecode, SeekIterator<Token> tokens) {
        ArrayList<MathOperation> operations = new ArrayList<>();
        Stack<ArrayList<MathOperation>> stackOperations = new Stack<>();
        compile0(0, operations, stackOperations, tokens);
        File dot = new File("math.dot");
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(dot));
            out.write("digraph Math {\n");
            for(MathOperation op : operations) {
                out.write("    root -> op" + current + "\n");
                generateDotFile(op, out);
            }
            out.write("}");
            out.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        printOperations(operations.get(0));
    }

    private void printOperations(MathOperation operation) {
        if(operation instanceof MathNumber) {
            Token number = ((MathNumber) operation).number;
            if(number instanceof NumberToken) {
                System.out.println("push " + ((NumberToken) number).number);
            }else if(number instanceof WordToken) {
                System.out.println("push " + ((WordToken) number).word);
            }
            return;
        }else if(operation instanceof MathBrackets) {
            printOperations(operation.operation);
            return;
        }else if(operation instanceof MathField) {
            MathField ref = (MathField) operation;
            System.out.println("push field " + Token.toFriendlyString(ref.reference));
            return;
        }else if(operation instanceof MathCall) {
            MathCall call = (MathCall) operation;
            for(MathOperation op : call.parameters) {
                printOperations(op);
            }
            System.out.println("call " + Token.toFriendlyString(call.reference));
            return;
        }
        printOperations(operation.operation);
        printOperations(operation.number);
        String op = MathOrder.classToOperation(operation.getClass());
        if(op != null) {
            System.out.println(MathOrder.ORDER_TO_NAME.get(op));
        }
    }

    private void generateDotFile(MathOperation operation, BufferedWriter writer) throws IOException {
        if(operation instanceof MathNumber) {
            Token number = ((MathNumber) operation).number;
            if(number instanceof NumberToken) {
                writer.write("    op" + current + " [label=" + ((NumberToken) number).number + "]\n");
            }else if(number instanceof WordToken) {
                writer.write("    op" + current + " [label=" + ((WordToken) number).word + "]\n");
            }
            current++;
            return;
        }else if(operation instanceof MathField) {
            MathField field = (MathField) operation;
            writer.write("    op" + current + " [label=\"" + Token.toFriendlyString(field.reference) + "\"]\n");
            current++;
            return;
        }else if(operation instanceof MathCall) {
            MathCall call = (MathCall) operation;
            int c = current;
            writer.write("    op" + current + " [label=\"" + Token.toFriendlyString(call.reference) + "()\"]\n");
            current++;
            for(MathOperation op : call.parameters) {
                writer.write("    op" + c + " -> op" + current + "\n");
                generateDotFile(op, writer);
                current++;
            }
            return;
        }else if(operation instanceof MathBrackets) {
            writer.write("    op" + current + " [label=\"()\"]\n");
            int c = current;
            current++;
            writer.write("    op" + c + " -> op" + current + "\n");
            generateDotFile(operation.operation, writer);
            return;
        }else if(operation instanceof MathAdd) {
            writer.write("    op" + current + " [label=\"" + MathOrder.classToOperation(operation.getClass()) + "\"]\n");
        }
        int c = current;
        current++;
        writer.write("    op" + c + " -> op" + current + "\n");
        generateDotFile(operation.operation, writer);
        current++;
        writer.write("    op" + c + " -> op" + current + "\n");
        generateDotFile(operation.number, writer);
    }
}
