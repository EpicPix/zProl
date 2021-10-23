package ga.epicpix.zprol.compiled.math;

public class MathMultiply extends MathOperation {

    public MathMultiply(MathOperation operation, MathOperation number) {
        this.operation = operation;
        this.number = number;
    }

    public String toString() {
        return "multiply " + operation + " " + number;
    }
}
