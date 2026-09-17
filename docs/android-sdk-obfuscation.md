# WishFox Android SDK 混淆与发布说明

更新：2026-09-17。适用于当前 Java 8 / AGP 4.1 工程及第三方宿主的 ProGuard/R8 消费流程。本次不改变 minSdk、依赖版本或宿主构建开关，不执行编译构建。

## 1. 规则分别在哪一阶段生效

| 文件 | 使用阶段 | 本次处理 |
|---|---|---|
| `foxsdk/consumer-rules.pro` | 第三方游戏开启混淆时，由 AAR 自动引入 | 补齐 SDK 运行时入口、反射与第三方闭源依赖保护 |
| `foxsdk/proguard-rules.pro` | SDK 自身开启 minify 时 | 引入同一份 consumer 规则，额外保留发行所需 public/protected API |
| `foxsdk/build.gradle` | AAR 打包配置 | 在 defaultConfig 统一声明 consumerProguardFiles，去除 release 重复声明 |

SDK 自身目前仍为 `minifyEnabled false`。这不意味着宿主也不能混淆：宿主构建会把 SDK 字节码一起交给 R8，届时使用的是 AAR 携带的 consumer 规则。反过来，若未来 SDK 自身预先混淆删除了入口，宿主的 keep 规则无法恢复它。[Android 官方的两阶段说明](https://developer.android.com/topic/performance/app-optimization/library-optimization)。

已清理旧 SDK 构建规则中的全局禁用混淆/优化、全局日志删除、大范围缺失类警告屏蔽，以及无实际方法声明的 `void (*Callback)` / `void (*Listener)` 条目。未向消费方输出上述配置，也不强制宿主关闭 R8 full mode。

## 2. 按当前代码覆盖的边界

| 边界 | 保护方式与原因 |
|---|---|
| 初始化与 H5 配置 | 保留 WishFoxSdk、FoxSdkConfig/Builder/WishFoxActions 的公开入口、参数和返回类型名称 |
| Overlay 与游戏返回键 | 保留 FoxSdkOverlayManager 公开入口及 Page 枚举，供普通 Java 或 Unity/Cocos 字符串调用 |
| 现有登录/支付接口 | 保留 FoxSdkLongingPayUtils、V1、嵌套回调/枚举、FoxSdkUtils、登录/支付 Dialog 公开接口 |
| H5 Bridge | 保留 NavigationBridge 的类及全部 JavascriptInterface 方法，包含 postMessage/openInternal/closeSecondary/openExternal/closeOverlay；兼容旧 AndroidFunction Bridge |
| JSON 请求、返回、缓存 | 对 data.model 下现有模型及嵌套模型保留字段、构造器和方法；很多字段没有 SerializedName，不能只保留已注解字段，否则后端字段或缓存 JSON 会失配 |
| Retrofit 2.9 | 保留 FoxSdkApiService、方法/参数注解、Signature，以及 Single/Call/Response 泛型类型边界，覆盖 full mode 下的代理与返回类型识别 |
| Gson | 保留模型和 TypeToken 泛型边界；不整包保留所有 Gson/OkHttp/RxJava 代码 |
| Manifest/外部回调 | 保留旧 Activity 类名、构造器与静态打开入口，以及 WishFoxEntryActivity、QuickMoneyPayResultActivity |
| XML 自定义 View | 保留支付布局中 FSIconRadioGroupLayout / FSIconRadioTextView 的名称与 XML 构造器 |
| BRVAH 3.0.7 | 保留 SDK adapter 的泛型继承关系、BaseQuickAdapter 类型和反射创建的 BaseViewHolder 构造器 |
| 快钱本地 JAR | 已核对归档：内含快钱、银联、华为 NFC Binder 代码，未附消费方规则；对这些闭源包保守保留，防止内部 WebView/JNI/Parcelable/反射入口受损 |
| Bugly 4.1.9.3 | 本地 AAR 的 proguard.txt 为空；对其闭源包保守保留，不扩展到整个 com.tencent 包 |

R8 full mode 下，仅声明保留 Signature 不足以覆盖反射类型；本方案同时保留相关类/成员。模型字段与泛型规则依据 [R8 官方兼容性说明](https://r8.googlesource.com/r8/+/refs/tags/agp-8.9.3/compatibility-faq.md)，并结合当前 Retrofit 接口实际返回 `Single<FoxSdkBaseResponse<...>>` 的代码设置。

消费方未使用全 SDK 整包 keep。业务实现、Repository、媒体状态机等仍可按正常 Java 可达性进行优化。模型是反射数据边界；几个闭源支付/Bugly 包因内部实现不可完整审计而采用较保守的保留，可能增加这部分保留代码量。后续若获得厂商当前版本精确规则并通过 release 回归，可继续缩小这些例外范围。

## 3. 新媒体预览不需要额外整包保护

- 视频通过 MediaPlayer/TextureView 的 Android 标准回调执行；图片通过 Glide/CustomTarget、手势 View 执行，直接 Java 引用及框架方法覆盖可被 R8 追踪。
- 视频/图片从 H5 发起的入口为 NavigationBridge.postMessage，必须保留的是该 JS 入口和注解，`media.previewVideo` 等方法名是 JSON 字符串，不是对应的 Java 方法名。
- API 33 返回手势通过反射访问 Android 系统的 OnBackInvokedCallback / OnBackInvokedDispatcher；系统类不属于应用可混淆程序类，不添加 `-keep android.**` 或大范围 `-dontwarn android.window.**`。
- 不为播放/缓冲/暂停/关闭状态机保留所有私有字段或 lambda 名称；不改变在线播放、不缓存视频、不主动旋转的现有行为。
- 悬浮球远程图片与预览图片仍依赖 Glide。已核对本地 Glide 4.12 AAR 自带 GlideModule、ImageHeaderParser、InternalRewinder 等规则，正常依赖方式应保留这些元数据，不需要整包禁止 Glide 混淆。

## 4. 第三方接入和重新分发

### 标准 Gradle / Maven / AAR 接入

第三方保留常规应用混淆设置即可，不需要手工复制 SDK consumer 规则：

```groovy
android {
    buildTypes {
        release {
            minifyEnabled true
            // 是否启用资源压缩由宿主决定。
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),
                    'proguard-rules.pro'
        }
    }
}
```

以上为现有 Groovy DSL 示例，不要求宿主切换 AGP 版本。宿主仍维护自己应用的反射/JNI/序列化规则。特别是游戏自行增加的 Unity/Cocos Java wrapper，SDK不知道它的类名，需要宿主保留 wrapper 的字符串调用入口；不能用 SDK 的规则代替宿主规则。

### 渠道合包、Unity 插件或手工 AAR

1. 新发行 AAR 中必须包含 `proguard.txt`，内容应包括这次 consumer 规则；之前已交付的旧 AAR 不会自动获得新规则，需要重新发行。
2. 如果仅分发裸 AAR 而非 Maven 坐标，还要提供 SDK 的传递依赖说明。普通 Android library AAR 不会自动打包全部 Maven 依赖；当前 `api fileTree` 引入的本地 JAR 与远端 Maven 依赖要区别处理。
3. 不要只拷贝 classes.jar、删除 AAR 的资源/Manifest/assets/规则，也不要在合包时丢弃依赖 JAR 的 `META-INF/proguard` 或依赖 AAR 的规则。
4. 自定义渠道流程确实不读取 AAR consumer 规则时，将本文件对应的 `consumer-rules.pro` 单独加入宿主 `proguardFiles`；依赖库自己的规则也必须保留，不能只补 WishFox 的文件。
5. 配套 `wishfox-media-bridge.js` 是交付给 H5 工程打包的脚本，不会被 Java 混淆重命名；H5 工程若单独压缩脚本，仍需遵守全局桥接对象及协议方法名契约。

## 5. 代码保护不能替代资源与依赖校验

保留 R 类不等于保留资源。当前 SDK 自有代码未发现 `Resources.getIdentifier()` 动态拼接资源名；XML、自定义 View 和 ViewBinding 使用正常资源引用，不额外添加 `tools:keep="@*/*"`。如果未来增加动态资源查找，应针对实际资源名增加资源 keep 文件，不能用整包保留 R 类替代。宿主启用 shrinkResources 时仍需覆盖原生登录、支付、悬浮球及图片预览 UI。

规则不会引入微信 OpenSDK、支付宝 SDK或额外权限。scheme 拉起仍是既有实现；不能因宿主存在其他支付 SDK 就给整个 com.tencent / com.alipay 包追加规则。本项目已经打包的银联/华为相关字节码按实际本地 JAR 保护，若宿主也使用同名厂商包，应核对重复类与版本兼容。

出现 Missing class、Duplicate class、NoSuchMethodError、缺失 so 或资源时先处理依赖/合包问题；不通过 `-ignorewarnings` 或大范围 `-dontwarn` 掩盖。宿主强制替换依赖版本时也需要重新验证，混淆规则无法保证不兼容 ABI 正常运行。

## 6. 发布前验收（由项目方编译运行）

- 检查实际 AAR 的 proguard.txt、新 Bridge 类和资源是否存在；不是只检查源码文件。
- 选择常规混淆宿主与启用 R8 full mode 的宿主，分别验证 release 包；SDK 当前老 AGP 能打包，不代表所有宿主 AGP/R8 组合已经通过。
- 检查宿主最终合并配置（通常为 `app/build/outputs/mapping/release/configuration.txt`），确认包含 SDK consumer 规则；路径依 AGP/variant 而异。
- 使用真实字符串入口验证 Unity/Cocos 初始化、配置、登录/支付回调与返回键；所有新增宿主 wrapper 单独验收。
- 登录/用户信息/悬浮球远程配置、分页列表、下单/查单：确认 JSON 字段没有丢失，泛型没有退化为原始 Map，未出现“return type must be parameterized”。
- WishFoxNative.postMessage 与旧 AndroidFunction：确认名字和注解保留，H5导航与媒体 capability 正常。
- 视频在线准备/缓冲/播放/暂停/重试/退出，图片缩放/关闭，宿主 pause/stop/旋转/重建，API 21～35+系统返回分别回归。
- 验证快钱/银联可用支付路径、外部回跳及 Bugly 上报；根据实际支持产品和设备验收，不把未开启的厂商通道宣称已验证。
- 开启资源压缩后检查所有原生弹窗与旧页面入口；100次媒体预览开关、后台释放与视频不落盘要求保持不变。
- 保存该发行版本宿主 mapping.txt；需要时由宿主设置行号保留并向自己的崩溃平台上传，不由 SDK consumer 规则强制接管全应用日志或映射策略。

本轮只进行规则与源码/依赖归档的静态核对、引用和差异检查；未运行 Gradle、javac、R8/ProGuard 压缩或设备测试。尚不能标注“所有第三方混淆组合已验证通过”。
