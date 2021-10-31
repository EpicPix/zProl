package ga.epicpix.zprol.compiled.operation;

public class OperationShiftLeft extends Operation {

    public OperationShiftLeft(Operation operation, Operation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "shl " + left + " " + right;
    }
}
