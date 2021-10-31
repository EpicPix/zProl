package ga.epicpix.zprol.compiled.operation;

public class OperationMultiply extends Operation {

    public OperationMultiply(Operation operation, Operation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "multiply " + left + " " + right;
    }
}
