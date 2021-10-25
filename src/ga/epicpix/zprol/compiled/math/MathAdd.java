package ga.epicpix.zprol.compiled.math;

public class MathAdd extends MathOperation {

    public MathAdd(MathOperation operation, MathOperation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "add " + left + " " + right;
    }
}
