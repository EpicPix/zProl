package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;

public class Function {

    public String name;
    public FunctionSignature signature;
    public IBytecodeStorage code;

    public Function(String name, FunctionSignature signature, IBytecodeStorage code) {
        this.name = name;
        this.signature = signature;
        this.code = code;
    }

}
