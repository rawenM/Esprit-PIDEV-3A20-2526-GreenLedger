package Services;

import Models.User;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service de validation des données utilisateur
 */
public class ValidationService {

    // Patterns de validation
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{8,15}$");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-ZÀ-ÿ\\s\\-']{2,100}$");

    /**
     * Valider toutes les données d'un utilisateur
     * @return Liste des erreurs (vide si aucune erreur)
     */
    public List<String> validateUser(User user) {
        List<String> errors = new ArrayList<>();

        // Validation nom
        if (user.getNom() == null || user.getNom().trim().isEmpty()) {
            errors.add("Le nom est obligatoire");
        } else if (!NAME_PATTERN.matcher(user.getNom()).matches()) {
            errors.add("Le nom doit contenir uniquement des lettres (2-100 caractères)");
        }

        // Validation prénom
        if (user.getPrenom() == null || user.getPrenom().trim().isEmpty()) {
            errors.add("Le prénom est obligatoire");
        } else if (!NAME_PATTERN.matcher(user.getPrenom()).matches()) {
            errors.add("Le prénom doit contenir uniquement des lettres (2-100 caractères)");
        }

        // Validation email
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            errors.add("L'email est obligatoire");
        } else if (!isEmailValid(user.getEmail())) {
            errors.add("Format d'email invalide");
        }

        // Validation téléphone (si fourni)
        if (user.getTelephone() != null && !user.getTelephone().trim().isEmpty()) {
            if (!isPhoneValid(user.getTelephone())) {
                errors.add("Format de téléphone invalide (8-15 chiffres)");
            }
        }

        // Validation date de naissance (doit avoir au moins 18 ans)
        if (user.getDateNaissance() != null) {
            int age = Period.between(user.getDateNaissance(), LocalDate.now()).getYears();
            if (age < 18) {
                errors.add("Vous devez avoir au moins 18 ans");
            }
            if (age > 120) {
                errors.add("Date de naissance invalide");
            }
        }

        return errors;
    }

    /**
     * Valider un email
     */
    public boolean isEmailValid(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Valider un numéro de téléphone
     */
    public boolean isPhoneValid(String phone) {
        if (phone == null) return false;
        String cleanPhone = phone.replaceAll("\\s", "").replaceAll("-", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Valider un nom ou prénom
     */
    public boolean isNameValid(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }
}
