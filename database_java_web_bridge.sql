-- Java <-> Web bridge migration
-- Apply on the same MySQL schema used by both apps (ex: greenledger).

-- 1) ML predictions
CREATE TABLE IF NOT EXISTS ml_predictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id INT NULL,
    project_id INT NULL,
    predicted_esg_score INT NULL,
    credibility_score INT NULL,
    carbon_risk VARCHAR(50) NULL,
    decision VARCHAR(50) NULL,
    recommendations TEXT NULL,
    model_version VARCHAR(120) NULL,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ml_predictions_project_created (project_id, created_at),
    INDEX idx_ml_predictions_eval_created (evaluation_id, created_at)
);

-- 2) ML decision snapshots
CREATE TABLE IF NOT EXISTS ml_decision_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NULL,
    evaluation_id INT NULL,
    project_name VARCHAR(255) NULL,
    decision VARCHAR(50) NULL,
    confidence DECIMAL(10,6) NULL,
    score DECIMAL(10,6) NULL,
    compliance DECIMAL(10,6) NULL,
    min_note INT NULL,
    esg_score INT NULL,
    factors LONGTEXT NULL,
    explanation LONGTEXT NULL,
    recommendations LONGTEXT NULL,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ml_snapshots_project_created (project_id, created_at),
    INDEX idx_ml_snapshots_eval_created (evaluation_id, created_at)
);

ALTER TABLE ml_decision_snapshots
    ADD COLUMN IF NOT EXISTS project_id INT NULL,
    ADD COLUMN IF NOT EXISTS evaluation_id INT NULL,
    ADD COLUMN IF NOT EXISTS project_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS decision VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS confidence DECIMAL(10,6) NULL,
    ADD COLUMN IF NOT EXISTS score DECIMAL(10,6) NULL,
    ADD COLUMN IF NOT EXISTS compliance DECIMAL(10,6) NULL,
    ADD COLUMN IF NOT EXISTS min_note INT NULL,
    ADD COLUMN IF NOT EXISTS esg_score INT NULL,
    ADD COLUMN IF NOT EXISTS factors LONGTEXT NULL,
    ADD COLUMN IF NOT EXISTS explanation LONGTEXT NULL,
    ADD COLUMN IF NOT EXISTS recommendations LONGTEXT NULL,
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 3) PDF export logs
CREATE TABLE IF NOT EXISTS pdf_exports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id INT NULL,
    project_id INT NULL,
    provider VARCHAR(80) NULL,
    output_path VARCHAR(1024) NULL,
    status VARCHAR(60) NULL,
    error_message TEXT NULL,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pdf_exports_project_created (project_id, created_at),
    INDEX idx_pdf_exports_eval_created (evaluation_id, created_at)
);

-- 4) Carbon metric table used by Java service
CREATE TABLE IF NOT EXISTS carbon_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    evaluation_id INT NULL,
    metric_date DATE NULL,
    scope1_tco2 DECIMAL(14,6) NULL,
    scope2_tco2 DECIMAL(14,6) NULL,
    scope3_tco2 DECIMAL(14,6) NULL,
    total_tco2 DECIMAL(14,6) NULL,
    method VARCHAR(120) NULL,
    data_quality_score DECIMAL(10,4) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_carbon_metric_project_date (project_id, metric_date),
    INDEX idx_carbon_metric_eval_date (evaluation_id, metric_date)
);

ALTER TABLE carbon_metric
    ADD COLUMN IF NOT EXISTS evaluation_id INT NULL,
    ADD COLUMN IF NOT EXISTS metric_date DATE NULL,
    ADD COLUMN IF NOT EXISTS scope1_tco2 DECIMAL(14,6) NULL,
    ADD COLUMN IF NOT EXISTS scope2_tco2 DECIMAL(14,6) NULL,
    ADD COLUMN IF NOT EXISTS scope3_tco2 DECIMAL(14,6) NULL,
    ADD COLUMN IF NOT EXISTS total_tco2 DECIMAL(14,6) NULL,
    ADD COLUMN IF NOT EXISTS method VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS data_quality_score DECIMAL(10,4) NULL,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

