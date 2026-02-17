package Utils;

/**
 * Singleton class to track navigation context in the application.
 * Keeps track of the current and previous page to enable dynamic back navigation.
 */
public class NavigationContext {
    private static NavigationContext instance;
    private String currentPage;
    private String previousPage;

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

    /**
     * Navigate to a new page, updating current and previous page tracking.
     * @param newPage The page to navigate to
     */
    public void navigateTo(String newPage) {
        // Only update previous page if we're not navigating back to the same page
        if (!newPage.equals(currentPage)) {
            previousPage = currentPage;
            currentPage = newPage;
        }
    }

    /**
     * Get the current page name.
     * @return Current page FXML name (without .fxml extension)
     */
    public String getCurrentPage() {
        return currentPage;
    }

    /**
     * Get the previous page name for back navigation.
     * @return Previous page FXML name (without .fxml extension)
     */
    public String getPreviousPage() {
        return previousPage;
    }

    /**
     * Set the current page directly (useful for initialization).
     * @param page The page to set as current
     */
    public void setCurrentPage(String page) {
        currentPage = page;
    }
}
