package com.soundread.agent.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品知识 RAG 服务
 *
 * <p>
 * 将用户视角的产品文档向量化并存入 pgvector，供 AI Agent 检索以回答
 * "声读能做什么" / "怎么换音色" 等产品相关问题。
 * </p>
 *
 * <h3>数据来源（仅用户视角文档，不含内部技术文档）</h3>
 * <ul>
 * <li>ai-workshop.md — AI 工作坊功能说明</li>
 * <li>product-modules.md — 产品功能模块总览</li>
 * <li>voice-library.md — 声音库说明</li>
 * <li>quota-system.md — 配额/套餐说明</li>
 * </ul>
 *
 * @author SoundRead
 */
@Slf4j
@Service
public class KnowledgeService {

    /** 向量维度：AllMiniLmL6V2 固定 384 维 */
    private static final int EMBEDDING_DIM = 384;

    /** 向量相似度搜索 topK */
    private static final int TOP_K = 3;

    /** 相似度阈值：低于此分数的结果不采用，避免注入无关内容 */
    private static final double MIN_SCORE = 0.45;

    /** 用于向量化的产品文档（类路径下） */
    private static final String[] PRODUCT_DOC_PATHS = {
            "/docs/ai-workshop.md",
            "/docs/product-modules.md",
            "/docs/voice-library.md",
            "/docs/quota-system.md"
    };

    @Value("${ai.vector-db.host:101.32.128.2}")
    private String pgHost;

    @Value("${ai.vector-db.port:5432}")
    private int pgPort;

    @Value("${ai.vector-db.database:soundread_vector}")
    private String pgDatabase;

    @Value("${ai.vector-db.user:postgres}")
    private String pgUser;

    @Value("${ai.vector-db.password:zy123456}")
    private String pgPassword;

    @Value("${ai.vector-db.table:product_knowledge}")
    private String tableName;

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;

    @PostConstruct
    public void init() {
        try {
            // 本地 ONNX 模型，无须 API Key，冷启动 ~1s
            embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            embeddingStore = PgVectorEmbeddingStore.builder()
                    .host(pgHost)
                    .port(pgPort)
                    .database(pgDatabase)
                    .user(pgUser)
                    .password(pgPassword)
                    .table(tableName)
                    .dimension(EMBEDDING_DIM)
                    .createTable(true) // 首次运行时自动建表
                    .build();

            // 检查是否已有索引，没有则导入文档
            if (isStoreEmpty()) {
                log.info("[KnowledgeService] 向量库为空，开始导入产品文档...");
                ingestDocs();
                log.info("[KnowledgeService] 产品文档导入完成");
            } else {
                log.info("[KnowledgeService] 产品知识向量库已就绪");
            }
        } catch (Exception e) {
            // pgvector 初始化失败不影响核心服务，RAG 降级为无增强模式
            log.warn("[KnowledgeService] RAG 初始化失败 (Agent 将在无知识增强模式下运行): {}", e.getMessage());
            embeddingStore = null;
        }
    }

    /**
     * 根据用户问题检索相关产品知识段落
     *
     * @param userQuery 用户原始问题
     * @return 相关知识片段列表，每条 ≤ 500 字符；无相关内容时返回空列表
     */
    public List<String> search(String userQuery) {
        if (embeddingStore == null || userQuery == null || userQuery.isBlank()) {
            return List.of();
        }
        try {
            var queryEmbedding = embeddingModel.embed(userQuery).content();
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(queryEmbedding)
                            .maxResults(TOP_K)
                            .minScore(MIN_SCORE)
                            .build());

            List<String> snippets = new ArrayList<>();
            for (var match : result.matches()) {
                snippets.add(match.embedded().text());
            }
            log.debug("[KnowledgeService] 检索到 {} 条相关知识, query={}", snippets.size(), userQuery);
            return snippets;
        } catch (Exception e) {
            log.warn("[KnowledgeService] 知识检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    // ═══════════════════════════════════════════════════
    // Private Helpers
    // ═══════════════════════════════════════════════════

    private boolean isStoreEmpty() {
        try {
            var dummyEmbedding = embeddingModel.embed("test").content();
            var result = embeddingStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(dummyEmbedding)
                            .maxResults(1)
                            .minScore(0.0)
                            .build());
            return result.matches().isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    private void ingestDocs() {
        var ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(400, 60))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        for (String docPath : PRODUCT_DOC_PATHS) {
            try (InputStream is = KnowledgeService.class.getResourceAsStream(docPath)) {
                if (is == null) {
                    log.warn("[KnowledgeService] 文档未找到（跳过）: {}", docPath);
                    continue;
                }
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Document doc = Document.from(content);
                ingestor.ingest(doc);
                log.info("[KnowledgeService] 已导入文档: {}", docPath);
            } catch (IOException e) {
                log.error("[KnowledgeService] 读取文档失败: {}", docPath, e);
            }
        }
    }
}
