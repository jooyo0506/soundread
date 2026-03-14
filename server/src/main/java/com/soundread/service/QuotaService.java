package com.soundread.service;

import com.soundread.common.exception.QuotaExceededException;
import com.soundread.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Duration;

/**
 * 配额服务 — Redis 计数器实现免费/VIP 差异化限额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final StringRedisTemplate redisTemplate;
    private final TierPolicyService tierPolicyService;

    /**
     * 检查并扣减 TTS 1.0 基础合成字数配额
     */
    public void checkAndDeductTextQuota(User user, int charCount) {
        int limit = getQuotaLimit(user, "ttsDailyChars");
        if (limit == -1)
            return;
        long used = getDailyUsage(user.getId(), "text");
        if (used + charCount > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日基础合成额度 (%d 字) 已用完，请升级解锁更多", limit));
        }
        increment(quotaKey(user.getId(), "text"), charCount);
    }

    /**
     * 检查并扣减 TTS 2.0 情感合成字数配额
     */
    public void checkAndDeductTextV2Quota(User user, int charCount) {
        int limit = getQuotaLimit(user, "ttsV2DailyChars");
        if (limit == -1)
            return;
        long used = getDailyUsage(user.getId(), "text_v2");
        if (used + charCount > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日情感合成额度 (%d 字) 已用完，请升级解锁更多", limit));
        }
        increment(quotaKey(user.getId(), "text_v2"), charCount);
    }

    /**
     * 检查并扣减"边听边问"次数配额
     */
    public void checkAndDeductAskQuota(User user) {
        int limit = getQuotaLimit(user, "askDailyCount");
        if (limit == -1)
            return;
        long used = getDailyUsage(user.getId(), "ask");
        if (used >= limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日互动额度 (%d 次) 已用完，请升级解锁更多", limit));
        }
        increment(quotaKey(user.getId(), "ask"), 1);
    }

    /**
     * 检查并扣减 AI 剧本每日运行配额
     */
    public void checkAndDeductAiScriptQuota(User user) {
        int limit = getQuotaLimit(user, "aiScriptDailyCount");
        if (limit == -1)
            return;
        long used = getDailyUsage(user.getId(), "ai_script");
        if (used >= limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日 AI 剧本生成配额 (%d 次) 已用完，请明天再来", limit));
        }
        increment(quotaKey(user.getId(), "ai_script"), 1);
    }

    /**
     * 仅检查 AI 播客每日配额（不扣减）
     * 在生成开始时调用，超额时立即拒绝
     */
    public void checkPodcastQuota(User user) {
        int limit = getQuotaLimit(user, "podcastDailyCount");
        if (limit == -1)
            return;
        long used = getDailyUsage(user.getId(), "podcast");
        if (used >= limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日 AI 播客配额 (%d 次) 已用完，请明天再来 🎙️", limit));
        }
    }

    /**
     * 扣减 AI 播客配额（仅在生成成功完成后调用）
     */
    public void deductPodcastQuota(User user) {
        int limit = getQuotaLimit(user, "podcastDailyCount");
        if (limit == -1)
            return;
        String key = quotaKey(user.getId(), "podcast");
        increment(key, 1);
        log.info("[Quota] 播客配额已扣减: userId={} 限额={}", user.getId(), limit);
    }

    /**
     * 检查并扣减有声小说每日合成字数配额
     */
    public void checkAndDeductNovelCharsQuota(User user, int charCount) {
        int limit = getQuotaLimit(user, "novelDailyChars");
        if (limit == -1)
            return;
        long used = getDailyUsage(user.getId(), "novel");
        if (used + charCount > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日小说合成额度 (%d 字) 已用完，请升级解锁更多 📖", limit));
        }
        increment(quotaKey(user.getId(), "novel"), charCount);
    }

    /**
     * 检查并扣减 AI 音乐每日生成配额
     */
    public void checkAndDeductMusicQuota(User user) {
        int limit = getQuotaLimit(user, "musicDailyCount");
        if (limit == -1) {
            return; // 无限额
        }

        long used = getDailyUsage(user.getId(), "music");
        if (used >= limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日 AI 音乐配额 (%d 次) 已用完，请明天再来 🎵", limit));
        }
        increment(quotaKey(user.getId(), "music"), 1);
    }

    /**
     * 根据当前用户等级，动态拉取额度上限值
     */
    private int getQuotaLimit(User user, String type) {
        com.soundread.model.entity.SysTierPolicy policy = tierPolicyService.getByTierCode(user.getTierCode());
        if (policy == null || policy.getQuotaLimits() == null) {
            // 找不到策略时返回 -1（无限制），避免配置缺失导致所有功能被封锁
            log.warn("[QuotaService] 未找到用户配额策略: userId={} tierCode={}", user.getId(), user.getTierCode());
            return -1;
        }
        return switch (type) {
            case "ttsDailyChars" -> policy.getQuotaLimits().getTtsDailyChars();
            case "ttsV2DailyChars" -> policy.getQuotaLimits().getTtsV2DailyChars();
            case "askDailyCount" -> policy.getQuotaLimits().getAskDailyCount();
            case "aiScriptDailyCount" -> policy.getQuotaLimits().getAiScriptDailyCount();
            case "podcastDailyCount" -> policy.getQuotaLimits().getPodcastDailyCount();
            case "novelDailyChars" -> policy.getQuotaLimits().getNovelDailyChars();
            case "musicDailyCount" -> policy.getQuotaLimits().getMusicDailyCount();
            default -> 0;
        };
    }

    /**
     * 检查专属功能权限 (可保留历史习惯判定，或未来改为判断 featureFlags)
     */
    public void checkVipRequired(User user, String featureName) {
        if (!user.isVip()) {
            throw new QuotaExceededException(featureName + " 为高级功能，请先开通对应等级");
        }
    }

    /**
     * 获取当日已使用额度
     */
    public long getDailyUsage(Long userId, String feature) {
        String key = quotaKey(userId, feature);
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0;
    }

    /**
     * 获取累计型已使用额度（如克隆总次数）
     */
    public long getTotalUsage(Long userId, String feature) {
        String key = "quota:" + userId + ":" + feature;
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0;
    }

    private long increment(String key, int amount) {
        Long result = redisTemplate.opsForValue().increment(key, amount);
        // 设置到当日结束的过期时间
        redisTemplate.expire(key, Duration.ofDays(1));
        return result != null ? result : 0;
    }

    private String quotaKey(Long userId, String feature) {
        return "quota:" + userId + ":" + LocalDate.now() + ":" + feature;
    }
}
