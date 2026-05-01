-- ============================================================
-- GreenLedger — Swipe Card + Stripe Payment Migration
-- Run once against the greenledger database
-- ============================================================

-- 1. Add Stripe + status columns to financements table
ALTER TABLE financements
  ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS statut                   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  ADD COLUMN IF NOT EXISTS completed_at             DATETIME     DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS investisseur_id          BIGINT       DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS project_id               INT          DEFAULT NULL;

-- 2. Index for fast webhook lookup
CREATE INDEX IF NOT EXISTS idx_fin_intent
  ON financements (stripe_payment_intent_id);

-- 3. Add statut_financement + funded_at to projet if missing
ALTER TABLE projet
  ADD COLUMN IF NOT EXISTS statut_financement VARCHAR(30) DEFAULT 'SEEKING_FUNDING',
  ADD COLUMN IF NOT EXISTS funded_at          DATETIME    DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS roi                DOUBLE      DEFAULT NULL;

-- 4. conversation_threads (already exists, ensure columns)
CREATE TABLE IF NOT EXISTS conversation_threads (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  project_id      INT    NOT NULL,
  investisseur_id BIGINT NOT NULL,
  porteur_id      BIGINT NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. thread_messages (already exists, ensure columns)
CREATE TABLE IF NOT EXISTS thread_messages (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  thread_id  INT    NOT NULL,
  sender_id  BIGINT NOT NULL,
  content    LONGTEXT NOT NULL,
  sent_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_read    TINYINT(1) NOT NULL DEFAULT 0
);

-- 6. notifications (already exists, ensure columns)
CREATE TABLE IF NOT EXISTS notifications (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id            BIGINT NOT NULL,
  type               VARCHAR(50) NOT NULL,
  message            LONGTEXT NOT NULL,
  is_read            TINYINT(1) NOT NULL DEFAULT 0,
  related_project_id INT DEFAULT NULL,
  redirect_url       VARCHAR(500) DEFAULT NULL,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. swipe_decisions — track investor swipe history
CREATE TABLE IF NOT EXISTS swipe_decisions (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  investisseur_id BIGINT NOT NULL,
  project_id      INT    NOT NULL,
  decision        ENUM('RIGHT','LEFT','SKIP') NOT NULL,
  decided_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_swipe (investisseur_id, project_id)
);
