package ga.epicpix.zprol.compiled.operation;

public class OperationAnd extends Operation {

    public OperationAnd(Operation operation, Operation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "and " + left + " " + right;
    }
}
