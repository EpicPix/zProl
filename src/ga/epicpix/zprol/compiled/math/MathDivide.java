package ga.epicpix.zprol.compiled.math;

public class MathDivide extends MathOperation {

    public MathDivide(MathOperation operation, MathOperation number) {
        this.operation = operation;
        this.number = number;
    }

    public String toString() {
        return "divide " + operation + " " + number;
    }
}
