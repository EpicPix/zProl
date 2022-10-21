package ga.epicpix.zprol.interpreter.classes;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.interpreter.LocalStorage;
import ga.epicpix.zprol.interpreter.VMState;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.PrimitiveType;

import java.util.HashMap;

public class DefaultClassImpl extends ClassImpl {

    private final Class clazz;
    private final HashMap<String, Object> fields = new HashMap<>();

    public DefaultClassImpl(Class clz) {
        clazz = clz;
        for(var v : clz.fields()) {
            if(v.type() instanceof PrimitiveType p) {
                if(p.size == 1) fields.put(v.name(), (byte) 0);
                else if(p.size == 2) fields.put(v.name(), (short) 0);
                else if(p.size == 4) fields.put(v.name(), (int) 0);
                else if(p.size == 8) fields.put(v.name(), (long) 0);
            }else {
                fields.put(v.name(), null);
            }
        }
    }

    public void setFieldValue(GeneratedData file, VMState state, String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    public Object getFieldValue(GeneratedData file, VMState state, String fieldName) {
        return fields.get(fieldName);
    }

    public Object runMethod(GeneratedData file, VMState state, LocalStorage locals) {
        throw new NotImplementedException("Cannot run methods yet");
    }
}
