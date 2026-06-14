-- Retheme demo stores to Beijing and add richer Beijing demo data.
-- Keep this file free of real credentials and production-only data.

INSERT INTO store (id, name, city, business_area, address, latitude, longitude, phone, opening_time, closing_time, status, description, cover_url, area_square_meter) VALUES
  (1, 'NekoCafé 北京朝阳旗舰店', '北京', '国贸商圈', '北京市朝阳区建国门外大街猫爪巷 12 号', 39.9087000, 116.4570000, '010-10000001', '09:00:00', '22:00:00', 'OPEN', '面向北京核心商圈的旗舰演示门店，适合预约、点单和猫咪互动流程展示。', '', 210.00),
  (2, 'NekoCafé 北京三里屯夜猫店', '北京', '三里屯商圈', '北京市朝阳区三里屯绒球街 6 号', 39.9349000, 116.4541000, '010-10000002', '10:00:00', '23:00:00', 'OPEN', '适合夜间活动和社交聚会的北京演示门店。', '', 220.00),
  (3, 'NekoCafé 北京中关村亲子店', '北京', '中关村商圈', '北京市海淀区中关村猫爪路 18 号', 39.9839000, 116.3163000, '010-10000003', '09:30:00', '22:00:00', 'OPEN', '适合亲子互动、周末活动和安静陪伴的北京演示门店。', '', 180.00),
  (4, 'NekoCafé 北京望京试营业店', '北京', '望京商圈', '北京市朝阳区望京小鱼干路 9 号', 39.9968000, 116.4815000, '010-10000004', '11:00:00', '21:00:00', 'PREPARING', '用于测试筹备、试营业和不可预约状态的北京门店。', '', 150.00)
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
  (1, 'C01', 4, '国贸观景区', 'AVAILABLE'),
  (1, 'D01', 6, '团体聚会区', 'AVAILABLE'),
  (2, 'D01', 4, '夜猫社交区', 'AVAILABLE'),
  (2, 'E01', 6, '露台活动区', 'AVAILABLE'),
  (3, 'D01', 4, '亲子手作区', 'AVAILABLE'),
  (3, 'E01', 2, '安静自习区', 'AVAILABLE'),
  (4, 'B01', 4, '试营业体验区', 'DISABLED')
ON DUPLICATE KEY UPDATE
  capacity = VALUES(capacity),
  area = VALUES(area),
  status = VALUES(status),
  deleted = 0;

