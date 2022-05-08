package ga.epicpix.zprol.precompiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.exceptions.compilation.CompileException;
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
                if(named.name.equals("Using")) {
                    pre.using.add(named.getSingleTokenWithName("DotWord").asWordToken().getWord());
                }else if(named.name.equals("Namespace")) {
                    if(usedOther) throw new CompileException("Namespace not defined at the top of the file", named);
                    if(pre.namespace != null) throw new CompileException("Defined namespace for a file multiple times", named);
                    pre.namespace = named.getSingleTokenWithName("DotWord").asWordToken().getWord();
                }else if(named.name.equals("Function")) {
                    PreFunction func = new PreFunction();
                    if(named.getTokenWithName("FunctionModifiers") != null) {
                        for(Token modifier : named.getTokenWithName("FunctionModifiers").tokens) {
                            PreFunctionModifiers modifiers = PreFunctionModifiers.getModifier(modifier.asKeywordToken().getWord());
                            if(func.modifiers.contains(modifiers)) {
                                throw new CompileException("Duplicate function modifier: '" + modifiers.getName() + "'", modifier);
                            }
                            func.modifiers.add(modifiers);
                        }
                    }
                    func.returnType = named.getSingleTokenWithName("Type").asWordHolder().getWord();
                    func.name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();
                    var paramList = named.getTokenWithName("ParameterList");
                    if(paramList != null) {
                        for (NamedToken namedToken : paramList.getTokensWithName("Parameter")) {
                            PreParameter param = new PreParameter();
                            param.type = namedToken.getSingleTokenWithName("Type").asWordHolder().getWord();
                            param.name = namedToken.getSingleTokenWithName("Identifier").asWordToken().getWord();
                            func.parameters.add(param);
                        }
                    }
                    if(func.hasCode()) {
                        if(named.getTokenWithName("Code") == null) {
                            throw new CompileException("Expected code", named);
                        }
                        for (var a : named.getTokenWithName("Code").getTokensWithName("Statement"))
                            func.code.addAll(List.of(a.tokens));
                    }else {
                        if(named.getTokenWithName("Code") != null) {
                            throw new CompileException("Expected no code", named);
                        }
                    }
                    pre.functions.add(func);
                }else if(named.name.equals("Class")) {
                    PreClass clazz = new PreClass();
                    clazz.name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();

                    for(var fieldToken : named.getTokensWithName("ClassField")) {
                        PreField field = new PreField();
                        field.type = fieldToken.getSingleTokenWithName("Type").asWordToken().getWord();
                        field.name = fieldToken.getSingleTokenWithName("Identifier").asWordToken().getWord();
                        clazz.fields.add(field);
                    }

                    pre.classes.add(clazz);
                } else {
                    throw new CompileException("Unsupported named token \"" + named.name + "\"", named);
                }
            }else {
                throw new CompileException("Expected named token, this might be a bug in parsing code", token);
            }
        }

        return pre;
    }

}
