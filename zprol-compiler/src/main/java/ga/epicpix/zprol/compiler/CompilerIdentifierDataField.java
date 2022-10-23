package ga.epicpix.zprol.compiler;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.compiled.locals.LocalVariable;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.compiler.precompiled.PreCompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreField;
import ga.epicpix.zprol.compiler.precompiled.PreFieldModifiers;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tree.ITree;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.structures.FieldModifiers;
import ga.epicpix.zprol.structures.IBytecodeStorage;
import ga.epicpix.zprol.types.BooleanType;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Type;
import ga.epicpix.zprol.types.VoidType;

import java.util.EnumSet;
import java.util.Objects;

import static ga.epicpix.zprol.compiler.Compiler.doCast;
import static ga.epicpix.zprol.compiler.CompilerUtils.*;

public class CompilerIdentifierDataField extends CompilerIdentifierData {

    public final String identifier;

    public CompilerIdentifierDataField(ITree location, String identifier) {
        super(location);
        this.identifier = identifier;
    }

    public String getFieldName() {
        return identifier;
    }

    public Type loadField(PreClass classContext, LocalScopeManager localsManager, IBytecodeStorage bytecode, CompiledData data, boolean searchPublic) {
        String fieldName = getFieldName();

        if(searchPublic) {
            LocalVariable local = localsManager.tryGetLocalVariable(fieldName);
            if (local != null) {
                Type localType = local.type;

                if (localType instanceof PrimitiveType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) localType).getSize(), "load_local", local.index));
                } else if (localType instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_local", local.index));
                } else if (localType instanceof VoidType) {
                    throw new TokenLocatedException("Cannot load void type");
                } else {
                    bytecode.pushInstruction(getConstructedInstruction("aload_local", local.index));
                }

                return local.type;
            }
        }

        if(classContext != null) {
            for(PreField field : classContext.fields) {
                if(field.name.equals(fieldName)) {
                    Type fieldType = data.resolveType(field.type);
                    if(searchPublic) {
                        bytecode.pushInstruction(getConstructedInstruction("aload_local", localsManager.getLocalVariable("this").index));
                    }
                    bytecode.pushInstruction(getConstructedInstruction("class_field_load", new Class(classContext.namespace, classContext.name, null, null), fieldName));
                    return fieldType;
                }
            }
        }
        if(searchPublic) {
            for(PreCompiledData using : data.getUsing()) {
                for(PreField field : using.fields) {
                    if(!field.name.equals(fieldName)) continue;
                    Type fieldType = data.resolveType(field.type);

                    EnumSet<FieldModifiers> modifiers = EnumSet.noneOf(FieldModifiers.class);
                    for(PreFieldModifiers modifier : field.modifiers) {
                        modifiers.add(modifier.getCompiledModifier());
                    }
                    Field f = new Field(using.namespace, modifiers, field.name, fieldType, null);

                    if(fieldType instanceof PrimitiveType) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) fieldType).getSize(), "load_field", f));
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
            for(GeneratedData gen : data.getAllGenerated()) {
                for(Field field : gen.fields) {
                    if(Objects.equals(field.namespace, data.namespace) || data.getUsingNamespaces().contains(field.namespace)) {
                        if(!field.name.equals(fieldName)) continue;
                        Type fieldType = field.type;
                        if(fieldType instanceof PrimitiveType) {
                            bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) fieldType).getSize(), "load_field", field));
                        } else if(fieldType instanceof BooleanType) {
                            bytecode.pushInstruction(getConstructedSizeInstruction(8, "load_field", field));
                        } else if(fieldType instanceof VoidType) {
                            throw new TokenLocatedException("Cannot load void type");
                        } else {
                            bytecode.pushInstruction(getConstructedInstruction("aload_field", field));
                        }

                        return fieldType;
                    }
                }
            }
        }

        return null;
    }

    public Type storeField(PreClass classContext, LocalScopeManager localsManager, Type type, IBytecodeStorage bytecode, CompiledData data, boolean searchPublic, DataParser parser) {
        String fieldName = getFieldName();

        if(searchPublic) {
            LocalVariable local = localsManager.tryGetLocalVariable(fieldName);
            if (local != null) {
                Type localType = local.type;
                if(type != null) doCast(localType, type, false, bytecode, location, parser);
                if (localType instanceof PrimitiveType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) localType).getSize(), "store_local", local.index));
                } else if (localType instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_local", local.index));
                } else if (localType instanceof VoidType) {
                    throw new TokenLocatedException("Cannot store void type");
                } else {
                    bytecode.pushInstruction(getConstructedInstruction("astore_local", local.index));
                }

                return local.type;
            }
        }

        if(classContext != null) {
            for(PreField field : classContext.fields) {
                if(field.name.equals(fieldName)) {
                    Type fieldType = data.resolveType(field.type);
                    if(type != null) doCast(fieldType, type, false, bytecode, location, parser);
                    if(searchPublic) {
                        bytecode.pushInstruction(getConstructedInstruction("aload_local", localsManager.getLocalVariable("this").index));
                    }
                    bytecode.pushInstruction(getConstructedInstruction("class_field_store", new Class(classContext.namespace, classContext.name, null, null), fieldName));
                    return fieldType;
                }
            }
        }
        if(searchPublic) {
            for(PreCompiledData using : data.getUsing()) {
                for(PreField field : using.fields) {
                    if(!field.name.equals(fieldName)) continue;
                    Type fieldType = data.resolveType(field.type);
                    EnumSet<FieldModifiers> modifiers = EnumSet.noneOf(FieldModifiers.class);
                    for(PreFieldModifiers modifier : field.modifiers) {
                        modifiers.add(modifier.getCompiledModifier());
                    }
                    Field f = new Field(using.namespace, modifiers, field.name, fieldType, null);
                    if(modifiers.contains(FieldModifiers.CONST)) throw new TokenLocatedException("Cannot assign to const value");
                    if(type != null) doCast(fieldType, type, false, bytecode, location, parser);
                    if(fieldType instanceof PrimitiveType) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) fieldType).getSize(), "store_field", f));
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
            for(GeneratedData gen : data.getAllGenerated()) {
                for(Field field : gen.fields) {
                    if(Objects.equals(field.namespace, data.namespace) || data.getUsingNamespaces().contains(field.namespace)) {
                        if(field.modifiers.contains(FieldModifiers.CONST)) throw new TokenLocatedException("Cannot assign to const value");
                        Type fieldType = field.type;
                        if(type != null) doCast(fieldType, type, false, bytecode, location, parser);
                        if(fieldType instanceof PrimitiveType) {
                            bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) fieldType).getSize(), "store_field", field));
                        } else if(fieldType instanceof BooleanType) {
                            bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_field", field));
                        } else if(fieldType instanceof VoidType) {
                            throw new TokenLocatedException("Cannot store void type");
                        } else {
                            bytecode.pushInstruction(getConstructedInstruction("astore_field", field));
                        }

                        return fieldType;
                    }
                }
            }
        }

        return null;
    }

}
