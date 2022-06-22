package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.compiler.precompiled.PreField;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.structures.ClassField;
import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.structures.FieldModifiers;
import ga.epicpix.zprol.types.BooleanType;
import ga.epicpix.zprol.types.ClassType;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.VoidType;

import java.util.EnumSet;
import java.util.Objects;

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
                throw new TokenLocatedException("TODO: Allow non integers", value);
            }
        }
        return new Field(data.namespace, modifiers, field.name, type);
    }

}
