package com.wishfox.foxsdk.domain.intent;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:58
 */
public abstract class FSHomeIntent implements FoxSdkViewIntent {

    public static class Login extends FSHomeIntent {
        private String phone;
        private String code;
        private int type;

        public Login(String phone, String code, int type) {
            this.phone = phone;
            this.code = code;
            this.type = type;
        }

        public String getPhone() { return phone; }
        public String getCode() { return code; }
        public int getType() { return type; }
    }

    public static class Logout extends FSHomeIntent {
        public Logout() {}
    }

    public static class Init extends FSHomeIntent {
        public Init() {}
    }
}