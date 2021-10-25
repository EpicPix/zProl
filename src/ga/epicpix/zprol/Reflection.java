package ga.epicpix.zprol;

public class Reflection {

    public static <T> T createInstance(Class<T> t, Class<?>[] classArguments, Object... args) {
        try {
            return t.getConstructor(classArguments).newInstance(args);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

}
