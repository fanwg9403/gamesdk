# WishFox Android SDK H5 化技术实现方案

> 文档版本：1.4（2026-09-18：登录协议 Overlay、短时 Token 刷新、Builder 中文接入说明）
> 适用 SDK：WishFox Android SDK 1.4.0 及后续版本  
> 目标系统：Android API 21～35+  
> 配套协议：[WishFox JS Bridge 交互协议](./js-bridge-api.md)

## 1. 文档目的

本文档用于指导 WishFox Android SDK 将除以下能力之外的所有业务页面迁移为 H5。截图仅作为旧版页面层级和窗口关系的参考，具体颜色、字体、图片和间距由新版 H5 决定：

- 悬浮球；
- 悬浮球远程图片下载、缓存和兜底；
- 登录弹窗；
- 支付弹窗；
- 支付执行、支付回调和支付结果轮询；
- Toast；
- 商单视频原生全屏播放、封面图原生全屏预览（H5列表仍由 H5承载）；
- 微信小程序 scheme 跳转；
- H5 图片下载、SDK 沙盒保存和系统相册写入。

方案必须满足：

1. SDK 页面继续以 Overlay View 挂载到宿主 Activity，不因打开 SDK 页面启动新的业务 Activity；
2. 打开首页、列表、详情等 SDK 页面时，不触发 Unity、Cocos 或普通游戏宿主 Activity 的 `onPause()`；
3. 同时支持横屏、竖屏、运行时旋转、窗口尺寸变化和宿主 Activity 重建；
4. 支持一个主 WebView，以及按内部链接规则创建的副 WebView；横屏副 WebView位于右侧，竖屏副 WebView以等宽窗口覆盖在首页区域上；
5. 不申请存储、相机、麦克风、定位、媒体读取等权限；
6. 不接入微信 OpenSDK，小程序只通过服务端或 H5 提供的受控微信 scheme 跳转；
7. 最低兼容 API 21，覆盖 API 21～35+；
8. H5 可以独立发布，普通业务变化不需要第三方游戏重新集成 AAR；
9. JS Bridge 必须版本化、可降级、可审计，并限制可信来源；首页内点击只允许进入 Bridge、内部 H5 链接或外部浏览器三条路径；
10. SDK 关闭后不得残留 WebView、Activity 或异步任务引用。

## 2. 非目标

本次改造不负责：

- 改造宿主游戏自身生命周期；
- 阻止微信、支付宝、系统文件选择器等外部 Activity 导致的宿主 `onPause()`；
- 为 API 21～28 绕过 Android 存储权限模型静默写入公共相册；
- 为任意第三方网页提供通用浏览器能力；
- 向 H5 暴露 Android Context、Activity、WebView 或任意 Java 方法；
- 代理所有 H5 业务接口。普通列表、详情、活动和内容接口应由 H5 直接调用服务端。

## 3. 当前工程基础与改造边界

灰度接入时通过配置启用 H5 首页：

```java
FoxSdkConfig config = new FoxSdkConfig.Builder(appId, channelId, payScheme)
        .setH5HomeUrl("https://sdk.example.com/home")
        .setH5TrustedOrigin("https://sdk.example.com")
        .setAllowInsecureH5(false)
        .setH5MediaOrigins("https://media.example.com", "https://images.example.com")
        .build();
```

`h5HomeUrl` 为空时保留现有原生首页，便于分渠道灰度；启用后悬浮球点击按登录态进入原生登录弹窗或 H5 首页。

当前工程已经具备以下可复用基础：

- `FoxSdkOverlayManager`：按宿主 Activity 管理 Overlay，并将 View 挂载到 `DecorView`；
- `WindowLifecycleControl`：监听宿主 Activity 生命周期，并管理悬浮球窗口；
- `FSSemiStealthWindow`：悬浮球拖拽、贴边、半隐藏、沉浸式适配和位置恢复；
- `FSFloatImageManager`：远程图片下载、本地缓存、原子替换和默认图兜底；
- `FSOverlayInsets`：处理状态栏、导航栏和挖孔屏安全区；
- `FSWebOverlayView`：已有基础 WebView 页面，但安全、生命周期和 Bridge 能力不足；
- `FSLoginDialog`、`FSPayDialog`、`FoxSdkLongingPayUtils`：现有登录和支付能力。
- `FoxSdkBootstrapCoordinator`：新增的初始化、登录态检查、悬浮球资源准备和点击分流协调器。

本次不建议直接把现有 `FSWebOverlayView` 扩展成新的业务容器。应新增独立的 H5 容器，避免旧的通用网页详情页语义与新的 SDK H5 Runtime 混杂。

## 4. 总体架构

```text
宿主 Activity / UnityPlayerActivity / Cocos Activity
        │
        ├── Application.ActivityLifecycleCallbacks
        │       └── WindowLifecycleControl
        │              ├── Activity 创建/恢复/暂停/销毁
        │              ├── 旋转与配置变化
        │              └── 悬浮球显示隐藏
        │
        ├── FSSemiStealthWindow
        │       ├── 本地默认悬浮球图片
        │       ├── 远程悬浮球图片缓存
        │       ├── 位置保存与恢复
        │       └── 点击后打开 H5 Overlay
        │
        └── FoxSdkOverlayManager
                └── FoxSdkH5OverlayView
                        ├── Primary WebView
                        ├── Secondary WebView（按需）
                        ├── FoxSdkWebViewFactory
                        ├── FoxSdkJsBridge
                        ├── FoxSdkAuthCoordinator
                        ├── FoxSdkPaymentCoordinator
                        ├── FoxSdkSchemeJumpCoordinator
                        ├── FoxSdkMediaSaveCoordinator
                        ├── FoxSdkH5SessionStore
                        └── FoxSdkNavigationCoordinator
```

### 4.1 原生与 H5 的职责边界

| 能力 | 原生 SDK | H5 |
|---|---|---|
| 悬浮球 | 负责 | 不负责 |
| 初始化与点击分流 | 查询本地登录态、准备悬浮球图片、决定打开登录弹窗或首页 | 不绕过登录态直接渲染首页 |
| 页面 UI | 仅提供容器、错误页和 Loading | 负责全部业务页面 |
| 业务导航 | 提供主副 WebView 布局能力 | 负责路由和页面状态 |
| 登录 | 展示登录弹窗并保存原生登录态 | 触发登录、接收用户概要 |
| 支付 | 展示支付弹窗、执行支付、轮询结果 | 创建支付意图、展示业务结果 |
| Toast | 展示 | 触发 |
| 微信小程序 | 校验并执行受控 scheme | 请求服务端 scheme、触发跳转 |
| 图片下载 | 校验并下载到沙盒 | 提供 URL、文件名和保存策略 |
| 系统相册 | API 29+ 使用 MediaStore | 指定是否需要写入相册 |
| 普通业务接口 | 不代理 | 直接访问服务端 |
| 生命周期 | 监听宿主并通知 H5 | 保存和恢复页面业务状态 |

## 5. 建议新增的核心类

### 5.1 `FoxSdkH5OverlayView`

职责：

- 继承 `FSOverlayPageView` 或新的轻量 Overlay 基类；
- 持有 Overlay 根布局；
- 持有主 WebView 容器和副 WebView 容器；
- 处理 single、split、stacked 三种布局；
- 消费外部区域点击；
- 处理返回键；
- 应用安全区；
- 将宿主生命周期转发给两个 WebView；
- 负责 Loading、错误页、重试和渲染进程恢复；
- 页面销毁时完整释放 WebView 和 Bridge。

建议字段：

```java
final class FoxSdkH5OverlayView extends FrameLayout {
    Activity hostActivity;
    String overlayInstanceId;
    H5LayoutMode layoutMode;
    FoxSdkWebViewSlot primarySlot;
    FoxSdkWebViewSlot secondarySlot;
    FoxSdkJsBridge bridge;
    boolean hostResumed;
    boolean overlayVisible;
    boolean destroyed;
}
```

### 5.2 `FoxSdkWebViewSlot`

每个 WebView 一个 Slot，用于隔离状态：

```java
final class FoxSdkWebViewSlot {
    String webViewId;        // primary / secondary
    WebView webView;
    String currentRoute;
    String currentUrl;
    boolean bridgeReady;
    boolean pageReady;
    boolean canGoBack;
    long generation;
}
```

任何回调执行前必须检查 Slot 的 `generation`，防止旋转或页面重建后旧回调污染新实例。

### 5.3 `FoxSdkWebViewFactory`

负责：

- 创建 WebView；
- 统一配置 WebSettings；
- 设置 WebViewClient、WebChromeClient 和 DownloadListener；
- 注册 Bridge；
- 配置可信域名；
- 配置 Cookie；
- 注册渲染进程异常处理；
- 为 API 21～35+ 做能力分支。

禁止由各页面自行创建和配置 WebView。

### 5.4 `FoxSdkJsBridge`

负责：

- Web Message 和兼容 JS Interface 的传输适配；
- JSON envelope 解析；
- Origin、协议版本、requestId、方法白名单和参数校验；
- 主线程切换；
- 请求超时；
- 重复请求去重；
- 响应和事件派发；
- Overlay销毁后的回调拦截。

Bridge 对外协议见 `js-bridge-api.md`。

### 5.5 `FoxSdkH5SessionStore`

用于保存跨旋转、Activity 重建和外部应用跳转的轻量状态。

建议只保存：

