package ga.epicpix.zprol.compiled;

public class TypePointer extends Type {

    public Type holding;

    public TypePointer(Type holding) {
        super(Types.POINTER);
        if(holding == null) {
            this.holding = new Type(Types.VOID);
            return;
        }
        this.holding = holding;
    }

    public String toString() {
        return "pointer<" + holding + ">";
    }
}
