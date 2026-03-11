-- ============================================================
-- SoundRead 性能优化索引补充 V13（幂等版本）
-- 已存在的索引自动跳过，可重复执行
-- ⚠️ 建议业务低峰（凌晨2-4点）执行
-- ============================================================

-- 创建辅助存储过程（执行完后自动删除）
DROP PROCEDURE IF EXISTS _add_index_if_not_exists;

DELIMITER //
CREATE PROCEDURE _add_index_if_not_exists(
    p_table  VARCHAR(64),
    p_index  VARCHAR(64),
    p_def    TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name   = p_table
          AND index_name   = p_index
    ) THEN
        SET @_sql = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_def);
        PREPARE _stmt FROM @_sql;
        EXECUTE _stmt;
        DEALLOCATE PREPARE _stmt;
        SELECT CONCAT('✅ Created: ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('⏭ Skipped: ', p_table, '.', p_index, ' (already exists)') AS result;
    END IF;
END //
DELIMITER ;

-- ============================================================
-- 1. user 表：登录/查询最高频字段
-- ============================================================
CALL _add_index_if_not_exists('user', 'idx_phone',     'INDEX idx_phone (phone)');
CALL _add_index_if_not_exists('user', 'idx_tier_code', 'INDEX idx_tier_code (tier_code)');

-- ============================================================
-- 2. user_creation 表：创作记录查询
-- ============================================================
CALL _add_index_if_not_exists('user_creation', 'idx_user_type_created',      'INDEX idx_user_type_created (user_id, type, created_at)');
CALL _add_index_if_not_exists('user_creation', 'idx_is_published_created',   'INDEX idx_is_published_created (is_published, created_at)');

-- ============================================================
-- 3. work 表：发现页核心查询（最重要）
-- ============================================================
CALL _add_index_if_not_exists('work', 'idx_review_heat',      'INDEX idx_review_heat (review_status, heat_score)');
CALL _add_index_if_not_exists('work', 'idx_review_type_heat', 'INDEX idx_review_type_heat (review_status, content_type, heat_score)');
CALL _add_index_if_not_exists('work', 'idx_user_review',      'INDEX idx_user_review (user_id, review_status)');
CALL _add_index_if_not_exists('work', 'idx_review_created',   'INDEX idx_review_created (review_status, created_at)');

-- ============================================================
-- 4. vip_order 表
-- ============================================================
CALL _add_index_if_not_exists('vip_order', 'idx_vip_user_id', 'INDEX idx_vip_user_id (user_id)');
CALL _add_index_if_not_exists('vip_order', 'uk_order_no',     'UNIQUE INDEX uk_order_no (order_no)');

-- ============================================================
-- 5. tts_task 表：异步任务轮询
-- ============================================================
CALL _add_index_if_not_exists('tts_task', 'idx_tts_user_status', 'INDEX idx_tts_user_status (user_id, status)');

-- ============================================================
-- 6. music_task 表：MusicService 高频轮询（每5秒）
-- ============================================================
CALL _add_index_if_not_exists('music_task', 'idx_music_user_status',   'INDEX idx_music_user_status (user_id, status)');
CALL _add_index_if_not_exists('music_task', 'idx_music_status_created', 'INDEX idx_music_status_created (status, created_at)');

-- ============================================================
-- 7. studio_project 表
-- ============================================================
CALL _add_index_if_not_exists('studio_project', 'idx_project_user_created', 'INDEX idx_project_user_created (user_id, created_at)');

-- 清理辅助存储过程
DROP PROCEDURE IF EXISTS _add_index_if_not_exists;

-- ============================================================
-- 验证（执行后 type 列不应再是 ALL）
-- ============================================================
EXPLAIN SELECT * FROM work WHERE review_status = 'approved' ORDER BY heat_score DESC LIMIT 20;
EXPLAIN SELECT * FROM user_creation WHERE user_id = 1 AND type = 'podcast' ORDER BY created_at DESC;
EXPLAIN SELECT * FROM `user` WHERE phone = '18571696470';

-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;
SET GLOBAL log_queries_not_using_indexes = ON;
SHOW VARIABLES LIKE 'slow_query_log_file';
