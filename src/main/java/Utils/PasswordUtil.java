package Utils;


import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Hasher un mot de passe avec BCrypt
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Vérifier si un mot de passe correspond au hash
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valider la force d'un mot de passe
     * Critères:
     * - Au moins 8 caractères
     * - Au moins une majuscule
     * - Au moins une minuscule
     * - Au moins un chiffre
     * - Au moins un caractère spécial
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Obtenir un message d'erreur si le mot de passe n'est pas assez fort
     */
    public static String getPasswordErrorMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Le mot de passe est obligatoire";
        }

        if (password.length() < 8) {
            return "Le mot de passe doit contenir au moins 8 caractères";
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        if (!hasUpper) return "Le mot de passe doit contenir au moins une majuscule";
        if (!hasLower) return "Le mot de passe doit contenir au moins une minuscule";
        if (!hasDigit) return "Le mot de passe doit contenir au moins un chiffre";
        if (!hasSpecial) return "Le mot de passe doit contenir au moins un caractère spécial (@, #, $, etc.)";

        return null; // Mot de passe valide
    }
}
