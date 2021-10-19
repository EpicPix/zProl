package ga.epicpix.zprol.tokens;

public class StructureToken extends Token {

    private final String structureName;

    public StructureToken(String structureName) {
        super(TokenType.STRUCTURE);
        this.structureName = structureName;
    }

    public String getStructureName() {
        return structureName;
    }

    protected String getData() {
        return super.getData() + ", name=\"" + structureName + "\"";
    }
}
