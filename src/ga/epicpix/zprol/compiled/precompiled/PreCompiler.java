package ga.epicpix.zprol.compiled.precompiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.exceptions.InvalidOperationException;
import ga.epicpix.zprol.exceptions.ParserException;
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
                    PreFunction func = new PreFunction();
                    func.returnType = ((WordToken) tokens.next()).word;
                    func.name = ((WordToken) tokens.next()).word;
                    if(tokens.next().getType() != TokenType.OPEN) throw new RuntimeException("A processing error has occurred");
                    while(tokens.seek().getType() != TokenType.CLOSE) {
                        PreParameter param = new PreParameter();
                        param.type = ((WordToken) tokens.next()).word;
                        param.name = ((WordToken) tokens.next()).word;
                        if(tokens.seek().getType() == TokenType.COMMA) tokens.next();
                        func.parameters.add(param);
                    }
                    int opens = 0;
                    while(true) {
                        Token t = tokens.next();
                        func.code.add(t);

                        if(t.getType() == TokenType.OPEN_SCOPE) {
                            opens++;
                        } else if(t.getType() == TokenType.CLOSE_SCOPE) {
                            if(opens == 0) throw new InvalidOperationException("Function closed too much");
                            opens--;
                            if(opens == 0) break;
                        } else if(opens == 0 && t.getType() == TokenType.END_LINE) break;
                    }
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
