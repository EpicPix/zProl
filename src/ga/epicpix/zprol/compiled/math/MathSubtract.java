package ga.epicpix.zprol.compiled.math;

public class MathSubtract extends MathOperation {

    public MathSubtract(MathOperation operation, MathOperation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "subtract " + left + " " + right;
    }
}
