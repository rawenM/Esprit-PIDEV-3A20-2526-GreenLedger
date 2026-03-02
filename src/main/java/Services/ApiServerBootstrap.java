package Services;

public class ApiServerBootstrap {

    private static volatile boolean started = false;

    public static void startIfNeeded() {
        if (started) return;
        synchronized (ApiServerBootstrap.class) {
            if (started) return;
            started = true;
            Thread t = new Thread(() -> {
                try {
                    int port = Integer.parseInt(System.getenv().getOrDefault("API_PORT", "8081"));
                    new Api.ApiServer().start(port);
                } catch (Exception ex) {
                    // Port already in use or startup failed; avoid crashing UI
                    System.err.println("[API] Startup skipped: " + ex.getMessage());
                }
            }, "api-server");
            t.setDaemon(true);
            t.start();
        }
    }

    private ApiServerBootstrap() {}
}
