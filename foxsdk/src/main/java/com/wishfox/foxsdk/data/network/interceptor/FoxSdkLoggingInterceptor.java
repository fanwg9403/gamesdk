package com.wishfox.foxsdk.data.network.interceptor;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.utils.FoxSdkLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:51
 */
public class FoxSdkLoggingInterceptor implements Interceptor {

    private static final String TAG = "FoxSdk[Http]";
    private static final int MAX_LOG_LENGTH = 4000;

    private final FoxSdkConfig config;

    public FoxSdkLoggingInterceptor(FoxSdkConfig config) {
        this.config = config;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        // 如果不启用日志，直接执行请求
        if (!config.isEnableLog()) {
            return chain.proceed(chain.request());
        }

        Request request = chain.request();
        long requestStartTime = System.currentTimeMillis();

        // 打印请求信息
        logRequest(request);

        // 执行请求
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            logException(e, requestStartTime);
            throw e;
        }

        // 打印响应信息
        logResponse(response, requestStartTime);

        return response;
    }

    /**
     * 打印请求信息
     */
    private void logRequest(Request request) {
        try {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n");
            logBuilder.append("╔════════════════════════════════════════════════════════════════════════════════════════\n");
            logBuilder.append("║ 🌐 HTTP REQUEST\n");
            logBuilder.append("╠════════════════════════════════════════════════════════════════════════════════════════\n");
            logBuilder.append("║ Method: ").append(request.method()).append("\n");
            logBuilder.append("║ URL: ").append(request.url()).append("\n");

            // 打印请求头
            if (request.headers().size() > 0) {
                logBuilder.append("║ \n");
                logBuilder.append("║ Headers:\n");
                for (String name : request.headers().names()) {
                    logBuilder.append("║   ").append(name).append(": ")
                            .append(isSensitiveName(name) ? "***" : request.header(name)).append("\n");
                }
            }

            // 打印请求体
            if (request.body() != null) {
                logBuilder.append("║ \n");
                logBuilder.append("║ Body:\n");

                try {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);

                    String contentType = request.body().contentType() != null ?
                            request.body().contentType().toString() : null;
                    long contentLength = request.body().contentLength();

                    logBuilder.append("║   Content-Type: ").append(contentType).append("\n");
                    logBuilder.append("║   Content-Length: ").append(contentLength).append("\n");

                    if (request.body().contentLength() != 0L) {
                        logBuilder.append("║   \n");

                        if (contentType != null) {
                            if (contentType.contains("application/json")) {
                                String jsonString = buffer.readUtf8();
                                String formattedJson = formatJson(jsonString);
                                logBuilder.append("║   ").append(formatJsonForLogging(formattedJson)).append("\n");
                            } else if (contentType.contains("form-data")) {
                                String jsonString = buffer.readUtf8();
                                String formattedJson = formatJson(jsonString);
                                logBuilder.append("║   ").append(formatJsonForLogging(formattedJson)).append("\n");
                            } else if (contentType.contains("x-www-form-urlencoded")) {
                                String formString = buffer.readUtf8();
                                logBuilder.append("║   ").append(formatFormDataForLogging(formString)).append("\n");
                            } else {
                                logBuilder.append("║   [Binary Data - Content-Type: ").append(contentType).append("]\n");
                            }
                        }
                    }
                } catch (Exception e) {
                    logBuilder.append("║   [Error reading body: ").append(e.getMessage()).append("]\n");
                }
            }

            logBuilder.append("╚════════════════════════════════════════════════════════════════════════════════════════\n");

            logChunked(TAG, logBuilder.toString());
        } catch (Exception e) {
            FoxSdkLogger.e(TAG, "Failed to log request", e);
        }
    }

    /**
     * 打印响应信息
     */
    private void logResponse(Response response, long requestStartTime) {
        try {
            long responseTime = System.currentTimeMillis() - requestStartTime;
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n");
            logBuilder.append("╔════════════════════════════════════════════════════════════════════════════════════════\n");
            logBuilder.append("║ 📡 HTTP RESPONSE\n");
            logBuilder.append("╠════════════════════════════════════════════════════════════════════════════════════════\n");
            logBuilder.append("║ URL: ").append(response.request().url()).append("\n");
            logBuilder.append("║ Status: ").append(response.code()).append(" ").append(response.message()).append("\n");
            logBuilder.append("║ Response Time: ").append(responseTime).append("ms\n");
            logBuilder.append("║ Protocol: ").append(response.protocol()).append("\n");

            // 打印响应头
            if (response.headers().size() > 0) {
                logBuilder.append("║ \n");
                logBuilder.append("║ Headers:\n");
                for (String name : response.headers().names()) {
                    logBuilder.append("║   ").append(name).append(": ")
                            .append(isSensitiveName(name) ? "***" : response.header(name)).append("\n");
                }
            }

            // 打印响应体
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                logBuilder.append("║ \n");
                logBuilder.append("║ Body:\n");

                try {
                    BufferedSource source = responseBody.source();
                    source.request(Long.MAX_VALUE);
                    Buffer buffer = source.buffer();

                    String contentType = responseBody.contentType() != null ?
                            responseBody.contentType().toString() : null;
                    long contentLength = responseBody.contentLength();

                    logBuilder.append("║   Content-Type: ").append(contentType).append("\n");
                    logBuilder.append("║   Content-Length: ").append(contentLength).append("\n");

                    if (contentLength != 0L) {
                        logBuilder.append("║   \n");

                        String content = buffer.clone().readUtf8();

                        if (contentType != null) {
                            if (contentType.contains("application/json")) {
                                String formattedJson = formatJson(content);
                                logBuilder.append("║   ").append(formatJsonForLogging(formattedJson)).append("\n");
                            } else if (contentType.contains("text/plain")) {
                                logBuilder.append("║   ").append(formatTextForLogging(content)).append("\n");
                            } else if (contentType.contains("html")) {
                                logBuilder.append("║   [HTML Content - Length: ").append(content.length()).append("]\n");
                                // 选择性地打印HTML的前几行
                                String[] lines = content.split("\n");
                                if (lines.length > 0) {
                                    logBuilder.append("║   Preview:\n");
                                    for (int i = 0; i < Math.min(5, lines.length); i++) {
                                        logBuilder.append("║     ").append(lines[i]).append("\n");
                                    }
                                    if (lines.length > 5) {
                                        logBuilder.append("║     ... (truncated)\n");
                                    }
                                }
                            } else {
                                logBuilder.append("║   [Binary Data - Content-Type: ")
                                        .append(contentType)
                                        .append(", Length: ")
                                        .append(contentLength)
                                        .append("]\n");
                            }
                        }
                    }
                } catch (Exception e) {
                    logBuilder.append("║   [Error reading response body: ").append(e.getMessage()).append("]\n");
                }
            }

            logBuilder.append("╚════════════════════════════════════════════════════════════════════════════════════════\n");

            logChunked(TAG, logBuilder.toString());
        } catch (Exception e) {
            FoxSdkLogger.e(TAG, "Failed to log response", e);
        }
    }

    /**
     * 打印异常信息
     */
    private void logException(Exception e, long requestStartTime) {
        long responseTime = System.currentTimeMillis() - requestStartTime;
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n");
        logBuilder.append("╔════════════════════════════════════════════════════════════════════════════════════════\n");
        logBuilder.append("║ ❌ HTTP REQUEST FAILED\n");
        logBuilder.append("╠════════════════════════════════════════════════════════════════════════════════════════\n");
        logBuilder.append("║ Response Time: ").append(responseTime).append("ms\n");
        logBuilder.append("║ Exception: ").append(e.getClass().getSimpleName()).append("\n");
        logBuilder.append("║ Message: ").append(e.getMessage()).append("\n");

        // 打印堆栈信息（只打印前5行）
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            logBuilder.append("║ \n");
            logBuilder.append("║ StackTrace (first 5 lines):\n");
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                logBuilder.append("║   ").append(stackTrace[i].toString()).append("\n");
            }
        }

        logBuilder.append("╚════════════════════════════════════════════════════════════════════════════════════════\n");

        logChunked(TAG, logBuilder.toString());
    }

    /**
     * 格式化JSON字符串
     */
    private String formatJson(String jsonString) {
        try {
            JSONObject value = new JSONObject(jsonString);
            redact(value);
            return value.toString(2);
        } catch (Exception e1) {
            try {
                JSONArray value = new JSONArray(jsonString);
                redact(value);
                return value.toString(2);
            } catch (Exception e2) {
                return jsonString;
            }
        }
    }

    /**
     * 格式化JSON用于日志输出
     */
    private String formatJsonForLogging(String json) {
        String[] lines = json.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            result.append(line).append("\n║   ");
        }
        return result.toString();
    }

    /**
     * 格式化表单数据用于日志输出
     */
    private String formatFormDataForLogging(String formData) {
        String[] params = formData.split("&");
        StringBuilder result = new StringBuilder();
        for (String param : params) {
            String[] parts = param.split("=");
            if (parts.length == 2) {
                result.append(parts[0]).append(": ")
                        .append(isSensitiveName(parts[0]) ? "***" : parts[1]).append("\n║     ");
            } else {
                result.append(param).append("\n║     ");
            }
        }
        return result.toString();
    }

    private static boolean isSensitiveName(String name) {
        if (name == null) return false;
        String normalized = name.toLowerCase(Locale.US);
        return normalized.contains("token") || normalized.contains("password")
                || "authorization".equals(normalized) || "cookie".equals(normalized)
                || "set-cookie".equals(normalized);
    }

    private static void redact(JSONObject value) throws Exception {
        Iterator<String> keys = value.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object child = value.opt(key);
            if (isSensitiveName(key)) value.put(key, "***");
            else if (child instanceof JSONObject) redact((JSONObject) child);
            else if (child instanceof JSONArray) redact((JSONArray) child);
        }
    }

    private static void redact(JSONArray value) throws Exception {
        for (int index = 0; index < value.length(); index++) {
            Object child = value.opt(index);
            if (child instanceof JSONObject) redact((JSONObject) child);
            else if (child instanceof JSONArray) redact((JSONArray) child);
        }
    }

    /**
     * 格式化文本用于日志输出
     */
    private String formatTextForLogging(String text) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            result.append(line).append("\n║   ");
        }
        return result.toString();
    }

    /**
     * 分块打印长日志（解决Android Logcat单条日志长度限制）
     */
    private void logChunked(String tag, String message) {
        if (message.length() <= MAX_LOG_LENGTH) {
            FoxSdkLogger.d(tag, message);
            return;
        }

        // 分块打印
        int start = 0;
        while (start < message.length()) {
            int end = start + MAX_LOG_LENGTH;
            if (end > message.length()) {
                end = message.length();
            }

            // 尽量在行边界处分割
            String chunk = message.substring(start, end);
            if (end < message.length()) {
                int lastNewLine = chunk.lastIndexOf('\n');
                if (lastNewLine != -1 && lastNewLine > chunk.length() - 100) {
                    chunk = chunk.substring(0, lastNewLine + 1);
                    end = start + lastNewLine + 1;
                }
            }

            FoxSdkLogger.d(tag, chunk);
            start = end;
        }
    }
}
