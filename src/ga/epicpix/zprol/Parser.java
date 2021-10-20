package ga.epicpix.zprol;

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
                tokens.add(parseObject(parser));
            } else if(word.equals("{")) {
                tokens.add(new Token(TokenType.START_CODE));
            } else if(word.equals("}")) {
                tokens.add(new Token(TokenType.END_CODE));
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

    public static ObjectToken parseObject(DataParser parser) {
        String name = parser.nextWord();
        if(!parser.nextWord().equals("{")) {
            throw new RuntimeException("Error 3");
        }

        while(true) {
            if(parser.seekWord().equals("}")) {
                parser.nextWord();
                break;
            }

            String read = parser.nextWord();
            ArrayList<String> flags = new ArrayList<>();
            while(true) {
                if(read.equals("internal")) {
                    flags.add("internal");
                    read = parser.nextWord();
                }else {
                    break;
                }
            }

            if(read.equals("field")) {
                throw new UnsupportedOperationException("Cannot read fields yet: " + flags);
            }else if(read.equals(name)) {
                throw new UnsupportedOperationException("Cannot read constructors yet: " + flags);
            }else if(read.equals("function")) {
                throw new UnsupportedOperationException("Cannot read function yet: " + flags);
            }

            String tmp;
            if(!(tmp = parser.nextWord()).equals(";")) {
                throw new RuntimeException("Error 4: " + tmp);
            }
        }
        return new ObjectToken(name);
    }

}
