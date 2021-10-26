package ga.epicpix.zprol.compiled.math;

import java.util.HashMap;
import java.util.Map.Entry;

public class MathOrder {

    public static final String[][] ORDER = {{"*", "/", "%"}, {"+", "-", "&"}, {"<<", ">>"}};
    public static final HashMap<String, Class<? extends MathOperation>> ORDER_TO_CLASS = new HashMap<>();
    public static final HashMap<String, String> ORDER_TO_NAME = new HashMap<>();

    static {
        ORDER_TO_CLASS.put("<<", MathShiftLeft.class);
        ORDER_TO_NAME.put("<<", "shl");

        ORDER_TO_CLASS.put(">>", MathShiftRight.class);
        ORDER_TO_NAME.put(">>", "shr");

        ORDER_TO_CLASS.put("+", MathAdd.class);
        ORDER_TO_NAME.put("+", "add");

        ORDER_TO_CLASS.put("-", MathSubtract.class);
        ORDER_TO_NAME.put("-", "sub");

        ORDER_TO_CLASS.put("&", MathAnd.class);
        ORDER_TO_NAME.put("&", "and");

        ORDER_TO_CLASS.put("*", MathMultiply.class);
        ORDER_TO_NAME.put("*", "mul");

        ORDER_TO_CLASS.put("/", MathSubtract.class);
        ORDER_TO_NAME.put("/", "div");

        ORDER_TO_CLASS.put("%", MathMod.class);
        ORDER_TO_NAME.put("%", "mod");
    }

    public static String classToOperation(Class<? extends MathOperation> clazz) {
        for(Entry<String, Class<? extends MathOperation>> entry : ORDER_TO_CLASS.entrySet()) {
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
