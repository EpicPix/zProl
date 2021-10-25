package ga.epicpix.zprol.compiled.math;

public class MathAnd extends MathOperation {

    public MathAnd(MathOperation operation, MathOperation number) {
        this.operation = operation;
        this.number = number;
    }

    public String toString() {
        return "and " + operation + " " + number;
    }
}
