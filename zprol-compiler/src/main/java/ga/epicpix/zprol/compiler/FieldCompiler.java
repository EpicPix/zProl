package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.compiler.precompiled.PreField;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.types.*;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Objects;

import static ga.epicpix.zprol.compiler.Compiler.doCast;
import static ga.epicpix.zprol.compiler.Compiler.generateInstructionsFromExpression;
import static ga.epicpix.zprol.compiler.CompilerUtils.*;

public class FieldCompiler {

    public static Object compileField(CompiledData data, PreField field, PreClass thisClass) {
        var type = data.resolveType(field.type);
        if(type instanceof VoidType) {
            throw new TokenLocatedException("Cannot create a field with void type");
        }
        var modifiers = EnumSet.noneOf(FieldModifiers.class);
        for(var modifier : field.modifiers) {
            modifiers.add(modifier.getCompiledModifier());
        }

        if(thisClass != null) {
            return new ClassField(field.name, type);
        }
        var f = new Field(data.namespace, modifiers, field.name, type);
        if(field.isConst()) {
            var value = field.defaultValue;
            if(value.getTokenWithName("DecimalInteger") != null) {
                if(!(type instanceof PrimitiveType)) {
                    throw new TokenLocatedException("Expected " + type.normalName() + ", got an integer", value);
                }

            }else if(value.getTokenWithName("HexInteger") != null) {
                if(!(type instanceof PrimitiveType)) {
                    throw new TokenLocatedException("Expected " + type.normalName() + ", got an integer", value);
                }

            }else if(value.getTokenWithName("Boolean") != null) {
                if(!(type instanceof BooleanType)) {
                    throw new TokenLocatedException("Expected " + type.normalName() + ", got a boolean", value);
                }

            }else if(value.getTokenWithName("String") != null) {
                if(!(type instanceof ClassType clzType) || !(Objects.equals(clzType.getNamespace(), "zprol.lang") || clzType.getName().equals("String"))) {
                    throw new TokenLocatedException("Expected " + type.normalName() + ", got a string", value);
                }

            }else {
                var bytecode = createStorage();
                var got = new ArrayDeque<Type>();
                generateInstructionsFromExpression(value, null, got, data, new FunctionCodeScope(new LocalScopeManager(), thisClass), bytecode, false);
                doCast(got.pop(), type, false, bytecode, value);
                if(type instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_field", f));
                } else if(type instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_field", f));
                } else {
                    bytecode.pushInstruction(getConstructedInstruction("astore_field", f));
                }
                Function initFunction = null;
                for(var func : data.functions) {
                    if(!Objects.equals(func.namespace(), data.namespace)) continue;
                    if(!func.name().equals(".init")) continue;
                    if(!func.signature().validateFunctionSignature(new FunctionSignature(data.resolveType("void")))) continue;
                    initFunction = func;
                    break;
                }
                if(initFunction == null) {
                    initFunction = new Function(data.namespace, EnumSet.noneOf(FunctionModifiers.class), ".init", new FunctionSignature(data.resolveType("void")), bytecode);
                    data.functions.add(initFunction);
                }else {
                    var code = initFunction.code();
                    for(var i : bytecode.getInstructions()) code.pushInstruction(i);
                }
            }
        }
        return f;
    }

}
