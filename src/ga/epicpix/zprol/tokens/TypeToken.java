package ga.epicpix.zprol.tokens;

import ga.epicpix.zprol.compiled.Type;

public class TypeToken extends Token {

    private final Type type;

    public TypeToken(Type type) {
        super(TokenType.TYPE);
        this.type = type;
    }

    public Type type() {
        return type;
    }

}
