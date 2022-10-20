package ga.epicpix.zprol.interpreter;

import ga.epicpix.zprol.structures.Function;

public class VMState {

    public final DataStack stack = new DataStack();

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

}
