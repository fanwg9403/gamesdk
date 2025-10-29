package com.wishfox.foxsdk.utils.pay;

import android.content.Context;

import com.hjq.toast.Toaster;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.utils.FoxSdkConstant;
import com.wishfox.foxsdk.utils.FoxSdkSPUtils;

import java.util.Map;

import io.reactivex.rxjava3.core.Observable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:07
 */
public class FoxSdkWxPay {

    public static Observable<Boolean> wXMiniProgramPayment(Context context, Map<String, Object> map) {
        return Observable.fromCallable(() -> {
            if (!FoxSdkWechatService.isWeChatInstalled().blockingFirst()) {
                Toaster.show(context.getString(R.string.fs_str_wx_pay));
                return false;
            }

            FoxSdkSPUtils storage = FoxSdkSPUtils.getInstance();
            IWXAPI api = WXAPIFactory.createWXAPI(context, FoxSdkPayConfig.APP_ID);
            WXLaunchMiniProgram.Req req = new WXLaunchMiniProgram.Req();
            req.userName = FoxSdkPayConfig.MINI_PROGRAM_ID;

            StringBuilder pathBuilder = new StringBuilder();
            pathBuilder.append("contentPackages/payPage/payPage?token=")
                    .append(storage.get(FoxSdkConstant.AUTHORIZATION, ""));

            Map<String, Object> mutableMap = new java.util.HashMap<>(map);
            if (!"Android".equals(mutableMap.get("payChannel"))) {
                mutableMap.put("payChannel", "Android");
            }
            mutableMap.put("childPayType", ChildPayType.WX_PAY.getValue());

            for (Map.Entry<String, Object> entry : mutableMap.entrySet()) {
                Object value = entry.getValue();
                String valueStr = (value instanceof String) ?
                        (value != null ? (String) value : "") :
                        String.valueOf(value != null ? value : "");
                pathBuilder.append("&").append(entry.getKey()).append("=").append(valueStr);
            }

            req.path = pathBuilder.toString();
            req.miniprogramType = WXLaunchMiniProgram.Req.MINIPTOGRAM_TYPE_RELEASE;

            return api.sendReq(req);
        });
    }
}
