-- ============================================================
-- 统一发布架构 + AI Music 模块 — DDL 迁移脚本
-- 执行方式: mysql -uroot -p123456 sound_read < unified_publish_and_music.sql
-- ============================================================

-- 1. work 表扩展字段 (统一发布架构)
ALTER TABLE work ADD COLUMN content_type VARCHAR(32) DEFAULT 'audio' COMMENT '内容类型: audio/novel/podcast/music' AFTER source_type;
ALTER TABLE work ADD COLUMN description TEXT COMMENT '内容描述/摘要' AFTER title;
ALTER TABLE work ADD COLUMN word_count INT DEFAULT 0 COMMENT '字数（小说用）' AFTER description;
ALTER TABLE work ADD COLUMN chapter_count INT DEFAULT 0 COMMENT '章节数（小说用）' AFTER word_count;
ALTER TABLE work ADD COLUMN source_project_id BIGINT COMMENT '关联的 studio_project.id' AFTER chapter_count;
ALTER TABLE work ADD COLUMN extra_json TEXT COMMENT '扩展字段JSON' AFTER source_project_id;

-- 2. music_task 表 (AI 音乐模块)
CREATE TABLE IF NOT EXISTS music_task (
    id BIGINT PRIMARY KEY COMMENT '雪花ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    task_type VARCHAR(32) NOT NULL COMMENT '任务类型: song/instrumental/lyrics',
    mureka_task_id VARCHAR(64) COMMENT 'Mureka任务ID',
    title VARCHAR(256) COMMENT '作品标题',
    prompt VARCHAR(1024) COMMENT '风格提示词',
    lyrics TEXT COMMENT '歌词(song类型)',
    model VARCHAR(64) COMMENT '使用的模型',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/succeeded/failed',
    result_url VARCHAR(2048) COMMENT '生成的音频URL(R2永久链接)',
    duration INT COMMENT '时长(毫秒)',
    error_msg VARCHAR(1024) COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME COMMENT '完成时间',
    deleted TINYINT DEFAULT 0,
    INDEX idx_user_status (user_id, status, created_at DESC),
    INDEX idx_pending (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI音乐生成任务';
