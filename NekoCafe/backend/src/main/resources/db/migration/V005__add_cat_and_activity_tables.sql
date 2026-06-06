-- Add cat management and activity publishing tables.
-- This migration intentionally does not modify V001-V004.

CREATE TABLE IF NOT EXISTS cat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  breed VARCHAR(64) NULL,
  age INT NULL,
  gender VARCHAR(16) NULL,
  personality VARCHAR(255) NULL,
  health_status VARCHAR(64) NOT NULL DEFAULT '健康',
  photo_url VARCHAR(255) NULL,
  description TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_cat_store_name (store_id, name),
  KEY idx_cat_store_status (store_id, status),
  KEY idx_cat_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS cat_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cat_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  schedule_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cat_schedule_time (cat_id, schedule_date, start_time, end_time),
  KEY idx_cat_schedule_store_date (store_id, schedule_date),
  KEY idx_cat_schedule_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS reservation_cat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  cat_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reservation_cat (reservation_id, cat_id),
  KEY idx_reservation_cat_reservation (reservation_id),
  KEY idx_reservation_cat_cat (cat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS promotion_activity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL DEFAULT 'PROMOTION',
  description TEXT NULL,
  cover_url VARCHAR(255) NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_activity_title (title),
  KEY idx_activity_type_status (type, status),
  KEY idx_activity_time (start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS activity_store (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  accept_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  handled_by BIGINT NULL,
  handled_at DATETIME NULL,
  handle_remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_activity_store (activity_id, store_id),
  KEY idx_activity_store_activity (activity_id),
  KEY idx_activity_store_store (store_id),
  KEY idx_activity_store_accept_status (accept_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
