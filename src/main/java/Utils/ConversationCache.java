package Utils;

public class ConversationCache {

    private static final StringBuilder memory = new StringBuilder();

    public static void append(String text) {
        memory.append(text).append("\n");
    }

    public static String get() {
        return memory.toString();
    }

    public static void clear() {
        memory.setLength(0);
    }

    public static boolean isEmpty() {
        return memory.length() == 0;
    }
}