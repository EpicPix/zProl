package ga.epicpix.zpil.pool;

import ga.epicpix.zpil.exceptions.LostConstantPoolEntryException;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.structures.Method;

import java.util.ArrayList;

public class ConstantPool {

    public final ArrayList<ConstantPoolEntry> entries = new ArrayList<>();

    public int getOrCreateStringIndex(String str) {
        if(str == null) return 0;

        for(int i = 0; i < entries.size(); i++) {
            ConstantPoolEntry m = entries.get(i);
            if(m instanceof ConstantPoolEntry.StringEntry) {
                if(((ConstantPoolEntry.StringEntry) m).getString().equals(str)) {
                    return i + 1;
                }
            }
        }
        entries.add(new ConstantPoolEntry.StringEntry(str));
        return entries.size();
    }

    public int getStringIndex(String str) {
        if(str == null) return 0;

        for(int i = 0; i < entries.size(); i++) {
            ConstantPoolEntry m = entries.get(i);
            if(m instanceof ConstantPoolEntry.StringEntry) {
                if(((ConstantPoolEntry.StringEntry) m).getString().equals(str)) {
                    return i + 1;
                }
            }
        }
        throw new LostConstantPoolEntryException("Cannot find string ConstantPoolEntry with '" + str + "'");
    }

    public String getString(int index) {
        return ((ConstantPoolEntry.StringEntry) entries.get(index - 1)).getString();
    }

    public String getStringNullable(int index) {
        if(index == 0) return null;
        return ((ConstantPoolEntry.StringEntry) entries.get(index - 1)).getString();
    }

    public void prepareConstantPool(Object val) {
        if(val instanceof String) getOrCreateStringIndex((String) val);
        else if(val instanceof Function) {
            Function v = (Function) val;
            getOrCreateStringIndex(v.namespace);
            getOrCreateStringIndex(v.name);
            getOrCreateStringIndex(v.signature.toString());
        } else if(val instanceof Class) {
            Class v = (Class) val;
            getOrCreateStringIndex(v.namespace);
            getOrCreateStringIndex(v.name);
        } else if(val instanceof Method) {
            Method v = (Method) val;
            getOrCreateStringIndex(v.namespace);
            getOrCreateStringIndex(v.name);
            getOrCreateStringIndex(v.className);
            getOrCreateStringIndex(v.signature.toString());
        } else if(val instanceof Field) {
            Field v = (Field) val;
            getOrCreateStringIndex(v.namespace);
            getOrCreateStringIndex(v.name);
            getOrCreateStringIndex(v.type.getDescriptor());
        } else throw new IllegalArgumentException(val.getClass().getName());
    }

}
