-- Seed unified demo data for newly added tables.
-- Keep this file free of real credentials and production-only data.

INSERT INTO cat (store_id, name, breed, age, gender, personality, health_status, photo_url, description, status) VALUES
  (1, '拿铁', '英短', 3, 'MALE', '亲人、安静', '健康', '', '适合陪伴预约顾客互动', 'AVAILABLE'),
  (1, '布丁', '布偶', 2, 'FEMALE', '活泼、爱玩', '健康', '', '适合亲子互动', 'AVAILABLE'),
  (1, '团子', '橘猫', 4, 'MALE', '贪睡、温顺', '健康', '', '适合安静陪伴', 'RESTING')
ON DUPLICATE KEY UPDATE
  breed = VALUES(breed),
  age = VALUES(age),
  gender = VALUES(gender),
  personality = VALUES(personality),
  health_status = VALUES(health_status),
  photo_url = VALUES(photo_url),
  description = VALUES(description),
  status = VALUES(status),
  deleted = 0;

INSERT INTO cat_schedule (cat_id, store_id, schedule_date, start_time, end_time, status, remark)
SELECT c.id, c.store_id, CURRENT_DATE, '10:00:00', '14:00:00', 'AVAILABLE', '上午互动时段'
FROM cat c
WHERE c.store_id = 1 AND c.name IN ('拿铁', '布丁')
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  remark = VALUES(remark);

INSERT INTO cat_schedule (cat_id, store_id, schedule_date, start_time, end_time, status, remark)
SELECT c.id, c.store_id, CURRENT_DATE, '14:00:00', '18:00:00', 'RESTING', '下午休息观察'
FROM cat c
WHERE c.store_id = 1 AND c.name = '团子'
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  remark = VALUES(remark);

INSERT INTO promotion_activity (title, type, description, cover_url, start_at, end_at, status, created_by) VALUES
  ('周末猫爪甜品节', 'PROMOTION', '周末到店点单享甜品折扣。', '', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'PUBLISHED', NULL),
  ('猫咪互动课堂', 'ENTERTAINMENT', '预约猫咪互动区可参与猫咪护理小课堂。', '', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'PUBLISHED', NULL)
ON DUPLICATE KEY UPDATE
  type = VALUES(type),
  description = VALUES(description),
  cover_url = VALUES(cover_url),
  start_at = VALUES(start_at),
  end_at = VALUES(end_at),
  status = VALUES(status),
  deleted = 0;

INSERT INTO activity_store (activity_id, store_id, accept_status, handled_by, handled_at, handle_remark)
SELECT a.id, 1, 'ACCEPTED', u.id, NOW(), '演示门店已接受活动'
FROM promotion_activity a
LEFT JOIN `user` u ON u.username = 'demo_manager'
WHERE a.title IN ('周末猫爪甜品节', '猫咪互动课堂')
ON DUPLICATE KEY UPDATE
  accept_status = VALUES(accept_status),
  handled_by = VALUES(handled_by),
  handled_at = VALUES(handled_at),
  handle_remark = VALUES(handle_remark);

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 1, 'STAFF', 'ACTIVE'
FROM `user` u
WHERE u.username = 'demo_staff'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 1, 'STORE_MANAGER', 'ACTIVE'
FROM `user` u
WHERE u.username = 'demo_manager'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 1, 'CAT_CARETAKER', 'ACTIVE'
FROM `user` u
WHERE u.username = 'demo_cat'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 1, u.id, 'STAFF', CURRENT_DATE, '10:00:00', '18:00:00', 'SCHEDULED', '店员白班演示排班'
FROM `user` u
WHERE u.username = 'demo_staff'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 1, u.id, 'CAT_CARETAKER', CURRENT_DATE, '10:00:00', '18:00:00', 'SCHEDULED', '猫咪管家白班演示排班'
FROM `user` u
WHERE u.username = 'demo_cat'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO user_preference (user_id, preference_type, preference_value)
SELECT u.id, 'AREA', '猫咪互动区'
FROM `user` u
WHERE u.username = 'demo_customer'
ON DUPLICATE KEY UPDATE preference_value = VALUES(preference_value);

INSERT INTO user_preference (user_id, preference_type, preference_value)
SELECT u.id, 'TASTE', '甜品'
FROM `user` u
WHERE u.username = 'demo_customer'
ON DUPLICATE KEY UPDATE preference_value = VALUES(preference_value);

INSERT INTO points_transaction (user_id, member_account_id, order_id, type, points, balance_after, description)
SELECT u.id, m.id, NULL, 'ADJUST', 0, m.points, '演示会员积分初始化记录'
FROM `user` u
JOIN member_account m ON m.user_id = u.id
WHERE u.username = 'demo_customer';

UPDATE store
SET area_square_meter = COALESCE(area_square_meter, 180.00),
    business_area = COALESCE(business_area, '核心商圈'),
    latitude = COALESCE(latitude, 31.2304000),
    longitude = COALESCE(longitude, 121.4737000)
WHERE id = 1;
