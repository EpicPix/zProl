package ga.epicpix.zprol.tokens;

public class ObjectToken extends Token {

    private final String objectName;
    private final String extendsFrom;

    public ObjectToken(String objectName, String extendsFrom) {
        super(TokenType.OBJECT);
        this.objectName = objectName;
        this.extendsFrom = extendsFrom;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getExtendsFrom() {
        return extendsFrom;
    }

    protected String getData() {
        return "name=\"" + objectName + "\", extends=\"" + extendsFrom + "\"";
    }
}
