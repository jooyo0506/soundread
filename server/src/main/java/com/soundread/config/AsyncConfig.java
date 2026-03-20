package com.soundread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步 & 定时任务线程池配置
 *
 * <h3>设计思路</h3>
 * <ul>
 * <li>所有线程池均由 Spring 管理生命周期，应用关闭时 {@code waitForTasksToCompleteOnShutdown}
 * 确保正在执行的 TTS/上传任务能跑完，不会丢音频数据</li>
 * <li>参数基于单台 2C4G 云服务器调优，IO 密集任务主要瓶颈在网络等待</li>
 * <li>{@code ioTaskExecutor} 与 {@code r2UploadExecutor} 隔离，
 * 防止 R2 上传慢时占满 TTS 合成线程</li>
 * </ul>
 *
 * <h3>线程池一览</h3>
 * <table>
 * <tr>
 * <th>Bean</th>
 * <th>用途</th>
 * <th>核心/最大</th>
 * </tr>
 * <tr>
 * <td>taskScheduler</td>
 * <td>@Scheduled 定时任务</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>ioTaskExecutor</td>
 * <td>TTS 合成、外部 API 调用</td>
 * <td>4/16</td>
 * </tr>
 * <tr>
 * <td>r2UploadExecutor</td>
 * <td>R2 音频上传/下载</td>
 * <td>2/8</td>
 * </tr>
 * </table>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements SchedulingConfigurer {

    // ==================== 定时任务调度器 ====================

    /**
     * 定时任务专用调度器，替代 Spring 默认的单线程调度器。
     *
     * <p>
     * 当前定时任务：HeatScoreJob（每5分钟）+ MusicService（每5秒轮询），
     * 2 个线程足以避免互相阻断。由 Spring 管理关闭。
     * </p>
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setTaskScheduler(taskScheduler());
    }

    // ==================== IO 异步线程池 ====================

    /**
     * IO 密集型异步任务线程池（TTS 合成、外部 API 调用等）
     *
     * <p>
     * IO 密集公式：核心线程 = CPU核数 × 2，最大线程 = CPU核数 × 8。
     * 2C 服务器：core=4, max=16，队列 50 后触发扩容。
     * 队列满时由调用者线程同步执行（CallerRunsPolicy），兼做限流降级。
     * </p>
     */
    @Bean("ioTaskExecutor")
    public Executor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("io-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 空闲时回收核心线程，降低内存占用
        executor.setAllowCoreThreadTimeOut(true);
        // 优雅关闭：等待正在执行的 TTS 任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }

    // ==================== R2 存储线程池 ====================

    /**
     * R2 音频上传/下载专用线程池
     *
     * <p>
     * 与 ioTaskExecutor 隔离，防止 R2 上传延迟拖慢 TTS 合成。
     * 上传是后台操作，不影响用户感知延迟，参数可偏保守。
     * </p>
     */
    @Bean("r2UploadExecutor")
    public Executor r2UploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(30);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("r2-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        return executor;
    }
}
