package com.wishfox.foxsdk.auth;

import android.app.Activity;
import android.os.SystemClock;
import android.text.TextUtils;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.core.FoxSdkOverlayManager;
import com.wishfox.foxsdk.data.model.FoxSdkBaseResponse;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.data.model.entity.FSShortLogin;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.adapter.rxjava3.HttpException;

/** 单个 H5 文档的认证操作。所有状态只在主线程访问，不缓存/持久化短时 Token。 */
public final class FSH5AuthSession {
    public interface Result { void complete(String code, JSONObject data); }

    private final Activity host;
    private final FoxSdkConfig.H5SessionTokenProvider provider;
    private final String sessionId;
    private Operation pending;
    private long expiresAt;
    private long sessionRevision = -1;

    private static final class Operation {
        final Result result;
        FoxSdkOverlayManager.LoginCallback login;
        Disposable exchange;
        long revision;
        String nativeToken;
        Operation(Result result) { this.result = result; }
    }

    private static final class Exchange {
        final String token;
        final long seconds;
        final String error;
        Exchange(String token, long seconds, String error) {
            this.token = token; this.seconds = seconds; this.error = error;
        }
    }

    public FSH5AuthSession(Activity host, FoxSdkConfig config, String sessionId) {
        this.host = host;
        this.provider = config.getH5SessionTokenProvider();
        this.sessionId = sessionId;
    }

    private Object user() throws JSONException {
        FSLoginResult value = FSLoginResult.getInstance();
        if (value == null) return JSONObject.NULL;
        String phone = value.getPhone();
        String masked = TextUtils.isEmpty(phone) || phone.length() < 7 ? ""
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        return new JSONObject().put("id", value.getOpenId() == null ? JSONObject.NULL : value.getOpenId())
                .put("maskedMobile", masked);
    }

    public JSONObject state() throws JSONException {
        boolean loggedIn = !TextUtils.isEmpty(FSLoginResult.getTokenEd());
        if (!loggedIn || sessionRevision != FSLoginResult.getSessionRevision()) expiresAt = 0;
        long remaining = Math.max(0, expiresAt - SystemClock.elapsedRealtime());
        return new JSONObject().put("status", loggedIn ? "authenticated" : "anonymous")
                .put("user", loggedIn ? user() : JSONObject.NULL)
                .put("sessionMode", "short_token")
                .put("sessionStatus", expiresAt == 0 ? "none" : remaining > 0 ? "valid" : "expired")
                .put("sessionExpiresIn", remaining / 1000);
    }

    public void handle(String method, JSONObject params, Result result) throws JSONException {
        if ("auth.getState".equals(method)) { result.complete("OK", state()); return; }
        boolean login = "auth.login".equals(method);
        boolean refresh = "auth.refreshSession".equals(method);
        if (!login && !refresh) { result.complete("METHOD_NOT_SUPPORTED", null); return; }
        if ((params.has("exchangeH5Session") && !(params.opt("exchangeH5Session") instanceof Boolean))
                || (params.has("reason") && (!(params.opt("reason") instanceof String)
                || params.optString("reason").length() > 128))) {
            result.complete("INVALID_ARGUMENT", null); return;
        }
        // refresh 无条件交换，不能通过 exchangeH5Session=false 假成功。
        boolean exchange = refresh || params.optBoolean("exchangeH5Session", true);
        if (pending != null) { result.complete("BUSY", null); return; }
        if (TextUtils.isEmpty(FSLoginResult.getTokenEd()) && refresh) {
            result.complete("AUTH_REQUIRED", null); return;
        }
        Operation op = new Operation(result);
        pending = op;
        if (TextUtils.isEmpty(FSLoginResult.getTokenEd())) {
            op.login = new FoxSdkOverlayManager.LoginCallback() {
                @Override public void onSuccess(FSLoginResult ignored) {
                    if (pending != op) return;
                    op.login = null;
                    if (exchange) exchange(op); else succeed(op, null, 0);
                }
                @Override public void onCancelled() { finish(op, "USER_CANCELLED", null); }
                @Override public void onFailure(String code) { finish(op, code, null); }
            };
            FoxSdkOverlayManager.requestLogin(host, op.login);
        } else if (exchange) exchange(op);
        else succeed(op, null, 0);
    }

