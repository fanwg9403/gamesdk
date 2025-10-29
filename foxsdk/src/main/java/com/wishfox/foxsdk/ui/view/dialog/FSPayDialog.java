package com.wishfox.foxsdk.ui.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.data.model.entity.FSCreateOrder;
import com.wishfox.foxsdk.data.model.entity.FSPayResult;
import com.wishfox.foxsdk.data.model.FoxSdkBaseResponse;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;
import com.wishfox.foxsdk.databinding.FsDialogPayBinding;
import com.wishfox.foxsdk.ui.view.widgets.FSLoadingDialog;
import com.wishfox.foxsdk.utils.FoxSdkPayEnum;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;
import com.wishfox.foxsdk.utils.pay.FoxSdkAliPay;
import com.wishfox.foxsdk.utils.pay.FoxSdkWechatService;
import com.wishfox.foxsdk.utils.pay.FoxSdkWxPay;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:03
 */
public class FSPayDialog extends Dialog {

    private FsDialogPayBinding binding;
    private FSLoadingDialog loading;

    // 支付参数
    private String channelId = WishFoxSdk.getConfig().getChannelId();
    private String appId = WishFoxSdk.getConfig().getAppId();
    private String mallId = "";
    private String mallName = "";
    private String price = "";
    private long orderTime = 0;
    private String cpOrderId = "";

    // 用于存储设置的值，在 onCreate 中设置
    private String payName;
    private String payInfo;
    private FoxSdkPayEnum payType;
    private boolean disableFoxCoinPay = false;

    private OnConfirmListener onConfirm;
    private OnPayCreateListener onPayCreate;

