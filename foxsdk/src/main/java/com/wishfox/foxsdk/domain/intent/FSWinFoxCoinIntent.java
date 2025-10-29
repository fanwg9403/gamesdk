package com.wishfox.foxsdk.domain.intent;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 19:01
 */
public abstract class FSWinFoxCoinIntent implements FoxSdkViewIntent {

    public static class LoadInitial extends FSWinFoxCoinIntent {
        public LoadInitial() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            return "LoadInitial{}";
        }
    }

    public static class Refresh extends FSWinFoxCoinIntent {
        public Refresh() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            return "Refresh{}";
        }
    }

    public static class LoadMore extends FSWinFoxCoinIntent {
        public LoadMore() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            return "LoadMore{}";
        }
    }
}
