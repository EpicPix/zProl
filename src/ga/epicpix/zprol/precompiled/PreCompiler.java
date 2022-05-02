package ga.epicpix.zprol.precompiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.exceptions.CompileException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import java.util.ArrayList;
import java.util.List;

public class PreCompiler {

    public static PreCompiledData preCompile(String sourceFile, ArrayList<Token> pTokens) {
        PreCompiledData pre = new PreCompiledData();
        pre.sourceFile = sourceFile;

        SeekIterator<Token> tokens = new SeekIterator<>(pTokens);
        boolean usedOther = false;
        while(tokens.hasNext()) {
            Token token = tokens.next();
            if(!(token.getType() == TokenType.NAMED && token.asNamedToken().name.equals("Namespace"))) usedOther = true;
            if(token.getType() == TokenType.NAMED) {
                var named = token.asNamedToken();
                var ts = named.tokens;
                if(named.name.equals("Using")) {
                    pre.using.add(ts[1].asWordToken().getWord());
                }else if(named.name.equals("Namespace")) {
                    if(usedOther) throw new RuntimeException("Namespace not defined at the top of the file");
                    String namespace = ts[1].asWordToken().getWord();
                    if(pre.namespace != null) throw new RuntimeException("Defined namespace for a file multiple times");
                    pre.namespace = namespace;
                }else if(named.name.equals("Function")) {
                    PreFunction func = new PreFunction();
                    if(named.getTokenWithName("FunctionModifiers") != null) {
                        for(Token modifier : named.getTokenWithName("FunctionModifiers").tokens) {
                            PreFunctionModifiers modifiers = PreFunctionModifiers.getModifier(modifier.asKeywordToken().getWord());
                            if(func.modifiers.contains(modifiers)) {
                                throw new CompileException("Duplicate function modifier: '" + modifiers.getName() + "'");
                            }
                            func.modifiers.add(modifiers);
                        }
                    }
                    func.returnType = ts[1].asWordHolder().getWord();
                    func.name = named.getTokenWithName("Identifier").tokens[0].asWordToken().getWord();
                    var paramList = named.getTokenWithName("ParameterList");
                    if(paramList != null) {
                        for (NamedToken namedToken : paramList.getTokensWithName("Parameter")) {
                            var paramTokens = namedToken.tokens;
                            PreParameter param = new PreParameter();
                            param.type = paramTokens[0].asWordHolder().getWord();
                            param.name = namedToken.getTokenWithName("Identifier").tokens[0].asWordToken().getWord();
                            func.parameters.add(param);
                        }
                    }
                    if(func.hasCode()) {
                        if(named.getTokenWithName("Code") == null) {
                            throw new CompileException("Expected code");
                        }
                        for (var a : named.getTokenWithName("Code").getTokensWithName("Statement"))
                            func.code.addAll(List.of(a.tokens));
                    }else {
                        if(named.getTokenWithName("Code") != null) {
                            throw new CompileException("Expected no code");
                        }
                    }
                    pre.functions.add(func);
                }else {
                    System.out.println("token: " + named);
                }
            }else {
                throw new CompileException("Expected Named token, this might be a bug in parsing code");
            }
        }

        return pre;
    }

}
