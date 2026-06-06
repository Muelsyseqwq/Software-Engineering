-- Seed broader system test data for multi-store, multi-role, reservation, order, and operations scenarios.
-- All passwords use the same BCrypt hash as existing demo users for the course demo password.
-- Keep this file free of real credentials and production-only data.

INSERT INTO store (id, name, city, business_area, address, latitude, longitude, phone, opening_time, closing_time, status, description, cover_url, area_square_meter) VALUES
  (2, 'NekoCafé 夏夜外滩店', '上海', '外滩商圈', '外滩绒球街 6 号', 31.2397000, 121.4998000, '021-10000002', '09:30:00', '23:00:00', 'OPEN', '适合夜间活动和江景猫咪互动的演示门店', '', 220.00),
  (3, 'NekoCafé 云朵西湖店', '杭州', '西湖商圈', '西湖区猫爪路 18 号', 30.2460000, 120.1500000, '0571-10000003', '10:00:00', '22:00:00', 'OPEN', '适合亲子互动和周末活动的演示门店', '', 160.00),
  (4, 'NekoCafé 试营业静安店', '上海', '静安商圈', '静安区小鱼干路 9 号', 31.2292000, 121.4481000, '021-10000004', '11:00:00', '21:00:00', 'PREPARING', '用于测试停业、筹备和不可预约状态的门店', '', 140.00)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  city = VALUES(city),
  business_area = VALUES(business_area),
  address = VALUES(address),
  latitude = VALUES(latitude),
  longitude = VALUES(longitude),
  phone = VALUES(phone),
  opening_time = VALUES(opening_time),
  closing_time = VALUES(closing_time),
  status = VALUES(status),
  description = VALUES(description),
  cover_url = VALUES(cover_url),
  area_square_meter = VALUES(area_square_meter),
  deleted = 0;

INSERT INTO dining_table (store_id, table_no, capacity, area, status) VALUES
  (2, 'A01', 2, '窗边猫咪互动区', 'AVAILABLE'),
  (2, 'A02', 4, '窗边猫咪互动区', 'AVAILABLE'),
  (2, 'B01', 6, '团体用餐区', 'OCCUPIED'),
  (2, 'C01', 2, '安静阅读区', 'CLEANING'),
  (3, 'A01', 2, '亲子互动区', 'AVAILABLE'),
  (3, 'A02', 4, '亲子互动区', 'AVAILABLE'),
  (3, 'B01', 4, '安静用餐区', 'RESERVED'),
  (3, 'C01', 6, '活动教室', 'AVAILABLE'),
  (4, 'A01', 2, '试营业互动区', 'DISABLED')
ON DUPLICATE KEY UPDATE
  capacity = VALUES(capacity),
  area = VALUES(area),
  status = VALUES(status),
  deleted = 0;

