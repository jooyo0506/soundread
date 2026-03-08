package com.soundread.controller.ttsdrama;

import com.soundread.common.Result;
import com.soundread.model.entity.UserCreation;
import com.soundread.model.entity.User;
import com.soundread.service.AuthService;
import com.soundread.service.CreationService;
import com.soundread.service.StorageQuotaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 多角色剧本合成接口
 *
 * <p>
 * 支持多行对话剧本按角色分别指定音色进行合成，
 * 最终拼合为一段完整的戏剧音频。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/tts/drama")
@RequiredArgsConstructor
public class TtsDramaController {

    private final TtsDramaService ttsDramaService;
    private final AuthService authService;
    private final CreationService creationService;
    private final StorageQuotaService storageQuotaService;

    @PostMapping("/synthesize")
    public Result<TtsDramaResponse> synthesizeDrama(
            @RequestBody TtsDramaRequest request,
            HttpServletRequest httpRequest) {
        log.info("收到 TtsDrama 多角色合成请求，包含 {} 条台词",
                request.getLines() != null ? request.getLines().size() : 0);
        try {
            if (request.getLines() == null || request.getLines().isEmpty()) {
                return Result.fail("台词列表不能为空");
            }
            User user = authService.getCurrentUser();
            // 粗略估算存储占用：每字符约 200 字节
            int totalChars = request.getLines().stream()
                    .mapToInt(l -> l.getContent() != null ? l.getContent().length() : 0).sum();
            storageQuotaService.checkStorageQuota(user, totalChars * 200L);
            TtsDramaResponse response = ttsDramaService.synthesizeDrama(request);

            // 保存创作记录
            try {
                UserCreation creation = new UserCreation();
                creation.setUserId(user.getId());
                creation.setType("drama");
                // 拼接所有台词文本作为输入摘要
                StringBuilder sb = new StringBuilder();
                for (TtsDramaRequest.DialogLine line : request.getLines()) {
                    if (line.getContent() != null)
                        sb.append(line.getContent()).append("\n");
                }
                creation.setInputText(sb.toString().substring(0, Math.min(sb.length(), 500)));
                creation.setAudioUrl(response.getAudioUrl());
                creation.setAudioDuration(Math.max(1, (int) (sb.length() / 4.5)));
                creationService.save(creation);
            } catch (Exception ex) {
                log.warn("保存剧本创作记录失败: {}", ex.getMessage());
            }

            return Result.ok(response);
        } catch (Exception e) {
            log.error("TtsDrama 合成失败", e);
            return Result.fail("合成失败：" + e.getMessage());
        }
    }
}
