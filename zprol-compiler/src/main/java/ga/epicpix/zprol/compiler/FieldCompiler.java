package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.compiler.precompiled.PreField;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.structures.ClassField;
import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.structures.FieldModifiers;
import ga.epicpix.zprol.types.VoidType;

import java.util.EnumSet;

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
        return new Field(data.namespace, modifiers, field.name, type);
    }

}
