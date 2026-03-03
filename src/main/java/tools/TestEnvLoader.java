package tools; 
import Utils.EnvLoader; 
 
public class TestEnvLoader { 
    public static void main(String[] args) { 
        System.out.println("=== Test EnvLoader ==="); 
        System.out.println(); 
        EnvLoader.load(); 
        System.out.println(); 
        System.out.println("Variables chargees:"); 
        System.out.println("  GMAIL_API_ENABLED = " + EnvLoader.get("GMAIL_API_ENABLED")); 
        System.out.println("  GMAIL_FROM_EMAIL = " + EnvLoader.get("GMAIL_FROM_EMAIL")); 
        System.out.println("  GMAIL_FROM_NAME = " + EnvLoader.get("GMAIL_FROM_NAME")); 
        System.out.println(); 
        System.out.println("OK Test termine !"); 
    } 
} 
