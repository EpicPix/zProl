package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.compiler.precompiled.PreField;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.structures.ClassField;
import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.types.VoidType;

public class FieldCompiler {

    public static Object compileField(CompiledData data, PreField field, PreClass thisClass) {
        var type = data.resolveType(field.type);
        if(type instanceof VoidType) {
            throw new TokenLocatedException("Cannot create a field with void type");
        }
        if(thisClass != null) {
            return new ClassField(field.name, type);
        }
        return new Field(data.namespace, field.name, type);
    }

}
