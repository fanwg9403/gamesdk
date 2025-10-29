package com.wishfox.foxsdk.utils.customerservice;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.qiyukf.nimlib.sdk.NimIntent;
import com.qiyukf.nimlib.sdk.RequestCallback;
import com.qiyukf.nimlib.sdk.StatusBarNotificationConfig;
import com.qiyukf.nimlib.sdk.msg.constant.NotificationExtraTypeEnum;
import com.qiyukf.nimlib.sdk.msg.model.IMMessage;
import com.qiyukf.unicorn.api.ConsultSource;
import com.qiyukf.unicorn.api.OnBotEventListener;
import com.qiyukf.unicorn.api.UICustomization;
import com.qiyukf.unicorn.api.Unicorn;
import com.qiyukf.unicorn.api.UnreadCountChangeListener;
import com.qiyukf.unicorn.api.YSFOptions;
import com.qiyukf.unicorn.api.YSFUserInfo;
import com.qiyukf.unicorn.api.event.EventProcessFactory;
import com.qiyukf.unicorn.api.event.SDKEvents;
import com.qiyukf.unicorn.api.event.UnicornEventBase;
import com.qiyukf.unicorn.api.pop.Session;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSUserProfile;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:32
 */
public class QiyukfHelper {

    private final String key = "296904123fc762ad332610839363736a";
    private long tempId = 6676107;

    private static QiyukfHelper instance;

    private QiyukfHelper() {
        // 私有构造函数
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

    /**
     * 初始化七鱼客服
     */
    public void init(Context context, Class<? extends AppCompatActivity> clazz) {
        YSFOptions options = new YSFOptions();

        // 设置模板ID
        options.templateId = tempId;

        // 设置状态栏通知配置
        StatusBarNotificationConfig notificationConfig = new StatusBarNotificationConfig();
        notificationConfig.notificationSmallIconId = R.mipmap.ic_kf_icon;
        notificationConfig.notificationEntrance = clazz;
        notificationConfig.notificationExtraType = NotificationExtraTypeEnum.MESSAGE;
        options.statusBarNotificationConfig = notificationConfig;

        // 设置机器人事件监听器
        options.onBotEventListener = new OnBotEventListener() {
            @Override
            public boolean onUrlClick(Context context, String url) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
                return true;
            }
        };

        // 设置SDK事件
        options.sdkEvents = new SDKEvents();
        options.sdkEvents.eventProcessFactory = new EventProcessFactory() {
            @Override
            public UnicornEventBase eventOf(int eventType) {
                if (eventType == 5) {
                    return new QiyukfRequestPermissionEvent(context);
                }else {
                    return null;
                }
            }
        };

        // 设置UI自定义
        options.uiCustomization = new UICustomization();
        options.uiCustomization.leftAvatar = getDrawablePath(context, R.mipmap.ic_kf_icon);

        // 设置用户头像
        FSUserProfile userProfile = FSUserProfile.getInstance();
        if (userProfile != null && userProfile.getUserInfo() != null) {
            options.uiCustomization.rightAvatar = userProfile.getUserInfo().getAvatar();
        }

        // 配置七鱼客服
        Unicorn.config(context, key, options, new GlideImageLoader(context));
    }

