package ga.epicpix.zprol.compiled.math;

public class MathBrackets extends MathOperation {

    public MathBrackets(MathOperation operation) {
        this.left = operation;
        this.right = this;
    }

    public String toString() {
        return "(" + left + ")";
    }
}