INSERT INTO `user` (username, password_hash, nickname, phone, email, status) VALUES
  ('demo_customer_bj_1', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '北京甜品会员', NULL, 'demo_customer_bj_1@nekocafe.local', 'ACTIVE'),
  ('demo_customer_bj_2', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '北京亲子会员', NULL, 'demo_customer_bj_2@nekocafe.local', 'ACTIVE'),
  ('demo_staff_bj_1', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '朝阳前台铃铛', NULL, 'demo_staff_bj_1@nekocafe.local', 'ACTIVE'),
  ('demo_staff_bj_2', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '中关村咖啡师', NULL, 'demo_staff_bj_2@nekocafe.local', 'ACTIVE'),
  ('demo_staff_leave_bj', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '北京请假店员', NULL, 'demo_staff_leave_bj@nekocafe.local', 'ACTIVE'),
  ('demo_manager_bj_1', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '朝阳门店掌柜', NULL, 'demo_manager_bj_1@nekocafe.local', 'ACTIVE'),
  ('demo_manager_bj_2', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '中关村门店掌柜', NULL, 'demo_manager_bj_2@nekocafe.local', 'ACTIVE'),
  ('demo_cat_bj_1', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '北京猫咪管家', NULL, 'demo_cat_bj_1@nekocafe.local', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  email = VALUES(email),
  status = VALUES(status),
  deleted = 0;

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'CUSTOMER'
WHERE u.username IN ('demo_customer_bj_1', 'demo_customer_bj_2')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'STAFF'
WHERE u.username IN ('demo_staff_bj_1', 'demo_staff_bj_2', 'demo_staff_leave_bj')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'STORE_MANAGER'
WHERE u.username IN ('demo_manager_bj_1', 'demo_manager_bj_2')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'CAT_CARETAKER'
WHERE u.username = 'demo_cat_bj_1'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO member_account (user_id, level_code, points, total_spent)
SELECT u.id, 'GOLD', 368, 688.00 FROM `user` u
WHERE u.username = 'demo_customer_bj_1'
ON DUPLICATE KEY UPDATE level_code = VALUES(level_code), points = VALUES(points), total_spent = VALUES(total_spent);

INSERT INTO member_account (user_id, level_code, points, total_spent)
SELECT u.id, 'SILVER', 156, 326.00 FROM `user` u
WHERE u.username = 'demo_customer_bj_2'
ON DUPLICATE KEY UPDATE level_code = VALUES(level_code), points = VALUES(points), total_spent = VALUES(total_spent);

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 1, 'STAFF', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_staff_bj_1'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 3, 'STAFF', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_staff_bj_2'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 3, 'STAFF', 'SUSPENDED' FROM `user` u WHERE u.username = 'demo_staff_leave_bj'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 1, 'STORE_MANAGER', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_manager_bj_1'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 3, 'STORE_MANAGER', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_manager_bj_2'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO user_store_role (user_id, store_id, role_code, status)
SELECT u.id, 1, 'CAT_CARETAKER', 'ACTIVE' FROM `user` u WHERE u.username = 'demo_cat_bj_1'
ON DUPLICATE KEY UPDATE status = VALUES(status), dismissed_by = NULL, dismissed_at = NULL, dismiss_reason = NULL;

INSERT INTO dish_category (store_id, name, sort_order, status) VALUES
  (1, '北京限定', 4, 'ACTIVE'),
  (2, '北京限定', 4, 'ACTIVE'),
  (3, '北京限定', 4, 'ACTIVE'),
  (4, '试营业菜单', 1, 'INACTIVE')
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 1, c.id, '国贸猫爪烤鸭饭', 46.00, 45, 'ON_SHELF', '北京朝阳旗舰店限定主食'
FROM dish_category c WHERE c.store_id = 1 AND c.name = '北京限定'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 1 AND d.name = '国贸猫爪烤鸭饭');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 1, c.id, '胡同猫耳拿铁', 28.00, 60, 'ON_SHELF', '带有北京胡同主题拉花的饮品'
FROM dish_category c WHERE c.store_id = 1 AND c.name = '北京限定'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 1 AND d.name = '胡同猫耳拿铁');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 2, c.id, '三里屯夜猫拼盘', 58.00, 32, 'ON_SHELF', '夜间营业推荐分享餐'
FROM dish_category c WHERE c.store_id = 2 AND c.name = '北京限定'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 2 AND d.name = '三里屯夜猫拼盘');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 2, c.id, '工体橘猫气泡水', 26.00, 55, 'ON_SHELF', '三里屯店清爽饮品'
FROM dish_category c WHERE c.store_id = 2 AND c.name = '北京限定'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 2 AND d.name = '工体橘猫气泡水');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 3, c.id, '中关村云朵蛋包饭', 42.00, 38, 'ON_SHELF', '亲子店推荐主食'
FROM dish_category c WHERE c.store_id = 3 AND c.name = '北京限定'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 3 AND d.name = '中关村云朵蛋包饭');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 3, c.id, '海淀猫爪奶冻', 24.00, 52, 'ON_SHELF', '适合亲子互动后的轻甜甜品'
FROM dish_category c WHERE c.store_id = 3 AND c.name = '北京限定'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 3 AND d.name = '海淀猫爪奶冻');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 4, c.id, '望京试营业咖啡', 22.00, 20, 'OFF_SHELF', '试营业门店预置菜单'
FROM dish_category c WHERE c.store_id = 4 AND c.name = '试营业菜单'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 4 AND d.name = '望京试营业咖啡');

INSERT INTO cat (store_id, name, breed, age, gender, personality, health_status, photo_url, description, status) VALUES
  (1, '豆汁', '英短', 3, 'MALE', '亲人、喜欢晒太阳', '健康', '', '朝阳旗舰店互动猫咪', 'AVAILABLE'),
  (1, '糖葫芦', '布偶', 2, 'FEMALE', '活泼、适合拍照', '健康', '', '北京主题甜品区明星猫', 'AVAILABLE'),
  (2, '胡同', '橘猫', 4, 'MALE', '慢热、爱观察', '健康', '', '三里屯夜猫店陪伴猫咪', 'AVAILABLE'),
  (3, '圆明园', '金渐层', 3, 'FEMALE', '温柔、适合亲子互动', '健康', '', '中关村亲子店推荐猫咪', 'AVAILABLE'),
  (3, '书卷', '暹罗', 5, 'MALE', '安静、好奇', '观察中', '', '用于展示休息观察状态', 'RESTING')
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
SELECT c.id, c.store_id, CURRENT_DATE, '10:00:00', '14:00:00', 'AVAILABLE', '北京门店上午互动时段'
FROM cat c
WHERE c.name IN ('豆汁', '糖葫芦', '胡同', '圆明园')
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO cat_schedule (cat_id, store_id, schedule_date, start_time, end_time, status, remark)
SELECT c.id, c.store_id, CURRENT_DATE, '14:00:00', '18:00:00', 'RESTING', '北京门店下午休息观察'
FROM cat c
WHERE c.name = '书卷'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);

