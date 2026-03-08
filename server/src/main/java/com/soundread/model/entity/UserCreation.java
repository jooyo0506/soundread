package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户创作记录实体（对应 user_creation 表）
 *
 * <p>
 * 记录用户每次 TTS 合成的输出结果，涵盖普通 TTS、情感配音、AI 剧本、播客、有声书等类型。
 * 每条记录对应一段生成的音频文件，用于「我的创作库」展示和存储配额统计。
 * 发布后会在 Work 表中创建对应公开作品。
 * </p>
 *
 * @author SoundRead
 */
@Data
@TableName("user_creation")
public class UserCreation {

   /** 创作记录 ID（雪花算法，序列化为字符串防止前端精度损失） */
   @TableId(type = IdType.ASSIGN_ID)
   @JsonSerialize(using = ToStringSerializer.class)
   private Long id;

   /** 所属用户 ID（序列化为字符串，防止 JS 精度损失） */
   @JsonSerialize(using = ToStringSerializer.class)
   private Long userId;

   /**
    * 创作类型
    * <ul>
    * <li>{@code tts} — 普通 TTS 合成（对应前端 Create.vue 入口）</li>
    * <li>{@code emotion} — 情感配音 v2（对应 Emotion.vue）</li>
    * <li>{@code drama} — AI 剧本配音（对应 Emotion.vue 剧本模式）</li>
    * <li>{@code podcast} — AI 播客生成（对应 Podcast.vue）</li>
    * <li>{@code novel} — 有声书（对应 Novel.vue）</li>
    * <li>{@code studio_*}— AI 创作工坊发布的各类内容</li>
    * </ul>
    */
   private String type;

   /** 创作标题（展示在创作库列表中） */
   private String title;

   /** 输入文本（TTS 原文，超长时截断存储） */
   private String inputText;

   /** 使用的音色 ID（关联 sys_voice.voice_id） */
   private String voiceId;

   /** 合成音频的访问 URL（存储于 R2） */
   private String audioUrl;

   /** 音频时长（秒） */
   private Integer audioDuration;

   /**
    * 音频文件大小（字节）
    * <p>
    * 序列化为字符串，防止 JS Number 精度损失。
    * </p>
    */
   @JsonSerialize(using = ToStringSerializer.class)
   private Long fileSize;

   /** 字幕文件 URL（SRT 格式，无字幕时为 null） */
   private String subtitleUrl;

   /**
    * 扩展 JSON 字段
    * <p>
    * 存储各类额外元数据，如 AI 播客 system_prompt、音效配置、
    * 剧本角色设定等，格式为 JSON 字符串。
    * </p>
    */
   private String extraJson;

   /** 是否已发布到发现页（0=未发布，1=已发布） */
   private Integer isPublished;

   /**
    * 关联的公开作品 ID（关联 work.id）
    * <p>
    * 发布后写入，用于在发现页追踪对应作品。序列化为字符串防止精度损失。
    * </p>
    */
   @JsonSerialize(using = ToStringSerializer.class)
   private Long workId;

   /** 记录创建时间（自动填充） */
   @TableField(fill = FieldFill.INSERT)
   private LocalDateTime createdAt;

   /** 逻辑删除标记（0=正常，1=已删除） */
   @TableLogic
   private Integer deleted;
}
