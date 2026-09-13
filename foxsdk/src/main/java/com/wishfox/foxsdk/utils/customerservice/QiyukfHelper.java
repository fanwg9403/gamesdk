package com.wishfox.foxsdk.utils.customerservice;

import android.app.Activity;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.wishfox.foxsdk.utils.FoxSdkLogger;

import java.util.Collections;
import java.util.List;

/**
 * 七鱼客服已停用。
 *
 * <p>保留这个空实现是为了让历史调用点保持源码兼容，同时确保七鱼 SDK
 * 不再参与初始化、运行和构建。</p>
 */
public class QiyukfHelper {

    private static final String TAG = "QiyukfHelper";
    private static QiyukfHelper instance;

    private QiyukfHelper() {
    }

    public static QiyukfHelper getInstance() {
        if (instance == null) {
            synchronized (QiyukfHelper.class) {
                if (instance == null) {
                    instance = new QiyukfHelper();
                }
            }
        }
        return instance;
    }

    public void init(Context context, Class<? extends AppCompatActivity> clazz) {
        FoxSdkLogger.d(TAG, "Qiyu customer service is disabled, skip init");
    }

    public void initKFSDK() {
        FoxSdkLogger.d(TAG, "Qiyu customer service is disabled, skip initKFSDK");
    }

    public void openCustomerService(Context context, String title, String sourceUrl,
                                    String sourceTitle, String otherInfo) {
        FoxSdkLogger.d(TAG, "Qiyu customer service is disabled, skip openCustomerService");
    }

    public void setUserInfo(YSFUserInfoBean user) {
        FoxSdkLogger.d(TAG, "Qiyu customer service is disabled, skip setUserInfo");
    }

    public void setUserInfoAndOpenCustomerService(Context context, YSFUserInfoBean user) {
        FoxSdkLogger.d(TAG, "Qiyu customer service is disabled, skip setUserInfoAndOpenCustomerService");
    }

    public boolean isStatusBarNotificationClick(Activity context) {
        return false;
    }

    public void addUnreadCountChangeListener(Object listener) {
        FoxSdkLogger.d(TAG, "Qiyu customer service is disabled, skip addUnreadCountChangeListener");
    }

    public Object queryLastMessage() {
        return null;
    }

    public List<Object> getSessionList() {
        return Collections.emptyList();
    }

    public void logout() {
        FoxSdkLogger.d(TAG, "Qiyu customer service is disabled, skip logout");
    }

    public void clearCache() {
        FoxSdkLogger.d(TAG, "Qiyu customer service is disabled, skip clearCache");
    }

    public static class YSFUserInfoBean {
        private String userId = "";
        private String authToken = "";
        private String realName = "";
        private String mobilePhone = "";
        private String email = "";
        private String avatar = "";
        private String account = "";
        private String sex = "";
        private String regDate = "";
        private String lastLogin = "";

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }

        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }

        public String getMobilePhone() { return mobilePhone; }
        public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        public String getAccount() { return account; }
        public void setAccount(String account) { this.account = account; }

        public String getSex() { return sex; }
        public void setSex(String sex) { this.sex = sex; }

        public String getRegDate() { return regDate; }
        public void setRegDate(String regDate) { this.regDate = regDate; }

        public String getLastLogin() { return lastLogin; }
        public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    }
}
