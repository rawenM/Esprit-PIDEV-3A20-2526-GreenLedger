package Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to load environment variables from .env file.
 * Supports both classpath resource and file system .env files.
 */
public class DotEnvLoader {

    private static final String ENV_FILE_NAME = ".env";
    private static boolean loaded = false;

    /**
     * Load environment variables from .env file.
     * Priority order:
     * 1. .env file in working directory
     * 2. .env file in project root
     */
    public static synchronized void load() {
        if (loaded) return;

        try {
            // Try to load from working directory first
            File workingDirEnv = new File(ENV_FILE_NAME);
            if (workingDirEnv.exists() && workingDirEnv.isFile()) {
                loadFromFile(workingDirEnv);
                System.out.println("[DotEnvLoader] Loaded .env from working directory: " + workingDirEnv.getAbsolutePath());
                loaded = true;
                return;
            }

            // Try to load from project root (parent of current directory)
            File projectRootEnv = new File("../" + ENV_FILE_NAME);
            if (projectRootEnv.exists() && projectRootEnv.isFile()) {
                loadFromFile(projectRootEnv);
                System.out.println("[DotEnvLoader] Loaded .env from project root: " + projectRootEnv.getAbsolutePath());
                loaded = true;
                return;
            }

            System.out.println("[DotEnvLoader] No .env file found in working directory or project root");
            loaded = true;

        } catch (Exception e) {
            System.err.println("[DotEnvLoader] Error loading .env file: " + e.getMessage());
            e.printStackTrace();
            loaded = true;
        }
    }

    /**
     * Load key-value pairs from a .env file
     */
    private static void loadFromFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse key=value
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    // Remove quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    // Set as system property and environment variable
                    System.setProperty(key, value);

                    // Also set environment variable (simulated via System.setProperty)
                    // This allows ApiConfig to read from System.getenv() fallback
                    System.out.println("[DotEnvLoader] Loaded: " + key);
                }
            }
        }
    }

    /**
     * Get an environment variable (from loaded .env or system environment)
     */
    public static String getEnv(String key) {
        return getEnv(key, null);
    }

    /**
     * Get an environment variable with a default value
     */
    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return value != null ? value : defaultValue;
    }
}

