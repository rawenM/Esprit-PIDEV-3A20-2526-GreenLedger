package Models;

public enum StatutUtilisateur {
    EN_ATTENTE("En attente", "#FFA500"),
    ACTIVE("Active", "#28A745"),
    BLOQUE("Bloqué", "#DC3545"),
    SUSPENDU("Suspendu", "#6C757D");

    private final String libelle;
    private final String couleur;

    StatutUtilisateur(String libelle, String couleur) {
        this.libelle = libelle;
        this.couleur = couleur;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getCouleur() {
        return couleur;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
