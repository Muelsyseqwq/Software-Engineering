-- Normalize cat profile status values and backfill caretaker-maintained demo fields.

UPDATE cat
SET status = 'AVAILABLE'
WHERE status IS NULL OR status = '';

UPDATE cat SET status = 'AVAILABLE' WHERE status IN ('ACTIVE', '可互动', '可预约', '空闲');
UPDATE cat SET status = 'RESTING' WHERE status IN ('INACTIVE', 'OFFLINE', 'DISABLED', '休息中', '不可互动');
UPDATE cat SET status = 'ADOPTED' WHERE status = '已领养';

UPDATE cat
SET health_status = '健康'
WHERE health_status IS NULL OR health_status = '';

UPDATE cat SET health_status = '健康' WHERE health_status IN ('HEALTHY', '正常');
UPDATE cat SET health_status = '观察中' WHERE health_status IN ('OBSERVING', '观察');
UPDATE cat SET health_status = '治疗中' WHERE health_status IN ('TREATMENT', 'TREATING', '治疗');
UPDATE cat SET health_status = '恢复中' WHERE health_status IN ('RECOVERING', '恢复');

UPDATE cat
SET weight = COALESCE(weight, 4.20),
    interact = COALESCE(interact, '喜欢安静陪伴，适合轻柔抚摸'),
    vaccinium = COALESCE(vaccinium, '已完成年度疫苗')
WHERE store_id = 1 AND name = '拿铁';

UPDATE cat
SET weight = COALESCE(weight, 3.60),
    interact = COALESCE(interact, '活泼亲人，适合逗猫棒互动'),
    vaccinium = COALESCE(vaccinium, '已完成核心疫苗')
WHERE store_id = 1 AND name = '布丁';

UPDATE cat
SET weight = COALESCE(weight, 5.10),
    interact = COALESCE(interact, '偏爱午后休息，互动前先轻声安抚'),
    vaccinium = COALESCE(vaccinium, '已完成年度疫苗，定期驱虫')
WHERE store_id = 1 AND name = '团子';

UPDATE cat
SET weight = COALESCE(weight, 6.80),
    interact = COALESCE(interact, '喜欢被梳毛，适合低强度陪伴'),
    vaccinium = COALESCE(vaccinium, '已完成年度疫苗')
WHERE store_id = 2 AND name = '摩卡';

UPDATE cat
SET weight = COALESCE(weight, 3.90),
    interact = COALESCE(interact, '亲人爱撒娇，适合拍照互动'),
    vaccinium = COALESCE(vaccinium, '已完成核心疫苗')
WHERE store_id = 2 AND name = '奶盖';

UPDATE cat
SET weight = COALESCE(weight, 4.40),
    interact = COALESCE(interact, '好奇敏捷，观察期减少高强度互动'),
    vaccinium = COALESCE(vaccinium, '疫苗记录已复核，观察期内')
WHERE store_id = 2 AND name = '可可';

UPDATE cat
SET weight = COALESCE(weight, 4.80),
    interact = COALESCE(interact, '温柔稳定，适合亲子陪伴互动'),
    vaccinium = COALESCE(vaccinium, '已完成年度疫苗')
WHERE store_id = 3 AND name = '云朵';

UPDATE cat
SET weight = COALESCE(weight, 5.60),
    interact = COALESCE(interact, '慢热安静，适合短时陪伴'),
    vaccinium = COALESCE(vaccinium, '已完成核心疫苗，定期驱虫')
WHERE store_id = 3 AND name = '年糕';
