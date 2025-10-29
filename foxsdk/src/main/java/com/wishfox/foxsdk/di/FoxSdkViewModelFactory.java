package com.wishfox.foxsdk.di;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.wishfox.foxsdk.data.repository.FSGameRecordRepository;
import com.wishfox.foxsdk.data.repository.FSHomeRepository;
import com.wishfox.foxsdk.data.repository.FSMessageRepository;
import com.wishfox.foxsdk.data.repository.FSRechargeRecordRepository;
import com.wishfox.foxsdk.data.repository.FSStarterPackRepository;
import com.wishfox.foxsdk.data.repository.FSWinFoxCoinRepository;
import com.wishfox.foxsdk.ui.viewmodel.FSGameRecordViewModel;
import com.wishfox.foxsdk.ui.viewmodel.FSHomeViewModel;
import com.wishfox.foxsdk.ui.viewmodel.FSMessageViewModel;
import com.wishfox.foxsdk.ui.viewmodel.FSRechargeRecordViewModel;
import com.wishfox.foxsdk.ui.viewmodel.FSStarterPackViewModel;
import com.wishfox.foxsdk.ui.viewmodel.FSWinFoxCoinViewModel;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:38
 */
public class FoxSdkViewModelFactory implements ViewModelProvider.Factory {

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FSStarterPackViewModel.class)) {
            FSStarterPackRepository starterPackRepository = FoxSdkRepositoryContainer.getStarterPackRepository();
            return (T) new FSStarterPackViewModel(starterPackRepository);
        } else if (modelClass.isAssignableFrom(FSHomeViewModel.class)) {
            FSHomeRepository homeRepository = FoxSdkRepositoryContainer.getHomeRepository();
            return (T) new FSHomeViewModel(homeRepository);
        } else if (modelClass.isAssignableFrom(FSWinFoxCoinViewModel.class)) {
            FSWinFoxCoinRepository winFoxCoinRepository = FoxSdkRepositoryContainer.getWinFoxCoinRepository();
            return (T) new FSWinFoxCoinViewModel(winFoxCoinRepository);
        } else if (modelClass.isAssignableFrom(FSRechargeRecordViewModel.class)) {
            FSRechargeRecordRepository rechargeRecordRepository = FoxSdkRepositoryContainer.getRechargeRecordRepository();
            return (T) new FSRechargeRecordViewModel(rechargeRecordRepository);
        } else if (modelClass.isAssignableFrom(FSGameRecordViewModel.class)) {
            FSGameRecordRepository gameRecordRepository = FoxSdkRepositoryContainer.getGameRecordRepository();
            return (T) new FSGameRecordViewModel(gameRecordRepository);
        } else if (modelClass.isAssignableFrom(FSMessageViewModel.class)) {
            FSMessageRepository messageRepository = FoxSdkRepositoryContainer.getMessageRepository();
            return (T) new FSMessageViewModel(messageRepository);
        } else {
            throw new IllegalArgumentException("未知的 ViewModel class: " + modelClass.getName() + "，请先在FoxSdkRepositoryContainer中注册");
        }
    }
}
