INSERT INTO role (code, name, description) VALUES
  ('CUSTOMER', '顾客', '顾客端用户'),
  ('STAFF', '店员', '门店一线工作人员'),
  ('STORE_MANAGER', '店长', '门店管理者'),
  ('HQ_OPERATOR', '总部运营', '跨门店运营人员'),
  ('CAT_CARETAKER', '猫咪管家', '猫咪档案和健康记录维护人员'),
  ('ADMIN', '系统管理员', '平台系统管理员')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO store (id, name, city, address, phone, opening_time, closing_time, status, description) VALUES
  (1, 'NekoCafé 春日旗舰店', '上海', '示例路 100 号', '021-00000000', '10:00:00', '22:00:00', 'OPEN', '课程设计演示门店')
ON DUPLICATE KEY UPDATE name = VALUES(name), city = VALUES(city), address = VALUES(address), status = VALUES(status);

INSERT INTO dining_table (store_id, table_no, capacity, area, status) VALUES
  (1, 'A01', 2, '猫咪互动区', 'AVAILABLE'),
  (1, 'A02', 4, '猫咪互动区', 'AVAILABLE'),
  (1, 'B01', 6, '安静用餐区', 'AVAILABLE')
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity), area = VALUES(area), status = VALUES(status);

INSERT INTO dish_category (store_id, name, sort_order, status) VALUES
  (1, '主食', 1, 'ACTIVE'),
  (1, '甜品', 2, 'ACTIVE'),
  (1, '饮品', 3, 'ACTIVE')
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 1, id, '猫爪咖喱饭', 38.00, 50, 'ON_SHELF', '招牌主食'
FROM dish_category WHERE store_id = 1 AND name = '主食'
ON DUPLICATE KEY UPDATE name = VALUES(name);
