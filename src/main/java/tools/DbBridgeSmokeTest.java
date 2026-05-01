package tools;

import Models.MlDecisionSnapshot;
import Models.MlPrediction;
import Models.PdfExportLog;
import Models.dto.CarbonMetricResult;
import Services.CarbonMetricService;
import Services.MlDecisionSnapshotService;
import Services.MlPredictionService;
import Services.PdfExportService;

/**
 * Quick smoke test for Java/Web DB bridge reads.
 * Usage: DbBridgeSmokeTest <projectId> [evaluationId]
 */
public class DbBridgeSmokeTest {

    public static void main(String[] args) {
        int projectId = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        Integer evaluationId = args.length > 1 ? Integer.parseInt(args[1]) : null;

        CarbonMetricService carbonService = new CarbonMetricService();
        MlPredictionService mlService = new MlPredictionService();
        MlDecisionSnapshotService snapshotService = new MlDecisionSnapshotService();
        PdfExportService pdfService = new PdfExportService();

        CarbonMetricResult carbon = carbonService.findLatestByProject(projectId);
        MlPrediction ml = mlService.findLatestByProject(projectId);
        MlDecisionSnapshot snap = snapshotService.findLatestByProject(projectId);
        PdfExportLog pdf = pdfService.findLatestByProject(projectId);

        System.out.println("[BRIDGE] Project=" + projectId);
        System.out.println("[BRIDGE] Carbon total=" + (carbon != null ? carbon.getTotalTco2() : null));
        System.out.println("[BRIDGE] ML decision=" + (ml != null ? ml.getDecision() : null));
        System.out.println("[BRIDGE] Snapshot decision=" + (snap != null ? snap.getDecision() : null));
        System.out.println("[BRIDGE] PDF status=" + (pdf != null ? pdf.getStatus() : null));

        if (evaluationId != null) {
            CarbonMetricResult carbonEval = carbonService.findLatestByEvaluation(evaluationId);
            MlPrediction mlEval = mlService.findLatestByEvaluation(evaluationId);
            MlDecisionSnapshot snapEval = snapshotService.findLatestByEvaluation(evaluationId);
            PdfExportLog pdfEval = pdfService.findLatestByEvaluation(evaluationId);

            System.out.println("[BRIDGE] Evaluation=" + evaluationId);
            System.out.println("[BRIDGE] Carbon total (eval)=" + (carbonEval != null ? carbonEval.getTotalTco2() : null));
            System.out.println("[BRIDGE] ML decision (eval)=" + (mlEval != null ? mlEval.getDecision() : null));
            System.out.println("[BRIDGE] Snapshot decision (eval)=" + (snapEval != null ? snapEval.getDecision() : null));
            System.out.println("[BRIDGE] PDF status (eval)=" + (pdfEval != null ? pdfEval.getStatus() : null));
        }
    }
}

