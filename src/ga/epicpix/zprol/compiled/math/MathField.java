package ga.epicpix.zprol.compiled.math;

import ga.epicpix.zprol.tokens.Token;
import java.util.ArrayList;

public class MathField extends MathOperation {

    public ArrayList<Token> reference;

    public MathField(ArrayList<Token> reference) {
        this.reference = reference;
        super.number = this;
    }

    public String toString() {
        return reference.toString();
    }
}
