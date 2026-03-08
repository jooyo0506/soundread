-- ============================================
-- 声读 (sound_read) 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS sound_read DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE sound_read;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`              BIGINT       NOT NULL COMMENT '雪花ID',
    `phone`           VARCHAR(20)  NOT NULL COMMENT '手机号',
    `password_hash`   VARCHAR(128) DEFAULT NULL COMMENT 'SHA-256密码哈希',
    `nickname`        VARCHAR(50)  DEFAULT '声读用户' COMMENT '昵称',
    `avatar_url`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `vip_level`       INT          DEFAULT 0 COMMENT 'VIP等级: 0免费 1月度 2年度 3终身',
    `vip_expire_time` DATETIME     DEFAULT NULL COMMENT 'VIP过期时间',
    `tier_code`       VARCHAR(20)  DEFAULT 'user' COMMENT '当前等级代号, 关联 sys_tier_policy.tier_code',
    `role`            VARCHAR(20)  DEFAULT 'user' COMMENT '系统角色(user/admin)',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB COMMENT='用户表';

-- VIP 订单表
CREATE TABLE IF NOT EXISTS `vip_order` (
    `id`          BIGINT        NOT NULL COMMENT '雪花ID',
    `user_id`     BIGINT        NOT NULL,
    `plan_id`     VARCHAR(20)   NOT NULL COMMENT 'month/year/lifetime',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `pay_method`  VARCHAR(20)   DEFAULT NULL COMMENT 'wechat/alipay',
    `status`      VARCHAR(20)   DEFAULT 'pending' COMMENT 'pending/paid/cancelled/refunded',
    `trade_no`    VARCHAR(64)   DEFAULT NULL COMMENT '第三方交易号',
    `paid_at`     DATETIME      DEFAULT NULL,
    `expire_at`   DATETIME      DEFAULT NULL,
    `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='VIP订单表';

