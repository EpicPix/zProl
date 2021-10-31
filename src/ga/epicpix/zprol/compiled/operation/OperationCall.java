package ga.epicpix.zprol.compiled.operation;

import ga.epicpix.zprol.tokens.Token;
import java.util.ArrayList;

public class OperationCall extends Operation {

    public ArrayList<Token> reference;
    public ArrayList<Operation> parameters;

    public OperationCall(ArrayList<Token> reference, ArrayList<Operation> parameters) {
        this.reference = reference;
        this.parameters = parameters;
        super.right = this;
    }

    public String toString() {
        return reference.toString();
    }

}
