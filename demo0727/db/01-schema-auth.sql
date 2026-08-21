-- ============================================================
-- 企业级 AI 知识管理平台 - 认证域数据库初始化脚本
-- Schema: enterprise_auth（用户/角色/权限/系统配置）
-- 数据库: MySQL 8.x | 字符集: utf8mb4
-- 仅首次初始化空数据卷时执行（docker-entrypoint-initdb.d）
-- ============================================================

-- 创建认证库
CREATE DATABASE IF NOT EXISTS enterprise_auth
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE enterprise_auth;

-- 关键：声明当前连接客户端字符集为 utf8mb4。
-- 否则 docker-entrypoint-initdb.d 初始化时 mysql 客户端以 latin1 连接，
-- 会把 UTF-8 的中文二次编码成乱码（UTF-8 → CP1252 → UTF-8 双重编码）
SET NAMES utf8mb4;

-- ============================================================
-- 1. 用户表
-- ============================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键 ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码（BCrypt 加密）',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',
    locked_until DATETIME DEFAULT NULL COMMENT '账户锁定时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_email (email),
    KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 角色表
-- ============================================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键 ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码（唯一标识）',
    description VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ============================================================
-- 3. 权限表
-- ============================================================
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id BIGINT NOT NULL COMMENT '主键 ID',
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码（唯一标识）',
    permission_type VARCHAR(20) NOT NULL DEFAULT 'api' COMMENT '权限类型：menu-菜单，button-按钮，api-接口',
    parent_id BIGINT DEFAULT NULL COMMENT '父级 ID',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ============================================================
-- 4. 用户角色关联表
-- ============================================================
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================================
-- 5. 角色权限关联表
-- ============================================================
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id BIGINT NOT NULL COMMENT '主键 ID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    permission_id BIGINT NOT NULL COMMENT '权限 ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ============================================================
-- 6. 系统配置表
-- ============================================================
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id BIGINT NOT NULL COMMENT '主键 ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(2000) DEFAULT NULL COMMENT '配置值',
    remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 初始化角色数据
INSERT INTO sys_role (id, role_name, role_code, description, status) VALUES
(1, '超级管理员', 'ROLE_ADMIN', '系统超级管理员，拥有所有权限', 1),
(2, '普通员工', 'ROLE_USER', '普通员工，基础操作权限', 1);

-- 初始化权限数据（API 级权限，含知识域权限编码，供 RBAC 元数据统一管理）
INSERT INTO sys_permission (id, permission_name, permission_code, permission_type, description) VALUES
(1, '用户注册', 'api:user:register', 'api', '用户注册接口'),
(2, '用户登录', 'api:user:login', 'api', '用户登录接口'),
(3, '查看用户信息', 'api:user:view', 'api', '查看用户信息接口'),
(4, '修改用户信息', 'api:user:update', 'api', '修改用户信息接口'),
(5, '修改密码', 'api:user:password', 'api', '修改密码接口'),
(10, '查看角色', 'api:role:view', 'api', '查看角色接口'),
(11, '管理角色', 'api:role:manage', 'api', '角色增删改查接口'),
(20, '查看权限', 'api:permission:view', 'api', '查看权限接口'),
(21, '管理权限', 'api:permission:manage', 'api', '权限分配接口'),
(30, '查看知识库', 'api:knowledge:view', 'api', '查看知识库接口'),
(31, '创建知识库', 'api:knowledge:create', 'api', '创建知识库接口'),
(32, '编辑知识库', 'api:knowledge:edit', 'api', '编辑知识库接口'),
(33, '删除知识库', 'api:knowledge:delete', 'api', '删除知识库接口'),
(40, '查看文档', 'api:document:view', 'api', '查看文档接口'),
(41, '上传文档', 'api:document:upload', 'api', '上传文档接口'),
(42, '编辑文档', 'api:document:edit', 'api', '编辑文档接口'),
(43, '删除文档', 'api:document:delete', 'api', '删除文档接口'),
(50, '文档搜索', 'api:search', 'api', '文档搜索接口'),
(60, 'AI 问答', 'api:ai:ask', 'api', 'AI 问答接口'),
(61, '创建 Embedding', 'api:ai:embedding', 'api', 'Embedding 接口');

-- 为超级管理员分配所有权限（id 由 ROW_NUMBER 生成，避免 1364 报错）
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT ROW_NUMBER() OVER (ORDER BY id), 1, id FROM sys_permission WHERE status = 1;

-- 为普通员工分配基础权限（id 从 100 起，与超级管理员区分）
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(100, 2, 1), (101, 2, 2), (102, 2, 3), (103, 2, 4), (104, 2, 5),
(105, 2, 10), (106, 2, 20), (107, 2, 30), (108, 2, 31), (109, 2, 40),
(110, 2, 41), (111, 2, 50), (112, 2, 60);

-- 初始化管理员用户（密码: admin123，BCrypt 加密）
INSERT INTO sys_user (id, username, password, real_name, email, phone, status) VALUES
(1, 'admin', '$2a$10$bJ2NlMo2AUd/Mg/ZdvcKTOwH6rsaNiCUIJatQQy7InT1y.SE5FEpi', '系统管理员', 'admin@enterprise.com', '13800138000', 1);

-- 为管理员分配角色
INSERT INTO sys_user_role (id, user_id, role_id) VALUES (1, 1, 1);

-- 初始化系统配置
INSERT INTO sys_config (id, config_key, config_value, remark) VALUES
(1, 'system.name', '企业 AI 知识管理平台', '系统名称'),
(2, 'system.description', '面向企业内部的知识文档管理系统，支持文档管理、智能检索和 AI 问答', '系统描述'),
(3, 'system.icp', '', '备案信息');

-- ============================================================
-- 验证初始化结果
-- ============================================================
SELECT '用户表' as table_name, COUNT(*) as count FROM sys_user
UNION ALL
SELECT '角色表', COUNT(*) FROM sys_role
UNION ALL
SELECT '权限表', COUNT(*) FROM sys_permission
UNION ALL
SELECT '用户角色关联表', COUNT(*) FROM sys_user_role
UNION ALL
SELECT '角色权限关联表', COUNT(*) FROM sys_role_permission
UNION ALL
SELECT '系统配置表', COUNT(*) FROM sys_config;
