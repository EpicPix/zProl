package ga.epicpix.zprol.operation;

import java.util.ArrayList;

public class OperationCall extends Operation {

    private final String methodName;
    private final ArrayList<OperationRoot> operations;

    public OperationCall(String methodName, ArrayList<OperationRoot> operations) {
        this.methodName = methodName;
        this.operations = operations;
    }

    public String getFunctionName() {
        return methodName;
    }

    public ArrayList<OperationRoot> getOperations() {
        return operations;
    }
}
