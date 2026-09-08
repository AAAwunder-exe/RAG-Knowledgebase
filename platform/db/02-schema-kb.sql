-- ============================================================
-- 企业级 AI 知识管理平台 - 知识域数据库初始化脚本
-- Schema: enterprise_kb（知识库/文档）
-- 数据库: MySQL 8.x | 字符集: utf8mb4
-- 仅首次初始化空数据卷时执行（docker-entrypoint-initdb.d）
-- ============================================================

-- 创建知识库
CREATE DATABASE IF NOT EXISTS enterprise_kb
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE enterprise_kb;

-- 关键：声明当前连接客户端字符集为 utf8mb4。
-- 否则 docker-entrypoint-initdb.d 初始化时 mysql 客户端以 latin1 连接，
-- 会把 UTF-8 的中文二次编码成乱码（UTF-8 → CP1252 → UTF-8 双重编码）
SET NAMES utf8mb4;

-- ============================================================
-- 1. 知识库表
-- ============================================================
DROP TABLE IF EXISTS knowledge_base;
CREATE TABLE knowledge_base (
    id BIGINT NOT NULL COMMENT '主键 ID',
    name VARCHAR(100) NOT NULL COMMENT '知识库名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '知识库描述',
    creator_id BIGINT NOT NULL COMMENT '创建人 ID',
    creator_name VARCHAR(50) DEFAULT NULL COMMENT '创建人姓名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    document_count INT NOT NULL DEFAULT 0 COMMENT '文档数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    KEY idx_creator_id (creator_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ============================================================
-- 2. 文档表
-- ============================================================
DROP TABLE IF EXISTS document;
CREATE TABLE document (
    id BIGINT NOT NULL COMMENT '主键 ID',
    knowledge_id BIGINT NOT NULL COMMENT '知识库 ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    original_name VARCHAR(200) DEFAULT NULL COMMENT '原始文件名',
    type VARCHAR(20) NOT NULL COMMENT '文件类型：pdf, word, markdown, txt',
    size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    creator_id BIGINT NOT NULL COMMENT '上传人 ID',
    creator_name VARCHAR(50) DEFAULT NULL COMMENT '上传人姓名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    tags VARCHAR(500) DEFAULT NULL COMMENT '分类标签',
    summary VARCHAR(500) DEFAULT NULL COMMENT '摘要/描述',
    content MEDIUMTEXT DEFAULT NULL COMMENT '全文内容（用于搜索）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    KEY idx_knowledge_id (knowledge_id),
    KEY idx_creator_id (creator_id),
    KEY idx_type (type),
    KEY idx_status (status),
    FULLTEXT KEY ft_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ============================================================
-- 验证初始化结果（知识域无种子数据，正常应为 0）
-- ============================================================
SELECT '知识库表' as table_name, COUNT(*) as count FROM knowledge_base
UNION ALL
SELECT '文档表', COUNT(*) FROM document;