    public FSPayDialog(Context context) {
        super(context, R.style.FSLoadingDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FsDialogPayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.fsCheckRadioFoxCoin.setIcon(R.mipmap.fs_ic_fox_coin_pay);
        binding.fsCheckRadioFoxCoin.setText(getContext().getString(R.string.fs_fox_coin_pay));
        binding.fsCheckRadioAli.setIcon(R.mipmap.fs_ic_ali_pay);
        binding.fsCheckRadioAli.setText(getContext().getString(R.string.fs_ali_pay));
        binding.fsCheckRadioWechat.setIcon(R.mipmap.fs_ic_wechat_pay);
        binding.fsCheckRadioWechat.setText(getContext().getString(R.string.fs_wechat_pay));

        // 在绑定初始化后设置之前保存的值
        if (payName != null) {
            binding.fsTvName.setText(payName);
        }

        if (payInfo != null) {
            binding.fsTvInfo.setText(payInfo);
        }

        if (payType != null) {
            setPayTypeInternal(payType);
        }

        if (disableFoxCoinPay) {
            binding.fsCheckRadioFoxCoin.setEnabled(false);
        }

        setupConfirmButton();
        updateFoxCoinBalance();
    }

    private void setupConfirmButton() {
        // 初始化确认按钮状态
        updateConfirmButtonState();

        FoxSdkViewExt.setOnClickListener(binding.fsTvConfirm, v -> {
            if (!binding.fsTvConfirm.isEnabled()) return;

            loading = new FSLoadingDialog(getContext());
            loading.show();

            int selectedPosition = binding.fsCheckRadioGroup.getCheckedItemPosition();
            FoxSdkPayEnum payType = getPayTypeFromPosition(selectedPosition);

            if (payType != null) {
                pay(payType);
            } else {
                Toaster.show("请选择支付方式");
                loading.dismiss();
            }

            dismiss();
        });
    }

    private void updateConfirmButtonState() {
        boolean hasSelection = binding.fsCheckRadioGroup.getCheckedItem() != null;
        binding.fsTvConfirm.setEnabled(hasSelection);
        binding.fsTvConfirm.setAlpha(hasSelection ? 1.0f : 0.5f);
    }

    private FoxSdkPayEnum getPayTypeFromPosition(int position) {
        switch (position) {
            case 0:
                return FoxSdkPayEnum.FOX_COIN;
            case 1:
                return FoxSdkPayEnum.ALI_PAY;
            case 2:
                return FoxSdkPayEnum.WECHAT;
            default:
                return null;
        }
    }

    /**
     * 设置支付下单参数
     */
    public FSPayDialog setPayParams(String mallId, String mallName, String price,
                                    long orderTime, String cpOrderId) {
        this.mallId = mallId;
        this.mallName = mallName;
        this.price = price;
        this.orderTime = orderTime;
        this.cpOrderId = cpOrderId;
        return this;
    }

    /**
     * 执行支付
     */
    private void pay(FoxSdkPayEnum payType) {
        Map<String, Object> params = createPayParams(payType);

        Single.fromCallable(() -> FoxSdkRetrofitManager.getApiService().createOrder(params))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    handlePayResponse(response.blockingGet(), payType);
                }, throwable -> {
                    loading.dismiss();
                    Toaster.show(throwable.getMessage() != null ? throwable.getMessage() : "支付失败");
                });
    }

    private Map<String, Object> createPayParams(FoxSdkPayEnum payType) {
        Map<String, Object> params = new HashMap<>();
        params.put("channel_id", channelId);
        params.put("app_id", appId);
        params.put("mall_id", mallId);
        params.put("mall_name", mallName);
        params.put("price", price);
        params.put("order_time", orderTime);
        params.put("cp_order_id", cpOrderId);

        switch (payType) {
            case FOX_COIN:
                params.put("pay_type", 6);
                break;
            case ALI_PAY:
                params.put("pay_type", 1);
                break;
            case WECHAT:
                params.put("pay_type", 3);
                break;
        }

        return params;
    }

    private void handlePayResponse(FoxSdkBaseResponse<FSCreateOrder> response,
                                   FoxSdkPayEnum payType) {
        if (response.getCode() == 200 && response.getData() != null) {
            switch (payType) {
                case FOX_COIN:
                    handleFoxCoinPayment(response.getData());
                    break;
                case ALI_PAY:
                    handleAliPayment(response.getData());
                    break;
                case WECHAT:
                    handleWechatPayment(response.getData());
                    break;
            }
        } else {
            loading.dismiss();
            Toaster.show(response.getMsg() != null ? response.getMsg() : "支付失败");
        }
    }

    private void handleFoxCoinPayment(FSCreateOrder data) {
        loading.dismiss();
        if (onPayCreate != null) {
            String tradeNumber = data.getTrade_number() != null ? data.getTrade_number() :
                    (data.getBusy_code() != null ? data.getBusy_code() : "");
            onPayCreate.onPayCreate(new FSPayResult(true, tradeNumber, FoxSdkPayEnum.FOX_COIN));
        }
    }

    private void handleAliPayment(FSCreateOrder data) {
        if (data.getCode_url() != null) {
            boolean success = FoxSdkAliPay.payAliYiMa(getContext(), data.getCode_url(), data.getJump_url()).blockingLast();
            if (success && onPayCreate != null) {
                onPayCreate.onPayCreate(new FSPayResult(true, data.getPos_seq(), FoxSdkPayEnum.ALI_PAY));
            } else {
                Toaster.show("支付失败");
            }
        } else {
            Toaster.show("支付失败");
        }
        loading.dismiss();
    }

    private void handleWechatPayment(FSCreateOrder data) {
        FoxSdkWechatService.init(getContext());
        Map<String, Object> wxParams = createWechatParams();

        boolean success = FoxSdkWxPay.wXMiniProgramPayment(getContext(), wxParams).blockingLast();
        if (success && onPayCreate != null) {
            String posSeq = data.getPos_seq() != null ? data.getPos_seq() : "";
            onPayCreate.onPayCreate(new FSPayResult(true, posSeq, FoxSdkPayEnum.WECHAT));
        }
        loading.dismiss();
    }

    private Map<String, Object> createWechatParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("app_id", appId);
        params.put("mall_id", mallId);
        params.put("mall_name", mallName);
        params.put("price", price);
        params.put("pay_type", 1);
        params.put("channel_id", channelId);
        params.put("cp_order_id", cpOrderId);
        params.put("paySource", 23);
        return params;
    }

    private void updateFoxCoinBalance() {
        FSCoinInfo coinInfo = FSCoinInfo.getInstance();
        String balanceText = getContext().getString(R.string.fs_fox_coin_pay) +
                "（余额：" + (coinInfo != null ? coinInfo.getFoxCoin() : 0) + "）";
        binding.fsCheckRadioFoxCoin.setText(balanceText);
    }

    @Override
    public void show() {
        // 验证参数
        if (TextUtils.isEmpty(mallId) || TextUtils.isEmpty(mallName) || TextUtils.isEmpty(price) ||
                orderTime == 0L || TextUtils.isEmpty(cpOrderId)) {
            Toaster.show("请检查参数");
            return;
        }
        super.show();
    }

    // 设置支付名称
    public FSPayDialog setPayName(String text) {
        this.payName = text;
        if (binding != null) {
            binding.fsTvName.setText(text);
        }
        return this;
    }

    public FSPayDialog setPayName(int textRes) {
        this.payName = getContext().getString(textRes);
        if (binding != null) {
            binding.fsTvName.setText(textRes);
        }
        return this;
    }

    // 设置支付信息
    public FSPayDialog setPayInfo(String text) {
        this.payInfo = text;
        if (binding != null) {
            binding.fsTvInfo.setText(text);
        }
        return this;
    }

    public FSPayDialog setPayInfo(int textRes) {
        this.payInfo = getContext().getString(textRes);
        if (binding != null) {
            binding.fsTvInfo.setText(textRes);
        }
        return this;
    }

    // 设置支付类型
    public FSPayDialog setPayType(FoxSdkPayEnum payType) {
        this.payType = payType;
        if (binding != null) {
            setPayTypeInternal(payType);
        }
        return this;
    }

    private void setPayTypeInternal(FoxSdkPayEnum payType) {
        int position;
        switch (payType) {
            case FOX_COIN:
                position = 0;
                break;
            case ALI_PAY:
                position = 1;
                break;
            case WECHAT:
                position = 2;
                break;
            default:
                position = -1;
        }

        if (position >= 0) {
            binding.fsCheckRadioGroup.setCheckedItem(position);
        }
    }

    // 禁用狐币支付
    public FSPayDialog setDisableFoxCoinPay(boolean disable) {
        this.disableFoxCoinPay = disable;
        if (binding != null) {
            binding.fsCheckRadioFoxCoin.setEnabled(!disable);
        }
        return this;
    }

    // 设置确认监听器
    public FSPayDialog setOnConfirmListener(OnConfirmListener listener) {
        this.onConfirm = listener;
        return this;
    }

    // 设置支付创建监听器
    public FSPayDialog setOnPayCreateListener(OnPayCreateListener listener) {
        this.onPayCreate = listener;
        return this;
    }

    public interface OnConfirmListener {
        void onConfirm(FoxSdkPayEnum payType);
    }

    public interface OnPayCreateListener {
        void onPayCreate(FSPayResult result);
    }
}
