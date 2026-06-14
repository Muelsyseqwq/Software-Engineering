-- Customer-flow demo data: menu richness and reservable slots.
-- Keep this file free of real credentials or production-only data.

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 1, id, '三文鱼猫爪饭团', 32.00, 40, 'ON_SHELF', '适合轻食预约的招牌主食'
FROM dish_category c
WHERE c.store_id = 1 AND c.name = '主食'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 1 AND d.name = '三文鱼猫爪饭团');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 1, id, '焦糖猫耳布丁', 24.00, 60, 'ON_SHELF', '焦糖脆面配柔滑布丁'
FROM dish_category c
WHERE c.store_id = 1 AND c.name = '甜品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 1 AND d.name = '焦糖猫耳布丁');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 1, id, '抹茶毛线球蛋糕', 28.00, 36, 'ON_SHELF', '抹茶奶油与松软蛋糕胚'
FROM dish_category c
WHERE c.store_id = 1 AND c.name = '甜品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 1 AND d.name = '抹茶毛线球蛋糕');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 1, id, '暖爪拿铁', 26.00, 80, 'ON_SHELF', '热拿铁与猫爪拉花'
FROM dish_category c
WHERE c.store_id = 1 AND c.name = '饮品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 1 AND d.name = '暖爪拿铁');

INSERT INTO dish (store_id, category_id, name, price, stock, status, description)
SELECT 1, id, '橘猫气泡水', 22.00, 70, 'ON_SHELF', '柑橘香气的清爽气泡饮'
FROM dish_category c
WHERE c.store_id = 1 AND c.name = '饮品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.store_id = 1 AND d.name = '橘猫气泡水');

INSERT INTO reservation_slot (store_id, table_id, slot_date, start_time, end_time, capacity, reserved_count, available_count, status)
SELECT
  t.store_id,
  t.id,
  DATE_ADD(CURRENT_DATE, INTERVAL days.day_offset DAY),
  slots.start_time,
  slots.end_time,
  t.capacity,
  0,
  1,
  'AVAILABLE'
FROM dining_table t
JOIN (
  SELECT 1 AS day_offset UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) days
JOIN (
  SELECT TIME '11:00:00' AS start_time, TIME '12:30:00' AS end_time UNION ALL
  SELECT TIME '13:00:00', TIME '14:30:00' UNION ALL
  SELECT TIME '18:00:00', TIME '19:30:00' UNION ALL
  SELECT TIME '20:00:00', TIME '21:30:00'
) slots
WHERE t.store_id = 1
  AND t.deleted = 0
  AND t.status = 'AVAILABLE'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_slot rs
    WHERE rs.store_id = t.store_id
      AND rs.table_id = t.id
      AND rs.slot_date = DATE_ADD(CURRENT_DATE, INTERVAL days.day_offset DAY)
      AND rs.start_time = slots.start_time
      AND rs.end_time = slots.end_time
  );
