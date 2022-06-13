package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;

import java.util.ArrayList;

public class CompilerIdentifierDataFunction extends CompilerIdentifierData {

    public final String identifier;
    public final NamedToken functionInvocation;
    public final NamedToken[] arguments;

    public CompilerIdentifierDataFunction(Token location, String identifier, NamedToken functionInvocation) {
        super(location);
        this.identifier = identifier;
        this.functionInvocation = functionInvocation;
        arguments = loadArguments(functionInvocation);
    }

    public static NamedToken[] loadArguments(NamedToken functionInvocation) {
        if(!functionInvocation.name.equals("FunctionInvocation")) throw new TokenLocatedException("Expected 'FunctionInvocation' got '" + functionInvocation.name + "'", functionInvocation);

        NamedToken argumentList = functionInvocation.getTokenWithName("ArgumentList");
        if(argumentList == null) return new NamedToken[0];
        ArrayList<NamedToken> arguments = argumentList.getTokensWithName("Argument");
        NamedToken[] expressions = new NamedToken[arguments.size()];
        for(int i = 0; i<expressions.length; i++) {
            expressions[i] = arguments.get(i).getTokenWithName("Expression");
        }
        return expressions;
    }

    public String getFunctionName() {
        return identifier;
    }

    public ArrayList<LookupFunction> lookupFunction(CompiledData data, PreClass classContext, boolean searchPublic) {
        ArrayList<LookupFunction> possibilities = new ArrayList<>();
        String funcName = getFunctionName();

        if(classContext != null) {
            for (var method : classContext.methods){
                if (method.name.equals(funcName)) {
                    if (method.parameters.size() == arguments.length) {
                        possibilities.add(new LookupFunction(true, method));
                    }
                }
            }
        }
        if(searchPublic) {
            for (var using : data.getUsing()) {
                for (var func : using.functions) {
                    if (func.name.equals(funcName)) {
                        if (func.parameters.size() == arguments.length) {
                            possibilities.add(new LookupFunction(false, func));
                        }
                    }
                }
            }
        }
        return possibilities;
    }

}
