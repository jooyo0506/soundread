package com.soundread.controller.ttsdrama;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsDramaResponse {
    /**
     * 最终融合好的多人连续对话完整音频URL
     */
    private String audioUrl;
}