INSERT INTO `user` (username, password_hash, nickname, phone, email, status) VALUES
  ('demo_customer2', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '甜品偏好会员', NULL, 'demo_customer2@nekocafe.local', 'ACTIVE'),
  ('demo_customer3', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '安静区会员', NULL, 'demo_customer3@nekocafe.local', 'ACTIVE'),
  ('demo_staff2', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '外滩前台铃铛', NULL, 'demo_staff2@nekocafe.local', 'ACTIVE'),
  ('demo_staff3', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '西湖前台铃铛', NULL, 'demo_staff3@nekocafe.local', 'ACTIVE'),
  ('demo_staff_leave', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '请假店员演示', NULL, 'demo_staff_leave@nekocafe.local', 'ACTIVE'),
  ('demo_manager2', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '外滩门店掌柜', NULL, 'demo_manager2@nekocafe.local', 'ACTIVE'),
  ('demo_manager3', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '西湖门店掌柜', NULL, 'demo_manager3@nekocafe.local', 'ACTIVE'),
  ('demo_cat2', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '外滩猫咪管家', NULL, 'demo_cat2@nekocafe.local', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  status = VALUES(status),
  deleted = 0;

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'CUSTOMER'
WHERE u.username IN ('demo_customer2', 'demo_customer3')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'STAFF'
WHERE u.username IN ('demo_staff2', 'demo_staff3', 'demo_staff_leave')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'STORE_MANAGER'
WHERE u.username IN ('demo_manager2', 'demo_manager3')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'CAT_CARETAKER'
WHERE u.username IN ('demo_cat2')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO member_account (user_id, level_code, points, total_spent)
SELECT u.id, 'SILVER', 120, 238.00 FROM `user` u
WHERE u.username = 'demo_customer2'
ON DUPLICATE KEY UPDATE level_code = VALUES(level_code), points = VALUES(points), total_spent = VALUES(total_spent);

INSERT INTO member_account (user_id, level_code, points, total_spent)
SELECT u.id, 'NORMAL', 30, 76.00 FROM `user` u
WHERE u.username = 'demo_customer3'
ON DUPLICATE KEY UPDATE level_code = VALUES(level_code), points = VALUES(points), total_spent = VALUES(total_spent);

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 2, 'STAFF', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_staff2'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 3, 'STAFF', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_staff3'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 3, 'STAFF', 'SUSPENDED' FROM `user` u WHERE u.username = 'demo_staff_leave'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 2, 'STORE_MANAGER', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_manager2'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 3, 'STORE_MANAGER', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_manager3'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 2, 'CAT_CARETAKER', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_cat2'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO dish_category (store_id, name, sort_order, status) VALUES
  (2, '主食', 1, 'ACTIVE'),
  (2, '甜品', 2, 'ACTIVE'),
  (2, '饮品', 3, 'ACTIVE'),
  (3, '主食', 1, 'ACTIVE'),
  (3, '甜品', 2, 'ACTIVE'),
  (3, '饮品', 3, 'ACTIVE')
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 2, c.id, '外滩夜猫咖喱饭', 42.00, 35, 'ON_SHELF', '外滩店招牌夜间主食'
FROM dish_category c WHERE c.store_id = 2 AND c.name = '主食'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 2 AND d.name = '外滩夜猫咖喱饭');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 2, c.id, '月光猫耳慕斯', 29.00, 42, 'ON_SHELF', '适合活动页展示的甜品'
FROM dish_category c WHERE c.store_id = 2 AND c.name = '甜品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 2 AND d.name = '月光猫耳慕斯');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 2, c.id, '外滩橘猫气泡水', 25.00, 58, 'ON_SHELF', '清爽气泡饮'
FROM dish_category c WHERE c.store_id = 2 AND c.name = '饮品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 2 AND d.name = '外滩橘猫气泡水');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 3, c.id, '西湖云朵蛋包饭', 39.00, 30, 'ON_SHELF', '亲子互动区推荐主食'
FROM dish_category c WHERE c.store_id = 3 AND c.name = '主食'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 3 AND d.name = '西湖云朵蛋包饭');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 3, c.id, '云朵猫爪奶冻', 26.00, 40, 'ON_SHELF', '轻甜口味演示甜品'
FROM dish_category c WHERE c.store_id = 3 AND c.name = '甜品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 3 AND d.name = '云朵猫爪奶冻');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 3, c.id, '西湖桂花拿铁', 28.00, 50, 'OFF_SHELF', '用于测试下架菜品不可点单'
FROM dish_category c WHERE c.store_id = 3 AND c.name = '饮品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 3 AND d.name = '西湖桂花拿铁');

INSERT INTO cat (store_id, name, breed, age, gender, personality, health_status, photo_url, description, status) VALUES
  (2, '摩卡', '缅因', 5, 'MALE', '稳重、喜欢被梳毛', '健康', '', '外滩店夜间互动明星猫', 'AVAILABLE'),
  (2, '奶盖', '金渐层', 2, 'FEMALE', '亲人、爱撒娇', '健康', '', '适合拍照互动', 'AVAILABLE'),
  (2, '可可', '暹罗', 4, 'MALE', '敏捷、好奇', '观察中', '', '用于测试健康观察状态', 'RESTING'),
  (3, '云朵', '布偶', 3, 'FEMALE', '温柔、适合亲子', '健康', '', '西湖店亲子互动推荐猫咪', 'AVAILABLE'),
  (3, '年糕', '英短', 6, 'MALE', '慢热、安静', '健康', '', '适合安静区陪伴', 'RESTING')
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
SELECT c.id, c.store_id, CURRENT_DATE, '10:00:00', '14:00:00', 'AVAILABLE', '系统测试上午互动时段'
FROM cat c
WHERE c.store_id IN (2, 3) AND c.status = 'AVAILABLE'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO cat_schedule (cat_id, store_id, schedule_date, start_time, end_time, status, remark)
SELECT c.id, c.store_id, CURRENT_DATE, '14:00:00', '18:00:00', 'RESTING', '系统测试下午休息时段'
FROM cat c
WHERE c.store_id IN (2, 3) AND c.status = 'RESTING'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO reservation_slot (store_id, table_id, slot_date, start_time, end_time, capacity, reserved_count, available_count, status)
SELECT t.store_id, t.id, CURRENT_DATE, '11:00:00', '12:30:00', t.capacity, 0, 1, 'AVAILABLE'
FROM dining_table t
WHERE t.store_id IN (2, 3) AND t.deleted = 0 AND t.status IN ('AVAILABLE', 'RESERVED')
  AND NOT EXISTS (
    SELECT 1 FROM reservation_slot rs
    WHERE rs.store_id = t.store_id AND rs.table_id = t.id AND rs.slot_date = CURRENT_DATE
      AND rs.start_time = '11:00:00' AND rs.end_time = '12:30:00'
  );

