package com.soundread.service;

import com.soundread.adapter.R2StorageAdapter;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soundread.agent.creative.CreativeAgent;
import com.soundread.agent.creative.CreativeAgentFactory;
import com.soundread.agent.creative.DramaAgent;
import com.soundread.config.ai.LlmRouter;
import com.soundread.mapper.CreativeTemplateMapper;
import com.soundread.mapper.StudioProjectMapper;
import com.soundread.mapper.StudioSectionMapper;
import com.soundread.mapper.UserCreationMapper;
import com.soundread.mapper.WorkMapper;
import com.soundread.model.entity.CreativeTemplate;
import com.soundread.model.entity.StudioProject;
import com.soundread.model.entity.StudioSection;
import com.soundread.model.entity.User;
import com.soundread.model.entity.Work;
import com.soundread.model.entity.UserCreation;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * AI 创作工坊核心服务
 *
 * <p>
 * 本服务是 Studio 模块的核心入口，统一管理以下职责：
 * <ol>
 * <li><b>模板管理</b>：查询创作模板（novel/drama/podcast/radio 等）</li>
 * <li><b>项目 CRUD</b>：创建、查询、更新、删除创作项目（StudioProject）</li>
 * <li><b>AI 生成</b>：通过 LlmRouter 调用 AI 流式/同步生成灵感、大纲、正文、改写</li>
 * <li><b>段落管理</b>：对 StudioSection 进行增删改查</li>
 * <li><b>发布 / 下架</b>：将项目内容写入 UserCreation 并在发现页创建 Work</li>
 * <li><b>音频拼接</b>：将各段音频 URL 合并上传至 R2</li>
 * </ol>
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudioService {

    private final CreativeTemplateMapper templateMapper;
    private final StudioProjectMapper projectMapper;
    private final StudioSectionMapper sectionMapper;
    private final LlmRouter llmRouter;
    private final AuthService authService;
    private final SectionMemoryService sectionMemory;
    private final UserCreationMapper userCreationMapper;
    private final WorkMapper workMapper;
    private final R2StorageAdapter r2StorageAdapter;
    private final CreativeAgentFactory agentFactory;

    // ========================
    // 1. 模板管理
    // ========================

    /**
     * 获取所有启用的创作模板列表，按排序权重升序返回
     */
    public List<CreativeTemplate> listTemplates() {
        return templateMapper.selectList(
                new LambdaQueryWrapper<CreativeTemplate>()
                        .eq(CreativeTemplate::getEnabled, 1)
                        .orderByAsc(CreativeTemplate::getSortOrder));
    }

    /**
     * 根据 typeCode 查询单个创作模板
     *
     * @param typeCode 模板类型编码，如 novel / drama / podcast / radio
     * @return 对应模板实体，不存在时返回 null
     */
    public CreativeTemplate getTemplate(String typeCode) {
        return templateMapper.selectOne(
                new LambdaQueryWrapper<CreativeTemplate>()
                        .eq(CreativeTemplate::getTypeCode, typeCode));
    }

    // ========================
    // 2. 项目 CRUD
    // ========================

    /**
     * 创建新的创作项目
     *
     * <p>
     * 根据 typeCode 查询模板，用模板信息初始化项目；
     * 若未传标题则使用「模板名 · 新项目」作为默认标题。
     * </p>
     *
     * @param typeCode    创作类型编码
     * @param title       项目标题（可为 null，使用默认值）
     * @param inspiration 用户灵感描述
     * @return 已持久化的 StudioProject 实体
     */
    public StudioProject createProject(String typeCode, String title, String inspiration) {
        User user = authService.getCurrentUser();
        CreativeTemplate template = getTemplate(typeCode);
        if (template == null) {
            throw new RuntimeException("创作模板不存在，typeCode=" + typeCode);
        }

        StudioProject project = new StudioProject();
        project.setUserId(user.getId());
        project.setTemplateId(template.getId());
        project.setTypeCode(typeCode);
        project.setTitle(title != null && !title.isEmpty() ? title : template.getTypeName() + " · 新项目");
        project.setInspiration(inspiration);
        project.setStatus("draft");
        project.setTotalSections(0);
        projectMapper.insert(project);

        return project;
    }

    /**
     * 查询当前登录用户的所有创作项目，按最后更新时间降序排列
     */
    public List<StudioProject> listMyProjects() {
        Long userId = StpUtil.getLoginIdAsLong();
        return projectMapper.selectList(
                new LambdaQueryWrapper<StudioProject>()
                        .eq(StudioProject::getUserId, userId)
                        .orderByDesc(StudioProject::getUpdatedAt));
    }

    /**
     * 获取单个项目详情，并进行权限校验
     *
     * <p>
     * 若项目不存在或不属于当前用户，抛出 RuntimeException。
     * </p>
     *
     * @param projectId 项目 ID
     * @return 当前用户的 StudioProject 实体
     * @throws RuntimeException 项目不存在或无权访问
     */
    public StudioProject getProject(Long projectId) {
        StudioProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权访问该项目");
        }
        return project;
    }

    /**
     * 校验 StudioSection 是否属于当前用户的项目（权限检查）
     *
     * <p>
     * 内部调用 {@link #getProject(Long)} 完成鉴权，抛出异常即代表无权操作。
     * </p>
     */
    public void checkSectionOwnership(StudioSection section) {
        if (section.getProjectId() == null) {
            throw new RuntimeException("段落没有关联项目 ID，无法校验权限");
        }
        // 通过 getProject 完成项目归属与权限验证
        getProject(section.getProjectId());
    }

    /**
     * 更新项目基本信息（灵感、状态等），已通过 {@link #getProject(Long)} 完成权限校验后方可调用
     */
    public void updateProject(StudioProject project) {
        projectMapper.updateById(project);
    }

    // ========================
    // 3. AI Agent 流式生成
    // ========================

    /**
     * 生成创作灵感种子（同步，返回约 6 条候选灵感）
     *
     * <p>
     * 通过对应类型的 CreativeAgent 构建 Prompt，调用同步 LLM 返回 JSON 格式的灵感列表。
     * </p>
     *
     * @param typeCode 创作类型编码
     * @return LLM 返回的灵感文本（通常为 JSON 字符串）
     */
    public String generateInspiration(String typeCode) {
        User user = authService.getCurrentUser();
        CreativeTemplate template = getTemplate(typeCode);
        if (template == null) {
            throw new RuntimeException("创作模板不存在，typeCode=" + typeCode);
        }

        // Agent Prompt
        CreativeAgent agent = agentFactory.getAgent(typeCode);
        String systemPrompt = agent.buildInspirationPrompt(template.getTypeName());
        String userInput = "请为「" + template.getTypeName() + "」类型生成 6 个灵感种子。";

        var chatModel = llmRouter.getChatModelWithFallback(user);
        var result = chatModel.generate(
                new SystemMessage(systemPrompt),
                new UserMessage(userInput));
        return result.content().text();
    }

    /**
     * 生成简单大纲（同步）
     *
     * <p>
     * 根据项目灵感调用 Agent 构建 Prompt，返回 JSON 格式大纲并自动保存到 project.outline 字段。
     * </p>
     *
     * @param projectId 项目 ID
     * @return LLM 返回的大纲 JSON 字符串
     */
    public String generateOutline(Long projectId) {
        User user = authService.getCurrentUser();
        StudioProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(user.getId())) {
            throw new RuntimeException("项目不存在或无权限");
        }

        CreativeTemplate template = templateMapper.selectById(project.getTemplateId());
        String typeInfo = template != null ? template.getTypeName() : project.getTypeCode();

        // Agent Prompt
        CreativeAgent agent = agentFactory.getAgent(project.getTypeCode());
        String systemPrompt = agent.buildOutlinePrompt(typeInfo, project.getInspiration());
        String userInput = "内容类型：" + typeInfo + "\n用户灵感：" + project.getInspiration();

        var chatModel = llmRouter.getChatModelWithFallback(user);
        var result = chatModel.generate(
                new SystemMessage(systemPrompt),
                new UserMessage(userInput));

        String outlineJson = result.content().text();

        // 将大纲 JSON 保存到项目字段
        project.setOutline(outlineJson);
        projectMapper.updateById(project);

        return outlineJson;
    }

    /**
     * 生成结构化大纲（同步），支持 style 和 targetCount 参数
     *
     * <p>
     * 相比 {@link #generateOutline}，该方法额外接收题材偏好和目标段落数，
     * 通过 Agent 构建更精细的 Prompt，返回结构化 JSON schema。
     * 生成结果自动保存到 project.outline 字段。
     * </p>
     *
     * @param projectId   项目 ID
     * @param style       题材偏好（如「悬疑」「古风」等），可为 null
     * @param targetCount 目标内容段落/章节数量
     * @return LLM 返回的结构化大纲 JSON 字符串
     */
    public String generateStructuredOutline(Long projectId, String style, int targetCount) {
        User user = authService.getCurrentUser();
        StudioProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(user.getId())) {
            throw new RuntimeException("项目不存在或无权限");
        }

        CreativeTemplate template = templateMapper.selectById(project.getTemplateId());
        String typeInfo = template != null ? template.getTypeName() : project.getTypeCode();

        CreativeAgent agent = agentFactory.getAgent(project.getTypeCode());
        String systemPrompt = agent.buildStructuredOutlinePrompt(
                typeInfo, project.getInspiration(), style, targetCount);

        String userInput = "内容类型：" + typeInfo +
                "\n用户灵感：" + project.getInspiration() +
                "\n题材偏好：" + style +
                "\n目标数量：" + targetCount;

        var chatModel = llmRouter.getChatModelWithFallback(user);
        var result = chatModel.generate(
                new SystemMessage(systemPrompt),
                new UserMessage(userInput));

        String outlineJson = result.content().text();

        // 将结构化大纲 JSON 保存到项目字段
        project.setOutline(outlineJson);
        projectMapper.updateById(project);

        return outlineJson;
    }

    /**
     * 多角色剧本创作（SSE 流式）
     *
     * <p>
     * 专为「剧本」类型设计。通过 DramaAgent 构建包含角色设定、题材偏好的
     * System Prompt，调用流式 LLM 逐 token 推送给前端 SSE。
     * 生成完成后自动调用 {@link #saveGeneratedContent} 持久化内容。
     * </p>
     *
     * @param projectId    项目 ID
     * @param dialogueMode 对话模式（single / duo / ensemble 等）
     * @param genre        题材（如「都市言情」「古风仙侠」）
     * @param genreTips    题材补充提示（可为 null）
     * @param characters   角色设定（换行分隔的文本或 JSON，可为 null）
     * @param inspiration  本次生成灵感（为 null 时使用项目默认灵感）
     * @param emitter      SSE 发射器，用于推送生成 token
     */
    public void generateDrama(Long projectId, String dialogueMode, String genre,
            String genreTips, String characters, String inspiration,
            SseEmitter emitter) {
        User user = authService.getCurrentUser();
        StudioProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(user.getId())) {
            throw new RuntimeException("项目不存在或无权限");
        }

        // 获取 DramaAgent 并构建 System Prompt
        CreativeAgent rawAgent = agentFactory.getAgent("drama");
        if (!(rawAgent instanceof DramaAgent dramaAgent)) {
            throw new RuntimeException("[DramaAgent] 类型不匹配，预期 DramaAgent 但得到 " + rawAgent.getClass().getSimpleName());
        }

        String systemPrompt = dramaAgent.buildDialogueSystemPrompt(
                dialogueMode, genre, genreTips, characters);
        int maxTokens = dramaAgent.getDialogueMaxTokens(dialogueMode);
        String wordLimit = dramaAgent.getDialogueWordLimit(dialogueMode);

        // 用户消息：灵感 + 字数要求
        String userMessage = "创作灵感：" + (inspiration != null ? inspiration : project.getInspiration()) +
                "\n\n请根据以上角色设定和灵感，创作一段完整的对话剧本。" +
                "\n【字数硬性限制】" + wordLimit + "字！" +
                "\n直接输出对话正文，第一行为标题（#标题名）。";

        // 将 characters 文本转换为 JSON 数组并保存到项目
        if (characters != null && !characters.isBlank()) {
            try {
                // 每行一个角色设定，转为 JSON 数组
                com.alibaba.fastjson2.JSONArray arr = new com.alibaba.fastjson2.JSONArray();
                for (String line : characters.split("\n")) {
                    if (!line.isBlank())
                        arr.add(line.trim());
                }
                project.setCharacters(arr.toJSONString());
            } catch (Exception e) {
                // 转换失败则直接存为 JSON 字符串
                project.setCharacters(com.alibaba.fastjson2.JSON.toJSONString(characters));
            }
        }
        project.setStatus("creating");
        projectMapper.updateById(project);

        // 调用流式模型开始生成剧本
        var streamingModel = llmRouter.getStreamingModelWithFallback(user, maxTokens);
        var messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userMessage));

        StringBuilder fullResponse = new StringBuilder();

        streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                try {
                    emitter.send(SseEmitter.event().data(token));
                } catch (IOException e) {
                    log.warn("[StudioService] SSE 推送 token 失败（客户端可能已断开）: projectId={}", project.getId());
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                // 剧本必为单层内容，固定写入 sectionIndex=0
                saveGeneratedContent(project, fullResponse.toString(), user, userMessage, 0);

                project.setStatus("editing");
                projectMapper.updateById(project);

                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[StudioService] AI 剧本生成异常", error);
                try {
                    emitter.send(SseEmitter.event().data("\n\n[AI 生成失败，请重试]"));
                } catch (Exception e) {
                    log.warn("SSE 错误提示发送失败");
                }
                emitter.completeWithError(error);
            }
        });
    }

    /**
     * 根据大纲按章节生成正文内容（SSE 流式）
     *
     * <p>
     * 将大纲信息（章节情节、关键事件、伏笔）组装成 Agent 的 userMessage，
     * 再委托给 {@link #generateContent(Long, String, SseEmitter, int)} 流式生成。
     * </p>
     *
     * @param projectId     项目 ID
     * @param chapterIndex  章节索引（0-based，决定 Section 存储位置）
     * @param outlinePlot   该章节的大纲情节描述
     * @param keyEvents     关键事件列表（可为 null）
     * @param foreshadowing 伏笔细节（可为 null）
     * @param userExtra     用户额外补充说明（可为 null）
     * @param emitter       SSE 发射器
     */
    public void generateContentFromOutline(Long projectId, int chapterIndex,
            String outlinePlot, String keyEvents,
            String foreshadowing, String userExtra,
            SseEmitter emitter) {
        // 校验权限，获取对应类型的创作 Agent
        User user = authService.getCurrentUser();
        StudioProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(user.getId())) {
            throw new RuntimeException("项目不存在或无权限: projectId=" + projectId);
        }

        CreativeAgent agent = agentFactory.getAgent(project.getTypeCode());
        // 构建章节级别的用户消息（含大纲情节、关键事件、伏笔等上下文）
        String userInput = agent.buildChapterUserMessage(
                chapterIndex, outlinePlot, keyEvents, foreshadowing, userExtra);

        // 指定 chapterIndex 作为目标 sectionIndex，实现按章节覆盖写入
        generateContent(projectId, userInput, emitter, chapterIndex);
    }

    /**
     * AI 正文生成入口（SSE 流式），自动累加 sectionIndex
     *
     * <p>
     * 内部流程：
     * <ol>
     * <li>通过 CreativeAgent 构建 System Prompt（含模板规则）</li>
     * <li>使用 SectionMemoryService 拼接历史摘要 + RAG 上下文</li>
     * <li>调用流式 LLM 推送 token 到 SSE</li>
     * <li>生成完毕后调用 {@link #saveGeneratedContent} 持久化</li>
     * </ol>
     * </p>
     *
     * @param projectId 项目 ID
     * @param userInput 用户输入（大纲摘要 / 灵感 / 章节指令等）
     * @param emitter   SSE 发射器，用于推送生成 token
     */
    public void generateContent(Long projectId, String userInput, SseEmitter emitter) {
        generateContent(projectId, userInput, emitter, -1);
    }

    /**
     * AI 正文生成（SSE 流式），可指定目标 sectionIndex
     *
     * @param projectId          项目 ID
     * @param userInput          用户输入文本
     * @param emitter            SSE 发射器
     * @param targetSectionIndex 指定写入的段落索引（≥0 时覆盖该位置，-1 时自动追加）
     */
    public void generateContent(Long projectId, String userInput, SseEmitter emitter, int targetSectionIndex) {
        User user = authService.getCurrentUser();
        StudioProject project = projectMapper.selectById(projectId);

        if (project == null || !project.getUserId().equals(user.getId())) {
            throw new RuntimeException("项目不存在或无权限");
        }

        CreativeTemplate template = templateMapper.selectById(project.getTemplateId());
        if (template == null) {
            throw new RuntimeException("创作模板不存在");
        }

        // ======== 获取 Agent，构建携带历史记忆的 System Prompt ========
        CreativeAgent agent = agentFactory.getAgent(project.getTypeCode());

        // 双引擎记忆注入：摘要压缩记忆 + pgvector RAG 检索
        List<StudioSection> existingSections = listSections(projectId);
        StringBuilder memoryBuilder = new StringBuilder();
        if (!existingSections.isEmpty()) {
            // 引擎 1：摘要记忆（将前 N 段内容摘要拼入 System Prompt）
            String summaryMemory = sectionMemory.buildSummaryMemory(existingSections);
            if (!summaryMemory.isBlank()) {
                memoryBuilder.append(summaryMemory);
            }
            // 引擎 2：RAG 语义检索（段落数 ≥ 3 时启用，检索最相关的 2 段原文）
            if (existingSections.size() >= 3) {
                sectionMemory.warmUpIndex(projectId, existingSections);
                String ragContext = sectionMemory.retrieveRelevantContext(projectId, userInput, 2);
                if (!ragContext.isBlank()) {
                    memoryBuilder.append("\n").append(ragContext);
                }
            }
        }

        // 调用 Agent 构建完整 System Prompt（含模板规则 + 记忆上下文）
        String systemPrompt = agent.buildContentSystemPrompt(
                template, project, existingSections, userInput, memoryBuilder.toString());

        // 通过 LlmRouter 获取流式模型（由 Agent 指定最大输出 Token 数）
        var streamingModel = llmRouter.getStreamingModelWithFallback(user, agent.getMaxOutputTokens());
        var messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userInput));

        // 将项目状态更新为「生成中」，防止重复触发
        project.setStatus("creating");
        projectMapper.updateById(project);

        // 累积完整响应文本，用于生成完毕后持久化
        StringBuilder fullResponse = new StringBuilder();

        streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                try {
                    emitter.send(SseEmitter.event().data(token));
                } catch (IOException e) {
                    log.warn("SSE 发送失败（客户端可能已断开）");
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                // 生成完毕，持久化 Section 并绑定 userInput 到历史记录
                saveGeneratedContent(project, fullResponse.toString(), user, userInput, targetSectionIndex);

                // 生成完成，将项目状态改为「编辑中」
                project.setStatus("editing");
                projectMapper.updateById(project);

                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[StudioService] AI 正文生成异常", error);
                try {
                    emitter.send(SseEmitter.event().data("\n\n[AI 生成失败，请重试]"));
                } catch (Exception e) {
                    log.warn("SSE 错误提示发送失败");
                }
                emitter.completeWithError(error);
            }
        });
    }

    /**
     * AI 段落改写（SSE 流式）
     *
     * <p>
     * 根据用户改写指令对已有段落内容进行 AI 重新生成：
     * <ol>
     * <li>校验段落归属权限</li>
     * <li>加载历史摘要 + RAG 上下文，注入 System Prompt</li>
     * <li>流式生成，onComplete 后直接更新该 section 内容</li>
     * </ol>
     * </p>
     *
     * @param sectionId   段落 ID
     * @param instruction 用户改写指令（如「改为悲情结局」「精简到200字以内」）
     * @param emitter     SSE 发射器
     */
    public void rewriteSection(Long sectionId, String instruction, SseEmitter emitter) {
        User user = authService.getCurrentUser();
        StudioSection section = sectionMapper.selectById(sectionId);
        // 【阿里规范】if/else 单行体必须加花括号
        if (section == null) {
            throw new RuntimeException("段落不存在: sectionId=" + sectionId);
        }
        checkSectionOwnership(section);

        StudioProject project = projectMapper.selectById(section.getProjectId());

        // Agent Prompt
        CreativeAgent agent = agentFactory.getAgent(project.getTypeCode());

        // 注入历史记忆（摘要 + RAG），保持改写风格与全文一致
        StringBuilder memoryBuilder = new StringBuilder();
        List<StudioSection> allSections = listSections(section.getProjectId());
        if (!allSections.isEmpty()) {
            String summaryMemory = sectionMemory.buildSummaryMemory(allSections);
            if (!summaryMemory.isBlank()) {
                memoryBuilder.append("\n\n").append(summaryMemory);
            }
            // 段落数 ≥ 3 时启用 RAG 检索，给改写模型提供更多上下文
            if (allSections.size() >= 3) {
                String ragContext = sectionMemory.retrieveRelevantContext(section.getProjectId(), instruction, 2);
                if (!ragContext.isBlank()) {
                    memoryBuilder.append("\n").append(ragContext);
                }
            }
        }

        // 构建改写专属 System Prompt（含改写指令 + 原有内容 + 记忆上下文）
        String systemPrompt = agent.buildRewriteSystemPrompt(instruction, section.getContent())
                + memoryBuilder;
        String userInput = instruction;

        var streamingModel = llmRouter.getStreamingModelWithFallback(user);
        var messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userInput));

        StringBuilder fullResponse = new StringBuilder();

        streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                try {
                    emitter.send(SseEmitter.event().data(token));
                } catch (IOException e) {
                    log.warn("[StudioService] 改写 SSE 推送失败（客户端可能已断开）: sectionId={}", sectionId);
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                // 更新段落内容
                section.setContent(fullResponse.toString());
                section.setStatus("generated");
                sectionMapper.updateById(section);
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[StudioService] AI 段落改写异常: sectionId={}", sectionId, error);
                try {
                    emitter.send(SseEmitter.event().data("\n\n[AI 改写失败，请重试]"));
                } catch (Exception ex) {
                    log.warn("[StudioService] 改写错误 SSE 提示发送失败: {}", ex.getMessage());
                }
                emitter.completeWithError(error);
            }
        });
    }

    // ========================
    // 4. 段落（Section）操作
    // ========================

    /**
     * 查询项目下所有段落，按 sectionIndex 升序排列
     *
     * @param projectId 项目 ID
     * @return 有序的 StudioSection 列表
     */
    public List<StudioSection> listSections(Long projectId) {
        return sectionMapper.selectList(
                new LambdaQueryWrapper<StudioSection>()
                        .eq(StudioSection::getProjectId, projectId)
                        .orderByAsc(StudioSection::getSectionIndex));
    }

    /**
     * 新增或更新段落
     *
     * <p>
     * 若 section.id 不为 null 则执行 UPDATE，否则执行 INSERT。
     * </p>
     */
    public void saveSection(StudioSection section) {
        if (section.getId() != null) {
            sectionMapper.updateById(section);
        } else {
            sectionMapper.insert(section);
        }
    }

    /**
     * 根据 ID 查询段落实体
     *
     * @param sectionId 段落 ID
     * @return StudioSection 实体，不存在时返回 null
     */
    public StudioSection getSectionById(Long sectionId) {
        return sectionMapper.selectById(sectionId);
    }

    /**
     * 删除单个段落
     *
     * @param sectionId 段落 ID
     */
    public void deleteSection(Long sectionId) {
        sectionMapper.deleteById(sectionId);
    }

    /**
     * 删除整个项目（含权限校验 + 级联删除所有段落）
     *
     * <p>
     * 先通过 {@link #getProject(Long)} 校验当前用户权限，
     * 再删除该项目下所有 StudioSection，最后删除 StudioProject 本身。
     * </p>
     *
     * @param projectId 项目 ID
     */
    public void deleteProject(Long projectId) {
        // 校验权限（非本人项目会在此抛出异常）
        getProject(projectId);
        // 级联删除所有关联段落
        sectionMapper.delete(new LambdaQueryWrapper<StudioSection>()
                .eq(StudioSection::getProjectId, projectId));
        // 删除项目本体
        projectMapper.deleteById(projectId);
        log.info("[StudioService] 项目删除成功: projectId={}", projectId);
    }

    // ========================
    // 5. 内容持久化（内部方法）
    // ========================

    /**
     * 将 AI 生成的原始文本持久化为 StudioSection
     *
     * <p>
     * 处理逻辑：
     * <ul>
     * <li>若 targetSectionIndex ≥ 0，则覆盖该位置的已有段落</li>
     * <li>否则从 userInput 解析章节编号（匹配「第N章」），解析失败则自动追加</li>
     * <li>若原始文本以 {@code ##} 开头，自动提取首行作为段落标题</li>
     * <li>调用 {@link SectionMemoryService#generateSummary} 生成摘要并写入索引</li>
     * </ul>
     * </p>
     *
     * @param project            当前创作项目
     * @param rawOutput          LLM 原始输出文本
     * @param user               当前操作用户
     * @param userInput          用户输入（用于章节索引解析）
     * @param targetSectionIndex 目标段落索引，-1 表示自动追加
     */
    private void saveGeneratedContent(StudioProject project, String rawOutput, User user, String userInput,
            int targetSectionIndex) {
        int nextIndex;
        if (targetSectionIndex >= 0) {
            // 指定覆盖模式：先删除同位置已有段落，再插入新内容（幂等保证）
            nextIndex = targetSectionIndex;
            sectionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudioSection>()
                    .eq(StudioSection::getProjectId, project.getId())
                    .eq(StudioSection::getSectionIndex, nextIndex));
        } else {
            // 自动追加：先尝试从 userInput 解析章节编号
            nextIndex = parseChapterIndex(userInput);
            if (nextIndex < 0) {
                Integer maxIndex = sectionMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudioSection>()
                                .eq(StudioSection::getProjectId, project.getId())
                                .select(StudioSection::getSectionIndex)
                                .orderByDesc(StudioSection::getSectionIndex)
                                .last("LIMIT 1"))
                        .stream().findFirst().map(StudioSection::getSectionIndex).orElse(-1);
                nextIndex = maxIndex + 1;
            } else {
                sectionMapper
                        .delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudioSection>()
                                .eq(StudioSection::getProjectId, project.getId())
                                .eq(StudioSection::getSectionIndex, nextIndex));
            }
        }

        // 判断是否为单段内容类型（radio / lecture / ad / picture_book / news）
        // 单段内容直接使用项目标题；多段内容按「第N段」命名
        String typeCode = project.getTypeCode();
        boolean isSingleContent = "radio".equals(typeCode) || "lecture".equals(typeCode)
                || "ad".equals(typeCode) || "picture_book".equals(typeCode) || "news".equals(typeCode);
        String title = isSingleContent ? project.getTitle() : "第" + (nextIndex + 1) + " 段";
        String content = rawOutput;

        if (rawOutput != null && rawOutput.startsWith("##")) {
            int newline = rawOutput.indexOf('\n');
            if (newline > 0) {
                title = rawOutput.substring(2, newline).trim();
                content = rawOutput.substring(newline + 1).trim();
            }
        }

        StudioSection section = new StudioSection();
        section.setProjectId(project.getId());
        section.setSectionIndex(nextIndex);
        section.setTitle(title);
        section.setContent(content);
        section.setStatus("generated");

        // ======== 生成摘要 + 建立向量索引 ========
        try {
            String summary = sectionMemory.generateSummary(user, content, title);
            section.setSummary(summary);
        } catch (Exception e) {
            log.warn("段落摘要生成失败，跳过索引: {}", e.getMessage());
        }

        sectionMapper.insert(section);
        sectionMemory.indexSection(project.getId(), section);

        // 更新项目总段落数（取 max 防止乱序写入导致回退）
        int newTotal = Math.max(nextIndex + 1, project.getTotalSections() != null ? project.getTotalSections() : 0);
        project.setTotalSections(newTotal);
        projectMapper.updateById(project);
    }

    /**
     * 从 userInput 中解析章节索引（0-based）
     *
     * <p>
     * 匹配形如「【章节标题：第N章】」的标记，将其转换为 0-based 索引（第1章 → 0）。
     * </p>
     *
     * @param userInput 用户输入文本
     * @return 解析到的 0-based 索引，解析失败返回 -1
     */
    private int parseChapterIndex(String userInput) {
        // 【阿里规范】if 单行体必须加花括号
        if (userInput == null) {
            return -1;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("【章节标题：第(\\d+)章】").matcher(userInput);
        if (m.find()) {
            return Integer.parseInt(m.group(1)) - 1; // 第1章 → index 0
        }
        return -1;
    }

    // ========================
    // 6. 发布 / 下架
    // ========================

    /**
     * 发布创作项目
     *
     * <p>
     * 完整发布流程：StudioProject → UserCreation → Work（发现页作品）
     * <ul>
     * <li>非小说类型：必须至少有一段已合成音频才能发布</li>
     * <li>小说类型：仅需有文本段落，无需音频</li>
     * <li>自动从 outline.synopsis 字段提取作品简介</li>
     * <li>发布后将项目状态置为 「completed」</li>
     * </ul>
     * </p>
     *
     * @param projectId 项目 ID
     * @return 包含 workId 和提示消息的 Map
     */
    public Map<String, Object> publishProject(Long projectId) {
        Long userId = StpUtil.getLoginIdAsLong();
        StudioProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw new RuntimeException("项目不存在");
        }

        boolean isNovel = "novel".equals(project.getTypeCode());

        // 查询所有段落，按序号排列
        List<StudioSection> allSections = sectionMapper.selectList(
                new LambdaQueryWrapper<StudioSection>()
                        .eq(StudioSection::getProjectId, projectId)
                        .orderByAsc(StudioSection::getSectionIndex));

        if (allSections.isEmpty()) {
            throw new RuntimeException("项目下无任何段落内容，无法发布");
        }

        // 筛选出已有音频的段落（用于获取主音频 URL）
        List<StudioSection> audioSections = allSections.stream()
                .filter(s -> s.getAudioUrl() != null)
                .toList();

        if (!isNovel && audioSections.isEmpty()) {
            throw new RuntimeException("请先合成至少一段音频后再发布");
        }

        String mainAudioUrl = audioSections.isEmpty() ? null : audioSections.get(0).getAudioUrl();

        // 拼接全文内容，统计总字数
        StringBuilder allText = new StringBuilder();
        int totalWordCount = 0;
        for (StudioSection s : allSections) {
            if (s.getContent() != null) {
                allText.append(s.getContent()).append("\n\n");
                totalWordCount += s.getContent().length();
            }
        }

        // 优先从 outline.synopsis 字段提取简介，超过 200 字则截断
        String description = "";
        if (isNovel && project.getOutline() != null) {
            // ?JSON ?synopsis
            try {
                var outlineJson = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(project.getOutline());
                if (outlineJson.has("synopsis")) {
                    description = outlineJson.get("synopsis").asText("");
                }
            } catch (Exception e) {
                description = project.getOutline().length() > 200
                        ? project.getOutline().substring(0, 200)
                        : project.getOutline();
            }
        }
        if (description.isEmpty()) {
            description = allText.length() > 200 ? allText.substring(0, 200) : allText.toString();
        }

        // 1. 创建 UserCreation 记录
        UserCreation creation = new UserCreation();
        creation.setUserId(userId);
        creation.setType("studio_" + project.getTypeCode());
        creation.setTitle(project.getTitle());
        creation.setInputText(allText.length() > 2000 ? allText.substring(0, 2000) : allText.toString());
        if (!audioSections.isEmpty()) {
            creation.setVoiceId(audioSections.get(0).getVoiceId());
        }
        creation.setAudioUrl(mainAudioUrl);
        // 估算音频时长：按 TTS 标准 4.5 字/秒计算
        int estimatedDuration = Math.max(5,
                (int) (audioSections.stream().mapToInt(s -> s.getContent() != null ? s.getContent().length() : 0).sum()
                        / 4.5));
        creation.setAudioDuration(estimatedDuration);
        creation.setIsPublished(1);
        userCreationMapper.insert(creation);

        // 2. 创建发现页 Work 记录（待运营审核后展示）
        Work work = new Work();
        work.setUserId(userId);
        work.setCreationId(creation.getId());
        work.setTitle(project.getTitle());
        work.setCategory(mapTypeToCategory(project.getTypeCode()));
        work.setSourceType("studio");
        work.setContentType(mapContentType(project.getTypeCode()));
        work.setDescription(description);
        work.setSourceProjectId(project.getId());
        work.setAudioUrl(mainAudioUrl);
        int workDuration = Math.max(5,
                (int) (audioSections.stream().mapToInt(s -> s.getContent() != null ? s.getContent().length() : 0).sum()
                        / 4.5));
        work.setAudioDuration(audioSections.isEmpty() ? 0 : workDuration);

        if (isNovel) {
            work.setWordCount(totalWordCount);
            work.setChapterCount(allSections.size());
        }

        work.setPlayCount(0);
        work.setLikeCount(0);
        work.setShareCount(0);
        work.setCommentCount(0);
        work.setStatus("published");
        work.setReviewStatus("approved");
        work.setIsFeatured(0);
        workMapper.insert(work);

        // 3. 回填 UserCreation.workId，建立双向关联
        creation.setWorkId(work.getId());
        userCreationMapper.updateById(creation);

        // 4. 更新项目状态为「已发布」
        project.setStatus("completed");
        if (mainAudioUrl != null) {
            project.setAudioUrl(mainAudioUrl);
        }
        projectMapper.updateById(project);

        log.info("[StudioService] 项目发布成功: projectId={} workId={} typeCode={}", projectId, work.getId(),
                project.getTypeCode());

        return Map.of(
                "workId", work.getId(),
                "message", "发布成功，等待运营审核");
    }

    /**
     * 下架已发布的创作项目
     *
     * <p>
     * 操作流程：
     * <ol>
     * <li>根据 sourceProjectId 查询最近一条已发布的 Work 并置为 「unpublished」</li>
     * <li>将项目状态回退为 「editing」</li>
     * </ol>
     * </p>
     *
     * @param projectId 项目 ID
     * @return 包含 workId 和提示消息的 Map
     */
    public Map<String, Object> unpublishProject(Long projectId) {
        Long userId = StpUtil.getLoginIdAsLong();
        StudioProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw new RuntimeException("项目不存在");
        }

        // 通过 sourceProjectId 查找最新发布的 Work
        Work work = workMapper.selectOne(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getSourceProjectId, projectId)
                        .eq(Work::getUserId, userId)
                        .eq(Work::getStatus, "published")
                        .orderByDesc(Work::getCreatedAt)
                        .last("LIMIT 1"));

        if (work == null) {
            throw new RuntimeException("未找到已发布的作品");
        }

        // 1. 将 Work 状态改为下架
        work.setStatus("unpublished");
        workMapper.updateById(work);

        // 2. 将项目状态回退到「编辑中」
        project.setStatus("editing");
        projectMapper.updateById(project);

        log.info("[StudioService] 项目下架成功: projectId={} workId={}", projectId, work.getId());

        return Map.of(
                "workId", work.getId(),
                "message", "作品已下架");
    }

    /**
     * 根据创作类型映射到发现页内容分类
     *
     * @param typeCode 创作类型编码
     * @return 发现页 category 字段值
     */
    private String mapTypeToCategory(String typeCode) {
        switch (typeCode) {
            case "novel":
                return "novel";
            case "drama":
                return "story";
            case "podcast":
                return "podcast";
            case "radio":
                return "emotion";
            case "news":
                return "news";
            default:
                return "latest";
        }
    }

    /**
     * 根据创作类型映射到内容格式类型（content_type 字段）
     *
     * @param typeCode 创作类型编码
     * @return content_type 值（novel / podcast / music / audio）
     */
    private String mapContentType(String typeCode) {
        switch (typeCode) {
            case "novel":
                return "novel";
            case "podcast":
                return "podcast";
            case "music":
                return "music";
            default:
                return "audio";
        }
    }

    // ========================
    // 7. 音频工具
    // ========================

    /**
     * 将多段 MP3 音频拼接并上传至 R2
     *
     * <p>
     * 按顺序下载各 URL 的 MP3 字节流，拼接为一个完整文件后上传 R2，
     * 返回永久访问 URL。注意：该方法仅执行二进制拼接，不重新编码，
     * 适用于同码率 + 同采样率的 MP3 文件合并。
     * </p>
     *
     * @param audioUrls 已按顺序排列的 MP3 音频 URL 列表
     * @return 合并后上传到 R2 的永久 URL
     * @throws IOException 下载或上传失败时抛出
     */
    public String concatAudio(List<String> audioUrls) throws IOException {
        ByteArrayOutputStream merged = new ByteArrayOutputStream();
        for (String url : audioUrls) {
            try (InputStream in = URI.create(url).toURL().openStream()) {
                in.transferTo(merged);
            }
        }
        String filename = "studio_concat_" + System.currentTimeMillis() + ".mp3";
        return r2StorageAdapter.uploadAudio(merged.toByteArray(), filename);
    }
}
