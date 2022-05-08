package ga.epicpix.zprol.operation;

import ga.epicpix.zprol.zld.LanguageOperator;

public class OperationOperator extends Operation {

    public LanguageOperator operator;

    public OperationOperator(LanguageOperator operator) {
        this.operator = operator;
    }

    public String toString() {
        return operator.operator();
    }

}
