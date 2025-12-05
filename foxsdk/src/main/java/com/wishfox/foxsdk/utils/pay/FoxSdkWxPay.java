package com.wishfox.foxsdk.utils.pay;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;
import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.utils.FoxSdkConstant;
import com.wishfox.foxsdk.utils.FoxSdkSPUtils;

import java.net.URLEncoder;
import java.util.Map;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:07
 */
public class FoxSdkWxPay {

    public static Pair<Boolean, String> wXMiniProgramPayment(Context context, Map<String, Object> map) {
        if (!checkWechatInstallation(context)) {
            Toaster.show(context.getString(R.string.fs_str_wx_pay));
            return new Pair(false, context.getString(R.string.fs_str_wx_pay));
        }

        FoxSdkSPUtils storage = FoxSdkSPUtils.getInstance();
        Map<String, Object> mutableMap = new java.util.HashMap<>(map);
        if (!"Android".equals(mutableMap.get("payChannel"))) {
            mutableMap.put("payChannel", "Android");
        }
        mutableMap.put("childPayType", ChildPayType.WX_PAY.getValue());
        mutableMap.put("token", storage.get(FoxSdkConstant.AUTHORIZATION, ""));
        String resultQuery = "";
        try {
            resultQuery = new Gson().toJson(mutableMap);
        } catch (Exception e) {
            e.printStackTrace();
            resultQuery = "";
        }
        if (TextUtils.isEmpty(resultQuery))
            return new Pair(false, "打开微信失败，请联系客服");
        else
            return new Pair(true, resultQuery);
    }

    private static boolean checkWechatInstallation(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.tencent.mm", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
