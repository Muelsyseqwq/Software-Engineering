-- Add more Beijing open stores for customer discovery and nearby-store demos.
-- Coordinates use latitude, longitude order and include a Beijing Forestry University store.

INSERT INTO store (id, name, city, business_area, address, latitude, longitude, phone, opening_time, closing_time, status, description, cover_url, area_square_meter) VALUES
  (5, 'NekoCafé 北京林业大学店', '北京', '学院路商圈', '北京市海淀区清华东路 35 号北京林业大学东门附近', 40.0051000, 116.3442000, '010-10000005', '09:00:00', '22:30:00', 'OPEN', '面向北京林业大学与学院路学生群体的校园周边猫咖，适合自习、社团小聚和猫咪陪伴。', '', 175.00),
  (6, 'NekoCafé 北京五道口店', '北京', '五道口商圈', '北京市海淀区五道口成府路猫爪街 16 号', 39.9929000, 116.3377000, '010-10000006', '10:00:00', '23:00:00', 'OPEN', '五道口夜间社交与轻食甜品演示门店，适合学生聚会。', '', 190.00),
  (7, 'NekoCafé 北京奥森公园店', '北京', '奥林匹克森林公园商圈', '北京市朝阳区林萃路奥森猫步巷 8 号', 40.0156000, 116.3879000, '010-10000007', '09:30:00', '22:00:00', 'OPEN', '适合周末公园散步后顺路打卡的自然主题猫咖。', '', 205.00),
  (8, 'NekoCafé 北京西直门店', '北京', '西直门商圈', '北京市西城区西直门外猫铃路 21 号', 39.9405000, 116.3554000, '010-10000008', '09:30:00', '22:30:00', 'OPEN', '交通便利的换乘商圈猫咖，适合下班后预约与点单。', '', 185.00)
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
  (5, 'A01', 2, '窗边自习区', 'AVAILABLE'),
  (5, 'A02', 4, '校园社团区', 'AVAILABLE'),
  (5, 'B01', 6, '猫咪互动区', 'AVAILABLE'),
  (6, 'A01', 2, '夜猫吧台区', 'AVAILABLE'),
  (6, 'B01', 4, '朋友聚会区', 'AVAILABLE'),
  (6, 'C01', 6, '活动包间', 'AVAILABLE'),
  (7, 'A01', 2, '森林窗景区', 'AVAILABLE'),
  (7, 'B01', 4, '亲子互动区', 'AVAILABLE'),
  (8, 'A01', 2, '通勤快闪区', 'AVAILABLE'),
  (8, 'B01', 4, '安静用餐区', 'AVAILABLE')
ON DUPLICATE KEY UPDATE
  capacity = VALUES(capacity),
  area = VALUES(area),
  status = VALUES(status),
  deleted = 0;

INSERT INTO dish_category (store_id, name, sort_order, status) VALUES
  (5, '校园限定', 1, 'ACTIVE'),
  (5, '饮品', 2, 'ACTIVE'),
  (6, '夜猫轻食', 1, 'ACTIVE'),
  (6, '饮品', 2, 'ACTIVE'),
  (7, '自然主题', 1, 'ACTIVE'),
  (8, '通勤套餐', 1, 'ACTIVE')
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 5, c.id, '北林银杏猫爪饭', 39.00, 45, 'ON_SHELF', '北京林业大学店校园限定主食'
FROM dish_category c WHERE c.store_id = 5 AND c.name = '校园限定'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 5 AND d.name = '北林银杏猫爪饭');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 5, c.id, '清华东路拿铁', 26.00, 60, 'ON_SHELF', '适合自习搭配的轻咖啡饮品'
FROM dish_category c WHERE c.store_id = 5 AND c.name = '饮品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 5 AND d.name = '清华东路拿铁');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 6, c.id, '五道口夜猫三明治', 34.00, 40, 'ON_SHELF', '夜间营业推荐轻食'
FROM dish_category c WHERE c.store_id = 6 AND c.name = '夜猫轻食'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 6 AND d.name = '五道口夜猫三明治');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 6, c.id, '成府路橘猫气泡水', 24.00, 58, 'ON_SHELF', '五道口店清爽饮品'
FROM dish_category c WHERE c.store_id = 6 AND c.name = '饮品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 6 AND d.name = '成府路橘猫气泡水');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 7, c.id, '奥森森林猫爪蛋糕', 32.00, 36, 'ON_SHELF', '自然主题甜品'
FROM dish_category c WHERE c.store_id = 7 AND c.name = '自然主题'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 7 AND d.name = '奥森森林猫爪蛋糕');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 8, c.id, '西直门通勤咖啡套餐', 36.00, 42, 'ON_SHELF', '适合下班后快速点单的套餐'
FROM dish_category c WHERE c.store_id = 8 AND c.name = '通勤套餐'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 8 AND d.name = '西直门通勤咖啡套餐');

INSERT INTO cat (store_id, name, breed, age, gender, personality, health_status, photo_url, description, status) VALUES
  (5, '银杏', '英短', 3, 'FEMALE', '安静、适合陪伴自习', '健康', '', '北京林业大学店校园陪伴猫咪', 'AVAILABLE'),
  (5, '松果', '橘猫', 4, 'MALE', '亲人、喜欢互动', '健康', '', '适合社团小聚互动', 'AVAILABLE'),
  (6, '夜航', '暹罗', 2, 'MALE', '活泼、好奇', '健康', '', '五道口夜猫店互动猫咪', 'AVAILABLE'),
  (7, '森林', '布偶', 3, 'FEMALE', '温柔、适合亲子', '健康', '', '奥森公园店自然主题猫咪', 'AVAILABLE'),
  (8, '站台', '金渐层', 5, 'MALE', '稳重、适合安静陪伴', '健康', '', '西直门店通勤陪伴猫咪', 'AVAILABLE')
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
SELECT c.id, c.store_id, CURRENT_DATE, '10:00:00', '14:00:00', 'AVAILABLE', '新增北京门店上午互动时段'
FROM cat c
WHERE c.store_id IN (5, 6, 7, 8) AND c.status = 'AVAILABLE'
ON DUPLICATE KEY UPDATE status = VALUES(status), remark = VALUES(remark);
