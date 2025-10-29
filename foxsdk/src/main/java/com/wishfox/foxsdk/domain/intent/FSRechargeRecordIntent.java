package com.wishfox.foxsdk.domain.intent;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 19:01
 */
public abstract class FSRechargeRecordIntent implements FoxSdkViewIntent {

    public static class LoadInitial extends FSRechargeRecordIntent {
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

    public static class Refresh extends FSRechargeRecordIntent {
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

    public static class LoadMore extends FSRechargeRecordIntent {
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
