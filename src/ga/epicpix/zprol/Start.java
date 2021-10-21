package ga.epicpix.zprol;

import ga.epicpix.zprol.tokens.Token;
import java.io.IOException;
import java.util.ArrayList;

public class Start {

    public static void main(String[] args) throws IOException {
        if(args.length == 0) {
            throw new IllegalArgumentException("File not specified");
        }
        String fileName = String.join(" ", args);
        ArrayList<Token> tokens = Parser.tokenize(fileName);
        Compiler.compile(tokens);
    }

}
