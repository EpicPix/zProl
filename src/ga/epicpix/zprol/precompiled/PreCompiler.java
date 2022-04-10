package ga.epicpix.zprol.precompiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.exceptions.InvalidOperationException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.ParsedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import java.util.ArrayList;

public class PreCompiler {

    public static PreCompiledData preCompile(String sourceFile, ArrayList<Token> pTokens) {
        PreCompiledData pre = new PreCompiledData();
        pre.sourceFile = sourceFile;

        SeekIterator<Token> tokens = new SeekIterator<>(pTokens);
        boolean usedOther = false;
        while(tokens.hasNext()) {
            Token token = tokens.next();
            if(!(token.getType() == TokenType.PARSED && ((ParsedToken) token).name.equals("Namespace"))) usedOther = true;
            if(token.getType() == TokenType.PARSED) {
                ParsedToken parsed = (ParsedToken) token;
                ArrayList<Token> ts = parsed.tokens;
                if(parsed.name.equals("Using")) {
                    pre.using.add(ts.get(1).asWordToken().getWord());
                }else if(parsed.name.equals("Namespace")) {
                    if(usedOther) throw new RuntimeException("Namespace not defined at the top of the file");
                    String namespace = ts.get(1).asWordToken().getWord();
                    if(pre.namespace != null) throw new RuntimeException("Defined namespace for a file multiple times");
                    pre.namespace = namespace;
                }else if(parsed.name.equals("Function")) {
                    PreFunction func = new PreFunction();
                    func.returnType = ts.get(0).asWordHolder().getWord();
                    func.name = ts.get(1).asWordToken().getWord();
                    for(NamedToken namedToken : parsed.getTokensWithName("FunctionParameter")) {
                        var paramTokens = namedToken.tokens;
                        PreParameter param = new PreParameter();
                        param.type = paramTokens[0].asWordHolder().getWord();
                        param.name = paramTokens[1].asWordToken().getWord();
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
                        } else if(opens == 0 && t.getType() == TokenType.PARSED) break;
                    }
                    pre.functions.add(func);
                }else {
                    System.out.println("token: " + parsed);
                }
            }else {
                System.out.println("token: " + token);
            }
        }

        return pre;
    }

}
