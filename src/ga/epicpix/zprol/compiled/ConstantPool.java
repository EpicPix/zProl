package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.exceptions.LostConstantPoolEntryException;

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
                    return i;
                }
            }
        }
        entries.add(new ConstantPoolEntry.FunctionEntry(namespaceIndex, nameIndex, signatureIndex));
        return entries.size();
    }

    public int getOrCreateStringIndex(String str) {
        if(str == null) return 0;

        for(int i = 0; i < entries.size(); i++) {
            if(entries.get(i) instanceof ConstantPoolEntry.StringEntry e) {
                if(e.getString().equals(str)) {
                    return i;
                }
            }
        }
        entries.add(new ConstantPoolEntry.StringEntry(str));
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

}