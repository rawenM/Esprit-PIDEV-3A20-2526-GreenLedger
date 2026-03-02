-- ==================== CARBON ANALYTICS EXTENDED SCHEMA ====================
-- Target: Carbon Expert module (ML, synthetic data, risk, scenarios, APIs)
-- Compatible with MySQL/MariaDB (InnoDB, utf8mb4)

-- NOTE: Existing tables assumed:
--   projet, evaluation, evaluation_resultat, critere_reference
-- Add/alter columns as needed using the commented ALTERs below.

-- ==================== OPTIONAL EXTENSIONS TO EXISTING TABLES ====================
-- ALTER TABLE projet ADD COLUMN secteur VARCHAR(100) NULL;
-- ALTER TABLE projet ADD COLUMN region VARCHAR(100) NULL;
-- ALTER TABLE projet ADD COLUMN activite_type VARCHAR(120) NULL;
-- ALTER TABLE projet ADD COLUMN taille_entreprise ENUM('MICRO','SMALL','MEDIUM','LARGE') NULL;
-- ALTER TABLE evaluation ADD COLUMN version_no INT DEFAULT 1;
-- ALTER TABLE evaluation ADD COLUMN data_quality_score DECIMAL(5,2) NULL;
-- ALTER TABLE critere_reference ADD COLUMN categorie ENUM('E','S','G') DEFAULT 'E';
-- ALTER TABLE critere_reference ADD COLUMN est_bloquant BOOLEAN DEFAULT FALSE;
-- ALTER TABLE critere_reference ADD COLUMN note_max INT DEFAULT 10;

