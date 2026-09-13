package com.wishfox.foxsdk.core;

import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;

import com.tencent.bugly.crashreport.CrashReport;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Small, rate limited diagnostics helper used at SDK boundaries.  It deliberately
 * keeps only lifecycle breadcrumbs and never stores user credentials.
 */
public final class FoxSdkDiagnostics {

    private static final int MAX_BREADCRUMBS = 40;
    private static final long REPORT_INTERVAL_MS = 60_000L;
    private static final Deque<String> breadcrumbs = new ArrayDeque<>();
    private static long lastFailureReportAt;

    private FoxSdkDiagnostics() {
    }

    public static synchronized void record(String event, Activity activity, String detail) {
        String activityName = activity == null ? "null" : activity.getClass().getName();
        String item = System.currentTimeMillis() + "|" + event + "|" + activityName +
                (TextUtils.isEmpty(detail) ? "" : "|" + detail);
        breadcrumbs.addLast(item);
        while (breadcrumbs.size() > MAX_BREADCRUMBS) {
            breadcrumbs.removeFirst();
        }
    }

    public static synchronized void putContext(Activity activity, String route, String state) {
        if (!WishFoxSdk.isInitialized()) {
            return;
        }

        DiagnosticContext context = new DiagnosticContext(activity, route, state);
        try {
            android.content.Context sdkContext = WishFoxSdk.getContext();
            CrashReport.putUserData(sdkContext, "fox_overlay_activity", context.activity);
            CrashReport.putUserData(sdkContext, "fox_overlay_route", context.route);
            CrashReport.putUserData(sdkContext, "fox_overlay_state", context.state);
            CrashReport.putUserData(sdkContext, "fox_overlay_api", String.valueOf(Build.VERSION.SDK_INT));
            CrashReport.putUserData(sdkContext, "fox_overlay_breadcrumbs", breadcrumbs.toString());
        } catch (Throwable ignored) {
            // Diagnostics must never become the reason the SDK UI fails.
        }
    }

    public static synchronized void reportFailure(Activity activity, String route, String state, Throwable throwable) {
        long now = System.currentTimeMillis();
        record("failure", activity, throwable == null ? "unknown" : throwable.getClass().getName());
        if (now - lastFailureReportAt < REPORT_INTERVAL_MS) {
            return;
        }
        lastFailureReportAt = now;
        putContext(activity, route, state);
        if (throwable != null && WishFoxSdk.isInitialized()) {
            try {
                CrashReport.postCatchedException(throwable);
            } catch (Throwable ignored) {
                // Bugly is optional from the point of view of the overlay.
            }
        }
    }

    private static final class DiagnosticContext {
        final String activity;
        final String route;
        final String state;

        DiagnosticContext(Activity activity, String route, String state) {
            this.activity = activity == null ? "null" : activity.getClass().getName();
            this.route = route == null ? "unknown" : route;
            this.state = state == null ? "unknown" : state;
        }
    }
}
