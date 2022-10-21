package ga.epicpix.zprol.structures;

import ga.epicpix.zprol.types.Type;
import ga.epicpix.zprol.types.Types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class FunctionSignature {
    public final Type returnType;
    public final Type[] parameters;

    public FunctionSignature(Type returnType, Type... parameters) {
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public boolean validateFunctionSignature(FunctionSignature cmp) {
        if(cmp.returnType == null || returnType == cmp.returnType) {
            if(cmp.parameters.length == parameters.length) {
                boolean success = true;
                for(int i = 0; i < cmp.parameters.length; i++) {
                    if(cmp.parameters[i] != parameters[i]) {
                        success = false;
                        break;
                    }
                }
                return success;
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(returnType.getDescriptor());
        output.append("(");
        for(Type parameter : parameters) {
            output.append(parameter.getDescriptor());
        }
        output.append(")");
        return output.toString();
    }

    public static FunctionSignature fromDescriptor(String descriptor) {
        String returnTypeDescriptor = descriptor.substring(0, descriptor.indexOf("("));
        String paramsDescriptor = descriptor.substring(descriptor.indexOf("(") + 1, descriptor.lastIndexOf(")"));
        Type returnType = Types.getTypeFromDescriptor(returnTypeDescriptor);
        ArrayList<Type> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        while(!paramsDescriptor.isEmpty()) {
            current.append(paramsDescriptor.charAt(0));
            Type type = Types.getTypeFromDescriptor(current.toString());
            if(type != null) {
                params.add(type);
                current = new StringBuilder();
            }
            paramsDescriptor = paramsDescriptor.substring(1);
        }
        return new FunctionSignature(returnType, params.toArray(new Type[0]));
    }

    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        FunctionSignature that = (FunctionSignature) o;
        return returnType.equals(that.returnType) && Arrays.equals(parameters, that.parameters);
    }

    public int hashCode() {
        int result = Objects.hash(returnType);
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }
}