```java
final class FoxSdkH5SessionSnapshot {
    String sessionId;
    String hostActivityClass;
    int hostTaskId;
    String primaryRoute;
    String secondaryRoute;
    H5LayoutMode layoutMode;
    boolean secondaryVisible;
    boolean overlayVisible;
    String pendingPaymentIntentId;
    String pendingSchemeRequestId;
    String pendingMediaRequestId;
    long updatedAt;
}
```

不得保存：

- WebView 实例；
- Activity、View 或 Context；
- JS Callback 对象；
- token、Cookie；
- 大体积 Base64；
- 页面完整 HTML；
- 无上限的 WebView history Bundle。

## 6. Overlay 状态机

```text
HIDDEN
  │ 点击悬浮球
  ├── 未登录 → LOGIN_DIALOG
  │              │ 登录成功且 openHomeAfterLogin
  │              └──────────────► CREATING
  └── 已登录 ───────────────────► CREATING
  │ WebView 创建完成
  ▼
LOADING
  │ H5 bridge.ready
  ▼
READY
  │ 宿主跳外部应用
  ▼
PAUSED
  │ 宿主 onResume
  └──────────────► READY

READY / LOADING
  │ 用户关闭、宿主销毁
  ▼
DESTROYING
  │ 资源释放完成
  ▼
HIDDEN
```

`LOGIN_DIALOG` 不属于 H5 Overlay 状态；它复用现有原生登录弹窗状态机，关闭或取消后回到 `HIDDEN`。

约束：

- 同一宿主 Activity 同时只能存在一个 H5 Overlay；
- Overlay 内最多两个 WebView；
- CREATING/DESTROYING 阶段拒绝新的布局、支付和媒体请求；
- 支付和媒体操作使用独立 requestId，不以页面是否刷新作为完成依据；
- Overlay销毁后，迟到响应只记录诊断，不再调用 JS。

## 6.1 初始化与悬浮球点击分流

初始化分为“SDK进程初始化”和“悬浮球点击初始化”两层，不能在 SDK 初始化阶段无条件创建首页 WebView。

```text
SDK.initialize(config)
  → 校验 appId/channel/可信 H5 origin
  → 注册 ActivityLifecycleCallbacks
  → 初始化 WishFoxSdk 网络与生命周期组件
  → 读取本地登录态（不访问网络）
  → 启动悬浮球图片缓存刷新（异步、去重、失败回退）
  → 绑定当前前台 Activity 的悬浮球

点击悬浮球
  → 若已有登录态：创建 H5 Overlay，加载 /home
  → 若无登录态：只显示原生 FSLoginDialog
       → 登录成功：保存登录态、刷新悬浮球用户相关资源
       → 根据 openHomeAfterLogin 决定立即打开 /home 或仅回调登录结果
       → 用户取消：恢复悬浮球，不创建 WebView
```

点击分流要求：

- 未登录点击悬浮球不得短暂显示首页、不得先创建 WebView再等待登录；
- 登录态判断只使用原生安全存储中的会话摘要，过期或校验失败按未登录处理；
- 登录弹窗由现有原生实现承载，弹窗关闭、旋转和 Activity销毁均由原有逻辑处理；
- 登录成功后打开可匿名加载的 H5 静态壳；JS 就绪后通过 auth.refreshSession 获取短时 Token，再请求受保护接口；不在首屏 URL 传递登录凭证；
- 已登录点击悬浮球只创建一个 Overlay；重复点击在 CREATING/LOADING阶段应被忽略；
- 悬浮球远程图片下载与首页是否打开解耦，初始化时可并行执行，点击时使用“内置图→旧缓存→新缓存”的可用资源；
- 图片下载不得阻塞登录弹窗或首页 WebView创建，失败只记录诊断并使用旧缓存/默认图。

## 6.2 首页点击行为与内部链接路由

首页及 H5业务页面的点击结果必须先归类：

| 点击类型 | H5动作 | Android动作 |
|---|---|---|
| SDK能力 | 调用 `WishFoxSDK.*` | 执行登录、Toast、支付、布局等原生能力 |
| 内部链接 | 调用 `navigation.openInternal` 或 `<a>`被拦截 | 在当前方向对应的 WebView窗口加载内部 URL |
| 外部链接 | 调用 `navigation.openExternal` 或外链被拦截 | `ACTION_VIEW`打开外部浏览器，Overlay保持可恢复状态 |

内部链接判定必须同时满足：HTTPS、可信 H5 origin、路径在业务白名单内、无用户信息/支付密文泄露。`WebViewClient.shouldOverrideUrlLoading` 和 Bridge方法使用同一判定器，避免两套规则产生差异。

H5 项目不应把全局链接配置为新标签页，也不应依赖 `target="_blank"` 或 `window.open()` 实现右侧页面。右侧窗口是 SDK 的 Native WebView 容器行为；H5只提交内部导航意图，Android负责创建、复用和销毁 Secondary。WebView统一关闭多窗口能力，避免 `onCreateWindow()`产生不可控实例。

窗口路由规则：

```text
内部链接 + 横屏
  → Secondary不存在：按 split 创建右侧 Secondary WebView并加载 URL
  → Secondary已存在：保留 Secondary实例，在右侧 WebView加载 URL

内部链接 + 竖屏
  → Secondary不存在：创建等宽 Secondary，覆盖首页区域（stacked）并加载 URL
  → Secondary已存在：保留当前覆盖窗口，在 Secondary内加载 URL
```

横屏右侧 Secondary 的宽度由 `secondaryWidthRatio` 配置（默认 0.552），Primary保留列表/首页区域；竖屏 stacked 的 Secondary 宽度为 Overlay内容区宽度，高度覆盖内容区，关闭后恢复首页滚动位置。方向改变时：

1. split 自动转换为 stacked，并保留 Secondary 当前 route；
2. stacked 转横屏时恢复为 split，复用 Secondary，不重新创建页面；
3. 只有 WebView实例确实被销毁时才重新加载 URL；普通旋转只重新布局，不触发 `loadUrl()`；
4. 路由加载失败时保留窗口，显示 H5错误页并提供重试/关闭，不回退到外部浏览器。

`FoxSdkNavigationCoordinator` 统一维护 `primaryRoute`、`secondaryRoute`、窗口模式、页面 generation 和外链 pending 状态；返回键优先关闭 Secondary或回退 Secondary history，再处理 Primary。

## 7. WebView 安全配置

### 7.1 必选设置

```java
WebSettings settings = webView.getSettings();
settings.setJavaScriptEnabled(true);
settings.setDomStorageEnabled(true);
settings.setSupportZoom(false);
settings.setBuiltInZoomControls(false);
settings.setDisplayZoomControls(false);
settings.setAllowFileAccess(false);
settings.setAllowContentAccess(false);
settings.setAllowFileAccessFromFileURLs(false);
settings.setAllowUniversalAccessFromFileURLs(false);
settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
settings.setJavaScriptCanOpenWindowsAutomatically(false);
settings.setSupportMultipleWindows(false);
settings.setMediaPlaybackRequiresUserGesture(true);
```

补充规则：

- H5 主文档必须与可信 Origin 精确匹配；生产环境建议只使用 HTTPS；
- 本地/内网联调可显式开启 `setAllowInsecureH5(true)` 使用 HTTP，但仍严格匹配协议、域名和端口；
- 不允许 `file://`；
- 不允许 H5直接加载 `content://`；
- 非白名单网页交给系统浏览器或直接拒绝；
- `weixin://` 只能交给 SchemeJumpCoordinator；
- 支付 scheme 只能交给支付模块；
- SSL 错误必须 `cancel()`；
- 生产环境关闭 WebView调试；
- 不允许任意 popup、新窗口和 JS自动打开窗口；
- 不实现通用文件上传、摄像头、麦克风或地理位置授权；
- 不在每次打开时执行 `clearCache(true)`；
- 不调用 `CookieManager.removeAllCookies()`；
- 不调用会影响整个进程其他 WebView的全局清理或定时器 API。

### 7.2 Cookie 策略

- 允许 WishFox H5自身的一方 Cookie；
- 默认关闭第三方 Cookie：`CookieManager.setAcceptThirdPartyCookies(webView, false)`；
- 当前认证使用 Bridge 返回的内存短时 Token，不再使用旧稿的 exchangeCode/HttpOnly Cookie 交换；普通非认证 Cookie 应按服务端策略设置安全属性；
- 登出时只清理 WishFox域名和指定 Cookie名称；
- 禁止把原生 token 放入 URL、LocalStorage或日志；
- 两个 WishFox WebView 可能共享系统 Cookie 存储，但认证短时 Token 各自在 H5 内存持有、各自申请；不依赖 Cookie 共享完成认证。

### 7.3 Origin 白名单

白名单必须内置在 SDK 配置或可信初始化配置中，例如：

```text
https://sdk.wishfoxs.com
https://sdk-pre.wishfoxs.com
https://static.wishfoxs.com
```

远程配置只能在内置可信 Origin 范围内选择 URL，不能把 Bridge动态开放给任意新域名。`setAllowInsecureH5(true)` 只改变是否允许 HTTP，不会取消 Origin 精确匹配；正式环境默认关闭。

检查内容包括：

- scheme 必须是 HTTPS，或在开发/内网测试显式开启明文 H5 后使用 HTTP；
- host 完整匹配，不能使用 `contains()`；
- 明确端口；
- 禁止 username/password URL；
- 主文档跳转和 iframe资源分别校验；
- 非主文档不得获得高权限 Bridge；
- 页面发生非白名单导航前移除兼容 JS Interface。

## 8. JS Bridge 传输实现

### 8.1 首选传输

引入 AndroidX WebKit，运行时检查：

```java
WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
```

