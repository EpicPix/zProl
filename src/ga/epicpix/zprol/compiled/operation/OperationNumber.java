package ga.epicpix.zprol.compiled.operation;

import ga.epicpix.zprol.tokens.NumberToken;

public class OperationNumber extends Operation {

    public NumberToken number;

    public OperationNumber(NumberToken token) {
        number = token;
        super.right = this;
    }

    public String toString() {
        return number.toString();
    }
}
