-- Add store display/location/area fields and normalize existing business status values.
-- Store area supports per-square-meter revenue calculations.

ALTER TABLE store
  ADD COLUMN business_area VARCHAR(128) NULL,
  ADD COLUMN latitude DECIMAL(10,7) NULL,
  ADD COLUMN longitude DECIMAL(10,7) NULL,
  ADD COLUMN cover_url VARCHAR(255) NULL,
  ADD COLUMN area_square_meter DECIMAL(10,2) NULL,
  ADD KEY idx_store_location (latitude, longitude);

UPDATE food_order
SET status = 'CREATED'
WHERE status IS NULL OR status = '';

UPDATE food_order SET status = 'CREATED' WHERE status = '待支付';
UPDATE food_order SET status = 'PAID' WHERE status = '已支付';
UPDATE food_order SET status = 'PREPARING' WHERE status = '制作中';
UPDATE food_order SET status = 'COMPLETED' WHERE status = '已完成';
UPDATE food_order SET status = 'CANCELLED' WHERE status = '已取消';
UPDATE food_order SET status = 'REFUNDING' WHERE status = '退款中';
UPDATE food_order SET status = 'REFUNDED' WHERE status = '已退款';

UPDATE reservation
SET status = 'RESERVED'
WHERE status = 'PENDING_PAYMENT';

UPDATE reservation
SET status = 'RESERVED'
WHERE status IS NULL OR status = '';

UPDATE reservation SET status = 'RESERVED' WHERE status = '已预约';
UPDATE reservation SET status = 'CHECKED_IN' WHERE status = '已签到';
UPDATE reservation SET status = 'COMPLETED' WHERE status = '已完成';
UPDATE reservation SET status = 'CANCELLED' WHERE status = '已取消';
UPDATE reservation SET status = 'NO_SHOW' WHERE status = '爽约';

UPDATE dining_table
SET status = 'AVAILABLE'
WHERE status IS NULL OR status = '';

UPDATE dining_table SET status = 'AVAILABLE' WHERE status = '空闲';
UPDATE dining_table SET status = 'OCCUPIED' WHERE status = '使用中';
UPDATE dining_table SET status = 'RESERVED' WHERE status = '已预约';
UPDATE dining_table SET status = 'CLEANING' WHERE status = '清洁中';
UPDATE dining_table SET status = 'DISABLED' WHERE status = '停用';

UPDATE food_order fo
JOIN reservation r ON fo.reservation_id = r.id
SET fo.table_id = r.table_id
WHERE fo.table_id IS NULL;

UPDATE store
SET area_square_meter = 180.00,
    business_area = COALESCE(business_area, '核心商圈')
WHERE id = 1;