INSERT INTO reservation_slot (store_id, table_id, slot_date, start_time, end_time, capacity, reserved_count, available_count, status)
SELECT t.store_id, t.id, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '18:00:00', '19:30:00', t.capacity, 0, 1, 'AVAILABLE'
FROM dining_table t
WHERE t.store_id IN (2, 3) AND t.deleted = 0 AND t.status IN ('AVAILABLE', 'RESERVED')
  AND NOT EXISTS (
    SELECT 1 FROM reservation_slot rs
    WHERE rs.store_id = t.store_id AND rs.table_id = t.id AND rs.slot_date = DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY)
      AND rs.start_time = '18:00:00' AND rs.end_time = '19:30:00'
  );

INSERT INTO reservation (reservation_no, user_id, store_id, table_id, slot_id, party_size, status, contact_name, contact_phone, remark, checked_in_at, completed_at)
SELECT 'RTEST-S2-CHECKIN', u.id, s.id, t.id, rs.id, 2, 'CHECKED_IN', '甜品偏好会员', '13800001002', '靠窗，想和奶盖互动', NOW(), NULL
FROM `user` u
JOIN store s ON s.id = 2
JOIN dining_table t ON t.store_id = s.id AND t.table_no = 'A01'
JOIN reservation_slot rs ON rs.store_id = s.id AND rs.table_id = t.id AND rs.slot_date = CURRENT_DATE AND rs.start_time = '11:00:00'
WHERE u.username = 'demo_customer2'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark), checked_in_at = VALUES(checked_in_at), completed_at = VALUES(completed_at);

INSERT INTO reservation (reservation_no, user_id, store_id, table_id, slot_id, party_size, status, contact_name, contact_phone, remark, checked_in_at, completed_at)
SELECT 'RTEST-S3-RESERVED', u.id, s.id, t.id, rs.id, 4, 'RESERVED', '安静区会员', '13800001003', '需要安静区，少打扰', NULL, NULL
FROM `user` u
JOIN store s ON s.id = 3
JOIN dining_table t ON t.store_id = s.id AND t.table_no = 'B01'
JOIN reservation_slot rs ON rs.store_id = s.id AND rs.table_id = t.id AND rs.slot_date = CURRENT_DATE AND rs.start_time = '11:00:00'
WHERE u.username = 'demo_customer3'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark), checked_in_at = VALUES(checked_in_at), completed_at = VALUES(completed_at);

INSERT INTO reservation_cat (reservation_id, cat_id)
SELECT r.id, c.id
FROM reservation r
JOIN cat c ON c.store_id = r.store_id AND c.name IN ('奶盖', '云朵')
WHERE r.reservation_no IN ('RTEST-S2-CHECKIN', 'RTEST-S3-RESERVED')
ON DUPLICATE KEY UPDATE cat_id = VALUES(cat_id);

INSERT INTO food_order (order_no, user_id, store_id, reservation_id, table_id, total_amount, status, paid_at, completed_at, refund_status, remark)
SELECT 'OTEST-S2-PAID', r.user_id, r.store_id, r.id, r.table_id, 71.00, 'PAID', NOW(), NULL, 'NONE', '系统测试已支付待制作订单'
FROM reservation r
WHERE r.reservation_no = 'RTEST-S2-CHECKIN'
ON DUPLICATE KEY UPDATE status = VALUES(status), paid_at = VALUES(paid_at), completed_at = VALUES(completed_at), refund_status = VALUES(refund_status), remark = VALUES(remark);

INSERT INTO food_order (order_no, user_id, store_id, reservation_id, table_id, total_amount, status, paid_at, completed_at, refund_status, remark)
SELECT 'OTEST-S3-PREPARING', r.user_id, r.store_id, r.id, r.table_id, 65.00, 'PREPARING', NOW(), NULL, 'NONE', '系统测试制作中订单'
FROM reservation r
WHERE r.reservation_no = 'RTEST-S3-RESERVED'
ON DUPLICATE KEY UPDATE status = VALUES(status), paid_at = VALUES(paid_at), completed_at = VALUES(completed_at), refund_status = VALUES(refund_status), remark = VALUES(remark);