INSERT INTO reservation_slot (store_id, table_id, slot_date, start_time, end_time, capacity, reserved_count, available_count, status)
SELECT t.store_id, t.id, CURRENT_DATE, '12:00:00', '13:30:00', t.capacity, 0, 1, 'AVAILABLE'
FROM dining_table t
WHERE t.store_id IN (1, 2, 3) AND t.deleted = 0 AND t.status IN ('AVAILABLE', 'RESERVED')
  AND NOT EXISTS (
    SELECT 1 FROM reservation_slot rs
    WHERE rs.store_id = t.store_id AND rs.table_id = t.id AND rs.slot_date = CURRENT_DATE
      AND rs.start_time = '12:00:00' AND rs.end_time = '13:30:00'
  );

INSERT INTO reservation_slot (store_id, table_id, slot_date, start_time, end_time, capacity, reserved_count, available_count, status)
SELECT t.store_id, t.id, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '18:30:00', '20:00:00', t.capacity, 0, 1, 'AVAILABLE'
FROM dining_table t
WHERE t.store_id IN (1, 2, 3) AND t.deleted = 0 AND t.status IN ('AVAILABLE', 'RESERVED')
  AND NOT EXISTS (
    SELECT 1 FROM reservation_slot rs
    WHERE rs.store_id = t.store_id AND rs.table_id = t.id AND rs.slot_date = DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY)
      AND rs.start_time = '18:30:00' AND rs.end_time = '20:00:00'
  );

INSERT INTO reservation (reservation_no, user_id, store_id, table_id, slot_id, party_size, status, contact_name, contact_phone, remark, checked_in_at, completed_at)
SELECT 'R-BJ-1-CHECKIN', u.id, s.id, t.id, rs.id, 2, 'CHECKED_IN', '北京甜品会员', '13800002001', '想体验糖葫芦互动', NOW(), NULL
FROM `user` u
JOIN store s ON s.id = 1
JOIN dining_table t ON t.store_id = s.id AND t.table_no = 'C01'
JOIN reservation_slot rs ON rs.store_id = s.id AND rs.table_id = t.id AND rs.slot_date = CURRENT_DATE AND rs.start_time = '12:00:00'
WHERE u.username = 'demo_customer_bj_1'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark), checked_in_at = VALUES(checked_in_at), completed_at = VALUES(completed_at);

INSERT INTO reservation (reservation_no, user_id, store_id, table_id, slot_id, party_size, status, contact_name, contact_phone, remark, checked_in_at, completed_at)
SELECT 'R-BJ-2-RESERVED', u.id, s.id, t.id, rs.id, 4, 'RESERVED', '北京亲子会员', '13800002002', '亲子活动后需要安静座位', NULL, NULL
FROM `user` u
JOIN store s ON s.id = 3
JOIN dining_table t ON t.store_id = s.id AND t.table_no = 'D01'
JOIN reservation_slot rs ON rs.store_id = s.id AND rs.table_id = t.id AND rs.slot_date = DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY) AND rs.start_time = '18:30:00'
WHERE u.username = 'demo_customer_bj_2'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark), checked_in_at = VALUES(checked_in_at), completed_at = VALUES(completed_at);

INSERT INTO reservation_cat (reservation_id, cat_id)
SELECT r.id, c.id
FROM reservation r
JOIN cat c ON c.store_id = r.store_id AND c.name IN ('糖葫芦', '圆明园')
WHERE r.reservation_no IN ('R-BJ-1-CHECKIN', 'R-BJ-2-RESERVED')
ON DUPLICATE KEY UPDATE cat_id = VALUES(cat_id);

