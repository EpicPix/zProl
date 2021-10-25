package ga.epicpix.zprol.compiled.math;

public class MathAnd extends MathOperation {

    public MathAnd(MathOperation operation, MathOperation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "and " + left + " " + right;
    }
}
