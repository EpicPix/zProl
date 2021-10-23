package ga.epicpix.zprol.compiled.math;

public class MathSubtract extends MathOperation {

    public MathSubtract(MathOperation operation, MathOperation number) {
        this.operation = operation;
        this.number = number;
    }

    public String toString() {
        return "subtract " + operation + " " + number;
    }
}
