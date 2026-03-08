package com.soundread;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 声读 (SoundRead) - AI 语音合成与内容创作平台
 */
@SpringBootApplication
@MapperScan("com.soundread.mapper")
@EnableAsync
@EnableScheduling
public class SoundReadApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoundReadApplication.class, args);
    }
}
