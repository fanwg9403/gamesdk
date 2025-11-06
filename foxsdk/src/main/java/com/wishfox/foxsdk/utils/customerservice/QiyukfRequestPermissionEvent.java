package com.wishfox.foxsdk.utils.customerservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.qiyukf.unicorn.api.event.EventCallback;
import com.qiyukf.unicorn.api.event.UnicornEventBase;
import com.qiyukf.unicorn.api.event.entry.RequestPermissionEventEntry;
import com.wishfox.foxsdk.ui.view.dialog.FSPermissionMissTipsDialog;
import com.wishfox.foxsdk.ui.view.dialog.FSPrivacyFloatingTipsDialog;
import com.wishfox.foxsdk.utils.FoxSdkLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:41
 */
public class QiyukfRequestPermissionEvent implements UnicornEventBase<RequestPermissionEventEntry> {

    private final Context mApplicationContext;
    private final Map<String, String> h5MessageHandlerMap = new HashMap<>();
    private BasePopupView mPopupView;

    public QiyukfRequestPermissionEvent(Context context) {
        this.mApplicationContext = context;

        // 初始化权限映射
        h5MessageHandlerMap.put("android.permission.RECORD_AUDIO", "麦克风");
        h5MessageHandlerMap.put("android.permission.CAMERA", "相机");
        h5MessageHandlerMap.put("android.permission.READ_EXTERNAL_STORAGE", "存储");
        h5MessageHandlerMap.put("android.permission.WRITE_EXTERNAL_STORAGE", "存储");
        h5MessageHandlerMap.put("android.permission.READ_MEDIA_AUDIO", "多媒体文件");
        h5MessageHandlerMap.put("android.permission.READ_MEDIA_IMAGES", "多媒体文件");
        h5MessageHandlerMap.put("android.permission.READ_MEDIA_VIDEO", "多媒体文件");
        h5MessageHandlerMap.put("android.permission.POST_NOTIFICATIONS", "通知栏权限");
    }

    /**
     * 转换权限列表为可读字符串
     */
    private String transToPermissionStr(List<String> permissionList) {
        if (permissionList == null || permissionList.isEmpty()) {
            return "";
        }

        HashSet<String> set = new HashSet<>();
        for (String permission : permissionList) {
            if (h5MessageHandlerMap.containsKey(permission)) {
                set.add(h5MessageHandlerMap.get(permission));
            }
        }

        if (set.isEmpty()) {
            return "";
        }

        StringBuilder str = new StringBuilder();
        for (String temp : set) {
            str.append(temp).append("、");
        }

        if (str.length() > 0) {
            str.deleteCharAt(str.length() - 1);
        }
        return str.toString();
    }

    @Override
    public void onEvent(RequestPermissionEventEntry requestPermissionEventEntry,
                        Context context, EventCallback<RequestPermissionEventEntry> callback) {

        int type = requestPermissionEventEntry.getScenesType();

        if (type == 10) {
            // 通知栏权限，直接返回
            return;
        } else if (type == 5) {
            // 选择本地文件场景，需要添加额外权限
            List<String> list = new ArrayList<>();
            list.add(getImagePermission());
            list.add(getVideoPermission());
            list.add(getMediaAudioPermission());
            requestPermissionEventEntry.setPermissionList(list);
        }

        requestPermissionListBys(
                (Activity) context,
                requestPermissionEventEntry.getPermissionList(),
                getTypeName(type),
                new PermissionCallbackBys() {
                    @Override
                    public void onPermissionComplete(boolean isGranted) {
                        if (isGranted) {
                            callback.onProcessEventSuccess(requestPermissionEventEntry);
                        } else {
                            // 用户拒绝权限，SDK自行处理
                            // callback.onInterceptEvent();
                        }
                    }
                });
    }

    @Override
    public boolean onDenyEvent(Context context, RequestPermissionEventEntry requestPermissionEventEntry) {
        String permissionName = transToPermissionStr(requestPermissionEventEntry.getPermissionList());
        // 自定义拒绝后的处理逻辑
        return true;
    }

    /**
     * 获取场景名称
     */
    private String getTypeName(int type) {
        switch (type) {
            case 0:
                return "从本地选择媒体文件(视频和图片)";
            case 1:
                return "拍摄视频场景";
            case 2:
                return "保存图片到本地";
            case 3:
                return "保存视频到本地";
            case 4:
                return "选择本地视频";
            case 5:
                return "选择本地文件";
            case 6:
                return "选择本地图片";
            case 7:
                return "拍照";
            case 8:
                return "录音";
            case 9:
                return "视频客服";
            case 10:
                return "通知栏权限";
            default:
                return "";
        }
    }

    /**
     * 获取多媒体音频权限
     */
    private String getMediaAudioPermission() {
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    /**
     * 获取视频权限
     */
    private String getVideoPermission() {
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    /**
     * 获取图片权限
     */
    private String getImagePermission() {
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    /**
     * 请求权限列表
     */
    public void requestPermissionListBys(Activity activity, List<String> permission,
                                         String typeName, PermissionCallbackBys permissionCallback) {

        List<String> mPermission = new ArrayList<>();

        if (activity != null && permission != null) {
            if (XXPermissions.isGranted(activity, permission)) {
                permissionCallback.onPermissionComplete(true);
                return;
            }

            for (String s : permission) {
                if (!XXPermissions.isGranted(activity, s)) {
                    mPermission.add(s);
                }
            }
        }

        if (activity != null) {
            FSPrivacyFloatingTipsDialog dialog = new FSPrivacyFloatingTipsDialog(activity, mPermission);

            mPopupView = new XPopup.Builder(activity)
                    .isViewMode(true)
                    .asCustom(dialog)
                    .show();

            dialog.setMListener(view -> {
                XXPermissions.with(activity)
                        .permission(mPermission)
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean allGranted) {
                                if (!allGranted) {
                                    permissionCallback.onPermissionComplete(false);
                                    showDialog(activity, mPermission, typeName,
                                            transToPermissionStr(permission));
                                    return;
                                }
                                permissionCallback.onPermissionComplete(allGranted);
                            }

                            @SuppressLint("LongLogTag")
                            @Override
                            public void onDenied(List<String> permissions, boolean doNotAskAgain) {
                                FoxSdkLogger.d("QiyukfRequestPermissionEvent",
                                        "doNotAskAgain--" + doNotAskAgain);
                                permissionCallback.onPermissionComplete(false);
                                showDialog(activity, mPermission, typeName,
                                        transToPermissionStr(permission));
                            }
                        });
            });
        }
    }

    /**
     * 权限回调接口
     */
    public interface PermissionCallbackBys {
        void onPermissionComplete(boolean isGranted);
    }

    /**
     * 显示权限提示对话框
     */
    private void showDialog(Context activity, List<String> permissions,
                            String typeName, String permissionName) {

        String content;
        if (TextUtils.isEmpty(permissionName)) {
            content = "您已拒绝了 " + typeName + " 功能相关权限,如需使用,请手动授予权限";
        } else {
            content = "您已拒绝了 " + permissionName + " 相关权限,如需使用 " + typeName + " 功能,请手动授予权限";
        }

        FSPermissionMissTipsDialog dialog = new FSPermissionMissTipsDialog(activity, content);
        dialog.setMListener(view -> {
            XXPermissions.startPermissionActivity(activity, permissions);
        });

        new XPopup.Builder(activity)
                .isViewMode(true)
                .asCustom(dialog)
                .show();
    }
}
