package com.soundread.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.soundread.agent.drama.ChapterSplitterAgent;
import com.soundread.agent.emotion.EmotionAnnotatorAgent;
import com.soundread.config.ai.LlmRouter;
import com.soundread.model.entity.NovelChapter;
import com.soundread.model.entity.NovelProject;
import com.soundread.model.entity.NovelSegment;
import com.soundread.mapper.NovelChapterMapper;
import com.soundread.mapper.NovelProjectMapper;
import com.soundread.mapper.NovelSegmentMapper;
import com.soundread.model.EmotionState;
import com.soundread.model.entity.User;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ?Pipeline
 *
 * <p>
 * :
 * Stage 1 ?ChapterSplitterAgent
 * Stage 2 ?500~800?
 * Stage 3 ?EmotionAnnotatorAgent + EmotionState ?
 * Stage 4 ?TTS 2.0 section_id ?
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NovelPipelineService {

    private final LlmRouter llmRouter;
    private final NovelProjectMapper projectMapper;
    private final NovelChapterMapper chapterMapper;
    private final NovelSegmentMapper segmentMapper;
    private final NovelService novelService;
    private final AuthService authService;

    /** */
    private static final int SEGMENT_TARGET_CHARS = 600;
    /** ? */
    private static final int SEGMENT_MAX_CHARS = 900;

    /**
     * Pipeline
     *
     * @param projectId ID
     * @param rawText
     */
    @Async
    public void startPipeline(Long projectId, String rawText) {
        try {
            log.info("[NovelPipeline] 开始处理流水线: projectId={}", projectId);
            novelService.updateProgress(projectId, 5, "analyzing");

            // Stage 1: AI 读取小说断章
            List<NovelChapter> chapters = splitChapters(projectId, rawText);
            novelService.updateProgress(projectId, 20, "annotating");

            // Stage 2 + 3: 情感标注 + 段落切割
            EmotionState state = null;
            int totalChapters = chapters.size();
            for (int i = 0; i < totalChapters; i++) {
                NovelChapter chapter = chapters.get(i);

                // 初始化情感上下文（第一章用空状态启动）
                if (state == null) {
                    state = EmotionState.initial(chapter.getTitle());
                } else {
                    state = EmotionState.fromPreviousChapter(state.getPreviousSummary(), chapter.getTitle());
                }

                // 切割当前章节为自然段落
                List<NovelSegment> segments = splitSegments(chapter);

                // 对段落进行情感标注
                state = annotateSegments(segments, state);

                // 更新章节状态并保存情感摘要
                chapter.setStatus("annotated");
                chapter.setEmotionSummary(state.getPreviousSummary());
                chapter.setTotalSegments(segments.size());
                chapterMapper.updateById(chapter);

                // 按章节进度计算总进度（20%~70%）
                int chapterProgress = 20 + (int) ((i + 1.0) / totalChapters * 50);
                novelService.updateProgress(projectId, chapterProgress, "annotating");
            }

            // Stage 4: TTS 70%~100%?
            novelService.updateProgress(projectId, 70, "synthesizing");
            // TODO: TTS 2.0 + section_id +
            // ?Tts2Client WebSocket ?

            novelService.updateProgress(projectId, 100, "completed");
            log.info("[NovelPipeline] 流水线处理完成: projectId={}", projectId);

        } catch (Exception e) {
            log.error("[NovelPipeline] 流水线处理失败: projectId={}", projectId, e);
            novelService.updateProgress(projectId, -1, "failed");
        }
    }

    /**
     * Stage 1: AI
     */
    private List<NovelChapter> splitChapters(Long projectId, String rawText) {
        // ?Agent?
        User user = authService.getCurrentUser();
        var model = llmRouter.getChatModelWithFallback(user);
        ChapterSplitterAgent agent = AiServices.builder(ChapterSplitterAgent.class)
                .chatLanguageModel(model)
                .build();

        String result = agent.splitChapters(rawText);
        JSONArray chaptersJson = JSON.parseArray(result);

        List<NovelChapter> chapters = new ArrayList<>();
        for (int i = 0; i < chaptersJson.size(); i++) {
            JSONObject ch = chaptersJson.getJSONObject(i);
            NovelChapter chapter = new NovelChapter();
            chapter.setProjectId(projectId);
            chapter.setChapterIndex(i + 1);
            chapter.setTitle(ch.getString("title"));
            chapter.setRawText(ch.getString("text"));
            chapter.setCharCount(ch.getString("text").length());
            chapter.setStatus("pending");
            chapterMapper.insert(chapter);
            chapters.add(chapter);
        }

        // 更新项目总章节数
        NovelProject update = new NovelProject();
        update.setId(projectId);
        update.setTotalChapters(chapters.size());
        projectMapper.updateById(update);

        log.info("[NovelPipeline] 章节切割完成: projectId={} chapters={}", projectId, chapters.size());
        return chapters;
    }

    /**
     * Stage 2: 将章节文本按自然断句切割为不超过 600 字的语义段落
     */
    private List<NovelSegment> splitSegments(NovelChapter chapter) {
        String text = chapter.getRawText();
        List<NovelSegment> segments = new ArrayList<>();

        int offset = 0;
        int segIndex = 1;
        while (offset < text.length()) {
            int end = Math.min(offset + SEGMENT_TARGET_CHARS, text.length());

            // ?
            if (end < text.length()) {
                int bestBreak = findNaturalBreak(text, offset + SEGMENT_TARGET_CHARS / 2,
                        Math.min(offset + SEGMENT_MAX_CHARS, text.length()));
                if (bestBreak > offset) {
                    end = bestBreak;
                }
            }

            String segText = text.substring(offset, end).trim();
            if (!segText.isEmpty()) {
                NovelSegment seg = new NovelSegment();
                seg.setChapterId(chapter.getId());
                seg.setSegmentIndex(segIndex++);
                seg.setRawText(segText);
                seg.setCharCount(segText.length());
                seg.setStatus("pending");
                segmentMapper.insert(seg);
                segments.add(seg);
            }
            offset = end;
        }

        log.debug("info", chapter.getId(), segments.size());
        return segments;
    }

    /**
     * Stage 3: AI
     */
    private EmotionState annotateSegments(List<NovelSegment> segments, EmotionState state) {
        for (NovelSegment seg : segments) {
            try {
                // ?Agent
                User user = authService.getCurrentUser();
                var model = llmRouter.getChatModelWithFallback(user);
                EmotionAnnotatorAgent agent = AiServices.builder(EmotionAnnotatorAgent.class)
                        .chatLanguageModel(model)
                        .build();

                String result = agent.annotate(
                        state.getChapterTitle(),
                        state.getCurrentMood(),
                        state.getTensionLevel(),
                        state.getPreviousSummary(),
                        seg.getRawText());

                JSONObject json = JSON.parseObject(result);
                seg.setAnnotatedText(json.getString("annotated_text"));
                seg.setContextTexts(json.getString("context_texts"));
                seg.setEmotionLabel(json.getString("emotion_label"));
                seg.setTensionLevel(json.getIntValue("tension_level"));
                seg.setStatus("annotated");
                segmentMapper.updateById(seg);

                // EmotionState
                state.setCurrentMood(json.getString("emotion_label"));
                state.setTensionLevel(json.getIntValue("tension_level"));
                state.setPreviousSummary(json.getString("summary"));

            } catch (Exception e) {
                log.warn("[NovelPipeline] 情感标注失败，使用默认风格: segmentId={}", seg.getId(), e);
                seg.setAnnotatedText(seg.getRawText()); // Fallback: use raw text
                seg.setContextTexts(null);
                seg.setEmotionLabel("");
                seg.setTensionLevel(5);
                seg.setStatus("annotated");
                segmentMapper.updateById(seg);
            }
        }
        return state;
    }

    /**
     * ?
     * ? > /? > /
     */
    private int findNaturalBreak(String text, int from, int to) {
        // ?
        for (int i = to - 1; i >= from; i--) {
            if (text.charAt(i) == '\n') {
                return i + 1;
            }
        }
        // ?
        for (int i = to - 1; i >= from; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        //
        for (int i = to - 1; i >= from; i--) {
            char c = text.charAt(i);
            if (c == '，' || c == '；' || c == ',' || c == ';') {
                return i + 1;
            }
        }
        return to; // Hard truncation
    }
}
