package ga.epicpix.zprol;

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

public class Parser {

    public static final char[] specialCharacters = "\"{}".toCharArray();

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
                tokens.add(new StructureToken(parser.nextWord()));
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

}
