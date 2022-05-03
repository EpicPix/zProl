package ga.epicpix.zprol.operation;

import ga.epicpix.zprol.exceptions.CompileException;

import java.math.BigInteger;

public class OperationNumber extends Operation {

    public BigInteger number;

    public OperationNumber(BigInteger number) {
        this.number = number;
    }

    public String toString() {
        return number.toString();
    }

    public static BigInteger getDecimalInteger(String str) {
        try {
            return new BigInteger(str, 10);
        }catch(NumberFormatException e) {
            throw new CompileException("Decimal Integer not a valid integer '" + str + "'");
        }
    }

}
