package ga.epicpix.zprol.compiled.math;

import ga.epicpix.zprol.tokens.Token;
import java.util.ArrayList;

public class MathCall extends MathOperation {

    public ArrayList<Token> reference;
    public ArrayList<MathOperation> parameters;

    public MathCall(ArrayList<Token> reference, ArrayList<MathOperation> parameters) {
        this.reference = reference;
        this.parameters = parameters;
        super.right = this;
    }

    public String toString() {
        return reference.toString();
    }

}
