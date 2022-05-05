package ga.epicpix.zprol.operation;

import ga.epicpix.zprol.exceptions.compilation.CompileException;
import ga.epicpix.zprol.parser.tokens.Token;

import java.math.BigInteger;

public class OperationNumber extends Operation {

    public BigInteger number;

    public OperationNumber(BigInteger number) {
        this.number = number;
    }

    public String toString() {
        return number.toString();
    }

    public static BigInteger getDecimalInteger(Token token) {
        try {
            return new BigInteger(token.asWordToken().getWord(), 10);
        }catch(NumberFormatException e) {
            throw new CompileException("Decimal Integer not a valid integer '" + token.asWordToken().getWord() + "'", token);
        }
    }

}
