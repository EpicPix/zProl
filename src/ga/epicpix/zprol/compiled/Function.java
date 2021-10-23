package ga.epicpix.zprol.compiled;

import java.util.ArrayList;

public class Function {

    public String name;
    public TypeFunctionSignatureNamed signature;
    public ArrayList<Flag> flags;
    public Bytecode code;

    public Function(String name, TypeFunctionSignatureNamed signature, ArrayList<Flag> flags, Bytecode code) {
        this.name = name;
        this.signature = signature;
        this.flags = flags;
        this.code = code;
    }

}
