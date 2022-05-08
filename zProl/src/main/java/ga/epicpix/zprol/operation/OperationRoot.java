package ga.epicpix.zprol.operation;

import java.util.ArrayList;

public class OperationRoot extends Operation {

    private final ArrayList<Operation> operations;

    public OperationRoot(ArrayList<Operation> operations) {
        this.operations = operations;
    }

    public ArrayList<Operation> getOperations() {
        return operations;
    }
}
