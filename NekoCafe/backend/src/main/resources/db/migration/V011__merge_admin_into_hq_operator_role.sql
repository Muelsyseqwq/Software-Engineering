-- Merge the visible headquarters operations experience while keeping ADMIN as a compatibility role.
-- Do not delete ADMIN or demo_admin; historical data and issued tokens may still reference them.

UPDATE role
SET name = '总部运营',
    description = '跨门店数据看板、活动运营、用户角色和平台配置管理'
WHERE code = 'HQ_OPERATOR';

UPDATE role
SET name = '总部运营兼容角色',
    description = '历史系统管理员兼容角色；总部运营入口已统一承载后台管理能力'
WHERE code = 'ADMIN';

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM `user` u
JOIN role r ON r.code = 'HQ_OPERATOR'
WHERE u.username = 'demo_admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
