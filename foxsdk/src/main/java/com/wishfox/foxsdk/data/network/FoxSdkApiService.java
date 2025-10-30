package com.wishfox.foxsdk.data.network;

import com.wishfox.foxsdk.data.model.entity.FSCheckOrder;
import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.data.model.entity.FSCreateOrder;
import com.wishfox.foxsdk.data.model.entity.FSGameRecord;
import com.wishfox.foxsdk.data.model.entity.FSHomeBanner;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.data.model.entity.FSMessage;
import com.wishfox.foxsdk.data.model.entity.FSPageContainer;
import com.wishfox.foxsdk.data.model.entity.FSRechargeRecord;
import com.wishfox.foxsdk.data.model.entity.FSStarterPack;
import com.wishfox.foxsdk.data.model.entity.FSUserProfile;
import com.wishfox.foxsdk.data.model.entity.FSWinFoxCoin;
import com.wishfox.foxsdk.data.model.FoxSdkBaseResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:30
 */
public interface FoxSdkApiService {

    /**
     * 获取新手礼包列表（分页）
     */
    @GET("/api/mail/list")
    Single<FoxSdkBaseResponse<FSPageContainer<FSStarterPack>>> getStarterPacks(
            @QueryMap Map<String, Object> params
    );

    /**
     * 领取礼包
     */
    @POST("/api/mail/mark_all_received")
    Single<FoxSdkBaseResponse<Object>> receiveStarterPack(
            @QueryMap Map<String, String> params
    );

    /**
     * 登录接口
     */
    @GET("/api/user/login")
    Single<FoxSdkBaseResponse<FSLoginResult>> login(
            @QueryMap Map<String, String> params
    );

    /**
     * 获取用户信息
     */
    @GET("/api/user/user_info")
    Single<FoxSdkBaseResponse<FSUserProfile>> getUserInfo(
            @QueryMap Map<String, String> body
    );

    /**
     * 获取赢狐币列表（分页）
     */
    @GET("/api/java/busy_order_list")
    Single<FoxSdkBaseResponse<List<FSWinFoxCoin>>> getWinFoxCoins(
            @Query("page_num") int page,
            @Query("page_size") int pageSize
    );

    /**
     * 获取充值记录列表（分页）
     */
    @GET("/api/user/order_list")
    Single<FoxSdkBaseResponse<FSPageContainer<FSRechargeRecord>>> getRechargeRecords(
            @Query("channel_id") String channel_id,
            @Query("app_id") String app_id,
            @Query("page_num") int page,
            @Query("page_size") int pageSize
    );

    /**
     * 退出登录
     */
    @DELETE("/api/user/logout")
    Single<FoxSdkBaseResponse<Object>> logout();

    /**
     * 获取游戏记录列表（分页）
     */
    @GET("/api/user/game_play_log_list")
    Single<FoxSdkBaseResponse<FSPageContainer<FSGameRecord>>> getGameRecordList(
            @Query("page_num") int page,
            @Query("page_size") int pageSize,
            @Query("channel_id") String channelId,
            @Query("app_id") String appId
    );

    /**
     * 系统消息（公告）列表
     */
    @GET("/api/mail/list")
    Single<FoxSdkBaseResponse<FSPageContainer<FSMessage>>> getSystemMessages(
            @Query("page_num") int page,
            @Query("page_size") int pageSize,
            @Query("channel_id") String channelId,
            @Query("app_id") String appId,
            @Query("type") int type
    );

    /**
     * 标记消息已读（使用RequestBody）
     */
    @POST("/api/mail/mark_all_read")
    Single<FoxSdkBaseResponse<Object>> readWithBody(
            @Body RequestBody body
    );

    /**
     * 标记消息已读（使用Query参数）
     */
    @POST("/api/mail/mark_all_read")
    Single<FoxSdkBaseResponse<Object>> read(
            @Query("channel_id") String channelId,
            @Query("app_id") String appId
    );

    /**
     * 账户狐币余额
     */
    @POST("/api/java/user_virtual_info")
    Single<FoxSdkBaseResponse<FSCoinInfo>> getUserVirtualInfo();

    /**
     * 商品下单
     */
    @POST("/api/user/order")
    Single<FoxSdkBaseResponse<FSCreateOrder>> createOrder(
            @QueryMap Map<String, Object> params
    );

    /**
     * 订单查询接口
     */
    @GET("/api/user/order_detail")
    Single<FoxSdkBaseResponse<FSCheckOrder>> getOrderDetail(
            @QueryMap Map<String, Object> params
    );

    /**
     * 获取广告列表
     */
    @GET("/api/java/ad_list")
    Single<FoxSdkBaseResponse<List<FSHomeBanner>>> getAdvertiseList(
            @Query("page_num") int page,
            @Query("page_size") int pageSize,
            @Query("ad_place_code") String adPlace
    );

    /**
     * 发送短信验证码
     */
    @POST("/api/user/code")
    Single<FoxSdkBaseResponse<Object>> sendSmsCode(
            @QueryMap Map<String, String> params
    );
}
