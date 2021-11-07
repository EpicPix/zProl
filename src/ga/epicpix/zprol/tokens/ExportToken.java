package ga.epicpix.zprol.tokens;

public class ExportToken extends Token {

    public final String exportName;

    public ExportToken(String exportName) {
        super(TokenType.EXPORT);
        this.exportName = exportName;
    }

}
