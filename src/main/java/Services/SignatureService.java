package Services;

import DataBase.MyConnection;
import Models.Evaluation;
import Models.SignatureData;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles electronic signature storage and verification for evaluations.
 *
 * The signature is stored as a text block APPENDED to evaluation.observations_globales:
 *
 *   <visible comment>
 *
 *   [SIGNATURE]
 *   signedAt=2026-04-30 14:23:11
 *   signedByName=Mehdi Benzaied
 *   signatureHash=a3f9b2c1d4e5f6789abc...
 *   signatureImage=data:image/png;base64,iVBORw0KGgo...
 *
 * This format is shared with Symfony — both sides parse the same block.
 */
public class SignatureService {

    private static final String SIGNATURE_MARKER = "\n\n[SIGNATURE]";
    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Parse ─────────────────────────────────────────────────────────────

    /**
     * Parse the signature block from observations_globales.
     * Returns a SignatureData with isSigned=false if no [SIGNATURE] block exists.
     */
    public SignatureData extractSignature(String observationsGlobales) {
        if (observationsGlobales == null) {
            return new SignatureData("", false);
        }

        int markerIdx = observationsGlobales.indexOf(SIGNATURE_MARKER);
        if (markerIdx < 0) {
            return new SignatureData(observationsGlobales.trim(), false);
        }

        String visibleComment = observationsGlobales.substring(0, markerIdx).trim();
        String block = observationsGlobales.substring(markerIdx + SIGNATURE_MARKER.length());

        SignatureData data = new SignatureData(visibleComment, false);
        data.setSignedAt(extractField(block, "signedAt"));
        data.setSignedByName(extractField(block, "signedByName"));
        data.setSignatureHash(extractField(block, "signatureHash"));
        data.setSignatureImage(extractField(block, "signatureImage"));

        boolean signed = data.getSignedAt()      != null
                      && data.getSignedByName()  != null
                      && data.getSignatureHash() != null;
        data.setSigned(signed);
        return data;
    }

    private String extractField(String block, String key) {
        Pattern p = Pattern.compile("(?m)^" + Pattern.quote(key) + "=(.+)$");
        Matcher m = p.matcher(block);
        return m.find() ? m.group(1).trim() : null;
    }

    // ── Hash ──────────────────────────────────────────────────────────────

    /**
     * Build the SHA-256 hash for signature verification.
     * Format: evaluationId|projectId|scoreFinal|decision|signerName|timestamp
     */
    public String buildSignatureHash(long evaluationId, int projectId,
                                     String scoreFinal, String decision,
                                     String signerName, String timestamp) {
        String data = evaluationId + "|" + projectId + "|"
                    + safe(scoreFinal) + "|" + safe(decision) + "|"
                    + safe(signerName) + "|" + safe(timestamp);
        return sha256Hex(data);
    }

    // ── Sign ──────────────────────────────────────────────────────────────

    /**
     * Sign an evaluation and persist the signature block to the DB.
     *
     * @param evaluationId   id_evaluation PK
     * @param signerName     name of the signer (must not be blank)
     * @param signatureDataUrl base64 PNG data URL (must start with "data:image/")
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if evaluation not found
     */
    public void signEvaluation(long evaluationId, String signerName, String signatureDataUrl) {
        // Validate inputs
        if (signerName == null || signerName.isBlank()) {
            throw new IllegalArgumentException("Le nom du signataire est obligatoire.");
        }
        if (signatureDataUrl == null
                || (!signatureDataUrl.startsWith("data:image/png;base64,")
                    && !signatureDataUrl.startsWith("data:image/jpeg;base64,"))) {
            throw new IllegalArgumentException(
                "La signature doit être une image PNG ou JPEG encodée en base64.");
        }

        // Load evaluation
        Evaluation eval = loadEvaluation(evaluationId);
        if (eval == null) {
            throw new IllegalStateException("Évaluation introuvable: " + evaluationId);
        }

        // Extract visible comment (strip any previous signature)
        SignatureData existing = extractSignature(eval.getObservations());
        String baseComment = existing.getCommentaireGlobal();

        // Build timestamp and hash
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String scoreFinal = String.format(java.util.Locale.ROOT, "%.2f", eval.getScoreGlobal());
        String decision   = safe(eval.getDecision());

        // Hash includes a SHA-256 of the image data URL to bind the image to the hash
        String imageHash  = sha256Hex(signatureDataUrl);
        String hash = buildSignatureHash(
            evaluationId, eval.getIdProjet(),
            scoreFinal, decision,
            signerName + "|" + imageHash,
            timestamp
        );

        // Build signature block
        String cleanName  = signerName.replace("\n", "").replace("\r", "");
        String cleanImage = signatureDataUrl.replace("\n", "").replace("\r", "");

        String signatureBlock = SIGNATURE_MARKER + "\n"
            + "signedAt="       + timestamp  + "\n"
            + "signedByName="   + cleanName  + "\n"
            + "signatureHash="  + hash       + "\n"
            + "signatureImage=" + cleanImage;

        String newObservations = baseComment.trim() + signatureBlock;

        // Persist
        saveObservations(evaluationId, newObservations);
        System.out.printf("[Signature] Evaluation %d signed by '%s' at %s hash=%s%n",
            evaluationId, cleanName, timestamp, hash.substring(0, Math.min(16, hash.length())));
    }

    // ── Verify ────────────────────────────────────────────────────────────

    /**
     * Verify that the stored signature hash matches the evaluation data.
     * Returns true if the signature is valid and untampered.
     */
    public boolean verifySignature(long evaluationId, SignatureData signature) {
        if (!signature.isSigned()) return false;

        Evaluation eval = loadEvaluation(evaluationId);
        if (eval == null) return false;

        // Recompute hash from stored image
        String imageHash = sha256Hex(safe(signature.getSignatureImage()));
        String expected = buildSignatureHash(
            evaluationId, eval.getIdProjet(),
            String.format(java.util.Locale.ROOT, "%.2f", eval.getScoreGlobal()),
            safe(eval.getDecision()),
            safe(signature.getSignedByName()) + "|" + imageHash,
            safe(signature.getSignedAt())
        );
        return expected.equals(signature.getSignatureHash());
    }

    // ── DB helpers ────────────────────────────────────────────────────────

    private Evaluation loadEvaluation(long evaluationId) {
        String sql = "SELECT id_evaluation, id_projet, score_final, est_valide, " +
                     "observations_globales FROM evaluation WHERE id_evaluation=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, evaluationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Evaluation e = new Evaluation();
                    e.setIdEvaluation(rs.getInt("id_evaluation"));
                    e.setIdProjet(rs.getInt("id_projet"));
                    e.setScoreGlobal(rs.getDouble("score_final"));
                    e.setEstValide(rs.getBoolean("est_valide"));
                    e.setObservations(rs.getString("observations_globales"));
                    return e;
                }
            }
        } catch (SQLException ex) {
            System.err.println("[Signature] loadEvaluation failed: " + ex.getMessage());
        }
        return null;
    }

    private void saveObservations(long evaluationId, String observations) {
        String sql = "UPDATE evaluation SET observations_globales=? WHERE id_evaluation=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, observations);
            ps.setLong(2, evaluationId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[Signature] saveObservations failed: " + ex.getMessage());
            throw new RuntimeException("Impossible de sauvegarder la signature: " + ex.getMessage(), ex);
        }
    }

    // ── Crypto ────────────────────────────────────────────────────────────

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private String safe(String s) { return s != null ? s : ""; }
}
