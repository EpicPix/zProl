package ga.epicpix.zprol.compiled.operation;

public class OperationShiftRight extends Operation {

    public OperationShiftRight(Operation operation, Operation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "shr " + left + " " + right;
    }
}
