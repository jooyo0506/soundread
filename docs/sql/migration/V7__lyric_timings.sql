-- 歌词时间戳字段 (来自 Mureka recognize API)
ALTER TABLE music_task ADD COLUMN lyric_timings TEXT NULL COMMENT '歌词时间戳 JSON [{start,end,text}]';
