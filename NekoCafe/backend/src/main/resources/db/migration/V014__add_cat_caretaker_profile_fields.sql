-- Add caretaker-maintained cat profile fields.

ALTER TABLE cat
  ADD COLUMN weight DECIMAL(4,2) NULL COMMENT '体重，单位 kg' AFTER age,
  ADD COLUMN interact VARCHAR(255) NULL COMMENT '互动偏好与注意事项' AFTER personality,
  ADD COLUMN vaccinium VARCHAR(255) NULL COMMENT '疫苗接种信息' AFTER health_status,
  ADD CONSTRAINT chk_cat_weight_range CHECK (weight IS NULL OR (weight >= 0.20 AND weight <= 20.00));
