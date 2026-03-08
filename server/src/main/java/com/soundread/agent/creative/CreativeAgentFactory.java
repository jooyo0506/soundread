package com.soundread.agent.creative;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创作 Agent 工厂 — 策略模式注册中心
 *
 * <p>
 * 利用 Spring 的 {@code List<CreativeAgent>} 自动注入，
 * 将所有 @Component 标注的 CreativeAgent 实现类收集到路由表中。
 * 新增创作类型只需加一个实现类，零配置自动注册。
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Component
public class CreativeAgentFactory {

    // 初始容量 16，满足当前 8+ 种 Agent 类型 + 未来扩展，避免扩容
    private final Map<String, CreativeAgent> agentMap = new HashMap<>(16);

    /**
     * 利用 Spring 的 {@code List<CreativeAgent>} 自动注入，
     * 将所有 @Component 标注的 CreativeAgent 实现类收集到路由表。
     *
     * @param agents Spring 自动收集的全部 CreativeAgent Bean
     */
    public CreativeAgentFactory(List<CreativeAgent> agents) {
        for (CreativeAgent agent : agents) {
            agentMap.put(agent.getTypeCode(), agent);
            log.info("注册创作 Agent: {} → {}", agent.getTypeCode(), agent.getClass().getSimpleName());
        }
        log.info("共注册 {} 个创作 Agent", agentMap.size());
    }

    /**
     * 根据 typeCode 获取对应的创作 Agent
     *
     * @param typeCode 创作类型编码
     * @return 对应的 CreativeAgent 实现
     * @throws RuntimeException 不支持的创作类型
     */
    public CreativeAgent getAgent(String typeCode) {
        CreativeAgent agent = agentMap.get(typeCode);
        if (agent == null) {
            throw new RuntimeException("不支持的创作类型: " + typeCode + "，已注册: " + agentMap.keySet());
        }
        return agent;
    }

    /**
     * 检查是否支持某创作类型
     */
    public boolean supports(String typeCode) {
        return agentMap.containsKey(typeCode);
    }
}
