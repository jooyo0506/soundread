-- ============================================
-- 持久化 + 内容生态 — 全量 DDL
-- ============================================

USE sound_read;

-- 1. 用户创作记录表（统一所有模块）
CREATE TABLE IF NOT EXISTS `user_creation` (
    `id`             BIGINT       NOT NULL COMMENT '雪花ID',
    `user_id`        BIGINT       NOT NULL,
    `type`           VARCHAR(20)  NOT NULL COMMENT 'tts/emotion/drama/podcast/novel',
    `title`          VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `input_text`     TEXT         DEFAULT NULL COMMENT '输入文本(回看)',
    `voice_id`       VARCHAR(100) DEFAULT NULL COMMENT '音色ID',
    `audio_url`      VARCHAR(500) NOT NULL COMMENT '音频URL(R2)',
    `audio_duration` INT          DEFAULT 0 COMMENT '时长(秒)',
    `file_size`      BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    `subtitle_url`   VARCHAR(500) DEFAULT NULL COMMENT '字幕URL',
    `extra_json`     JSON         DEFAULT NULL COMMENT '扩展(情感标签/AI prompt等)',
    `is_published`   TINYINT      DEFAULT 0 COMMENT '是否已发布到发现页',
    `work_id`        BIGINT       DEFAULT NULL COMMENT '关联的work.id(发布后回填)',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `deleted`        TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_type` (`user_id`, `type`),
    KEY `idx_user_created` (`user_id`, `created_at` DESC)
) ENGINE=InnoDB COMMENT='用户创作记录表';

-- 2. 用户存储计量表
CREATE TABLE IF NOT EXISTS `user_storage` (
    `user_id`            BIGINT   NOT NULL,
    `used_bytes`         BIGINT   DEFAULT 0 COMMENT '已用字节',
    `file_count`         INT      DEFAULT 0 COMMENT '文件数',
    `quota_override_mb`  INT      DEFAULT NULL COMMENT '运营手动覆盖的容量上限(NULL=按策略)',
    `last_calculated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB COMMENT='用户存储计量表';

-- 3. 有声书项目表
CREATE TABLE IF NOT EXISTS `novel_project` (
    `id`              BIGINT       NOT NULL COMMENT '雪花ID',
    `user_id`         BIGINT       NOT NULL,
    `title`           VARCHAR(200) NOT NULL COMMENT '项目名称',
    `cover_url`       VARCHAR(500) DEFAULT NULL COMMENT '封面',
    `voice_id`        VARCHAR(100) NOT NULL COMMENT '主音色ID',
    `total_chars`     INT          DEFAULT 0 COMMENT '原文总字数',
    `total_chapters`  INT          DEFAULT 0 COMMENT '总章节数',
    `total_duration`  INT          DEFAULT 0 COMMENT '合成总时长(秒)',
    `status`          VARCHAR(20)  DEFAULT 'draft' COMMENT 'draft/analyzing/annotating/synthesizing/completed/failed',
    `progress`        INT          DEFAULT 0 COMMENT '进度 0~100',
    `audio_url`       VARCHAR(500) DEFAULT NULL COMMENT '完整音频URL',
    `subtitle_url`    VARCHAR(500) DEFAULT NULL COMMENT '完整字幕URL',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB COMMENT='有声书项目表';

-- 4. 有声书章节表
CREATE TABLE IF NOT EXISTS `novel_chapter` (
    `id`              BIGINT       NOT NULL COMMENT '雪花ID',
    `project_id`      BIGINT       NOT NULL,
    `chapter_index`   INT          NOT NULL COMMENT '章节序号(从1开始)',
    `title`           VARCHAR(200) DEFAULT NULL COMMENT '章节标题',
    `raw_text`        LONGTEXT     NOT NULL COMMENT '原始文本',
    `char_count`      INT          DEFAULT 0 COMMENT '字符数',
    `total_segments`  INT          DEFAULT 0 COMMENT '分段总数',
    `status`          VARCHAR(20)  DEFAULT 'pending' COMMENT 'pending/splitting/annotating/synthesizing/completed/failed',
    `audio_url`       VARCHAR(500) DEFAULT NULL COMMENT '本章音频URL',
    `audio_duration`  INT          DEFAULT 0 COMMENT '本章时长(秒)',
    `emotion_summary` VARCHAR(200) DEFAULT NULL COMMENT 'AI本章主情绪',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_index` (`project_id`, `chapter_index`)
) ENGINE=InnoDB COMMENT='有声书章节表';

-- 5. 有声书分段表
CREATE TABLE IF NOT EXISTS `novel_segment` (
    `id`              BIGINT       NOT NULL COMMENT '雪花ID',
    `chapter_id`      BIGINT       NOT NULL,
    `segment_index`   INT          NOT NULL COMMENT '段内序号(从1开始)',
    `raw_text`        TEXT         NOT NULL COMMENT '原始文本片段',
    `annotated_text`  TEXT         DEFAULT NULL COMMENT 'AI标注文本(含cot标签)',
    `context_texts`   VARCHAR(500) DEFAULT NULL COMMENT 'context_texts(JSON数组)',
    `emotion_label`   VARCHAR(50)  DEFAULT NULL COMMENT '情感标签',
    `tension_level`   INT          DEFAULT 5 COMMENT '紧张度1~10',
    `char_count`      INT          DEFAULT 0 COMMENT '字符数',
    `status`          VARCHAR(20)  DEFAULT 'pending' COMMENT 'pending/annotated/synthesizing/completed/failed',
    `audio_url`       VARCHAR(500) DEFAULT NULL COMMENT '本段音频URL',
    `audio_duration`  INT          DEFAULT 0 COMMENT '本段时长(ms)',
    `section_id`      VARCHAR(64)  DEFAULT NULL COMMENT 'TTS session_id',
    `subtitle_json`   JSON         DEFAULT NULL COMMENT '逐字时间戳',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_chapter_index` (`chapter_id`, `segment_index`)
) ENGINE=InnoDB COMMENT='有声书分段表';

-- 6. 改造 work 表 — 新增审核/热度/来源字段
-- 注意: 如果字段已存在会报错, 请按需注释已存在的行
ALTER TABLE `work`
    ADD COLUMN `creation_id`     BIGINT        DEFAULT NULL COMMENT '关联创作记录ID' AFTER `user_id`,
    ADD COLUMN `source_type`     VARCHAR(20)   DEFAULT 'tts' COMMENT '来源: tts/emotion/drama/podcast/novel' AFTER `creation_id`,
    ADD COLUMN `review_status`   VARCHAR(20)   DEFAULT 'pending' COMMENT 'pending/approved/rejected/featured' AFTER `status`,
    ADD COLUMN `review_note`     VARCHAR(500)  DEFAULT NULL COMMENT '审核备注' AFTER `review_status`,
    ADD COLUMN `reviewed_at`     DATETIME      DEFAULT NULL COMMENT '审核时间' AFTER `review_note`,
    ADD COLUMN `reviewed_by`     BIGINT        DEFAULT NULL COMMENT '审核人ID' AFTER `reviewed_at`,
    ADD COLUMN `is_featured`     TINYINT       DEFAULT 0 COMMENT '运营精选' AFTER `reviewed_by`,
    ADD COLUMN `heat_score`      DECIMAL(10,2) DEFAULT 0 COMMENT '热度分' AFTER `is_featured`,
    ADD COLUMN `share_count`     INT           DEFAULT 0 COMMENT '分享数' AFTER `like_count`,
    ADD COLUMN `comment_count`   INT           DEFAULT 0 COMMENT '评论数' AFTER `share_count`;
