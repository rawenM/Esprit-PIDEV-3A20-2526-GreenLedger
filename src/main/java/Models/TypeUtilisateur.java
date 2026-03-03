package Models;

public enum TypeUtilisateur {
    INVESTISSEUR("Investisseur"),
    PORTEUR_PROJET("Porteur de Projet"),
    EXPERT_CARBONE("Expert Carbone"),
    ADMIN("Administrateur");

    private final String libelle;

    TypeUtilisateur(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
