package com.soundread.service;

import com.soundread.config.ai.LlmRouter;
import com.soundread.mapper.AiPromptRoleMapper;
import com.soundread.model.dto.SmartToneResult;
import com.soundread.model.entity.AiPromptRole;
import com.soundread.model.entity.User;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能语气匹配服务 — 向量检索 + LLM 混合分析
 *
 * <pre>
 * 核心流程:
 * 1. 启动时将 ai_prompt_role 全量 Embedding 建立内存索引
 * 2. 匹配时先向量检索 Top3 相似角色标签
 * 3. 以检索结果为 few-shot 示例注入 LLM prompt 精细化分析
 * 4. 返回结构化的 SmartToneResult
 * </pre>
 */
@Slf4j
@Service
public class ToneMatchService {

    private final LlmRouter llmRouter;
    private final AiPromptRoleMapper roleMapper;

    /** 本地 Embedding 模型 (复用与 SectionMemoryService 相同的模型) */
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    /** 内存向量存储 (角色数量有限，无需持久化) */
    private final EmbeddingStore<TextSegment> toneStore = new InMemoryEmbeddingStore<>();

    public ToneMatchService(LlmRouter llmRouter, AiPromptRoleMapper roleMapper) {
        this.llmRouter = llmRouter;
        this.roleMapper = roleMapper;
    }

    /**
     * 启动时预热：将 ai_prompt_role 全量向量化
     */
    @PostConstruct
    public void warmUpToneIndex() {
        try {
            List<AiPromptRole> roles = roleMapper.selectList(null);
            if (roles == null || roles.isEmpty()) {
                log.warn("[ToneMatch] ai_prompt_role 表为空，跳过预热");
                return;
            }
            for (AiPromptRole role : roles) {
                String text = role.getName() + " " + (role.getDescription() != null ? role.getDescription() : "")
                        + " " + (role.getTags() != null ? role.getTags() : "");
                TextSegment segment = TextSegment.from(text,
                        dev.langchain4j.data.document.Metadata.from("roleId", role.getId().toString())
                                .put("name", role.getName())
                                .put("tags", role.getTags() != null ? role.getTags() : ""));
                Embedding embedding = embeddingModel.embed(segment).content();
                toneStore.add(embedding, segment);
            }
            log.info("[ToneMatch] ✅ 语气知识库预热完成: {} 个角色标签已索引", roles.size());
        } catch (Exception e) {
            log.warn("[ToneMatch] 语气知识库预热失败（不影响主流程）", e);
        }
    }

    /**
     * 智能语气匹配 — 向量检索 + LLM 精细化
     *
     * @param user       当前用户
     * @param content    文章内容（前500字）
     * @param moduleType 模块类型（novel/podcast/drama/radio/ad 等）
     * @return SmartToneResult 结构化匹配结果
     */
    public SmartToneResult smartMatch(User user, String content, String moduleType) {
        // Step 1: 向量检索 Top3 相似角色
        List<RoleMatch> matches = searchSimilarRoles(content, 3);

        // Step 2: 构建 LLM prompt（带 RAG 结果作为 few-shot）
        ChatLanguageModel model = llmRouter.getChatModelWithFallback(user);
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是专业配音导演，请分析以下文本内容，推荐最合适的情感语气指令。\n");
        prompt.append("语气指令将用于 TTS 2.0 的 context_texts 控制情感合成。\n\n");

        if (!matches.isEmpty()) {
            prompt.append("【参考语气库（按相似度排序）】：\n");
            for (int i = 0; i < matches.size(); i++) {
                RoleMatch m = matches.get(i);
                prompt.append(String.format("%d. %s — %s (相似度%.0f%%)\n",
                        i + 1, m.name, m.tags, m.score * 100));
            }
            prompt.append("\n");
        }

        prompt.append("【内容类型】：").append(moduleType).append("\n");
        prompt.append("【待分析文本】：\n").append(content.substring(0, Math.min(content.length(), 500))).append("\n\n");
        prompt.append("请直接输出一行语气指令（如：压低声音、带点气声、语速放缓），不要输出任何其他内容。");

        String instruction;
        try {
            instruction = model.generate(prompt.toString()).trim();
            // 清理可能的格式包裹
            instruction = instruction.replaceAll("^[#\\[\\]用的语气]", "").trim();
            if (instruction.startsWith("用") && instruction.endsWith("的语气")) {
                instruction = instruction.substring(1, instruction.length() - 3);
            }
        } catch (Exception e) {
            log.warn("[ToneMatch] LLM 分析失败, fallback to RAG", e);
            if (!matches.isEmpty()) {
                instruction = matches.get(0).tags;
            } else {
                instruction = "自然流畅";
            }
        }

        // Step 3: 构建结果
        String source = matches.isEmpty() ? "llm" : "rag+llm";
        double confidence = matches.isEmpty() ? 0.6 : Math.min(matches.get(0).score + 0.1, 1.0);
        String sceneName = matches.isEmpty() ? null : matches.get(0).name;

        // 备选方案
        List<SmartToneResult.Alternative> alternatives = new ArrayList<>();
        for (int i = 1; i < matches.size(); i++) {
            alternatives.add(SmartToneResult.Alternative.builder()
                    .instruction(matches.get(i).tags)
                    .sceneName(matches.get(i).name)
                    .confidence(matches.get(i).score)
                    .build());
        }

        log.info("[ToneMatch] 🎭 智能匹配完成 | instruction={} | source={} | confidence={} | module={}",
                instruction, source, confidence, moduleType);

        return SmartToneResult.builder()
                .instruction(instruction)
                .sceneName(sceneName)
                .confidence(confidence)
                .source(source)
                .alternatives(alternatives)
                .build();
    }

    /**
     * 向量检索相似角色
     */
    private List<RoleMatch> searchSimilarRoles(String content, int maxResults) {
        List<RoleMatch> results = new ArrayList<>();
        try {
            Embedding queryEmbedding = embeddingModel.embed(content).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(0.3)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = toneStore.search(searchRequest);
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                TextSegment seg = match.embedded();
                results.add(new RoleMatch(
                        seg.metadata().getString("name"),
                        seg.metadata().getString("tags"),
                        match.score()));
            }
        } catch (Exception e) {
            log.warn("[ToneMatch] 向量检索失败", e);
        }
        return results;
    }

    /** 内部匹配结果 */
    private record RoleMatch(String name, String tags, double score) {
    }
}
