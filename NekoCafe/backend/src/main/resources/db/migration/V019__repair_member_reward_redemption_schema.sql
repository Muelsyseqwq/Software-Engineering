CREATE TABLE IF NOT EXISTS reward_catalog (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) NULL,
  points_cost INT NOT NULL,
  reward_type VARCHAR(32) NOT NULL DEFAULT 'COUPON',
  cover_url VARCHAR(255) NULL,
  stock INT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  valid_from DATETIME NULL,
  valid_to DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_reward_catalog_status (status, deleted),
  KEY idx_reward_catalog_points_cost (points_cost),
  KEY idx_reward_catalog_validity (valid_from, valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS reward_redemption (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  redemption_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  member_account_id BIGINT NOT NULL,
  reward_id BIGINT NOT NULL,
  reward_name VARCHAR(128) NOT NULL,
  points_cost INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'REDEEMED',
  redeemed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_reward_redemption_no (redemption_no),
  KEY idx_reward_redemption_user (user_id, redeemed_at),
  KEY idx_reward_redemption_member (member_account_id),
  KEY idx_reward_redemption_reward (reward_id),
  KEY idx_reward_redemption_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @add_reward_redemption_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE points_transaction ADD COLUMN reward_redemption_id BIGINT NULL AFTER order_id', 'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'points_transaction'
    AND COLUMN_NAME = 'reward_redemption_id'
);
PREPARE add_reward_redemption_id_stmt FROM @add_reward_redemption_id;
EXECUTE add_reward_redemption_id_stmt;
DEALLOCATE PREPARE add_reward_redemption_id_stmt;

SET @add_points_reward_redemption_idx = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE points_transaction ADD KEY idx_points_reward_redemption (reward_redemption_id)', 'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'points_transaction'
    AND INDEX_NAME = 'idx_points_reward_redemption'
);
PREPARE add_points_reward_redemption_idx_stmt FROM @add_points_reward_redemption_idx;
EXECUTE add_points_reward_redemption_idx_stmt;
DEALLOCATE PREPARE add_points_reward_redemption_idx_stmt;

INSERT INTO reward_catalog (name, description, points_cost, reward_type, stock, status)
SELECT '8 元甜品券', '兑换后到店出示记录，可抵扣甜品消费 8 元。', 80, 'COUPON', 200, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM reward_catalog WHERE name = '8 元甜品券');

INSERT INTO reward_catalog (name, description, points_cost, reward_type, stock, status)
SELECT '猫咪互动优先券', '兑换后到店可优先安排一次猫咪互动时段。', 120, 'SERVICE', 120, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM reward_catalog WHERE name = '猫咪互动优先券');

INSERT INTO reward_catalog (name, description, points_cost, reward_type, stock, status)
SELECT '限定猫爪拿铁兑换券', '兑换一杯限定猫爪拿铁，适合答辩前补充猫咖能量。', 180, 'ITEM', 80, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM reward_catalog WHERE name = '限定猫爪拿铁兑换券');

INSERT INTO reward_catalog (name, description, points_cost, reward_type, stock, status)
SELECT '生日月小食券', '生日月可兑换一份猫咖小食，领取时请出示兑换记录。', 260, 'COUPON', 60, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM reward_catalog WHERE name = '生日月小食券');
