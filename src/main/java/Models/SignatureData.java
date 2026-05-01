package Models;

/**
 * Holds the parsed signature block from evaluation.observations_globales.
 *
 * Storage format appended to observations_globales:
 *
 *   <visible comment>
 *
 *   [SIGNATURE]
 *   signedAt=2026-04-30 14:23:11
 *   signedByName=Mehdi Benzaied
 *   signatureHash=a3f9b2c1d4e5f6789abc...
 *   signatureImage=data:image/png;base64,iVBORw0KGgo...
 */
public class SignatureData {

    private String  commentaireGlobal;   // visible comment (before [SIGNATURE])
    private String  signedAt;            // nullable
    private String  signedByName;        // nullable
    private String  signatureHash;       // SHA-256 hex, nullable
    private String  signatureImage;      // base64 data URL, nullable
    private boolean isSigned;

    public SignatureData() {}

    public SignatureData(String commentaireGlobal, boolean isSigned) {
        this.commentaireGlobal = commentaireGlobal;
        this.isSigned = isSigned;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public String  getCommentaireGlobal()              { return commentaireGlobal; }
    public void    setCommentaireGlobal(String v)      { this.commentaireGlobal = v; }

    public String  getSignedAt()                       { return signedAt; }
    public void    setSignedAt(String v)               { this.signedAt = v; }

    public String  getSignedByName()                   { return signedByName; }
    public void    setSignedByName(String v)           { this.signedByName = v; }

    public String  getSignatureHash()                  { return signatureHash; }
    public void    setSignatureHash(String v)          { this.signatureHash = v; }

    public String  getSignatureImage()                 { return signatureImage; }
    public void    setSignatureImage(String v)         { this.signatureImage = v; }

    public boolean isSigned()                          { return isSigned; }
    public void    setSigned(boolean v)                { this.isSigned = v; }
}
