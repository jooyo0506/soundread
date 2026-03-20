package com.soundread.service;

import com.soundread.adapter.LlmAdapter;
import com.soundread.adapter.WhisperAdapter;
import com.soundread.mapper.AiInteractionMapper;
import com.soundread.model.entity.AiInteraction;
import com.soundread.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * "边听边问" AI 语音交互服务
 * 闭环流程: 用户语音 → ASR 转文字 → LLM 回答 → TTS 合成 → 语音回传
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInteractionService {

    private final WhisperAdapter whisperAdapter;
    private final LlmAdapter llmAdapter;
    private final AiInteractionMapper aiInteractionMapper;
    private final QuotaService quotaService;

    private static final String INTERACTION_PROMPT = """
            你是"声读"智能助手，用户正在收听一段音频内容，他/她提出了一个问题。
            请根据上下文简洁、准确地回答用户的问题。回答控制在100字以内，语气自然口语化。
            """;

    /**
     * 处理一次完整的"边听边问"交互
     * 
     * @param user        当前用户
     * @param audioData   用户语音消息
     * @param contextText 当前音频的文本上下文
     * @param sessionId   会话 ID
     * @return AI 回答文本 (音频由 WebSocket 异步推送)
     */
    public String processInteraction(User user, byte[] audioData, String contextText, String sessionId) {
        // 1. 检查配额
        quotaService.checkAndDeductAskQuota(user);

        // 2. ASR: 语音 → 文字
        String question = whisperAdapter.transcribe(audioData, "question.wav");
        log.info("用户提问 (ASR): {}", question);

        // 3. LLM: 结合上下文回答
        String contextPrompt = INTERACTION_PROMPT;
        if (contextText != null && !contextText.isBlank()) {
            contextPrompt += "\n\n当前音频内容摘要:\n" + contextText;
        }
        String answer = llmAdapter.chat(contextPrompt, question);
        log.info("AI 回答: {}", answer);

        // 4. 记录交互
        AiInteraction interaction = new AiInteraction();
        interaction.setUserId(user.getId());
        interaction.setSessionId(sessionId != null ? sessionId : UUID.randomUUID().toString());
        interaction.setQuestion(question);
        interaction.setAnswer(answer);
        aiInteractionMapper.insert(interaction);

        return answer;
    }

    /**
     * AI 翻译同传 — 将文本翻译后合成
     */
    public String translateForTts(String text, String targetLanguage) {
        return llmAdapter.translate(text, targetLanguage);
    }
}