INSERT INTO food_order (order_no, user_id, store_id, reservation_id, table_id, total_amount, status, paid_at, completed_at, refund_status, handler_id, remark)
SELECT 'O-BJ-1-PAID', r.user_id, r.store_id, r.id, r.table_id, 74.00, 'PAID', NOW(), NULL, 'NONE', staff.id, '北京演示已支付待制作订单'
FROM reservation r
LEFT JOIN `user` staff ON staff.username = 'demo_staff_bj_1'
WHERE r.reservation_no = 'R-BJ-1-CHECKIN'
ON DUPLICATE KEY UPDATE status = VALUES(status), paid_at = VALUES(paid_at), completed_at = VALUES(completed_at), refund_status = VALUES(refund_status), handler_id = VALUES(handler_id), remark = VALUES(remark);

INSERT INTO food_order (order_no, user_id, store_id, reservation_id, table_id, total_amount, status, paid_at, completed_at, refund_status, handler_id, remark)
SELECT 'O-BJ-2-PREPARING', r.user_id, r.store_id, r.id, r.table_id, 66.00, 'PREPARING', NOW(), NULL, 'NONE', staff.id, '北京演示制作中亲子订单'
FROM reservation r
LEFT JOIN `user` staff ON staff.username = 'demo_staff_bj_2'
WHERE r.reservation_no = 'R-BJ-2-RESERVED'
ON DUPLICATE KEY UPDATE status = VALUES(status), paid_at = VALUES(paid_at), completed_at = VALUES(completed_at), refund_status = VALUES(refund_status), handler_id = VALUES(handler_id), remark = VALUES(remark);

INSERT INTO food_order (order_no, user_id, store_id, reservation_id, table_id, total_amount, status, paid_at, completed_at, refund_status, handler_id, remark)
SELECT 'O-BJ-3-COMPLETED', u.id, 2, NULL, t.id, 84.00, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'REFUNDING', staff.id, '北京演示已完成散客订单，含退款申请'
FROM `user` u
JOIN dining_table t ON t.store_id = 2 AND t.table_no = 'D01'
LEFT JOIN `user` staff ON staff.username = 'demo_staff2'
WHERE u.username = 'demo_customer_bj_1'
ON DUPLICATE KEY UPDATE status = VALUES(status), paid_at = VALUES(paid_at), completed_at = VALUES(completed_at), refund_status = VALUES(refund_status), handler_id = VALUES(handler_id), remark = VALUES(remark);

INSERT INTO food_order_item (order_id, dish_id, dish_name, unit_price, quantity, subtotal)
SELECT o.id, d.id, d.name, d.price, 1, d.price
FROM food_order o
JOIN dish d ON d.store_id = o.store_id
WHERE o.order_no = 'O-BJ-1-PAID' AND d.name IN ('国贸猫爪烤鸭饭', '胡同猫耳拿铁')
  AND NOT EXISTS (SELECT 1 FROM food_order_item i WHERE i.order_id = o.id AND i.dish_id = d.id);

INSERT INTO food_order_item (order_id, dish_id, dish_name, unit_price, quantity, subtotal)
SELECT o.id, d.id, d.name, d.price, 1, d.price
FROM food_order o
JOIN dish d ON d.store_id = o.store_id
WHERE o.order_no = 'O-BJ-2-PREPARING' AND d.name IN ('中关村云朵蛋包饭', '海淀猫爪奶冻')
  AND NOT EXISTS (SELECT 1 FROM food_order_item i WHERE i.order_id = o.id AND i.dish_id = d.id);

INSERT INTO food_order_item (order_id, dish_id, dish_name, unit_price, quantity, subtotal)
SELECT o.id, d.id, d.name, d.price, 1, d.price
FROM food_order o
JOIN dish d ON d.store_id = o.store_id
WHERE o.order_no = 'O-BJ-3-COMPLETED' AND d.name IN ('三里屯夜猫拼盘', '工体橘猫气泡水')
  AND NOT EXISTS (SELECT 1 FROM food_order_item i WHERE i.order_id = o.id AND i.dish_id = d.id);

INSERT INTO payment_record (payment_no, order_id, idempotency_key, amount, channel, status, paid_at)
SELECT CONCAT('P-', o.order_no), o.id, CONCAT('seed-', o.order_no), o.total_amount, 'SANDBOX', 'SUCCESS', o.paid_at
FROM food_order o
WHERE o.order_no IN ('O-BJ-1-PAID', 'O-BJ-2-PREPARING', 'O-BJ-3-COMPLETED')
ON DUPLICATE KEY UPDATE amount = VALUES(amount), status = VALUES(status), paid_at = VALUES(paid_at);

