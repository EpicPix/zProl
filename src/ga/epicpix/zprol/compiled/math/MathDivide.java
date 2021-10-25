package ga.epicpix.zprol.compiled.math;

public class MathDivide extends MathOperation {

    public MathDivide(MathOperation operation, MathOperation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "divide " + left + " " + right;
    }
}