    private void exchange(Operation op) {
        op.nativeToken = FSLoginResult.getTokenEd();
        op.revision = FSLoginResult.getSessionRevision();
        if (TextUtils.isEmpty(op.nativeToken)) { finish(op, "AUTH_REQUIRED", null); return; }
        Single<Exchange> exchangeRequest = provider == null
                ? FoxSdkRetrofitManager.getApiService().getShortLogin().map(FSH5AuthSession::parseResponse)
                : providerExchange(op.nativeToken);
        op.exchange = exchangeRequest.subscribeOn(Schedulers.io()).timeout(15, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(value -> {
                    if (pending != op) return;
                    if (!sameAccount(op)) { finish(op, "AUTH_STATE_CHANGED", null); return; }
                    if (value.error != null) {
                        if ("AUTH_REQUIRED".equals(value.error)) {
                            // 只有后端明确宣告长期凭证失效才清除；网络错误不清除登录态。
                            if (!FSLoginResult.clearIfSessionMatches(op.revision, op.nativeToken)) {
                                finish(op, "AUTH_STATE_CHANGED", null);
                                return;
                            }
                            expiresAt = 0;
                        }
                        finish(op, value.error, null);
                    } else if (TextUtils.isEmpty(value.token) || value.token.trim().isEmpty()
                            || value.token.length() > 8192 || value.token.equals(op.nativeToken)) {
                        finish(op, "INVALID_SESSION_RESPONSE", null);
                    } else {
                        sessionRevision = op.revision;
                        // 有效期是可选元数据。短 Token 的真实失效由服务端业务接口判定，
                        // 原生不因为缺少/超出 expires_in 而拒绝把 Token 交给 H5。
                        expiresAt = value.seconds > 0
                                ? SystemClock.elapsedRealtime() + value.seconds * 1000
                                : 0;
                        succeed(op, value.token, value.seconds);
                    }
                }, error -> {
                    if (pending != op) return;
                    String code = exchangeError(error);
                    if ("AUTH_REQUIRED".equals(code)) {
                        if (!FSLoginResult.clearIfSessionMatches(op.revision, op.nativeToken)) {
                            finish(op, "AUTH_STATE_CHANGED", null);
                            return;
                        }
                        expiresAt = 0;
                    }
                    finish(op, !sameAccount(op) && !"AUTH_REQUIRED".equals(code)
                            ? "AUTH_STATE_CHANGED" : code, null);
                });
    }

    /** 宿主显式配置 provider 时继续支持覆盖默认接口，未配置则直接调用 SDK 的短时 Token 接口。 */
    private Single<Exchange> providerExchange(String capturedToken) {
        return Single.create(emitter -> {
            if (emitter.isDisposed()) return;
            try {
                provider.exchange(capturedToken, sessionId, new FoxSdkConfig.H5SessionTokenProvider.Callback() {
                    @Override public void onSuccess(String token, long seconds) {
                        if (!emitter.isDisposed()) emitter.onSuccess(new Exchange(token, seconds, null));
                    }
                    @Override public void onFailure(String code) {
                        if (!emitter.isDisposed()) emitter.onSuccess(new Exchange(null, 0, safeCode(code)));
                    }
                });
            } catch (RuntimeException failure) {
                if (!emitter.isDisposed()) emitter.onSuccess(new Exchange(null, 0, "SESSION_EXCHANGE_FAILED"));
            }
        });
    }

    private static Exchange parseResponse(FoxSdkBaseResponse<FSShortLogin> response) {
        if (response == null) return new Exchange(null, 0, "SESSION_EXCHANGE_FAILED");
        if (!response.isSuccess()) {
            return new Exchange(null, 0, response.getCode() == 401 ? "AUTH_REQUIRED"
                    : response.getCode() == 429 ? "RATE_LIMITED" : "SESSION_EXCHANGE_FAILED");
        }
        FSShortLogin data = response.getData();
        if (data == null) return new Exchange(null, 0, "INVALID_SESSION_RESPONSE");
        return new Exchange(data.getShortToken(), resolveExpiresIn(data), null);
    }

    private static long resolveExpiresIn(FSShortLogin data) {
        Long expiresIn = data.getExpiresIn();
        if (expiresIn != null && expiresIn > 0) return expiresIn;
        Long expireAt = data.getExpireAt();
        if (expireAt == null || expireAt <= 0) return 0;
        long expireAtSeconds = expireAt > 10_000_000_000L ? expireAt / 1000 : expireAt;
        return Math.max(0, expireAtSeconds - System.currentTimeMillis() / 1000);
    }

    private static String exchangeError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        if (error instanceof TimeoutException || cause instanceof java.net.SocketTimeoutException) return "TIMEOUT";
        HttpException http = error instanceof HttpException ? (HttpException) error : null;
        if (http != null && http.code() == 401) return "AUTH_REQUIRED";
        if (http != null && http.code() == 429) return "RATE_LIMITED";
        if (cause instanceof java.io.IOException) return "NETWORK_ERROR";
        return "SESSION_EXCHANGE_FAILED";
    }

    private boolean sameAccount(Operation op) {
        return op.revision == FSLoginResult.getSessionRevision()
                && TextUtils.equals(op.nativeToken, FSLoginResult.getTokenEd());
    }

    private static String safeCode(String code) {
        if ("AUTH_REQUIRED".equals(code) || "NETWORK_ERROR".equals(code)
                || "RATE_LIMITED".equals(code) || "SESSION_EXCHANGE_FAILED".equals(code)) return code;
        return "SESSION_EXCHANGE_FAILED";
    }

    private void succeed(Operation op, String token, long seconds) {
        try {
            JSONObject data = state();
            if (token != null) {
                data.put("sessionToken", token);
                if (seconds > 0) data.put("expiresIn", seconds);
            }
            finish(op, "OK", data);
        } catch (JSONException ignored) { finish(op, "SESSION_EXCHANGE_FAILED", null); }
    }

    private void finish(Operation op, String code, JSONObject data) {
        if (pending != op) return;
        pending = null;
        if (op.exchange != null) op.exchange.dispose();
        if (op.login != null) FoxSdkOverlayManager.cancelLogin(host, op.login);
        op.nativeToken = null;
        op.result.complete(code, data);
    }

    /** 换文档、关闭 WebView、宿主销毁：取消回调与本调用方登录，不把结果发给下一页。 */
    public void reset() {
        Operation previous = pending;
        pending = null;
        expiresAt = 0;
        sessionRevision = -1;
        if (previous == null) return;
        if (previous.exchange != null) previous.exchange.dispose();
        if (previous.login != null) FoxSdkOverlayManager.cancelLogin(host, previous.login);
        previous.nativeToken = null;
    }
}
