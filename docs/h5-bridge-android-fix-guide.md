# H5 端桥接修改说明（配合 Android SDK 修复版）

本文档对应 Android 侧当前已完成的 Bridge 修复。H5 必须按本文调整后再提供新的 dist 包。

## 1. 必须修复：登录请求不能把 null 当成默认超时

`auth.login` 由 Android 弹出原生登录窗口，用户完成操作所需时间不可预测。H5 传入 `timeoutMs: null` 时，必须表示“不启用普通 JS 超时”。

错误写法：

```js
const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
```

正确写法：

```js
const timeoutMs = options.timeoutMs === undefined
  ? DEFAULT_TIMEOUT_MS
  : options.timeoutMs;

if (timeoutMs !== null) {
  // 只有非 null 才启动 JS 超时计时器
}
```

`auth.login` 保持传入 `timeoutMs: null`。`auth.refreshSession` 可使用 20 秒左右的等待时间。不能在 10 秒后删除登录请求的 pending 记录，否则 Android 后续返回的成功响应会被 H5 丢弃。

## 2. 初始化顺序

建议初始化顺序：

```js
const ready = await WishFoxSDK.bridge.ready();
const appId = ready.appId;
const channelId = ready.channelId;
const capabilities = await WishFoxSDK.bridge.getCapabilities();
const environment = await WishFoxSDK.environment.get();
const authState = await WishFoxSDK.auth.getState();

if (ready.isLoggedIn === true) {
  await WishFoxSDK.auth.refreshSession({ reason: 'bootstrap' });
}
```

`environment.get` 已由 Android 实现，返回屏幕方向、尺寸、密度、安全区、布局模式、WebView 标识和 SDK 版本。

如果某个旧版本 Android 未实现 `environment.get`，H5 可以将它降级为非阻断能力，但不能让该接口失败导致整个 Bridge 初始化失败。

### ready 中的应用标识与短时 Token

`appId` 和 `channelId` 是 SDK 初始化时固定的应用/渠道标识，Android 会在 `bridge.ready` 成功响应中直接返回：

```js
const ready = await WishFoxSDK.bridge.ready();
const appId = ready.appId;
const channelId = ready.channelId;
```

这两个字段不会随登录用户或短时 Token 改变，不需要从 `auth.login` 或 `auth.refreshSession` 响应中读取。

用户已经登录时，点击悬浮球直接打开 H5，不会触发 `auth.login`。H5 在 `bridge.ready` 成功、确认 `ready.isLoggedIn === true` 后，应立即调用：

```js
const session = await WishFoxSDK.auth.refreshSession({ reason: 'bootstrap' });
const sessionToken = session.sessionToken;
const expiresIn = session.expiresIn;
```

这样每个新建的 Primary/Secondary 文档都会获取自己最新的短时 Token；Android 不在 `bridge.ready` 中同步执行网络换 Token，也不把短时 Token 放入 ready 响应。`sessionToken` 仍然只能保存在 H5 内存中，不得写入 URL、localStorage、日志或持久化缓存。

## 3. 登出流程（必须由 H5 发起）

H5 只负责调用：

```js
await WishFoxSDK.auth.logout({ confirm: true });
```

Android 会显示原生二次确认弹窗。H5 不得自行弹确认框后直接清除 Token，也不得自行调用接口模拟登出。

### 用户确认

Android 会：

1. 尽力调用服务端登出接口；
2. 清除原生用户信息；
3. 清除长期 Token；
4. 清除兼容 Authorization 存储键；
5. 清除所有 H5 文档内存中的短时 Token；
6. 返回 `OK` 和匿名状态；
7. 关闭 Primary/Secondary 所有 H5 页面；
8. 恢复悬浮球。

成功响应数据为匿名状态：

```js
{
  status: 'anonymous',
  user: null,
  sessionMode: 'short_token',
  sessionStatus: 'none',
  sessionExpiresIn: 0
}
```

成功后 H5 应清理自己的内存状态、取消业务请求和定时器，不要尝试继续操作已关闭的 H5 页面。Android 会在响应发送后关闭 Overlay，所以不要依赖响应之后仍能访问 DOM。

### 用户取消

用户取消原生确认时返回：

```text
USER_CANCELLED
```

H5 应保留当前登录状态和页面，不得清空本地状态。

### 登出错误

H5 应区分处理：

| code | 处理 |
|---|---|
| `USER_CANCELLED` | 保持当前页面和登录态 |
| `BUSY` | 稍后重试，不重复弹窗 |
| `ACTIVITY_UNAVAILABLE` | 等宿主恢复后重试 |
| `OK` | 清理 H5 内存状态，等待 Android 关闭 Overlay |

