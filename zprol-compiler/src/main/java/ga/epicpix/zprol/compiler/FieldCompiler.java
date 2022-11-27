package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.compiler.precompiled.PreField;
import ga.epicpix.zprol.compiler.precompiled.PreFieldModifiers;
import ga.epicpix.zprol.data.ConstantValue;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tree.IExpression;
import ga.epicpix.zprol.parser.tree.LiteralTree;
import ga.epicpix.zprol.parser.tree.LiteralType;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.types.*;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Objects;

import static ga.epicpix.zprol.compiler.Compiler.doCast;
import static ga.epicpix.zprol.compiler.Compiler.generateInstructionsFromExpression;
import static ga.epicpix.zprol.compiler.CompilerUtils.*;

public class FieldCompiler {

    public static Object compileField(CompiledData data, PreField field, PreClass thisClass, DataParser parser) {
        Type type = data.resolveType(field.type);
        if(type instanceof VoidType) {
            throw new TokenLocatedException("Cannot create a field with void type");
        }
        EnumSet<FieldModifiers> modifiers = EnumSet.noneOf(FieldModifiers.class);
        for(PreFieldModifiers modifier : field.modifiers) {
            modifiers.add(modifier.getCompiledModifier());
        }

        if(thisClass != null) {
            return new ClassField(field.name, type);
        }
        Field f = new Field(data.namespace, modifiers, field.name, type, null);
        ConstantValue defaultValue = null;
        if(field.isConst()) {
            IExpression value = field.defaultValue;
            if(value instanceof LiteralTree) {
                LiteralTree literal = (LiteralTree) value;
                if(literal.type == LiteralType.INTEGER) {
                    if(!(type instanceof PrimitiveType)) {
                        throw new TokenLocatedException("Expected " + type.normalName() + ", got an integer", value, parser);
                    }
                    PrimitiveType prim = (PrimitiveType) type;
                    BigInteger n = (BigInteger) literal.value;
                    if(prim.getSize() == 1) defaultValue = new ConstantValue(n.byteValue());
                    else if(prim.getSize() == 2) defaultValue = new ConstantValue(n.shortValue());
                    else if(prim.getSize() == 4) defaultValue = new ConstantValue(n.intValue());
                    else if(prim.getSize() == 8) defaultValue = new ConstantValue(n.longValue());
                }else if(literal.type == LiteralType.BOOLEAN) {
                    if(!(type instanceof BooleanType)) {
                        throw new TokenLocatedException("Expected " + type.normalName() + ", got a boolean", value, parser);
                    }
                    defaultValue = new ConstantValue(literal.value);
                }else if(literal.type == LiteralType.STRING) {
                    if(!(type instanceof ClassType)) {
                        throw new TokenLocatedException("Expected " + type.normalName() + ", got a string", value, parser);
                    }else {
                        ClassType c = (ClassType) type;
                        if(!Objects.equals(c.getNamespace(), "zprol.lang") || !c.getName().equals("String")) {
                            throw new TokenLocatedException("Expected " + type.normalName() + ", got a string", value, parser);
                        }
                    }
                    defaultValue = new ConstantValue(literal.value);
                }else if(literal.type == LiteralType.NULL) {
                    if(!(type instanceof ClassType || type instanceof ArrayType)) {
                        throw new TokenLocatedException("Expected a reference type, got " + type.normalName(), value, parser);
                    }
                    defaultValue = new ConstantValue(null);
                }else {
                    throw new TokenLocatedException("Unhandled case for LiteralType", value, parser);
                }
            }else {
                IBytecodeStorage bytecode = createStorage();
                ArrayDeque<Type> got = new ArrayDeque<Type>();
                generateInstructionsFromExpression(value, null, got, data, new FunctionCodeScope(new LocalScopeManager(), null), bytecode, false, parser);
                doCast(got.pop(), type, false, bytecode, value, parser);
                if(type instanceof PrimitiveType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(((PrimitiveType) type).getSize(), "store_field", f));
                } else if(type instanceof BooleanType) {
                    bytecode.pushInstruction(getConstructedSizeInstruction(8, "store_field", f));
                } else {
                    bytecode.pushInstruction(getConstructedInstruction("astore_field", f));
                }
                Function initFunction = null;
                for(Function func : data.functions) {
                    if(!Objects.equals(func.namespace, data.namespace)) continue;
                    if(!func.name.equals(".init")) continue;
                    if(!func.signature.validateFunctionSignature(new FunctionSignature(data.resolveType("void")))) continue;
                    initFunction = func;
                    break;
                }
                if(initFunction == null) {
                    initFunction = new Function(data.namespace, EnumSet.noneOf(FunctionModifiers.class), ".init", new FunctionSignature(data.resolveType("void")), bytecode);
                    bytecode.pushInstruction(getConstructedInstruction("vreturn"));
                    data.functions.add(initFunction);
                }else {
                    IBytecodeStorage code = initFunction.code;
                    for(IBytecodeInstruction i : bytecode.getInstructions()) code.pushInstruction(code.getInstructionsLength() - 1, i);
                }
            }
        }
        return new Field(data.namespace, modifiers, field.name, type, defaultValue);
    }

}
