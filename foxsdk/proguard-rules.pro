# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ============================ 基本配置 ============================
# 代码混淆压缩比，在0~7之间，默认为5，一般不做修改
-optimizationpasses 5

# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses

# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步可以加快混淆速度。
-dontpreverify

-dontobfuscate
-dontoptimize

# 有了verbose这句话，混淆后就会生成映射文件
-verbose

# 指定混淆时采用的算法，后面的参数是一个过滤器
-optimizations !code/simplification/cast,!field/*,!class/merging/*

# 保留注解不混淆
-keepattributes *Annotation*

# 保留泛型
-keepattributes Signature,InnerClasses,EnclosingMethod

# 保留源文件及行号，用于异常时定位代码
-keepattributes SourceFile,LineNumberTable

# ============================ 解决 D8 编译错误 ============================
# 保留 error_prone_annotations 相关类
-keep class com.google.errorprone.annotations.** { *; }
-dontwarn com.google.errorprone.annotations.**

# 抑制 D8 相关警告
-dontwarn sun.misc.**
-dontwarn java.lang.**
-dontwarn javax.annotation.**

# ============================ AndroidX 和 Jetpack ============================
# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# ViewBinding
-keep class * extends androidx.viewbinding.ViewBinding { *; }
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static ** inflate(android.view.LayoutInflater);
    public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static ** bind(android.view.View);
}

# ============================ Retrofit + OkHttp ============================
# Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit 接口
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ============================ Gson ============================
# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# 保留Gson反序列化的类
-keep class com.wishfox.foxsdk.data.model.** { *; }
-keep class com.wishfox.foxsdk.data.entity.** { *; }

# 保留带有@SerializedName注解的字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================ 项目自定义类 ============================

# ========== 数据层 (Data Layer) ==========
# 网络响应模型
-keep class com.wishfox.foxsdk.data.model.FoxSdkBaseResponse { *; }
-keep class com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult { *; }

# 分页模型
-keep class com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest { *; }
-keep class com.wishfox.foxsdk.data.model.paging.FoxSdkPageResponse { *; }

# 实体类
-keep class com.wishfox.foxsdk.data.model.entity.** { *; }

# Repository
-keep class com.wishfox.foxsdk.data.repository.** { *; }

# 网络相关
-keep class com.wishfox.foxsdk.data.network.** { *; }

# API服务接口
-keep interface com.wishfox.foxsdk.data.network.FoxSdkApiService { *; }

# ========== 领域层 (Domain Layer) ==========
# UseCase
-keep class com.wishfox.foxsdk.domain.usecase.** { *; }

# Intent
-keep class com.wishfox.foxsdk.domain.intent.** { *; }

# ========== UI层 (UI Layer) ==========
# Base类
-keep class com.wishfox.foxsdk.ui.base.** { *; }

# ViewModel
-keep class com.wishfox.foxsdk.ui.viewmodel.** { *; }
-keep class * extends com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel { *; }

# ViewState
-keep class com.wishfox.foxsdk.ui.viewstate.** { *; }

# Activity和Fragment
-keep class com.wishfox.foxsdk.ui.view.** { *; }
-keep class * extends com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity { *; }
-keep class * extends com.wishfox.foxsdk.ui.base.FoxSdkBaseMviFragment { *; }

# ========== 依赖注入层 (DI Layer) ==========
-keep class com.wishfox.foxsdk.di.** { *; }

# ========== 工具类 (Util) ==========
-keep class com.wishfox.foxsdk.utils.** { *; }

# LoadingDialog
-keep class com.wishfox.foxsdk.ui.view.widgets.FSLoadingDialog { *; }

# Utils
-keep class com.wishfox.foxsdk.utils.FoxSdkSPUtils { *; }
-keep class com.wishfox.foxsdk.utils.FoxSdkSPUtils$* { *; }
-keep class com.wishfox.foxsdk.utils.FoxSdkUtils { *; }
-keep class com.wishfox.foxsdk.utils.FoxSdkUtils$* { *; }

# ========== 核心配置 (Core) ==========
-keep class com.wishfox.foxsdk.core.** { *; }

# ============================ MVI 架构特定配置 ============================
# 保留密封类
-keep class * implements com.wishfox.foxsdk.ui.viewstate.FoxSdkViewState { *; }
-keep class * implements com.wishfox.foxsdk.domain.intent.FoxSdkViewIntent { *; }
-keep class * extends com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect { *; }

# ============================ 反射相关 ============================
# 保留通过反射调用的方法
-keepclassmembers class * {
    public <init>(...);
}

# 保留R文件
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# 保留Parcelable序列化类
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留本地方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留自定义View的构造方法
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留Activity中参数为View的方法（用于XML中的onClick）
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# ============================ 第三方库警告抑制 ============================
# 抑制已知的第三方库警告
-dontwarn org.intellij.lang.annotations.**
-dontwarn org.jetbrains.annotations.**
-dontwarn sun.misc.**
-dontwarn java.lang.**
-dontwarn com.google.**
-dontwarn javax.annotation.**


# ============================ SDK 特定配置 ============================

# 保留所有公共API
-keep class com.wishfox.foxsdk.** {
    public *;
    protected *;
}

# 保留SDK初始化方法
-keepclassmembers class com.wishfox.foxsdk.core.WishFoxSdk {
    public static *** initialize(...);
    public static *** requireInitialized();
}

# 保留SDK配置类
-keep class com.wishfox.foxsdk.core.FoxSdkConfig { *; }
-keepclassmembers class com.wishfox.foxsdk.core.FoxSdkConfig {
    public <init>(...);
}

# 保留网络请求执行器
-keep class com.wishfox.foxsdk.data.network.FoxSdkNetworkExecutor { *; }
-keepclassmembers class com.wishfox.foxsdk.data.network.FoxSdkNetworkExecutor {
    public static <methods>;
}

# 保留Retrofit管理器
-keep class com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager { *; }
-keepclassmembers class com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager {
    public static <methods>;
}

# 保留拦截器
-keep class com.wishfox.foxsdk.data.network.interceptor.** { *; }
-keepclassmembers class com.wishfox.foxsdk.data.network.interceptor.** {
    public <methods>;
}

# 保留Repository容器
-keep class com.wishfox.foxsdk.di.FoxSdkRepositoryContainer { *; }
-keepclassmembers class com.wishfox.foxsdk.di.FoxSdkRepositoryContainer {
    public static <methods>;
}

# 保留ViewModel工厂
-keep class com.wishfox.foxsdk.di.FoxSdkViewModelFactory { *; }
-keepclassmembers class com.wishfox.foxsdk.di.FoxSdkViewModelFactory {
    public <methods>;
}

# ============================ 序列化/反序列化配置 ============================
# 保留所有数据类的无参构造方法
-keepclassmembers class com.wishfox.foxsdk.data.model.** {
    public <init>();
}

# 保留数据类的所有字段（Gson需要）
-keepclassmembers class com.wishfox.foxsdk.data.model.** {
    *;
}

# ============================ 回调接口配置 ============================
# 保留回调接口
-keep interface com.wishfox.foxsdk.**Callback { *; }
-keep interface com.wishfox.foxsdk.**Listener { *; }

# 保留带有回调方法的类
-keepclassmembers class * {
    void (*Callback);
    void (*Listener);
}


#微信支付
-keep class com.tencent.mm.opensdk.** {
    *;
}

-keep class com.tencent.wxop.** {
    *;
}

-keep class com.tencent.mm.sdk.** {
    *;
}
#支付宝支付
-keep class com.alipay.android.app.IAlixPay{*;}
-keep class com.alipay.android.app.IAlixPay$Stub{*;}
-keep class com.alipay.android.app.IRemoteServiceCallback{*;}
-keep class com.alipay.android.app.IRemoteServiceCallback$Stub{*;}
-keep class com.alipay.sdk.app.PayTask{ public *;}
-keep class com.alipay.sdk.app.AuthTask{ public *;}
-keep class com.alipay.sdk.app.H5PayCallback {
    <fields>;
    <methods>;
}
-keep class com.alipay.android.phone.mrpc.core.** { *; }
-keep class com.alipay.apmobilesecuritysdk.** { *; }
-keep class com.alipay.mobile.framework.service.annotation.** { *; }
-keep class com.alipay.mobilesecuritysdk.face.** { *; }
-keep class com.alipay.tscenter.biz.rpc.** { *; }
-keep class org.json.alipay.** { *; }
-keep class com.alipay.tscenter.** { *; }
-keep class com.ta.utdid2.** { *;}
-keep class com.ut.device.** { *;}

# ============================ 其他必要保留 ============================
# 保留资源绑定类
-keep class **.databinding.** { *; }
-keep class **.DataBinderMapperImpl { *; }
-keep class **.DataBinderMapperImpl$InnerBrLookup { *; }

# 保留 BR 类
-keep class **.BR { *; }

# 保留 BuildConfig
-keep class **.BuildConfig { *; }

# 保留 lambda 表达式
-keepclassmembers class * {
    private static synthetic *** lambda$*(...);
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}