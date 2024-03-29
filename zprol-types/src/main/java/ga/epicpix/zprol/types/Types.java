package ga.epicpix.zprol.types;

import java.util.HashMap;

public class Types {

    private static final HashMap<String, Type> TYPES = new HashMap<>();

    static {
        Types.registerType(new VoidType(), "void");
        Types.registerPrimitiveType(1, false, "B", "int8", "byte");
        Types.registerPrimitiveType(2, false, "S", "int16", "short");
        Types.registerPrimitiveType(4, false, "I", "int32", "int");
        Types.registerPrimitiveType(8, false, "L", "int64", "long");
        Types.registerPrimitiveType(1, true, "uB", "uint8", "ubyte");
        Types.registerPrimitiveType(2, true, "uS", "uint16", "ushort");
        Types.registerPrimitiveType(4, true, "uI", "uint32", "uint");
        Types.registerPrimitiveType(8, true, "uL", "uint64", "ulong");
        Types.registerType(new BooleanType(), "bool");
    }

    public static void registerPrimitiveType(int size, boolean unsigned, String descriptor, String... names) {
        for(String name : names) {
            TYPES.put(name, new PrimitiveType(size, unsigned, descriptor, name));
        }
    }

    public static void registerType(Type type, String name) {
        TYPES.put(name, type);
    }

    public static Type getType(String name) {
        return TYPES.get(name);
    }

    private static Type putInArrayType(Type type, int times) {
        for(int i = 0; i<times; i++) {
            type = new ArrayType(type);
        }
        return type;
    }

    public static Type getTypeFromDescriptor(String descriptor) {
        int arrTimes = 0;
        while(descriptor.startsWith("[")) {
            descriptor = descriptor.substring(1);
            arrTimes++;
        }
        if(descriptor.startsWith("C")) {
            if(!descriptor.endsWith(";")) return null;
            String data = descriptor.substring(1, descriptor.length() - 1);

            String namespace = descriptor.lastIndexOf('.') == -1 ? null : data.substring(0, data.lastIndexOf("."));
            String name = descriptor.lastIndexOf('.') == -1 ? data : data.substring(data.lastIndexOf('.') + 1);

            return putInArrayType(new ClassType(namespace, name), arrTimes);
        }
        if(descriptor.equals("b")) {
            return putInArrayType(new BooleanType(), arrTimes);
        }
        for(Type type : TYPES.values()) {
            if(type.getDescriptor().equals(descriptor)) {
                return putInArrayType(type, arrTimes);
            }
        }
        return null;
    }

}
