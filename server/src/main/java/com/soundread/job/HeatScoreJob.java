package com.soundread.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soundread.mapper.WorkMapper;
import com.soundread.model.entity.Work;
import com.soundread.service.AdminWorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 热度分定时刷新任务
 *
 * <p>
 * 每 5 分钟批量刷新所有已通过审核的作品的 heat_score，
 * 公式：播放×1 + 点赞×3 + 分享×5 + 评论×2 + 精选加权50
 * </p>
 *
 * <p>
 * 为什么不实时计算？高并发下每次播放/点赞都 UPDATE 热度分会打爆数据库。
 * 批量定时刷新是内容平台的标准做法（抖音/B站同理）。
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatScoreJob {

    private final WorkMapper workMapper;
    private final AdminWorkService adminWorkService;

    /**
     * 每 5 分钟执行一次热度刷新
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void refreshAll() {
        List<Long> ids = workMapper.selectList(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getReviewStatus, "approved")
                        .select(Work::getId))
                .stream().map(Work::getId).toList();

        if (ids.isEmpty()) {
            return;
        }

        ids.forEach(adminWorkService::refreshHeatScore);
        log.info("[HeatScoreJob] 热度刷新完成, 共{}条作品", ids.size());
    }
}
