package utils;

public class SyncOutput {
    private static final Object lock = new Object();

    public static void println(String message) {
        synchronized (lock) {
            System.out.println(message);
        }
    }
}
