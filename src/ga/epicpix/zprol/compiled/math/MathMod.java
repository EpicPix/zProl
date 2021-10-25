package ga.epicpix.zprol.compiled.math;

public class MathMod extends MathOperation {

    public MathMod(MathOperation operation, MathOperation number) {
        this.operation = operation;
        this.number = number;
    }

    public String toString() {
        return "mod " + operation + " " + number;
    }
}