支持时使用 `WebViewCompat.addWebMessageListener`，并设置精确 `allowedOriginRules`。

### 8.2 API 21 或旧 WebView Provider 降级

如果运行时不支持 Web Message：

- 仅注入一个对象名：`WishFoxNative`；
- 仅暴露一个方法：`postMessage(String json)`；
- 禁止暴露其他 public 方法；
- 只在可信主页面加载完成后注册；
- 每个页面会话使用随机 nonce；
- 非白名单跳转前移除接口；
- 服务端 CSP禁止任意 iframe；
- 高风险请求仍必须经过原生弹窗或系统 UI；
- Bridge握手结果中返回 `bridgeMode=restricted_js_interface`。

### 8.3 请求处理顺序

```text
收到 JSON
  │
  ├── 检查 Overlay/WebView 是否有效
  ├── 检查当前页面 Origin
  ├── 检查协议版本
  ├── 检查 nonce（fallback 模式）
  ├── 检查 requestId
  ├── 检查方法白名单
  ├── 校验字段、长度、枚举和类型
  ├── 检查重复请求与并发状态
  ├── 切换到主线程或 IO 执行器
  └── 返回统一响应
```

## 9. 横竖屏布局设计

### 9.1 模式定义

```java
enum H5LayoutMode {
    SINGLE,
    SPLIT,
    STACKED
}
```

### 9.2 横屏

建议沿用当前消息页视觉比例：

- Primary：约 44.8%；
- Secondary或外部区域：约 55.2%；
- Primary 从物理屏幕起始边缘开始；
- 挖孔、状态栏、导航栏由 `FSOverlayInsets` 动态处理；
- Single模式右侧为空白/半透明外部区域；
- Split模式右侧容器创建 Secondary WebView；
- 点击外部区域关闭 Overlay；
- 点击 Secondary 内部不冒泡关闭 Overlay。

### 9.3 竖屏

- Primary占主要区域，可保留约 4:1 的页面/外部区域比例；
- Split请求自动降级为 STACKED；
- Secondary详情使用主区域覆盖或 H5内部路由；
- 原生响应必须返回 `actualMode`，H5不能假设请求一定成功。

### 9.4 方向判断

以真实环境为准：

```java
activity.getResources().getConfiguration().orientation
```

`FoxSdkConfig.screenOrientation` 只能作为集成期预期信息，不能覆盖当前真实方向，更不能调用 `setRequestedOrientation()` 改变游戏方向。

## 10. 旋转、配置变化和 Activity 重建

### 10.1 Activity未重建

当宿主自行处理方向变化时：

1. `WindowLifecycleControl.onConfigurationChanged()`；
2. 找到当前 Activity 对应的 OverlayManager；
3. 调用 `FoxSdkH5OverlayView.onHostConfigurationChanged()`；
4. 重新读取屏幕尺寸和 Insets；
5. 调整两个容器的 LayoutParams；
6. 调用 WebView `requestLayout()` 和 `invalidate()`；
7. 不调用 `loadUrl()`；
8. 发送 `environment.changed`；
9. 如果竖屏不支持 Split，则切换为 Stacked并发送 `layout.changed`。

### 10.2 Activity被重建

旧 Activity `onDestroy()` 时：

```java
boolean changing = activity.isChangingConfigurations();
```

如果为 true：

- 保存 `FoxSdkH5SessionSnapshot`；
- 标记 Overlay需要恢复；
- 从父 View移除 WebView；
- 移除 Bridge；
- stopLoading、clearHistory按需、destroy；
- 清除旧 Activity引用；
- 不清除登录、支付和业务 session。

新 Activity 创建时：

- 比较宿主类名和 taskId；
- 恢复 Overlay；
- 创建新 WebView；
- 加载 H5入口；
- Bridge ready后返回 snapshot；
- H5恢复当前路由和业务草稿；
- 原生恢复 pending payment/scheme/media操作。

### 10.3 H5 状态责任

H5需要持续通过 `navigation.updateState` 或页面状态事件告诉原生：

- 当前 route；
- 是否可返回；
- 当前标题；
- Secondary route；
- 是否存在未提交表单。

复杂草稿、筛选项、滚动位置等由 H5存储到 IndexedDB/localStorage或服务端草稿。原生只保存恢复入口，不保存页面业务模型。

### 10.4 防止重复数据加载

- Activity未重建：不重新 `loadUrl()`，因此 H5数据不重载；
- Activity重建：H5重新加载壳页面，但根据 sessionId、route和本地缓存恢复；
- H5接口应对相同分页、详情请求做请求去重；
- 支付创建和写相册请求必须依赖 idempotencyKey；
- Bridge响应携带原请求 ID；
- 旧 generation 响应不得派发给新页面。

## 11. 生命周期转发

### 11.1 显示 Overlay

```text
点击悬浮球
  → 查询原生登录态
  → 未登录：显示 FSLoginDialog，流程结束（不创建 WebView）
  → 已登录：保存悬浮球位置
       → 隐藏并 recycle 悬浮球窗口
       → 创建 Overlay
       → 创建 Primary WebView
       → WebView.onResume()
       → loadUrl(H5入口 /home)
       → bridge.ready
       → lifecycle.changed(overlay_visible)
```

登录弹窗成功后，若配置 `openHomeAfterLogin=true`，从“登录成功”节点继续执行已登录分支；若为 false，仅发送登录结果并恢复悬浮球。

### 11.2 隐藏 Overlay

```text
H5关闭/外部区域点击/返回键关闭
  → lifecycle.changed(overlay_hidden)
  → WebView.onPause()
  → 取消普通页面请求回调
  → 销毁 Secondary
  → 销毁 Primary
  → 移除 Overlay
  → 恢复悬浮球
```

### 11.3 宿主 onPause/onResume

宿主跳转微信、支付宝或系统文件选择器时：

- 先发送 `lifecycle.changed(host_pausing)`；
- 分别调用可见 WebView的 `onPause()`；
- 不销毁 Overlay；
- 不调用 `WebView.pauseTimers()`，因为它可能影响同一进程的其他 WebView；
- 保存 pending operation；
- 宿主恢复时调用 WebView `onResume()`；
- 发送 `lifecycle.changed(host_resumed)`；
- PaymentCoordinator、SchemeJumpCoordinator、MediaSaveCoordinator分别恢复自身状态。

### 11.4 Activity销毁

- 非配置变化：完整销毁 Overlay和操作回调；
- 配置变化：保存 snapshot后销毁旧 View，但保留会话恢复标记；
- 不允许静态字段持有 Activity；
- 异步任务持有 Application Context或弱 Activity引用；
- Dialog在 Activity销毁前 dismiss。

## 12. 返回键

处理优先级：

1. 有系统登录/支付弹窗时由弹窗处理；
2. Secondary H5声明可返回时，向 Secondary发送 `navigation.backRequested`；
3. Secondary未处理时关闭 Secondary；
4. Primary声明可返回时，向 Primary发送事件；
5. H5未在约定超时内响应时，检查 `webView.canGoBack()`；
6. 仍不可返回时关闭整个 Overlay并恢复悬浮球。

兼容策略：

- API 33+：注册 `OnBackInvokedDispatcher`；
- AndroidX ComponentActivity：使用 `OnBackPressedDispatcher`；
- 普通 Activity/Unity/Cocos：在主副 WebView和 Overlay根 View上处理 KeyEvent；
- 如果某类宿主完全截获返回键，提供可选的 `FoxSdkOverlayManager.onHostBackPressed(Activity)` 集成入口。

## 13. 登录、协议页与短时会话（当前代码）

### 13.1 原生登录及 H5 初始化

```text
浮球点击 → 原生长期 Token 存在？
  否 → FSLoginDialog → 登录成功 → FSLoginResult.save
  是 / 登录成功 → 创建 Primary、加载匿名静态壳
H5 bridge.ready / auth.getState
  → auth.refreshSession
  → FSH5AuthSession → FoxSdkApiService.getShortLogin()
  → 后端校验长期 Token、签发短时 Token
  → H5 内存保存 sessionToken（expiresIn 若服务端提供则作为可选提示）；appId/channelId 使用 bridge.ready 返回的固定配置
  → 使用短时 Token 请求业务接口
短时 Token 临近过期/服务端明确拒绝
  → 合并一次 refreshSession → 更新内存 → 安全请求至多重试一次
长期 Token 失效
  → AUTH_REQUIRED → 清理 H5 凭证 → 用户触发 auth.login
```

已登录调用 auth.login 默认只交换，不重复展示登录框。exchangeH5Session=false 可仅登录原生，但不能据此请求受保护 H5 API。login/refresh 默认调用 SDK 内置短时 Token 接口，不将长期 Token 当作短时 Token。

FoxSdkOverlayManager 管理一个 LoginAttempt，防止重复提交及不同入口重复打开登录框。登录接口失败留在原弹窗重试；成功、用户关闭、宿主不可用才终结请求。保存登录前校验开始时的登录世代，页面销毁取消自身操作；不使用静态 dismissInstance 误关其他调用方弹窗。

### 13.2 协议页层级与返回

根因：原生 Dialog 是独立 Window，往 Activity DecorView 加普通业务 Overlay 不能越过仍显示的 Dialog。单纯 bringToFront/提高业务 View 的 Z 无法解决跨 Window 层级。

