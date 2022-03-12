package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.zld.Language;
import ga.epicpix.zprol.compiled.ConstantPoolEntry.FunctionEntry;
import ga.epicpix.zprol.compiled.ConstantPoolEntry.StringEntry;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import java.util.ArrayList;

public class CompiledData {

    public final String namespace;
    private final ArrayList<PreCompiledData> using = new ArrayList<>();

    public CompiledData(String namespace) {
        this.namespace = namespace;
    }

    public void using(PreCompiledData using) {
        this.using.add(using);
    }

    private final ArrayList<Function> functions = new ArrayList<>();
    private final ArrayList<ConstantPoolEntry> constantPool = new ArrayList<>();

    public short getOrCreateFunctionIndex(Function func) {
        for(short i = 0; i<constantPool.size(); i++) {
            if(constantPool.get(i) instanceof FunctionEntry e) {
                if(e.getName().equals(func.name) && e.getSignature().validateFunctionSignature(func.signature)) {
                    return i;
                }
            }
        }
        constantPool.add(new FunctionEntry(namespace, func));
        return (short) (constantPool.size() - 1);
    }

    public short getOrCreateStringIndex(String str) {
        for(short i = 0; i<constantPool.size(); i++) {
            if(constantPool.get(i) instanceof StringEntry e) {
                if(e.getString().equals(str)) {
                    return i;
                }
            }
        }
        constantPool.add(new StringEntry(str));
        return (short) (constantPool.size() - 1);
    }

    public ArrayList<ConstantPoolEntry> getConstantPool() {
        return new ArrayList<>(constantPool);
    }

    public ArrayList<Function> getFunctions() {
        return new ArrayList<>(functions);
    }

    public Function getFunction(String name, FunctionSignature sig) {
        for(Function func : functions) {
            if(func.name.equals(name)) {
                if(sig.returnType == null || func.signature.returnType == sig.returnType) {
                    if(func.signature.parameters.length == sig.parameters.length) {
                        boolean success = true;
                        for(int i = 0; i<func.signature.parameters.length; i++) {
                            if(func.signature.parameters[i] != sig.parameters[i]) {
                                success = false;
                                break;
                            }
                        }
                        if(success) {
                            return func;
                        }
                    }
                }
            }
        }
        throw new FunctionNotDefinedException(name);
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public PrimitiveType resolveType(String type) throws UnknownTypeException {
        PrimitiveType t = Language.TYPES.get(type);
        if(t != null) return t;
        throw new NotImplementedException("Not implemented yet");
    }

}
