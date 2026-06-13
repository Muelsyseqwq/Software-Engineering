SET @add_reward_discount_amount = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE reward_catalog ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 10.00 AFTER points_cost', 'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reward_catalog'
    AND COLUMN_NAME = 'discount_amount'
);
PREPARE add_reward_discount_amount_stmt FROM @add_reward_discount_amount;
EXECUTE add_reward_discount_amount_stmt;
DEALLOCATE PREPARE add_reward_discount_amount_stmt;

SET @add_order_reward_redemption_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE food_order ADD COLUMN reward_redemption_id BIGINT NULL AFTER total_amount', 'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'food_order'
    AND COLUMN_NAME = 'reward_redemption_id'
);
PREPARE add_order_reward_redemption_id_stmt FROM @add_order_reward_redemption_id;
EXECUTE add_order_reward_redemption_id_stmt;
DEALLOCATE PREPARE add_order_reward_redemption_id_stmt;

SET @add_order_coupon_discount_amount = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE food_order ADD COLUMN coupon_discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER reward_redemption_id', 'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'food_order'
    AND COLUMN_NAME = 'coupon_discount_amount'
);
PREPARE add_order_coupon_discount_amount_stmt FROM @add_order_coupon_discount_amount;
EXECUTE add_order_coupon_discount_amount_stmt;
DEALLOCATE PREPARE add_order_coupon_discount_amount_stmt;

SET @add_order_payable_amount = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE food_order ADD COLUMN payable_amount DECIMAL(10,2) NULL AFTER coupon_discount_amount', 'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'food_order'
    AND COLUMN_NAME = 'payable_amount'
);
PREPARE add_order_payable_amount_stmt FROM @add_order_payable_amount;
EXECUTE add_order_payable_amount_stmt;
DEALLOCATE PREPARE add_order_payable_amount_stmt;

SET @add_order_coupon_name = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE food_order ADD COLUMN coupon_name VARCHAR(128) NULL AFTER payable_amount', 'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'food_order'
    AND COLUMN_NAME = 'coupon_name'
);
PREPARE add_order_coupon_name_stmt FROM @add_order_coupon_name;
EXECUTE add_order_coupon_name_stmt;
DEALLOCATE PREPARE add_order_coupon_name_stmt;

SET @add_redemption_order_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE reward_redemption ADD COLUMN order_id BIGINT NULL AFTER reward_id', 'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reward_redemption'
    AND COLUMN_NAME = 'order_id'
);
PREPARE add_redemption_order_id_stmt FROM @add_redemption_order_id;
EXECUTE add_redemption_order_id_stmt;
DEALLOCATE PREPARE add_redemption_order_id_stmt;

SET @add_reward_redemption_user_status_idx = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE reward_redemption ADD KEY idx_reward_redemption_user_status_used (user_id, status, used_at)', 'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reward_redemption'
    AND INDEX_NAME = 'idx_reward_redemption_user_status_used'
);
PREPARE add_reward_redemption_user_status_idx_stmt FROM @add_reward_redemption_user_status_idx;
EXECUTE add_reward_redemption_user_status_idx_stmt;
DEALLOCATE PREPARE add_reward_redemption_user_status_idx_stmt;

SET @add_reward_redemption_order_idx = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE reward_redemption ADD KEY idx_reward_redemption_order (order_id)', 'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reward_redemption'
    AND INDEX_NAME = 'idx_reward_redemption_order'
);
PREPARE add_reward_redemption_order_idx_stmt FROM @add_reward_redemption_order_idx;
EXECUTE add_reward_redemption_order_idx_stmt;
DEALLOCATE PREPARE add_reward_redemption_order_idx_stmt;

SET @add_food_order_reward_redemption_idx = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE food_order ADD KEY idx_food_order_reward_redemption (reward_redemption_id)', 'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'food_order'
    AND INDEX_NAME = 'idx_food_order_reward_redemption'
);
PREPARE add_food_order_reward_redemption_idx_stmt FROM @add_food_order_reward_redemption_idx;
EXECUTE add_food_order_reward_redemption_idx_stmt;
DEALLOCATE PREPARE add_food_order_reward_redemption_idx_stmt;

SET @add_reservation_slot_template_idx = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE reservation_slot ADD KEY idx_reservation_slot_template_lookup (store_id, table_id, slot_date, start_time)', 'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reservation_slot'
    AND INDEX_NAME = 'idx_reservation_slot_template_lookup'
);
PREPARE add_reservation_slot_template_idx_stmt FROM @add_reservation_slot_template_idx;
EXECUTE add_reservation_slot_template_idx_stmt;
DEALLOCATE PREPARE add_reservation_slot_template_idx_stmt;

UPDATE reward_catalog
SET discount_amount = 8.00
WHERE reward_type = 'COUPON'
  AND name = '8 元甜品券';

UPDATE reward_catalog
SET discount_amount = 10.00
WHERE reward_type = 'COUPON'
  AND (discount_amount IS NULL OR discount_amount <= 0);

UPDATE food_order
SET payable_amount = GREATEST(total_amount - coupon_discount_amount, 0)
WHERE payable_amount IS NULL;