    /**
     * 获取drawable资源的URI路径
     */
    public String getDrawablePath(Context context, int resourceId) {
        String packageName = context.getResources().getResourcePackageName(resourceId);
        String typeName = context.getResources().getResourceTypeName(resourceId);
        String entryName = context.getResources().getResourceEntryName(resourceId);

        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                packageName + '/' + typeName + '/' + entryName);
        return uri.toString();
    }

    /**
     * 初始化七鱼SDK
     */
    public void initKFSDK() {
        Unicorn.initSdk();
    }

    /**
     * 打开客服服务
     */
    public void openCustomerService(Context context, String title, String sourceUrl,
                                    String sourceTitle, String otherInfo) {
        ConsultSource source = new ConsultSource(sourceUrl, sourceTitle, otherInfo);
        Unicorn.openServiceActivity(context, title, source);
    }

    /**
     * 设置用户信息
     */
    public void setUserInfo(YSFUserInfoBean user, RequestCallback<Void> callback) {
        YSFUserInfo userInfo = new YSFUserInfo();
        userInfo.userId = user.getUserId();
        userInfo.authToken = user.getAuthToken();

        // CRM扩展字段
        String data = "[\n" +
                "        {\"key\":\"real_name\", \"value\":\"" + user.getRealName() + "\"},\n" +
                "        {\"key\":\"mobile_phone\", \"hidden\":true, \"value\":\"" + user.getMobilePhone() + "\"},\n" +
                "        {\"key\":\"email\", \"value\":\"" + user.getEmail() + "\"},\n" +
                "        {\"key\":\"avatar\", \"value\": \"" + user.getAvatar() + "\"},\n" +
                "        {\"index\":0, \"key\":\"account\", \"label\":\"账号\", \"value\":\"" + user.getAccount() + "\" , \"href\":\"http://example.domain/user/zhangsan\"},\n" +
                "        {\"index\":1, \"key\":\"sex\", \"label\":\"性别\", \"value\":\"" + user.getSex() + "\"},\n" +
                "        {\"index\":5, \"key\":\"reg_date\", \"label\":\"注册日期\", \"value\":\"" + user.getRegDate() + "\"},\n" +
                "        {\"index\":6, \"key\":\"last_login\", \"label\":\"上次登录时间\", \"value\":\"" + user.getLastLogin() + "\"}\n" +
                "        ]";
        userInfo.data = data;

        Unicorn.setUserInfo(userInfo, callback);
    }

    /**
     * 设置用户信息（无回调）
     */
    public void setUserInfo(YSFUserInfoBean user) {
        setUserInfo(user, null);
    }

    /**
     * 设置用户信息并打开客服
     */
    public void setUserInfoAndOpenCustomerService(Context context, YSFUserInfoBean user) {
        YSFUserInfo userInfo = new YSFUserInfo();
        userInfo.userId = user.getUserId();
        userInfo.authToken = user.getAuthToken();

        String data = "[\n" +
                "        {\"key\":\"real_name\", \"value\":\"" + user.getRealName() + "\"},\n" +
                "        {\"key\":\"mobile_phone\", \"hidden\":true, \"value\":\"" + user.getMobilePhone() + "\"},\n" +
                "        {\"key\":\"email\", \"value\":\"" + user.getEmail() + "\"},\n" +
                "        {\"key\":\"avatar\", \"value\": \"" + user.getAvatar() + "\"},\n" +
                "        {\"index\":0, \"key\":\"account\", \"label\":\"账号\", \"value\":\"" + user.getAccount() + "\" , \"href\":\" \"},\n" +
                "        {\"index\":1, \"key\":\"sex\", \"label\":\"性别\", \"value\":\"" + user.getSex() + "\"},\n" +
                "        {\"index\":5, \"key\":\"reg_date\", \"label\":\"注册日期\", \"value\":\"" + user.getRegDate() + "\"},\n" +
                "        {\"index\":6, \"key\":\"last_login\", \"label\":\"上次登录时间\", \"value\":\"" + user.getLastLogin() + "\"}\n" +
                "        ]";
        userInfo.data = data;

        Unicorn.setUserInfo(userInfo, new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void param) {
                Unicorn.openServiceActivity(context, "在线客服", null);
            }

            @Override
            public void onFailed(int errorCode) {
                // 错误码处理
                android.util.Log.e("QiyukfHelper", "设置用户信息失败，错误码: " + errorCode);
            }

            @Override
            public void onException(Throwable exception) {
                android.util.Log.e("QiyukfHelper", "设置用户信息异常", exception);
            }
        });
    }

    /**
     * 自定义UI
     */
    private UICustomization uiCustomization(Context context) {
        UICustomization customization = new UICustomization();
        customization.titleBackgroundResId = R.color.white;
        customization.titleBarStyle = 1;
        customization.hideKeyboardOnEnterConsult = true;
        customization.inputTextColor = R.color.color_333333;
        customization.rightAvatar = "";
        return customization;
    }

    /**
     * 检查状态栏通知点击
     */
    public boolean isStatusBarNotificationClick(Activity context) {
        Intent csintent = context.getIntent();
        if (csintent.hasExtra(NimIntent.EXTRA_NOTIFY_CONTENT)) {
            openCustomerService(context, "在线客服", "Main", "", "");
            context.setIntent(new Intent());
            context.finish();
            return true;
        }
        return false;
    }

    /**
     * 添加未读消息数变化监听器
     */
    public void addUnreadCountChangeListener(UnreadCountChangeListener listener) {
        Unicorn.addUnreadCountChangeListener(listener, true);
    }

    /**
     * 查询最后一条消息
     */
    public IMMessage queryLastMessage() {
        return Unicorn.queryLastMessage();
    }

    /**
     * 获取会话列表
     */
    public List<Session> getSessionList() {
        return com.qiyukf.unicorn.api.pop.POPManager.getSessionList();
    }

    /**
     * 退出登录
     */
    public void logout() {
        Unicorn.logout();
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        Unicorn.clearCache();
    }

    /**
     * 用户信息Bean
     */
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

        // Getter和Setter方法
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
