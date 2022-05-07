package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.zld.Language;
import ga.epicpix.zprol.exceptions.compilation.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.compilation.UnknownTypeException;
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
    private final ArrayList<Class> classes = new ArrayList<>();

    public ArrayList<Function> getFunctions() {
        return new ArrayList<>(functions);
    }

    public ArrayList<Class> getClasses() {
        return new ArrayList<>(classes);
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

    public void addClass(Class clazz) {
        classes.add(clazz);
    }

    public Type resolveType(String type) {
        var t = Language.TYPES.get(type);
        if(t != null) return t;
        String namespace = type.lastIndexOf('.') != -1 ? type.substring(0, type.lastIndexOf('.')) : null;
        for(var data : using) {
            if(data.namespace != null && !data.namespace.equals(namespace)) continue;
            for(var clz : data.classes) {
                if(clz.name.equals(type)) {
                    return new ClassType(data.namespace, clz.name);
                }
            }
        }
        throw new UnknownTypeException(type);
    }

}
