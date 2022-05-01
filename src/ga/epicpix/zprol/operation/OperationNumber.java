package ga.epicpix.zprol.operation;

import ga.epicpix.zprol.parser.tokens.NumberToken;

import java.math.BigInteger;

public class OperationNumber extends Operation {

    public BigInteger number;

    public OperationNumber(NumberToken token) {
        this.number = token.number;
    }

    public OperationNumber(BigInteger number) {
        this.number = number;
    }

    public String toString() {
        return number.toString();
    }

}
