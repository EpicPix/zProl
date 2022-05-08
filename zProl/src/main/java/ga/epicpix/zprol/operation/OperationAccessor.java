package ga.epicpix.zprol.operation;

public class OperationAccessor extends Operation {

    private final String[] identifiers;

    public OperationAccessor(String... identifiers) {
        this.identifiers = identifiers;
    }

    public String[] getIdentifiers() {
        return identifiers;
    }
}
