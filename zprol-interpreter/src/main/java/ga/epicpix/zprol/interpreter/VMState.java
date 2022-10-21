package ga.epicpix.zprol.interpreter;

import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.structures.Function;

import java.util.ArrayList;

public class VMState {

    public final DataStack stack = new DataStack();
    public final ArrayList<FieldStorage> fields = new ArrayList<>();
    public final NativeImpl natives;
    public final MemoryImpl memory;
    public int currentInstruction;

    VMState(NativeImpl natives, MemoryImpl memory) {
        this.natives = natives;
        this.memory = memory;
    }

    public Function currentFunction() {
        var s = stack.valueStack();
        for(int i = s.size() - 1; i >= 0; i--) {
            if(s.get(i).value() instanceof Function f) {
                return f;
            }
        }
        throw new IllegalStateException("Current function not found");
    }

    public void pushFunction(Function function) {
        stack.push(function, 8);
    }

    public void popFunction() {
        var v = stack.pop(8);
        if(!(v.value() instanceof Function)) {
            throw new IllegalStateException("Could not pop function, popped " + v.value());
        }
    }

    public FieldStorage getField(Field field) {
        for(var f : fields) {
            if(f.field == field) {
                return f;
            }
        }
        throw new IllegalStateException("Field not defined");
    }

    public Object getFieldValue(Field f) {
        var field = getField(f);
        if(!field.defined) {
            throw new IllegalStateException("Field content not defined!");
        }
        return field.value;
    }

    public boolean hasReturned;
    public Object returnValue;
    public int returnSize;

    public void returnFunction(Object value, int size) {
        hasReturned = true;
        returnValue = value;
        returnSize = size;
    }
}
