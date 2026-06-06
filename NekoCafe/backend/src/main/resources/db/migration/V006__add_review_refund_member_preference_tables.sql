-- Add customer review, refund, member point transaction, and preference tables.
-- These tables support customer evaluation, refund application, points history, and personalized recommendations.

CREATE TABLE IF NOT EXISTS review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  reservation_id BIGINT NULL,
  cat_id BIGINT NULL,
  rating INT NOT NULL,
  content VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_review_user (user_id),
  KEY idx_review_store (store_id),
  KEY idx_review_order (order_id),
  KEY idx_review_cat (cat_id),
  KEY idx_review_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS refund_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  refund_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  payment_id BIGINT NULL,
  amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  review_remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_refund_user (user_id),
  KEY idx_refund_order (order_id),
  KEY idx_refund_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS points_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  member_account_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  type VARCHAR(32) NOT NULL,
  points INT NOT NULL,
  balance_after INT NOT NULL,
  description VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_points_user (user_id),
  KEY idx_points_member_account (member_account_id),
  KEY idx_points_order (order_id),
  KEY idx_points_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS user_preference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  preference_type VARCHAR(64) NOT NULL,
  preference_value VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_preference (user_id, preference_type, preference_value),
  KEY idx_user_preference_user (user_id),
  KEY idx_user_preference_type (preference_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
