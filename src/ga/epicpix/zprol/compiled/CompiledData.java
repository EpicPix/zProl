package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.DataParser;
import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.ConstantPoolEntry.FunctionEntry;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import ga.epicpix.zprol.tokens.OperatorToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.WordToken;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class CompiledData {

    private final ArrayList<Structure> structures = new ArrayList<>();
    private final ArrayList<Object> objects = new ArrayList<>();
    private final ArrayList<Function> functions = new ArrayList<>();
    private final ArrayList<ObjectField> fields = new ArrayList<>();
    private final HashMap<String, Type> typedef = new HashMap<>();
    private final ArrayList<ConstantPoolEntry> constantPool = new ArrayList<>();

    public short getFunctionIndex(Function func) {
        for(short i = 0; i<constantPool.size(); i++) {
            if(constantPool.get(i) instanceof FunctionEntry) {
                FunctionEntry e = (FunctionEntry) constantPool.get(i);
                if(e.getName().equals(func.name) && e.getSignature().validateFunctionSignature(func.signature.getNormalSignature())) {
                    return i;
                }
            }
        }
        constantPool.add(new FunctionEntry(func));
        return (short) (constantPool.size() - 1);
    }

    public ArrayList<Structure> getStructures() {
        return new ArrayList<>(structures);
    }

    public ArrayList<Object> getObjects() {
        return new ArrayList<>(objects);
    }

    public ArrayList<Function> getFunctions() {
        return new ArrayList<>(functions);
    }

    public ArrayList<ObjectField> getFields() {
        return new ArrayList<>(fields);
    }
    public ObjectField getField(String name) {
        for(ObjectField field : fields) {
            if(field.name.equals(name)) {
                return field;
            }
        }
        throw new VariableNotDefinedException(name);
    }

    public short getFieldIndex(String name) {
        short index = 0;
        for(ObjectField field : fields) {
            if(field.name.equals(name)) {
                return index;
            }
            index++;
        }
        throw new VariableNotDefinedException(name);
    }

    public Function getFunction(String name, TypeFunctionSignature sig) {
        for(Function func : functions) {
            if(func.name.equals(name)) {
                if(sig.returnType == null || func.signature.returnType.type == sig.returnType.type) {
                    if(func.signature.parameters.length == sig.parameters.length) {
                        boolean success = true;
                        for(int i = 0; i<func.signature.parameters.length; i++) {
                            if((func.signature.parameters[i].type.type != sig.parameters[i].type) && !(func.signature.parameters[i].type.type.isNumberType() && sig.parameters[i].type == Types.NUMBER)) {
                                success = false;
                                break;
                            }
                        }
                        if(success) {
                            return func;
                        }
                    }
                }
            }
        }
        throw new FunctionNotDefinedException(name);
    }

    public void addTypeDefinition(String typeName, Type type) {
        if(typedef.get(typeName) == null) {
            typedef.put(typeName, type);
        }else {
            throw new RuntimeException("Type definition for " + typeName + " is already defined");
        }
    }

    public void addStructure(Structure structure) {
        structures.add(structure);
    }

    public void addObject(Object object) {
        objects.add(object);
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public void addField(ObjectField field) {
        fields.add(field);
    }

    public Function getInitFunction() {
        Function func;
        try {
            func = getFunction("<clinit>", new TypeFunctionSignature(new Type(Types.VOID)));
        } catch(FunctionNotDefinedException e) {
            func = new Function("<clinit>", new TypeFunctionSignatureNamed(new Type(Types.VOID)), new ArrayList<>(), new Bytecode());
            addFunction(func);
        }
        return func;
    }

    public Type resolveType(SeekIterator<Token> iter) throws UnknownTypeException {
        String type = ((WordToken) iter.next()).word;
        if(type == null) return new Type(Types.NONE);
        if(iter.seek().getType() == TokenType.OPEN) {
            iter.next();
            Type ret = resolveType(type);
            ArrayList<Type> parameters = new ArrayList<>();
            while(true) {
                if(iter.seek().getType() == TokenType.CLOSE) {
                    iter.next();
                    break;
                }
                Type param = resolveType(iter);
                parameters.add(param);

                if(iter.seek().getType() == TokenType.COMMA) {
                    iter.next();
                }
            }
            return new TypeFunctionSignature(ret, parameters.toArray(new Type[0]));
        }
        if(type.equals("int8")) return new Type(Types.INT8);
        else if(type.equals("int16")) return new Type(Types.INT16);
        else if(type.equals("int32")) return new Type(Types.INT32);
        else if(type.equals("int64")) return new Type(Types.INT64);
        else if(type.equals("uint8")) return new Type(Types.UINT8);
        else if(type.equals("uint16")) return new Type(Types.UINT16);
        else if(type.equals("uint32")) return new Type(Types.UINT32);
        else if(type.equals("uint64")) return new Type(Types.UINT64);
        else if(type.equals("pointer")) {
            if(iter.seek().getType() == TokenType.OPERATOR && ((OperatorToken) iter.seek()).operator.equals("<")) {
                iter.next();
                Type ret = resolveType(iter);
                Token n = iter.next();
                if(n.getType() != TokenType.OPERATOR && !((OperatorToken) n).operator.equals(">")) {
                    throw new RuntimeException("Missing '>'");
                }
                return new TypePointer(ret);
            }else {
                return new TypePointer(null);
            }
        } else if(type.equals("void")) return new Type(Types.VOID);
        Type t = typedef.get(type);
        if(t != null) return t;

        for(Structure struct : structures) {
            if(struct.name.equals(type)) {
                return new TypeStructure(struct.name);
            }
        }

        for(Object obj : objects) {
            if(obj.name.equals(type)) {
                return new TypeObject(obj.name);
            }
        }

        throw new UnknownTypeException("Unknown type: " + type);
    }

    public Type resolveType(String type) throws UnknownTypeException {
        if(type == null) return new Type(Types.NONE);
        if(type.equals("int8")) return new Type(Types.INT8);
        else if(type.equals("int16")) return new Type(Types.INT16);
        else if(type.equals("int32")) return new Type(Types.INT32);
        else if(type.equals("int64")) return new Type(Types.INT64);
        else if(type.equals("uint8")) return new Type(Types.UINT8);
        else if(type.equals("uint16")) return new Type(Types.UINT16);
        else if(type.equals("uint32")) return new Type(Types.UINT32);
        else if(type.equals("uint64")) return new Type(Types.UINT64);
        else if(type.equals("void")) return new Type(Types.VOID);
        Type t = typedef.get(type);
        if(t != null) return t;

        for(Structure struct : structures) {
            if(struct.name.equals(type)) {
                return new TypeStructure(struct.name);
            }
        }

        for(Object obj : objects) {
            if(obj.name.equals(type)) {
                return new TypeObject(obj.name);
            }
        }

        DataParser parser = new DataParser("<internal>", type);

        String pre = parser.nextWord();

        if(pre.equals("pointer")) {
            Type t2 = null;
            if(parser.seekWord().equals("<")) {
                parser.nextWord();
                t2 = resolveType(parser.nextType());
                if(!parser.nextWord().equals(">")) {
                    throw new RuntimeException("Missing '>'");
                }
            }
            return new TypePointer(t2);
        }

        if(parser.seekWord() == null || !parser.nextWord().equals("(")) {
            throw new UnknownTypeException("Unknown type: " + type);
        }
        Type ret = resolveType(pre);
        ArrayList<Type> parameters = new ArrayList<>();
        while(true) {
            if(parser.seekWord().equals(")")) {
                parser.nextWord();
                break;
            }
            Type param = resolveType(parser.nextType());
            parameters.add(param);

            if(parser.seekWord().equals(",")) {
                parser.nextWord();
            }
        }
        return new TypeFunctionSignature(ret, parameters.toArray(new Type[0]));
    }

    public static CompiledData load(File file) throws IOException {
        DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        if(input.readInt() != 0x7a50524c) throw new RuntimeException("Invalid file");
        CompiledData data = new CompiledData();
        int cpSize = input.readUnsignedShort();
        for(int i = 0; i<cpSize; i++) {
            data.constantPool.add(ConstantPoolEntry.read(input));
        }

        int namespaces = input.readInt();
        for(int p = 0; p<namespaces; p++) {
            int structuresLength = input.readUnsignedShort();
            for(int i = 0; i < structuresLength; i++) {
                String structName = input.readUTF();
                int fieldsLength = input.readUnsignedShort();
                ArrayList<StructureField> fields = new ArrayList<>();
                for(int j = 0; j < fieldsLength; j++) {
                    fields.add(new StructureField(input.readUTF(), readType(input)));
                }
                data.addStructure(new Structure(structName, fields));
            }
            int objectsLength = input.readUnsignedShort();
            for(int i = 0; i < objectsLength; i++) {
                String objectName = input.readUTF();
                Type ext = readType(input);
                int fieldsLength = input.readUnsignedShort();
                ArrayList<ObjectField> fields = new ArrayList<>();
                for(int j = 0; j < fieldsLength; j++) {
                    String name = input.readUTF();
                    Type type = readType(input);
                    fields.add(new ObjectField(name, type, Flag.fromBits(input.readInt())));
                }

                int functionsLength = input.readUnsignedShort();
                ArrayList<Function> functions = new ArrayList<>();
                for(int j = 0; j < functionsLength; j++) {
                    functions.add(readFunction(input));
                }
                data.addObject(new Object(objectName, ext, fields, functions));
            }
            int functionsLength = input.readUnsignedShort();
            for(int j = 0; j < functionsLength; j++) {
                data.functions.add(readFunction(input));
            }
            int fieldsLength = input.readUnsignedShort();
            for(int j = 0; j < fieldsLength; j++) {
                String name = input.readUTF();
                Type type = readType(input);
                data.fields.add(new ObjectField(name, type, Flag.fromBits(input.readInt())));
            }
        }
        return data;
    }

    private static Function readFunction(DataInputStream in) throws IOException {
        String name = in.readUTF();
        ArrayList<Flag> flags = Flag.fromBits(in.readInt());
        TypeFunctionSignatureNamed sig = readFunctionSignatureTypeNamed(in);
        Bytecode bytecode = null;
        if(!flags.contains(Flag.NO_IMPLEMENTATION)) {
            bytecode = new Bytecode();
            bytecode.load(in);
        }
        return new Function(name, sig, flags, bytecode);
    }

    private static TypeFunctionSignatureNamed readFunctionSignatureTypeNamed(DataInputStream in) throws IOException {
        Type ret = readType(in);
        int parametersLength = in.readUnsignedShort();
        TypeNamed[] parameters = new TypeNamed[parametersLength];
        for(int i = 0; i<parametersLength; i++) {
            String name = in.readUTF();
            parameters[i] = new TypeNamed(readType(in), name);
        }
        return new TypeFunctionSignatureNamed(ret, parameters);
    }

    public static TypeFunctionSignature readFunctionSignature(DataInputStream in) throws IOException {
        Type ret = readType(in);
        short paramsLength = in.readShort();
        Type[] params = new Type[paramsLength];
        for(int i = 0; i<params.length; i++) {
            params[i] = readType(in);
        }
        return new TypeFunctionSignature(ret, params);
    }

    private static Type readType(DataInputStream in) throws IOException {
        Types types = Types.fromId(in.readUnsignedByte());
        if(types.additionalData) {
            if(types == Types.FUNCTION_SIGNATURE) {
                return readFunctionSignature(in);
            }else if(types == Types.STRUCTURE) {
                return new TypeStructure(in.readUTF());
            }else if(types == Types.OBJECT) {
                return new TypeObject(in.readUTF());
            }else if(types == Types.NAMED) {
                String name = in.readUTF();
                return new TypeNamed(readType(in), name);
            }else if(types == Types.POINTER) {
                return new TypePointer(readType(in));
            }
            throw new RuntimeException("TODO");
        }else {
            return new Type(types);
        }
    }

    public void save(File file) throws IOException {
        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        output.writeBytes("zPRL");

        output.writeShort(constantPool.size());
        for(ConstantPoolEntry e : constantPool) e.write(output);

        output.writeInt(1);
        output.writeShort(structures.size());
        for(Structure struct : structures) {
            output.writeUTF(struct.name);
            output.writeShort(struct.fields.size());
            for(StructureField field : struct.fields) {
                output.writeUTF(field.name);
                writeType(field.type, output);
            }
        }

        output.writeShort(objects.size());
        for(Object obj : objects) {
            output.writeUTF(obj.name);
            writeType(obj.ext, output);
            output.writeShort(obj.fields.size());
            for(ObjectField field : obj.fields) {
                output.writeUTF(field.name);
                writeType(field.type, output);
                writeFlags(field.flags, output);
            }
            output.writeShort(obj.functions.size());
            for(Function func : obj.functions) {
                writeFunction(func, output);
            }
        }

        output.writeShort(functions.size());
        for(Function func : functions) {
            writeFunction(func, output);
        }

        output.writeShort(fields.size());
        for(ObjectField field : fields) {
            output.writeUTF(field.name);
            writeType(field.type, output);
            writeFlags(field.flags, output);
        }

        output.close();
    }

    private static void writeFlags(ArrayList<Flag> flags, DataOutputStream out) throws IOException {
        int flagsOut = 0;
        for(Flag f : flags) {
            flagsOut |= f.mask;
        }
        out.writeInt(flagsOut);
    }

    private static void writeFunction(Function func, DataOutputStream out) throws IOException {
        out.writeUTF(func.name);
        writeFlags(func.flags, out);
        writeFunctionSignatureTypeNamed(func.signature, out);
        if(!func.flags.contains(Flag.NO_IMPLEMENTATION)) {
            func.code.write(out);
        }
    }

    private static void writeFunctionSignatureTypeNamed(TypeFunctionSignatureNamed sig, DataOutputStream out) throws IOException {
        writeType(sig.returnType, out);
        out.writeShort(sig.parameters.length);
        for(TypeNamed param : sig.parameters) {
            out.writeUTF(param.name);
            writeType(param.type, out);
        }
    }

    public static void writeFunctionSignatureType(TypeFunctionSignature sig, DataOutputStream out) throws IOException {
        writeType(sig.returnType, out);
        out.writeShort(sig.parameters.length);
        for(Type param : sig.parameters) {
            writeType(param, out);
        }
    }

    private static void writeType(Type type, DataOutputStream out) throws IOException {
        out.writeByte(type.type.id);
        if(type.type.additionalData) {
            if(type instanceof TypeFunctionSignature) {
                writeFunctionSignatureType((TypeFunctionSignature) type, out);
            }else if(type instanceof TypeStructure) {
                TypeStructure sig = (TypeStructure) type;
                out.writeUTF(sig.name);
            }else if(type instanceof TypeObject) {
                TypeObject obj = (TypeObject) type;
                out.writeUTF(obj.name);
            }else if(type instanceof TypeNamed) {
                TypeNamed named = (TypeNamed) type;
                out.writeUTF(named.name);
                writeType(named.type, out);
            }else if(type instanceof TypePointer) {
                TypePointer ptr = (TypePointer) type;
                writeType(ptr.holding, out);
            }else {
                throw new RuntimeException("Unhandled additional data class: " + type.getClass());
            }
        }
    }

    public static CompiledData link(ArrayList<CompiledData> data) {
        CompiledData compiled = new CompiledData();
        for(CompiledData comp : data) {

        }
        throw new NotImplementedException("Linking is not implemented yet!");
    }

}
