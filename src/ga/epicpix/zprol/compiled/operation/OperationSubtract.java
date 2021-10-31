package ga.epicpix.zprol.compiled.operation;

public class OperationSubtract extends Operation {

    public OperationSubtract(Operation operation, Operation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "subtract " + left + " " + right;
    }
}
