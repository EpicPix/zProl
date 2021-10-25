package ga.epicpix.zprol.compiled.math;

public class MathMultiply extends MathOperation {

    public MathMultiply(MathOperation operation, MathOperation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "multiply " + left + " " + right;
    }
}
