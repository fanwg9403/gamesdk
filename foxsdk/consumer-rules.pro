# WishFox SDK - consumer rules bundled into the AAR as proguard.txt.
# These rules run in the HOST build, including R8 full mode.
# Do not add global -dontobfuscate/-dontoptimize/-ignorewarnings here.
# Dependency upgrades require a new audit; keep rules cannot repair missing
# dependencies, resources, native libraries or an incompatible dependency ABI.

# 1. Runtime metadata: JS annotations, Retrofit parameters and generic models.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault,Exceptions

# 2. Host entry points (including Unity/Cocos string-based Java calls).
# Preserve only these public surfaces, not all SDK implementation classes.
-keep,includedescriptorclasses class com.wishfox.foxsdk.core.WishFoxSdk {
    public *;
}
-keep,includedescriptorclasses class com.wishfox.foxsdk.core.FoxSdkConfig {
    public *;
}
-keep,includedescriptorclasses class com.wishfox.foxsdk.core.FoxSdkConfig$Builder {
    public *;
}
-keep class com.wishfox.foxsdk.core.FoxSdkConfig$WishFoxActions { public *; }
-keep,includedescriptorclasses class com.wishfox.foxsdk.core.FoxSdkOverlayManager {
    public *;
}
-keep enum com.wishfox.foxsdk.core.FoxSdkOverlayManager$Page { *; }
-keep,includedescriptorclasses class com.wishfox.foxsdk.utils.FoxSdkLongingPayUtils {
    public *;
}
-keep,includedescriptorclasses class com.wishfox.foxsdk.utils.FoxSdkLongingPayUtilsV1 {
    public *;
}
-keep public class com.wishfox.foxsdk.utils.FoxSdkLongingPayUtils$* { public *; }
-keep public class com.wishfox.foxsdk.utils.FoxSdkLongingPayUtilsV1$* { public *; }
-keep,includedescriptorclasses class com.wishfox.foxsdk.utils.FoxSdkUtils {
    public *;
}
-keep,includedescriptorclasses class com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog {
    public *;
}
-keep,includedescriptorclasses class com.wishfox.foxsdk.ui.view.dialog.FSPayDialog {
    public *;
}
-keep public interface com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog$* { *; }
-keep public interface com.wishfox.foxsdk.ui.view.dialog.FSPayDialog$* { *; }

# 3. JS calls are invisible to the Java call graph. Keep the bridge class and
# its annotated methods, including names and argument descriptors, in full mode.
# Private helpers/fields remain eligible for shrinking and obfuscation.
-keep class com.wishfox.foxsdk.ui.view.widgets.FSH5OverlayView$NavigationBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.wishfox.foxsdk.ui.view.widgets.FSWebOverlayView {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.wishfox.foxsdk.ui.view.activity.FSWebActivity {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers,includedescriptorclasses class com.wishfox.foxsdk.** {
    @android.webkit.JavascriptInterface <methods>;
}

# 4. JSON wire/persistence schema. Several existing fields have NO SerializedName
# annotation. Keep all current model fields AND constructors, including nested
# PayData and generic response/page wrappers. Keeping only annotated fields is
# insufficient; changing private field names would also break persisted JSON.
-keep class com.wishfox.foxsdk.data.model.** { *; }

# Retrofit 2.9 uses a dynamic proxy and generic return-type reflection.
# Attribute flags alone are not sufficient with R8 full mode.
-keep interface com.wishfox.foxsdk.data.network.FoxSdkApiService { *; }
-keep,allowobfuscation class io.reactivex.rxjava3.core.Single
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation class retrofit2.Response
# Gson's own artifact carries its library rules; protect the generic type-token
# boundary too, without keeping the entire Gson/OkHttp/RxJava dependency.
-keep,allowobfuscation class com.google.gson.reflect.TypeToken { *; }
-keep,allowobfuscation class com.wishfox.foxsdk.** extends com.google.gson.reflect.TypeToken

# 5. External component names / legacy Activity open(...) entry points.
# AGP also derives component keeps from the merged manifest.
-keep public class com.wishfox.foxsdk.ui.view.activity.* extends android.app.Activity {
    public <init>();
    public static <methods>;
}
-keep public class com.wishfox.foxsdk.core.WishFoxEntryActivity extends android.app.Activity {
    public <init>();
}
-keep public class com.wishfox.foxsdk.wxapi.QuickMoneyPayResultActivity extends android.app.Activity {
    public <init>();
    public <methods>;
}

# These two Views are instantiated by class name from fs_dialog_pay.xml.
-keep public class com.wishfox.foxsdk.ui.view.widgets.FSIconRadioGroupLayout {
    public <init>(android.content.Context,android.util.AttributeSet);
    public <init>(android.content.Context,android.util.AttributeSet,int);
}
-keep public class com.wishfox.foxsdk.ui.view.widgets.FSIconRadioTextView {
    public <init>(android.content.Context,android.util.AttributeSet);
    public <init>(android.content.Context,android.util.AttributeSet,int);
}

# 6. BRVAH 3.0.7 reflects on the adapter superclass's generic ViewHolder type.
# Keep both sides of that Signature and the reflectively invoked constructor.
-keep,allowobfuscation class com.chad.library.adapter.base.BaseQuickAdapter
-keep,allowobfuscation class com.wishfox.foxsdk.ui.view.adapter.* extends com.chad.library.adapter.base.BaseQuickAdapter
-keep,allowobfuscation class com.chad.library.adapter.base.viewholder.BaseViewHolder {
    public <init>(android.view.View);
}
-keep,allowobfuscation class com.wishfox.foxsdk.ui.view.adapter.** extends com.chad.library.adapter.base.viewholder.BaseViewHolder {
    <init>(...);
}

# 7. The bundled libs/kuaiqian.jar has NO consumer rules and contains prebuilt,
# partly obfuscated payment/WebView/Binder code from the following vendors.
# Preserve this closed-source boundary conservatively (not all com.tencent.*,
# all AndroidX, or unrelated host payment SDKs). Includes its JS/JNI callbacks,
# Parcelable CREATOR fields, reflected entities and vendor Activity names.
-keep class com.kuaiqian.fusedpay.** { *; }
-keep class com.unionpay.** { *; }
-keep class com.huawei.nfc.sdk.service.** { *; }

# Bugly 4.1.9.3 ships an empty proguard.txt. Its native/reflective boundary is
# vendor code; keep its namespace, not every Tencent product in the host.
-keep class com.tencent.bugly.** { *; }

# 8. No keep-all for MediaPlayer/TextureView/AndroidX/Glide or generated binding
# classes: their normal Java/Android callbacks remain reachable. The preview's
# API 33 back-dispatch reflection targets ANDROID FRAMEWORK classes, which R8
# does not rename. Do not suppress android.window warnings indiscriminately.
# Glide 4.12 ships its own GlideModule/ImageHeaderParser/InternalRewinder rules.
# Preserve dependencies' original AAR/JAR consumer metadata when distributing.
# MediaPlayer state, gesture and lifecycle helpers are not JS entry points.
# No R/BR/BuildConfig blanket keeps: code keeps do not protect Android resources.
