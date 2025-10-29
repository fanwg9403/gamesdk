package com.wishfox.foxsdk.ui.base;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:04
 */
public abstract class LoadingState {

    public static class Idle extends LoadingState {}

    public static class Show extends LoadingState {
        private final String message;

        public Show(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class Dismiss extends LoadingState {}

    // 实例
    public static final Idle IDLE = new Idle();
    public static final Dismiss DISMISS = new Dismiss();
}
