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
        if (limit == -1) {
            return; // 无限额
        }

        String key = quotaKey(user.getId(), "text");
        long used = increment(key, charCount);
        if (used > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日基础合成额度 (%d 字) 已用完，请升级解锁更多", limit));
        }
    }

    /**
     * 检查并扣减 TTS 2.0 情感合成字数配额
     */
    public void checkAndDeductTextV2Quota(User user, int charCount) {
        int limit = getQuotaLimit(user, "ttsV2DailyChars");
        if (limit == -1) {
            return; // 无限额
        }

        String key = quotaKey(user.getId(), "text_v2");
        long used = increment(key, charCount);
        if (used > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日情感合成额度 (%d 字) 已用完，请升级解锁更多", limit));
        }
    }

    /**
     * 检查并扣减"边听边问"次数配额
     */
    public void checkAndDeductAskQuota(User user) {
        int limit = getQuotaLimit(user, "askDailyCount");
        if (limit == -1) {
            return; // 无限额
        }

        String key = quotaKey(user.getId(), "ask");
        long used = increment(key, 1);
        if (used > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日互动额度 (%d 次) 已用完，请升级解锁更多", limit));
        }
    }

    /**
     * 检查并扣减 AI 剧本每日运行配额
     */
    public void checkAndDeductAiScriptQuota(User user) {
        int limit = getQuotaLimit(user, "aiScriptDailyCount");
        if (limit == -1) {
            return; // 无限额
        }

        String key = quotaKey(user.getId(), "ai_script");
        long used = increment(key, 1);
        if (used > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日 AI 剧本生成配额 (%d 次) 已用完，请明天再来", limit));
        }
    }

    /**
     * 检查并扣减 AI 播客每日生成配额
     */
    public void checkAndDeductPodcastQuota(User user) {
        int limit = getQuotaLimit(user, "podcastDailyCount");
        if (limit == -1) {
            return; // 无限额
        }

        String key = quotaKey(user.getId(), "podcast");
        long used = increment(key, 1);
        if (used > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日 AI 播客配额 (%d 次) 已用完，请明天再来 🎙️", limit));
        }
    }

    /**
     * 检查并扣减有声小说每日合成字数配额
     */
    public void checkAndDeductNovelCharsQuota(User user, int charCount) {
        int limit = getQuotaLimit(user, "novelDailyChars");
        if (limit == -1) {
            return; // 无限额
        }

        String key = quotaKey(user.getId(), "novel");
        long used = increment(key, charCount);
        if (used > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级每日小说合成额度 (%d 字) 已用完，请升级解锁更多 📖", limit));
        }
    }

    /**
     * 检查并扣减声音克隆总次数配额（累计型，非每日重置）
     */
    public void checkAndDeductCloneQuota(User user) {
        int limit = getQuotaLimit(user, "cloneTotalCount");
        if (limit == -1) {
            return; // 无限额
        }

        // 克隆是累计型，不按日重置
        String key = "quota:" + user.getId() + ":clone_total";
        Long result = redisTemplate.opsForValue().increment(key, 1);
        long used = result != null ? result : 0;
        if (used > limit) {
            throw new QuotaExceededException(
                    String.format("您的等级克隆次数 (%d 次) 已用完，请升级解锁更多 🎤", limit));
        }
    }

    /**
     * 根据当前用户等级，动态拉取额度上限值
     */
    private int getQuotaLimit(User user, String type) {
        com.soundread.model.entity.SysTierPolicy policy = tierPolicyService.getByTierCode(user.getTierCode());
        if (policy == null || policy.getQuotaLimits() == null) {
            return 0; // 找不到策略则默认限额0
        }
        return switch (type) {
            case "ttsDailyChars" -> policy.getQuotaLimits().getTtsDailyChars();
            case "ttsV2DailyChars" -> policy.getQuotaLimits().getTtsV2DailyChars();
            case "askDailyCount" -> policy.getQuotaLimits().getAskDailyCount();
            case "aiScriptDailyCount" -> policy.getQuotaLimits().getAiScriptDailyCount();
            case "podcastDailyCount" -> policy.getQuotaLimits().getPodcastDailyCount();
            case "novelDailyChars" -> policy.getQuotaLimits().getNovelDailyChars();
            case "cloneTotalCount" -> policy.getQuotaLimits().getCloneTotalCount();
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
