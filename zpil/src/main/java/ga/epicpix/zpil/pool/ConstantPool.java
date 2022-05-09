package ga.epicpix.zpil.pool;

import ga.epicpix.zpil.exceptions.LostConstantPoolEntryException;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.structures.FunctionModifiers;

import java.util.ArrayList;

public class ConstantPool {

    public final ArrayList<ConstantPoolEntry> entries = new ArrayList<>();

    public int getOrCreateFunctionIndex(Function func) {
        int namespaceIndex = getOrCreateStringIndex(func.namespace());
        int nameIndex = getOrCreateStringIndex(func.name());
        int signatureIndex = getOrCreateStringIndex(func.signature().toString());

        for(int i = 0; i < entries.size(); i++) {
            if(entries.get(i) instanceof ConstantPoolEntry.FunctionEntry e) {
                if(e.getNamespace() == namespaceIndex && e.getName() == nameIndex && e.getSignature() == signatureIndex) {
                    return i + 1;
                }
            }
        }
        entries.add(new ConstantPoolEntry.FunctionEntry(namespaceIndex, nameIndex, signatureIndex, FunctionModifiers.toBits(func.modifiers())));
        return entries.size();
    }

    public int getOrCreateStringIndex(String str) {
        if(str == null) return 0;

        for(int i = 0; i < entries.size(); i++) {
            if(entries.get(i) instanceof ConstantPoolEntry.StringEntry e) {
                if(e.getString().equals(str)) {
                    return i + 1;
                }
            }
        }
        entries.add(new ConstantPoolEntry.StringEntry(str));
        return entries.size();
    }

    public int getOrCreateClassIndex(Class clz) {
        int namespaceIndex = getOrCreateStringIndex(clz.namespace());
        int nameIndex = getOrCreateStringIndex(clz.name());

        for(int i = 0; i < entries.size(); i++) {
            if(entries.get(i) instanceof ConstantPoolEntry.ClassEntry e) {
                if(e.getNamespace() == namespaceIndex && e.getName() == nameIndex) {
                    return i + 1;
                }
            }
        }
        entries.add(new ConstantPoolEntry.ClassEntry(namespaceIndex, nameIndex));
        return entries.size();
    }



    public int getFunctionIndex(Function func) {
        int namespaceIndex = getStringIndex(func.namespace());
        int nameIndex = getStringIndex(func.name());
        int signatureIndex = getStringIndex(func.signature().toString());

        for(int i = 0; i < entries.size(); i++) {
            if(entries.get(i) instanceof ConstantPoolEntry.FunctionEntry e) {
                if(e.getNamespace() == namespaceIndex && e.getName() == nameIndex && e.getSignature() == signatureIndex) {
                    return i + 1;
                }
            }
        }
        throw new LostConstantPoolEntryException("Cannot find function ConstantPoolEntry with " + (func.namespace() != null ? func.namespace() + "." : "") + func.name() + " - " + func.signature());
    }

    public int getStringIndex(String str) {
        if(str == null) return 0;

        for(int i = 0; i < entries.size(); i++) {
            if(entries.get(i) instanceof ConstantPoolEntry.StringEntry e) {
                if(e.getString().equals(str)) {
                    return i + 1;
                }
            }
        }
        throw new LostConstantPoolEntryException("Cannot find string ConstantPoolEntry with '" + str + "'");
    }

    public int getClassIndex(Class clz) {
        int namespaceIndex = getStringIndex(clz.namespace());
        int nameIndex = getStringIndex(clz.name());

        for(int i = 0; i < entries.size(); i++) {
            if(entries.get(i) instanceof ConstantPoolEntry.ClassEntry e) {
                if(e.getNamespace() == namespaceIndex && e.getName() == nameIndex) {
                    return i + 1;
                }
            }
        }
        throw new LostConstantPoolEntryException("Cannot find class ConstantPoolEntry with " + (clz.namespace() != null ? clz.namespace() + "." : "") + clz.name());
    }

}
