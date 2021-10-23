package ga.epicpix.zprol.compiled.math;

public class MathAdd extends MathOperation {

    public MathAdd(MathOperation operation, MathOperation number) {
        this.operation = operation;
        this.number = number;
    }

    public String toString() {
        return "add " + operation + " " + number;
    }
}
