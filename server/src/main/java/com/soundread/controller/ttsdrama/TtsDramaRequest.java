package com.soundread.controller.ttsdrama;

import lombok.Data;
import java.util.List;

@Data
public class TtsDramaRequest {
    /**
     * 全局背景设定(可选)，如：在一个阴森的地下室
     */
    private String globalContext;

    /**
     * 多角色对话列表
     */
    private List<DialogLine> lines;

    /**
     * 用户标识
     */
    private String userKey;

    @Data
    public static class DialogLine {
        /**
         * 角色音色ID，对应 speaker (例如：BV001_male)
         */
        private String speakerVoiceType;

        /**
         * 具体台词内容(支持 [#语气指令])
         */
        private String content;
    }
}
