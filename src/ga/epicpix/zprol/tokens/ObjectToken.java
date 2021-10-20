package ga.epicpix.zprol.tokens;

public class ObjectToken extends Token {

    private final String objectName;

    public ObjectToken(String objectName) {
        super(TokenType.OBJECT);
        this.objectName = objectName;
    }

    public String getObjectName() {
        return objectName;
    }

    protected String getData() {
        return "name=\"" + objectName + "\"";
    }
}
