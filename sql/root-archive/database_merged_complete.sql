-- phpMyAdmin SQL Dump
-- Merged Database Schema for GreenLedger
-- Server version: MariaDB 10.11 / MySQL 8.x compatible
-- Generation Time: Mar 03, 2026
-- MERGED FROM: greenledger (8).sql, greenledger (6)(1).sql, greenledger (7)(1).sql

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `greenledger`
--
CREATE DATABASE IF NOT EXISTS `greenledger` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `greenledger`;

DELIMITER $$
--
-- Procedures
--
DROP PROCEDURE IF EXISTS `sp_get_batch_provenance`$$
CREATE PROCEDURE `sp_get_batch_provenance` (IN `p_batch_id` INT)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SELECT 'Error retrieving batch provenance' AS error_message;
    END;
    
    START TRANSACTION;
    
    -- Get batch details
    SELECT * FROM carbon_credit_batches WHERE id = p_batch_id;
    
    -- Get batch events
    SELECT * FROM batch_events WHERE batch_id = p_batch_id ORDER BY created_at;
    
    -- Get parent batch if exists
    SELECT parent.* FROM carbon_credit_batches parent
    INNER JOIN carbon_credit_batches child ON child.parent_batch_id = parent.id
    WHERE child.id = p_batch_id;
    
    -- Get child batches
    SELECT * FROM carbon_credit_batches WHERE parent_batch_id = p_batch_id;
    
    -- Get retirement details
    SELECT brd.*, wt.reference_note, wt.created_at as transaction_date
    FROM batch_retirement_details brd
    INNER JOIN wallet_transactions wt ON wt.id = brd.transaction_id
    WHERE brd.batch_id = p_batch_id;
    
    COMMIT;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `action_type` varchar(50) NOT NULL,
  `entity_type` varchar(50) NOT NULL,
  `entity_id` bigint(20) DEFAULT NULL,
  `old_value` text DEFAULT NULL,
  `new_value` text DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_entity` (`entity_type`, `entity_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit log for user actions';

-- --------------------------------------------------------

--
-- Table structure for table `batch_events`
--

