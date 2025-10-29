package com.wishfox.foxsdk.domain.intent;

import java.util.Objects;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 19:01
 */
public abstract class FSStarterPackIntent implements FoxSdkViewIntent {

    public static class LoadInitial extends FSStarterPackIntent {
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

    public static class Refresh extends FSStarterPackIntent {
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

    public static class LoadMore extends FSStarterPackIntent {
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

    public static class ReceiveStarterPack extends FSStarterPackIntent {
        private String mailId;

        public ReceiveStarterPack(String mailId) {
            this.mailId = mailId;
        }

        public String getMailId() { return mailId; }
        public void setMailId(String mailId) { this.mailId = mailId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReceiveStarterPack that = (ReceiveStarterPack) o;
            return Objects.equals(mailId, that.mailId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mailId);
        }

        @Override
        public String toString() {
            return "ReceiveStarterPack{" +
                    "mailId='" + mailId + '\'' +
                    '}';
        }
    }
}
