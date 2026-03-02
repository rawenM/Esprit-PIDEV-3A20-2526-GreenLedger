package Services;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * RiskAnalysisService
 * -------------------
 * Loads the trained Weka J48 model (ClaudeRisk.model) and uses it
 * to predict investment risk level: "low", "medium", or "high"
 *
 * Called from: Controllers.FinanceRiskAgentController
 *
 * Field mapping (FXML field → ARFF attribute):
 *   txtMontant     → budget
 *   txtDuree       → duration
 *   cmbSecteur     → type       (solar / wind / agriculture / recycling)
 *   txtTaux        → co2_impact
 *   txtScoreCredit → maturity
 *   txtApport      → funding_ratio
 */
public class RiskAnalysisService {

    private Classifier model;
    private Instances datasetStructure;

    // ─────────────────────────────────────────────────────────────
    // Constructor — called in FinanceRiskAgentController.initialize()
    // Loads the model immediately so it's ready when user clicks Analyser
    // ─────────────────────────────────────────────────────────────
    public RiskAnalysisService() {
        try {
            loadModel();
            buildDatasetStructure();
            System.out.println("✅ RiskAnalysisService ready.");
        } catch (Exception e) {
            System.err.println("❌ RiskAnalysisService failed to init: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Loads ClaudeRisk.model from resources/FinanceAiModels/
    // ─────────────────────────────────────────────────────────────
    private void loadModel() throws Exception {
        InputStream is = getClass().getResourceAsStream("/FinanceAiModels/ClaudeRisk.model");

        if (is == null) {
            throw new Exception("❌ Model file not found at /FinanceAiModels/ClaudeRisk.model\n"
                    + "Make sure the file exists in src/main/resources/FinanceAiModels/");
        }

        ObjectInputStream ois = new ObjectInputStream(is);
        model = (Classifier) ois.readObject();
        ois.close();

        System.out.println("✅ ClaudeRisk.model loaded.");
    }

    // ─────────────────────────────────────────────────────────────
    // Builds the dataset schema — must match green_finance_risk.arff
    // EXACTLY: same attribute names, same order, same nominal values
    //
    // Attributes in order:
    //   1. budget          (NUMERIC)
    //   2. duration        (NUMERIC)
    //   3. type            (NOMINAL: solar, wind, agriculture, recycling)
    //   4. co2_impact      (NUMERIC)
    //   5. maturity        (NUMERIC)
    //   6. funding_ratio   (NUMERIC)
    //   7. risk            (NOMINAL: low, medium, high) ← CLASS
    // ─────────────────────────────────────────────────────────────
    private void buildDatasetStructure() {
        ArrayList<Attribute> attributes = new ArrayList<>();

        // 1. budget — from txtMontant
        attributes.add(new Attribute("budget"));

        // 2. duration — from txtDuree
        attributes.add(new Attribute("duration"));

        // 3. type — from cmbSecteur
        // Must match ARFF values exactly (lowercase)
        ArrayList<String> typeValues = new ArrayList<>();
        typeValues.add("solar");
        typeValues.add("wind");
        typeValues.add("agriculture");
        typeValues.add("recycling");
        attributes.add(new Attribute("type", typeValues));

        // 4. co2_impact — from txtTaux
        attributes.add(new Attribute("co2_impact"));

        // 5. maturity — from txtScoreCredit
        attributes.add(new Attribute("maturity"));

        // 6. funding_ratio — from txtApport
        attributes.add(new Attribute("funding_ratio"));

        // 7. risk — the class attribute (what we predict)
        // Must match ARFF values exactly (lowercase)
        ArrayList<String> riskValues = new ArrayList<>();
        riskValues.add("low");
        riskValues.add("medium");
        riskValues.add("high");
        attributes.add(new Attribute("risk", riskValues));

        // Create empty dataset with just the schema (0 rows)
        datasetStructure = new Instances("GreenFinanceRisk", attributes, 0);

        // Tell Weka the last attribute is the class we want to predict
        datasetStructure.setClassIndex(datasetStructure.numAttributes() - 1);

        System.out.println("✅ Dataset structure built. Attributes: "
                + datasetStructure.numAttributes()
                + ", Class: " + datasetStructure.classAttribute().name());
    }

    // ─────────────────────────────────────────────────────────────
    // THE MAIN METHOD — called from handleRunRiskAnalysis()
    //
    // @param budget        txtMontant value     (e.g. 120000)
    // @param duration      txtDuree value       (e.g. 5)
    // @param type          cmbSecteur value     (e.g. "solar")
    // @param co2Impact     txtTaux value        (e.g. 85.0)
    // @param maturity      txtScoreCredit value (e.g. 6.0)
    // @param fundingRatio  txtApport value      (e.g. 0.8)
    // @return              "low", "medium", "high", or "error"
    // ─────────────────────────────────────────────────────────────
    public String predictRisk(double budget, double duration, String type,
                              double co2Impact, double maturity, double fundingRatio) {

        // Guard: if model failed to load, return error immediately
        if (model == null || datasetStructure == null) {
            System.err.println("❌ Cannot predict: model or structure is null.");
            return "error";
        }

        try {
            // Step 1: Create one empty instance with 7 slots (one per attribute)
            DenseInstance instance = new DenseInstance(7);

            // Step 2: Attach it to our schema so Weka knows what each slot means
            instance.setDataset(datasetStructure);

            // Step 3: Fill in values — ORDER MUST MATCH buildDatasetStructure()
            instance.setValue(datasetStructure.attribute("budget"),        budget);
            instance.setValue(datasetStructure.attribute("duration"),      duration);
            instance.setValue(datasetStructure.attribute("type"),          type);
            instance.setValue(datasetStructure.attribute("co2_impact"),    co2Impact);
            instance.setValue(datasetStructure.attribute("maturity"),      maturity);
            instance.setValue(datasetStructure.attribute("funding_ratio"), fundingRatio);

            // Step 4: Ask J48 to classify — returns a double index (0.0, 1.0, or 2.0)
            // 0.0 = "low", 1.0 = "medium", 2.0 = "high"
            // (matches the order you declared riskValues above)
            double classIndex = model.classifyInstance(instance);

            // Step 5: Convert the double index back to the string label
            String result = datasetStructure.classAttribute().value((int) classIndex);

            System.out.println("✅ Prediction → budget=" + budget
                    + " | duration=" + duration
                    + " | type=" + type
                    + " | co2=" + co2Impact
                    + " | maturity=" + maturity
                    + " | fundingRatio=" + fundingRatio
                    + " → RISK: " + result.toUpperCase());

            return result;

        } catch (Exception e) {
            System.err.println("❌ Prediction failed: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
    }
}