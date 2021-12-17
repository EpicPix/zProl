package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.DataParser;
import ga.epicpix.zprol.Language;
import ga.epicpix.zprol.compiled.ConstantPoolEntry.FunctionEntry;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class CompiledData {

    public final String namespace;

    public CompiledData(String namespace) {
        this.namespace = namespace;
    }

    private final ArrayList<Structure> structures = new ArrayList<>();
    private final ArrayList<Object> objects = new ArrayList<>();
    private final ArrayList<Function> functions = new ArrayList<>();
    private final ArrayList<ObjectField> fields = new ArrayList<>();
    private final ArrayList<ConstantPoolEntry> constantPool = new ArrayList<>();

    public short getFunctionIndex(Function func) {
        for(short i = 0; i<constantPool.size(); i++) {
            if(constantPool.get(i) instanceof FunctionEntry) {
                FunctionEntry e = (FunctionEntry) constantPool.get(i);
                if(e.getName().equals(func.name) && e.getSignature().validateFunctionSignature(func.signature)) {
                    return i;
                }
            }
        }
        constantPool.add(new FunctionEntry(namespace, func));
        return (short) (constantPool.size() - 1);
    }

    public ArrayList<ConstantPoolEntry> getConstantPool() {
        return new ArrayList<>(constantPool);
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

    public Function getFunction(String name, FunctionSignature sig) {
        for(Function func : functions) {
            if(func.name.equals(name)) {
                if(sig.returnType == null || func.signature.returnType == sig.returnType) {
                    if(func.signature.parameters.length == sig.parameters.length) {
                        boolean success = true;
                        for(int i = 0; i<func.signature.parameters.length; i++) {
                            if(func.signature.parameters[i] != sig.parameters[i]) {
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

    public void addStructure(Structure structure) {
        structures.add(structure);
    }

    public void addObject(Object object) {
        objects.add(object);
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public Type resolveType(String type) throws UnknownTypeException {
        Type t = Language.TYPES.get(type);
        if(t != null) return t;
        throw new NotImplementedException("Not implemented yet");
    }

    public static class LinkedData {
        public final Collection<CompiledData> data;

        public LinkedData(Collection<CompiledData> data) {
            this.data = data;
        }

        public static LinkedData load(File file) throws IOException {
            DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            if(input.readInt() != 0x7a50524c) throw new RuntimeException("Invalid file");
            int d = input.readInt();

            ArrayList<CompiledData> da = new ArrayList<>();
            for(int a = 0; a<d; a++) {
                String namespace = input.readUTF();

                CompiledData data = new CompiledData(namespace);
                int cpSize = input.readUnsignedShort();
                for(int i = 0; i < cpSize; i++) {
                    data.constantPool.add(ConstantPoolEntry.read(input));
                }

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
                da.add(data);
            }
            return new LinkedData(da);
        }

        public void save(File file) throws IOException {
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            output.writeBytes("zPRL");

            output.writeInt(data.size());
            for(CompiledData d : data) {
                output.writeUTF(d.namespace);

                output.writeShort(d.constantPool.size());
                for(ConstantPoolEntry e : d.constantPool) e.write(output);

                output.writeShort(d.structures.size());
                for(Structure struct : d.structures) {
                    output.writeUTF(struct.name);
                    output.writeShort(struct.fields.size());
                    for(StructureField field : struct.fields) {
                        output.writeUTF(field.name);
                        writeType(field.type, output);
                    }
                }

                output.writeShort(d.objects.size());
                for(Object obj : d.objects) {
                    output.writeUTF(obj.name);
                    writeType(obj.ext, output);
                    output.writeShort(obj.fields.size());
                    for(ObjectField field : obj.fields) {
                        output.writeUTF(field.name);
                        writeType(field.type, output);
                        writeMask(field.flags, output);
                    }
                    output.writeShort(obj.functions.size());
                    for(Function func : obj.functions) {
                        writeFunction(func, output);
                    }
                }

                output.writeShort(d.functions.size());
                for(Function func : d.functions) {
                    writeFunction(func, output);
                }

                output.writeShort(d.fields.size());
                for(ObjectField field : d.fields) {
                    output.writeUTF(field.name);
                    writeType(field.type, output);
                    writeMask(field.flags, output);
                }
            }
            output.close();
        }
    }

    private static Function readFunction(DataInputStream in) throws IOException {
        String name = in.readUTF();
        ArrayList<Flag> flags = Flag.fromBits(in.readInt());
        FunctionSignature sig = readFunctionSignature(in);
        Bytecode bytecode = null;
        if(!flags.contains(Flag.NO_IMPLEMENTATION)) {
            bytecode = new Bytecode();
            bytecode.load(in);
        }
        return new Function(name, sig, flags, bytecode);
    }

    public static FunctionSignature readFunctionSignature(DataInputStream in) throws IOException {
        Type ret = readType(in);
        short paramsLength = in.readShort();
        Type[] params = new Type[paramsLength];
        for(int i = 0; i<params.length; i++) {
            params[i] = readType(in);
        }
        return new FunctionSignature(ret, params);
    }

    private static Type readType(DataInputStream in) throws IOException {
        throw new NotImplementedException("Not implemented yet");
    }

    private static void writeMask(ArrayList<Flag> flags, DataOutputStream out) throws IOException {
        int flagsOut = 0;
        for(int i = 0; i<flags.size(); i++) flagsOut |= flags.get(i).mask;
        out.writeInt(flagsOut);
    }

    private static void writeFunction(Function func, DataOutputStream out) throws IOException {
        out.writeUTF(func.name);
        writeMask(func.flags, out);
        writeFunctionSignatureType(func.signature, out);
        if(!func.flags.contains(Flag.NO_IMPLEMENTATION)) {
            func.code.write(out);
        }
    }

    public static void writeFunctionSignatureType(FunctionSignature sig, DataOutputStream out) throws IOException {
        writeType(sig.returnType, out);
        out.writeShort(sig.parameters.length);
        for(Type param : sig.parameters) {
            writeType(param, out);
        }
    }

    private static void writeType(Type type, DataOutputStream out) throws IOException {
        throw new NotImplementedException("Not implemented yet");
    }

    public static LinkedData link(Collection<CompiledData> data) {
        return new LinkedData(data);
    }

}
