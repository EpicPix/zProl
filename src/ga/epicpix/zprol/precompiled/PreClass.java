package ga.epicpix.zprol.precompiled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreClass {

    public PreClass() {}
    public PreClass(String name, PreField... fields) {
        this.name = name;
        this.fields = Arrays.asList(fields);
    }

    public String name;

    public List<PreField> fields = new ArrayList<>();

}
