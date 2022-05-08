package ga.epicpix.zprol.operation;

public class OperationCast extends Operation {

    private final boolean hardCast;
    private final String type;
    private final OperationRoot operation;

    public OperationCast(boolean hardCast, String type, OperationRoot operation) {
        this.hardCast = hardCast;
        this.type = type;
        this.operation = operation;
    }

    public boolean isHardCast() {
        return hardCast;
    }

    public String getType() {
        return type;
    }

    public OperationRoot getOperation() {
        return operation;
    }
}
