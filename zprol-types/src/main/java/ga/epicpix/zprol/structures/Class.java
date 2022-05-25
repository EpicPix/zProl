package ga.epicpix.zprol.structures;

public record Class(String namespace, String name, ClassField[] fields, Method[] methods) {
    public String toString() {
        return "Class[\"" + (namespace != null ? namespace : "") + "\" \"" + name + "\"]";
    }
}
