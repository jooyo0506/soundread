package com.soundread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步 & 定时任务线程池配置
 *
 * <p>
 * Spring @Scheduled 默认只有 1 个调度线程，多个 Job（HeatScoreJob + MusicService）
 * 共用时，一个卡住另一个就延迟。本类将定时任务线程数扩展为 4，并独立区分不同用途的线程池。
 * </p>
 *
 * <ul>
 * <li>{@code scheduledTaskPool} — 定时任务专用（4线程），防止 Job 互相阻断</li>
 * <li>{@code ioTaskExecutor} — IO密集型异步任务（TTS合成、外部API调用）</li>
 * <li>{@code r2UploadExecutor} — R2音频上传/下载专用，与业务IO隔离</li>
 * </ul>
 *
 * @author SoundRead
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements SchedulingConfigurer {

    /**
     * 定时任务独立线程池（防止 Job 互相阻断）
     *
     * <p>
     * 当前定时任务：HeatScoreJob（每5分钟）+ MusicService（每5秒轮询）
     * </p>
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
                4,
                r -> {
                    Thread t = new Thread(r, "scheduled-task-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                });
        registrar.setScheduler(scheduler);
    }

    /**
     * IO 密集型异步任务线程池（外部 API 调用、TTS 合成等）
     *
     * <p>
     * IO 密集公式：线程数 = CPU核数 × (1 + 平均等待时间/计算时间)
     * TTS 调用约 99% 在等 API，单核服务器可配置较大线程数。
     * 队列满时由调用者线程执行（CallerRunsPolicy），避免丢任务。
     * </p>
     */
    @Bean("ioTaskExecutor")
    public Executor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("io-task-");
        executor.setKeepAliveSeconds(60);
        // 队列满时由调用者线程执行（降级策略，避免丢任务）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * R2 音频上传/下载专用线程池
     *
     * <p>
     * 与 ioTaskExecutor 隔离，防止 R2 上传慢时占用 TTS 合成线程。
     * </p>
     */
    @Bean("r2UploadExecutor")
    public Executor r2UploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("r2-upload-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
