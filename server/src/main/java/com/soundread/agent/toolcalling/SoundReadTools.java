package com.soundread.agent.toolcalling;

import com.soundread.config.ai.LlmRouter;
import com.soundread.controller.ttsv2.TtsV2Request;
import com.soundread.controller.ttsv2.TtsV2Response;
import com.soundread.controller.ttsv2.TtsV2Service;
import com.soundread.common.exception.QuotaExceededException;
import com.soundread.model.entity.SysVoice;
import com.soundread.model.entity.UserCreation;
import com.soundread.model.entity.User;
import com.soundread.service.CreationService;
import com.soundread.service.QuotaService;
import com.soundread.service.VoiceService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * AI 声音工坊大模型 Tool Calling 工具集
 *
 * <p>
 * 提供给以 @Tool 注解标注的内部 Service 能力给 LLM 调用。
 * 使大模型具备获取数据、生成音频等真实系统能力。
 * </p>
 *
 * <h3>安全性设计</h3>
 * <p>
 * 因为大模型的调用脱离了常规 Controller 的 HTTP 请求上下文，我们通过
 * ThreadLocal {@link #setCurrentUser} 在使用 Tool 前将会话用户注入，
 * 处理结束后清理。这样在 LangChain4j 的多线程回调时也能正确识别当前用户，
 * 而不会触发 Sa-Token 的 NotLoginException。
 * </p>
 */
@Slf4j
@Component
public class SoundReadTools {

    private final LlmRouter llmRouter;
    private final VoiceService voiceService;
    private final CreationService creationService;
    private final TtsV2Service ttsV2Service;
    private final QuotaService quotaService;

    /** 显式绑定的用户（用于流式调用时注入，解决 ThreadLocal 丢失问题） */
    private final User explicitUser;

    /** 默认构造函数（Spring 注入用，无显式用户） */
    @Autowired
    public SoundReadTools(LlmRouter llmRouter, VoiceService voiceService, CreationService creationService,
            TtsV2Service ttsV2Service, QuotaService quotaService) {
        this.llmRouter = llmRouter;
        this.voiceService = voiceService;
        this.creationService = creationService;
        this.ttsV2Service = ttsV2Service;
        this.quotaService = quotaService;
        this.explicitUser = null;
    }

    /** 拷贝构造函数（供 Controller 针对于当前请求创建一个包含有具体用户状态的工具集） */
    public SoundReadTools(SoundReadTools source, User user) {
        this.llmRouter = source.llmRouter;
        this.voiceService = source.voiceService;
        this.creationService = source.creationService;
        this.ttsV2Service = source.ttsV2Service;
        this.quotaService = source.quotaService;
        this.explicitUser = user;
    }

    /**
     * 存放当前调用会话的用户，供 Tool 方法读取当前操作者
     */
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    /** 供 Controller 在调用大模型前设值 */
    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    /** 供 Controller 在调用结束后清理 */
    public static void clearCurrentUser() {
        CURRENT_USER.remove();
    }

    /** 从 ThreadLocal 或显式状态获取当前用户，获取不到说明调用链路异常 */
    private User getUser() {
        if (explicitUser != null) {
            return explicitUser;
        }
        User user = CURRENT_USER.get();
        if (user == null) {
            throw new RuntimeException("Tool 调用链路异常：未找到当前用户上下文，请检查 ThreadLocal 是否正确传递");
        }
        return user;
    }

    // ==========================================
    // 技能 1: 查询可用音色列表
    // ==========================================

    @Tool("查询平台可用的情感 AI 音色列表，返回音色名称、性别、风格标签和适用场景。当用户询问有哪些声音、音色、发音人、想选择声音时调用。")
    public String listVoices() {
        log.info("[ToolCalling] 🔧 listVoices — 查询 2.0 音色库");
        try {
            List<SysVoice> voices = voiceService.getSupportedVoices("tts-2.0");
            if (voices.isEmpty()) {
                return "当前暂无可用的情感音色。（查询完毕）";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("共找到 %d 款可用音色：\n", voices.size()));
            for (int i = 0; i < voices.size(); i++) {
                SysVoice v = voices.get(i);
                sb.append(String.format("%d. 【%s】（性别：%s，风格：%s）适用场景：%s | ID: %s\n",
                        i + 1,
                        v.getName(),
                        "male".equals(v.getGender()) ? "男声" : "female".equals(v.getGender()) ? "女声" : v.getGender(),
                        v.getTags() != null ? v.getTags() : "通用",
                        v.getDescription() != null ? v.getDescription() : "无特定场景",
                        v.getVoiceId()));
            }
            sb.append("\n您可以根据上述列表，告诉我要使用哪款音色。");
            return sb.toString();
        } catch (Exception e) {
            log.error("[SoundReadTools] 查询音色列表失败: userId={}", getUser().getId(), e);
            return "音色查询失败：" + e.getMessage();
        }
    }

    // ==========================================
    // 技能 2: 调用 LLM 生成配音台本
    // ==========================================

    @Tool("根据用户描述生成一段配音台本。当用户想要创作台本、写一段文字、生成朗读内容、制作有声内容时调用。")
    public String generateScript(String theme, String emotion, int wordCount) {
        log.info("generateScript called theme={} emotion={} count={}", theme, emotion, wordCount);
        try {
            User user = getUser();
            var model = llmRouter.getChatModelWithFallback(user);
            var result = model.generate(
                    String.format("你是一位专业的配音台本撰稿人。请用%s的语气，写一段关于「%s」的优美台本，严格控制在%d字左右。只输出台本内容，不要加标题和解释。",
                            emotion, theme, wordCount));
            return "\n" + result + "\n";
        } catch (Exception e) {
            log.error("[SoundReadTools] 台本生成失败: theme={}, emotion={}, wordCount={}", theme, emotion, wordCount, e);
            return "台本生成失败：" + e.getMessage();
        }
    }

    // ==========================================
    // 技能 3: 调用 LLM 分析情感类型
    // ==========================================

    @Tool("分析一段文字的情感倾向，返回情感标签、推荐语气和适合的音色。当用户想知道一段文字该用什么语气读、或想了解情感方向时调用。")
    public String analyzeEmotion(String text) {
        log.info("analyzeEmotion called length={}", text.length());
        try {
            User user = getUser();
            var model = llmRouter.getChatModelWithFallback(user);
            var result = model.generate(
                    "请分析以下文字的情感倾向，输出：1.主情感标签 2.张力值(0-1) 3.推荐的语气方向 4.推荐的音色类型（男声/女声+风格）。\n文字：" + text);
            return "\n" + result + "\n";
        } catch (Exception e) {
            log.error("[SoundReadTools] 情感分析失败: textLen={}", text.length(), e);
            return "情感分析失败：" + e.getMessage();
        }
    }

    // ==========================================
    // 技能 4: TS 2.0 语音合成并在后台持久化记录
    // ==========================================

    @Tool("将文字用指定音色合成为有感情的语音音频，返回可播放的音频URL。当用户想听某段文字、想试听、想合成语音、想录音时调用。参数 voiceId 是音色的技术ID（如 zh_female_shuangkuaisisi_moon_bigtts），如不确定可先调用 listVoices 获取。")
    public String synthesizeSpeech(String text, String voiceId) {
        log.info("synthesizeSpeech called length={} voice={}", text.length(), voiceId);
        try {
            User user = getUser();

            // ★ 配额检查 — TTS 2.0 每日字数
            try {
                quotaService.checkAndDeductTextV2Quota(user, text.length());
            } catch (QuotaExceededException e) {
                log.warn("[SoundReadTools] 配额超限: userId={}, chars={}", user.getId(), text.length());
                return "⚠️ " + e.getMessage() + "\n（合成取消，此次不消耗您的额度）";
            }

            TtsV2Request request = new TtsV2Request();
            request.setText(text);
            request.setVoiceType(voiceId);
            request.setUserKey(user.getId().toString());

            TtsV2Response response = ttsV2Service.synthesize(request);
            String audioUrl = response.getAudioUrl();

            // 为合成的音频自动创建云端库记录
            UserCreation creation = new UserCreation();
            creation.setUserId(user.getId());
            creation.setType("workshop");
            creation.setTitle("工坊 AI 合成");
            creation.setInputText(text.length() > 200 ? text.substring(0, 200) : text);
            creation.setAudioUrl(audioUrl);
            creation.setVoiceId(voiceId);
            creation.setAudioDuration((int) Math.round(text.length() / 4.5));
            creation.setFileSize(0L);
            creationService.save(creation);

            return String.format("\n🎶 音频已成功合成！\n播放链接：%s\n使用音色ID：%s\n正文字数：%d字\n",
                    audioUrl, voiceId, text.length());
        } catch (Exception e) {
            log.error("[SoundReadTools] 语音合成失败: voiceId={}, textLen={}", voiceId, text.length(), e);
            return "合成失败：" + e.getMessage() + "。可能是音色ID不正确，请先调用 listVoices 获取正确的音色ID。";
        }
    }

    // ==========================================
    // 技能 5: 查询用户的个人云端创作记录
    // ==========================================

    @Tool("查询当前用户的创作作品列表，返回作品标题、类型、创建时间。当用户问'我有哪些作品'、'我的创作'、'历史记录'时调用。")
    public String listMyWorks() {
        log.info("[SoundReadTools] 查询我的作品列表: userId={}", getUser().getId());
        try {
            User user = getUser();
            var page = creationService.listByUser(user.getId(), null, 1, 10);
            var records = page.getRecords();
            if (records.isEmpty()) {
                return "📚 您还没有创作作品，快来试试语音合成吧！（查询完毕）";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("为您找到最近的 %d 个作品：\n", page.getTotal()));
            for (int i = 0; i < records.size(); i++) {
                UserCreation c = records.get(i);
                sb.append(String.format("%d. 【%s】标签：%s (创建于 %s)\n",
                        i + 1,
                        c.getTitle() != null ? c.getTitle() : "未命名作品",
                        c.getType() != null ? c.getType() : "未知类型",
                        c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "未知时间"));
            }
            sb.append("\n（以上只展示最新10条，全部作品请移步创作库查看）");
            return sb.toString();
        } catch (Exception e) {
            log.error("[SoundReadTools] 查询作品列表失败: userId={}", getUser().getId(), e);
            return "作品查询失败：" + e.getMessage();
        }
    }
}