INSERT INTO refund_request (refund_no, user_id, order_id, payment_id, amount, reason, status, reviewed_by, reviewed_at, review_remark)
SELECT 'RF-BJ-1-APPLIED', o.user_id, o.id, p.id, 26.00, '北京演示退款申请：饮品口味调整', 'APPLIED', NULL, NULL, NULL
FROM food_order o
LEFT JOIN payment_record p ON p.order_id = o.id
WHERE o.order_no = 'O-BJ-3-COMPLETED'
ON DUPLICATE KEY UPDATE amount = VALUES(amount), reason = VALUES(reason), status = VALUES(status), reviewed_by = VALUES(reviewed_by), reviewed_at = VALUES(reviewed_at), review_remark = VALUES(review_remark);

INSERT INTO review (user_id, store_id, order_id, reservation_id, cat_id, rating, content, status)
SELECT o.user_id, o.store_id, o.id, o.reservation_id, c.id, 5, '三里屯夜猫店氛围很好，猫咪互动也很放松。', 'VISIBLE'
FROM food_order o
LEFT JOIN cat c ON c.store_id = o.store_id AND c.name = '胡同'
WHERE o.order_no = 'O-BJ-3-COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM review r WHERE r.order_id = o.id);

INSERT INTO points_transaction (user_id, member_account_id, order_id, type, points, balance_after, description)
SELECT o.user_id, m.id, o.id, 'EARN', 84, m.points + 84, '北京演示订单完成获得积分'
FROM food_order o
JOIN member_account m ON m.user_id = o.user_id
WHERE o.order_no = 'O-BJ-3-COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM points_transaction pt WHERE pt.order_id = o.id AND pt.type = 'EARN');

INSERT INTO user_preference (user_id, preference_type, preference_value)
SELECT u.id, 'CITY', '北京'
FROM `user` u
WHERE u.username IN ('demo_customer_bj_1', 'demo_customer_bj_2')
ON DUPLICATE KEY UPDATE preference_value = VALUES(preference_value);

INSERT INTO user_preference (user_id, preference_type, preference_value)
SELECT u.id, 'AREA', '亲子互动区'
FROM `user` u
WHERE u.username = 'demo_customer_bj_2'
ON DUPLICATE KEY UPDATE preference_value = VALUES(preference_value);

INSERT INTO promotion_activity (title, type, description, cover_url, start_at, end_at, status, created_by) VALUES
  ('北京猫爪城市打卡季', 'PROMOTION', '北京门店限定饮品与城市打卡活动。', '', NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), 'PUBLISHED', NULL),
  ('中关村亲子猫咪课堂', 'ENTERTAINMENT', '中关村店周末亲子猫咪护理体验课。', '', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'PUBLISHED', NULL),
  ('望京试营业体验官招募', 'NOTICE', '望京店试营业前体验官招募。', '', NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 'DRAFT', NULL)
ON DUPLICATE KEY UPDATE type = VALUES(type), description = VALUES(description), start_at = VALUES(start_at), end_at = VALUES(end_at), status = VALUES(status), deleted = 0;

INSERT INTO activity_store (activity_id, store_id, accept_status, handled_by, handled_at, handle_remark)
SELECT a.id, 1, 'ACCEPTED', u.id, NOW(), '朝阳旗舰店接受北京城市打卡活动'
FROM promotion_activity a
LEFT JOIN `user` u ON u.username = 'demo_manager_bj_1'
WHERE a.title = '北京猫爪城市打卡季'
ON DUPLICATE KEY UPDATE accept_status = VALUES(accept_status), handled_by = VALUES(handled_by), handled_at = VALUES(handled_at), handle_remark = VALUES(handle_remark);

INSERT INTO activity_store (activity_id, store_id, accept_status, handled_by, handled_at, handle_remark)
SELECT a.id, 3, 'PENDING', NULL, NULL, '等待中关村店店长确认'
FROM promotion_activity a
WHERE a.title = '中关村亲子猫咪课堂'
ON DUPLICATE KEY UPDATE accept_status = VALUES(accept_status), handled_by = VALUES(handled_by), handled_at = VALUES(handled_at), handle_remark = VALUES(handle_remark);

