package ga.epicpix.zprol.compiler.precompiled;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.utils.SeekIterator;

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
                    if(usedOther) throw new TokenLocatedException("Namespace not defined at the top of the file", named);
                    if(pre.namespace != null) throw new TokenLocatedException("Defined namespace for a file multiple times", named);
                    pre.namespace = named.getSingleTokenWithName("DotWord").asWordToken().getWord();
                }else if(named.name.equals("Function")) {
                    pre.functions.add(parseFunction(named));
                }else if(named.name.equals("Field")) {
                    PreField field = new PreField();
                    field.type = named.getSingleTokenWithName("Type").asWordToken().getWord();
                    field.name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();
                    pre.fields.add(field);
                }else if(named.name.equals("Class")) {
                    PreClass clazz = new PreClass();
                    clazz.name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();

                    for(var fieldToken : named.getTokensWithName("ClassField")) {
                        PreField field = new PreField();
                        field.type = fieldToken.getSingleTokenWithName("Type").asWordToken().getWord();
                        field.name = fieldToken.getSingleTokenWithName("Identifier").asWordToken().getWord();
                        clazz.fields.add(field);
                    }

                    for(var methodToken : named.getTokensWithName("ClassMethod")) {
                        clazz.methods.add(parseFunction(methodToken));
                    }

                    pre.classes.add(clazz);
                } else {
                    throw new TokenLocatedException("Unsupported named token \"" + named.name + "\"", named);
                }
            }else {
                throw new TokenLocatedException("Expected named token, this might be a bug in parsing code", token);
            }
        }

        return pre;
    }

    private static PreFunction parseFunction(NamedToken function) {
        var func = new PreFunction();
        if(function.getTokenWithName("FunctionModifiers") != null) {
            for(Token modifier : function.getTokenWithName("FunctionModifiers").tokens) {
                PreFunctionModifiers modifiers = PreFunctionModifiers.getModifier(modifier.asKeywordToken().getWord());
                if(func.modifiers.contains(modifiers)) {
                    throw new TokenLocatedException("Duplicate function modifier: '" + modifiers.getName() + "'", modifier);
                }
                func.modifiers.add(modifiers);
            }
        }
        func.returnType = function.getSingleTokenWithName("Type").asWordHolder().getWord();
        func.name = function.getSingleTokenWithName("Identifier").asWordToken().getWord();
        var paramList = function.getTokenWithName("ParameterList");
        if(paramList != null) {
            for (NamedToken namedToken : paramList.getTokensWithName("Parameter")) {
                PreParameter param = new PreParameter();
                param.type = namedToken.getSingleTokenWithName("Type").asWordHolder().getWord();
                param.name = namedToken.getSingleTokenWithName("Identifier").asWordToken().getWord();
                func.parameters.add(param);
            }
        }
        if(func.hasCode()) {
            if(function.getTokenWithName("Code") == null) {
                throw new TokenLocatedException("Expected code", function);
            }
            for (var a : function.getTokenWithName("Code").getTokensWithName("Statement"))
                func.code.addAll(List.of(a.tokens));
        }else {
            if(function.getTokenWithName("Code") != null) {
                throw new TokenLocatedException("Expected no code", function);
            }
        }
        return func;
    }

}
