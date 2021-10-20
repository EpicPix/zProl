package ga.epicpix.zprol.tokens;

import ga.epicpix.zprol.StructureType;
import java.util.ArrayList;

public class StructureToken extends Token {

    private final String structureName;
    private final ArrayList<StructureType> types;

    public StructureToken(String structureName, ArrayList<StructureType> types) {
        super(TokenType.STRUCTURE);
        this.structureName = structureName;
        this.types = types;
    }

    public String getStructureName() {
        return structureName;
    }

    protected String getData() {
        return "name=\"" + structureName + "\", types=" + types;
    }
}