DROP TABLE IF EXISTS `batch_events`;
CREATE TABLE `batch_events` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` int(11) NOT NULL,
  `event_type` enum('ISSUED','TRANSFERRED','MARKETPLACE_SOLD','RETIRED','SPLIT','VERIFIED','DISPUTED') NOT NULL,
  `event_data_json` text DEFAULT NULL,
  `actor` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `previous_event_id` bigint(20) DEFAULT NULL,
  `event_hash` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_created` (`created_at`),
  KEY `idx_event_hash` (`event_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable event log for batch lifecycle';

-- --------------------------------------------------------

--
-- Table structure for table `batch_retirement_details`
--

DROP TABLE IF EXISTS `batch_retirement_details`;
CREATE TABLE `batch_retirement_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint(20) NOT NULL,
  `batch_id` int(11) NOT NULL,
  `amount_retired` decimal(15,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_transaction` (`transaction_id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Links retirement transactions to specific batches';

-- --------------------------------------------------------

--
-- Table structure for table `budget`
--

DROP TABLE IF EXISTS `budget`;
CREATE TABLE `budget` (
  `id_budget` int(11) NOT NULL AUTO_INCREMENT,
  `montant` bigint(20) NOT NULL,
  `raison` varchar(255) NOT NULL,
  `devise` varchar(10) NOT NULL DEFAULT 'TND',
  `id_projet` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_budget`),
  UNIQUE KEY `id_projet` (`id_projet`),
  KEY `idx_projet` (`id_projet`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Project budgets';

-- --------------------------------------------------------

--
-- Table structure for table `carbon_credit_batches`
--

DROP TABLE IF EXISTS `carbon_credit_batches`;
CREATE TABLE `carbon_credit_batches` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  `wallet_id` int(11) NOT NULL,
  `total_amount` decimal(15,2) NOT NULL,
  `remaining_amount` decimal(15,2) NOT NULL,
  `status` enum('AVAILABLE','PARTIALLY_RETIRED','FULLY_RETIRED','LOCKED','DISPUTED') NOT NULL DEFAULT 'AVAILABLE',
  `issued_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `batch_type` enum('PRIMARY','SECONDARY') DEFAULT 'PRIMARY',
  `calculation_audit_id` varchar(100) DEFAULT NULL,
  `parent_batch_id` int(11) DEFAULT NULL,
  `verification_standard` varchar(50) DEFAULT NULL,
  `vintage_year` int(11) DEFAULT NULL,
  `serial_number` varchar(50) DEFAULT NULL,
  `lineage_json` text DEFAULT NULL,
  `metadata_json` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_batch_wallet` (`wallet_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_status` (`status`),
  KEY `idx_parent` (`parent_batch_id`),
  KEY `idx_serial_number` (`serial_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Carbon credit batch tracking with full traceability';

-- --------------------------------------------------------

--
-- Table structure for table `carbon_price_history`
--

DROP TABLE IF EXISTS `carbon_price_history`;
CREATE TABLE `carbon_price_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `credit_type` varchar(100) NOT NULL,
  `usd_per_ton` decimal(10,4) NOT NULL,
  `market_index` varchar(50) DEFAULT NULL,
  `source_api` varchar(100) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_lookup` (`credit_type`, `timestamp` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Historical carbon price tracking';

-- --------------------------------------------------------

--
-- Table structure for table `carbon_projects`
--

DROP TABLE IF EXISTS `carbon_projects`;
CREATE TABLE `carbon_projects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `enterprise_id` bigint(20) NOT NULL,
  `project_name` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `estimated_reduction` decimal(15,2) DEFAULT NULL,
  `verified_reduction` decimal(15,2) DEFAULT NULL,
  `status` enum('PLANNED','IN_PROGRESS','VERIFIED','CANCELLED') NOT NULL DEFAULT 'PLANNED',
  `verification_date` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_enterprise` (`enterprise_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Carbon reduction projects';

-- --------------------------------------------------------

--
-- Table structure for table `critere_impact`
--

DROP TABLE IF EXISTS `critere_impact`;
CREATE TABLE `critere_impact` (
  `id_impact` int(11) NOT NULL AUTO_INCREMENT,
  `id_evaluation` int(11) NOT NULL,
  `domaine` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `niveau_impact` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_impact`),
  KEY `id_evaluation` (`id_evaluation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Evaluation impact criteria';

-- --------------------------------------------------------

--
-- Table structure for table `critere_reference`
--

DROP TABLE IF EXISTS `critere_reference`;
CREATE TABLE `critere_reference` (
  `id_critere` int(11) NOT NULL AUTO_INCREMENT,
  `nom_critere` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `poids` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_critere`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Reference criteria for evaluations';

-- --------------------------------------------------------

--
-- Table structure for table `dossier_projet`
--

DROP TABLE IF EXISTS `dossier_projet`;
CREATE TABLE `dossier_projet` (
  `id_dossier` int(11) NOT NULL AUTO_INCREMENT,
  `id_projet` int(11) NOT NULL,
  `nom_fichier` varchar(255) NOT NULL,
  `chemin_fichier` varchar(500) NOT NULL,
  `type_fichier` varchar(50) DEFAULT NULL,
  `date_upload` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_dossier`),
  UNIQUE KEY `id_projet` (`id_projet`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Project file attachments';

-- --------------------------------------------------------

--
-- Table structure for table `evaluation`
--

DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation` (
  `id_evaluation` int(11) NOT NULL AUTO_INCREMENT,
  `id_projet` int(11) NOT NULL,
  `date_evaluation` timestamp NOT NULL DEFAULT current_timestamp(),
  `observations_globales` text DEFAULT NULL,
  `score_final` decimal(5,2) DEFAULT NULL,
  `est_valide` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id_evaluation`),
  KEY `idx_projet` (`id_projet`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Project evaluations';

-- --------------------------------------------------------

--
-- Table structure for table `evaluation_resultat`
--

DROP TABLE IF EXISTS `evaluation_resultat`;
CREATE TABLE `evaluation_resultat` (
  `id_resultat` int(11) NOT NULL AUTO_INCREMENT,
  `id_evaluation` int(11) NOT NULL,
  `id_critere` int(11) NOT NULL,
  `est_respecte` tinyint(1) NOT NULL DEFAULT 0,
  `note` decimal(5,2) DEFAULT NULL,
  `commentaire_expert` text DEFAULT NULL,
  PRIMARY KEY (`id_resultat`),
  KEY `idx_evaluation` (`id_evaluation`),
  KEY `idx_critere` (`id_critere`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Detailed evaluation results per criteria';

-- --------------------------------------------------------

--
-- Table structure for table `financement`
--

DROP TABLE IF EXISTS `financement`;
CREATE TABLE `financement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `montant` decimal(15,2) NOT NULL,
  `taux_interet` decimal(5,2) DEFAULT NULL,
  `duree_mois` int(11) DEFAULT NULL,
  `statut` varchar(50) DEFAULT 'EN_ATTENTE',
  `projet_id` int(11) NOT NULL,
  `banque_id` bigint(20) DEFAULT NULL,
  `date_demande` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `projet_id` (`projet_id`),
  KEY `banque_id` (`banque_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Financing requests';

-- --------------------------------------------------------

--
-- Table structure for table `fraud_detection_results`
--

DROP TABLE IF EXISTS `fraud_detection_results`;
CREATE TABLE `fraud_detection_results` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `risk_score` decimal(5,2) NOT NULL,
  `risk_level` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  `indicators` text DEFAULT NULL,
  `recommendations` text DEFAULT NULL,
  `analysis_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `model_version` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_risk` (`risk_level`),
  KEY `idx_timestamp` (`analysis_timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI fraud detection results';

-- --------------------------------------------------------

--
-- Table structure for table `green_wallets`
--

DROP TABLE IF EXISTS `green_wallets`;
CREATE TABLE `green_wallets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `wallet_number` varchar(50) NOT NULL,
  `holder_name` varchar(255) NOT NULL,
  `owner_type` enum('ENTERPRISE','BANK','INDIVIDUAL','PLATFORM') NOT NULL,
  `owner_id` bigint(20) DEFAULT NULL,
  `available_credits` decimal(15,2) NOT NULL DEFAULT 0.00,
  `retired_credits` decimal(15,2) NOT NULL DEFAULT 0.00,
  `status` enum('ACTIVE','SUSPENDED','CLOSED','PENDING_REVIEW') NOT NULL DEFAULT 'ACTIVE',
  `registry_id` varchar(100) DEFAULT NULL,
  `is_external` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `wallet_number` (`wallet_number`),
  KEY `idx_owner` (`owner_type`, `owner_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Enhanced green wallets with registry integration';

-- --------------------------------------------------------

--
-- Table structure for table `marketplace_disputes`
--

DROP TABLE IF EXISTS `marketplace_disputes`;
CREATE TABLE `marketplace_disputes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `order_id` int(11) DEFAULT NULL,
  `trade_id` int(11) DEFAULT NULL,
  `raised_by_user_id` bigint(20) NOT NULL,
  `dispute_type` enum('PAYMENT_ISSUE','QUALITY_CONCERN','NON_DELIVERY','FRAUDULENT_ACTIVITY','OTHER') NOT NULL,
  `description` text NOT NULL,
  `status` enum('OPEN','UNDER_REVIEW','RESOLVED','ESCALATED','CLOSED') NOT NULL DEFAULT 'OPEN',
  `resolution_notes` text DEFAULT NULL,
  `resolved_by` bigint(20) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `resolved_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_trade` (`trade_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Marketplace dispute management';

-- --------------------------------------------------------

--
-- Table structure for table `marketplace_escrow`
--

DROP TABLE IF EXISTS `marketplace_escrow`;
CREATE TABLE `marketplace_escrow` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `order_id` int(11) NOT NULL,
  `trade_id` int(11) DEFAULT NULL,
  `buyer_id` bigint(20) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `amount_usd` decimal(15,2) NOT NULL,
  `held_by_platform` tinyint(1) NOT NULL DEFAULT 1,
  `stripe_hold_id` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','HELD','RELEASED_TO_SELLER','RELEASED_TO_BUYER','DISPUTED') NOT NULL DEFAULT 'PENDING',
  `hold_reason` text DEFAULT NULL,
  `release_date` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_trade` (`trade_id`),
  KEY `idx_status` (`status`),
  KEY `idx_active_holds` (`status`, `release_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Escrow for marketplace transactions';

-- --------------------------------------------------------

--
-- Table structure for table `marketplace_fees`
--

DROP TABLE IF EXISTS `marketplace_fees`;
CREATE TABLE `marketplace_fees` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `order_id` int(11) DEFAULT NULL,
  `trade_id` int(11) DEFAULT NULL,
  `seller_id` bigint(20) NOT NULL,
  `fee_amount_usd` decimal(10,2) NOT NULL,
  `fee_type` enum('TRANSACTION_FEE','LISTING_FEE','PREMIUM_FEE','DISPUTE_FEE') NOT NULL DEFAULT 'TRANSACTION_FEE',
  `description` text DEFAULT NULL,
  `status` enum('PENDING','COLLECTED','WAIVED','REFUNDED') NOT NULL DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `collected_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_trade` (`trade_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Platform fees for marketplace';

-- --------------------------------------------------------

--
-- Table structure for table `marketplace_listings`
--

DROP TABLE IF EXISTS `marketplace_listings`;
CREATE TABLE `marketplace_listings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `seller_id` bigint(20) NOT NULL,
  `wallet_id` int(11) NOT NULL,
  `listing_type` enum('SELL','BUY') NOT NULL DEFAULT 'SELL',
  `credit_amount` decimal(15,2) NOT NULL,
  `price_per_ton_usd` decimal(10,2) NOT NULL,
  `total_price_usd` decimal(15,2) GENERATED ALWAYS AS (`credit_amount` * `price_per_ton_usd`) STORED,
  `status` enum('ACTIVE','SOLD','CANCELLED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
  `description` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `expires_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_wallet` (`wallet_id`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`listing_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Marketplace listings for carbon credits';

-- --------------------------------------------------------

--
-- Table structure for table `marketplace_orders`
--

DROP TABLE IF EXISTS `marketplace_orders`;
CREATE TABLE `marketplace_orders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `listing_id` int(11) DEFAULT NULL,
  `buyer_id` bigint(20) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `credit_amount` decimal(15,2) NOT NULL,
  `price_per_ton_usd` decimal(10,2) NOT NULL,
  `total_price_usd` decimal(15,2) NOT NULL,
  `stripe_payment_intent` varchar(255) DEFAULT NULL,
  `status` enum('PENDING_PAYMENT','PAID','COMPLETED','FAILED','CANCELLED','REFUNDED') NOT NULL DEFAULT 'PENDING_PAYMENT',
  `payment_method` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `completed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_listing` (`listing_id`),
  KEY `idx_buyer` (`buyer_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_status` (`status`),
  KEY `idx_stripe` (`stripe_payment_intent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Marketplace order processing';

-- --------------------------------------------------------

--
-- Table structure for table `marketplace_reviews`
--

DROP TABLE IF EXISTS `marketplace_reviews`;
CREATE TABLE `marketplace_reviews` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `order_id` int(11) NOT NULL,
  `reviewer_id` bigint(20) NOT NULL,
  `reviewed_user_id` bigint(20) NOT NULL,
  `rating` tinyint(4) NOT NULL,
  `comment` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_reviewed_user` (`reviewed_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Marketplace user reviews';

-- --------------------------------------------------------

--
-- Table structure for table `marketplace_trades`
--

DROP TABLE IF EXISTS `marketplace_trades`;
CREATE TABLE `marketplace_trades` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `listing_id` int(11) DEFAULT NULL,
  `buyer_id` bigint(20) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `batch_id` int(11) NOT NULL,
  `credit_amount` decimal(15,2) NOT NULL,
  `price_per_ton_usd` decimal(10,2) NOT NULL,
  `total_price_usd` decimal(15,2) NOT NULL,
  `trade_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `settlement_status` enum('PENDING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
  PRIMARY KEY (`id`),
  KEY `idx_listing` (`listing_id`),
  KEY `idx_buyer` (`buyer_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Completed marketplace trades';

-- --------------------------------------------------------

--
-- Table structure for table `offre_financement`
--

DROP TABLE IF EXISTS `offre_financement`;
CREATE TABLE `offre_financement` (
  `id_offre` int(11) NOT NULL AUTO_INCREMENT,
  `id_financement` int(11) NOT NULL,
  `banque_id` bigint(20) NOT NULL,
  `montant_propose` decimal(15,2) NOT NULL,
  `taux_interet` decimal(5,2) NOT NULL,
  `duree_mois` int(11) NOT NULL,
  `status` varchar(50) DEFAULT 'EN_ATTENTE',
  `date_offre` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_offre`),
  UNIQUE KEY `id_financement` (`id_financement`, `banque_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bank financing offers';

-- --------------------------------------------------------

--
-- Table structure for table `operation_wallet`
--

DROP TABLE IF EXISTS `operation_wallet`;
CREATE TABLE `operation_wallet` (
  `id_operation` int(11) NOT NULL AUTO_INCREMENT,
  `id_wallet` int(11) NOT NULL,
  `type_operation` varchar(50) NOT NULL,
  `montant` decimal(15,2) NOT NULL,
  `description` text DEFAULT NULL,
  `date_operation` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_operation`),
  KEY `id_wallet` (`id_wallet`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wallet operations history';

-- --------------------------------------------------------

--
-- Table structure for table `project_document`
--

DROP TABLE IF EXISTS `project_document`;
CREATE TABLE `project_document` (
  `id_document` int(11) NOT NULL AUTO_INCREMENT,
  `id_projet` int(11) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `stored_name` varchar(255) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `mime_type` varchar(100) DEFAULT NULL,
  `file_size` bigint(20) DEFAULT NULL,
  `is_image` tinyint(1) NOT NULL DEFAULT 0,
  `uploaded_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_document`),
  KEY `fk_doc_projet` (`id_projet`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Project document attachments';

-- --------------------------------------------------------

--
-- Table structure for table `projet`
--

DROP TABLE IF EXISTS `projet`;
CREATE TABLE `projet` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `entreprise_id` bigint(20) DEFAULT NULL,
  `titre` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `statut` enum('SUBMITTED','IN_PROGRESS','COMPLETED','CANCELLED','PENDING_REVIEW') NOT NULL DEFAULT 'SUBMITTED',
  `score_esg` decimal(5,2) DEFAULT NULL,
  `date_creation` timestamp NOT NULL DEFAULT current_timestamp(),
  `company_address` varchar(500) DEFAULT NULL,
  `company_email` varchar(255) DEFAULT NULL,
  `company_phone` varchar(50) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `entreprise_id` (`entreprise_id`),
  KEY `idx_status` (`statut`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Projects';

-- --------------------------------------------------------

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nom_role` varchar(50) NOT NULL,
  `description` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nom_role` (`nom_role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User roles';

-- --------------------------------------------------------

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `prenom` varchar(100) NOT NULL,
  `email` varchar(255) NOT NULL,
  `mot_de_passe` varchar(255) NOT NULL,
  `telephone` varchar(50) DEFAULT NULL,
  `adresse` varchar(500) DEFAULT NULL,
  `date_naissance` date DEFAULT NULL,
  `type_utilisateur` enum('ADMIN','PORTEUR_PROJET','INVESTISSEUR','BANQUE','EXPERT_CARBONE') NOT NULL,
  `statut` enum('EN_ATTENTE','ACTIVE','BLOQUE','SUSPENDU') NOT NULL DEFAULT 'EN_ATTENTE',
  `photo` varchar(500) DEFAULT NULL,
  `date_inscription` timestamp NOT NULL DEFAULT current_timestamp(),
  `derniere_connexion` timestamp NULL DEFAULT NULL,
  `email_verifie` tinyint(1) NOT NULL DEFAULT 0,
  `token_verification` varchar(255) DEFAULT NULL,
  `token_expiry` timestamp NULL DEFAULT NULL,
  `token_hash` varchar(255) DEFAULT NULL,
  `fraud_score` decimal(5,2) DEFAULT NULL,
  `fraud_checked` tinyint(1) NOT NULL DEFAULT 0,
  `fraud_check_date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_type` (`type_utilisateur`),
  KEY `idx_statut` (`statut`),
  KEY `idx_email_verified` (`email_verifie`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Users with fraud detection';

-- --------------------------------------------------------

--
-- Table structure for table `wallet`
--

DROP TABLE IF EXISTS `wallet`;
CREATE TABLE `wallet` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `wallet_number` varchar(50) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `owner_type` varchar(50) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `available_credits` decimal(15,2) NOT NULL DEFAULT 0.00,
  `retired_credits` decimal(15,2) NOT NULL DEFAULT 0.00,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `wallet_number` (`wallet_number`),
  KEY `idx_owner` (`owner_type`, `owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Basic wallets';

-- --------------------------------------------------------

--
-- Table structure for table `wallet_transactions`
--

DROP TABLE IF EXISTS `wallet_transactions`;
CREATE TABLE `wallet_transactions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `wallet_id` int(11) NOT NULL,
  `batch_id` int(11) DEFAULT NULL,
  `type` enum('ISSUE','RETIRE','TRANSFER_IN','TRANSFER_OUT','PURCHASE','SALE') NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `reference_note` text DEFAULT NULL,
  `transfer_pair_id` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_wallet` (`wallet_id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_type` (`type`),
  KEY `idx_created` (`created_at`),
  KEY `idx_transfer_pair` (`transfer_pair_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wallet transaction history';

-- --------------------------------------------------------

--
-- Views for batch lineage tracking
--

DROP VIEW IF EXISTS `v_batch_full_lineage`;
CREATE VIEW `v_batch_full_lineage` AS
WITH RECURSIVE batch_tree AS (
    -- Base case: root batches (no parent)
    SELECT id, parent_batch_id, project_id, wallet_id, total_amount, 
           remaining_amount, status, batch_type, 0 as lineage_depth
    FROM carbon_credit_batches
    WHERE parent_batch_id IS NULL
    
    UNION ALL
    
    -- Recursive case: child batches
    SELECT c.id, c.parent_batch_id, c.project_id, c.wallet_id, c.total_amount,
           c.remaining_amount, c.status, c.batch_type, bt.lineage_depth + 1
    FROM carbon_credit_batches c
    INNER JOIN batch_tree bt ON c.parent_batch_id = bt.id
)
SELECT * FROM batch_tree;

-- --------------------------------------------------------

--
-- Sample data for testing (Optional - comment out if not needed)
--

-- Insert admin user
INSERT INTO `user` (`nom`, `prenom`, `email`, `mot_de_passe`, `type_utilisateur`, `statut`, `email_verifie`) VALUES
('Admin', 'Super', 'admin@plateforme.com', '$2a$12$vtpnsqNvz3Gr8PjRk9PsHepmJRGIyHRXsFBUl65a38o/.OX55Aci.', 'ADMIN', 'ACTIVE', 1);

-- Insert reference criteria
INSERT INTO `critere_reference` (`nom_critere`, `description`, `poids`) VALUES
('Emissions CO2', 'impact sur le gaz', 3),
('Consommation d\'eau', 'Gestion durable de l\'eau', 2),
('Déchets recyclés', 'Taux de recyclage des déchets', 2);

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
