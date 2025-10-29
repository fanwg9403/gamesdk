package com.wishfox.foxsdk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.wishfox.foxsdk.core.WishFoxSdk;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:09
 */
public class FoxSdkSPUtils {
    private static final int MODE = Context.MODE_PRIVATE;
    private static final Map<String, FoxSdkSPUtils> sSPMap = new HashMap<>();

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private FoxSdkSPUtils(String spName) {
        WishFoxSdk.requireInitialized();
        String finalSpName = !TextUtils.isEmpty(spName) ? spName : "spUtils";
        sp = WishFoxSdk.getContext().getSharedPreferences(finalSpName, MODE);
        editor = sp.edit();
    }

    public static FoxSdkSPUtils getInstance() {
        return getInstance("WishFoxSdkSP");
    }

    public static FoxSdkSPUtils getInstance(String spName) {
        if (TextUtils.isEmpty(spName)) {
            spName = "WishFoxSdkSP";
        }

        FoxSdkSPUtils instance = sSPMap.get(spName);
        if (instance == null) {
            instance = new FoxSdkSPUtils(spName);
            sSPMap.put(spName, instance);
        }
        return instance;
    }

    // RxJava 风格的存储方法
    public Completable putAsync(String key, String value) {
        return Completable.fromAction(() -> put(key, value));
    }

    public Maybe<String> getStringAsync(String key) {
        return Maybe.fromCallable(() -> getString(key));
    }

    // 原有的同步方法
    public void put(String key, String value) {
        editor.putString(key, value).apply();
    }

    public String getString(String key) {
        return getString(key, "");
    }

    public String getString(String key, String defaultValue) {
        return sp.getString(key, defaultValue);
    }

    public void put(String key, int value) {
        editor.putInt(key, value).apply();
    }

    public int getInt(String key) {
        return getInt(key, -1);
    }

    public int getInt(String key, int defaultValue) {
        return sp.getInt(key, defaultValue);
    }

    // 其他类型的方法类似...
    public void put(String key, boolean value) {
        editor.putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sp.getBoolean(key, defaultValue);
    }

    public <T> Completable saveAsync(String keyword, T value) {
        return Completable.fromAction(() -> save(keyword, value));
    }

    public <T> Maybe<T> getAsync(String keyword, T defValue) {
        return Maybe.fromCallable(() -> get(keyword, defValue));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String keyword, T defValue) {
        if (defValue instanceof String) {
            return (T) sp.getString(keyword, (String) defValue);
        } else if (defValue instanceof Integer) {
            return (T) Integer.valueOf(sp.getInt(keyword, (Integer) defValue));
        } else if (defValue instanceof Boolean) {
            return (T) Boolean.valueOf(sp.getBoolean(keyword, (Boolean) defValue));
        } else if (defValue instanceof Long) {
            return (T) Long.valueOf(sp.getLong(keyword, (Long) defValue));
        } else if (defValue instanceof Float) {
            return (T) Float.valueOf(sp.getFloat(keyword, (Float) defValue));
        }
        return defValue;
    }

    public <T> void save(String keyword, T value) {
        if (value == null) {
            editor.remove(keyword).apply();
        } else if (value instanceof String) {
            editor.putString(keyword, (String) value).apply();
        } else if (value instanceof Integer) {
            editor.putInt(keyword, (Integer) value).apply();
        } else if (value instanceof Boolean) {
            editor.putBoolean(keyword, (Boolean) value).apply();
        } else if (value instanceof Long) {
            editor.putLong(keyword, (Long) value).apply();
        } else if (value instanceof Float) {
            editor.putFloat(keyword, (Float) value).apply();
        }
    }

    public void remove(String key) {
        editor.remove(key).apply();
    }

    public boolean contains(String key) {
        return sp.contains(key);
    }

    public Completable clearAsync() {
        return Completable.fromAction(this::clear);
    }

    public void clear() {
        editor.clear().apply();
    }
}