服务端登出失败不会阻止本地登出和 Overlay 关闭；本地认证状态以 Android 清理结果为准。

## 4. 根据 capability 控制功能入口

不能无条件展示或调用 Bridge 方法。启动后读取 `bridge.getCapabilities()`，当前 Android 修复版主要能力如下：

```js
{
  environmentGet: true,
  authGetState: true,
  authLogin: true,
  authRefreshSession: true,
  authLogout: true,
  clipboardCopyText: true,
  uiToast: true,
  uiClose: true,
  navigationOpenInternal: true,
  navigationOpenExternal: true,
  navigationUpdateState: true,
  navigationResolveBack: true,
  layoutSetMode: true,
  layoutCloseSecondary: true,
  mediaPreviewImage: true,
  mediaPreviewVideo: true, // 硬件加速关闭时以实际返回值为准
  mediaPreviewClose: true
}
```

以下能力仍需继续按 `false` 或 `METHOD_NOT_SUPPORTED` 处理：

```text
payment.start
payment.query
miniProgram.openScheme
media.saveImage
media.cancel
media.removeSandboxImage
diagnostics.report
```

退出登录入口必须使用 `authLogout` 判断；不要因为接口文档中曾经写过“规划能力”就继续把它隐藏。

## 5. 导航和布局调用

### 内部链接

```js
await WishFoxSDK.navigation.openInternal({
  url: '/message/detail/123',
  target: 'auto'
});
```

只允许同源相对路径或已登记的 HTTPS 地址。不要使用 `window.open()`，也不要把普通内部链接设置为 `target="_blank"`。

### 外部链接

```js
await WishFoxSDK.navigation.openExternal({
  url: 'https://www.example.com/activity/1',
  returnToOverlay: true
});
```

只允许 `http`/`https`。微信、支付等专用 Scheme 不要通过 `navigation.openExternal`。

### 路由状态

路由变化后调用：

```js
await WishFoxSDK.navigation.updateState({
  route: '/message/detail/123',
  title: '消息详情',
  canGoBack: true,
  hasUnsavedChanges: false
});
```

### 关闭 Overlay

```js
await WishFoxSDK.ui.close({
  reason: 'user_click',
  force: false
});
```

Android 返回成功后会关闭整个 H5 Overlay 并恢复悬浮球。

### 复制到剪贴板

```js
await WishFoxSDK.clipboard.copyText({
  text: '要复制的普通文本'
});
```

调用前检查 `clipboardCopyText` capability。文本去除首尾空白后不能为空，最大 8192 字符。不要复制短时 Token、长期 Token、支付签名或完整授权 URL。

## 6. 事件监听

H5 必须监听以下事件：

```text
auth.changed
environment.changed
layout.changed
layout.secondaryClosed
media.previewChanged
```

横竖屏切换时 Android 会发送 `environment.changed` 和 `layout.changed`。H5 不要固定假设横屏一定是 split、竖屏一定是 single，应使用事件中的 `actualMode`。

## 7. 错误展示和日志

Bridge 错误必须保留：

```text
method
requestId
code
message
```

示例：

```js
catch (error) {
  console.error('[WishFoxSDK]', {
    method: 'auth.logout',
    requestId: error.requestId,
    code: error.code,
    message: error.message
  });
}
```

不要把所有错误都转换成“操作失败”，否则无法和 Android 日志中的 requestId 对齐。

## 8. dist 构建要求

新的联调包必须包含：

- `manifest.json`；
- H5 commit；
- adapter commit；
- `dirty: false`；
- `sdkVersion` 和 `adapterVersion`；
- source map 或者与本次构建完全对应的源码压缩包。

构建前请确认 `index.html` 和资源路径可以在 Android WebView 的实际 HTTPS 地址或 Debug 本地地址下正常加载。不要把登录 Token 写入 URL、HTML、localStorage 或普通日志。

## 9. 联调验收清单

- `bridge.ready` 返回 `OK`；
- `environment.get` 返回 `OK`；
- `auth.getState` 返回正确登录状态；
- 登录超过 10 秒后完成仍能收到成功响应；
- 登录取消返回 `USER_CANCELLED`；
- 登出会出现 Android 原生确认弹窗；
- 取消登出后页面和登录态不变；
- 确认登出后用户信息、长期 Token、短时 Token 均不可继续使用；
- 确认登出后 Primary/Secondary 均关闭并恢复悬浮球；
- 横竖屏切换能收到环境和布局事件；
- 不支持的方法返回明确的 `METHOD_NOT_SUPPORTED`，而不是无响应；
- modern 和 legacy 两个 JS bundle 均通过以上测试。
