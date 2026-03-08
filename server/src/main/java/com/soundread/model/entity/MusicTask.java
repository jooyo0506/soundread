package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 音乐生成任务
 */
@Data
@TableName("music_task")
public class MusicTask {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 任务类型: song / instrumental / lyrics */
    private String taskType;

    /** Mureka 平台的任务 ID */
    private String murekaTaskId;

    /** 作品标题 */
    private String title;

    /** 风格提示词 */
    private String prompt;

    /** 歌词 (song 类型) */
    private String lyrics;

    /** 使用的模型 */
    private String model;

    /** pending / processing / succeeded / failed */
    private String status;

    /** 生成的音频 URL (R2 永久链接) */
    private String resultUrl;

    /** 流式播放 URL (HLS m3u8, 边生成边听) */
    private String streamUrl;

    /** 时长 (毫秒) */
    private Integer duration;

    /** 错误信息 */
    private String errorMsg;

    /** 歌词时间戳 JSON (来自 Mureka recognize API) [{start,end,text}] */
    private String lyricTimings;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

    @TableLogic
    private Integer deleted;
}
