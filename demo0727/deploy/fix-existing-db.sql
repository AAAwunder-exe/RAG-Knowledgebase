-- ============================================================
-- 活库修复脚本（针对已存在的数据卷执行，schema.sql 只对全新数据卷生效）
-- 执行方式：docker exec -i ai-platform-mysql mysql -uroot -pai_platform_2024 enterprise_ai < fix-existing-db.sql
-- 或直接粘贴到 mysql 客户端执行
-- ============================================================

-- 1. sys_role 补 sort 列（Role 实体映射了 sort，缺列会导致角色查询报错）
ALTER TABLE sys_role ADD COLUMN sort INT NOT NULL DEFAULT 0 COMMENT '排序';

-- 2. 修复 admin 用户 real_name 乱码（存成了 '?????'）
UPDATE sys_user SET real_name='系统管理员', status=1, deleted=0 WHERE username='admin';

-- 3. 为 admin 绑定超级管理员角色（sys_user_role 当前为空）
INSERT INTO sys_user_role (id, user_id, role_id, create_time, update_time, deleted)
VALUES (2085625945519800323, 2085625945519800322, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);
