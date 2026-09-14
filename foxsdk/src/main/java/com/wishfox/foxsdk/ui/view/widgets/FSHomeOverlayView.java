package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;

import com.scwang.smart.refresh.header.ClassicsHeader;
import com.wishfox.foxsdk.BuildConfig;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.core.FoxSdkDiagnostics;
import com.wishfox.foxsdk.core.FoxSdkOverlayManager;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;
import com.wishfox.foxsdk.databinding.FsActivityHomeBinding;
import com.wishfox.foxsdk.di.FoxSdkRepositoryContainer;
import com.wishfox.foxsdk.domain.intent.FSHomeIntent;
import com.wishfox.foxsdk.ui.view.adapter.FSBannerAdapter;
import com.wishfox.foxsdk.ui.view.adapter.FSHomeActionAdapter;
import com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog;
import com.wishfox.foxsdk.ui.viewmodel.FSHomeViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSHomeViewState;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;
import com.youth.banner.Banner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * 直接渲染在宿主 Activity 中的 SDK 首页 Overlay。
 *
 * <p>页面复用现有首页布局和适配器，不修改宿主窗口标志、请求方向或生命周期，
 * 从而避免打开 SDK 时暂停 Unity/Cocos 宿主 Activity。</p>
 */
public final class FSHomeOverlayView extends FrameLayout {

    public interface Callback {
        /** 请求关闭首页 Overlay。 */
        void onCloseRequested();

        /**
         * 请求打开指定 SDK 子页面。
         *
         * @param page 要打开的页面
         */
        void onOpenPage(FoxSdkOverlayManager.Page page);

        /**
         * 请求打开网页页面。
         *
         * @param url 网页地址
         * @param showTitle 是否显示标题栏
         */
        void onOpenWeb(String url, boolean showTitle);
    }

    private final Activity activity;
    private final Callback callback;
    private final CompositeDisposable viewDisposables = new CompositeDisposable();
    private final FsActivityHomeBinding binding;
    private final FSHomeViewModel viewModel;

    private FSHomeActionAdapter actionAdapter;
    private View userHead;
    private View bannerHead;
    private View regionHead;
    private FSBannerAdapter bannerAdapter;
    private boolean destroyed;

    private final List<Pair<String, Integer>> actionItems = Arrays.asList(
            new Pair<>("游戏记录", 0),
            new Pair<>("充值记录", 1),
            new Pair<>("我的消息", 2),
            new Pair<>("SDK版本号：" + BuildConfig.XYH_GAME_SDK_VERSION_NAME, 3),
            new Pair<>("", -1)
    );

    /**
     * 创建首页 Overlay 并加载最新首页数据。
     *
     * @param activity 宿主 Activity
     * @param callback 页面交互回调
     */
    public FSHomeOverlayView(Activity activity, Callback callback) {
        this(activity, callback, null);
    }

    /**
     * 创建首页 Overlay，可选恢复之前保存的首页状态。
     *
     * @param activity 宿主 Activity
     * @param callback 页面交互回调
     * @param preservedState 需要恢复的首页状态
     */
    public FSHomeOverlayView(
            Activity activity,
            Callback callback,
            FSHomeViewState preservedState
    ) {
        super(activity);
        this.activity = activity;
        this.callback = callback;
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setBackgroundColor(getResources().getColor(android.R.color.transparent));
        setClickable(true);
        setFocusableInTouchMode(true);
        setOnKeyListener((view, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                    event != null &&
                    event.getAction() == KeyEvent.ACTION_UP) {
                if (callback != null) {
                    callback.onCloseRequested();
                }
                return true;
            }
            return false;
        });

