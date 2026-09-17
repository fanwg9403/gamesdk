package com.wishfox.foxsdk.media;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/** 无 Android 依赖，便于独立测试 URL 和尺寸边界。 */
public final class FSMediaPolicy {
    public static final long IMAGE_BYTES = 20L * 1024 * 1024;
    private FSMediaPolicy() { }

    public static String origin(String value) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException("HTTPS origin required");
            }
            int port = uri.getPort();
            return "https://" + uri.getHost().toLowerCase(Locale.ROOT)
                    + ((port == -1 || port == 443) ? "" : ":" + port);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid HTTPS URL");
        }
    }

    public static boolean allowed(String url, List<String> origins) {
        if (url == null || url.length() > 4096 || origins == null) return false;
        try {
            URI uri = new URI(url);
            return uri.getRawFragment() == null && origins.contains(origin(url));
        } catch (Exception ignored) { return false; }
    }

    public static int[] fit(int viewportWidth, int viewportHeight, int mediaWidth, int mediaHeight) {
        if (viewportWidth <= 0 || viewportHeight <= 0 || mediaWidth <= 0 || mediaHeight <= 0)
            return new int[] {0, 0};
        double scale = Math.min((double) viewportWidth / mediaWidth, (double) viewportHeight / mediaHeight);
        return new int[] {Math.max(1, (int) Math.round(mediaWidth * scale)),
                Math.max(1, (int) Math.round(mediaHeight * scale))};
    }
}
