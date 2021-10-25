package ga.epicpix.zprol.compiled.math;

public class MathMod extends MathOperation {

    public MathMod(MathOperation operation, MathOperation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "mod " + left + " " + right;
    }
}
