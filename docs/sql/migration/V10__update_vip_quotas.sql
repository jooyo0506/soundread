-- ============================================================
-- V10: VIP 配额方案（倍数制，取代无限制）
-- 配额设计原则：
--   tts_v2_daily_chars   = 统一覆盖所有情感合成入口（广播剧/有声绘本/情感电台等 Studio 合成 + 普通配音页）
--   ai_script_daily_count = AI 工作台创作次数（除小说外：广播剧/有声绘本/电台/讲解/文案/新闻）
--   novel_daily_chars     = 小说正文生成字数（独立计量，消耗最高）
--   podcast_daily_count  = AI 双人播客（独立计量，双倍TTS消耗）
--   music_daily_count    = AI 音乐生成（独立计量）
-- ============================================================

-- VIP 通用配额（月/年/终身有效期不同，配额数字相同）
SET @vip_quota = JSON_OBJECT(
    'tts_daily_chars',       2000,   -- TTS 1.0 基础配音，免费版 100字 × 20倍
    'tts_v2_daily_chars',    3000,   -- 情感语音合成（覆盖所有 Studio TTS + 配音页），VIP 专属
    'ask_daily_count',       -1,     -- 边听边问已从产品下线，字段保留不封印
    'ai_script_daily_count', 30,     -- AI 工作台创作（广播剧/绘本/电台/讲解/文案/新闻），免费版 3次 × 10倍
    'podcast_daily_count',   5,      -- AI 双人播客（独立配额，双倍TTS成本）
    'music_daily_count',     10,     -- AI 音乐生成
    'novel_daily_chars',     5000,   -- 小说正文字数（独立配额，最高消耗）
    'novel_max_projects',    20,     -- 小说项目数上限
    'storage_max_mb',        500,    -- 存储空间，免费版 50MB × 10倍
    'max_projects',          50,     -- 项目总数上限，免费版 5个 × 10倍
    'data_retention_days',   -1      -- 永久保存
);

UPDATE sys_tier_policy
SET quota_limits = @vip_quota,
    updated_at   = NOW()
WHERE tier_code IN ('vip_month', 'vip_year', 'vip_lifetime');

-- 验证
SELECT tier_code, quota_limits FROM sys_tier_policy
WHERE tier_code IN ('user', 'vip_month', 'vip_year', 'vip_lifetime');
