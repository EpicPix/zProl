package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.parser.tokens.ParsedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.precompiled.PreFunction;
import ga.epicpix.zprol.precompiled.PreParameter;

import java.util.ArrayList;

import static ga.epicpix.zprol.StaticImports.createStorage;

public class Compiler {

    public static IBytecodeStorage parseFunctionCode(CompiledData data, SeekIterator<Token> tokens, FunctionSignature sig, String[] names) {
        IBytecodeStorage storage = createStorage();
        LocalScopeManager localsManager = new LocalScopeManager();
        for(int i = 0; i<names.length; i++) {
            localsManager.getCurrentScope().defineLocalVariable(names[i], sig.parameters()[i]);
        }
        localsManager.newScope();
        int opens = 0;
        Token token;
        while(tokens.hasNext()) {
            token = tokens.next();
            if(token.getType() == TokenType.PARSED) {
                ParsedToken parsed = (ParsedToken) token;
                throw new RuntimeException("Not implemented language feature: " + parsed.name + " / " + parsed.tokens);
            }else {
                if(token.getType() == TokenType.OPEN_SCOPE) {
                    opens++;
                    localsManager.newScope();
                }else if(token.getType() == TokenType.CLOSE_SCOPE) {
                    opens--;
                    localsManager.leaveScope();
                    if(opens == 0) {
                        localsManager.leaveScope();
                        break;
                    }
                }else {
                    throw new RuntimeException("Not implemented token type: " + token);
                }
                //TODO: ++x or --x
            }
        }
        storage.setLocalsSize(localsManager.getLocalVariablesSize());
        return storage;
    }

    public static void compileFunction(CompiledData data, PreFunction function) throws UnknownTypeException {
        PrimitiveType returnType = data.resolveType(function.returnType);
        PrimitiveType[] parameters = new PrimitiveType[function.parameters.size()];
        String[] names = new String[function.parameters.size()];
        for(int i = 0; i<function.parameters.size(); i++) {
            PreParameter param = function.parameters.get(i);
            parameters[i] = data.resolveType(param.type);
            names[i] = param.name;
        }
        FunctionSignature signature = new FunctionSignature(returnType, parameters);
        data.addFunction(new Function(data.namespace, function.name, signature, parseFunctionCode(data, new SeekIterator<>(function.code), signature, names)));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other) throws UnknownTypeException {
        CompiledData data = new CompiledData(preCompiled.namespace);

        for(PreCompiledData o : other) if(preCompiled.using.contains(o.namespace)) data.using(o);

        for(PreFunction function : preCompiled.functions) compileFunction(data, function);
        return data;
    }

}