        binding = FsActivityHomeBinding.inflate(LayoutInflater.from(activity), this, true);
        viewModel = new FSHomeViewModel(FoxSdkRepositoryContainer.getHomeRepository());
        try {
            initView();
            if (preservedState != null) {
                viewModel.restoreStateForOverlay(preservedState);
            }
            observeState();
            if (preservedState == null) {
                viewModel.dispatch(new FSHomeIntent.Init());
            }
        } catch (Throwable throwable) {
            viewDisposables.dispose();
            viewModel.disposeForOverlay();
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            throw new RuntimeException(throwable);
        }
    }

    /**
     * 获取当前首页状态，供页面切换时保存和恢复。
     *
     * @return 当前首页状态
     */
    public FSHomeViewState getCurrentState() {
        return viewModel.getCurrentState();
    }

    /** 初始化首页布局、安全区、Banner、操作列表及刷新监听。 */
    private void initView() {
        applyWindowInsets(
                binding.fsVTopSafeArea,
                findViewById(R.id.fs_v_start_safe_area),
                binding.fsHomeRoot
        );
        userHead = LayoutInflater.from(activity).inflate(R.layout.fs_layout_home_user_info, null);
        bannerHead = LayoutInflater.from(activity).inflate(R.layout.fs_layout_home_banner, null);
        regionHead = LayoutInflater.from(activity).inflate(R.layout.fs_layout_home_region, null);
        hideCustomerServiceEntry(regionHead);

        bannerAdapter = new FSBannerAdapter(new ArrayList<>(), url -> {
            if (callback != null) {
                callback.onOpenWeb(url, false);
            }
        });
        ((Banner) bannerHead.findViewById(R.id.fs_home_banner)).setAdapter(bannerAdapter);

        initActions();
        initHeaders();

        binding.fsVOutside.setOnClickListener(v -> {
            if (callback != null) {
                callback.onCloseRequested();
            }
        });
        binding.fsHomeRoot.setRefreshHeader(new ClassicsHeader(activity));
        binding.fsHomeRoot.setEnableLoadMore(false);
        binding.fsHomeRoot.setOnRefreshListener(refreshLayout ->
                viewModel.dispatch(new FSHomeIntent.Init()));
    }

    /**
     * 将系统窗口安全区应用到首页内容。
     *
     * @param topSafeView 顶部安全区视图
     * @param startSafeView 起始方向安全区视图
     * @param contentView 页面内容视图
     */
    private void applyWindowInsets(
            View topSafeView,
            View startSafeView,
            View contentView
    ) {
        FSOverlayInsets.apply(
                activity,
                this,
                topSafeView,
                startSafeView,
                contentView
        );
    }

    /**
     * 视图挂载后重新请求窗口安全区，确保动态添加时布局正确。
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 首页在宿主窗口首次分发 Insets 后动态挂载，因此需要主动请求当前值。
        ViewCompat.requestApplyInsets(this);
    }

    /** 初始化用户信息、快捷入口及点击处理。 */
    private void initActions() {
        TextView username = userHead.findViewById(R.id.fs_tv_username);
        TextView coin = userHead.findViewById(R.id.fs_stv_coin);
        username.setText(activity.getString(R.string.fs_login_now));
        coin.setVisibility(View.GONE);

        addClick(userHead.findViewById(R.id.fs_cl_top_bar), v -> showLoginDialog());
        addClick(regionHead.findViewById(R.id.fs_iv_coin), v -> {
            if (FSUserInfo.getInstance() == null) {
                showLoginDialog();
            } else {
                openPage(FoxSdkOverlayManager.Page.WIN_FOX_COIN);
            }
        });
        addClick(regionHead.findViewById(R.id.fs_iv_gift), v -> {
            if (FSUserInfo.getInstance() == null) {
                showLoginDialog();
            } else {
                openPage(FoxSdkOverlayManager.Page.STARTER_PACK);
            }
        });
        actionAdapter = new FSHomeActionAdapter(new ArrayList<>());
        actionAdapter.setOnItemClickListener((adapter, view, position) -> {
            List<Pair<String, Integer>> data = (List<Pair<String, Integer>>) adapter.getData();
            if (position < 0 || position >= data.size()) {
                return;
            }
            Pair<String, Integer> item = data.get(position);
            if (item == null || item.second == null) {
                return;
            }
            switch (item.second) {
                case 0:
                    openPage(FoxSdkOverlayManager.Page.GAME_RECORD);
                    break;
                case 1:
                    openPage(FoxSdkOverlayManager.Page.RECHARGE_RECORD);
                    break;
                case 2:
                    openPage(FoxSdkOverlayManager.Page.MESSAGE);
                    break;
                default:
                    break;
            }
        });
        binding.fsRv.setAdapter(actionAdapter);
    }

    /** 按当前登录状态添加首页头部视图。 */
    private void initHeaders() {
        actionAdapter.addHeaderView(userHead);
        if (FSUserInfo.getInstance() != null) {
            actionAdapter.addHeaderView(regionHead);
        }
    }

    /** 订阅 ViewModel 状态并在状态变化时刷新页面。 */
    private void observeState() {
        viewDisposables.add(viewModel.getViewState().subscribe(
                this::renderState,
                throwable -> FoxSdkDiagnostics.reportFailure(
                        activity,
                        "home",
                        "state_observer_failed",
                        throwable
                )
        ));
    }

    /**
     * 隐藏客服入口并将狐币入口调整到区域右侧。
     *
     * @param regionView 区域头部视图
     */
    private void hideCustomerServiceEntry(View regionView) {
        View serviceView = regionView.findViewById(R.id.fs_iv_service);
        View coinView = regionView.findViewById(R.id.fs_iv_coin);
        if (serviceView != null) {
            serviceView.setVisibility(View.GONE);
            serviceView.setOnClickListener(null);
        }
        if (coinView != null && coinView.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) coinView.getLayoutParams();
            params.endToStart = -1;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            coinView.setLayoutParams(params);
        }
    }

    /**
     * 为视图注册可自动释放的点击监听器。
     *
     * @param view 目标视图
     * @param listener 点击监听器
     */
    private void addClick(View view, View.OnClickListener listener) {
        if (view != null) {
            viewDisposables.add(FoxSdkViewExt.setOnClickListener(view, listener));
        }
    }

    /** 在宿主仍可用时显示登录对话框。 */
    private void showLoginDialog() {
        if (destroyed || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        try {
            new FSLoginDialog(activity)
                    .setOnLoginClickListener((phone, codeOrPassword, loginType, loadingDialog) ->
                            viewModel.dispatch(new FSHomeIntent.Login(
                                    phone,
                                    codeOrPassword,
                                    loginType
                            )))
                    .show();
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(activity, "home_login", "dialog_show_failed", throwable);
        }
    }

    /**
     * 通过回调请求打开 SDK 子页面。
     *
     * @param page 要打开的页面
     */
    private void openPage(FoxSdkOverlayManager.Page page) {
        if (callback != null) {
            callback.onOpenPage(page);
        }
    }

    /** 根据首页状态更新用户信息、快捷入口、Banner 和区域头部。 */
    private void renderState(FSHomeViewState state) {
        if (destroyed || state == null) {
            return;
        }

        try {
            binding.fsHomeRoot.finishRefresh();
            if (state.getUserInfo() != null && state.isLoginSuccess()) {
                if (state.getCoinInfo() != null) {
                    state.getUserInfo().setFoxCoin(parseCoin(state.getCoinInfo().getBalanceCoin()));
                }
                ((TextView) userHead.findViewById(R.id.fs_tv_username))
                        .setText(state.getUserInfo().getUserName());
                ((TextView) userHead.findViewById(R.id.fs_stv_coin)).setText(
                        activity.getString(R.string.fs_fox_coin) +
                                (state.getUserInfo().getFoxCoin() == null
                                        ? "0"
                                        : state.getUserInfo().getFoxCoin())
                );
                ((TextView) userHead.findViewById(R.id.fs_stv_coin)).setVisibility(View.VISIBLE);
                actionAdapter.setNewInstance(new ArrayList<>(actionItems));
            } else {
                ((TextView) userHead.findViewById(R.id.fs_tv_username))
                        .setText(activity.getString(R.string.fs_login_now));
                ((TextView) userHead.findViewById(R.id.fs_stv_coin)).setVisibility(View.GONE);
                actionAdapter.setNewInstance(Collections.singletonList(
                        new Pair<>("SDK版本号：" + BuildConfig.XYH_GAME_SDK_VERSION_NAME, 3)
                ));
            }

            updateBanner(state);
            updateRegion(state);
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(activity, "home", "render_failed", throwable);
        }
    }

    /**
     * 将服务端返回的狐币字符串转换为整数。
     *
     * @param value 服务端狐币数值
     * @return 转换后的整数，输入无效时返回 0
     */
    private int parseCoin(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return (int) Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /** 根据状态添加、更新或移除 Banner 头部。 */
    private void updateBanner(FSHomeViewState state) {
        boolean hasBanner = false;
        if (actionAdapter.getHeaderLayout() != null &&
                actionAdapter.getHeaderLayout().getChildCount() >= 2) {
            hasBanner = actionAdapter.getHeaderLayout().getChildAt(1) instanceof ConstraintLayout &&
                    ((ConstraintLayout) actionAdapter.getHeaderLayout().getChildAt(1))
                            .getChildAt(0) instanceof Banner;
        }
        if (state.getBannerList() != null && !state.getBannerList().isEmpty()) {
            bannerAdapter.setDatas(state.getBannerList());
            if (!hasBanner) {
                detachFromParent(bannerHead);
                actionAdapter.addHeaderView(bannerHead, 1);
            }
        } else if (hasBanner) {
            actionAdapter.removeHeaderView(bannerHead);
        }
    }

    /** 根据登录状态添加或移除用户区域头部。 */
    private void updateRegion(FSHomeViewState state) {
        boolean hasRegion;
        if (actionAdapter.getHeaderLayout() == null) {
            hasRegion = false;
        } else if (actionAdapter.getHeaderLayout().getChildCount() == 2) {
            hasRegion = actionAdapter.getHeaderLayout().getChildAt(1) instanceof ConstraintLayout &&
                    ((ConstraintLayout) actionAdapter.getHeaderLayout().getChildAt(1))
                            .getChildAt(0) instanceof ImageView;
        } else {
            hasRegion = actionAdapter.getHeaderLayout().getChildCount() == 3;
        }

        if (state.getUserInfo() != null && state.isLoginSuccess()) {
            if (!hasRegion) {
                actionAdapter.addHeaderView(
                        regionHead,
                        state.getBannerList() != null && !state.getBannerList().isEmpty() ? 2 : 1
                );
            }
        } else if (hasRegion) {
            actionAdapter.removeHeaderView(regionHead);
        }
    }

    /** 将视图从现有父容器中安全移除。 */
    private void detachFromParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    /** 销毁首页并释放订阅及 ViewModel 资源。 */
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        viewDisposables.dispose();
        try {
            viewModel.disposeForOverlay();
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(activity, "home", "view_model_dispose_failed", throwable);
        }
        try {
            com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog.dismissInstance();
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(activity, "home_login", "dialog_dispose_failed", throwable);
        }
    }

    /** 处理视图从窗口移除，确保资源释放逻辑执行。 */
    @Override
    protected void onDetachedFromWindow() {
        if (!destroyed) {
            destroy();
        }
        super.onDetachedFromWindow();
    }
}