-- TTS 异步任务表
CREATE TABLE IF NOT EXISTS `tts_task` (
    `id`               BIGINT       NOT NULL COMMENT '雪花ID',
    `user_id`          BIGINT       NOT NULL,
    `input_text`       LONGTEXT     DEFAULT NULL COMMENT '输入文本',
    `voice_id`         VARCHAR(100) NOT NULL COMMENT '音色ID',
    `task_type`        VARCHAR(20)  DEFAULT 'short' COMMENT 'short/long/emotion/ai_script',
    `status`           VARCHAR(20)  DEFAULT 'pending' COMMENT 'pending/processing/completed/failed',
    `audio_url`        VARCHAR(500) DEFAULT NULL COMMENT '合成音频URL (R2)',
    `audio_duration`   INT          DEFAULT NULL COMMENT '音频时长(秒)',
    `external_task_id` VARCHAR(64)  DEFAULT NULL COMMENT '火山引擎任务ID',
    `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `completed_at`     DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB COMMENT='TTS异步任务表';

-- 克隆音色表
CREATE TABLE IF NOT EXISTS `cloned_voice` (
    `id`              BIGINT       NOT NULL COMMENT '雪花ID',
    `user_id`         BIGINT       NOT NULL,
    `sample_url`      VARCHAR(500) NOT NULL COMMENT 'R2样本存储路径',
    `cloned_voice_id` VARCHAR(100) DEFAULT NULL COMMENT '克隆后音色ID',
    `voice_name`      VARCHAR(50)  DEFAULT NULL COMMENT '自定义音色名',
    `sample_duration`  INT         DEFAULT NULL COMMENT '样本时长(秒)',
    `status`          VARCHAR(20)  DEFAULT 'uploaded' COMMENT 'uploaded/training/ready/failed',
    `quality_score`   INT          DEFAULT NULL COMMENT 'AI质量评分(0-100)',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='克隆音色表';

-- 作品表
CREATE TABLE IF NOT EXISTS `work` (
    `id`             BIGINT       NOT NULL COMMENT '雪花ID',
    `user_id`        BIGINT       NOT NULL,
    `title`          VARCHAR(200) NOT NULL COMMENT '作品标题',
    `category`       VARCHAR(20)  DEFAULT 'hot' COMMENT 'hot/latest/emotion/story/news',
    `cover_url`      VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    `audio_url`      VARCHAR(500) NOT NULL COMMENT '音频URL (R2)',
    `audio_duration` INT          DEFAULT 0 COMMENT '音频时长(秒)',
    `play_count`     INT          DEFAULT 0,
    `like_count`     INT          DEFAULT 0,
    `ai_summary`     TEXT         DEFAULT NULL COMMENT 'AI摘要(语义推荐)',
    `vector_id`      VARCHAR(100) DEFAULT NULL COMMENT '向量库ID',
    `status`         VARCHAR(20)  DEFAULT 'draft' COMMENT 'draft/published/moderation/rejected',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `deleted`        TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_category_play` (`category`, `play_count` DESC),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='作品表';

-- AI 交互记录表
CREATE TABLE IF NOT EXISTS `ai_interaction` (
    `id`               BIGINT       NOT NULL COMMENT '雪花ID',
    `user_id`          BIGINT       NOT NULL,
    `session_id`       VARCHAR(64)  NOT NULL COMMENT '会话ID',
    `context_work_id`  BIGINT       DEFAULT NULL COMMENT '关联作品ID',
    `question`         TEXT         NOT NULL COMMENT '用户问题(ASR)',
    `answer`           TEXT         NOT NULL COMMENT 'AI回答',
    `answer_audio_url` VARCHAR(500) DEFAULT NULL COMMENT '回答音频URL',
    `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_session` (`user_id`, `session_id`)
) ENGINE=InnoDB COMMENT='AI交互记录表';

-- 会员等级策略字典表
CREATE TABLE IF NOT EXISTS `sys_tier_policy` (
    `id`             INT          NOT NULL AUTO_INCREMENT,
    `tier_code`      VARCHAR(20)  NOT NULL COMMENT '等级代号',
    `tier_name`      VARCHAR(50)  NOT NULL COMMENT '等级名称',
    `feature_flags`  JSON         NOT NULL COMMENT '功能开关',
    `quota_limits`   JSON         NOT NULL COMMENT '配额限制',
    `resource_rules` JSON         NOT NULL COMMENT '资源分配规则',
    `updated_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tier_code` (`tier_code`)
) ENGINE=InnoDB COMMENT='会员等级策略字典表';

-- 系统音色字典表
CREATE TABLE IF NOT EXISTS `sys_voice` (
    `id`             INT          NOT NULL AUTO_INCREMENT,
    `voice_id`       VARCHAR(100) NOT NULL COMMENT '底层模型调用ID, 如 BV002_streaming',
    `name`           VARCHAR(50)  NOT NULL COMMENT '前台展示名称，如：通用男声',
    `category`       VARCHAR(50)  NOT NULL COMMENT '大类(如: 中文场景, 特色方言, 外语多语种, 特色声音)',
    `language`       VARCHAR(20)  DEFAULT 'zh-CN' COMMENT '支持语种(zh-CN, en-US等)',
    `gender`         VARCHAR(20)  DEFAULT 'Female' COMMENT '发音人性别(Male/Female/Child)',
    `tags`           VARCHAR(100) DEFAULT NULL COMMENT '特点/场景标签',
    `supported_engines` VARCHAR(100) DEFAULT 'tts-1.0,tts-2.0,podcast' COMMENT '模块可用范围标识',
    `description`    VARCHAR(255) DEFAULT NULL COMMENT '备注或试听文案',
    `preview_url`    VARCHAR(500) DEFAULT NULL COMMENT '前台试听音频的 URL',
    `price`          DECIMAL(10,2) DEFAULT 0.00 COMMENT '购买价格(0表示完全免费)',
    `is_vip_free`    TINYINT      DEFAULT 0 COMMENT '是否针对 VIP 用户免费(1:VIP免费 0:必须单独购买)',
    `status`         VARCHAR(20)  DEFAULT 'active' COMMENT '上架状态(active, offline)',
    `sort_order`     INT          DEFAULT 0 COMMENT '排序权重',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_voice_id` (`voice_id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB COMMENT='系统标准音色字典库';

-- 用户已购资产表
CREATE TABLE IF NOT EXISTS `user_voice` (
    `id`             BIGINT       NOT NULL COMMENT '雪花ID',
    `user_id`        BIGINT       NOT NULL COMMENT '购买者ID',
    `voice_id`       VARCHAR(100) NOT NULL COMMENT '购买的音色ID',
    `obtain_way`     VARCHAR(20)  DEFAULT 'purchased' COMMENT '获取途径: purchased(购买), gift(赠送), activity(活动)',
    `expire_time`    DATETIME     DEFAULT NULL COMMENT '过期时间(NULL表示永久专属)',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_voice` (`user_id`, `voice_id`)
) ENGINE=InnoDB COMMENT='用户音色私有资产表';

-- 音色独立订单明细表
CREATE TABLE IF NOT EXISTS `voice_order` (
    `id`          BIGINT        NOT NULL COMMENT '雪花ID(订单号)',
    `user_id`     BIGINT        NOT NULL,
    `voice_id`    VARCHAR(100)  NOT NULL COMMENT '关联购买音色ID',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '实际支付金额',
    `pay_method`  VARCHAR(20)   DEFAULT NULL COMMENT '支付通道(wechat/alipay)',
    `status`      VARCHAR(20)   DEFAULT 'pending' COMMENT 'pending/paid/cancelled/refunded',
    `trade_no`    VARCHAR(64)   DEFAULT NULL COMMENT '第三方支付系统流水号',
    `paid_at`     DATETIME      DEFAULT NULL,
    `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='独立音色购买交易订单表';


-- ========= 初始化策略数据 =========

-- 游客 (未注册)
INSERT IGNORE INTO `sys_tier_policy` (`tier_code`, `tier_name`, `feature_flags`, `quota_limits`, `resource_rules`) VALUES
('guest', '游客', 
 '{"tts_basic": true, "tts_emotion_v2": false, "ai_podcast": false, "voice_clone": false, "ai_script": false, "multi_language": false}',
 '{"tts_daily_chars": 100, "tts_v2_daily_chars": 0, "clone_total_count": 0, "ask_daily_count": 1, "podcast_daily_count": 0, "storage_max_mb": 0, "max_projects": 0, "data_retention_days": 1}',
 '{"llm_model": "doubao-seed-2-0-lite-260215", "task_priority": 0, "qps_limit": 1, "voice_tier": "basic"}'
);

-- 普通用户 (已注册免费)
INSERT IGNORE INTO `sys_tier_policy` (`tier_code`, `tier_name`, `feature_flags`, `quota_limits`, `resource_rules`) VALUES
('user', '普通用户',
 '{"tts_basic": true, "tts_emotion_v2": false, "ai_podcast": false, "voice_clone": true, "ai_script": true, "multi_language": false}',
 '{"tts_daily_chars": 2000, "tts_v2_daily_chars": 0, "clone_total_count": 1, "ask_daily_count": 5, "podcast_daily_count": 0, "storage_max_mb": 50, "max_projects": 5, "data_retention_days": 15}',
 '{"llm_model": "doubao-seed-2-0-lite-260215", "task_priority": 0, "qps_limit": 2, "voice_tier": "basic"}'
);

-- VIP 月度会员
INSERT IGNORE INTO `sys_tier_policy` (`tier_code`, `tier_name`, `feature_flags`, `quota_limits`, `resource_rules`) VALUES
('vip_month', 'VIP月度会员',
 '{"tts_basic": true, "tts_emotion_v2": true, "ai_podcast": true, "voice_clone": true, "ai_script": true, "multi_language": true}',
 '{"tts_daily_chars": 50000, "tts_v2_daily_chars": 10000, "clone_total_count": 3, "ask_daily_count": 50, "podcast_daily_count": 5, "novel_daily_chars": 50000, "novel_max_projects": 3, "storage_max_mb": 500, "max_projects": 50, "data_retention_days": 30}',
 '{"llm_model": "doubao-seed-2-0-lite-260215", "task_priority": 50, "qps_limit": 5, "voice_tier": "premium"}'
);

-- VIP 年度会员
INSERT IGNORE INTO `sys_tier_policy` (`tier_code`, `tier_name`, `feature_flags`, `quota_limits`, `resource_rules`) VALUES
('vip_year', 'VIP年度会员',
 '{"tts_basic": true, "tts_emotion_v2": true, "ai_podcast": true, "voice_clone": true, "ai_script": true, "multi_language": true}',
 '{"tts_daily_chars": -1, "tts_v2_daily_chars": 50000, "clone_total_count": 10, "ask_daily_count": -1, "podcast_daily_count": 20, "novel_daily_chars": 200000, "novel_max_projects": 10, "storage_max_mb": 2000, "max_projects": 200, "data_retention_days": 90}',
 '{"llm_model": "doubao-seed-2-0-lite-260215", "task_priority": 80, "qps_limit": 10, "voice_tier": "all"}'
);

-- VIP 终身会员
INSERT IGNORE INTO `sys_tier_policy` (`tier_code`, `tier_name`, `feature_flags`, `quota_limits`, `resource_rules`) VALUES
('vip_lifetime', 'VIP终身会员',
 '{"tts_basic": true, "tts_emotion_v2": true, "ai_podcast": true, "voice_clone": true, "ai_script": true, "multi_language": true}',
 '{"tts_daily_chars": -1, "tts_v2_daily_chars": -1, "clone_total_count": -1, "ask_daily_count": -1, "podcast_daily_count": -1, "novel_daily_chars": -1, "novel_max_projects": -1, "storage_max_mb": -1, "max_projects": -1, "data_retention_days": -1}',
 '{"llm_model": "doubao-seed-2-0-lite-260215", "task_priority": 99, "qps_limit": -1, "voice_tier": "all"}'
);

-- 初始化基础音色数据 (依据最新规则补充语种与性别，并全面设定价格 0 应对短文本免费福利)
INSERT IGNORE INTO `sys_voice` (`voice_id`, `name`, `language`, `gender`, `category`, `tags`, `is_vip_free`, `price`, `sort_order`, `supported_engines`) VALUES
('BV002_streaming', '通用男声', 'zh-CN', 'Male', '中文场景', '通用场景，标准男声', 0, 0.00, 100, 'tts-1.0,tts-2.0,podcast'),
('BV001_streaming', '通用女声', 'zh-CN', 'Female', '中文场景', '通用场景，标准女声', 0, 0.00, 99, 'tts-1.0,tts-2.0,podcast'),
('BV700_streaming', '灿灿', 'zh-CN', 'Female', '中文场景', '通用场景，中文', 0, 0.00, 98, 'tts-1.0,tts-2.0,podcast'),
('BV056_streaming', '阳光男声', 'zh-CN', 'Male', '中文场景', '视频配音，阳光活力', 0, 0.00, 97, 'tts-1.0,tts-2.0,podcast'),
('BV005_streaming', '活泼女声', 'zh-CN', 'Female', '中文场景', '视频配音，活泼可爱', 0, 0.00, 96, 'tts-1.0,tts-2.0,podcast'),
('BV701_streaming', '擎苍', 'zh-CN', 'Male', '中文场景', '有声阅读，浑厚有力', 0, 0.00, 90, 'tts-1.0,tts-2.0,podcast'),
('BV102_streaming', '儒雅青年', 'zh-CN', 'Male', '中文场景', '有声阅读，温文尔雅', 0, 0.00, 89, 'tts-1.0,tts-2.0,podcast'),
('BV113_streaming', '甜宠少御', 'zh-CN', 'Female', '中文场景', '有声阅读，甜美温柔', 0, 0.00, 88, 'tts-1.0,tts-2.0,podcast'),
('BV115_streaming', '古风少御', 'zh-CN', 'Female', '中文场景', '有声阅读，古风韵味', 0, 0.00, 87, 'tts-1.0,tts-2.0,podcast'),
('BV007_streaming', '亲切女声', 'zh-CN', 'Female', '中文场景', '客服场景，亲切自然', 0, 0.00, 86, 'tts-1.0,tts-2.0,podcast'),
('BV033_streaming', '温柔小哥', 'zh-CN', 'Male', '中文场景', '教育场景，温和耐心', 0, 0.00, 85, 'tts-1.0,tts-2.0,podcast'),
('BV034_streaming', '知性姐姐-双语', 'zh-CN', 'Female', '中文场景', '教育场景，知性专业', 0, 0.00, 84, 'tts-1.0,tts-2.0,podcast'),
('BV021_streaming', '东北老铁', 'zh-CN', 'Male', '特色方言', '方言，东北话', 0, 0.00, 80, 'tts-1.0,tts-2.0,podcast'),
('BV019_streaming', '重庆小伙', 'zh-CN', 'Male', '特色方言', '方言，重庆话', 0, 0.00, 79, 'tts-1.0,tts-2.0,podcast'),
('BV524_streaming', '日语男声', 'ja-JP', 'Male', '外语及多语种', '多语种，日语男声', 0, 0.00, 70, 'tts-1.0,tts-2.0,podcast'),
('BV522_streaming', '气质女生', 'ja-JP', 'Female', '外语及多语种', '多语种，日语女声', 0, 0.00, 69, 'tts-1.0,tts-2.0,podcast'),
('BV503_streaming', '活力女声-Ariana', 'en-US', 'Female', '外语及多语种', '美式发音，英语女声', 0, 0.00, 68, 'tts-1.0,tts-2.0,podcast'),
('BV504_streaming', '活力男声-Jackson', 'en-US', 'Male', '外语及多语种', '美式发音，英语男声', 0, 0.00, 67, 'tts-1.0,tts-2.0,podcast'),
('BV051_streaming', '奶气萌娃', 'zh-CN', 'Child', '特色音色', '特色音色，儿童声音', 0, 0.00, 60, 'tts-1.0,tts-2.0,podcast');
