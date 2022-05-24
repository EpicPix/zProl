package ga.epicpix.zprol.compiler.operation;

public class OperationAccessor extends Operation {

    private final String[] identifiers;

    public OperationAccessor(String... identifiers) {
        this.identifiers = identifiers;
    }

    public String[] getIdentifiers() {
        return identifiers;
    }
}
