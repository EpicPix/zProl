package ga.epicpix.zprol.compiled.math;

public class MathShiftRight extends MathOperation {

    public MathShiftRight(MathOperation operation, MathOperation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "shr " + left + " " + right;
    }
}
