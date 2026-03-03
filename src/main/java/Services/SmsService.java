package Services;

import Utils.ApiConfig;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsService {

    private static boolean initialized = false;

    private static void initIfNeeded() {
        if (initialized) return;

        if (!ApiConfig.isTwilioEnabled()) {
            System.out.println("[SMS] Twilio désactivé (api.twilio.enabled=false)");
            initialized = true;
            return;
        }

        String sid = ApiConfig.getTwilioAccountSid();
        String token = ApiConfig.getTwilioAuthToken();
        String from = ApiConfig.getTwilioFromNumber();

        if (sid.isEmpty() || token.isEmpty() || from.isEmpty()) {
            System.out.println("[SMS] Twilio activé mais SID/TOKEN/FROM manquants. Vérifie api-config.properties");
            initialized = true;
            return;
        }

        Twilio.init(sid, token);
        initialized = true;
        System.out.println("[SMS] Twilio initialisé");
    }

    /**
     * Envoi SMS lors de la soumission du projet.
     */
    public static void sendProjectSubmittedSms(String rawPhone, int projetId) {
        System.out.println("[SMS] sendProjectSubmittedSms called");
        System.out.println("[SMS] enabled=" + ApiConfig.isTwilioEnabled());
        System.out.println("[SMS] rawPhone=" + rawPhone);
        System.out.println("[SMS] SID empty? " + ApiConfig.getTwilioAccountSid().isEmpty());
        System.out.println("[SMS] TOKEN empty? " + ApiConfig.getTwilioAuthToken().isEmpty());
        System.out.println("[SMS] FROM=" + ApiConfig.getTwilioFromNumber());
        initIfNeeded();
        if (!ApiConfig.isTwilioEnabled()) return;

        String to = normalizePhone(rawPhone);
        System.out.println("[SMS] normalized TO=" + to);
        if (to == null) {
            System.out.println("[SMS] Numéro invalide: " + rawPhone);
            return;
        }

        String body = "Félicitations ! Votre projet #" + projetId
                + " est sur la plateforme GreenLedger et en attente d'être vu par les investisseurs.";

        try {
            Message msg = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(ApiConfig.getTwilioFromNumber()),
                    body
            ).create();

            System.out.println("[SMS] Envoyé. SID=" + msg.getSid());

        } catch (Exception e) {
            System.out.println("[SMS] normalized TO=" + to);
            System.out.println("[SMS] Erreur envoi: " + e.getMessage());
        }
    }

    /**
     * Normalisation MVP numéro Tunisie:
     * - "22900036" => "+21622900036"
     * - "21622900036" => "+21622900036"
     * - "+21622900036" => "+21622900036"
     */
    private static String normalizePhone(String raw) {
        if (raw == null) return null;

        String s = raw.trim()
                .replace(" ", "")
                .replace("-", "");

        if (s.isEmpty()) return null;

        if (s.startsWith("+")) return s;

        if (s.matches("\\d{8}")) return "+216" + s;

        if (s.matches("216\\d{8}")) return "+" + s;

        return null;
    }
}