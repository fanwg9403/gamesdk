package com.wishfox.foxsdk.ui.view.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.lifecycle.ViewModelProvider;

import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.databinding.FsActivityAllinpaysdkAlipayBinding;
import com.wishfox.foxsdk.di.FoxSdkViewModelFactory;
import com.wishfox.foxsdk.domain.intent.FSAllinpaysdkAlipayIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.ui.viewmodel.FSAllinpaysdkAlipayViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSAllinpaysdkAlipayViewState;

/**
 * 通联支付，支付宝支付回调过度页面
 */
public class FSAllinpaysdkAlipayActivity extends FoxSdkBaseMviActivity<FSAllinpaysdkAlipayViewState, FSAllinpaysdkAlipayIntent, FsActivityAllinpaysdkAlipayBinding> {
    private FSAllinpaysdkAlipayViewModel viewModel;

    public static void start(Context context) {
        context.startActivity(new Intent(context, FSAllinpaysdkAlipayActivity.class));
    }
    @Override
    protected FSAllinpaysdkAlipayViewModel getViewModel() {
        if (viewModel == null) {
            FoxSdkViewModelFactory factory = new FoxSdkViewModelFactory();
            viewModel = new ViewModelProvider(this, factory).get(FSAllinpaysdkAlipayViewModel.class);
        }
        return viewModel;
    }

    @Override
    protected FsActivityAllinpaysdkAlipayBinding createBinding() {
        return  FsActivityAllinpaysdkAlipayBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {

        Intent intent = getIntent();
        Uri data = intent.getData();
        String scheme =  WishFoxSdk.getConfig().getKqFusedApplicationScheme();
        if (data != null && scheme.equals(data.getScheme())) {
            finish();
        }
    }

    @Override
    protected void renderState(FSAllinpaysdkAlipayViewState state) {

    }


}
