package com.soundread.service;

import com.soundread.adapter.LlmAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * AI 播客服务
 * 支持三种内容来源: 主题生成、URL 提取、原文直读
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastService {

    private final LlmAdapter llmAdapter;

    private static final String PODCAST_DIALOG_PROMPT = """
            你是一位优秀的播客编剧。将以下内容改编成双人播客对话脚本。

            格式要求:
            1. 主播A和主播B轮流发言，用 [A] 和 [B] 标记
            2. 对话语气要自然、口语化，像真实的聊天
            3. 适当加入语气词 ("嗯"、"对对对"、"哈哈"、"这个点挺有意思")
            4. A负责抛出观点/提问，B负责分析/总结/追问
            5. 自然地插入过渡句，避免生硬切换
            6. 对话控制在 10-15 轮

            示例:
            [A] 诶，最近你有没有关注特斯拉的财报？我觉得挺有意思的。
            [B] 嗯，看了看了。说实话这次的数据挺让人意外的...
            """;

    private static final String TOPIC_EXPAND_PROMPT = """
            你是一位资深的内容策划。根据给定的主题，生成一段详细的讨论素材 (500-800字)。
            内容要有深度、有观点、有数据佐证。用自然段落，不要用标题或列表。
            """;

    /**
     * 根据来源类型生成播客脚本
     */
    public String generatePodcastScript(String sourceType, String content) {
        String dialogContent;

        switch (sourceType) {
            case "topic" -> {
                // 先扩展主题为详细内容，再生成对话
                String expanded = llmAdapter.chat(TOPIC_EXPAND_PROMPT, content);
                dialogContent = llmAdapter.chat(PODCAST_DIALOG_PROMPT, expanded);
            }
            case "url" -> {
                // URL 内容先做摘要，再生成对话
                String summary = llmAdapter.summarizeUrl(content);
                dialogContent = llmAdapter.chat(PODCAST_DIALOG_PROMPT, summary);
            }
            case "text" -> {
                // 原文直接生成对话
                dialogContent = llmAdapter.chat(PODCAST_DIALOG_PROMPT, content);
            }
            default -> throw new IllegalArgumentException("不支持的内容来源类型: " + sourceType);
        }

        return dialogContent;
    }

    /**
     * 流式生成播客脚本
     */
    public void generatePodcastScriptStream(String sourceType, String content, Consumer<String> onChunk) {
        String sourceContent;

        switch (sourceType) {
            case "topic" -> sourceContent = llmAdapter.chat(TOPIC_EXPAND_PROMPT, content);
            case "url" -> sourceContent = llmAdapter.summarizeUrl(content);
            case "text" -> sourceContent = content;
            default -> throw new IllegalArgumentException("不支持的内容来源类型: " + sourceType);
        }

        llmAdapter.chatStream(PODCAST_DIALOG_PROMPT, sourceContent, onChunk);
    }
}
