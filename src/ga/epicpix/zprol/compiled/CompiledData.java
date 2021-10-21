package ga.epicpix.zprol.compiled;

import java.util.ArrayList;

public class CompiledData {

    private final ArrayList<Structure> structures = new ArrayList<>();

    public Type resolveType(String type) {
        throw new UnsupportedOperationException("Cannot resolve types yet: " + type);
    }

    public void addStructure(Structure structure) {
        structures.add(structure);
    }

}
