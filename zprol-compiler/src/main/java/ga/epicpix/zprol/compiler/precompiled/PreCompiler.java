package ga.epicpix.zprol.compiler.precompiled;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tree.*;
import ga.epicpix.zprol.structures.FunctionModifiers;

import java.util.List;

public class PreCompiler {

    public static PreCompiledData preCompile(String sourceFile, FileTree file, DataParser parser) {
        PreCompiledData pre = new PreCompiledData();
        pre.parser = parser;
        pre.sourceFile = sourceFile;
        List<IDeclaration> declarations = file.declarations;
        for(int i = 0; i < declarations.size(); i++) {
            IDeclaration nsSearch = declarations.get(i);
            if(nsSearch instanceof NamespaceTree) {
                if(i != 0) {
                    throw new TokenLocatedException("Namespace not defined at the top of the file", nsSearch, parser);
                }
            }
        }

        for(IDeclaration decl : declarations) {
            if(decl instanceof UsingTree) {
                UsingTree using = (UsingTree) decl;
                pre.using.add(using.identifier.toString());
            }else if(decl instanceof NamespaceTree) {
                NamespaceTree namespace = (NamespaceTree) decl;
                if(pre.namespace != null) throw new TokenLocatedException("Defined namespace for a file multiple times", namespace, parser);
                pre.namespace = namespace.identifier.toString();
            }else if(decl instanceof FunctionTree) {
                FunctionTree func = (FunctionTree) decl;
                pre.functions.add(parseFunction(func, parser));
            }else if(decl instanceof FieldTree) {
                FieldTree f = (FieldTree) decl;
                PreField field = new PreField(f.value);
                field.type = f.type.toString();
                field.name = f.name.toStringRaw();
                if(f.isConst) {
                    field.modifiers.add(PreFieldModifiers.CONST);
                }
                pre.fields.add(field);
            }else if(decl instanceof ClassTree) {
                ClassTree clz = (ClassTree) decl;
                PreClass clazz = new PreClass();
                clazz.namespace = pre.namespace;
                clazz.name = clz.name.toStringRaw();

                for(IDeclaration decl2 : clz.declarations) {
                    if(decl2 instanceof FieldTree) {
                        FieldTree f = (FieldTree) decl2;
                        PreField field = new PreField(f.value);
                        field.type = f.type.toString();
                        field.name = f.name.toStringRaw();
                        clazz.fields.add(field);
                    }else if(decl2 instanceof FunctionTree) {
                        clazz.methods.add(parseFunction((FunctionTree) decl2, parser));
                    }else {
                        throw new TokenLocatedException("Unsupported declaration \"" + decl2.getClass().getSimpleName() + "\"", decl2, parser);
                    }
                }

                pre.classes.add(clazz);
            }else {
                throw new TokenLocatedException("Unsupported declaration \"" + decl.getClass().getSimpleName() + "\"", decl, parser);
            }
        }

        return pre;
    }

    private static PreFunction parseFunction(FunctionTree function, DataParser parser) {
        PreFunction func = new PreFunction();
        for(ModifierTree modifier : function.modifiers.modifiers) {
            FunctionModifiers modifiers = FunctionModifiers.getModifier(modifier.mod);
            if(func.modifiers.contains(modifiers)) {
                throw new TokenLocatedException("Duplicate function modifier: '" + modifiers.name().toLowerCase() + "'", modifier, parser);
            }
            func.modifiers.add(modifiers);
        }
        func.returnType = function.type.toString();
        func.name = function.name.toStringRaw();
        ParametersTree paramList = function.parameters;
        for (ParameterTree paramTree : paramList.parameters) {
            PreParameter param = new PreParameter();
            param.type = paramTree.type.toString();
            param.name = paramTree.name.toStringRaw();
            func.parameters.add(param);
        }
        if(func.hasCode()) {
            if(function.code == null) {
                throw new TokenLocatedException("Expected code", function, parser);
            }
            func.code.addAll(function.code.statements);
        }else {
            if(function.code != null) {
                throw new TokenLocatedException("Expected no code", function, parser);
            }
        }
        return func;
    }

}
