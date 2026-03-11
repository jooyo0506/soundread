-- ============================================================
-- V10: VIP 配额改革 — 从无限(-1)改为 10 倍免费版的具体数字
-- 免费版基线: tts_daily_chars=100, ask_daily_count=3, ai_script_daily_count=3
-- ============================================================

-- VIP 通用配额 JSON（月/年/终身共用同一配额数字，差异体现在有效期）
SET @vip_quota = JSON_OBJECT(
    'tts_daily_chars',       2000,   -- 免费版 100字 × 20倍
    'tts_v2_daily_chars',    2000,   -- VIP 专属情感语音，2000字/天
    'ask_daily_count',       30,     -- 免费版 3次 × 10倍
    'ai_script_daily_count', 50,     -- AI 工作台创作次数
    'podcast_daily_count',   10,     -- AI 双人播客，VIP 专属
    'music_daily_count',     10,     -- AI 音乐生成，VIP 专属
    'novel_daily_chars',     10000,  -- 小说创作字数，VIP 专属
    'novel_max_projects',    20,     -- 小说项目上限
    'storage_max_mb',        500,    -- 免费版 50MB × 10倍
    'max_projects',          50,     -- 免费版 5个 × 10倍
    'data_retention_days',   -1      -- 永久保存
);

UPDATE sys_tier_policy
SET quota_limits = @vip_quota,
    updated_at   = NOW()
WHERE tier_code IN ('vip_month', 'vip_year', 'vip_lifetime');

-- 验证
SELECT tier_code, quota_limits FROM sys_tier_policy
WHERE tier_code IN ('user', 'vip_month', 'vip_year', 'vip_lifetime');
