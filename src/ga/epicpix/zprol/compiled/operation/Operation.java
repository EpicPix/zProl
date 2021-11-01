package ga.epicpix.zprol.compiled.operation;

import ga.epicpix.zprol.tokens.NumberToken;
import ga.epicpix.zprol.tokens.StringToken;
import ga.epicpix.zprol.tokens.Token;
import java.util.ArrayList;

public class Operation {

    public Operation left;
    public Operation right;

    public Operation() {}

    public static class OperationAdd extends Operation {

        public OperationAdd(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "add " + left + " " + right;
        }
    }

    public static class OperationAnd extends Operation {

        public OperationAnd(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "and " + left + " " + right;
        }
    }

    public static class OperationAssignment extends Operation {

        public OperationAssignment(OperationField operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "set " + left + " " + right;
        }
    }

    public static class OperationBrackets extends Operation {

        public OperationBrackets(Operation operation) {
            this.left = operation;
            this.right = this;
        }

        public String toString() {
            return "(" + left + ")";
        }
    }

    public static class OperationCall extends Operation {

        public ArrayList<Token> reference;
        public ArrayList<Operation> parameters;

        public OperationCall(ArrayList<Token> reference, ArrayList<Operation> parameters) {
            this.reference = reference;
            this.parameters = parameters;
            super.right = this;
        }

        public String toString() {
            return Token.toFriendlyString(reference) + " " + parameters;
        }

    }

    public static class OperationComparison extends Operation {

        public OperationComparison(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "equal " + left + " " + right;
        }
    }

    public static class OperationComparisonNot extends Operation {

        public OperationComparisonNot(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "notequal " + left + " " + right;
        }
    }

    public static class OperationDivide extends Operation {

        public OperationDivide(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "divide " + left + " " + right;
        }
    }

    public static class OperationField extends Operation {

        public ArrayList<Token> reference;

        public OperationField(ArrayList<Token> reference) {
            this.reference = reference;
            super.right = this;
        }

        public String toString() {
            return reference.toString();
        }
    }

    public static class OperationMod extends Operation {

        public OperationMod(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "mod " + left + " " + right;
        }
    }

    public static class OperationMultiply extends Operation {

        public OperationMultiply(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "multiply " + left + " " + right;
        }
    }

    public static class OperationNumber extends Operation {

        public NumberToken number;

        public OperationNumber(NumberToken token) {
            number = token;
            super.right = this;
        }

        public String toString() {
            return number.toString();
        }
    }

    public static class OperationShiftLeft extends Operation {

        public OperationShiftLeft(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "shl " + left + " " + right;
        }
    }

    public static class OperationShiftRight extends Operation {

        public OperationShiftRight(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "shr " + left + " " + right;
        }
    }

    public static class OperationString extends Operation {

        public StringToken string;

        public OperationString(StringToken token) {
            string = token;
            super.right = this;
        }

        public String toString() {
            return string.toString();
        }
    }

    public static class OperationSubtract extends Operation {

        public OperationSubtract(Operation operation, Operation number) {
            this.left = operation;
            this.right = number;
        }

        public String toString() {
            return "subtract " + left + " " + right;
        }
    }


}
