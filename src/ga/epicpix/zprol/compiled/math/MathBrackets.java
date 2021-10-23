package ga.epicpix.zprol.compiled.math;

public class MathBrackets extends MathOperation {

    public MathBrackets(MathOperation operation) {
        this.operation = operation;
        this.number = this;
    }

    public String toString() {
        return "(" + operation + ")";
    }
}
