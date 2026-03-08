package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 有声书项目实体（对应 novel_project 表）
 *
 * <p>
 * 一个有声书项目代表用户上传的一部完整小说，经 AI Pipeline 处理后：
 * 自动完成章节切割 → 情感标注 → TTS 合成，最终生成可播放的音频。
 * 项目状态流转：{@code draft} → {@code analyzing} → {@code annotating} →
 * {@code synthesizing} → {@code completed}
 * </p>
 *
 * @author SoundRead
 */
@Data
@TableName("novel_project")
public class NovelProject {

   /** 项目 ID（雪花算法自动生成） */
   @TableId(type = IdType.ASSIGN_ID)
   private Long id;

   /** 所属用户 ID（关联 user.id） */
   private Long userId;

   /** 项目标题（有声书名称） */
   private String title;

   /** 封面图片 URL（存储于 R2，可为 null） */
   private String coverUrl;

   /** 朗读音色 ID（关联 sys_voice.voice_id） */
   private String voiceId;

   /** 原始文本总字符数（用于进度估算） */
   private Integer totalChars;

   /** AI 切割出的总章节数 */
   private Integer totalChapters;

   /** 全书合成后的音频总时长（秒） */
   private Integer totalDuration;

   /**
    * 项目处理状态
    * <ul>
    * <li>{@code draft} — 已创建，尚未启动 Pipeline</li>
    * <li>{@code analyzing} — 正在进行 AI 章节切割分析</li>
    * <li>{@code annotating} — 正在进行 AI 情感标注</li>
    * <li>{@code synthesizing}— 正在进行 TTS 音频合成</li>
    * <li>{@code completed} — 全部处理完成</li>
    * <li>{@code failed} — 处理过程中出现异常</li>
    * </ul>
    */
   private String status;

   /** 整体处理进度百分比（0~100，-1 表示失败） */
   private Integer progress;

   /** 合成完成的完整音频 URL（存储于 R2，处理中为 null） */
   private String audioUrl;

   /** 字幕文件 URL（SRT 格式，存储于 R2，可为 null） */
   private String subtitleUrl;

   /** 记录创建时间（自动填充） */
   @TableField(fill = FieldFill.INSERT)
   private LocalDateTime createdAt;

   /** 记录最后更新时间（自动填充） */
   @TableField(fill = FieldFill.INSERT_UPDATE)
   private LocalDateTime updatedAt;

   /** 逻辑删除标记（0=正常，1=已删除） */
   @TableLogic
   private Integer deleted;
}
