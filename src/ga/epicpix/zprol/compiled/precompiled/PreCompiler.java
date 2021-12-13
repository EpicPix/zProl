package ga.epicpix.zprol.compiled.precompiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.exceptions.InvalidOperationException;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.tokens.KeywordToken;
import ga.epicpix.zprol.tokens.LongWordToken;
import ga.epicpix.zprol.tokens.ParsedToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.TypeToken;
import ga.epicpix.zprol.tokens.WordToken;
import java.util.ArrayList;

public class PreCompiler {

    public static PreCompiledData preCompile(ArrayList<Token> pTokens) {
//        System.out.println(" *** Tokens *** ");
//        System.out.println(Token.toFriendlyString(pTokens));
//        System.out.println(" *** Tokens *** ");
        PreCompiledData pre = new PreCompiledData();

        SeekIterator<Token> tokens = new SeekIterator<>(pTokens);
        while(tokens.hasNext()) {
            Token token = tokens.next();
            if(token.getType() == TokenType.PARSED) {
                ParsedToken parsed = (ParsedToken) token;
                ArrayList<Token> ts = parsed.tokens;
                if(parsed.name.equals("Typedef")) {
                    String type = ts.get(1).asWordToken().word;
                    String name = ts.get(2).asWordToken().word;
                    if(pre.typedef.get(name) != null) throw new RuntimeException("Redefined typedef definition");
                    pre.typedef.put(name, type);
                }else if(parsed.name.equals("Import")) {
                    String imported = ts.get(1).asLongWordToken().word;
                    if(pre.imported.get(imported) != null) throw new RuntimeException("Imported '" + imported + "' multiple times");
                    pre.imported.put(imported, imported);
                }else if(parsed.name.equals("ImportAs")) {
                    String imported = ts.get(1).asLongWordToken().word;
                    if(pre.imported.get(imported) != null) throw new RuntimeException("Imported '" + imported + "' multiple times");
                    String as = ts.get(3).asWordToken().word;
                    pre.imported.put(imported, as);
                }else if(parsed.name.equals("Export")) {
                    String exported = ts.get(1).asLongWordToken().word;
                    if(pre.exportName != null) throw new RuntimeException("Exported file multiple times");
                    pre.exportName = exported;
                }else if(parsed.name.equals("FunctionEmpty")) {
                    PreFunction func = new PreFunction();
                    func.returnType = ts.get(0).asWordToken().word;
                    func.name = ts.get(1).asWordToken().word;
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
                    pre.functions.add(func);
                }else if(parsed.name.equals("FunctionParameters")) {
                    PreFunction func = new PreFunction();
                    func.returnType = ts.get(0).asWordToken().word;
                    func.name = ts.get(1).asWordToken().word;
                    int paramCount = (ts.size() - 3) / 3;
                    for(int i = 0; i<paramCount; i++) {
                        PreParameter param = new PreParameter();
                        param.type = ts.get(i * 3 + 3).asWordToken().word;
                        param.name = ts.get(i * 3 + 4).asWordToken().word;
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
                }else if(parsed.name.equals("Structure")) {
                    PreStructure structure = new PreStructure();
                    structure.name = ts.get(1).asWordToken().word;
                    int fields = (parsed.tokens.size() - 4) / 3;
                    for(int i = 0; i<fields; i++) {
                        PreStructureField field = new PreStructureField();
                        field.type = parsed.tokens.get(i * 3 + 3).asWordToken().word;
                        field.name = parsed.tokens.get(i * 3 + 4).asWordToken().word;
                        structure.fields.add(field);
                    }
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
