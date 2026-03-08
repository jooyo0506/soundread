package com.soundread.sdk.common.exception;

/**
 * SDK异常基类
 */
public class SdkException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public SdkException(String message) {
        super(message);
        this.errorCode = "SDK_ERROR";
        this.errorMessage = message;
    }

    public SdkException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public SdkException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SDK_ERROR";
        this.errorMessage = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 配置未找到异常
     */
    public static class ConfigurationNotFoundException extends SdkException {
        public ConfigurationNotFoundException(String configKey) {
            super("CONFIG_NOT_FOUND", "Configuration not found: " + configKey);
        }
    }

    /**
     * API调用异常
     */
    public static class ApiException extends SdkException {
        private final int httpStatus;

        public ApiException(String message, int httpStatus) {
            super("API_ERROR", message);
            this.httpStatus = httpStatus;
        }

        public ApiException(String message, int httpStatus, Throwable cause) {
            super("API_ERROR " + message, cause);
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }

    /**
     * 认证异常
     */
    public static class AuthException extends SdkException {
        public AuthException(String message) {
            super("AUTH_ERROR", message);
        }
    }

    /**
     * 参数校验异常
     */
    public static class ValidationException extends SdkException {
        public ValidationException(String message) {
            super("VALIDATION_ERROR", message);
        }
    }
}
