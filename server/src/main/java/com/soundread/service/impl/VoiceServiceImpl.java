package com.soundread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.soundread.model.entity.SysVoice;
import com.soundread.model.entity.User;
import com.soundread.model.entity.UserVoice;
import com.soundread.model.entity.VoiceOrder;
import com.soundread.mapper.SysVoiceMapper;
import com.soundread.mapper.UserVoiceMapper;
import com.soundread.mapper.VoiceOrderMapper;
import com.soundread.mapper.UserMapper;
import com.soundread.service.VoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoiceServiceImpl extends ServiceImpl<SysVoiceMapper, SysVoice> implements VoiceService {

    private final SysVoiceMapper sysVoiceMapper;
    private final UserVoiceMapper userVoiceMapper;
    private final VoiceOrderMapper voiceOrderMapper;
    private final UserMapper userMapper;

    @Override
    public List<SysVoice> getSupportedVoices(String engine) {
        QueryWrapper<SysVoice> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "active");
        if (engine != null && !engine.isEmpty()) {
            wrapper.like("supported_engines", engine);
        }
        wrapper.orderByDesc("sort_order");
        return sysVoiceMapper.selectList(wrapper);
    }

    @Override
    public boolean checkUserVoicePermission(Long userId, String voiceId, String engine) {
        // tts-1.0 所有用户可用
        if ("tts-1.0".equals(engine) || "short".equals(engine)) {
            return true;
        }

        // 1. 查询音色配置
        QueryWrapper<SysVoice> voiceQuery = new QueryWrapper<>();
        voiceQuery.eq("voice_id", voiceId);
        SysVoice voice = sysVoiceMapper.selectOne(voiceQuery);
        if (voice == null) {
            return false;
        }

        // 2. 免费音色直接放行
        if (voice.getPrice() != null && voice.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        // 3. 检查是否 VIP 免费，且用户是有效 VIP
        User user = userMapper.selectById(userId);
        if (voice.getIsVipFree() != null && voice.getIsVipFree() == 1) {
            if (user != null && user.getVipLevel() != null && user.getVipLevel() > 0) {
                // VIP 未过期则放行
                if (user.getVipExpireTime() == null || user.getVipExpireTime().isAfter(LocalDateTime.now())) {
                    return true;
                }
            }
        }

        // 4. 检查用户是否已购买该音色
        QueryWrapper<UserVoice> uvQuery = new QueryWrapper<>();
        uvQuery.eq("user_id", userId).eq("voice_id", voiceId);
        UserVoice userVoice = userVoiceMapper.selectOne(uvQuery);
        if (userVoice != null) {
            // 永久授权或在有效期内
            if (userVoice.getExpireTime() == null || userVoice.getExpireTime().isAfter(LocalDateTime.now())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<UserVoice> getUserOwnVoices(Long userId) {
        QueryWrapper<UserVoice> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return userVoiceMapper.selectList(wrapper);
    }

    @Override
    public String purchaseVoice(Long userId, String voiceId, String payMethod) {
        // 查询音色配置
        QueryWrapper<SysVoice> voiceQuery = new QueryWrapper<>();
        voiceQuery.eq("voice_id", voiceId);
        SysVoice voice = sysVoiceMapper.selectOne(voiceQuery);
        if (voice == null) {
            throw new RuntimeException("音色不存在");
        }
        if (voice.getPrice() == null || voice.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("该音色无需购买");
        }

        // 创建订单
        VoiceOrder order = new VoiceOrder();
        order.setUserId(userId);
        order.setVoiceId(voiceId);
        order.setAmount(voice.getPrice());
        order.setPayMethod(payMethod);
        order.setStatus("pending");
        // 生成唯一交易号
        order.setTradeNo(UUID.randomUUID().toString().replace("-", ""));
        voiceOrderMapper.insert(order);

        // 模拟支付成功（待接入真实支付回调）
        this.mockPaySuccess(order.getId());

        return order.getId().toString();
    }

    // 模拟支付成功，将订单改为 paid 并授予音色使用权限
    private void mockPaySuccess(Long orderId) {
        VoiceOrder order = voiceOrderMapper.selectById(orderId);
        if (order != null && "pending".equals(order.getStatus())) {
            order.setStatus("paid");
            order.setPaidAt(LocalDateTime.now());
            voiceOrderMapper.updateById(order);

            // 写入用户音色授权记录
            UserVoice uv = new UserVoice();
            uv.setUserId(order.getUserId());
            uv.setVoiceId(order.getVoiceId());
            uv.setObtainWay("purchased");
            userVoiceMapper.insert(uv);
        }
    }

    @Override
    public String detectVoiceEngine(String voiceId) {
        QueryWrapper<SysVoice> wrapper = new QueryWrapper<>();
        wrapper.eq("voice_id", voiceId);
        SysVoice voice = sysVoiceMapper.selectOne(wrapper);
        if (voice == null) {
            return "tts-1.0"; // 兜底默认 v1
        }
        // 检查 supported_engines 是否包含 tts-2.0
        String engines = voice.getSupportedEngines();
        if (engines != null && engines.contains("tts-2.0")) {
            return "tts-2.0";
        }
        return "tts-1.0";
    }
}
