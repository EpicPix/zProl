package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.exceptions.NotImplementedException;

public abstract class NativeImpl {

    public Object runNativeFunction(GeneratedData file, VMState state, LocalStorage locals) {
        throw new NotImplementedException("runNativeFunction");
    }

    public Object runNativeMethod(GeneratedData file, VMState state, LocalStorage locals) {
        throw new NotImplementedException("runNativeMethod");
    }

}
