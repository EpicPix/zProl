package ga.epicpix.zprol.compiled;

import java.util.ArrayList;

public class Function {

    public String name;
    public TypeFunctionSignatureNamed signature;
    public ArrayList<Flag> flags;

    public Function(String name, TypeFunctionSignatureNamed signature, ArrayList<Flag> flags) {
        this.name = name;
        this.signature = signature;
        this.flags = flags;
    }

}
