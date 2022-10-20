package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;

public abstract class NativeImpl {

    public abstract Object runNative(GeneratedData file, VMState state, LocalStorage locals);

}
