package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;

import java.util.ArrayList;

public class CompilerIdentifierData {

    public final Token location;

    public CompilerIdentifierData(Token location) {
        this.location = location;
    }

    public static CompilerIdentifierData[] accessorToData(NamedToken accessor) {
        var list = new ArrayList<CompilerIdentifierData>();
        var first = accessor.tokens[0].asNamedToken();
        if(first.name.equals("FunctionInvocationAccessor")) {
            list.add(new CompilerIdentifierDataFunction(first, first.getSingleTokenWithName("Identifier").asWordToken(), first.getTokenWithName("FunctionInvocation")));
        }else if(first.name.equals("Identifier")) {
            list.add(new CompilerIdentifierDataField(first, first.tokens[0].asWordToken()));
        }else {
            throw new TokenLocatedException("Unknown named name: " + first.name, first);
        }
        for(var element : accessor.getTokensWithName("AccessorElement")) {
            NamedToken loc;
            if((loc = element.getTokenWithName("FunctionInvocationAccessor")) != null) {
                list.add(new CompilerIdentifierDataFunction(loc, loc.getSingleTokenWithName("Identifier").asWordToken(), loc.getTokenWithName("FunctionInvocation")));
            }else if((loc = element.getTokenWithName("Identifier")) != null) {
                list.add(new CompilerIdentifierDataField(loc, loc.tokens[0].asWordToken()));
            }else if((loc = element.getTokenWithName("ArrayAccessor")) != null) {
                list.add(new CompilerIdentifierDataArray(loc, loc.getTokenWithName("Expression")));
            }else {
                throw new TokenLocatedException("Unknown named name: " + first.name, first);
            }
        }
        return list.toArray(new CompilerIdentifierData[0]);
    }
}
