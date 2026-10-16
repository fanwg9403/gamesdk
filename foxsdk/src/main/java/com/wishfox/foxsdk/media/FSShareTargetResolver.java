package com.wishfox.foxsdk.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.wishfox.foxsdk.utils.FoxSdkLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/** 固定白名单；不接受 H5 提供的包名/组件，也不选择任意匹配的 Activity。 */
public final class FSShareTargetResolver {
    private static final String TAG = "FoxSdk[H5Share]";
    private FSShareTargetResolver() { }

    public enum Target {
        WECHAT_FRIEND("wechat_friend", "微信", "com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI"),
        WECHAT_TIMELINE("wechat_timeline", "微信", "com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI"),
        QQ_FRIEND("qq_friend", "QQ", "com.tencent.mobileqq", "com.tencent.mobileqq.activity.JumpActivity"),
        DOUYIN("douyin", "抖音", "com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.share.ui.SystemShareNewActivity"),
        XIAOHONGSHU("xiaohongshu", "小红书", "com.xingin.xhs", "com.xingin.xhs.routers.RouterPageActivity");

        public final String id;
        public final String appName;
        public final String packageName;
        private final String activityName;

        Target(String id, String appName, String packageName, String activityName) {
            this.id = id;
            this.appName = appName;
            this.packageName = packageName;
            this.activityName = activityName;
        }

        public ComponentName component() { return new ComponentName(packageName, activityName); }
    }

    public static Target find(String id) {
        for (Target target : Target.values()) if (target.id.equals(id)) return target;
        return null;
    }

    /** null 表示声明匹配，仍需捕获启动异常；不证明对方已经导入图片。 */
    public static String check(Context context, Target target, String mime) {
        if (target == null) return "INVALID_ARGUMENT";
        ComponentName component = target.component();
        String code = checkComponent(context, component, mime);
        FoxSdkLogger.d(TAG, "resolve: component=" + component.flattenToShortString()
                + ", version=" + version(context, target)
                + ", mime=" + mime + ", code=" + (code == null ? "OK" : code));
        return code;
    }

    private static String checkComponent(Context context, ComponentName component, String mime) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo app = pm.getApplicationInfo(component.getPackageName(), PackageManager.MATCH_DISABLED_COMPONENTS);
            if (!app.enabled) return "APP_DISABLED";
        } catch (PackageManager.NameNotFoundException missing) {
            return "APP_NOT_INSTALLED";
        } catch (RuntimeException failure) {
            return "LAUNCH_FAILED";
        }
        try {
            ActivityInfo info = pm.getActivityInfo(component, PackageManager.MATCH_DISABLED_COMPONENTS);
            int state = pm.getComponentEnabledSetting(component);
            FoxSdkLogger.d(TAG, "component state: component=" + component.flattenToShortString()
                    + ", enabledSetting=" + state + ", manifestEnabled=" + info.enabled
                    + ", exported=" + info.exported);
            // 目标应用可通过运行时设置关闭入口；显式 Intent 同样不能启动已禁用组件。
            boolean enabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    || (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && info.enabled);
            if (!enabled) return "TARGET_DISABLED";
            if (!info.exported || (info.permission != null
                    && context.checkCallingOrSelfPermission(info.permission) != PackageManager.PERMISSION_GRANTED)) {
                return "TARGET_ACCESS_DENIED";
            }
            // 保留隐式查询以核实目标组件确实声明接收这种图片，最终启动使用显式组件。
            Intent probe = new Intent(Intent.ACTION_SEND).setType(mime).setPackage(component.getPackageName());
            List<ResolveInfo> matches = pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo match : matches) {
                ActivityInfo candidate = match.activityInfo;
                if (candidate != null && component.equals(new ComponentName(candidate.packageName, candidate.name))) {
                    return null;
                }
            }
            return "TARGET_UNSUPPORTED";
        } catch (PackageManager.NameNotFoundException missing) {
            return "TARGET_UNSUPPORTED";
        } catch (SecurityException denied) {
            return "TARGET_ACCESS_DENIED";
        } catch (RuntimeException failure) {
            return "LAUNCH_FAILED";
        }
    }

    public static String version(Context context, Target target) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(target.packageName, 0);
            return info.versionName == null ? "unknown" : info.versionName;
        } catch (Exception ignored) { return "unknown"; }
    }

    public static JSONObject capabilities(Context context) throws JSONException {
        JSONObject targets = new JSONObject();
        for (Target target : Target.values()) {
            String code = check(context, target, "image/png");
            targets.put(target.id, new JSONObject().put("appName", target.appName)
                    .put("available", code == null)
                    .put("mode", "explicit_intent")
                    .put("code", code == null ? "OK" : code)
                    .put("message", code == null ? "" : FSMediaFeedback.message(code, target.appName))
                    .put("packageName", target.packageName)
                    .put("versionName", version(context, target))
                    .put("publicationResultAvailable", false));
        }
        return new JSONObject().put("targets", targets);
    }
}
