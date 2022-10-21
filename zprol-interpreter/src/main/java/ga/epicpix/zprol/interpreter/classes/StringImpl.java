package ga.epicpix.zprol.interpreter.classes;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.interpreter.LocalStorage;
import ga.epicpix.zprol.interpreter.VMState;

public final class StringImpl extends ClassImpl {

    public final String string;

    public StringImpl(String string) {
        this.string = string;
    }

    public void setFieldValue(GeneratedData file, VMState state, String fieldName, Object value) {
        throw new RuntimeException("Cannot set field zprol.lang.String[" + fieldName + "]");
    }

    public Object getFieldValue(GeneratedData file, VMState state, String fieldName) {
        if(fieldName.equals("bytes")) {
            return string.getBytes();
        }else if(fieldName.equals("length")) {
            return (long) string.length();
        }
        throw new IllegalArgumentException("Tried to get an unknown field");
    }

    public Object runMethod(GeneratedData file, VMState state, LocalStorage locals) {
        throw new RuntimeException("Cannot call methods on zprol.lang.String");
    }

    public String toString() {
        return "StringImpl[" + string + "]";
    }
}
