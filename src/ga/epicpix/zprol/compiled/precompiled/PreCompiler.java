package ga.epicpix.zprol.compiled.precompiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.tokens.KeywordToken;
import ga.epicpix.zprol.tokens.LongWordToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.TypeToken;
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
                    if(tokens.next().getType() != TokenType.END_LINE) throw new RuntimeException("A processing error has occurred");
                }else if(keyword.equals("import")) {
                    String imported = ((LongWordToken) tokens.next()).word;
                    if(pre.imported.get(imported) != null) {
                        throw new RuntimeException("Imported '" + imported + "' multiple times");
                    }
                    Token t = tokens.next();
                    if(t.getType() == TokenType.END_LINE) {
                        pre.imported.put(imported, imported);
                    }else {
                        String name = ((WordToken) tokens.next()).word;
                        pre.imported.put(imported, name);
                        if(tokens.next().getType() != TokenType.END_LINE) throw new RuntimeException("A processing error has occurred");
                    }
                }else if(keyword.equals("export")) {
                    String export = ((LongWordToken) tokens.next()).word;
                    if(pre.exportName != null) {
                        throw new RuntimeException("Exported file multiple times");
                    }
                    pre.exportName = export;
                    if(tokens.next().getType() != TokenType.END_LINE) throw new RuntimeException("A processing error has occurred");
                }else if(keyword.equals("function")) {
                    String type = ((WordToken) tokens.next()).word;
                    String name = ((WordToken) tokens.next()).word;
                    System.out.println(tokens.next());
                }else {
                    System.out.println("keyword: " + keyword);
                }
            }else {
                System.out.println("token: " + token);
            }
        }

        return pre;
    }

}
