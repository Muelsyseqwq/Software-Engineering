-- Add store staff relationship, staff shifts, table status logs, and order/reservation tracking fields.
-- This migration enables store-scoped manager/staff features.

CREATE TABLE IF NOT EXISTS user_store_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  dismissed_by BIGINT NULL,
  dismissed_at DATETIME NULL,
  dismiss_reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_store_role (user_id, store_id, role_code),
  KEY idx_user_store_role_user (user_id),
  KEY idx_user_store_role_store (store_id),
  KEY idx_user_store_role_role (role_code),
  KEY idx_user_store_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS staff_shift (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  shift_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
  remark VARCHAR(255) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_staff_shift_user_time (user_id, shift_date, start_time, end_time),
  KEY idx_staff_shift_store_date (store_id, shift_date),
  KEY idx_staff_shift_user_date (user_id, shift_date),
  KEY idx_staff_shift_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS dining_table_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  table_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  old_status VARCHAR(32) NULL,
  new_status VARCHAR(32) NOT NULL,
  changed_by BIGINT NULL,
  reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_table_status_log_table (table_id),
  KEY idx_table_status_log_store (store_id),
  KEY idx_table_status_log_changed_by (changed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE food_order
  ADD COLUMN table_id BIGINT NULL,
  ADD COLUMN paid_at DATETIME NULL,
  ADD COLUMN completed_at DATETIME NULL,
  ADD COLUMN cancelled_at DATETIME NULL,
  ADD COLUMN refund_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
  ADD KEY idx_order_table (table_id),
  ADD KEY idx_order_refund_status (refund_status);

ALTER TABLE reservation
  ADD COLUMN checked_out_at DATETIME NULL,
  ADD COLUMN completed_at DATETIME NULL,
  ADD COLUMN cancelled_at DATETIME NULL,
  ADD KEY idx_reservation_table_status (table_id, status);
