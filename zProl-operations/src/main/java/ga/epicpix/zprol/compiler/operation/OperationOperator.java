package ga.epicpix.zprol.compiler.operation;

public class OperationOperator extends Operation {

    public LanguageOperator operator;

    public OperationOperator(LanguageOperator operator) {
        this.operator = operator;
    }

    public String toString() {
        return operator.operator();
    }

}
