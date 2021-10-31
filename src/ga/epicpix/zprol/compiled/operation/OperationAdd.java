package ga.epicpix.zprol.compiled.operation;

public class OperationAdd extends Operation {

    public OperationAdd(Operation operation, Operation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "add " + left + " " + right;
    }
}
