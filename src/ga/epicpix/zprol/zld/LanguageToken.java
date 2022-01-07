package ga.epicpix.zprol.zld;

import java.util.Stack;

public record LanguageToken(String contextRequirement, String name, String... args) {

    public static boolean checkContextRequirement(String contextRequirement, Stack<String> contexts) {
        if(contextRequirement.equals("*")) return true;
        if(contexts.size() == 0 && contextRequirement.startsWith("!")) return true;
        if(contexts.size() != 0 && contextRequirement.startsWith("!")) return !contexts.peek().equals(contextRequirement);
        if(contexts.size() != 0) return contexts.peek().equals(contextRequirement);
        return false;
    }

}