实现：
- FSLoginDialog 持有 FSLoginAgreementView。点击协议后创建只读专用 Overlay，临时清除登录 Window 的 DIM_BEHIND 并 hide 原 Dialog，再将协议页挂在宿主 DecorView 最高的 SDK 页面层。
- 不 dismiss 原 Dialog、不覆盖原 OnDismissListener/登录监听器、不替换原首页/Secondary。手机、验证码、密码、勾选状态、验证码倒计时及回调原样保留。
- 原生固定标题栏只保留一个返回图标，复用项目现有切图 `fs_right_back`（20dp 图标、48dp 点击区域），不使用系统文字按钮，不提供额外关闭按钮。返回优先 WebView 历史，无历史再恢复原 Dialog 及其蒙层。H5 不需要添加返回 JS。
- 协议页禁用 JavaScript、不注入 WishFoxNative，只允许入口 HTTPS 同源导航；关闭 file/content 和 mixed content，SSL 错误拒绝加载。协议 HTML 应为服务端可直接阅读的静态内容。
- 不新增 Activity、不调整宿主方向。横竖屏都使用宿主窗口安全区内的全尺寸协议页；仅尺寸变化重排布局，不重新创建 WebView。
- 通过 ActivityLifecycleCallbacks 转发协议 WebView onPause/onResume；不调用影响所有 WebView 的 pauseTimers。关闭/宿主销毁注销生命周期和返回回调，销毁自身 WebView，不清全局缓存/Cookie。
- AndroidX/Android 33+ 返回处理沿用媒体 Overlay 兼容思路；游戏抢占返回键时宿主仍需调用 FoxSdkOverlayManager.onHostBackPressed。原生返回图标始终存在。
- Activity 真正重建时关闭旧协议及登录窗口，不把旧 Activity/View 留给新实例；不承诺跨 Activity 重建保留密码或验证码，避免旧回调绑定新宿主。