INSERT INTO activity_store (activity_id, store_id, accept_status, handled_by, handled_at, handle_remark)
SELECT a.id, 4, 'PENDING', NULL, NULL, '试营业门店暂未接受'
FROM promotion_activity a
WHERE a.title = '望京试营业体验官招募'
ON DUPLICATE KEY UPDATE accept_status = VALUES(accept_status), handled_by = VALUES(handled_by), handled_at = VALUES(handled_at), handle_remark = VALUES(handle_remark);

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 1, u.id, 'STAFF', CURRENT_DATE, '09:00:00', '17:00:00', 'SCHEDULED', '朝阳旗舰店早班'
FROM `user` u WHERE u.username = 'demo_staff_bj_1'
ON DUPLICATE KEY UPDATE store_id = VALUES(store_id), role_code = VALUES(role_code), status = VALUES(status), remark = VALUES(remark);

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 3, u.id, 'STAFF', CURRENT_DATE, '10:00:00', '18:00:00', 'SCHEDULED', '中关村亲子店白班'
FROM `user` u WHERE u.username = 'demo_staff_bj_2'
ON DUPLICATE KEY UPDATE store_id = VALUES(store_id), role_code = VALUES(role_code), status = VALUES(status), remark = VALUES(remark);

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 3, u.id, 'STAFF', CURRENT_DATE, '14:00:00', '22:00:00', 'ON_LEAVE', '北京演示请假班次'
FROM `user` u WHERE u.username = 'demo_staff_leave_bj'
ON DUPLICATE KEY UPDATE store_id = VALUES(store_id), role_code = VALUES(role_code), status = VALUES(status), remark = VALUES(remark);

INSERT INTO staff_shift (store_id, user_id, role_code, shift_date, start_time, end_time, status, remark)
SELECT 1, u.id, 'CAT_CARETAKER', CURRENT_DATE, '10:00:00', '18:00:00', 'SCHEDULED', '北京猫咪管家白班'
FROM `user` u WHERE u.username = 'demo_cat_bj_1'
ON DUPLICATE KEY UPDATE store_id = VALUES(store_id), role_code = VALUES(role_code), status = VALUES(status), remark = VALUES(remark);

INSERT INTO staff_leave_request (store_id, user_id, leave_type, start_date, end_date, reason, status, approved_by, approved_at)
SELECT 3, staff.id, 'PERSONAL', CURRENT_DATE, CURRENT_DATE, '北京演示：店长审批店员请假', 'APPROVED', manager.id, NOW()
FROM `user` staff
JOIN `user` manager ON manager.username = 'demo_manager_bj_2'
WHERE staff.username = 'demo_staff_leave_bj'
  AND NOT EXISTS (
    SELECT 1 FROM staff_leave_request lr
    WHERE lr.store_id = 3 AND lr.user_id = staff.id AND lr.start_date = CURRENT_DATE AND lr.end_date = CURRENT_DATE
      AND lr.reason = '北京演示：店长审批店员请假'
  );

INSERT INTO dish_price_history (dish_id, store_id, old_price, new_price, changed_by, reason)
SELECT d.id, d.store_id, 24.00, d.price, u.id, '北京演示：门店调整限定饮品价格'
FROM dish d
LEFT JOIN `user` u ON u.username = 'demo_manager_bj_1'
WHERE d.store_id = 1 AND d.name = '胡同猫耳拿铁'
  AND NOT EXISTS (SELECT 1 FROM dish_price_history h WHERE h.dish_id = d.id AND h.new_price = d.price);

INSERT INTO dashboard_stat (store_id, stat_date, reservation_count, order_count, revenue)
SELECT 1, CURRENT_DATE, 1, 1, 74.00
ON DUPLICATE KEY UPDATE reservation_count = VALUES(reservation_count), order_count = VALUES(order_count), revenue = VALUES(revenue);

INSERT INTO dashboard_stat (store_id, stat_date, reservation_count, order_count, revenue)
SELECT 2, CURRENT_DATE, 0, 1, 84.00
ON DUPLICATE KEY UPDATE reservation_count = VALUES(reservation_count), order_count = VALUES(order_count), revenue = VALUES(revenue);

INSERT INTO dashboard_stat (store_id, stat_date, reservation_count, order_count, revenue)
SELECT 3, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 1, 1, 66.00
ON DUPLICATE KEY UPDATE reservation_count = VALUES(reservation_count), order_count = VALUES(order_count), revenue = VALUES(revenue);
