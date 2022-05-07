package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.compiled.generated.ConstantPool;
import ga.epicpix.zprol.compiled.generated.IConstantPoolPreparable;

public record ClassField(String name, Type type) implements IConstantPoolPreparable {

    public void prepareConstantPool(ConstantPool pool) {
        pool.getOrCreateStringIndex(name);
        pool.getOrCreateStringIndex(type.getDescriptor());
    }
}
