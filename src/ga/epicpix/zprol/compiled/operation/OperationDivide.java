package ga.epicpix.zprol.compiled.operation;

public class OperationDivide extends Operation {

    public OperationDivide(Operation operation, Operation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "divide " + left + " " + right;
    }
}
