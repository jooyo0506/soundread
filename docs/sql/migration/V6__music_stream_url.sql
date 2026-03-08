-- ============================================================
-- 音乐流式播放 — 添加 stream_url 字段
-- 执行方式: mysql -uroot -p123456 sound_read < music_stream_url.sql
-- ============================================================

ALTER TABLE music_task ADD COLUMN stream_url VARCHAR(2048) COMMENT '流式播放URL(HLS m3u8)' AFTER result_url;
