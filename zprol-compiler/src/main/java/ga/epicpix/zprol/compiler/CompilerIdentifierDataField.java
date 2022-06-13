package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.structures.IBytecodeStorage;
import ga.epicpix.zprol.types.BooleanType;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Type;
import ga.epicpix.zprol.types.VoidType;

import static ga.epicpix.zprol.compiler.Compiler.doCast;
import static ga.epicpix.zprol.compiler.CompilerUtils.*;

public class CompilerIdentifierDataField extends CompilerIdentifierData {

    public final String identifier;

    public CompilerIdentifierDataField(Token location, String identifier) {
        super(location);
        this.identifier = identifier;
    }

    public String getFieldName() {
        return identifier;
    }

    public Type loadField(PreClass classContext, LocalScopeManager localsManager, IBytecodeStorage bytecode, CompiledData data, boolean searchPublic) {
        String fieldName = getFieldName();

        if(searchPublic) {
            var local = localsManager.tryGetLocalVariable(fieldName);
            if (local != null) {
                var localType = local.type();

                if (localType instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_local", local.index()));
                } else if (localType instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_local", local.index()));
                } else if (localType instanceof VoidType) {
                    throw new TokenLocatedException("Cannot load void type");
                } else {
                    bytecode.pushInstruction(getConstructedInstruction("aload_local", local.index()));
                }

                return local.type();
            }
        }

        if(classContext != null) {
            for(var field : classContext.fields) {
                if(field.name.equals(fieldName)) {
                    var fieldType = data.resolveType(field.type);
                    if(searchPublic) {
                        bytecode.pushInstruction(getConstructedInstruction("aload_local", localsManager.getLocalVariable("this").index()));
                    }
                    bytecode.pushInstruction(getConstructedInstruction("class_field_load", new Class(classContext.namespace, classContext.name, null, null), fieldName));
                    return fieldType;
                }
            }
        }
        if(searchPublic) {
            for(var using : data.getUsing()) {
                for(var field : using.fields) {
                    if(!field.name.equals(fieldName)) continue;
                    var fieldType = data.resolveType(field.type);
                    var f = new Field(using.namespace, field.name, fieldType);

                    if(fieldType instanceof PrimitiveType primitive) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "load_field", f));
                    } else if(fieldType instanceof BooleanType) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_field", f));
                    } else if(fieldType instanceof VoidType) {
                        throw new TokenLocatedException("Cannot load void type");
                    } else {
                        bytecode.pushInstruction(getConstructedInstruction("aload_field", f));
                    }

                    return fieldType;

                }
            }
        }

        return null;
    }

    public Type storeField(PreClass classContext, LocalScopeManager localsManager, Type type, IBytecodeStorage bytecode, CompiledData data, boolean searchPublic) {
        String fieldName = getFieldName();

        if(searchPublic) {
            var local = localsManager.tryGetLocalVariable(fieldName);
            if (local != null) {
                var localType = local.type();
                if(type != null) doCast(localType, type, false, bytecode, location);
                if (localType instanceof PrimitiveType primitive) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_local", local.index()));
                } else if (localType instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_local", local.index()));
                } else if (localType instanceof VoidType) {
                    throw new TokenLocatedException("Cannot store void type");
                } else {
                    bytecode.pushInstruction(getConstructedInstruction("astore_local", local.index()));
                }

                return local.type();
            }
        }

        if(classContext != null) {
            for(var field : classContext.fields) {
                if(field.name.equals(fieldName)) {
                    var fieldType = data.resolveType(field.type);
                    if(type != null) doCast(fieldType, type, false, bytecode, location);
                    if(searchPublic) {
                        bytecode.pushInstruction(getConstructedInstruction("aload_local", localsManager.getLocalVariable("this").index()));
                    }
                    bytecode.pushInstruction(getConstructedInstruction("class_field_store", new Class(classContext.namespace, classContext.name, null, null), fieldName));
                    return fieldType;
                }
            }
        }
        if(searchPublic) {
            for(var using : data.getUsing()) {
                for(var field : using.fields) {
                    if(!field.name.equals(fieldName)) continue;
                    var fieldType = data.resolveType(field.type);
                    var f = new Field(using.namespace, field.name, fieldType);
                    if(type != null) doCast(fieldType, type, false, bytecode, location);
                    if(fieldType instanceof PrimitiveType primitive) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(primitive.getSize(), "store_field", f));
                    } else if(fieldType instanceof BooleanType) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_field", f));
                    } else if(fieldType instanceof VoidType) {
                        throw new TokenLocatedException("Cannot store void type");
                    } else {
                        bytecode.pushInstruction(getConstructedInstruction("astore_field", f));
                    }

                    return fieldType;

                }
            }
        }

        return null;
    }

}
