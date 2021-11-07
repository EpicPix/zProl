package ga.epicpix.zprol.tokens;

public class ImportToken extends Token {

    public final String importName;
    public final String varName;

    public ImportToken(String importName, String varName) {
        super(TokenType.IMPORT);
        this.importName = importName;
        this.varName = varName;
    }

}
