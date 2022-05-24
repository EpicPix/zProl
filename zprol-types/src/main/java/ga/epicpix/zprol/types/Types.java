package ga.epicpix.zprol.types;

import java.util.HashMap;

public class Types {

    private static final HashMap<String, Type> TYPES = new HashMap<>();

    public static void registerType(String name, Type type) {
        TYPES.put(name, type);
    }

    public static Type getType(String name) {
        return TYPES.get(name);
    }

    public static Type getTypeFromDescriptor(String descriptor) {
        if(descriptor.startsWith("C")) {
            if(!descriptor.endsWith(";")) return null;
            String data = descriptor.substring(1, descriptor.length() - 1);

            String namespace = descriptor.lastIndexOf('.') == -1 ? null : data.substring(0, data.lastIndexOf("."));
            String name = descriptor.lastIndexOf('.') == -1 ? data : data.substring(data.lastIndexOf('.') + 1);

            return new ClassType(namespace, name);
        }
        if(descriptor.equals("b")) {
            return new BooleanType();
        }
        for(var type : TYPES.values()) {
            if(type.getDescriptor().equals(descriptor)) {
                return type;
            }
        }
        return null;
    }

}