Android 官方 API 说明：[Dialog.hide 保留实例而非 dismiss](https://developer.android.com/reference/android/app/Dialog#hide())；[WebView.pauseTimers 影响所有 WebView](https://developer.android.com/reference/android/webkit/WebView#pauseTimers())，本实现不调用它。

### 13.3 原生短时 Token 接口

SDK 默认通过 `POST /api/user/token/short`（`FoxSdkApiService.getShortLogin()`）获取短时 Token。原生长期 Token 由统一请求拦截器放入 Authorization，请求成功后把非空 `short_token`（兼容 `shortToken`）回传给发起请求的 H5 文档；`expires_in`/`expire_at` 如果存在则作为可选元数据回传。固定的 `appId`/`channelId` 在 `bridge.ready` 响应中返回，不随认证交换重复传递。短 Token 的真实失效由后端业务接口判定，H5 收到失效响应后再次调用 refreshSession。保留 `setH5SessionTokenProvider(...)` 仅用于宿主明确需要覆盖默认交换实现的兼容场景。

后端最小契约：

| 项目 | 要求 |
|---|---|
| 认证输入 | 原生长期 Token 仅传受信任 HTTPS 后端；appId/channelId 使用配置，sessionId 由原生生成 |
| 绑定校验 | 校验长期 Token 用户及所属游戏/渠道，短时凭证绑定用户、应用、渠道和 H5 会话用途 |
| 输出 | 独立非空短时 Token（最多8192字符）；`expires_in`/`expire_at` 可选 |
| 多窗口 | 同一会话允许多个并行有效短时凭证；一次刷新不能立即废除另一 WebView 的凭证 |
| 错误 | AUTH_REQUIRED 只代表长期凭证无效；NETWORK_ERROR/RATE_LIMITED/SESSION_EXCHANGE_FAILED 不清登录态 |
| 安全 | 不将 Token 写 URL、日志、埋点、磁盘缓存或错误文本；限制签发频率、防重放、限定业务权限 |

SDK 在 IO 线程调用接口，15 秒超时，结果统一切主线程；页面销毁会取消 Retrofit 订阅。接口与可选 provider 的错误均统一归一化，禁止原样回传后端异常文本。原生只校验 Token 非空、不等于长期 Token且长度不超过8192；有效期缺失或超出原有建议范围不会阻止透传，避免把业务侧的失效判断错误地收敛成 INVALID_SESSION_RESPONSE。

注意：Rx 订阅取消/超时只保证 SDK 不再消费结果，不会自动取消 provider 自己创建的外部异步网络请求。provider 必须自行配置网络超时并关闭响应体，不持有 Activity；后续若需要强制传输取消，可扩展专门的取消句柄，不能声称当前已中止底层网络。

### 13.4 竞态、生命周期与凭证存储

- 每个 NavigationBridge 文档独立 FSH5AuthSession，持有操作引用、到期时间、登录世代，不存储短时 Token 本体。
- FSLoginResult.save/clear 推进进程内世代。交换完成验证世代和长期 Token 均未改变；否则返回 AUTH_STATE_CHANGED。退出后再登录即便服务器给出相同字符串，也不接受之前的结果。
- getState.status 是原生登录状态，sessionStatus 才是当前文档短时会话 none/valid/expired，二者不可混淆。
- 只保存认证完成 ID（最多128），不把含凭证的响应放入通用 responses 缓存。同一在途 ID 不重复执行；完成 ID 重发返回 DUPLICATE_REQUEST。
- 导航/销毁重置认证状态、取消订阅及该请求拥有的登录弹窗，旧 generation 不回调新文档；普通布局改变不重置会话。
- 普通交换失败/超时不清除原生登录；只有后端明确 AUTH_REQUIRED 才清除，且只清除仍属于本次请求的账号。
- auth.changed 向可信存活窗口广播各自状态（无 Token），不是完整原生账户事件总线；宿主通过其他原生入口变更登录后，H5 需在恢复、进入及鉴权失败时主动查询。
- H5 只在内存保存短时 Token，页面新建/重载后重新交换；SDK 不做后台定时刷新，H5 不做无限重试或支付请求盲目重放。
- 当前 Bridge 仍是 restricted_js_interface：addJavascriptInterface 不能证明 iframe 来源，H5 必须是审核通过的自有 HTTPS 页面，不嵌入不可信 iframe/脚本，服务端设置严格 CSP。短时 Token 不能修复页面 XSS；域名白名单不等于 iframe 身份校验。
- auth.logout 由 H5 发起，Android 原生确认后尽力调用服务端登出接口，清理用户信息、长期 Token、兼容 Authorization 存储键及所有 H5 文档内存中的短时 Token，并关闭整个 H5 Overlay、恢复悬浮球；取消确认返回 USER_CANCELLED。不得由 H5 侧只清除自己的状态模拟登出成功。

### 13.5 Builder 中文说明与发布

本轮补齐每个 Builder 参数的中文含义、默认值、单位、返回值及约束。尤其：

- timeout 为毫秒，默认30000，与会话交换15秒超时分开。
- screenOrientation 使用 ActivityInfo 常量，不要把历史 FoxSdkConfig.ORIENTATION_LANDSCAPE=2 当成 ActivityInfo 的横屏0；H5/预览跟随宿主，不主动旋转。
- setFloatXScale/setFloatXxOffset 是历史 FloatingX 字段，当前原生悬浮球流程未读取；不能将保留接口误写成已生效的配置。旧 offset 调用按 dp 转 px。
- setWechatTest 选择既有支付的小程序 trial/release，不是全局环境切换。
- H5 首页/可信 Origin/媒体 Origin 分别配置；媒体域名不取得 Bridge 权限。
- 新 provider/Callback、LoginCallback 的 consumer keep 规则随 AAR 提供，内部认证状态类仍可正常压缩混淆。

### 13.6 本轮验收清单（不编译构建）

1. 未登录/已登录，从浮球和 H5 auth.login 两种入口阅读协议；返回后表单、勾选、倒计时保留，取消/成功各一次回调。
2. 横/竖屏、有网页历史/无历史、网络失败/证书失败、返回图标/系统返回/游戏抢占返回；检查无旧蒙层遮挡和误回首页。
3. 宿主暂停恢复、真实重建、退出游戏、协议页渲染进程退出；无旧窗口恢复、Activity 泄漏或 SDK 主动重启宿主。
4. 正常交换、未配置 provider、空/长期 Token 回传、超长/非法有效期、provider 异常/多次回调/永不回调。
5. 并发刷新、重复 ID、不同窗口分别刷新、完成前导航/关闭、交换中退出并登录新账号（含同 Token 情况）。
6. 短时 Token 过期而原生有效：只刷新；长期 Token 无效：明确 AUTH_REQUIRED；网络失败不注销。
7. 源码静态检查和 JS helper 模拟测试可本地执行；按用户要求不执行 Gradle/javac/R8/设备运行。实际 Android 设备、Release 混淆与真实后端联调由接入方验证。

## 14. 支付实现

### 14.1 Bridge输入

推荐只传服务端创建的支付意图：

```json
{
  "paymentIntentId": "pi_xxx",
  "idempotencyKey": "uuid"
}
```

SDK再从服务端读取：

- 商品 ID；
- 商品名称；
- 实际金额；
- CP订单号；
- 支付方式；
- 展示说明；
- 过期时间。

禁止信任 H5传来的实际金额。

### 14.2 状态机

```text
IDLE
  → PREPARING
  → DIALOG_SHOWING
  → ORDER_CREATED
  → EXTERNAL_APP / POLLING
  → SUCCESS / FAILED / CANCELLED / UNKNOWN
```

### 14.3 外部支付返回

- 外部支付开始前保存 paymentIntentId和原始 WebViewId；
- 宿主 onPause时不销毁 H5；
- onResume后只允许启动一个轮询任务；
- 轮询与 Overlay生命周期解耦；
- 即使原始 WebView已销毁，支付结果仍保存到 SessionStore；
- 下次 H5 ready或 `payment.query` 时补发最终状态；
- 同一 idempotencyKey不能创建多个订单。

## 15. 微信小程序 Scheme 实现

### 15.1 不使用 OpenSDK

SDK不引入、初始化或调用微信 OpenSDK，小程序只通过 `Intent.ACTION_VIEW` 调起微信 scheme。

### 15.2 Scheme校验

允许：

```text
weixin://dl/business/...
```

校验：

- scheme必须精确为 `weixin`；
- host/path必须在白名单；
- 拒绝 `intent:`、`javascript:`、`file:` 和 `content:`；
- 限制 scheme总长度；
- 推荐 scheme由 WishFox服务端生成并短期有效；
- H5不能通过该接口打开任意 App。

### 15.3 调起流程

```text
H5 miniProgram.openScheme
  → 参数和 Origin校验
  → 检查微信包是否可处理 Intent
  → 保存 pendingSchemeRequestId
  → startActivity(ACTION_VIEW)
  → 返回 accepted_to_launch
  → 宿主 onPause
  → 用户返回宿主
  → 发送 miniProgram.returned
```

`accepted_to_launch` 不代表小程序内业务成功，最终业务结果由 H5向服务端查询。

## 16. 图片保存实现

### 16.1 目标

支持：

1. 下载 H5指定的可信图片；
2. 保存到 SDK私有沙盒；
3. 根据 H5参数决定是否写入系统相册；
4. API 29+ 不申请权限直接写 MediaStore；
5. API 21～28 无权限时明确降级；
6. 下载和相册写入均异步执行；
7. H5通过事件获得最终结果。

### 16.2 图片来源

优先支持 HTTPS URL：

- URL必须属于图片/CDN白名单；
- 私有图片由服务端生成短期签名 URL；
- SDK不读取 WebView Cookie用于下载；
- 不接受任意本地文件路径；
- 可选支持受限 data URL，但必须设置较小上限。

### 16.3 沙盒存储

长期保存：

```text
filesDir/wishfox_sdk/images/{assetId}.{ext}
```

临时保存：

```text
cacheDir/wishfox_sdk/images/{requestId}.tmp
```

流程：

```text
下载到 tmp
  → 校验响应码
  → 校验 Content-Type
  → 校验文件签名
  → 校验文件大小
  → 计算 SHA-256
  → 原子重命名到正式文件
  → 返回 assetId
```

不返回真实沙盒路径。需要在 H5内重新显示时，可通过 `WebViewAssetLoader` 提供只读虚拟 URL：

```text
https://appassets.androidplatform.net/assets/wishfox/{assetId}
```

### 16.4 API 29+ 写系统相册

使用 `MediaStore.Images.Media`：

```java
ContentValues values = new ContentValues();
values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WishFox");
values.put(MediaStore.Images.Media.IS_PENDING, 1);
```

写入成功后：

```java
values.clear();
values.put(MediaStore.Images.Media.IS_PENDING, 0);
resolver.update(uri, values, null, null);
```

失败时删除未完成记录。

不声明：

- `WRITE_EXTERNAL_STORAGE`；
- `READ_EXTERNAL_STORAGE`；
- `READ_MEDIA_IMAGES`；
- `MANAGE_EXTERNAL_STORAGE`。

### 16.5 API 21～28

严格无权限模式下：

- 沙盒保存正常；
- 自动写系统相册不支持；
- 返回 `galleryStatus=unsupported_no_permission`。

可选用户选择模式：

- 启动 `ACTION_CREATE_DOCUMENT`；
- MIME为图片类型；
- 用户选择位置；
- SDK获得临时 URI写入权限；
- 复制沙盒文件到用户选择 URI；
- 返回 `galleryStatus=user_selected`。

该模式可能需要一个透明的 `FSFileSaveActivity` 负责接收 Activity结果。它只负责系统文件选择器，不承载业务页面。启动选择器会使宿主进入 `onPause()`，属于正常系统行为。

若产品要求 API 21～28 必须静默进入公共相册，则与“不申请存储权限”的约束冲突，必须二选一。

### 16.6 限制建议

- 单图最大 20 MB；
- data URL最大 5 MB；
- 文件名最大 128字符；
- 支持 MIME：`image/png`、`image/jpeg`、`image/webp`；
- GIF是否支持由 capability返回；
- 同时最多两个图片下载；
- 同一个 requestId不能重复执行；
- 相同 URL可以按 ETag或 hash复用沙盒文件；
- 定期按 LRU清理临时缓存；
- 正式 filesDir图片由业务或 TTL策略清理。

## 17. 悬浮球远程图片

现有 `FSFloatImageManager`继续保留：

- URL未变化且缓存可用时复用；
- 下载失败保留旧缓存；
- 缓存损坏回退内置图；
- 文件使用临时文件原子替换。

必须将刷新触发从 `FSHomeViewModel` 解耦到 `WishFoxSdk.initialize`：

- SDK初始化后异步刷新；
- App进入前台时按 TTL刷新；
- 不因每个 Activity创建都重复下载；
- H5首页是否打开不影响悬浮球图片更新；
- 新图片下载成功后可通知当前悬浮球刷新。

## 18. 权限与 Manifest

SDK Manifest只保留联网所需的普通权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

不新增：

```text
ACCESS_NETWORK_STATE
READ_EXTERNAL_STORAGE
WRITE_EXTERNAL_STORAGE
READ_MEDIA_IMAGES
READ_MEDIA_VIDEO
CAMERA
RECORD_AUDIO
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
MANAGE_EXTERNAL_STORAGE
```

由于不声明 `ACCESS_NETWORK_STATE`，SDK不应通过原生 ConnectivityManager提供网络状态。H5可使用浏览器 `online/offline` 事件，并以真实请求结果为准。

## 19. API 21～35+ 兼容策略

### 19.1 构建配置

当前模块最低版本为 API 23、编译版本为 API 30。目标建议：

```text
minSdk = 21
compileSdk >= 35
targetSdk = 与发布策略一致，建议 35 或更高
```

同时升级到与 compileSdk 35兼容的 Android Gradle Plugin、Gradle和 JDK。工具链升级应单独验证，避免与 H5业务迁移混成一个不可回滚的大变更。

### 19.2 兼容矩阵

| 能力 | 21～22 | 23～25 | 26～28 | 29～32 | 33～35+ |
|---|---|---|---|---|---|
| 基础 WebView | 支持 | 支持 | 支持 | 支持 | 支持 |
| Web Message | 运行时检测 | 运行时检测 | 运行时检测 | 运行时检测 | 运行时检测 |
| JS Interface fallback | 必须准备 | 必须准备 | 可选 | 可选 | 可选 |
| `onRenderProcessGone` | 不支持 | 不支持 | 支持 | 支持 | 支持 |
| Safe Browsing | 不支持 | 不支持 | 运行时检测 | 运行时检测 | 运行时检测 |
| 无权限 MediaStore相册 | 不支持 | 不支持 | 不支持 | 支持 | 支持 |
| ACTION_CREATE_DOCUMENT | 支持 | 支持 | 支持 | 支持 | 支持 |
| Predictive Back | 不支持 | 不支持 | 不支持 | 不支持 | 支持 |
| DisplayCutout | 无 | 无/有限 | 支持 | 支持 | 支持 |

### 19.3 旧版 WebView错误回调

- API 21～22：实现旧版 `onReceivedError(WebView, int, String, String)`；
- API 23+：同时实现新 `WebResourceRequest/WebResourceError`；
- 只在主文档失败时展示全局错误页；
- 图片、统计等子资源失败不覆盖整个页面；
- API 26+ 实现 `onRenderProcessGone`；
- API 21～25 通过页面超时、WebView异常和加载状态做兜底重建。

## 20. 网络、缓存和 H5发布

### 20.1 H5入口配置

启动配置建议包含：

```json
{
  "h5Url": "https://sdk.wishfoxs.com/index.html",
  "h5Version": "2026.09.16.1",
  "bridgeMinVersion": "1.0",
  "features": {
    "splitView": true,
    "saveImage": true,
    "miniProgramScheme": true
  },
  "floatImageUrl": "https://static.wishfoxs.com/float.png"
}
```

远程 URL必须通过本地 Origin白名单验证。

### 20.2 缓存策略

- `index.html`：短缓存或 no-cache；
- JS/CSS：内容 hash + immutable；
- 图片：长缓存；
- 不在每次打开时清 WebView缓存；
- 可选使用 Service Worker缓存 H5壳和静态资源；
- 保留 last-known-good H5版本或最小错误页；
- H5发布必须支持灰度和快速回滚；
- 新 H5不得依赖旧 SDK不具备的 capability。

## 21. 错误处理

### 21.1 原生错误页

至少包含：

- 加载中；
- 无法连接；
- 页面不存在；
- 服务异常；
- SSL/安全拦截；
- WebView版本过低；
- 渲染进程异常；
- Bridge初始化失败；
- 重试按钮；
- 关闭按钮。

### 21.2 超时建议

| 操作 | 建议超时 |
|---|---:|
| H5首屏连接 | 15秒 |
| Bridge ready | 页面完成后5秒 |
| 普通 Bridge请求 | 10秒 |
| 登录弹窗 | 不自动超时，由用户关闭 |
| 支付 | 按支付模块状态机 |
| 图片下载 | 30秒或配置值 |
| 返回键 H5响应 | 300毫秒 |

## 22. 诊断和日志

建议新增指标：

- `h5_overlay_create`；
- `h5_load_start`；
- `h5_first_content`；
- `h5_bridge_ready`；
- `h5_load_failed`；
- `h5_renderer_gone`；
- `bridge_request_invalid`；
- `bridge_origin_rejected`；
- `layout_mode_changed`；
- `activity_recreated_restore`；
- `payment_state_changed`；
- `scheme_launch_result`；
- `media_download_result`；
- `media_gallery_result`。

禁止记录：

- token；
- Cookie；
- 手机号完整值；
- 支付密文；
- 微信 scheme完整敏感参数；
- 图片 Base64；
- H5任意原始请求体。

日志中 requestId可保留，用户 ID、订单号和 URL应脱敏或 hash。

## 23. 线程模型

- WebView创建、销毁、布局和 JS响应：主线程；
- 登录、支付弹窗：主线程；
- 网络下载、文件复制、hash：IO线程；
- MediaStore insert/update/delete：IO线程执行，结果回主线程；
- 所有异步结果回主线程前检查 instanceId和 destroyed；
- Bridge Handler不得阻塞主线程；
- 图片写入过程中支持取消标志；
- PaymentCoordinator保证同一时刻只有一个前台支付流程。

## 24. 资源释放顺序

每个 WebView销毁时执行：

```text
标记 destroyed
  → 取消页面相关请求
  → 移除 Bridge listener/interface
  → 清除 WebViewClient/WebChromeClient回调
  → stopLoading
  → 从父容器 removeView
  → loadUrl("about:blank")（按需）
  → clearHistory
  → removeAllViews
  → destroy
  → Java字段置 null
```

不要清除全局 Cookie和全局 WebView缓存。

## 25. 迁移计划

### 阶段 0：工具链和协议

- minSdk调整到21；
- compileSdk升级到35+；
- 引入 AndroidX WebKit；
- 冻结 JS Bridge v1；
- 完成服务端 H5 session exchange、payment intent和 scheme接口。

### 阶段 1：H5容器

- 新增 WebViewFactory；
- 新增 Primary WebView；
- 完成 Overlay显示、关闭、错误页和 Insets；
- 完成 Bridge ready、Toast、关闭和环境信息。

### 阶段 2：生命周期

- 完成宿主 pause/resume转发；
- 完成旋转不重建路径；
- 完成 Activity重建 snapshot路径；
- 完成返回键；
- 完成渲染进程恢复。

### 阶段 3：原生能力

- 登录；
- 支付；
- 微信 scheme；
- 图片沙盒保存；
- API 29+ MediaStore；
- API 21～28系统选择器降级。

### 阶段 4：双 WebView

- 横屏 Split；
- 竖屏 Stacked；
- 主副 WebView独立 Bridge和返回栈；
- 旋转时模式自动降级/恢复。

### 阶段 5：灰度迁移

- 首页切 H5；
- 消息切 H5；
- 记录、礼包、狐币等切 H5；
- 保留原生页面远程回退一个版本周期；
- 稳定后删除旧原生业务页面。

## 26. 测试矩阵

### 26.1 宿主类型

- 普通 AppCompatActivity；
- 普通 Activity；
- UnityPlayerActivity；
- Cocos Activity；
- 沉浸式横屏游戏；
- 可旋转普通游戏。

### 26.2 系统版本

- API 21；
- API 22；
- API 23；
- API 26；
- API 28；
- API 29；
- API 30；
- API 31/32；
- API 33；
- API 34；
- API 35及更高预览环境。

### 26.3 WebView版本

- 系统旧 WebView；
- 最新 Android System WebView；
- Chrome WebView Provider；
- 不支持 Web Message的降级环境；
- WebView Provider被禁用或缺失环境。

### 26.4 方向和窗口

- 固定横屏；
- 固定竖屏；
- 横转竖；
- 竖转横；
- 快速连续旋转；
- 旋转时支付弹窗打开；
- 旋转时图片下载；
- 旋转时 Secondary显示；
- 多窗口调整尺寸；
- 刘海屏和手势导航；
- Activity重建与不重建两种宿主。

### 26.5 图片

- API 29+无权限保存系统相册；
- API 21～28只保存沙盒；
- API 21～28系统选择器保存；
- 用户取消选择器；
- 大文件；
- MIME伪造；
- 非白名单域名；
- 下载中断；
- 重复 requestId；
- 旋转和外部跳转后回调恢复。

### 26.6 验收标准

1. 未登录点击悬浮球只显示原生登录弹窗，不创建首页 WebView；
2. 登录成功后创建首页静态壳，H5 就绪后获取短时 Token，首次业务请求等待交换完成；
3. 已登录点击悬浮球打开首页，宿主不触发 `onPause()`；
4. 登录弹窗、支付弹窗和 Toast不触发宿主 `onPause()`；
5. 横屏内部链接创建/复用右侧 Secondary，竖屏内部链接创建/复用等宽 stacked窗口；
6. 横转竖、竖转横只重新布局，不重复加载未销毁的 WebView；
7. 微信 scheme、支付宝和系统文件选择器引起的 `onPause()` 能正确恢复；
8. Activity不重建时旋转不刷新 WebView；
9. Activity重建时恢复到原 route、布局和 pending operation；
10. 非白名单 Origin无法调用 Bridge；
11. API 21降级 Bridge可正常工作；
12. API 29+不申请存储权限即可写入应用创建的相册图片；
13. API 21～28不会错误声称相册写入成功；
14. 连续打开关闭 Overlay 100次后无 WebView/Activity泄漏；
15. 同一支付、媒体或内部导航 requestId不会重复执行；
16. 关闭 Overlay后活跃 WebView数量回到零；
17. 悬浮球远程图片失败时使用旧缓存或默认图，且不阻塞登录/首页；
18. H5发布新版本时旧 SDK能通过 capability安全降级。

## 27. 预计涉及的工程文件

主要修改或新增范围：

```text
foxsdk/build.gradle
foxsdk/src/main/AndroidManifest.xml
foxsdk/src/main/java/com/wishfox/foxsdk/core/FoxSdkOverlayManager.java
foxsdk/src/main/java/com/wishfox/foxsdk/core/WishFoxSdk.java
foxsdk/src/main/java/com/wishfox/foxsdk/core/FoxSdkH5SessionStore.java
foxsdk/src/main/java/com/wishfox/foxsdk/ui/view/widgets/FoxSdkH5OverlayView.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkWebViewFactory.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkWebViewSlot.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkJsBridge.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkBridgeRouter.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkAuthCoordinator.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkPaymentCoordinator.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkSchemeJumpCoordinator.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkMediaSaveCoordinator.java
foxsdk/src/main/java/com/wishfox/foxsdk/web/FoxSdkBootstrapCoordinator.java
foxsdk/src/main/res/layout/fs_overlay_h5.xml
foxsdk/src/main/res/layout-land/fs_overlay_h5.xml
```

旧的原生业务 Activity、Overlay、Adapter和 ViewModel在灰度期保留，稳定后再删除。

## 28. 最终约束总结

- SDK业务页面全部 H5；
- 原生 Overlay不启动业务 Activity；
- 横竖屏以真实宿主环境为准；
- Activity重建必须通过轻量 snapshot恢复；
- 微信小程序只使用受控 `weixin://` scheme；
- SDK不接入微信 OpenSDK；
- SDK不申请存储等运行时权限；
- API 29+无权限写入自己创建的 MediaStore图片；
- API 21～28只能保证沙盒保存，公共相册必须降级为用户文件选择器或宿主 Delegate；
- Bridge必须版本化、Origin受限、方法白名单化；
- 支付、媒体和 scheme全部使用 requestId/idempotencyKey防重复；
- WebView关闭后必须彻底销毁；
- 普通 H5业务更新不得要求第三方游戏更新 AAR。

## 29. 商单视频播放与图片预览（新增）

### 29.1 交互边界与显示规则

商单列表、Item与封面由 H5渲染；点击视频/图片后 H5调用 `media.previewVideo` / `media.previewImage`。Android使用 `FSMediaPreviewView`，作为当前 Activity DecorView中高于 H5 Overlay的全窗口 View。它是原生预览层，不占用 Primary/Secondary业务窗口、不改变它们的 URL和返回栈。

预览不启动 Activity、Service，不申请悬浮窗、存储、媒体读取权限，不调用 `setRequestedOrientation`，不写宿主系统栏、亮度、音量和游戏暂停开关。全屏指铺满宿主可用窗口；分屏/自由窗口不越过宿主边界。黑色背景铺满窗口，返回/播放按钮单独应用安全区，沿用游戏的沉浸模式。预览打开和关闭本身不会触发宿主 Activity onPause；游戏与 SDK仍共享进程和 GPU/内存，不能承诺资源消耗或任何异常绝对不影响宿主。

| 宿主窗口 | 视频画幅 | 显示方式 |
|---|---|---|
| 横屏 | 竖视频 | 居中、通常高度贴合、左右黑边 |
| 横屏 | 横视频 | 按比例完整显示，比例不同时保留必要黑边 |
| 竖屏 | 竖视频 | 按比例完整显示，比例不同时保留必要黑边 |
| 竖屏 | 横视频 | 居中、通常宽度贴合、上下黑边 |

统一 FIT_CENTER/contain：`scale=min(windowWidth/videoWidth,windowHeight/videoHeight)`；显示尺寸为原始显示尺寸乘 scale。这里“铺满”确定为尽可能放大且保留完整画面，不做裁剪或拉伸；对极端长宽比，以完整显示优先。Android不依据视频比例自动旋转窗口，不提供播放器旋转按钮。若宿主自身允许传感器旋转或主动切换方向，SDK跟随新窗口重新适配；既不修改宿主方向、又绝对禁止宿主旋转不能同时保证。若游戏要求方向固定，由宿主既有方向策略控制。

图片同样首次 contain居中，双指放大至基础适配比例的 4倍，缩小不低于 1倍；缩放围绕手指焦点，放大后可平移但不能把图片移出可视区域。当前展示单张静态位图，GIF/WebP动画不保证播放，超大长图以降采样预览而非原图分块浏览。

### 29.2 原生模块与数据流

| 模块 | 已实现职责 |
|---|---|
| `FSMediaPolicy` | 精确 HTTPS Origin比对、URL上限、文件上限、contain尺寸计算；不依赖 Android |
| `FSMediaFetcher` | 仅图片：工作线程私有缓存下载、手动校验最多3次重定向、字节/时间上限、取消和临时文件删除；视频不经过此模块 |
| `FSMediaPreviewView` | 全窗口层、MediaPlayer/TextureView在线缓冲播放、暂停/重播/重试/返回、音频焦点、弱网和播放器状态 |
| `FSZoomImageView` | 1～4倍缩放、双指焦点、拖动边界、单击/拖动关闭 |
| `FSH5OverlayView` | 请求校验、来源 WebView绑定、previewId、单实例互斥、响应/状态事件、document generation |
| `FoxSdkOverlayManager` | 转发 host pause/resume/stop/destroy、游戏返回键集成入口 |
| `wishfox-media-bridge.js` | H5预览调用封装、Promise超时、媒体事件订阅，复用已有完整 Bridge |

视频流程：用户点击 → JS请求 → 来源/参数/URL校验 → 创建原生层 → 立即返回 accepted/previewId → 将 HTTPS URL直接交给 MediaPlayer → prepareAsync在线准备 → ready → 用户播放意图成立时 start → buffering → playing；播放中可反复进入 buffering，关闭后释放连接、播放器和 Surface。**不等待完整文件下载，不创建视频临时文件。**

图片流程仍为：校验 → 创建预览/accepted → 有界图片临时下载 → Glide异步解码 → ready → closed并清理文件。accepted仅代表接收并打开预览，不代表视频已出画面或图片已解码。

### 29.3 视频实现与格式策略

本轮使用系统 `MediaPlayer + TextureView`，兼容既有 compileSdk 30/Java 8工程，不引入要求更高 minSdk的播放器依赖。TextureView便于视频随拖动移动，要求宿主硬件加速；未启用时返回 `HARDWARE_ACCELERATION_REQUIRED`，SDK不替宿主强开窗口硬件加速。

视频采用 HTTP渐进式在线播放：使用 API14起公开的 `setDataSource(Context, Uri, Map<String,String>)` 设置经过校验的 HTTPS地址，然后 `prepareAsync()`。不能使用非公开的 `setDataSource(String, Map)` 重载，也不使用先下载再播放的文件路径。网络读取/解复用/缓冲由系统播放器处理，达到可播条件即开始，不检查完整下载进度是否达到100%。连接、准备、缓冲期间始终可以返回或拖动关闭。

SDK不创建视频下载任务、不预取整个商单列表、不启用视频磁盘缓存，不再使用100 MiB视频下载上限。播放器仍会按自身策略预缓冲；SDK没有提供可调的缓冲字节数、网络速率或累计流量上限，不能将“无磁盘缓存”理解为“没有内存占用/没有网络流量”。每次重新打开可能重新联网获取数据，这是无需长期储存视频的取舍。请求附带 `Cache-Control: no-store`，但不替宿主设置全局 HTTP缓存，也不承诺所有厂商内部实现完全不使用临时存储。

服务端/CDN联调要求：

- 提供最终 HTTPS视频直链，而非播放网页、登录跳转页、blob URL或需要额外请求头鉴权的地址；可用有效期足够覆盖一次预览的短期签名 URL。
- 首期兼容基线为 MP4（H.264 + AAC），建议720p/1080p并控制码率；MP4使用 faststart，把 moov元数据放在文件前部，避免尾部元数据导致首帧等待。CDN支持正确的 Range/206与 Content-Range，尤其用于重连后定位；内容类型、长度和证书链需正确。
- 不做完整下载或 HEAD预检来决定能否播放；不满足渐进播放条件的资源可能准备失败/超时，应修复资源/CDN，不回退为完整下载。弱网仍可能缓冲，不能保证零等待。
- 不循环、不后台播放；不提供投屏/PiP/DRM/HLS/DASH/直播兼容保证。实际解码支持受设备、profile、level、音轨和文件质量影响。带90/270度旋转元数据的视频纳入真机用例，服务端最好将旋转烘焙到像素，减少厂商差异。

播放器状态和时限：

| 阶段 | 处理与退出条件 |
|---|---|
| 首次准备/手动重连 | `loading`，prepareAsync最多等待30秒；超时为 `MEDIA_TIMEOUT` |
| 准备完成 | `ready`只表示播放器已准备，不等于完整文件下载完成或首帧已显示；autoPlay默认true |
| 恢复位置 | 重连准备后，按记录位置异步seek；最多等待30秒，失败/超时可手动重试，不无限重连 |
| 启动/播放中缺数据 | `buffering`；缓冲结束、开始渲染或播放位置推进后转 `playing`；缺数据期间保留暂停与返回按钮 |
| 缓冲停滞 | 同一段连续缓冲最多30秒，超时为 `MEDIA_BUFFER_TIMEOUT`；重复缓冲通知不延长这段期限 |
| 用户暂停/音频失焦 | 清除缓冲计时与进度探测，不将正常暂停判为弱网超时；不因 BUFFERING_END或重新获得音频焦点自动续播 |
| 错误 | 释放播放器、连接、焦点，显示“重试/返回”；手动重试使用同一 previewId及 URL重新准备并尝试恢复位置 |
| 播放结束 | `ended`，位置归零，保留预览层与重播按钮；不是自动关闭 |

每秒读取播放位置作为厂商漏发缓冲通知时的兜底；只有有播放意图且宿主前台时运行。缓冲百分比来自系统播放器，仅供原生提示使用，不代表首帧剩余等待时间，也不新增高频 JS进度事件。`playing`是播放器运行状态而非已观看/任务完成证明。

播放器控制状态仅在主线程推进，使用异步prepare，不在主线程循环下载视频；未准备不得 start/pause/seek。所有异步回调核对 player实例和关闭标志，旧实例迟到回调不得恢复播放。宿主暂停时记录位置并释放播放器（包含网络数据源），回前台不会自行重新播放；点击播放后重连并尝试定位。Surface单独销毁同样释放旧播放器；同一 View的 Surface重新可用时可重新准备网络源、恢复暂停位置，但不自动出声。恢复依赖资源可寻址及签名仍有效，不保证逐帧准确；签名失效须关闭预览，由 H5重新取最新 URL后打开。

关闭是幂等终态：释放播放器/连接/Surface/焦点，移除本 View的全部 Handler任务，清除 URL和图片 Target，不改宿主全局 timer、不持有跨 Activity播放器。普通用户暂停可以保留当前连接及播放器自身缓冲；退后台和关闭则主动释放，不继续预缓冲。系统/厂商网络组件退出有内部时序，不承诺最后一个网络包在调用release的瞬间消失。

### 29.4 手势、层级和返回

- 视频支持上下拖动关闭：单指垂直位移超过 `max(96dp,窗口高度×20%)` 且大于水平位移时关闭；未过阈值回位。按钮区域优先点击，双指序列不会触发拖动退出。
- 图片单击关闭；双指缩放中的抬指不触发单击；基础比例允许上下拖动关闭，放大状态仅平移，缩回基础比例才能拖动退出。
- 预览吞掉自己的触摸和返回事件，退出动作不传到底层游戏/H5；返回按钮始终可用，包括加载失败和下载中。
- AndroidX宿主注册可移除的 OnBackPressedCallback；API33+尝试注册 OnBackInvokedCallback，保留旧按键路径，退出立即注销。不修改宿主 Manifest的返回手势配置。
- 游戏完全接管返回事件时，调用 `FoxSdkOverlayManager.onHostBackPressed(activity)`；返回 true则不要再退出游戏。预测返回的动画预览不在首期实现范围。
- 同一 H5 Overlay同一时刻最多一个原生预览，新打开请求返回 BUSY；只能由打开它的 WebView关闭对应 previewId。当前实现不允许两个业务 WebView同时各开一个预览。

### 29.5 生命周期与音频

| 场景 | 预览处理 | H5/宿主 |
|---|---|---|
| 打开/正常关闭预览 | attach/remove原生 View | Activity保持 resumed，H5滚动/路由保留 |
| Activity.onPause | 记录视频位置、禁用自动续播、释放播放器/网络源、放弃焦点 | 不主动启动后台服务 |
| Activity.onResume且未 stopped | 不自动重连；点击播放后在线准备并定位；不保证保留暂停帧 | 不把 resumed当播放成功 |
| Activity.onStop | 关闭预览，释放播放器/连接；若是图片则取消下载并清理临时文件 | 返回后重新点 Item打开 |
| Activity.onDestroy/配置重建 | 完整清理，不跨 Activity持有媒体 View/Player | 首期不自动恢复原生预览，更不自动出声 |
| Activity不重建且Surface保留的尺寸变化 | 重算 fit、图片边界，继续当前流 | 不换 URL、不主动重新连接 |
| Surface销毁再创建 | 保存位置、释放并重新准备视频流，保持不自动播放 | 没有本地视频文件可以恢复 |
| 来源文档导航、来源 WebView销毁 | 关闭预览，旧 generation回调丢弃 | 不向新文档投递旧请求结果 |
| 网络/格式/解码错误 | 释放播放器，显示视频重试/返回或图片返回错误层 | 发送 error；视频手动重试回 loading；关闭后发送 closed |

有声播放使用 transient音频焦点，API21～25旧接口、26+ AudioFocusRequest。失焦暂停、放弃焦点，不强制恢复其他 App或游戏的音量。`muted=true`不请求焦点且音量置零，适合宿主不希望影响游戏声音的商单预览。抢占焦点可能触发游戏自己的音频策略；SDK不直接操作游戏 AudioSource，宿主按自己的音频焦点回调恢复声音。API35且宿主 target35+在非顶部 App时可能无法取得焦点，返回 `paused/audio_focus_denied`并等待用户，不能为绕过限制创建服务。

### 29.6 资源与 URL安全

媒体 Origin由 `setH5MediaOrigins(...)`设置，默认无列表时只允许 H5可信 Origin；CDN白名单不会授予 CDN JS Bridge权限。入口允许 HTTP/HTTPS 绝对 URL（≤4096字符），生产环境建议仅使用 HTTPS；开发/内网使用 HTTP 时必须显式开启 `setAllowInsecureH5(true)`。SDK 始终精确校验 Origin及非默认端口，拒绝 userinfo、fragment、file/content/data/blob/weixin等。SDK不注入原生 token、H5 Cookie或业务自定义请求头，不记录带签名 URL；不修改宿主全局 CookieHandler。系统网络栈自身可能使用宿主配置的 Cookie策略，不能把“不注入”解释为独立 Cookie隔离。

图片区分于视频：图片使用自有下载器，禁用自动重定向，最多3跳且逐跳校验完整 Origin；视频由系统 MediaPlayer联网，通过公开头选项 `android-allow-cross-domain-redirect=0` 禁止跨域重定向。两个域名即使都在白名单内也不允许用跨域302串联，应直接传最终 CDN URL。系统内部跳转没有逐跳回调，不能宣称视频也具备图片下载器的“逐跳端口校验/3跳限制”；不同厂商的跳转/TLS行为需要兼容性测试，后端必须提供受控直链，不依赖重定向链。

只有图片下载有20 MiB上限，响应声明长度和实际读字节均检查；连接与单次读取超时15秒，总下载读循环120秒限制（最后一次网络阻塞可能另耗15秒）。图片文件只存 cacheDir/wishfox-preview，关闭时取消请求并删除本次临时文件，不进入相册、不用公共存储；进程异常退出留下的图片缓存需后续维护清理策略，当前不宣称已实现定时清理。当前视频分支不创建下载器、临时目录或文件，无视频缓存容量逐次累积；不扫描删除宿主其他缓存，也不自动删除旧版本遗留文件。

图片用已有 Glide 4.12在工作线程解码、限制最大2048×2048请求尺寸，复用 EXIF方向处理；关闭 clear该 Target，不清空 Glide全局缓存。最大位图约16 MiB，仍需计入GPU纹理/解码临时内存；4倍手势缩放只放大已解码位图，不重新加载原图。下载和播放取消/迟到回调均检查实例与关闭标志。

当前 Bridge仍使用旧系统兼容 addJavascriptInterface。原生可验证拥有它的主文档和 generation，但此接口不能可靠证明具体调用 iframe来源，因此上线 H5必须配置 `frame-src 'none'`、禁止第三方 iframe及非可信脚本；完整 Origin可验证 WebMessage迁移仍是总方案待办，不能把当前校验宣传成 iframe级安全隔离。

### 29.7 首期落地与联调

本轮实现的是原生媒体预览扩展及其 `postMessage`传输、媒体 capability和局部 ready。原有完整 Bridge方案中的登录会话交换、支付意图、媒体保存、恢复事件等并非本轮全部落地，不能因为预览 capability存在就认为整套协议已实现。H5可加载 assets中的 `wishfox-media-bridge.js`（复制到自己的构建产物；原生不会自动注入这个文件），通过 `media.getPreviewCapabilities`查询实际支持能力。旧 WebView由 H5补齐 Promise polyfill。

`bridge.ready` 当前返回 selectedProtocolVersion/sdkVersion/apiLevel/appId/channelId/isLoggedIn/orientation/navigationMode/safeInsetTop/safeInsetRight/safeInsetBottom/safeInsetLeft/safeInsetUnit/sessionId/webViewId/bridgeMode/capabilities/environment/authState；其中 appId/channelId 是初始化时固定配置，四个 safeInset 字段以 CSS px 返回（safeInsetUnit=css_px，已按 WebView devicePixelRatio 从 Android 物理 px 换算），用于 H5 内容安全区，authState 为原生登录及当前文档短时会话元信息。H5 Overlay 不再额外叠加同一组原生 padding。支付恢复等全量协议字段仍需按 capability 判断，不能仅凭 ready 响应猜测未实现能力。入口仍需配置真实 H5/CDN地址，示例域名不是可上线地址。

### 29.8 验收矩阵

API21/23/26/28/29/30/33/35+分别覆盖普通 Activity、AndroidX、Unity/Cocos返回键；宿主 target30与target35+分别测音频焦点和系统返回。横/竖宿主×横/竖/方形/超长视频、旋转元数据、导航栏/挖孔、分屏、无硬件加速、HEVC不支持、损坏视频、JPEG/PNG/WebP/EXIF图都需真机检查。

验证：图片下载/视频prepare/播放/缓冲/暂停/缩放时返回；100次开关无增长的 player/Surface/Activity引用，视频预览前后SDK缓存目录无新增视频文件；弱网/无网/404/过期签名/跨域重定向/图片超限；播放中Home键/来电/锁屏/外部支付；Activity重建；手势 CANCEL/多指切换；连续点两个 Item返回 BUSY；关闭后二次关闭不崩溃；图片放大平移不意外退出；任何关闭均无音频残留。缺乏设备验证时不能标注“API21～35+已全面验证”。

流式专项：大文件未传输完成即出现首帧；限速时先缓冲后继续；中途断网30秒停滞超时；网络恢复后手动重试；暂停超过30秒不报缓冲超时；准备/缓冲中退后台无继续播放，回前台点击才重连；重连定位与不支持Range/签名过期资源的错误提示；结束后重播；autoPlay=false只准备不自动播放；关闭后旧回调与超时不再触发。

参考：[MediaPlayer在线数据源与异步准备](https://developer.android.com/media/platform/mediaplayer/basics)、[MediaPlayer API与跨域重定向选项](https://developer.android.com/reference/android/media/MediaPlayer)、[MediaPlayer状态与资源释放](https://developer.android.com/media/platform/mediaplayer/state-resources)、[音频焦点与Android15限制](https://developer.android.com/media/optimize/audio-focus)、[TextureView API](https://developer.android.com/reference/android/view/TextureView)。

### 29.9 验证记录（2026-09-17）

以下为1.1版历史验证，不覆盖1.2版在线播放修改：

- 使用本机 Java 8、Android 30 android.jar与本地缓存依赖，对9个新增/修改 Java文件进行独立 javac检查通过（包含预览容器、Bridge接入、生命周期接入）。它不替代 Gradle依赖解析、资源合并、Manifest合并、D8/R8或 APK/AAR打包。
- `tools/tests/FSMediaPolicyCheck.java`：23项 URL白名单/协议/端口/欺骗地址/长宽比计算检查通过。
- `node tools/tests/media-bridge-check.js`：请求、响应、错误、事件订阅退订、重复加载脚本、复用已有Bridge、不可用传输与超时检查通过。
- 全量 Gradle构建未完成：Gradle 6.5使用 Java 8可启动，但离线依赖元数据解析失败，联网下载未获批准；没有以更换构建版本绕过此限制。
- 尚未执行真机播放器/图片手势/音频焦点/内存/旋转回归；不能将上述静态编译与脚本测试视为该项验收通过。

1.2版按用户要求不执行编译构建（包括独立javac及Gradle任务），不运行设备测试。本轮仅进行源码/公开API签名/状态流转审查、旧视频下载引用检索及差异空白检查；这些检查不是编译或真机验证，最终编译运行与上列流式验收由项目方执行。

## 30. 第三方接入混淆与发行保护

混淆规则已按当前实际代码补充到 `foxsdk/consumer-rules.pro`，通过 defaultConfig 的 consumerProguardFiles 随 AAR交付给宿主，而不是仅配置 SDK自身的 proguard-rules.pro。保留范围包括初始化/配置/Overlay/登录支付公开入口、新旧 JS Bridge、Gson模型字段及构造器、Retrofit代理和返回泛型、旧 Activity/支付回调、XML自定义 View、BRVAH反射创建及本地闭源支付/Bugly边界。

不强制宿主关闭混淆/优化，不使用全 SDK或全 AndroidX/Glide/OkHttp keep，不输出大范围缺失类警告屏蔽；媒体播放器、手势和生命周期内部实现继续依靠正常Java/Android回调可达性保留。API33返回键反射目标为系统类，无需对整个Android包添加规则。模型和已打包闭源厂商包有明确保守保留范围与体积取舍。

`foxsdk/proguard-rules.pro`已改为复用消费方规则并单独保护发行公共API；SDK release的 minifyEnabled=false保持不变。开启宿主混淆、资源压缩、R8 full mode后的实包回归仍由项目方执行，本轮未构建或运行混淆器。

完整规则说明、手工AAR/渠道合包要求、依赖边界及release验收清单见：[WishFox Android SDK 混淆与发布说明](./android-sdk-obfuscation.md)。
