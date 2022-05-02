package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.zld.Language;
import ga.epicpix.zprol.compiled.ConstantPoolEntry.FunctionEntry;
import ga.epicpix.zprol.compiled.ConstantPoolEntry.StringEntry;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
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

    public ArrayList<PreCompiledData> getUsing() {
        return using;
    }

    private final ArrayList<Function> functions = new ArrayList<>();

    public ArrayList<Function> getFunctions() {
        return new ArrayList<>(functions);
    }

    public Function getFunction(String namespace, String name, FunctionSignature sig) {
        for(Function func : functions) {
            if(!func.namespace().equals(namespace)) continue;
            if(!func.name().equals(name)) continue;

            var signature = func.signature();
            if(sig.returnType() != null && signature.returnType() != sig.returnType()) continue;
            var sigParams = sig.parameters();
            if(signature.parameters().length != sigParams.length) continue;

            boolean success = true;
            for(int i = 0; i<signature.parameters().length; i++) {
                if(signature.parameters()[i] != sigParams[i]) {
                    success = false;
                    break;
                }
            }
            if(success) {
                return func;
            }
        }
        throw new FunctionNotDefinedException(name);
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public PrimitiveType resolveType(String type) {
        PrimitiveType t = Language.TYPES.get(type);
        if(t != null) return t;
        throw new UnknownTypeException(type);
    }

}
