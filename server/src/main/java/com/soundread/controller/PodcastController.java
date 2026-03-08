package com.soundread.controller;

import com.soundread.common.RequireFeature;
import com.soundread.common.Result;
import com.soundread.model.dto.PodcastDto;
import com.soundread.service.PodcastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 播客 Controller
 */
@RestController
@RequestMapping("/api/podcast")
@RequiredArgsConstructor
public class PodcastController {

    private final PodcastService podcastService;

    /**
     * 生成播客 (同步 — 完整脚本)
     */
    @RequireFeature("ai_podcast")
    @PostMapping("/generate")
    public Result<String> generatePodcast(@Valid @RequestBody PodcastDto.GenerateRequest req) {
        String script = podcastService.generatePodcastScript(req.getSourceType(), req.getContent());
        // TODO: 将脚本传给 PodcastClient 合成音频
        return Result.ok(script);
    }

    /**
     * 获取主播预设组合
     */
    @GetMapping("/presets")
    public Result<List<PodcastDto.PresetResponse>> getPresets() {
        PodcastDto.PresetResponse preset = new PodcastDto.PresetResponse();
        preset.setName("黑猫侦探社系列");
        preset.setDescription("咪仔 & 大一先生");
        PodcastDto.VoiceInfo voiceA = new PodcastDto.VoiceInfo();
        voiceA.setVoiceId("zh_female_mizaitongxue_v2_saturn_bigtts");
        voiceA.setName("咪仔同学");
        voiceA.setGender("female");
        preset.setVoiceA(voiceA);
        PodcastDto.VoiceInfo voiceB = new PodcastDto.VoiceInfo();
        voiceB.setVoiceId("zh_male_dayixiansheng_v2_saturn_bigtts");
        voiceB.setName("大一先生");
        voiceB.setGender("male");
        preset.setVoiceB(voiceB);

        return Result.ok(List.of(preset));
    }
}
