-- V024: Add coupon distribution support to promotion activities
-- 1. Allow activities to link to a reward (coupon) from the catalog
-- 2. Track the source of reward redemptions (points vs activity gift)

ALTER TABLE promotion_activity
    ADD COLUMN reward_id BIGINT NULL AFTER description;

ALTER TABLE reward_redemption
    ADD COLUMN source_type VARCHAR(32) NULL AFTER points_cost,
    ADD COLUMN source_id BIGINT NULL AFTER source_type;

-- Index for dedup queries: find existing activity-gifted coupons for a user
CREATE INDEX idx_reward_redemption_source ON reward_redemption (source_type, source_id, user_id);