INSERT INTO food_order (order_no, user_id, store_id, reservation_id, table_id, total_amount, status, paid_at, completed_at, refund_status, remark)
SELECT 'OTEST-S2-COMPLETED', u.id, 2, NULL, t.id, 54.00, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'NONE', '系统测试已完成散客订单'
FROM `user` u
JOIN dining_table t ON t.store_id = 2 AND t.table_no = 'A02'
WHERE u.username = 'demo_customer2'
ON DUPLICATE KEY UPDATE status = VALUES(status), paid_at = VALUES(paid_at), completed_at = VALUES(completed_at), refund_status = VALUES(refund_status), remark = VALUES(remark);

INSERT INTO food_order_item (order_id, dish_id, dish_name, unit_price, quantity, subtotal)
SELECT o.id, d.id, d.name, d.price, 1, d.price
FROM food_order o
JOIN dish d ON d.store_id = o.store_id
WHERE o.order_no = 'OTEST-S2-PAID' AND d.name IN ('外滩夜猫咖喱饭', '月光猫耳慕斯')
  AND NOT EXISTS (SELECT 1 FROM food_order_item i WHERE i.order_id = o.id AND i.dish_id = d.id);

INSERT INTO food_order_item (order_id, dish_id, dish_name, unit_price, quantity, subtotal)
SELECT o.id, d.id, d.name, d.price, 1, d.price
FROM food_order o
JOIN dish d ON d.store_id = o.store_id
WHERE o.order_no = 'OTEST-S3-PREPARING' AND d.name IN ('西湖云朵蛋包饭', '云朵猫爪奶冻')
  AND NOT EXISTS (SELECT 1 FROM food_order_item i WHERE i.order_id = o.id AND i.dish_id = d.id);

INSERT INTO food_order_item (order_id, dish_id, dish_name, unit_price, quantity, subtotal)
SELECT o.id, d.id, d.name, d.price, 1, d.price
FROM food_order o
JOIN dish d ON d.store_id = o.store_id
WHERE o.order_no = 'OTEST-S2-COMPLETED' AND d.name IN ('月光猫耳慕斯', '外滩橘猫气泡水')
  AND NOT EXISTS (SELECT 1 FROM food_order_item i WHERE i.order_id = o.id AND i.dish_id = d.id);

INSERT INTO payment_record (payment_no, order_id, idempotency_key, amount, channel, status, paid_at)
SELECT CONCAT('P-', o.order_no), o.id, CONCAT('seed-', o.order_no), o.total_amount, 'SANDBOX', 'SUCCESS', o.paid_at
FROM food_order o
WHERE o.order_no IN ('OTEST-S2-PAID', 'OTEST-S3-PREPARING', 'OTEST-S2-COMPLETED')
ON DUPLICATE KEY UPDATE amount = VALUES(amount), status = VALUES(status), paid_at = VALUES(paid_at);

INSERT INTO refund_request (refund_no, user_id, order_id, payment_id, amount, reason, status, reviewed_by, reviewed_at, review_remark)
SELECT 'RFTEST-S2-APPLIED', o.user_id, o.id, p.id, 20.00, '测试退款申请：活动优惠未生效', 'APPLIED', NULL, NULL, NULL
FROM food_order o
LEFT JOIN payment_record p ON p.order_id = o.id
WHERE o.order_no = 'OTEST-S2-PAID'
ON DUPLICATE KEY UPDATE amount = VALUES(amount), reason = VALUES(reason), status = VALUES(status), reviewed_by = VALUES(reviewed_by), reviewed_at = VALUES(reviewed_at), review_remark = VALUES(review_remark);

INSERT INTO review (user_id, store_id, order_id, reservation_id, cat_id, rating, content, status)
SELECT o.user_id, o.store_id, o.id, o.reservation_id, c.id, 5, '猫咪很亲人，甜品也适合拍照。', 'VISIBLE'
FROM food_order o
LEFT JOIN cat c ON c.store_id = o.store_id AND c.name = '奶盖'
WHERE o.order_no = 'OTEST-S2-COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM review r WHERE r.order_id = o.id);

INSERT INTO points_transaction (user_id, member_account_id, order_id, type, points, balance_after, description)
SELECT o.user_id, m.id, o.id, 'EARN', 54, m.points + 54, '系统测试订单完成获得积分'
FROM food_order o
JOIN member_account m ON m.user_id = o.user_id
WHERE o.order_no = 'OTEST-S2-COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM points_transaction pt WHERE pt.order_id = o.id AND pt.type = 'EARN');

