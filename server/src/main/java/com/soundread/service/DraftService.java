package com.soundread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soundread.mapper.UserCreationMapper;
import com.soundread.model.entity.User;
import com.soundread.model.entity.UserCreation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 工作坊草稿服务
 *
 * <p>
 * 复用 {@code user_creation} 表，通过 {@code type = 'agent_draft'}
 * 区分 Agent 生成的草稿与普通 TTS 创作，无需新建数据表。
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftService {

    private static final String DRAFT_TYPE = "agent_draft";
    /** 每用户保留草稿数上限，防止无限积累 */
    private static final int MAX_DRAFTS_PER_USER = 30;

    private final UserCreationMapper userCreationMapper;

    /**
     * 保存 Agent 生成的草稿（文字/音频）
     *
     * @param user     当前用户
     * @param reply    AI 最终回复文本（已去除 think 标签）
     * @param audioUrl 合成的音频 URL，无音频时传 null
     */
    public UserCreation saveAgentDraft(User user, String reply, String audioUrl) {
        // 截取标题：取回复前 30 字
        String title = reply != null && reply.length() > 30
                ? reply.substring(0, 30) + "…"
                : (reply != null ? reply : "AI 草稿");

        UserCreation draft = new UserCreation();
        draft.setUserId(user.getId());
        draft.setType(DRAFT_TYPE);
        draft.setTitle(title);
        draft.setInputText(reply);
        draft.setAudioUrl(audioUrl);
        draft.setCreatedAt(LocalDateTime.now());
        draft.setIsPublished(0);
        draft.setDeleted(0);
        userCreationMapper.insert(draft);

        log.info("[DraftService] 草稿已保存: userId={}, draftId={}, hasAudio={}", user.getId(), draft.getId(),
                audioUrl != null);

        // 超限淘汰：删除最旧的草稿（保留最新 MAX_DRAFTS_PER_USER 条）
        trimOldDrafts(user.getId());

        return draft;
    }

    /**
     * 获取用户的草稿列表（按时间倒序，最多 30 条）
     */
    public List<UserCreation> listDrafts(Long userId) {
        return userCreationMapper.selectList(
                new LambdaQueryWrapper<UserCreation>()
                        .eq(UserCreation::getUserId, userId)
                        .eq(UserCreation::getType, DRAFT_TYPE)
                        .eq(UserCreation::getDeleted, 0)
                        .orderByDesc(UserCreation::getCreatedAt)
                        .last("LIMIT " + MAX_DRAFTS_PER_USER));
    }

    /**
     * 删除草稿（校验归属权）
     */
    public void deleteDraft(Long draftId, Long userId) {
        UserCreation draft = userCreationMapper.selectById(draftId);
        if (draft == null || !draft.getUserId().equals(userId) || !DRAFT_TYPE.equals(draft.getType())) {
            throw new RuntimeException("草稿不存在");
        }
        userCreationMapper.deleteById(draftId);
        log.info("[DraftService] 草稿已删除: draftId={}, userId={}", draftId, userId);
    }

    /**
     * 超限淘汰最旧草稿
     */
    private void trimOldDrafts(Long userId) {
        List<UserCreation> all = userCreationMapper.selectList(
                new LambdaQueryWrapper<UserCreation>()
                        .eq(UserCreation::getUserId, userId)
                        .eq(UserCreation::getType, DRAFT_TYPE)
                        .eq(UserCreation::getDeleted, 0)
                        .orderByAsc(UserCreation::getCreatedAt));

        if (all.size() > MAX_DRAFTS_PER_USER) {
            List<UserCreation> toDelete = all.subList(0, all.size() - MAX_DRAFTS_PER_USER);
            toDelete.forEach(d -> userCreationMapper.deleteById(d.getId()));
            log.info("[DraftService] 淘汰旧草稿 {} 条, userId={}", toDelete.size(), userId);
        }
    }
}
