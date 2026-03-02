package Services;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.util.ArrayList;

/**
 * TauxRecommendationService
 * --------------------------
 * Uses the trained Weka LinearRegression model (ClaudeTaux.model)
 * to predict the optimal financing taux (%) for a project.
 *
 * LOGIC:
 *   1. Caller fetches the Financement row → gets projet_id
 *   2. Caller fetches Projet row by that projet_id → gets score_esg, statut, entreprise_id
 *   3. Caller fetches OffreFinancement row → gets duree, type_offre
 *   4. This service receives all those values, builds a Weka Instance,
 *      runs the model, and returns the predicted taux as a double
 *
 * MODEL ATTRIBUTES (must match ARFF exactly):
 *   score_esg             NUMERIC
 *   budget_montant        NUMERIC
 *   statut                {SUBMITTED,IN_PROGRESS,APPROVED,CANCELLED,DRAFT}
 *   duree                 NUMERIC
 *   type_offre            {Solaire,Eolien,Agricole,Immobilier,Microcredit,Recyclage,PME,LongTerme}
 *   anciennete_entreprise NUMERIC
 *   taux                  NUMERIC  ← this is what the model predicts
 *
 * MODEL FILE: src/main/resources/FinanceAiModels/ClaudeTaux.model
 */
public class TauxRecommendationService {

    private Classifier model;
    private Instances datasetStructure;

    public TauxRecommendationService() {
        try {
            // Load the trained model from resources
            model = (Classifier) SerializationHelper.read(
                    getClass().getResourceAsStream("/FinanceAiModels/InvestmentModel.model")
            );
            buildDatasetStructure();
            System.out.println("[TauxRecommendationService] Model loaded successfully.");
        } catch (Exception e) {
            System.err.println("[TauxRecommendationService] Failed to load model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Builds the exact same attribute structure as the ARFF file
    // The order MUST match the ARFF attribute order exactly
    // ─────────────────────────────────────────────────────────────
    private void buildDatasetStructure() {
        ArrayList<Attribute> attributes = new ArrayList<>();

        // 1. score_esg — NUMERIC
        attributes.add(new Attribute("score_esg"));

        // 2. budget_montant — NUMERIC
        attributes.add(new Attribute("budget_montant"));

        // 3. statut — NOMINAL (must match ARFF values exactly)
        ArrayList<String> statutValues = new ArrayList<>();
        statutValues.add("SUBMITTED");
        statutValues.add("IN_PROGRESS");
        statutValues.add("APPROVED");
        statutValues.add("CANCELLED");
        statutValues.add("DRAFT");
        attributes.add(new Attribute("statut", statutValues));

        // 4. duree — NUMERIC
        attributes.add(new Attribute("duree"));

        // 5. type_offre — NOMINAL (must match ARFF values exactly)
        ArrayList<String> typeValues = new ArrayList<>();
        typeValues.add("Solaire");
        typeValues.add("Eolien");
        typeValues.add("Agricole");
        typeValues.add("Immobilier");
        typeValues.add("Microcredit");
        typeValues.add("Recyclage");
        typeValues.add("PME");
        typeValues.add("LongTerme");
        attributes.add(new Attribute("type_offre", typeValues));

        // 6. anciennete_entreprise — NUMERIC
        attributes.add(new Attribute("anciennete_entreprise"));

        // 7. taux — NUMERIC (the class/target — set to missing for prediction)
        attributes.add(new Attribute("taux"));

        // Build dataset structure with 1 instance capacity
        datasetStructure = new Instances("green_finance_taux_predictor", attributes, 1);
        datasetStructure.setClassIndex(datasetStructure.numAttributes() - 1);
    }

    // ─────────────────────────────────────────────────────────────
    // Main prediction method
    //
    // @param scoreEsg           projet.score_esg (use 50 if null)
    // @param budgetMontant      budget.montant linked to projet (use 100000 if null)
    // @param statut             projet.statut (SUBMITTED/IN_PROGRESS/APPROVED/CANCELLED/DRAFT)
    // @param duree              offre_financement.duree (months)
    // @param typeOffre          offre_financement.type_offre mapped to ARFF label
    // @param ancienneteEntreprise years entreprise has been active (use 5 if unknown)
    //
    // @return predicted taux % (e.g. 4.23) or -1 if model failed
    // ─────────────────────────────────────────────────────────────
    public double predictTaux(int scoreEsg, double budgetMontant, String statut,
                              int duree, String typeOffre, double ancienneteEntreprise) {
        if (model == null) {
            System.err.println("[TauxRecommendationService] Model not loaded.");
            return -1;
        }

        try {
            // Create a single instance with all attribute values
            Instance instance = new DenseInstance(datasetStructure.numAttributes());
            instance.setDataset(datasetStructure);

            // Set each attribute value in the exact order of the ARFF
            instance.setValue(datasetStructure.attribute("score_esg"), scoreEsg);
            instance.setValue(datasetStructure.attribute("budget_montant"), budgetMontant);
            instance.setValue(datasetStructure.attribute("statut"), normalizeStatut(statut));
            instance.setValue(datasetStructure.attribute("duree"), duree);
            instance.setValue(datasetStructure.attribute("type_offre"), mapTypeOffre(typeOffre));
            instance.setValue(datasetStructure.attribute("anciennete_entreprise"), ancienneteEntreprise);

            // Class (taux) is missing — this is what we want to predict
            instance.setClassMissing();

            // Run the model
            double predicted = model.classifyInstance(instance);
            return Math.round(predicted * 100.0) / 100.0; // round to 2 decimal places

        } catch (Exception e) {
            System.err.println("[TauxRecommendationService] Prediction error: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Normalizes statut from DB to ARFF-valid value
    // DB might have lowercase or unexpected values
    // ─────────────────────────────────────────────────────────────
    private String normalizeStatut(String statut) {
        if (statut == null) return "DRAFT";
        switch (statut.toUpperCase().trim()) {
            case "APPROVED":    return "APPROVED";
            case "IN_PROGRESS": return "IN_PROGRESS";
            case "SUBMITTED":   return "SUBMITTED";
            case "CANCELLED":   return "CANCELLED";
            default:            return "DRAFT";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Maps the real type_offre string from DB to ARFF label
    // The ARFF uses simplified single-word labels
    // ─────────────────────────────────────────────────────────────
    public String mapTypeOffre(String typeOffre) {
        if (typeOffre == null) return "PME";
        String t = typeOffre.toLowerCase();
        if (t.contains("solaire") || t.contains("solar"))       return "Solaire";
        if (t.contains("eolien") || t.contains("vent"))         return "Eolien";
        if (t.contains("agricol"))                               return "Agricole";
        if (t.contains("immobilier"))                            return "Immobilier";
        if (t.contains("micro") || t.contains("credit"))        return "Microcredit";
        if (t.contains("recycl"))                                return "Recyclage";
        if (t.contains("long"))                                  return "LongTerme";
        return "PME"; // default fallback
    }

    // ─────────────────────────────────────────────────────────────
    // Interprets the predicted taux and returns a human-readable
    // explanation of what the rate means
    // ─────────────────────────────────────────────────────────────
    public String interpretTaux(double taux) {
        if (taux < 0)  return "Modèle indisponible";
        if (taux <= 3) return "Excellent — Projet vert de premier rang";
        if (taux <= 5) return "Très bon — Profil solide et fiable";
        if (taux <= 7) return "Bon — Conditions de financement standard";
        if (taux <= 9) return "Modéré — ESG ou statut à améliorer";
        if (taux <= 12) return "Élevé — Projet à risque significatif";
        return "Très élevé — Profil de risque critique";
    }
}
