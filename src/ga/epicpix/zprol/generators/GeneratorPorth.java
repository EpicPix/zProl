package ga.epicpix.zprol.generators;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.compiled.TypeFunctionSignatureNamed;
import ga.epicpix.zprol.compiled.TypeNamed;
import ga.epicpix.zprol.compiled.Types;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstruction;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstructions;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class GeneratorPorth {

    public static void generate_porth(CompiledData data, File save) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(save));

        writer.write("proc *rot4 int int int int -- int int int int in memory tmp0 8 end tmp0 !64 memory tmp1 8 end tmp1 !64 memory tmp2 8 end tmp2 !64 memory tmp3 8 end tmp3 !64 tmp0 @64 tmp1 @64 tmp2 @64 tmp3 @64 end\n");
        writer.write("proc *rot5 int int int int int -- int int int int int in memory tmp0 8 end tmp0 !64 memory tmp1 8 end tmp1 !64 memory tmp2 8 end tmp2 !64 memory tmp3 8 end tmp3 !64 memory tmp4 8 end tmp4 !64 tmp0 @64 tmp1 @64 tmp2 @64 tmp3 @64 tmp4 @64 end\n");
        writer.write("proc *rot6 int int int int int int -- int int int int int int in memory tmp0 8 end tmp0 !64 memory tmp1 8 end tmp1 !64 memory tmp2 8 end tmp2 !64 memory tmp3 8 end tmp3 !64 memory tmp4 8 end tmp4 !64 memory tmp5 8 end tmp5 !64 tmp0 @64 tmp1 @64 tmp2 @64 tmp3 @64 tmp4 @64 tmp5 @64 end\n");
        writer.write("proc *rot7 int int int int int int int -- int int int int int int int in memory tmp0 8 end tmp0 !64 memory tmp1 8 end tmp1 !64 memory tmp2 8 end tmp2 !64 memory tmp3 8 end tmp3 !64 memory tmp4 8 end tmp4 !64 memory tmp5 8 end tmp5 !64 memory tmp6 8 end tmp6 !64 tmp0 @64 tmp1 @64 tmp2 @64 tmp3 @64 tmp4 @64 tmp5 @64 tmp6 @64 end\n\n");

        for(Function func : data.getFunctions()) {
            TypeFunctionSignatureNamed sig = func.signature;

            writer.write("proc " + func.name + " ");
            for(TypeNamed param : sig.parameters) writer.write(typeToPorthName(param.type) + " ");
            String returnPorth = typeToPorthName(sig.returnType);
            if(!returnPorth.isEmpty()) {
                writer.write("-- " + returnPorth + " ");
            }
            writer.write("in\n");

            for(int i = sig.parameters.length - 1; i>=0; i--) {
                writer.write("    memory " + sig.parameters[i].name + " " + sig.parameters[i].type.type.memorySize + " end " + sig.parameters[i].name + " !" + (sig.parameters[i].type.type.memorySize * 8) + "\n");
            }

            for(BytecodeInstruction instr : func.code.getInstructions()) {
                if(instr.instruction == BytecodeInstructions.PUSHI64F8) {
                    writer.write("" + ((byte)instr.data[0] & 0xff));
                }else if(instr.instruction == BytecodeInstructions.PUSHSTR) {
                    int strindex = (short) instr.data[0];
                    writer.write("\"" + func.code.getStrings().get(strindex).replace("\\", "\\\\").replace("\n", "\\n") + "\"c cast(int)");
                }else if(instr.instruction == BytecodeInstructions.SYSCALL1) {
                    writer.write("syscall0");
                }else if(instr.instruction == BytecodeInstructions.SYSCALL2) {
                    writer.write("swap syscall1");
                }else if(instr.instruction == BytecodeInstructions.SYSCALL3) {
                    writer.write("rot rot syscall2");
                }else if(instr.instruction == BytecodeInstructions.SYSCALL4) {
                    writer.write("*rot4 syscall3");
                }else if(instr.instruction == BytecodeInstructions.SYSCALL5) {
                    writer.write("*rot5 syscall3");
                }else if(instr.instruction == BytecodeInstructions.SYSCALL6) {
                    writer.write("*rot6 syscall3");
                }else if(instr.instruction == BytecodeInstructions.SYSCALL7) {
                    writer.write("*rot7 syscall3");
                }else if(instr.instruction == BytecodeInstructions.POP64) {
                    writer.write("drop");
                }else {
                    System.err.println("Not implemented instruction: " + instr.instruction);
                }
                writer.write(" // " + instr.instruction.name().toLowerCase() + " " + Arrays.toString(instr.data) + "\n");
            }

            writer.write("end\n");
        }

        writer.write("_start");

        writer.close();
    }

    public static String typeToPorthName(Type type) {
        if(type.type == Types.POINTER) {
            return "ptr";
        }else if(type.isNumberType()) {
            return "int";
        }else if(type.type.memorySize == 0) {
            return "";
        }
        System.err.println("Unknown type: " + type);
        return type.type.name();
    }

}
