package com.soundread.sdk.common;

import com.soundread.sdk.common.exception.SdkException;
import com.soundread.sdk.common.util.HttpUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * SDK基础客户端
 */
@Slf4j
public abstract class BaseClient {

    @Getter
    protected final String appId;

    @Getter
    protected final String accessToken;

    @Getter
    protected final String cluster;

    protected final HttpUtil httpUtil;

    /**
     * 构造函数
     */
    protected BaseClient(String appId, String accessToken, String cluster) {
        this.appId = appId;
        this.accessToken = accessToken;
        this.cluster = cluster;
        this.httpUtil = new HttpUtil();
        validate();
    }

    /**
     * 验证配置
     */
    protected void validate() {
        if (appId == null || appId.isEmpty()) {
            log.warn("AppId is not configured");
        }
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("AccessToken is not configured");
        }
    }

    /**
     * 验证必需参数
     */
    protected void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new SdkException.ValidationException(fieldName + " is required");
        }
    }

    /**
     * 验证文本长度
     */
    protected void validateTextLength(String text, int maxLength, String fieldName) {
        if (text == null || text.isEmpty()) {
            throw new SdkException.ValidationException(fieldName + " is required");
        }
        if (text.length() > maxLength) {
            throw new SdkException.ValidationException(
                    fieldName + " length exceeds maximum " + maxLength + " characters"
            );
        }
    }

    /**
     * 构建认证头
     */
    protected String buildAuthHeader() {
        return "Bearer; " + accessToken;
    }
}
