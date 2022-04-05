package ga.epicpix.zprol.generators;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.FunctionSignature;
import ga.epicpix.zprol.compiled.GeneratedData;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.exceptions.UndefinedOperationException;
import ga.epicpix.zprol.zld.Language;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

public final class GeneratorAssemblyLinux64 extends Generator {

    public String getGeneratorCommandLine() {
        return "asmlinux64";
    }

    public String getGeneratorName() {
        return "Assembly x86-64 Linux";
    }

    public String getFileExtension() {
        return ".asm";
    }

    private static final HashMap<String, InstructionGenerator> instructionGenerators = new HashMap<>();

    static {

    }

    private interface InstructionGenerator {
        public String generateInstruction(IBytecodeInstruction i, SeekIterator<IBytecodeInstruction> s);
    }

    public void generate(DataOutputStream outStream, GeneratedData generated) throws IOException {
        outStream.writeBytes("global _entry\n");
        outStream.writeBytes("_entry:\n");
        outStream.writeBytes("  call " + getMangledName(null, "main", new FunctionSignature(Language.getTypeFromDescriptor("V"))) + "\n");
        outStream.writeBytes("  mov rax, 60\n");
        outStream.writeBytes("  mov rdi, 0\n");
        outStream.writeBytes("  syscall\n");
        for(var function : generated.functions) {
            String functionName = getMangledName(function.namespace(), function.name(), function.signature());
            outStream.writeBytes(functionName + ":\n");
            SeekIterator<IBytecodeInstruction> instructions = new SeekIterator<>(function.code().getInstructions());
            while(instructions.hasNext()) {
                var instruction = instructions.next();
                var generator = instructionGenerators.get(instruction.getName());
                if(generator != null) {
                    outStream.writeBytes(generator.generateInstruction(instruction, instructions));
                }else {
                    throw new UndefinedOperationException("Unable to generate instructions for the " + instruction.getName() + " instruction");
                }
            }
        }
    }

    public static String getMangledName(String namespace, String name, FunctionSignature signature) {
        return (namespace != null ? namespace.replace('.', '$') + "$" : "") + name + "$" + signature.toString().replace("(", "").replace(")", "");
    }

}
