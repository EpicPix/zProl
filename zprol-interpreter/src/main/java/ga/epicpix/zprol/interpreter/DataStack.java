package ga.epicpix.zprol.interpreter;

import java.util.LinkedList;

public class DataStack {

    private final LinkedList<DataValue> valueStack = new LinkedList<>();

    public void push(Object value, int size) {
        valueStack.addLast(new DataValue(value, size));
    }

    public DataValue pop(int size) {
        if(size < 0) throw new IllegalArgumentException("Cannot pop negative bytes");
        if(size > 8) throw new IllegalArgumentException("Cannot pop more than 8 bytes");
        DataValue data = valueStack.removeLast();
        if(data.size == size) return data;
        throw new IllegalStateException("Popped " + data.size + " but requested " + size);
    }

    public LinkedList<DataValue> valueStack() {
        return valueStack;
    }

}
