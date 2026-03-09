package com.soundread.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.soundread.model.entity.SysVoice;
import com.soundread.model.entity.UserVoice;

import java.util.List;

/**
 * 音色服务接口
 *
 * <p>
 * 统一管理系统音色（SysVoice）和用户个人音色（UserVoice）的查询与权限校验逻辑，包含：
 * <ul>
 * <li>按 TTS 引擎查询可用音色列表</li>
 * <li>校验用户对指定音色的使用权限（免费/VIP/已购买）</li>
 * <li>查询用户已购音色列表</li>
 * <li>音色购买入口</li>
 * <li>根据音色 ID 自动识别所属 TTS 引擎</li>
 * </ul>
 * </p>
 *
 * @author SoundRead
 */
public interface VoiceService extends IService<SysVoice> {

    /**
     * 获取指定引擎支持的音色列表
     *
     * @param engine TTS 引擎标识（如 tts-1.0 / tts-2.0 / podcast），传 null 返回所有引擎音色
     * @return 可用音色列表
     */
    List<SysVoice> getSupportedVoices(String engine);

    /**
     * 校验用户是否有权使用某个音色
     *
     * <p>
     * 校验优先级：免费音色 → VIP 免费音色（用户是 VIP）→ 已购买音色
     * </p>
     *
     * @param userId  当前用户 ID
     * @param voiceId 音色 ID
     * @param engine  TTS 引擎标识（用于确认音色归属引擎）
     * @return true=有权使用，false=无权使用
     */
    boolean checkUserVoicePermission(Long userId, String voiceId, String engine);

    /**
     * 查询用户自己购买的所有音色列表
     *
     * @param userId 用户 ID
     * @return 已购音色列表
     */
    List<UserVoice> getUserOwnVoices(Long userId);

    /**
     * 购买音色
     *
     * @param userId    购买用户 ID
     * @param voiceId   音色 ID
     * @param payMethod 支付方式（如 coin / wechat / alipay 等）
     * @return 订单 ID 或支付跳转 URL
     */
    String purchaseVoice(Long userId, String voiceId, String payMethod);

    /**
     * 根据音色 ID 自动识别所属 TTS 引擎
     *
     * <p>
     * 优先查询 sys_voice.supported_engines 字段确认引擎归属；
     * 若音色 ID 包含 tts-2.0 特征则使用 TTS 2.0（WebSocket 全双工协议）；
     * 否则回退到 tts-1.0（HTTP 同步接口）。
     * </p>
     *
     * @param voiceId 音色 ID
     * @return 引擎标识（tts-1.0 / tts-2.0 / podcast）
     */
    String detectVoiceEngine(String voiceId);

    /**
     * 查询音色的试听 URL（来自数据库缓存，避免重复合成）
     *
     * @param voiceId 音色 ID
     * @return 已存储的试听 URL，不存在则返回 null
     */
    String getPreviewUrl(String voiceId);

    /**
     * 将合成后的试听 URL 持久化到数据库
     *
     * @param voiceId    音色 ID
     * @param previewUrl 试听音频 URL
     */
    void savePreviewUrl(String voiceId, String previewUrl);
}
