package ga.epicpix.zprol.interpreter.classes;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.interpreter.LocalStorage;
import ga.epicpix.zprol.interpreter.VMState;

public abstract class ClassImpl {

    public abstract void setFieldValue(GeneratedData file, VMState state, String fieldName, Object value);
    public abstract Object getFieldValue(GeneratedData file, VMState state, String fieldName);

    public abstract Object runMethod(GeneratedData file, VMState state, LocalStorage locals);

}
