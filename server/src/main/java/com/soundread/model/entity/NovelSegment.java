package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 有声书语义段落实体（对应 novel_segment 表，TTS 最小合成单元）
 *
 * <p>
 * 每个章节按自然断句被切割为若干不超过 500~800 字的语义段落，
 * 由 {@code EmotionAnnotatorAgent} 进行情感标注（链式思考 CoT 模式），
 * 标注结果写入 {@code annotatedText} 字段，同时生成 {@code context_texts} 供
 * TTS 2.0 WebSocket 接口参考，通过 {@code section_id} 与 TTS 会话关联。
 * </p>
 *
 * @author SoundRead
 */
@Data
@TableName("novel_segment")
public class NovelSegment {

   /** 段落 ID（雪花算法自动生成） */
   @TableId(type = IdType.ASSIGN_ID)
   private Long id;

   /** 所属章节 ID（关联 novel_chapter.id） */
   private Long chapterId;

   /** 段落序号（从 1 开始，章节内唯一） */
   private Integer segmentIndex;

   /** 段落原始文本（AI 标注前的纯文字内容） */
   private String rawText;

   /**
    * AI 情感标注后的文本
    * <p>
    * 格式：包含 {@code <cot text=...>...</cot>} 链式思考标签，
    * 以及情感指令标注（如语气、语速、停顿等）。
    * 实际 TTS 合成时使用此字段而非 rawText。
    * </p>
    */
   private String annotatedText;

   /**
    * AI 生成的上下文文本列表（JSON 数组格式）
    * <p>
    * 对应 TTS 2.0 接口的 {@code additions.context_texts} 参数，
    * 提供段落前后文给引擎参考以提升情感连贯性和语气自然度。
    * 示例格式：{@code ["前文摘要...", "后文摘要..."]}
    * </p>
    */
   private String contextTexts;

   /**
    * 情感标签（情感分类标识）
    * <p>
    * 常见值：{@code "平静"} / {@code "激动"} / {@code "悲伤"} / {@code "欢快"} 等，
    * 由 EmotionAnnotatorAgent 分析后写入。
    * </p>
    */
   private String emotionLabel;

   /** 情感张力等级（1~10，数值越大情感越强烈） */
   private Integer tensionLevel;

   /** 段落字符数（用于 TTS 平均时长估算） */
   private Integer charCount;

   /**
    * 段落处理状态
    * <ul>
    * <li>{@code pending} — 等待标注或合成</li>
    * <li>{@code annotated} — 情感标注完成，等待 TTS 合成</li>
    * <li>{@code synthesizing}— 正在 TTS 合成中</li>
    * <li>{@code completed} — 音频合成完成</li>
    * <li>{@code failed} — 处理失败</li>
    * </ul>
    */
   private String status;

   /** 段落合成后的音频 URL（存储于 R2，合成成功后写入） */
   private String audioUrl;

   /** 段落音频时长（秒，合成成功后写入） */
   private Integer audioDuration;

   /**
    * TTS 会话段落 ID（section_id）
    * <p>
    * 对应 TTS 2.0 协议中 {@code additions.section_id} 字段，
    * 用于在同一 WebSocket session 内区分不同段落（从 0 递增，最多 10 个），
    * 使引擎能保持跨段落的情感和语调连贯性。
    * </p>
    */
   private String sectionId;

   /**
    * TTS 合成生成的字幕数据（Subtitle JSON 格式）
    * <p>
    * 由 TTS 引擎返回的词级时间戳 JSON，用于字幕逐字高亮显示。
    * </p>
    */
   private String subtitleJson;

   /** 记录创建时间（自动填充） */
   @TableField(fill = FieldFill.INSERT)
   private LocalDateTime createdAt;
}
