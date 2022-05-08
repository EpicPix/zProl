package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.compiled.generated.ConstantPool;
import ga.epicpix.zprol.compiled.generated.IConstantPoolPreparable;

public record Class(String namespace, String name, ClassField[] fields) implements IConstantPoolPreparable {

    public void prepareConstantPool(ConstantPool pool) {
        pool.getOrCreateStringIndex(namespace);
        pool.getOrCreateStringIndex(name);
        for(ClassField field : fields) {
            field.prepareConstantPool(pool);
        }
    }

    public String toString() {
        return "Class[\"" + (namespace != null ? namespace : "") + "\" \"" + name + "\"]";
    }
}
