package com.soundread.service;

import com.soundread.config.ai.LlmRouter;
import com.soundread.model.entity.StudioSection;
import com.soundread.model.entity.User;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 混合记忆服务 — 双引擎架构（面试亮点 ⭐）
 *
 * <pre>
 * Engine 1: 摘要压缩记忆 (Summary Compression Memory)
 *   → 每段生成后 LLM 自动产出 ≤50字摘要
 *   → 续写时注入全部摘要 → 保证叙事连贯性
 *
 * Engine 2: RAG 向量检索记忆 (Retrieval-Augmented Generation)
 *   → 每段内容做 Embedding 存入 pgvector（PG 持久化）
 *   → 续写时根据用户指令语义检索最相关的历史段落
 *   → 解决"角色在第3段出场、第50段再提到时需要回忆初始设定"的问题
 *
 * 面试话术:
 * "我设计了双引擎混合记忆架构：摘要记忆保证叙事连续性——每段生成后 LLM
 *  自动产出50字摘要存DB，续写时注入全部摘要作为长期记忆；RAG 记忆使用
 *  PG + pgvector 持久化向量存储 + AllMiniLmL6V2 本地 Embedding 模型，
 *  将每段内容向量化存储。续写时根据用户新指令做语义检索，把最相关的历史
 *  段落注入 context，解决角色设定遗忘的问题。"
 * </pre>
 *
 * @author SoundRead
 */
@Slf4j
@Service
public class SectionMemoryService {

    private final LlmRouter llmRouter;

    /** 本地 Embedding 模型 (all-MiniLM-L6-v2, ONNX 本地运行, 无需 API) */
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    /** PgVector 持久化向量存储（应用重启不丢索引） */
    private final EmbeddingStore<TextSegment> embeddingStore;

    public SectionMemoryService(
            LlmRouter llmRouter,
            @Value("${ai.vector-db.host:localhost}") String pgHost,
            @Value("${ai.vector-db.port:5432}") int pgPort,
            @Value("${ai.vector-db.database:soundread_vector}") String pgDatabase,
            @Value("${ai.vector-db.user:postgres}") String pgUser,
            @Value("${ai.vector-db.password:123456}") String pgPassword,
            @Value("${ai.vector-db.table:section_embeddings}") String pgTable,
            @Value("${ai.vector-db.dimension:384}") int dimension) {
        this.llmRouter = llmRouter;

        // PgVector 持久化向量存储（自动建表，应用重启不丢数据）
        this.embeddingStore = PgVectorEmbeddingStore.builder()
                .host(pgHost)
                .port(pgPort)
                .database(pgDatabase)
                .user(pgUser)
                .password(pgPassword)
                .table(pgTable)
                .dimension(dimension)
                .createTable(true) // 自动创建表（首次启动）
                .build();

        log.info("[SectionMemory] ✅ PgVector 持久化向量存储初始化完成: {}:{}/{}", pgHost, pgPort, pgDatabase);
    }

    // ========================
    // Engine 1: 摘要压缩记忆
    // ========================

    /**
     * 为一段内容生成 AI 摘要 (≤50字)
     */
    public String generateSummary(User user, String content, String title) {
        try {
            ChatLanguageModel model = llmRouter.getChatModelWithFallback(user);
            String prompt = "用一句话（50字以内）概括以下内容的核心情节和情感状态，直接输出摘要，不要任何前缀：\n\n"
                    + "标题：" + title + "\n"
                    + "内容：" + (content.length() > 500 ? content.substring(0, 500) : content);
            String summary = model.generate(prompt);
            if (summary != null && summary.length() > 80) {
                summary = summary.substring(0, 80);
            }
            log.info("[SectionMemory] 📝 摘要生成完成: {}", summary);
            return summary;
        } catch (Exception e) {
            log.warn("[SectionMemory] 摘要生成失败, 降级为截取前50字", e);
            return content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }
    }

    /**
     * 构建摘要记忆上下文 — 将全部段落的摘要拼接为记忆链
     */
    public String buildSummaryMemory(List<StudioSection> sections) {
        if (sections == null || sections.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder("【故事记忆链（摘要压缩）】：\n");
        for (int i = 0; i < sections.size(); i++) {
            StudioSection s = sections.get(i);
            String summary = s.getSummary();
            if (summary == null || summary.isBlank()) {
                summary = s.getTitle();
            }
            sb.append(String.format("%d. [%s] %s\n", i + 1, s.getTitle(), summary));
        }
        return sb.toString();
    }

    // ========================
    // Engine 2: RAG 向量检索 (PgVector 持久化)
    // ========================

    /**
     * 将段落内容向量化并存入 PgVector
     */
    public void indexSection(Long projectId, StudioSection section) {
        try {
            String textToEmbed = section.getTitle() + "\n" + section.getContent();
            TextSegment segment = TextSegment.from(textToEmbed,
                    Metadata.from("projectId", projectId.toString())
                            .put("sectionId", section.getId().toString())
                            .put("title", section.getTitle())
                            .put("index", String.valueOf(section.getSectionIndex())));

            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            log.info("[SectionMemory] 🔍 向量索引完成 (PgVector 持久化): project={}, section={}, title={}",
                    projectId, section.getId(), section.getTitle());
        } catch (Exception e) {
            log.warn("[SectionMemory] 向量索引失败（不影响主流程）", e);
        }
    }

    /**
     * 根据用户输入语义检索最相关的历史段落
     */
    public String retrieveRelevantContext(Long projectId, String query, int maxResults) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(0.5)
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = result.matches();

            if (matches.isEmpty())
                return "";

            // 过滤出属于当前项目的段落
            StringBuilder sb = new StringBuilder("【RAG 语义检索 · 相关历史段落】：\n");
            int count = 0;
            for (EmbeddingMatch<TextSegment> match : matches) {
                TextSegment seg = match.embedded();
                String matchProjectId = seg.metadata().getString("projectId");
                if (matchProjectId != null && matchProjectId.equals(projectId.toString())) {
                    String title = seg.metadata().getString("title");
                    String text = seg.text();
                    if (text.length() > 200)
                        text = text.substring(0, 200) + "...";
                    sb.append(String.format("- [%s](相似度%.0f%%) %s\n",
                            title, match.score() * 100, text));
                    count++;
                }
            }
            if (count == 0)
                return "";

            log.info("[SectionMemory] 🎯 RAG 检索到 {} 条相关段落 (query: {})",
                    count, query.substring(0, Math.min(30, query.length())));
            return sb.toString();
        } catch (Exception e) {
            log.warn("[SectionMemory] RAG 检索失败（不影响主流程）", e);
            return "";
        }
    }

    /**
     * 预热不再需要 — PgVector 是持久化存储，数据已在数据库中
     * 保留方法签名以兼容 StudioService 调用
     */
    public void warmUpIndex(Long projectId, List<StudioSection> sections) {
        // PgVector 持久化存储无需预热，数据已在数据库中
        // 此方法仅保留兼容性
        log.debug("[SectionMemory] PgVector 持久化模式，跳过预热 (project={})", projectId);
    }
}
