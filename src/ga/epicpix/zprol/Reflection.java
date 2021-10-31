package ga.epicpix.zprol;

public class Reflection {

    public static <T> T createInstance(Class<T> t, Object... args) {
        try {
            return (T) t.getConstructors()[0].newInstance(args);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

}
