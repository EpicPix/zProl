package ga.epicpix.zprol.compiler.operation;

public class OperationBoolean extends Operation {

    private final boolean value;

    public OperationBoolean(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

}
