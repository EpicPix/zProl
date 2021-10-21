package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Structure;
import ga.epicpix.zprol.compiled.StructureField;
import ga.epicpix.zprol.tokens.ObjectToken;
import ga.epicpix.zprol.tokens.StructureToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.TypedefToken;
import java.util.ArrayList;

public class Compiler {

    public static CompiledData compile(ArrayList<Token> tokens) {
        CompiledData data = new CompiledData();
        for(Token token : tokens) {
            if(token.getType() == TokenType.STRUCTURE) {
                data.addFutureStructureDefinition(((StructureToken) token).getStructureName());
            }else if(token.getType() == TokenType.OBJECT) {
                data.addFutureObjectDefinition(((ObjectToken) token).getObjectName());
            }
        }
        for(int i = 0; i<tokens.size(); i++) {
            Token token = tokens.get(i);
            if(token.getType() == TokenType.STRUCTURE) {
                StructureToken structureToken = (StructureToken) token;
                ArrayList<StructureField> fields = new ArrayList<>();
                for(StructureType field : structureToken.getTypes()) {
                    fields.add(new StructureField(field.name, data.resolveType(field.type)));
                }
                data.addStructure(new Structure(structureToken.getStructureName(), fields));
            }else if(token.getType() == TokenType.TYPEDEF) {
                TypedefToken typedefToken = (TypedefToken) token;
                data.addTypeDefinition(typedefToken.getName(), data.resolveType(typedefToken.getToType()));
            }
        }
        data.finishFutures();
        return data;
    }

}
