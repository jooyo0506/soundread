-- 摘要压缩记忆链：为 studio_section 添加 summary 字段
-- 每段生成后 AI 自动生成 ≤50字摘要，供续写时作为长期记忆注入

ALTER TABLE studio_section ADD COLUMN summary VARCHAR(200) DEFAULT NULL COMMENT 'AI摘要（摘要压缩记忆链）';
