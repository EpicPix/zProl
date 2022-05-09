package ga.epicpix.zprol.compiler.operation;

public class OperationAssignment extends Operation {

    private final String identifier;
    private final OperationRoot operation;

    public OperationAssignment(String identifier, OperationRoot operation) {
        this.identifier = identifier;
        this.operation = operation;
    }

    public String getIdentifier() {
        return identifier;
    }

    public OperationRoot getOperation() {
        return operation;
    }
}
