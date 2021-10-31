package ga.epicpix.zprol.compiled.operation;

public class OperationBrackets extends Operation {

    public OperationBrackets(Operation operation) {
        this.left = operation;
        this.right = this;
    }

    public String toString() {
        return "(" + left + ")";
    }
}
