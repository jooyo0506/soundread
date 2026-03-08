package com.soundread.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置
 *
 * <p>
 * 解决 JavaScript 前端 Long 精度丢失问题。
 * JS Number 最大安全整数为 2^53-1 = 9007199254740991，
 * 而雪花 ID (如 2027355973823287298) 超过此范围，
 * 导致前端收到的 ID 末位被四舍五入（变成 ...300），
 * 反向请求时 ID 不匹配 → 删除/更新操作静默失败。
 * </p>
 *
 * <p>
 * 修复策略：将所有 Long/long 类型字段在 JSON 序列化时
 * 转为 String，前端以字符串形式传递 ID，避免精度损失。
 * </p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            // modulesToInstall 是追加模块，不会覆盖 Spring Boot 自动注册的 JavaTimeModule
            builder.modulesToInstall(module);
        };
    }
}
