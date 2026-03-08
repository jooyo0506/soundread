package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 有声书章节实体（对应 novel_chapter 表）
 *
 * <p>
 * 一个有声书项目（NovelProject）由多个章节组成，每个章节再细分为多个语义段落（NovelSegment）。
 * 章节记录由 AI {@code ChapterSplitterAgent} 自动从原始文本中切割生成。
 * </p>
 *
 * @author SoundRead
 */
@Data
@TableName("novel_chapter")
public class NovelChapter {

   /** 章节 ID（雪花算法自动生成） */
   @TableId(type = IdType.ASSIGN_ID)
   private Long id;

   /** 所属有声书项目 ID（关联 novel_project.id） */
   private Long projectId;

   /** 章节序号（从 1 开始，按原文顺序递增） */
   private Integer chapterIndex;

   /** 章节标题（由 AI 从原文提取或自动生成） */
   private String title;

   /** 章节原始文本（未经标注的纯文字内容） */
   private String rawText;

   /** 章节字符数（用于进度和存储估算） */
   private Integer charCount;

   /** 章节下的语义段落总数 */
   private Integer totalSegments;

   /**
    * 章节处理状态
    * <ul>
    * <li>{@code pending} — 等待处理</li>
    * <li>{@code splitting} — 正在切割段落</li>
    * <li>{@code annotating} — 正在进行情感标注</li>
    * <li>{@code synthesizing}— 正在 TTS 合成音频</li>
    * <li>{@code completed} — 处理完成</li>
    * <li>{@code failed} — 处理失败</li>
    * </ul>
    */
   private String status;

   /** 章节合成后的音频 URL（存储于 R2，处理中为 null） */
   private String audioUrl;

   /** 章节音频总时长（秒，合成完成后写入） */
   private Integer audioDuration;

   /** AI 生成的情感状态摘要（用于跨章节情感连贯性传递） */
   private String emotionSummary;

   /** 记录创建时间（自动填充） */
   @TableField(fill = FieldFill.INSERT)
   private LocalDateTime createdAt;
}
