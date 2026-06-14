-- Add manager-specific staff leave and dish price history tables.
-- These tables support employee leave/dismissal workflows and price-change audit trails.

CREATE TABLE IF NOT EXISTS staff_leave_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  leave_type VARCHAR(32) NOT NULL DEFAULT 'PERSONAL',
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  reason VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
  approved_by BIGINT NULL,
  approved_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_leave_store_user (store_id, user_id),
  KEY idx_leave_date (start_date, end_date),
  KEY idx_leave_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS dish_price_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dish_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  old_price DECIMAL(10,2) NOT NULL,
  new_price DECIMAL(10,2) NOT NULL,
  changed_by BIGINT NULL,
  reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_dish_price_history_dish (dish_id),
  KEY idx_dish_price_history_store (store_id),
  KEY idx_dish_price_history_changed_by (changed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
