package ga.epicpix.zprol.interpreter;

import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.structures.Method;
import ga.epicpix.zprol.types.PrimitiveType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

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
        LinkedList<DataValue> s = stack.valueStack();
        for(int i = s.size() - 1; i >= 0; i--) {
            Object v = s.get(i).value;
            if(v instanceof Function) {
                return (Function) v;
            }else if(v instanceof Method) {
                throw new IllegalStateException("Found a method instead of a function");
            }
        }
        throw new IllegalStateException("Current function not found");
    }

    public Method currentMethod() {
        LinkedList<DataValue> s = stack.valueStack();
        for(int i = s.size() - 1; i >= 0; i--) {
            Object v = s.get(i).value;
            if(v instanceof Method) {
                return (Method) v;
            }else if(v instanceof Function) {
                throw new IllegalStateException("Found a function instead of a method");
            }
        }
        throw new IllegalStateException("Current method not found");
    }

    public void pushFunction(Function function) {
        stack.push(function, 8);
    }

    public void pushMethod(Method method) {
        stack.push(method, 8);
    }

    public void popFunction() {
        DataValue v = stack.pop(8);
        if(!(v.value instanceof Function)) {
            throw new IllegalStateException("Could not pop function, popped " + v.value);
        }
    }

    public void popMethod() {
        DataValue v = stack.pop(8);
        if(!(v.value instanceof Method)) {
            throw new IllegalStateException("Could not pop method, popped " + v.value);
        }
    }

    public FieldStorage getField(Field field) {
        for(FieldStorage f : fields) {
            if(Objects.equals(f.field.namespace, field.namespace) && f.field.name.equals(field.name) && f.field.type.equals(field.type)) {
                return f;
            }
        }
        throw new IllegalStateException("Field not defined:" + field.namespace + "." + field.name);
    }

    public Object getFieldValue(Field f) {
        FieldStorage field = getField(f);
        if(!field.defined) {
            if(f.type instanceof PrimitiveType) {
                PrimitiveType t = (PrimitiveType) f.type;
                if(t.size == 1) return (byte) 0;
                else if(t.size == 2) return (short) 0;
                else if(t.size == 4) return (int) 0;
                else if(t.size == 8) return (long) 0;
            }else {
                return (long) 0;
            }
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
