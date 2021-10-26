package ga.epicpix.zprol.compiled.math;

public class MathShiftLeft extends MathOperation {

    public MathShiftLeft(MathOperation operation, MathOperation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "shl " + left + " " + right;
    }
}
