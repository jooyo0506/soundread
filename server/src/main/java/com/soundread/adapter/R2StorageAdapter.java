package com.soundread.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * Cloudflare R2 对象存储适配器
 */
@Slf4j
@Component
public class R2StorageAdapter {

        private final S3Client r2Client;
        private final S3Presigner r2Presigner;

        @Value("${r2.bucket-name}")
        private String bucketName;

        @Value("${r2.public-domain}")
        private String publicDomain;

        public R2StorageAdapter(S3Client r2Client, S3Presigner r2Presigner) {
                this.r2Client = r2Client;
                this.r2Presigner = r2Presigner;
        }

        /**
         * 上传合成音频 (公开访问)
         * 
         * @return 公开访问 URL
         */
        public String uploadAudio(byte[] audioData, String filename) {
                String folder = filename.startsWith("tts_v2_") ? "tts2" : "tts1";
                String key = "audio/" + folder + "/" + filename;
                r2Client.putObject(
                                PutObjectRequest.builder()
                                                .bucket(bucketName)
                                                .key(key)
                                                .contentType("audio/mp3")
                                                .build(),
                                RequestBody.fromBytes(audioData));
                log.info("音频上传至 R2: {}", key);
                return publicDomain + "/" + key;
        }

        /**
         * 上传 AI 音乐 (公开访问，存储到 audio/music/ 文件夹)
         *
         * @return 公开访问 URL
         */
        public String uploadMusic(byte[] audioData, String filename) {
                String key = "audio/music/" + filename;
                r2Client.putObject(
                                PutObjectRequest.builder()
                                                .bucket(bucketName)
                                                .key(key)
                                                .contentType("audio/mpeg")
                                                .build(),
                                RequestBody.fromBytes(audioData));
                log.info("AI 音乐上传至 R2: {} ({} bytes)", key, audioData.length);
                return publicDomain + "/" + key;
        }

        /**
         * 上传声音克隆样本 (私有访问)
         * 
         * @return 存储 key
         */
        public String uploadVoiceSample(byte[] sampleData, Long userId, String originalFilename) {
                String key = "samples/" + userId + "/" + UUID.randomUUID() + "/" + originalFilename;
                r2Client.putObject(
                                PutObjectRequest.builder()
                                                .bucket(bucketName)
                                                .key(key)
                                                .contentType("audio/wav")
                                                .build(),
                                RequestBody.fromBytes(sampleData));
                log.info("声音样本上传至 R2: {}", key);
                return key;
        }

        /**
         * 获取声音样本的签名下载 URL (有效期1小时)
         */
        public String getVoiceSamplePresignedUrl(String key) {
                return r2Presigner.presignGetObject(
                                GetObjectPresignRequest.builder()
                                                .signatureDuration(Duration.ofHours(1))
                                                .getObjectRequest(GetObjectRequest.builder()
                                                                .bucket(bucketName)
                                                                .key(key)
                                                                .build())
                                                .build())
                                .url().toString();
        }

        /**
         * 上传封面/静态资源
         */
        public String uploadAsset(byte[] data, String filename, String contentType) {
                String key = "assets/" + UUID.randomUUID() + "/" + filename;
                r2Client.putObject(
                                PutObjectRequest.builder()
                                                .bucket(bucketName)
                                                .key(key)
                                                .contentType(contentType)
                                                .build(),
                                RequestBody.fromBytes(data));
                return publicDomain + "/" + key;
        }

        /**
         * 根据公开 URL 删除对象 (数据保留策略清理用)
         *
         * @param publicUrl 完整的公开访问 URL
         */
        public void deleteByUrl(String publicUrl) {
                if (publicUrl == null || !publicUrl.startsWith(publicDomain)) {
                        log.warn("无法解析 R2 Key: {}", publicUrl);
                        return;
                }
                String key = publicUrl.substring(publicDomain.length() + 1);
                r2Client.deleteObject(
                                software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                                                .bucket(bucketName)
                                                .key(key)
                                                .build());
                log.info("R2 对象已删除: {}", key);
        }
}
