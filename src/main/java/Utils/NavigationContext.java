package Utils;

/**
 * Singleton class to track navigation context in the application.
 * Keeps track of the current and previous page to enable dynamic back navigation.
 */
public class NavigationContext {
    private static NavigationContext instance;
    private String currentPage;
    private String previousPage;

    // Context data passed between screens
    private Integer currentProjectId;
    private Integer currentUserId;
    private Integer currentEvaluationId;

    private NavigationContext() {
        currentPage = "fxml/dashboard";
        previousPage = "fxml/dashboard";
    }

    public static NavigationContext getInstance() {
        if (instance == null) {
            instance = new NavigationContext();
        }
        return instance;
    }

    public void navigateTo(String newPage) {
        if (!newPage.equals(currentPage)) {
            previousPage = currentPage;
            currentPage = newPage;
        }
    }

    public String getCurrentPage()   { return currentPage; }
    public String getPreviousPage()  { return previousPage; }
    public void   setCurrentPage(String page) { currentPage = page; }

    // ── Context data helpers ──────────────────────────────────────────────

    public Integer getCurrentProjectId()              { return currentProjectId; }
    public void    setCurrentProjectId(Integer id)    { this.currentProjectId = id; }

    public Integer getCurrentUserId()                 { return currentUserId; }
    public void    setCurrentUserId(Integer id)       { this.currentUserId = id; }

    public Integer getCurrentEvaluationId()           { return currentEvaluationId; }
    public void    setCurrentEvaluationId(Integer id) { this.currentEvaluationId = id; }
}
