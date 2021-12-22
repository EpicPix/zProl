package ga.epicpix.zprol.operation;

import ga.epicpix.zprol.operation.Operation.OperationAdd;
import ga.epicpix.zprol.operation.Operation.OperationAnd;
import ga.epicpix.zprol.operation.Operation.OperationAssignment;
import ga.epicpix.zprol.operation.Operation.OperationComparison;
import ga.epicpix.zprol.operation.Operation.OperationComparisonNot;
import ga.epicpix.zprol.operation.Operation.OperationDivide;
import ga.epicpix.zprol.operation.Operation.OperationMod;
import ga.epicpix.zprol.operation.Operation.OperationMultiply;
import ga.epicpix.zprol.operation.Operation.OperationShiftLeft;
import ga.epicpix.zprol.operation.Operation.OperationShiftRight;
import ga.epicpix.zprol.operation.Operation.OperationSubtract;
import java.util.HashMap;
import java.util.Map.Entry;

public class OperationOrder {

    public static final String[][] ORDER = {{"="}, {"*", "/", "%"}, {"+", "-", "&"}, {"<<", ">>"}, {"==", "!="}};
    public static final HashMap<String, Class<? extends Operation>> ORDER_TO_CLASS = new HashMap<>();
    public static final HashMap<String, String> ORDER_TO_NAME = new HashMap<>();

    static {
        ORDER_TO_CLASS.put("==", OperationComparison.class);
        ORDER_TO_NAME.put("==", "equal");

        ORDER_TO_CLASS.put("!=", OperationComparisonNot.class);
        ORDER_TO_NAME.put("!=", "notequal");

        ORDER_TO_CLASS.put("=", OperationAssignment.class);
        ORDER_TO_NAME.put("=", "set");

        ORDER_TO_CLASS.put("<<", OperationShiftLeft.class);
        ORDER_TO_NAME.put("<<", "shl");

        ORDER_TO_CLASS.put(">>", OperationShiftRight.class);
        ORDER_TO_NAME.put(">>", "shr");

        ORDER_TO_CLASS.put("+", OperationAdd.class);
        ORDER_TO_NAME.put("+", "add");

        ORDER_TO_CLASS.put("-", OperationSubtract.class);
        ORDER_TO_NAME.put("-", "sub");

        ORDER_TO_CLASS.put("&", OperationAnd.class);
        ORDER_TO_NAME.put("&", "and");

        ORDER_TO_CLASS.put("*", OperationMultiply.class);
        ORDER_TO_NAME.put("*", "mul");

        ORDER_TO_CLASS.put("/", OperationDivide.class);
        ORDER_TO_NAME.put("/", "div");

        ORDER_TO_CLASS.put("%", OperationMod.class);
        ORDER_TO_NAME.put("%", "mod");
    }

    public static String classToOperation(Class<? extends Operation> clazz) {
        for(Entry<String, Class<? extends Operation>> entry : ORDER_TO_CLASS.entrySet()) {
            if(entry.getValue() == clazz) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static int getOrder(String operator) {
        for(int i = 0; i<ORDER.length; i++) {
            for(String s : ORDER[i]) {
                if(s.equals(operator)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
