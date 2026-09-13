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
 * Home screen rendered directly in the host Activity.
 *
 * <p>This view intentionally reuses the existing home layout and adapters. It
 * does not change the host window flags, requested orientation, or lifecycle,
 * so opening the SDK cannot pause a Unity/Cocos Activity.</p>
 */
public final class FSHomeOverlayView extends FrameLayout {

    public interface Callback {
        void onCloseRequested();

        void onOpenPage(FoxSdkOverlayManager.Page page);

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

    public FSHomeOverlayView(Activity activity, Callback callback) {
        this(activity, callback, null);
    }

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

    public FSHomeViewState getCurrentState() {
        return viewModel.getCurrentState();
    }

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Home is attached dynamically after the host window's initial
        // insets dispatch, so explicitly request the current values.
        ViewCompat.requestApplyInsets(this);
    }

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

    private void initHeaders() {
        actionAdapter.addHeaderView(userHead);
        if (FSUserInfo.getInstance() != null) {
            actionAdapter.addHeaderView(regionHead);
        }
    }

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

    private void addClick(View view, View.OnClickListener listener) {
        if (view != null) {
            viewDisposables.add(FoxSdkViewExt.setOnClickListener(view, listener));
        }
    }

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

    private void openPage(FoxSdkOverlayManager.Page page) {
        if (callback != null) {
            callback.onOpenPage(page);
        }
    }

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

    private void detachFromParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

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

    @Override
    protected void onDetachedFromWindow() {
        if (!destroyed) {
            destroy();
        }
        super.onDetachedFromWindow();
    }
}
