package Utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility class to load environment variables from .env file
 * This ensures that .env variables are available via System.getenv()
 */
public class EnvLoader {
    
    private static boolean loaded = false;
    private static final Map<String, String> envVars = new HashMap<>();
    
    /**
     * Load .env file from project root
     */
    public static void load() {
        if (loaded) {
            return;
        }
        
        try {
            // Try multiple locations
            Path[] candidates = new Path[] {
                Paths.get(System.getProperty("user.dir"), ".env"),
                Paths.get(".env")
            };
            
            Path envFile = null;
            for (Path candidate : candidates) {
                if (Files.exists(candidate)) {
                    envFile = candidate;
                    break;
                }
            }
            
            if (envFile == null) {
                // Try classpath
                InputStream is = EnvLoader.class.getResourceAsStream("/.env");
                if (is != null) {
                    loadFromStream(is);
                    loaded = true;
                    System.out.println("[EnvLoader] Loaded .env from classpath");
                    return;
                }
                System.out.println("[EnvLoader] No .env file found");
                loaded = true;
                return;
            }
            
            // Load from file
            try (BufferedReader reader = Files.newBufferedReader(envFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    
                    // Parse KEY=VALUE
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = line.substring(0, equalsIndex).trim();
                        String value = line.substring(equalsIndex + 1).trim();
                        
                        // Remove quotes if present
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        
                        envVars.put(key, value);
                        
                        // Set as system property so it can be accessed
                        System.setProperty(key, value);
                    }
                }
            }
            
            loaded = true;
            System.out.println("[EnvLoader] Loaded " + envVars.size() + " variables from .env");
            
        } catch (Exception e) {
            System.err.println("[EnvLoader] Error loading .env: " + e.getMessage());
            loaded = true;
        }
    }
    
    private static void loadFromStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    envVars.put(key, value);
                    System.setProperty(key, value);
                }
            }
        }
    }
    
    /**
     * Get environment variable (checks .env first, then system env)
     */
    public static String get(String key) {
        return get(key, null);
    }
    
    /**
     * Get environment variable with default value
     */
    public static String get(String key, String defaultValue) {
        if (!loaded) {
            load();
        }
        
        // Check .env vars first
        String value = envVars.get(key);
        if (value != null) {
            return value;
        }
        
        // Check system properties
        value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // Check system environment
        value = System.getenv(key);
        if (value != null) {
            return value;
        }
        
        return defaultValue;
    }
}
