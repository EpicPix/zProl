package ga.epicpix.zprol.compiler;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.compiler.precompiled.PreCompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreFunction;
import ga.epicpix.zprol.parser.tree.ArgumentsTree;
import ga.epicpix.zprol.parser.tree.IExpression;
import ga.epicpix.zprol.parser.tree.ITree;
import ga.epicpix.zprol.structures.Function;

import java.util.ArrayList;
import java.util.Objects;

public class CompilerIdentifierDataFunction extends CompilerIdentifierData {

    public final String identifier;
    public final IExpression[] arguments;

    public CompilerIdentifierDataFunction(ITree location, String identifier, ArgumentsTree args) {
        super(location);
        this.identifier = identifier;
        arguments = args.arguments;
    }

    public String getFunctionName() {
        return identifier;
    }

    public ArrayList<LookupFunction> lookupFunction(CompiledData data, PreClass classContext, boolean searchPublic) {
        ArrayList<LookupFunction> possibilities = new ArrayList<>();
        String funcName = getFunctionName();

        if(classContext != null) {
            for (PreFunction method : classContext.methods){
                if (method.name.equals(funcName)) {
                    if (method.parameters.size() == arguments.length) {
                        possibilities.add(new LookupFunction(true, method, classContext.namespace));
                    }
                }
            }
        }
        if(searchPublic) {
            for (PreCompiledData using : data.getUsing()) {
                for (PreFunction func : using.functions) {
                    if (func.name.equals(funcName)) {
                        if (func.parameters.size() == arguments.length) {
                            possibilities.add(new LookupFunction(false, func, using.namespace));
                        }
                    }
                }
            }
            for (GeneratedData using : data.getAllGenerated()) {
                for (Function func : using.functions) {
                    if(Objects.equals(func.namespace, data.namespace) || data.getUsingNamespaces().contains(func.namespace)) {
                        if(func.name.equals(funcName)) {
                            if(func.signature.parameters.length == arguments.length) {
                                possibilities.add(new LookupFunction(false, func, func.namespace));
                            }
                        }
                    }
                }
            }
        }
        return possibilities;
    }

}
