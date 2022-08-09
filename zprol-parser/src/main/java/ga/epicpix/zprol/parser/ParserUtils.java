package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.tokens.LexerToken;

import java.math.BigInteger;

public class ParserUtils {

    public static BigInteger getInteger(LexerToken token) {
        String raw = token.toStringRaw();
        if(raw.startsWith("0x")) {
            return new BigInteger(token.toStringRaw().substring(2), 16);
        }
        return new BigInteger(token.toStringRaw(), 10);
    }

}
