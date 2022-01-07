package ga.epicpix.zprol.zld;

public record LanguageContextEvent(String on, ContextManipulationOperation manipulation, String context) {
    public enum ContextManipulationOperation {
        PUSH, POP
    }
}