INSERT INTO user_preference (user_id, preference_type, preference_value)
SELECT u.id, 'ACTIVITY', 'ENTERTAINMENT'
FROM `user` u
WHERE u.username = 'demo_customer2'
ON DUPLICATE KEY UPDATE preference_value = VALUES(preference_value);

INSERT INTO user_preference (user_id, preference_type, preference_value)
SELECT u.id, 'AREA', '安静用餐区'
FROM `user` u
WHERE u.username = 'demo_customer3'
ON DUPLICATE KEY UPDATE preference_value = VALUES(preference_value);

INSERT INTO promotion_activity (title, type, description, cover_url, start_at, end_at, status, created_by) VALUES
  ('外滩夜猫营业季', 'PROMOTION', '外滩店夜间堂食满 80 减 12。', '', NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), 'PUBLISHED', NULL),
  ('西湖亲子猫咪课堂', 'ENTERTAINMENT', '西湖店周末亲子猫咪护理体验课。', '', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'PUBLISHED', NULL),
  ('静安试营业招募', 'NOTICE', '静安店试营业前体验官招募。', '', NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 'DRAFT', NULL)
ON DUPLICATE KEY UPDATE type = VALUES(type), description = VALUES(description), start_at = VALUES(start_at), end_at = VALUES(end_at), status = VALUES(status), deleted = 0;

INSERT INTO activity_store (activity_id, store_id, accept_status, handled_by, handled_at, handle_remark)
SELECT a.id, 2, 'ACCEPTED', u.id, NOW(), '外滩店接受夜猫营业季活动'
FROM promotion_activity a
LEFT JOIN `user` u ON u.username = 'demo_manager2'
WHERE a.title = '外滩夜猫营业季'
ON DUPLICATE KEY UPDATE accept_status = VALUES(accept_status), handled_by = VALUES(handled_by), handled_at = VALUES(handled_at), handle_remark = VALUES(handle_remark);

INSERT INTO activity_store (activity_id, store_id, accept_status, handled_by, handled_at, handle_remark)
SELECT a.id, 3, 'PENDING', NULL, NULL, '等待西湖店店长确认'
FROM promotion_activity a
WHERE a.title = '西湖亲子猫咪课堂'
ON DUPLICATE KEY UPDATE accept_status = VALUES(accept_status), handled_by = VALUES(handled_by), handled_at = VALUES(handled_at), handle_remark = VALUES(handle_remark);

INSERT INTO activity_store (activity_id, store_id, accept_status, handled_by, handled_at, handle_remark)
SELECT a.id, 4, 'PENDING', NULL, NULL, '试营业门店暂未接受'
FROM promotion_activity a
WHERE a.title = '静安试营业招募'
ON DUPLICATE KEY UPDATE accept_status = VALUES(accept_status), handled_by = VALUES(handled_by), handled_at = VALUES(handled_at), handle_remark = VALUES(handle_remark);

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 2, u.id, 'STAFF', CURRENT_DATE, '09:30:00', '17:30:00', 'SCHEDULED', '外滩店早班'
FROM `user` u WHERE u.username = 'demo_staff2'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 3, u.id, 'STAFF', CURRENT_DATE, '10:00:00', '18:00:00', 'SCHEDULED', '西湖店白班'
FROM `user` u WHERE u.username = 'demo_staff3'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 3, u.id, 'STAFF', CURRENT_DATE, '14:00:00', '22:00:00', 'ON_LEAVE', '测试请假班次'
FROM `user` u WHERE u.username = 'demo_staff_leave'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO staff_leave_request (store_id, user_id, leave_type, start_date, end_date, reason, status, approved_by, approved_at)
SELECT 3, staff.id, 'PERSONAL', CURRENT_DATE, CURRENT_DATE, '系统测试：店长给店员放假', 'APPROVED', manager.id, NOW()
FROM `user` staff
JOIN `user` manager ON manager.username = 'demo_manager3'
WHERE staff.username = 'demo_staff_leave';

INSERT INTO dish_price_history (dish_id, store_id, old_price, new_price, changed_by, reason)
SELECT d.id, d.store_id, 24.00, d.price, u.id, '系统测试：店长调整甜品价格'
FROM dish d
LEFT JOIN `user` u ON u.username = 'demo_manager2'
WHERE d.store_id = 2 AND d.name = '月光猫耳慕斯'
  AND NOT EXISTS (SELECT 1 FROM dish_price_history h WHERE h.dish_id = d.id AND h.new_price = d.price);
