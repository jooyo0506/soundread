package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 系统音色实体（对应 sys_voice 表）
 *
 * <p>
 * 存储平台内置的所有 TTS 音色信息，包含音色基础属性、引擎支持情况、
 * 价格策略以及 VIP 免费标记。支持 tts-1.0 / tts-2.0 / podcast 等多种引擎。
 * </p>
 *
 * @author SoundRead
 */
@Data
@TableName("sys_voice")
public class SysVoice {

   /** 音色记录自增 ID */
   @TableId(type = IdType.AUTO)
   private Integer id;

   /**
    * 音色唯一标识（voice_type）
    * <p>
    * 对应 TTS 引擎中的 voice_type 参数，如 {@code BV001_streaming}。
    * </p>
    */
   private String voiceId;

   /** 音色展示名称（如「温柔女声」「磁性男声」） */
   private String name;

   /** 语言标识（如 zh / en / ja 等） */
   private String language;

   /** 性别（male=男性 / female=女性） */
   private String gender;

   /** 音色分类（如「情感主播」「知识讲解」「广告配音」等） */
   private String category;

   /** 风格标签（逗号分隔，如「温柔,治愈,读书」） */
   private String tags;

   /** 支持的 TTS 引擎列表（逗号分隔，如 "tts-1.0,tts-2.0"） */
   private String supportedEngines;

   /** 音色详细描述（适用场景、音色特点等） */
   private String description;

   /** 音色试听 URL（供用户在购买前预览） */
   private String previewUrl;

   /** 音色售价（积分/元，0 表示免费） */
   private BigDecimal price;

   /**
    * VIP 免费使用标记
    *
    * <ul>
    * <li>{@code 0} — 非 VIP 免费，需单独购买</li>
    * <li>{@code 1} — VIP 会员免费使用</li>
    * </ul>
    */
   private Integer isVipFree;

   /** 音色上架状态（active=上架中 / offline=已下架） */
   private String status;

   /** 排序权重（数值越小越靠前） */
   private Integer sortOrder;

   /** 记录创建时间 */
   private LocalDateTime createdAt;

   /** 记录最后更新时间 */
   private LocalDateTime updatedAt;
}
