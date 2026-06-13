-- V025: Add level-gated rewards and idempotent activity claim guard

ALTER TABLE reward_catalog
  ADD COLUMN required_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'Required member level: NORMAL, VIP, SVIP' AFTER reward_type;

ALTER TABLE reward_redemption
  ADD UNIQUE KEY uk_reward_redemption_activity_claim (source_type, source_id, user_id, reward_id);

INSERT INTO reward_catalog (
  name,
  description,
  points_cost,
  discount_amount,
  reward_type,
  required_level,
  cover_url,
  stock,
  status,
  valid_from,
  valid_to,
  deleted
)
SELECT
  'VIP 猫咪限定周边',
  'VIP 及以上会员可兑换的猫咪主题限定周边。',
  180,
  0.00,
  'ITEM',
  'VIP',
  NULL,
  30,
  'ACTIVE',
  NULL,
  NULL,
  0
WHERE NOT EXISTS (
  SELECT 1
  FROM reward_catalog
  WHERE name = 'VIP 猫咪限定周边'
);

INSERT INTO reward_catalog (
  name,
  description,
  points_cost,
  discount_amount,
  reward_type,
  required_level,
  cover_url,
  stock,
  status,
  valid_from,
  valid_to,
  deleted
)
SELECT
  'SVIP 专属包间体验券',
  'SVIP 会员专属，到店可享一次主题包间体验。',
  360,
  0.00,
  'SERVICE',
  'SVIP',
  NULL,
  10,
  'ACTIVE',
  NULL,
  NULL,
  0
WHERE NOT EXISTS (
  SELECT 1
  FROM reward_catalog
  WHERE name = 'SVIP 专属包间体验券'
);
