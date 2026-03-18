package com.wishfox.foxsdk.ui.viewmodel;

import com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest;
import com.wishfox.foxsdk.data.model.paging.PageConstants;
import com.wishfox.foxsdk.data.repository.FSAllinpaysdkAlipayRepository;
import com.wishfox.foxsdk.data.repository.FSStarterPackRepository;
import com.wishfox.foxsdk.domain.intent.FSAllinpaysdkAlipayIntent;
import com.wishfox.foxsdk.domain.intent.FSStarterPackIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSAllinpaysdkAlipayViewState;
import com.wishfox.foxsdk.ui.viewstate.FSStarterPackViewState;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;

import java.util.Collections;

/**
 * 主要功能:
 */
public class FSAllinpaysdkAlipayViewModel extends FoxSdkBaseMviViewModel<FSAllinpaysdkAlipayViewState, FSAllinpaysdkAlipayIntent, FoxSdkUiEffect> {

    private FSAllinpaysdkAlipayRepository starterPackRepository;
    private int currentPage = 1;
    private boolean hasMoreData = true;
    private int clickPosition = -1;

    public FSAllinpaysdkAlipayViewModel(FSAllinpaysdkAlipayRepository starterPackRepository) {
        this.starterPackRepository = starterPackRepository;
    }

    @Override
    protected FSAllinpaysdkAlipayViewState initialState() {
        return new FSAllinpaysdkAlipayViewState();
    }

    @Override
    protected void handleIntent(FSAllinpaysdkAlipayIntent intent) {
        if (intent instanceof FSAllinpaysdkAlipayIntent.LoadInitial) {

        } else if (intent instanceof FSAllinpaysdkAlipayIntent.Refresh) {

        } else if (intent instanceof FSAllinpaysdkAlipayIntent.LoadMore) {
        } else if (intent instanceof FSAllinpaysdkAlipayIntent.ReceiveStarterPack) {
        }
    }

    // 初次请求or刷新

}
