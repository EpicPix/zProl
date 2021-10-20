package ga.epicpix.zprol;

import ga.epicpix.zprol.tokens.FieldToken;
import ga.epicpix.zprol.tokens.FunctionToken;
import ga.epicpix.zprol.tokens.ObjectToken;
import ga.epicpix.zprol.tokens.StringToken;
import ga.epicpix.zprol.tokens.StructureToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Parser {

    public static final Pattern nonSpecialCharacters = Pattern.compile("[a-zA-Z0-9_]");

    public static ArrayList<Token> tokenize(String fileName) throws IOException {
        File file = new File(fileName);
        if(!file.exists()) {
            throw new FileNotFoundException(fileName);
        }

        List<String> listLines = Files.readAllLines(file.toPath());
        listLines.removeIf((line) -> line.trim().startsWith("//"));
        listLines.removeIf((line) -> line.trim().isEmpty());
        String[] lines = new String[listLines.size()];
        for(int i = 0; i<lines.length; i++) {
            lines[i] = listLines.get(i).trim();
        }

        DataParser parser = new DataParser(lines);

        ArrayList<Token> tokens = new ArrayList<>();

        String word;
        while((word = parser.nextWord()) != null) {
            if(word.equals("structure")) {
                tokens.add(parseStructure(parser));
            }else if(word.equals("object")) {
                tokens.addAll(parseObject(parser));
            } else if(word.equals("{")) {
                tokens.add(new Token(TokenType.START_DATA));
            } else if(word.equals("}")) {
                tokens.add(new Token(TokenType.END_DATA));
            } else if(word.equals("\"")) {
                tokens.add(new StringToken(parser.nextStringStarted()));
            } else {
                System.out.println("Unknown word: " + word);
            }
        }

        for(Token token : tokens) {
            System.out.println(token);
        }
        return tokens;

    }

    public static StructureToken parseStructure(DataParser parser) {
        String name = parser.nextWord();
        if(!parser.nextWord().equals("{")) {
            throw new RuntimeException("Error 1");
        }
        ArrayList<StructureType> types = new ArrayList<>();
        while(true) {
            if(parser.seekWord().equals("}")) {
                parser.nextWord();
                break;
            }
            String sType = parser.nextType();
            String sName = parser.nextWord();
            types.add(new StructureType(sType, sName));
            String tmp;
            if(!(tmp = parser.nextWord()).equals(";")) {
                throw new RuntimeException("Error 2: " + tmp);
            }
        }
        return new StructureToken(name, types);
    }

    public static ArrayList<Token> parseObject(DataParser parser) {
        String name = parser.nextWord();
        ArrayList<Token> tokens = new ArrayList<>();

        String w = parser.nextWord();
        if(!w.equals("{") && !w.equals("extends")) {
            throw new RuntimeException("Error 3");
        }
        String extendsFrom = "object";
        if(w.equals("extends")) {
            extendsFrom = parser.nextWord();
            if(!parser.nextWord().equals("{")) {
                throw new RuntimeException("Error 5");
            }
        }
        tokens.add(new ObjectToken(name, extendsFrom));

        while(true) {
            if(parser.seekWord().equals("}")) {
                tokens.add(new Token(TokenType.END_OBJECT));
                parser.nextWord();
                break;
            }

            String read = parser.nextWord();
            ArrayList<ParserFlag> flags = new ArrayList<>();
            while(true) {
                boolean found = false;
                for(ParserFlag flag : ParserFlag.values()) {
                    if(flag.isPublicFlag() && flag.name().toLowerCase().equals(read)) {
                        flags.add(flag);
                        read = parser.nextWord();
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    break;
                }
            }

            if(read.equals("field")) {
                String fieldType = parser.nextType();
                String fieldName = parser.nextWord();
                tokens.add(new FieldToken(fieldType, fieldName, flags));
                String tmp = parser.nextWord();
                if(!tmp.equals(";")) {
                    throw new RuntimeException("Error 4: " + tmp);
                }
            }else if(read.equals(name)) {
                ArrayList<ParameterDataType> functionParameters = parser.readParameters();

                String tmp = parser.nextWord();
                if(tmp.equals(";")) {
                    throw new RuntimeException("Constructors must have implementation");
                }else if(!tmp.equals("{")) {
                    throw new RuntimeException("Error 7: " + tmp);
                }

                tokens.add(new FunctionToken("void", "<init>", functionParameters, flags));
            }else if(read.equals("function")) {
                String functionReturn = parser.nextType();
                String functionName = parser.nextWord();
                ArrayList<ParameterDataType> functionParameters = parser.readParameters();

                String tmp = parser.nextWord();
                if(tmp.equals(";")) {
                    flags.add(ParserFlag.NO_IMPLEMENTATION);
                }else if(!tmp.equals("{")) {
                    throw new RuntimeException("Error 6: " + tmp);
                }

                tokens.add(new FunctionToken(functionReturn, functionName, functionParameters, flags));
            }
        }
        return tokens;
    }

}
