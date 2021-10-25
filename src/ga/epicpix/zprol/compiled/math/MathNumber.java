package ga.epicpix.zprol.compiled.math;

import ga.epicpix.zprol.tokens.Token;

public class MathNumber extends MathOperation {

    public Token number;

    public MathNumber(Token token) {
        number = token;
        super.right = this;
    }

    public String toString() {
        return number.toString();
    }
}
