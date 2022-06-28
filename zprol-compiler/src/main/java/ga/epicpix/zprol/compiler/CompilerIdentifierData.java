package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;

import java.util.ArrayList;

public class CompilerIdentifierData {

    public final Token location;

    public CompilerIdentifierData(Token location) {
        this.location = location;
    }

    public static CompilerIdentifierData[] accessorToData(NamedToken accessor) {
        var tokens = accessor.tokens;
        var list = new ArrayList<CompilerIdentifierData>();
        if(tokens[0] instanceof LexerToken lexer) {
            if(lexer.name.equals("Identifier")) {
                list.add(new CompilerIdentifierDataField(lexer, lexer.toStringRaw()));
            }else {
                throw new TokenLocatedException("Unknown lexer name: " + lexer.name, lexer);
            }
        }else if(tokens[0] instanceof NamedToken named) {
            if(named.name.equals("FunctionInvocationAccessor")) {
                list.add(new CompilerIdentifierDataFunction(named, named.getTokenAsString("Identifier"), named.getTokenWithName("FunctionInvocation")));
            }else {
                throw new TokenLocatedException("Unknown named name: " + named.name, named);
            }
        }
        for(var element : accessor.getTokensWithName("AccessorElement")) {
            NamedToken loc;
            LexerToken lex;
            if((loc = element.getTokenWithName("FunctionInvocationAccessor")) != null) {
                list.add(new CompilerIdentifierDataFunction(loc, loc.getTokenAsString("Identifier"), loc.getTokenWithName("FunctionInvocation")));
            }else if((loc = element.getTokenWithName("ArrayAccessor")) != null) {
                list.add(new CompilerIdentifierDataArray(loc, loc.getTokenWithName("Expression")));
            }else if((lex = element.getLexerToken("Identifier")) != null) {
                list.add(new CompilerIdentifierDataField(lex, lex.toStringRaw()));
            }else {
                throw new TokenLocatedException("Cannot handle accessor element: " + element, element);
            }
        }
        return list.toArray(new CompilerIdentifierData[0]);
    }
}
