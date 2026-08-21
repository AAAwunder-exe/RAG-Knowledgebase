-- ============================================================
-- 企业级 AI 知识管理平台 - 数据库拆分迁移脚本
-- 用途：把现有单库 enterprise_ai 中的数据，复制到拆分后的两个 schema
--   enterprise_auth（用户/角色/权限/系统配置，6 表）
--   enterprise_kb  （知识库/文档，2 表）
-- 执行对象：已初始化过的 mysql 数据卷（docker-entrypoint-initdb.d 只在空卷首次执行）
--
-- 注意：
--   1. 脚本可重复执行（幂等）：目标表先 DROP 再重建复制，以源库 enterprise_ai 为准
--   2. 原 enterprise_ai 库保留不动（作为备份），确认无误后可手工 DROP
--   3. 执行后两个服务需分别连新库：auth→enterprise_auth，kb→enterprise_kb
-- ============================================================

-- 创建两个新 schema（不存在才创建）
CREATE DATABASE IF NOT EXISTS enterprise_auth
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS enterprise_kb
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- 一、认证域（6 表）：enterprise_ai → enterprise_auth
-- ============================================================

USE enterprise_auth;

-- 1. 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user LIKE enterprise_ai.sys_user;
INSERT INTO sys_user SELECT * FROM enterprise_ai.sys_user;

-- 2. 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role LIKE enterprise_ai.sys_role;
INSERT INTO sys_role SELECT * FROM enterprise_ai.sys_role;

-- 3. 权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission LIKE enterprise_ai.sys_permission;
INSERT INTO sys_permission SELECT * FROM enterprise_ai.sys_permission;

-- 4. 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role LIKE enterprise_ai.sys_user_role;
INSERT INTO sys_user_role SELECT * FROM enterprise_ai.sys_user_role;

-- 5. 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission LIKE enterprise_ai.sys_role_permission;
INSERT INTO sys_role_permission SELECT * FROM enterprise_ai.sys_role_permission;

-- 6. 系统配置表
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config LIKE enterprise_ai.sys_config;
INSERT INTO sys_config SELECT * FROM enterprise_ai.sys_config;

-- ============================================================
-- 二、知识域（2 表）：enterprise_ai → enterprise_kb
-- ============================================================

USE enterprise_kb;

-- 1. 知识库表
DROP TABLE IF EXISTS knowledge_base;
CREATE TABLE knowledge_base LIKE enterprise_ai.knowledge_base;
INSERT INTO knowledge_base SELECT * FROM enterprise_ai.knowledge_base;

-- 2. 文档表
DROP TABLE IF EXISTS document;
CREATE TABLE document LIKE enterprise_ai.document;
INSERT INTO document SELECT * FROM enterprise_ai.document;

-- ============================================================
-- 验证迁移结果
-- ============================================================
USE enterprise_auth;
SELECT 'enterprise_auth.sys_user' as tbl, COUNT(*) as cnt FROM sys_user
UNION ALL SELECT 'enterprise_auth.sys_role', COUNT(*) FROM sys_role
UNION ALL SELECT 'enterprise_auth.sys_permission', COUNT(*) FROM sys_permission
UNION ALL SELECT 'enterprise_auth.sys_user_role', COUNT(*) FROM sys_user_role
UNION ALL SELECT 'enterprise_auth.sys_role_permission', COUNT(*) FROM sys_role_permission
UNION ALL SELECT 'enterprise_auth.sys_config', COUNT(*) FROM sys_config;

USE enterprise_kb;
SELECT 'enterprise_kb.knowledge_base' as tbl, COUNT(*) as cnt FROM knowledge_base
UNION ALL SELECT 'enterprise_kb.document', COUNT(*) FROM document;

-- ============================================================
-- 对比源库数据量（确认一致）
-- ============================================================
USE enterprise_ai;
SELECT 'enterprise_ai.sys_user' as tbl, COUNT(*) as cnt FROM sys_user
UNION ALL SELECT 'enterprise_ai.sys_role', COUNT(*) FROM sys_role
UNION ALL SELECT 'enterprise_ai.sys_permission', COUNT(*) FROM sys_permission
UNION ALL SELECT 'enterprise_ai.sys_user_role', COUNT(*) FROM sys_user_role
UNION ALL SELECT 'enterprise_ai.sys_role_permission', COUNT(*) FROM sys_role_permission
UNION ALL SELECT 'enterprise_ai.sys_config', COUNT(*) FROM sys_config
UNION ALL SELECT 'enterprise_ai.knowledge_base', COUNT(*) FROM knowledge_base
UNION ALL SELECT 'enterprise_ai.document', COUNT(*) FROM document;
