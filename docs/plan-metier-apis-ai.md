# Plan métier avancé, APIs et IA – Évaluation/Audit Carbone

Objectifs
- Structurer un moteur métier de scoring et de décision robuste (pondération, seuils, règles par secteur).
- Exposer des APIs REST pour orchestrer l’audit, la validation et le suivi des rapports/évaluations.
- Ajouter une brique IA pour suggestions, détection d’anomalies et aide à la décision avec explicabilité.
- Intégrer ceci de façon incrémentale sans casser l’existant côté UI (JavaFX) et services.

1) Métier avancé
1.1 Moteur de score pondéré
- Calculer un score global à partir des critères pondérés (issus des références de critères).
- Supporter plusieurs méthodes:
  - Moyenne pondérée classique.
  - Score pénalisé si “critères bloquants” non respectés (hard stop).
  - Score ajusté par secteur/réglementation (tables de seuils externes).
- Exposer un service dédié (ex: ScoringService) avec:
  - calculateScore(evaluationResults, context)
  - explainScore(evaluationResults) -> liste des contributions par critère (utile pour l’UI et l’IA).

1.2 Règles métier et seuils
- Règles par secteur/activité (ex: énergie, bâtiment, industrie) avec “Seuils réglementaires” (par région).
- Critères bloquants: si non respectés ⇒ décision “REJETER” ou forcer “MODIFICATIONS REQUISES”.
- Définir un RuleEngine simple (ex: interface PolicyRule + implémentations) pour:
  - apply(project, evaluation, results) -> PolicyOutcome (OK, WARN, BLOCK) + messages.
- Externaliser la configuration (JSON/YAML) des seuils et poids spécifiques par secteur.

1.3 Workflow et statuts
- Workflow states: EN_ATTENTE → EN_EVALUATION → VALIDE | REJETE | A_REVISER.
- Journaliser les transitions (audit trail) avec utilisateur, timestamp, justification.
- Mécanisme d’assignation automatique à un expert (règles: secteur, charge, SLA).

1.4 Traçabilité
- Historiser les versions d’une évaluation (v1, v2, …), raisons de modification, critères changés.
- Conserver les “explanations” du score et les “policy outcomes” pour audit ultérieur.

1.5 Scénarios et sensibilité
- Simuler des variantes (ex: +10% d’efficience sur critère X) et recalculer le score.
- Fournir des “what-if” pour aider un porteur de projet à améliorer son score avant soumission.

2) APIs REST (exposition)
Nota: ces endpoints peuvent être fournis via un module REST (Spring Boot ou JAX-RS). Les payloads réutilisent les modèles courants (projets, évaluations, rapports) en DTOs.

2.1 Évaluations
- POST /api/evaluations
  - body: { idProjet, observations, decision?, criteres:[{idCritere, note, commentaire, estRespecte}] }
  - crée une évaluation, applique scoring + règles, renvoie {idEvaluation, score, decision, explanations, policyOutcomes}
- PUT /api/evaluations/{id}
  - met à jour une évaluation, recalcule le score et les règles, versionne l’historique.
- GET /api/evaluations?projetId=...
  - liste des évaluations par projet (avec pagination, filtres par dates/decision).
- GET /api/evaluations/{id}
  - détails d’une évaluation, y compris contributions des critères et justifications.

2.2 Critères (référentiels et résultats)
- GET /api/criteres/references
  - liste des critères de référence avec poids, description, catégorie, secteur.
- POST /api/criteres/references
  - ajoute un critère de référence (protégé par rôle).
- DELETE /api/criteres/references/{id}
  - suppression (si non utilisé) ou soft-delete.
- GET /api/evaluations/{id}/resultats
  - résultats des critères d’une évaluation.

2.3 Rapports carbone (validation Expert)
- POST /api/reports
  - soumission d’un rapport en attente (liée à un projet).
- POST /api/reports/{id}/validate
  - corps: { expertId, emissionsEstimate, evaluationDetails, commentaires }
  - applique règles, bascule statut en VALIDE, trace l’action, recalcul KPIs.
- POST /api/reports/{id}/reject
  - corps: { expertId, motifRejet }
  - statut REJETE + log.
- GET /api/reports?statut=EN_ATTENTE|VALIDE|REJETE
  - filtrage par statut, + search par projet / porteur.
- GET /api/reports/stats
  - { total, pending, validated, rejected, avgEmissions, parSecteur... }

2.4 IA et recommandations
- POST /api/ai/evaluations/suggest-decision
  - body: { criteres, contexteProjet }
  - renvoie: { suggestionDecision, confiance, topFactors, warnings }
