package com.wishfox.foxsdk.ui.viewstate;

import android.os.Bundle;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 14:56
 */
public abstract class FoxSdkUiEffect {

    // 私有构造函数防止外部继承
    private FoxSdkUiEffect() {}

    /**
     * 显示Toast效果
     */
    public static final class ShowToast extends FoxSdkUiEffect {
        private final String message;

        public ShowToast(@NonNull String message) {
            this.message = message;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShowToast showToast = (ShowToast) o;
            return message.equals(showToast.message);
        }

        @Override
        public int hashCode() {
            return message.hashCode();
        }

        @NonNull
        @Override
        public String toString() {
            return "ShowToast{message='" + message + "'}";
        }
    }

    /**
     * 导航到指定页面效果
     */
    public static final class NavigateTo extends FoxSdkUiEffect {
        private final String destination;
        private final Bundle args;

        public NavigateTo(@NonNull String destination) {
            this(destination, null);
        }

        public NavigateTo(@NonNull String destination, @Nullable Bundle args) {
            this.destination = destination;
            this.args = args;
        }

        @NonNull
        public String getDestination() {
            return destination;
        }

        @Nullable
        public Bundle getArgs() {
            return args;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NavigateTo that = (NavigateTo) o;
            if (!destination.equals(that.destination)) return false;
            return args != null ? args.equals(that.args) : that.args == null;
        }

        @Override
        public int hashCode() {
            int result = destination.hashCode();
            result = 31 * result + (args != null ? args.hashCode() : 0);
            return result;
        }

        @NonNull
        @Override
        public String toString() {
            return "NavigateTo{destination='" + destination + "', args=" + args + "}";
        }
    }

    /**
     * 返回上一页效果
     */
    public static final class NavigateBack extends FoxSdkUiEffect {

        // 使用单例模式，因为不需要状态
        private static final NavigateBack INSTANCE = new NavigateBack();

        private NavigateBack() {}

        @NonNull
        public static NavigateBack getInstance() {
            return INSTANCE;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || (o != null && getClass() == o.getClass());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @NonNull
        @Override
        public String toString() {
            return "NavigateBack{}";
        }
    }

    // 类型检查方法
    public boolean isShowToast() {
        return this instanceof ShowToast;
    }

    public boolean isNavigateTo() {
        return this instanceof NavigateTo;
    }

    public boolean isNavigateBack() {
        return this instanceof NavigateBack;
    }

    // 安全类型转换方法
    @Nullable
    public ShowToast asShowToast() {
        return this instanceof ShowToast ? (ShowToast) this : null;
    }

    @Nullable
    public NavigateTo asNavigateTo() {
        return this instanceof NavigateTo ? (NavigateTo) this : null;
    }

    @Nullable
    public NavigateBack asNavigateBack() {
        return this instanceof NavigateBack ? (NavigateBack) this : null;
    }
}
