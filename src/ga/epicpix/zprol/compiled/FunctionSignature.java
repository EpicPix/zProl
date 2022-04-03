package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.zld.Language;

import java.util.ArrayList;

public record FunctionSignature(PrimitiveType returnType, PrimitiveType... parameters) {

    public boolean validateFunctionSignature(FunctionSignature cmp) {
        if (cmp.returnType == null || returnType == cmp.returnType) {
            if (cmp.parameters.length == parameters.length) {
                boolean success = true;
                for (int i = 0; i < cmp.parameters.length; i++) {
                    if (cmp.parameters[i] != parameters[i]) {
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
        output.append(returnType.descriptor);
        output.append("(");
        for(PrimitiveType parameter : parameters) {
            output.append(parameter.descriptor);
        }
        output.append(")");
        return output.toString();
    }

    public static FunctionSignature fromDescriptor(String descriptor) {
        String returnTypeDescriptor = descriptor.substring(0, descriptor.indexOf("("));
        String paramsDescriptor = descriptor.substring(descriptor.indexOf("(") + 1, descriptor.lastIndexOf(")"));
        PrimitiveType returnType = Language.getTypeFromDescriptor(returnTypeDescriptor);
        ArrayList<PrimitiveType> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        while(!paramsDescriptor.isEmpty()) {
            current.append(paramsDescriptor.charAt(0));
            PrimitiveType type = Language.getTypeFromDescriptor(current.toString());
            if(type != null) {
                params.add(type);
                current = new StringBuilder();
            }
            paramsDescriptor = paramsDescriptor.substring(1);
        }
        return new FunctionSignature(returnType, params.toArray(new PrimitiveType[0]));
    }

}
