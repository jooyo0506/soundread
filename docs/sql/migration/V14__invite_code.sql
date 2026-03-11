-- ============================================================
-- V14: 邀请码注册系统
-- 替换 SMS 验证码注册方案（成本高），改为运营驱动的邀请码模式
-- ============================================================

CREATE TABLE invite_code (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE              COMMENT '邀请码（字母+数字，大写）',
    max_uses    INT          NOT NULL DEFAULT 1           COMMENT '最大使用次数，-1 = 无限',
    used_count  INT          NOT NULL DEFAULT 0           COMMENT '已使用次数',
    remark      VARCHAR(128)                              COMMENT '备注（如：用于哪个渠道）',
    created_at  DATETIME     NOT NULL DEFAULT NOW()       COMMENT '创建时间',
    expired_at  DATETIME                                  COMMENT '过期时间，NULL = 永不过期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码表';

-- 初始邀请码（上线初期使用）
-- 建议上线后通过管理端 API /api/admin/invite-code 创建新码
INSERT INTO invite_code(code, max_uses, remark) VALUES
    ('SOUNDREAD2025', -1,  '初期公测邀请码，无限次数'),
    ('EARLYBIRD001',  100, '早鸟渠道邀请码 A'),
    ('EARLYBIRD002',  100, '早鸟渠道邀请码 B');
