package com.wishfox.foxsdk.domain.intent;

import java.util.Objects;

/**
 * 主要功能:
 */
public abstract class FSAllinpaysdkAlipayIntent implements FoxSdkViewIntent {

    public static class LoadInitial extends FSAllinpaysdkAlipayIntent {
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

    public static class Refresh extends FSAllinpaysdkAlipayIntent {
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

    public static class LoadMore extends FSAllinpaysdkAlipayIntent {
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

    public static class ReceiveStarterPack extends FSAllinpaysdkAlipayIntent {
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
