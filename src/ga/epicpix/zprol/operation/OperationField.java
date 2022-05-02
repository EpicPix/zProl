package ga.epicpix.zprol.operation;

public class OperationField extends Operation {

    private final String identifier;

    public OperationField(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
