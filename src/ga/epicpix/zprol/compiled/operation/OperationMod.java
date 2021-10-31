package ga.epicpix.zprol.compiled.operation;

public class OperationMod extends Operation {

    public OperationMod(Operation operation, Operation number) {
        this.left = operation;
        this.right = number;
    }

    public String toString() {
        return "mod " + left + " " + right;
    }
}
