package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.parser.tree.ArgumentsTree;
import ga.epicpix.zprol.parser.tree.IExpression;
import ga.epicpix.zprol.parser.tree.ITree;

import java.util.ArrayList;

public class CompilerIdentifierDataFunction extends CompilerIdentifierData {

    public final String identifier;
    public final IExpression[] arguments;

    public CompilerIdentifierDataFunction(ITree location, String identifier, ArgumentsTree args) {
        super(location);
        this.identifier = identifier;
        arguments = args.arguments();
    }

    public String getFunctionName() {
        return identifier;
    }

    public ArrayList<LookupFunction> lookupFunction(CompiledData data, PreClass classContext, boolean searchPublic) {
        ArrayList<LookupFunction> possibilities = new ArrayList<>();
        String funcName = getFunctionName();

        if(classContext != null) {
            for (var method : classContext.methods){
                if (method.name.equals(funcName)) {
                    if (method.parameters.size() == arguments.length) {
                        possibilities.add(new LookupFunction(true, method, classContext.namespace));
                    }
                }
            }
        }
        if(searchPublic) {
            for (var using : data.getUsing()) {
                for (var func : using.functions) {
                    if (func.name.equals(funcName)) {
                        if (func.parameters.size() == arguments.length) {
                            possibilities.add(new LookupFunction(false, func, using.namespace));
                        }
                    }
                }
            }
        }
        return possibilities;
    }

}
