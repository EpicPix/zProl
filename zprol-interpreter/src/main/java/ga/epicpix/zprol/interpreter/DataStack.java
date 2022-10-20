package ga.epicpix.zprol.interpreter;

import java.util.Stack;

class DataStack {

    private final Stack<DataValue> valueStack = new Stack<>();
    private int totalSize;

    public void push(Object value, int size) {
        valueStack.push(new DataValue(value, size));
        totalSize += size;
    }

    public DataValue pop(int size) {
        if(size < 0) throw new IllegalArgumentException("Cannot pop negative bytes");
        if(size > 8) throw new IllegalArgumentException("Cannot pop more than 8 bytes");
        var data = valueStack.pop();
        totalSize -= data.size();
        if(data.size() == size) return data;
        throw new IllegalStateException("Popped " + data.size() + " but requested " + size);
    }

    public Stack<DataValue> valueStack() {
        return valueStack;
    }

}
