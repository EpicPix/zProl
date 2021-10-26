package ga.epicpix.zprol.compiled.math;

import ga.epicpix.zprol.tokens.NumberToken;

public class MathNumber extends MathOperation {

    public NumberToken number;

    public MathNumber(NumberToken token) {
        number = token;
        super.right = this;
    }

    public String toString() {
        return number.toString();
    }
}
