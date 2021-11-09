package ga.epicpix.zprol.compiled.precompiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.tokens.KeywordToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.WordToken;
import java.util.ArrayList;

public class PreCompiler {

    public static PreCompiledData preCompile(ArrayList<Token> pTokens) {
        System.out.println(" *** Tokens *** ");
        System.out.println(Token.toFriendlyString(pTokens));
        System.out.println(" *** Tokens *** ");
        PreCompiledData pre = new PreCompiledData();

        SeekIterator<Token> tokens = new SeekIterator<>(pTokens);
        while(tokens.hasNext()) {
            Token token = tokens.next();
            if(token.getType() == TokenType.KEYWORD) {
                String keyword = ((KeywordToken) token).keyword;
                if(keyword.equals("typedef")) {
                    String type = ((WordToken) tokens.next()).word;
                    String name = ((WordToken) tokens.next()).word;
                    if(pre.typedef.get(name) != null) {
                        throw new RuntimeException("Redefined typedef definition");
                    }
                    pre.typedef.put(name, type);
                }
            }
        }

        return pre;
    }

}
