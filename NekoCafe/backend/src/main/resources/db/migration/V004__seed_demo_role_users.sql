INSERT INTO `user` (username, password_hash, nickname, phone, email, status) VALUES
  ('demo_customer', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '猫爪会员演示', NULL, 'demo_customer@nekocafe.local', 'ACTIVE'),
  ('demo_staff', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '前台店员演示', NULL, 'demo_staff@nekocafe.local', 'ACTIVE'),
  ('demo_cat', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '猫咪管家演示', NULL, 'demo_cat@nekocafe.local', 'ACTIVE'),
  ('demo_manager', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '门店店长演示', NULL, 'demo_manager@nekocafe.local', 'ACTIVE'),
  ('demo_hq', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '总部运营演示', NULL, 'demo_hq@nekocafe.local', 'ACTIVE'),
  ('demo_admin', '$2b$10$MPsQDg3Jt9DEsQ6I23NlpuOjAk5IZT0FMWRZpq7Circ1A9wwXev/e', '系统管理员演示', NULL, 'demo_admin@nekocafe.local', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  status = VALUES(status),
  deleted = 0;

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'CUSTOMER'
WHERE u.username = 'demo_customer'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'STAFF'
WHERE u.username = 'demo_staff'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'CAT_CARETAKER'
WHERE u.username = 'demo_cat'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'STORE_MANAGER'
WHERE u.username = 'demo_manager'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'HQ_OPERATOR'
WHERE u.username = 'demo_hq'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN role r ON r.code = 'ADMIN'
WHERE u.username = 'demo_admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO member_account (user_id, level_code, points, total_spent)
SELECT u.id, 'NORMAL', 0, 0.00
FROM `user` u
WHERE u.username = 'demo_customer'
ON DUPLICATE KEY UPDATE
  level_code = VALUES(level_code),
  points = VALUES(points),
  total_spent = VALUES(total_spent);
