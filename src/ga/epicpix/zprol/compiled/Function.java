package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import java.util.ArrayList;

public class Function {

    public String name;
    public FunctionSignature signature;
    public ArrayList<Flag> flags;
    public Bytecode code;

    public Function(String name, FunctionSignature signature, ArrayList<Flag> flags, Bytecode code) {
        this.name = name;
        this.signature = signature;
        this.flags = flags;
        this.code = code;
    }

}
