package ga.epicpix.zprol.compiled.operation;

import ga.epicpix.zprol.tokens.Token;
import java.util.ArrayList;

public class OperationField extends Operation {

    public ArrayList<Token> reference;

    public OperationField(ArrayList<Token> reference) {
        this.reference = reference;
        super.right = this;
    }

    public String toString() {
        return reference.toString();
    }
}
