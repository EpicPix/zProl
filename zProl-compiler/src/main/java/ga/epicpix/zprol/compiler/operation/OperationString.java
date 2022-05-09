package ga.epicpix.zprol.compiler.operation;

public class OperationString extends Operation {

    private final String string;

    public OperationString(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

}
