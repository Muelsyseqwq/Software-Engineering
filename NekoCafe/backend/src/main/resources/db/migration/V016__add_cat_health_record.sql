-- Add historical cat health records for caretaker-managed archives.

CREATE TABLE IF NOT EXISTS cat_health_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cat_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  weight DECIMAL(4,2) NULL COMMENT '体重，单位 kg',
  vaccinium VARCHAR(255) NULL COMMENT '疫苗接种信息',
  interact VARCHAR(255) NULL COMMENT '互动记录',
  note VARCHAR(500) NULL COMMENT '健康备注',
  recorded_by BIGINT NULL COMMENT '记录人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_cat_health_record_cat_date (cat_id, record_date),
  KEY idx_cat_health_record_store_date (store_id, record_date),
  KEY idx_cat_health_record_recorded_by (recorded_by),
  CONSTRAINT chk_cat_health_record_weight_range CHECK (weight IS NULL OR (weight >= 0.20 AND weight <= 20.00))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO cat_health_record (cat_id, store_id, record_date, weight, vaccinium, interact, note, recorded_by)
SELECT id, store_id, CURDATE(), weight, vaccinium, interact, '由现有猫咪档案初始化', NULL
FROM cat
WHERE deleted = 0
  AND (weight IS NOT NULL OR vaccinium IS NOT NULL OR interact IS NOT NULL);