-- ==================== PROJECT PROFILE (BASELINE CONTEXT) ====================
CREATE TABLE IF NOT EXISTS carbon_project_profile (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    baseline_emissions_tco2 DECIMAL(12,3) NULL,
    target_reduction_pct DECIMAL(6,2) NULL,
    climate_zone VARCHAR(50) NULL,
    supply_chain_scope ENUM('SCOPE1','SCOPE2','SCOPE3','ALL') DEFAULT 'ALL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_profile_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== EMISSION FACTORS ====================
CREATE TABLE IF NOT EXISTS emission_factor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sector VARCHAR(100) NOT NULL,
    activity_type VARCHAR(120) NOT NULL,
    unit VARCHAR(40) NOT NULL,
    factor_value DECIMAL(12,6) NOT NULL,
    region VARCHAR(100) NULL,
    source VARCHAR(200) NULL,
    valid_from DATE NULL,
    valid_to DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_factor_sector (sector),
    INDEX idx_factor_activity (activity_type),
    INDEX idx_factor_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== CARBON METRICS PER PROJECT/EVALUATION ====================
CREATE TABLE IF NOT EXISTS carbon_metric (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    evaluation_id INT NULL,
    metric_date DATE NOT NULL,
    scope1_tco2 DECIMAL(12,3) NULL,
    scope2_tco2 DECIMAL(12,3) NULL,
    scope3_tco2 DECIMAL(12,3) NULL,
    total_tco2 DECIMAL(12,3) NULL,
    method VARCHAR(80) NULL,
    data_quality_score DECIMAL(5,2) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_project (project_id),
    INDEX idx_metric_eval (evaluation_id),
    INDEX idx_metric_date (metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== POLICY RULES & OUTCOMES ====================
CREATE TABLE IF NOT EXISTS policy_rule (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    severity ENUM('INFO','WARN','BLOCK') NOT NULL,
    rule_json JSON NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_policy_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS policy_outcome (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id INT NOT NULL,
    rule_id INT NOT NULL,
    outcome ENUM('OK','WARN','BLOCK') NOT NULL,
    message TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_policy_eval (evaluation_id),
    INDEX idx_policy_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== EVALUATION VERSIONING ====================
CREATE TABLE IF NOT EXISTS evaluation_version (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id INT NOT NULL,
    version_no INT NOT NULL,
    changed_by INT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    snapshot_json JSON NOT NULL,
    INDEX idx_eval_version (evaluation_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== SCENARIOS & WHAT-IF RUNS ====================
CREATE TABLE IF NOT EXISTS scenario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    base_evaluation_id INT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    created_by INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_scenario_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scenario_run (
    id INT AUTO_INCREMENT PRIMARY KEY,
    scenario_id INT NOT NULL,
    run_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assumptions_json JSON NOT NULL,
    status ENUM('PENDING','RUNNING','DONE','FAILED') DEFAULT 'PENDING',
    INDEX idx_run_scenario (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scenario_result (
    id INT AUTO_INCREMENT PRIMARY KEY,
    run_id INT NOT NULL,
    score_delta DECIMAL(8,3) NULL,
    metric_delta_json JSON NULL,
    decision_recommendation VARCHAR(40) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_result_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== RISK MODELS & SCORES ====================
CREATE TABLE IF NOT EXISTS risk_model (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    version VARCHAR(40) NOT NULL,
    algorithm VARCHAR(120) NULL,
    trained_at TIMESTAMP NULL,
    metrics_json JSON NULL,
    active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uq_risk_model (name, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS risk_score (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    evaluation_id INT NULL,
    model_id INT NOT NULL,
    risk_score DECIMAL(6,3) NOT NULL,
    risk_level ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    drivers_json JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_risk_project (project_id),
    INDEX idx_risk_eval (evaluation_id),
    INDEX idx_risk_model (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== ML DATASETS ====================
CREATE TABLE IF NOT EXISTS ml_dataset (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    purpose ENUM('TRAIN','VALID','TEST','INFERENCE') DEFAULT 'TRAIN',
    schema_version VARCHAR(20) DEFAULT 'v1',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ml_dataset_row (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id INT NOT NULL,
    project_id INT NULL,
    evaluation_id INT NULL,
    features_json JSON NOT NULL,
    label VARCHAR(40) NULL,
    split_tag ENUM('TRAIN','VALID','TEST') DEFAULT 'TRAIN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ml_dataset (dataset_id),
    INDEX idx_ml_project (project_id),
    INDEX idx_ml_eval (evaluation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== API AUDIT LOG ====================
CREATE TABLE IF NOT EXISTS api_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint VARCHAR(200) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_id VARCHAR(64) NULL,
    user_id INT NULL,
    status_code INT NOT NULL,
    latency_ms INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_endpoint (endpoint),
    INDEX idx_api_status (status_code),
    INDEX idx_api_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== SYNTHETIC DATA TRACKING ====================
CREATE TABLE IF NOT EXISTS synthetic_project (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NULL,
    seed INT NOT NULL,
    generation_params_json JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS synthetic_evaluation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    synthetic_project_id INT NOT NULL,
    evaluation_id INT NULL,
    noise_level DECIMAL(5,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_syn_project (synthetic_project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== OPTIONAL: SAMPLE SYNTHETIC DATA (MySQL 8 CTE) ====================
-- This block generates 1000 synthetic projects and 1000 evaluations (adjust counts as needed).
-- Requires existing tables `projet` and `evaluation`.
/*
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 1000
)
INSERT INTO projet(titre, description, budget, statut, entreprise_id)
SELECT
  CONCAT('Projet Synth ', n),
  CONCAT('Projet synthétique pour entraînement #', n),
  10000 + (n * 5),
  'SUBMITTED',
  1
FROM seq;

WITH RECURSIVE seq2 AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq2 WHERE n < 1000
)
INSERT INTO evaluation(observations_globales, score_final, est_valide, id_projet)
SELECT
  CONCAT('Observations synthétiques ', n),
  ROUND((RAND() * 10), 2),
  RAND() > 0.3,
  (SELECT id FROM projet ORDER BY id DESC LIMIT 1 OFFSET n)
FROM seq2;
*/

