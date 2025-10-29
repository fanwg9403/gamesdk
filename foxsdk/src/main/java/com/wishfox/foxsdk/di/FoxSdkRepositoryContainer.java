package com.wishfox.foxsdk.di;

import com.wishfox.foxsdk.data.repository.FSGameRecordRepository;
import com.wishfox.foxsdk.data.repository.FSHomeRepository;
import com.wishfox.foxsdk.data.repository.FSMessageRepository;
import com.wishfox.foxsdk.data.repository.FSRechargeRecordRepository;
import com.wishfox.foxsdk.data.repository.FSStarterPackRepository;
import com.wishfox.foxsdk.data.repository.FSWinFoxCoinRepository;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:38
 */
public class FoxSdkRepositoryContainer {

    private static FSStarterPackRepository _starterPackRepository;
    private static FSWinFoxCoinRepository _winFoxCoinRepository;
    private static FSHomeRepository _homeRepository;
    private static FSRechargeRecordRepository _rechargeRecordRepository;
    private static FSGameRecordRepository _gameRecordRepository;
    private static FSMessageRepository _messageRepository;

    /**
     * 获取新手礼包Repository
     */
    public static FSStarterPackRepository getStarterPackRepository() {
        if (_starterPackRepository == null) {
            _starterPackRepository = new FSStarterPackRepository();
        }
        return _starterPackRepository;
    }

    /**
     * 获取首页Repository
     */
    public static FSHomeRepository getHomeRepository() {
        if (_homeRepository == null) {
            _homeRepository = new FSHomeRepository();
        }
        return _homeRepository;
    }

    /**
     * 获取赢狐币Repository
     */
    public static FSWinFoxCoinRepository getWinFoxCoinRepository() {
        if (_winFoxCoinRepository == null) {
            _winFoxCoinRepository = new FSWinFoxCoinRepository();
        }
        return _winFoxCoinRepository;
    }

    /**
     * 获取消息列表Repository
     */
    public static FSMessageRepository getMessageRepository() {
        if (_messageRepository == null) {
            _messageRepository = new FSMessageRepository();
        }
        return _messageRepository;
    }

    /**
     * 获取充值记录Repository
     */
    public static FSRechargeRecordRepository getRechargeRecordRepository() {
        if (_rechargeRecordRepository == null) {
            _rechargeRecordRepository = new FSRechargeRecordRepository();
        }
        return _rechargeRecordRepository;
    }

    /**
     * 获取游戏记录Repository
     */
    public static FSGameRecordRepository getGameRecordRepository() {
        if (_gameRecordRepository == null) {
            _gameRecordRepository = new FSGameRecordRepository();
        }
        return _gameRecordRepository;
    }
}
