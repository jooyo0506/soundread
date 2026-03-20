package com.soundread.sdk.common.util;

import com.soundread.sdk.common.exception.SdkException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP工具类
 */
@Slf4j
public class HttpUtil {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;

    public HttpUtil() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public HttpUtil(int connectTimeout, int readTimeout, int writeTimeout) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();
    }

    /**
     * GET请求
     */
    public String get(String url, Map<String, String> headers) throws SdkException {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            return handleResponse(response);
        } catch (IOException e) {
            throw new SdkException.ApiException("GET request failed: " + e.getMessage(), -1, e);
        }
    }

    /**
     * GET请求（带查询参数）
     */
    public String get(String url, Map<String, String> headers, Map<String, String> params) throws SdkException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (params != null) {
            params.forEach(urlBuilder::addQueryParameter);
        }

        Request.Builder builder = new Request.Builder().url(urlBuilder.build());
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            return handleResponse(response);
        } catch (IOException e) {
            throw new SdkException.ApiException("GET request failed: " + e.getMessage(), -1, e);
        }
    }

    /**
     * POST请求（JSON）
     */
    public String postJson(String url, String jsonBody, Map<String, String> headers) throws SdkException {
        RequestBody body = RequestBody.create(jsonBody, JSON_TYPE);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        if (headers != null) {
            headers.forEach(builder::addHeader);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            return handleResponse(response);
        } catch (IOException e) {
            throw new SdkException.ApiException("POST request failed: " + e.getMessage(), -1, e);
        }
    }

    /**
     * 处理响应
     */
    private String handleResponse(Response response) throws SdkException {
        try {
            if (!response.isSuccessful()) {
                String errorBody = "";
                try {
                    errorBody = response.body() != null ? response.body().string() : "empty";
                } catch (IOException e) {
                    errorBody = "empty (failed to read body)";
                }
                throw new SdkException.ApiException(
                        "HTTP " + response.code() + ": " + errorBody,
                        response.code());
            }

            if (response.body() == null) {
                throw new SdkException.ApiException("Response body is empty", response.code());
            }

            try {
                return response.body().string();
            } catch (IOException e) {
                throw new SdkException.ApiException("Failed to read response body: " + e.getMessage(), -1, e);
            }
        } finally {
            response.close();
        }
    }

    /**
     * 获取OkHttpClient实例
     */
    public OkHttpClient getClient() {
        return client;
    }
}