- POST /api/ai/evaluations/what-if
  - body: { criteres, modifications:[{idCritere, deltaNote}], nScenarios }
  - renvoie: scénarios + scores.

3) IA – Approche et intégration
3.1 Baseline heuristique (immédiate)
- Règles heuristiques: pondérations + seuils + critères bloquants => suggestion APPROVE/REJECT.
- Score-based suggestion: mapping score -> décision avec marges grises (“needs review”).

3.2 Modèle supervisé (Java friendly)
- Bibliothèques: Tribuo, Smile, Weka (au choix).
- Features candidates:
  - Statistiques issues des critères (moyenne, min/max, variance, somme pondérée).
  - Métadonnées projet (secteur, budget, taille, région).
  - Historique: décisions passées, retours, révisions, états finaux.
- Target: décision finale (binaire/multi-classes) ou score numérique à prédire.
- Pipeline:
  1) DataLoader: extrait des historiques d’évaluations/rapports.
  2) FeatureBuilder: transforme criteres -> features agrégées.
  3) Train/Validate: split temporel + cross-validation.
  4) Export du modèle: artefact .model.
  5) InferenceService: charge le modèle au démarrage et sert les prédictions.
- Explicabilité:
  - Importance des features (permutation, Gini, etc.).
  - Score d’influence par critère (remonté vers l’UI).

3.3 Détection d’anomalies
- IsolationForest/LOF sur features des projets/évaluations validées.
- Endpoint: /api/ai/evaluations/anomaly-score -> signale dossiers atypiques.

3.4 NLP (optionnel)
- Extraction d’indices depuis la description projet (mots-clés, classification secteur).
- Enrichit les features du modèle supervisé.

3.5 Alternative microservice IA (Python)
- Si préférence ML Python: microservice FastAPI exposant /predict, /train, /explain.
- Le service Java consomme ces endpoints (time-out, retry, circuit-breaker).

4) Sécurité et gouvernance
- AuthN/AuthZ: JWT, RBAC (EXPERT_CARBONE, PORTEUR, ADMIN).
- Règles d’accès: certaines routes POST/DELETE protégées.
- Audit logs des actions sensibles (validation/rejet) et des appels IA (inputs/outputs).
- Protection API: rate limiting, input validation, size limits pour payloads.

5) Observabilité et performance
- Metrics: latences endpoints, taux d’acceptation/révision/rejet, dérive du modèle (concept drift).
- Logs structurés + corrélation.
- Cache des referentiels de critères et seuils.
- Async pour opérations lourdes (scénarios, entraînement IA).

6) Plan d’implémentation incrémental
Étape 1 – Refactoring léger
- Introduire interfaces: ScoringService, PolicyEngine, AiSuggestionService.
- Implémentations “baseline” (pas d’IA) + tests unitaires.

Étape 2 – APIs REST essentielles
- Endpoints Évaluations et Reports (create/update/get, validate/reject) + DTOs + mapping.
- SSO/JWT minimal, rôles.

Étape 3 – Règles avancées & Explications
- PolicyEngine avec seuils par secteur (fichier config).
- explainScore() intégré aux réponses API et UI.

Étape 4 – IA v1 (supervisée)
- Data extraction + entraînement + modèle de prédiction embarqué.
- Endpoint /api/ai/evaluations/suggest-decision.

Étape 5 – Scénarios & Anomalies
- What-if + endpoint anomaly-score.
- UI: preview de scénarios et alertes anomalies.

Étape 6 – Observabilité & Hardening
- Metrics, audit trail, rate limiting, tests d’intégration, CI/CD.

7) Impacts sur le code (ciblage)
- Services:
  - Ajouter ScoringService (moteur), PolicyEngine (règles), AiSuggestionService (prédictions).
  - Étendre services existants pour appeler ces briques lors de la création/modification d’évaluation/validation de rapport.
- Modèles/DTOs:
  - Enrichir critères avec catégorie, maxNote, “blocking”.
  - Ajouter fields “explanations”, “policyOutcomes”, “confidence” (côté DTOs).
- UI (contrôleurs existants):
  - Afficher le détail des contributions au score, suggestions IA, anomalies, et scénarios “what-if”.
  - Préserver compatibilité si IA indisponible (fallback heuristique).

8) Données & schéma (si DB)
- Tables supplémentaires:
  - evaluation_versions, policy_audit, model_metadata, scenario_runs.
- Index sur clés de recherche (projet, statut, dates).
- Historisation et soft-delete pour référentiels.

Ce plan fournit un cadre clair et incrémental: on commence par un moteur métier solide et des APIs REST, puis on plug l’IA (suggestion + anomalies) et on renforce sécurité/observabilité. Il reste compatible avec l’UI actuelle tout en ouvrant la voie à des usages avancés.
