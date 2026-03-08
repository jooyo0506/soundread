package com.soundread.controller;

import com.soundread.common.Result;
import com.soundread.model.entity.SysVoice;
import com.soundread.model.entity.UserVoice;
import com.soundread.model.entity.User;
import com.soundread.service.AuthService;
import com.soundread.service.VoiceService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 音色库与购买接口
 *
 * <p>
 * 提供两类功能：
 * <ul>
 * <li>GET /library - 获取平台音色列表 + 用户已购音色标识</li>
 * <li>POST /purchase - 购买指定音色</li>
 * </ul>
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;
    private final AuthService authService;

    /**
     * 获取音色库列表
     *
     * <p>
     * 未登录用户也可查看，但不会返回资产（已购）信息。
     * </p>
     *
     * @param engine 引擎类型：tts-1.0 / tts-2.0 / podcast，默认 tts-1.0
     * @return 音色列表 + 用户已购 ID 集合
     */
    @GetMapping("/library")
    public Result<Map<String, Object>> getVoiceLibrary(
            @RequestParam(required = false, defaultValue = "tts-1.0") String engine) {
        User user = null;
        try {
            user = authService.getCurrentUser();
        } catch (Exception e) {
            // 未登录用户查看音色库，跳过资产查询
            log.debug("未登录用户查看音色库，跳过资产查询");
        }

        // 1. 获取平台音色列表
        List<SysVoice> systemVoices = voiceService.getSupportedVoices(engine);

        // 2. 获取用户已购音色 ID（仅登录用户）
        List<String> ownVoiceIds = null;
        if (user != null) {
            List<UserVoice> userVoices = voiceService.getUserOwnVoices(user.getId());
            ownVoiceIds = userVoices.stream()
                    .map(UserVoice::getVoiceId)
                    .toList();
        }

        Map<String, Object> data = new HashMap<>(4);
        data.put("list", systemVoices);
        data.put("owned", ownVoiceIds);

        return Result.ok(data);
    }

    /**
     * 购买音色
     *
     * @param voiceId   音色 ID
     * @param payMethod 支付方式：wechat / alipay，默认 wechat
     * @return 支付订单 ID
     */
    @PostMapping("/purchase")
    public Result<String> purchaseVoice(
            @RequestParam @NotBlank(message = "音色ID不能为空") String voiceId,
            @RequestParam(defaultValue = "wechat") String payMethod) {
        User user = authService.getCurrentUser();

        try {
            String orderId = voiceService.purchaseVoice(user.getId(), voiceId, payMethod);
            return Result.ok(orderId);
        } catch (Exception e) {
            log.error("音色购买失败: voiceId={} userId={}", voiceId, user.getId(), e);
            return Result.fail(e.getMessage());
        }
    }
}
