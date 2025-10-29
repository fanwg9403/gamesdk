package com.wishfox.foxsdk.data.model.paging;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:27
 */
public final class PageRequestUtils {
    private PageRequestUtils() {
        // 防止实例化
    }

    /**
     * 快速创建PageRequest
     */
    public static FoxSdkPageRequest createPageRequest() {
        return new PageRequestBuilder().build();
    }

    /**
     * 创建带参数的PageRequest
     */
    public static FoxSdkPageRequest createPageRequest(PageRequestConfigurator configurator) {
        PageRequestBuilder builder = new PageRequestBuilder();
        if (configurator != null) {
            configurator.configure(builder);
        }
        return builder.build();
    }

    public interface PageRequestConfigurator {
        void configure(PageRequestBuilder builder);
    }
}
