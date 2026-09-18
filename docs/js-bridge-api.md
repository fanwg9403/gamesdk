# WishFox H5 与 Android JS Bridge 交互协议

> 文档版本：1.3（2026-09-18：协议页返回链路与 H5 短时 Token 刷新闭环，协议仍为1.0）
> 协议版本：1.0  
> 适用平台：WishFox Android SDK，API 21～35+  
> 配套文档：[WishFox Android SDK H5 化技术实现方案](./android-h5-technical-design.md)

## 1. 文档目的

本文档定义 WishFox H5 页面与 Android SDK 原生层之间的交互协议，包括：

- Bridge初始化和能力协商；
- 请求、响应和事件的统一数据格式；
- 登录；
- Toast；
- Overlay关闭；
- 返回键；
- 横竖屏环境；
- 单 WebView和双 WebView布局；
- 支付；
- 微信小程序 scheme跳转；
- 图片下载、沙盒保存和系统相册保存；
- 商单视频原生播放、封面图原生预览及其关闭事件；
- 生命周期；
- 诊断；
- 错误码、超时和幂等约束。

H5业务代码不得直接调用底层注入对象，应通过 WishFox提供的 JS封装层调用，例如：

```javascript
await window.WishFoxSDK.auth.login();
```

底层 `WishFoxNative` 只作为传输通道。

## 2. 设计原则

1. **稳定**：协议采用版本号管理，H5发布不能随意改变已有方法语义；
2. **向后兼容**：新增方法、字段和枚举值必须允许旧 SDK安全忽略或降级；
3. **最小权限**：Bridge只暴露必要原生能力；
4. **Origin受限**：只有受信任的 WishFox H5 Origin可以调用；
5. **强类型**：所有字段必须有明确类型、必填性和长度限制；
6. **异步化**：登录、支付、图片保存、scheme跳转等均使用 Promise或事件；
7. **幂等**：支付、图片保存等有副作用的接口必须携带 requestId或 idempotencyKey；
8. **不泄密**：不向 H5提供原生长期 token，不把凭证放入 URL；
9. **多 WebView隔离**：Primary和 Secondary拥有独立通道和来源标识；
10. **生命周期安全**：旧 WebView销毁后，其回调不能发送到新 WebView。

## 3. 名词定义

| 名词 | 说明 |
|---|---|
| Primary WebView | SDK主 WebView，承载首页、列表和普通业务页面 |
| Secondary WebView | 横屏双栏模式下右侧详情 WebView |
| Overlay | 挂载在宿主 Activity中的 SDK根 View |
| Bridge | H5和原生之间的消息通信层 |
| Request | H5主动请求原生执行操作 |
| Response | 原生对某个 Request的直接结果 |
| Event | 原生主动发送给 H5的状态变化 |
| requestId | 单次请求的唯一 ID |
| idempotencyKey | 防止支付等副作用操作重复执行的业务幂等键 |
| sessionId | 本次 H5 Overlay会话 ID |
| webViewId | WebView标识：`primary` 或 `secondary` |
| capability | 当前 SDK和设备实际支持的功能 |

## 4. 传输层

### 4.1 H5 调用原生

优先传输方式：AndroidX WebKit `WebMessageListener`。

H5底层调用：

```javascript
window.WishFoxNative.postMessage(JSON.stringify(message));
```

在旧 WebView Provider上，Android可能降级为 `addJavascriptInterface`，但 H5调用形式保持一致。

业务代码禁止直接使用 `WishFoxNative`，必须调用封装后的：

```javascript
window.WishFoxSDK.invoke(method, params, options);
```

### 4.2 原生发送给 H5

H5 SDK初始化时注册统一入口：

```javascript
window.WishFoxSDK.__dispatch = function (message) {
  // 解析 response 或 event
};
```

原生通过 Web Message或兼容 `evaluateJavascript` 将 JSON消息发送到该入口。

H5业务代码通过事件订阅：

```javascript
const unsubscribe = window.WishFoxSDK.on('payment.statusChanged', event => {
  console.log(event.data.status);
});

// 页面销毁时取消订阅
unsubscribe();
```

### 4.3 字符编码

- 所有消息使用 UTF-8；
- JSON字符串不得通过字符串拼接生成；
- Android使用 JSON序列化器或 `JSONObject.quote()`；
- H5使用 `JSON.stringify()`；
- 禁止在方法名中传入 JS函数表达式；
- 禁止 H5指定原生要执行的任意 Java或 JavaScript方法名。

## 5. 公共消息格式

### 5.1 Request

```json
{
  "version": "1.0",
  "type": "request",
  "id": "01J8ZABC1234567890",
  "method": "auth.login",
  "timestamp": 1789526400000,
  "params": {}
}
```

字段定义：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `version` | string | 是 | 协议版本，当前为 `1.0` |
| `type` | string | 是 | 固定为 `request` |
| `id` | string | 是 | 请求唯一 ID，建议 UUID/ULID，最大64字符 |
| `method` | string | 是 | 方法名，最大64字符 |
| `timestamp` | number | 是 | H5发起请求的 Unix毫秒时间戳 |
| `params` | object | 是 | 方法参数，无参数时传空对象 |

约束：

- 同一个 session内 `id` 不得重复；
- 不接受空字符串 ID；
- 不接受超长 ID；
- 原生不信任 H5传入的 `webViewId`，实际来源由原生通道确定；
- 时间戳只用于诊断，不作为安全认证依据。

### 5.2 Response

成功：

```json
{
  "version": "1.0",
  "type": "response",
  "id": "01J8ZABC1234567890",
  "method": "auth.login",
  "success": true,
  "code": "OK",
  "message": "success",
  "timestamp": 1789526400200,
  "data": {}
}
```

失败：

```json
{
  "version": "1.0",
  "type": "response",
  "id": "01J8ZABC1234567890",
  "method": "auth.login",
  "success": false,
  "code": "USER_CANCELLED",
  "message": "用户取消登录",
  "timestamp": 1789526400200,
  "data": null
}
```

字段定义：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `version` | string | 是 | 原生选择的协议版本 |
| `type` | string | 是 | 固定为 `response` |
| `id` | string | 是 | 对应 Request.id |
| `method` | string | 是 | 对应 Request.method |
| `success` | boolean | 是 | 方法是否成功 |
| `code` | string | 是 | 统一结果码 |
| `message` | string | 是 | 可用于日志或提示的简短描述 |
| `timestamp` | number | 是 | 原生返回时间 |
| `data` | object/null | 是 | 方法返回数据 |

### 5.3 Event

```json
{
  "version": "1.0",
  "type": "event",
  "event": "environment.changed",
  "timestamp": 1789526400300,
  "webViewId": "primary",
  "data": {}
}
```

字段定义：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `version` | string | 是 | 协议版本 |
| `type` | string | 是 | 固定为 `event` |
| `event` | string | 是 | 事件名称 |
| `timestamp` | number | 是 | 事件时间 |
| `webViewId` | string | 否 | 目标或来源 WebView |
| `data` | object/null | 是 | 事件数据 |

## 6. JS SDK 推荐接口

建议 H5封装以下统一接口：

```typescript
interface WishFoxSDK {
  ready(): Promise<BridgeReadyResult>;
  invoke<T>(method: string, params?: object, options?: InvokeOptions): Promise<T>;
  on(event: string, handler: (event: BridgeEvent) => void): () => void;
  once(event: string, handler: (event: BridgeEvent) => void): () => void;
  off(event: string, handler: Function): void;

  bridge: BridgeApi;
  environment: EnvironmentApi;
  auth: AuthApi;
  ui: UiApi;
  navigation: NavigationApi;
  layout: LayoutApi;
  payment: PaymentApi;
  miniProgram: MiniProgramApi;
  media: MediaApi;
  diagnostics: DiagnosticsApi;
}
```

默认调用超时：10秒。登录、支付、媒体选择器等方法可使用独立超时策略。

## 7. 能力协商

H5不得仅根据 Android API判断能力，必须使用 `bridge.ready` 或 `bridge.getCapabilities` 返回结果。

### 7.1 `bridge.ready`

用途：

- H5通知原生页面已加载并准备接收消息；
- 协商协议版本；
- 获取 SDK版本、Bridge模式、能力、环境、登录态和恢复状态；
- 每个 WebView导航到新主文档后都必须重新 ready。

请求：

```json
{
  "version": "1.0",
  "type": "request",
  "id": "ready-001",
  "method": "bridge.ready",
  "timestamp": 1789526400000,
  "params": {
    "h5Version": "2026.09.16.1",
    "supportedProtocolVersions": ["1.0"],
    "route": "/home",
    "documentId": "doc-001"
  }
}
```

参数：

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `h5Version` | string | 是 | 最大64字符 |
| `supportedProtocolVersions` | string[] | 是 | 至少一个，最多10个 |
| `route` | string | 是 | 当前相对路由，最大512字符 |
| `documentId` | string | 是 | 本次 document唯一 ID，最大64字符 |

响应：

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "selectedProtocolVersion": "1.0",
    "sdkVersion": "1.4.0",
    "apiLevel": 35,
    "sessionId": "session-xxx",
    "webViewId": "primary",
    "bridgeMode": "web_message",
    "capabilities": {
      "auth": true,
      "toast": true,
      "payment": true,
      "splitView": true,
      "miniProgramScheme": true,
      "mediaSandbox": true,
      "mediaGalleryDirect": "api29_no_permission",
      "mediaGalleryPicker": "api21_plus_optional",
      "renderProcessRecovery": true,
      "predictiveBack": true,
      "supportedImageMimeTypes": [
        "image/png",
        "image/jpeg",
        "image/webp"
      ],
      "maxImageBytes": 20971520,
      "maxDataUrlBytes": 5242880
    },
    "environment": {},
    "authState": {},
    "restoreState": null
  }
}
```

`bridgeMode`：

| 值 | 说明 |
|---|---|
| `web_message` | 使用 AndroidX Web Message，推荐模式 |
| `restricted_js_interface` | 旧 WebView兼容模式 |

错误：

- `UNSUPPORTED_PROTOCOL_VERSION`
- `ORIGIN_NOT_ALLOWED`
- `INVALID_ARGUMENT`
- `BRIDGE_NOT_AVAILABLE`

### 7.2 `bridge.getCapabilities`

请求参数：空对象。

响应返回与 `bridge.ready.data.capabilities` 相同的对象。

使用场景：

- 页面运行期间重新确认能力；
- H5异步加载模块后查询；
- Secondary WebView确认自身能力。

## 8. 环境信息

### 8.1 `environment.get`

请求：

```json
{
  "method": "environment.get",
  "params": {}
}
```

响应：

```json
{
  "success": true,
  "data": {
    "orientation": "landscape",
    "widthPx": 1920,
    "heightPx": 1080,
    "widthDp": 960,
    "heightDp": 540,
    "density": 2,
    "fontScale": 1,
    "apiLevel": 35,
    "safeArea": {
      "top": 0,
      "right": 0,
      "bottom": 0,
      "left": 0
    },
    "layoutMode": "single",
    "webViewId": "primary",
    "locale": "zh-CN",
    "sdkVersion": "1.4.0"
  }
}
```

枚举：

- `orientation`: `portrait` / `landscape` / `undefined`
- `layoutMode`: `single` / `split` / `stacked`
- `webViewId`: `primary` / `secondary`

### 8.2 事件 `environment.changed`

触发时机：

- 横竖屏变化；
- 多窗口尺寸变化；
- 安全区变化；
- 状态栏/导航栏可见性变化；
- 字体缩放或 density变化导致布局需要更新；
- Activity重建后首次恢复。

事件示例：

```json
{
  "type": "event",
  "event": "environment.changed",
  "webViewId": "primary",
  "data": {
    "reason": "orientation",
    "orientation": "portrait",
    "widthPx": 1080,
    "heightPx": 1920,
    "widthDp": 540,
    "heightDp": 960,
    "density": 2,
    "safeArea": {
      "top": 48,
      "right": 0,
      "bottom": 48,
      "left": 0
    },
    "previousLayoutMode": "split",
    "actualLayoutMode": "stacked"
  }
}
```

H5处理要求：

- 不强制刷新整个页面；
- 使用响应式布局更新；
- 清理仅适用于旧方向的临时 UI；
- Split降级时保留当前详情 route；
- 不因为该事件重复请求支付或创建订单。

## 9. 生命周期

### 9.1 事件 `lifecycle.changed`

```json
{
  "type": "event",
  "event": "lifecycle.changed",
  "data": {
    "scope": "host",
    "state": "paused",
    "reason": "external_scheme"
  }
}
```

`scope`：

| 值 | 说明 |
|---|---|
| `host` | 宿主 Activity生命周期 |
| `overlay` | SDK Overlay显示状态 |
| `webview` | 当前 WebView状态 |

`state`：

| scope | state |
|---|---|
| host | `resumed`、`pausing`、`paused`、`destroying` |
| overlay | `visible`、`hidden`、`restoring`、`destroying` |
| webview | `active`、`inactive`、`recreated` |

`reason`可能值：

- `user_open`
- `user_close`
- `orientation`
- `activity_recreated`
- `external_scheme`
- `payment`
- `system_picker`
- `host_background`
- `renderer_recovery`
- `unknown`

H5建议：

- `host.pausing/paused`：暂停动画、视频、轮询和高频定时器；
- `host.resumed`：恢复必要逻辑，并查询 pending支付；
- `overlay.hidden`：停止非必要任务；
- `overlay.restoring`：等待 `bridge.ready` 的 restoreState；
- 不把生命周期事件当作支付成功依据。

## 10. 登录与短时会话（当前实现）

### 10.0 启动与能力边界

未登录点击悬浮球：原生登录弹窗 → 登录成功保存原生长期 Token → 打开 H5 首页。
已登录点击悬浮球：直接打开 H5 首页。悬浮球远程图片获取仍独立运行。

**首页首个 HTML 必须是可匿名加载的静态壳，不能要求先携带登录 Cookie。** H5 的 JS 就绪后，查询登录态并调用 `auth.refreshSession` 获取短时 Token，然后才加载受保护的业务接口。Secondary 中每次新文档也执行此初始化；原生不把 Token 拼入 URL、HTML 或首屏请求头。不要把“原生已登录”等同于“当前 H5 内存已有凭证”。

当前实现采用 `short_token`，取代旧稿的 exchangeCode/HttpOnly Cookie 交换方案。长期 Token 只在原生；短时 Token 只供 H5 内存使用。Android 尚无已定义的后端交换 API，因此必须由 SDK 原生集成方配置 `FoxSdkConfig.Builder.setH5SessionTokenProvider(...)` 接入真实服务端。未配置时不伪造成功、不透传长期 Token。

`bridge.getCapabilities` 和 `bridge.ready.data.capabilities` 增加：

| 字段 | 类型 | 当前意义 |
|---|---|---|
| authGetState | boolean | true，支持查询 |
| authLogin | boolean | true，支持原生登录 |
| h5SessionExchange | boolean | 是否配置交换器；不代表网络或服务端当前一定可用 |
| authRefreshSession | boolean | 与 h5SessionExchange 一致 |
| authLogout | boolean | false，本轮未开放 Bridge 登出 |

`bridge.ready.data.authState` 返回下节状态对象。旧稿其他环境/恢复字段仍属于全量协议设计，当前不能假定都已实现。

### 10.1 `auth.getState`

请求参数：`{}`。不弹窗、不交换凭证、不返回 Token。响应 data 示例：

```json
{
  "status": "authenticated",
  "user": { "id": "10001", "maskedMobile": "188****4782" },
  "sessionMode": "short_token",
  "sessionStatus": "none",
  "sessionExpiresIn": 0
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| status | string | 原生本地状态：anonymous / authenticated；不表示服务器已验证长期凭证仍有效 |
| user | object/null | 未登录为 null；已登录仅有 id（openId，可能 null）与 maskedMobile（不可脱敏则空串） |
| sessionMode | string | short_token 表示已配置交换器；none 表示未配置，不表示是否已换取 Token |
| sessionStatus | string | 当前 WebView 文档的会话元信息：none / valid / expired |
| sessionExpiresIn | number | 原生单调时钟计算的剩余秒数，向下取整且不小于 0；不是绝对时间戳 |

页面导航/销毁会清除该文档元信息；原生保存/清除登录结果后，旧世代会话变为 none。
`sessionStatus=valid` 也不保证 H5 内存里仍有 Token（例如业务主动清除了内存）；此时仍需刷新获取。到期不自动广播定时事件，H5 自行根据 expiresIn 或服务端明确的凭证过期错误刷新。

### 10.2 `auth.login`

用途：没有原生登录态时展示既有登录弹窗；已登录时不重复弹窗，直接返回状态或进行会话交换。

```json
{
  "version": "1.0",
  "type": "request",
  "id": "auth-login-001",
  "method": "auth.login",
  "params": { "reason": "user_action", "exchangeH5Session": true }
}
```

| 参数 | 类型 | 必填 | 默认/说明 |
|---|---|---|---|
| reason | string | 否 | 业务原因说明，最多 128 字符；可用 user_action/payment/session_expired；不是权限依据 |
| exchangeH5Session | boolean | 否 | true；false 仅原生登录，不返回 sessionToken/expiresIn |

成功时 data 包含 10.1 的全部字段；交换成功额外包含：

```json
{
  "status": "authenticated",
  "user": { "id": "10001", "maskedMobile": "188****4782" },
  "sessionMode": "short_token",
  "sessionStatus": "valid",
  "sessionExpiresIn": 299,
  "sessionToken": "server_issued_short_lived_token",
  "expiresIn": 300
}
```

expiresIn 为有效期秒数，范围 1～3600；300 秒为建议值，不是 SDK 写死的有效期。
H5 按服务端约定在业务请求中携带 `Authorization: Bearer <sessionToken>`。SDK 不会替 H5 自动加请求头。

登录 Promise 不设置普通 10 秒网络超时。验证码错误/登录网络失败留在同一个原生弹窗内提示并允许重试；用户最终关闭返回 USER_CANCELLED，成功仅完成一次。阅读协议及返回不算取消，不会结束这个 Promise。登录完成后的交换阶段有独立的 15 秒超时。

默认 exchangeH5Session=true 但未配置交换器时，在弹窗前返回 SESSION_EXCHANGE_NOT_CONFIGURED。交换失败时，已成功的原生登录不回滚，H5 可以稍后 refreshSession；只有明确 AUTH_REQUIRED 才表示需要重新登录。

### 10.3 `auth.refreshSession`

用途：使用原生本地长期 Token 换取新的短时 Token；绝不主动弹登录框。

```json
{
  "version": "1.0",
  "type": "request",
  "id": "auth-refresh-001",
  "method": "auth.refreshSession",
  "params": { "reason": "session_expired" }
}
```

params 可为空对象。可选 reason 的约束同 10.2。该方法总是交换，传 exchangeH5Session=false 不会跳过交换。成功 data 与 10.2 交换成功完全相同。

处理闭环：

1. 当前文档启动、H5 内存无 Token、即将过期，或业务接口明确报告短时凭证过期：调用 refreshSession。
2. H5 同一文档合并并发刷新为一个 Promise；其他业务请求等待该 Promise。
3. 成功：替换内存 Token。建议提前 min(30秒, expiresIn 的20%) 刷新，不能用固定提前60秒导致短有效期死循环。
4. AUTH_REQUIRED：清空 H5 内存 Token，给用户重新登录入口；通过 auth.login 完成原生登录与重新交换。
5. NETWORK_ERROR/TIMEOUT/RATE_LIMITED：提示或有限退避重试，不注销原生登录；过期 Token 不再用于业务接口。
6. AUTH_STATE_CHANGED：旧请求所属账号已变化，丢弃旧 Token，重新 getState，按新状态发起新操作。
7. 登录/刷新只解决鉴权，不代表原业务已成功。只允许安全、幂等请求最多重试一次；创建订单、支付、领取等操作必须查业务状态/使用服务端幂等键，不能盲目重放。

并发与生命周期：

- 原生每个文档至多一个登录/交换操作；不同请求 ID 并发返回 BUSY。
- 同一在途 ID 重发不重复执行；认证完成只保存最近 128 个 ID，不保存带 Token 的响应。重复已完成 ID 返回 DUPLICATE_REQUEST；重试必须用新 ID。
- Primary/Secondary 的短时凭证分别获取；共用 Overlay sessionId，但不共享 JS 全局变量。后端必须允许同一原生会话多个短时凭证同时有效，不能刷新右侧就撤销左侧。
- 原生交换在 IO 线程发起，15 秒截止；前端 refresh 等待 20 秒。重复/迟到回调丢弃。
- 导航、关闭来源 WebView、宿主销毁后取消订阅、清除状态，不把旧结果送给下一文档。
- Activity 原实例仅尺寸/方向变化不销毁文档，不主动刷新 Token；Activity 重建产生新文档重新初始化。
- 回后台不主动弹窗或重试；已发起交换可以完成，回前台重新检查剩余有效期。

### 10.4 错误码与登出边界

| code | 意义与处理 |
|---|---|
| AUTH_REQUIRED | 本地无长期凭证，或交换后端明确认定长期凭证失效；用户重新登录 |
| AUTH_STATE_CHANGED | 交换期间账号/登录世代变化；丢弃结果后重新查询 |
| SESSION_EXCHANGE_NOT_CONFIGURED | 未配置原生交换器，接入配置错误，不循环弹登录 |
| SESSION_EXCHANGE_FAILED | 交换器异常、服务端失败或未识别的错误码 |
| INVALID_SESSION_RESPONSE | Token 空/过长/等于长期 Token，或有效期不在 1～3600 秒 |
| NETWORK_ERROR / TIMEOUT / RATE_LIMITED | 网络失败 / 交换超时 / 限流；有限重试 |
| USER_CANCELLED | 用户关闭原生登录弹窗 |
| BUSY | 当前文档在执行认证，或已有其他原生登录窗口 |
| HOST_NOT_RESUMED / ACTIVITY_UNAVAILABLE | 宿主不适合发起操作；等待可用状态 |
| INVALID_ARGUMENT | 已知字段类型不正确或 reason 超长 |
| DUPLICATE_REQUEST | 最近已完成的认证 ID 被复用，改用新 ID |
| METHOD_NOT_SUPPORTED | 未实现的方法，包括当前 auth.logout |
| PAGE_UNLOADED | JS 辅助库在 pagehide 时拒绝未完成 Promise（前端本地错误） |

`auth.logout` 在旧稿为规划能力，目前**未接入**完整的原生确认、服务端登出及宿主回调流程，capability=false，返回 METHOD_NOT_SUPPORTED。不要通过简单清除 SP 模拟退出成功；既有原生退出逻辑保持不变。

### 10.5 事件 `auth.changed`

认证操作完成后，原生向仍存在的可信 Primary/Secondary 发送各自的状态快照，data 为 10.1 状态对象加上：
`reason: "auth_operation_completed"`。广播不含 sessionToken，不以事件代替请求响应。

当前实现不是全局账户监听器：游戏直接调用其他原生登录/退出入口时，不保证立刻发事件；H5 在进入/恢复页面、收到事件及业务报鉴权错误时主动 getState。短时 Token 自然到期也不承诺定时事件。原生的 save/clear 会推进登录世代，随后状态查询/交换结果校验会发现变化。

### 10.6 登录协议页

用户协议/隐私政策使用专用只读 Overlay WebView，不走首页 Primary/Secondary 业务路由、不依赖用户登录、没有 JS Bridge。

- 临时 hide 原登录 Dialog 并去除其蒙层，不 dismiss、不重新创建表单；账号、验证码、密码、勾选状态、倒计时和原登录回调都保留。
- 协议页只提供一个原生返回图标，复用项目现有切图 `fs_right_back`，不显示额外的文字返回或关闭按钮。返回优先回退协议页网页历史，无历史恢复原登录弹窗。
- 隐藏期间页面不被旧 Dialog 蒙层覆盖；返回后恢复登录窗口原有蒙层。不会打开首页或把 auth.login 当取消。
- 不新增 Activity，不主动触发游戏 onPause、不改宿主方向；协议页仅布局随宿主变化。
- 宿主销毁时释放协议 WebView 和原登录实例，不恢复旧 Activity 的 Dialog；系统真正重建 Activity 后不能保留旧 View 实例。
- AndroidX/系统返回分发做兼容处理；游戏完全接管返回键时仍应优先调用 FoxSdkOverlayManager.onHostBackPressed(activity)。原生可见按钮不依赖这项集成。
- 协议内容限定 HTTPS 同源，禁用 JS、文件/Content 访问，不开放下载或外部 Scheme；加载/证书失败也能随时返回。

### 10.7 H5 辅助库接入

将仓库 `foxsdk/src/main/assets/wishfox-auth-bridge.js` 同步到 H5 工程，业务 JS 之前加载（不是原生自动注入）。它提供：

```javascript
WishFoxSDK.auth.getState();                  // Promise<状态>
WishFoxSDK.auth.login({ exchangeH5Session: true }); // Promise<状态+短时Token>
WishFoxSDK.auth.refreshSession({ reason: 'bootstrap' }); // Promise<状态+短时Token>
var off = WishFoxSDK.auth.onChanged(function (event) { /* 使用 event.data */ });
off();                                     // 解除监听
```

辅助库内部为 login/refresh 分别合并在途 Promise，不跨 WebView 合并，不缓存 Token。包含响应/事件分发，可与 wishfox-media-bridge.js 组合，任一加载顺序均可；重复加载不重复安装。旧 WebView 必须先加载 Promise polyfill；下方 async/await 示例需要构建转译。若已有完整 Bridge，请统一其方法命名和超时语义，避免再覆盖这些认证方法。

## 11. Toast

### 11.1 `ui.toast`

请求：

```json
{
  "method": "ui.toast",
  "params": {
    "message": "保存成功",
    "duration": "short"
  }
}
```

参数：

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `message` | string | 是 | 去除首尾空格后1～200字符 |
| `duration` | string | 否 | `short` 或 `long`，默认 `short` |

响应：

```json
{
  "success": true,
  "data": {
    "shown": true
  }
}
```

限制：

- 同一 WebView高频调用应限流；
- 不支持任意布局、图片或富文本 Toast；
- 空文本返回 `INVALID_ARGUMENT`。

## 12. Overlay关闭

### 12.1 `ui.close`

请求：

```json
{
  "method": "ui.close",
  "params": {
    "reason": "user_click",
    "force": false
  }
}
```

参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `reason` | string | 否 | 关闭原因，用于诊断 |
| `force` | boolean | 否 | 默认 false；H5不能强制关闭原生支付弹窗 |

响应：

```json
{
  "success": true,
  "data": {
    "accepted": true
  }
}
```

以下情况原生可以拒绝或延后关闭：

- 支付确认弹窗正在显示；
- 登录弹窗正在显示；
- API 21～28系统文件保存选择器正在处理；
- Overlay处于 DESTROYING。

## 13. 导航与返回键

### 13.0 首页点击分类与默认路由

H5页面的点击结果必须归类为以下三种之一：

1. SDK能力：调用本协议中的 Bridge 方法；
2. 内部链接：调用 `navigation.openInternal`，或由原生 `WebViewClient` 拦截同源 `<a>` 链接；
3. 外部链接：调用 `navigation.openExternal`，或由原生拦截非白名单 URL。

内部链接不得直接调用 `window.open` 绕过路由。H5项目也不应把整个项目或普通业务链接配置为 `target="_blank"`；WebView不会把浏览器“新标签页”作为 SDK窗口模型。原生会根据真实方向决定窗口：横屏在右侧创建或复用 Secondary WebView；竖屏创建或复用等宽 stacked WebView覆盖首页。H5只依赖返回的 `actualMode` 和 `layout.changed`，不自行假定窗口位置。

推荐封装统一链接组件：普通内部链接调用 `navigation.openInternal`，外部链接调用 `navigation.openExternal`。Android会同时拦截 WebView中的同源 `<a>` 导航，因此历史页面即使没有接入组件，也不会意外创建浏览器新窗口；非白名单链接不会被当成内部页面加载。

### 13.0.1 `navigation.openInternal`

用途：在 SDK H5窗口内打开业务链接。

请求：

```json
{
  "method": "navigation.openInternal",
  "params": {
    "url": "/message/detail/123",
    "target": "auto",
    "title": "消息详情",
    "replace": false
  }
}
```

参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `url` | string | 是 | 同源相对路径或已登记的 HTTPS URL，最大1024字符 |
| `target` | string | 否 | 仅允许 `auto`；由原生按方向选择窗口 |
| `title` | string | 否 | 页面标题，最大100字符 |
| `replace` | boolean | 否 | 是否替换目标 WebView当前 history项，默认 false |

成功响应：

```json
{
  "success": true,
  "data": {
    "requestedUrl": "/message/detail/123",
    "actualUrl": "https://h5.example.com/message/detail/123",
    "webViewId": "secondary",
    "layoutMode": "split",
    "created": true
  }
}
```

`created` 为 false 时表示复用了已有 Secondary。竖屏时 `layoutMode` 返回 `stacked`；如果 URL不在白名单、协议不是 HTTPS或解析失败，返回 `INVALID_URL`/`ORIGIN_NOT_ALLOWED`，不得降级为外部浏览器。

### 13.0.2 `navigation.openExternal`

用途：请求系统浏览器打开外部页面。

```json
{
  "method": "navigation.openExternal",
  "params": {
    "url": "https://www.example.com/activity/1",
    "requestId": "external-001",
    "returnToOverlay": true
  }
}
```

原生只允许 `http`/`https` 外链，使用 `Intent.ACTION_VIEW`；`weixin://` 只能走 `miniProgram.openScheme`，支付 scheme只能由支付模块处理。调用成功仅表示系统已接受 Intent，返回 App后必须通过 `lifecycle.changed(host.resumed)`恢复 Overlay并由业务重新查询状态。

### 13.1 `navigation.updateState`

H5在 route变化后调用：

```json
{
  "method": "navigation.updateState",
  "params": {
    "route": "/message/detail/123",
    "title": "消息详情",
    "canGoBack": true,
    "hasUnsavedChanges": false
  }
}
```

参数：

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `route` | string | 是 | 必须是相对业务路由，最大512字符 |
| `title` | string | 否 | 最大100字符 |
| `canGoBack` | boolean | 是 | H5当前是否能处理返回 |
| `hasUnsavedChanges` | boolean | 否 | 是否有未保存编辑状态 |

原生将 route保存到轻量 snapshot，用于 Activity重建恢复。

### 13.2 事件 `navigation.backRequested`

```json
{
  "event": "navigation.backRequested",
  "webViewId": "secondary",
  "data": {
    "backRequestId": "back-001",
    "source": "system_back",
    "deadlineMs": 300
  }
}
```

`source`：

- `system_back`
- `gesture_back`
- `native_button`
- `outside_click`

H5收到后应在 deadline内调用 `navigation.resolveBack`。

### 13.3 `navigation.resolveBack`

```json
{
  "method": "navigation.resolveBack",
  "params": {
    "backRequestId": "back-001",
    "handled": true,
    "currentRoute": "/message"
  }
}
```

参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `backRequestId` | string | 是 | 对应事件 ID |
| `handled` | boolean | 是 | H5是否已经完成返回 |
| `currentRoute` | string | 否 | 返回后的 route |

如果 H5不响应：

1. Secondary存在时关闭 Secondary；
2. 否则检查 WebView history；
3. 最后关闭 Overlay。

## 14. 主副 WebView布局

`layout.setMode` 是显式布局控制接口；普通内部链接建议使用 `navigation.openInternal`，由原生自动创建或复用 Secondary。两者最终都返回并广播同一个 `actualMode`。

### 14.1 `layout.setMode`

请求单栏：

```json
{
  "method": "layout.setMode",
  "params": {
    "mode": "single"
  }
}
```

请求双栏：

```json
{
  "method": "layout.setMode",
  "params": {
    "mode": "split",
    "secondary": {
      "route": "/message/detail/123",
      "initialState": {
        "messageId": "123"
      }
    }
  }
}
```

参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `mode` | string | 是 | `single`、`split`、`stacked` |
| `secondary` | object | split时是 | Secondary配置 |
| `secondary.route` | string | 是 | 相对路由，不接受任意完整 URL |
| `secondary.initialState` | object | 否 | 小型初始数据，建议小于16KB |

成功响应：

```json
{
  "success": true,
  "data": {
    "requestedMode": "split",
    "actualMode": "split",
    "primaryWebViewId": "primary",
    "secondaryWebViewId": "secondary"
  }
}
```

竖屏降级：

```json
{
  "success": true,
  "data": {
    "requestedMode": "split",
    "actualMode": "stacked",
    "reason": "portrait"
  }
}
```

### 14.2 `layout.closeSecondary`

请求参数：

```json
{
  "reason": "detail_closed"
}
```

成功后原生销毁 Secondary WebView，并回到 single模式。

### 14.3 事件 `layout.changed`

```json
{
  "event": "layout.changed",
  "data": {
    "previousMode": "split",
    "actualMode": "stacked",
    "reason": "orientation",
    "secondaryRoute": "/message/detail/123"
  }
}
```

H5必须监听该事件，不能只依赖 `layout.setMode` 的同步结果，因为屏幕旋转也可能改变布局。

### 14.4 事件 `layout.secondaryClosed`

```json
{
  "event": "layout.secondaryClosed",
  "data": {
    "route": "/message/detail/123",
    "reason": "back"
  }
}
```

## 15. 支付

### 15.1 `payment.start`

推荐请求：

```json
{
  "method": "payment.start",
  "params": {
    "paymentIntentId": "pi_202609160001",
    "idempotencyKey": "7fb3042c-1111-2222-3333-1e92220e3f20"
  }
}
```

参数：

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `paymentIntentId` | string | 是 | 服务端生成，最大128字符 |
| `idempotencyKey` | string | 是 | 全局唯一，最大64字符 |

SDK通过 paymentIntentId从服务端获取真实商品和金额，不信任 H5直接提供的金额。

接受请求：

```json
{
  "success": true,
  "data": {
    "paymentIntentId": "pi_202609160001",
    "status": "preparing"
  }
}
```

可能错误：

- `UNAUTHORIZED`
- `BUSY`
- `DUPLICATE_REQUEST`
- `PAYMENT_INTENT_INVALID`
- `PAYMENT_INTENT_EXPIRED`
- `NETWORK_ERROR`
- `ACTIVITY_UNAVAILABLE`

### 15.2 支付状态

枚举：

| 状态 | 说明 |
|---|---|
| `preparing` | 获取支付配置/支付意图 |
| `dialog_showing` | 原生支付弹窗已显示 |
| `order_created` | 支付订单已创建 |
| `pending` | 等待外部支付或服务端确认 |
| `success` | 支付成功 |
| `failed` | 支付失败 |
| `cancelled` | 用户取消 |
| `unknown` | 无法确认，需要后续查询 |

### 15.3 事件 `payment.statusChanged`

```json
{
  "event": "payment.statusChanged",
  "data": {
    "paymentIntentId": "pi_202609160001",
    "status": "pending",
    "orderId": "order_xxx",
    "payType": "wechat",
    "reasonCode": null,
    "message": "等待支付结果",
    "final": false
  }
}
```

`payType`：

- `fox_coin`
- `wechat`
- `alipay`
- `unknown`

`final=true` 的状态：

- `success`
- `failed`
- `cancelled`

`unknown` 是否最终状态由服务端策略决定，通常应允许后续查询。

### 15.4 `payment.query`

请求：

```json
{
  "method": "payment.query",
  "params": {
    "paymentIntentId": "pi_202609160001"
  }
}
```

响应：

```json
{
  "success": true,
  "data": {
    "paymentIntentId": "pi_202609160001",
    "status": "success",
    "orderId": "order_xxx",
    "final": true
  }
}
```

### 15.5 支付约束

- 一个 Overlay同一时间只允许一个前台支付流程；
- 重复 idempotencyKey返回原结果，不创建新订单；
- H5页面刷新不会取消 pending支付；
- 微信/支付宝返回 App不代表支付成功；
- 最终状态以服务端查询为准；
- H5收到 `host.resumed` 后可以调用 `payment.query`，但应避免高频轮询；
- 原生会保存未完成支付并在恢复后继续处理。

## 16. 微信小程序 Scheme

### 16.1 `miniProgram.openScheme`

请求：

```json
{
  "method": "miniProgram.openScheme",
  "params": {
    "requestId": "wx-001",
    "scheme": "weixin://dl/business/?t=server_generated_token"
  }
}
```

参数：

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `requestId` | string | 是 | 业务请求唯一 ID，最大64字符 |
| `scheme` | string | 是 | 必须为允许的 `weixin://` scheme，最大2048字符 |

建议：

- scheme由 WishFox服务端生成；
- scheme短期有效；
- 不在日志中完整打印 scheme；
- H5不得把任意外部 App scheme传给该接口。

成功响应：

```json
{
  "success": true,
  "data": {
    "requestId": "wx-001",
    "status": "accepted_to_launch"
  }
}
```

失败：

- `WECHAT_NOT_INSTALLED`
- `SCHEME_REJECTED`
- `ACTIVITY_NOT_FOUND`
- `ACTIVITY_UNAVAILABLE`
- `DUPLICATE_REQUEST`

### 16.2 事件 `miniProgram.launchResult`

```json
{
  "event": "miniProgram.launchResult",
  "data": {
    "requestId": "wx-001",
    "status": "accepted_to_launch"
  }
}
```

状态：

- `accepted_to_launch`
- `wechat_not_installed`
- `scheme_rejected`
- `launch_failed`

### 16.3 事件 `miniProgram.returned`

用户从微信返回宿主后发送：

```json
{
  "event": "miniProgram.returned",
  "data": {
    "requestId": "wx-001",
    "elapsedMs": 6230
  }
}
```

该事件不表示小程序业务成功。H5应向服务端查询业务结果。

## 17. 图片保存

### 17.1 `media.saveImage`

用途：

1. 下载或接收一张图片；
2. 保存到 SDK沙盒；
3. 根据 `saveToGallery` 决定是否写入系统相册；
4. 通过异步事件返回最终结果。

URL方式，推荐：

```json
{
  "method": "media.saveImage",
  "params": {
    "requestId": "media-001",
    "source": {
      "type": "https_url",
      "value": "https://static.wishfoxs.com/images/poster.png"
    },
    "fileName": "wishfox-poster.png",
    "mimeType": "image/png",
    "saveToGallery": true,
    "legacyGalleryMode": "system_picker"
  }
}
```

Data URL方式，仅在 capability允许时使用：

```json
{
  "method": "media.saveImage",
  "params": {
    "requestId": "media-002",
    "source": {
      "type": "data_url",
      "value": "data:image/png;base64,..."
    },
    "fileName": "poster.png",
    "mimeType": "image/png",
    "saveToGallery": false,
    "legacyGalleryMode": "none"
  }
}
```

参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `requestId` | string | 是 | 唯一 ID，最大64字符 |
| `source` | object | 是 | 图片来源 |
| `source.type` | string | 是 | `https_url` 或 `data_url` |
| `source.value` | string | 是 | URL或 data URL |
| `fileName` | string | 是 | 建议包含扩展名，最大128字符 |
| `mimeType` | string | 是 | 支持的图片 MIME |
| `saveToGallery` | boolean | 是 | 是否尝试写入系统相册 |
| `legacyGalleryMode` | string | 否 | API 21～28：`none` 或 `system_picker` |

接受响应：

```json
{
  "success": true,
  "data": {
    "requestId": "media-001",
    "status": "accepted"
  }
}
```

该响应仅表示任务已接受，不表示保存完成。

### 17.2 事件 `media.saveProgress`

可选进度事件：

```json
{
  "event": "media.saveProgress",
  "data": {
    "requestId": "media-001",
    "stage": "downloading",
    "bytesReceived": 1048576,
    "totalBytes": 2097152,
    "percent": 50
  }
}
```

`stage`：

- `downloading`
- `validating`
- `saving_sandbox`
- `saving_gallery`
- `waiting_user_selection`

如果服务端未提供 Content-Length，`totalBytes` 和 `percent` 可以为 null。

### 17.3 事件 `media.saveResult`

API 29+成功写系统相册：

```json
{
  "event": "media.saveResult",
  "data": {
    "requestId": "media-001",
    "success": true,
    "assetId": "asset_01J8ZXYZ",
    "sandboxStatus": "saved",
    "sandboxUri": "https://appassets.androidplatform.net/assets/wishfox/asset_01J8ZXYZ",
    "galleryStatus": "saved",
    "galleryUri": "content://media/external/images/media/123",
    "fileName": "wishfox-poster.png",
    "mimeType": "image/png",
    "sizeBytes": 204800,
    "sha256": "hex-string",
    "error": null
  }
}
```

API 21～28无权限：

```json
{
  "event": "media.saveResult",
  "data": {
    "requestId": "media-001",
    "success": true,
    "assetId": "asset_01J8ZXYZ",
    "sandboxStatus": "saved",
    "sandboxUri": "https://appassets.androidplatform.net/assets/wishfox/asset_01J8ZXYZ",
    "galleryStatus": "unsupported_no_permission",
    "galleryUri": null,
    "fileName": "wishfox-poster.png",
    "mimeType": "image/png",
    "sizeBytes": 204800,
    "sha256": "hex-string",
    "error": {
      "code": "UNSUPPORTED_NO_PERMISSION",
      "message": "当前系统版本无法在无存储权限模式下自动写入公共相册"
    }
  }
}
```

注意：上例整体 `success=true` 表示沙盒保存成功；必须单独检查 `galleryStatus`，不能把整体 success理解为相册也成功。

用户通过系统选择器保存：

```json
{
  "event": "media.saveResult",
  "data": {
    "requestId": "media-001",
    "success": true,
    "assetId": "asset_01J8ZXYZ",
    "sandboxStatus": "saved",
    "galleryStatus": "user_selected",
    "galleryUri": "content://com.android.providers.../document/..."
  }
}
```

`sandboxStatus`：

- `saved`
- `failed`
- `cancelled`

`galleryStatus`：

- `not_requested`
- `saved`
- `user_selected`
- `unsupported_no_permission`
- `cancelled`
- `failed`

### 17.4 `media.cancel`

请求：

```json
{
  "method": "media.cancel",
  "params": {
    "requestId": "media-001"
  }
}
```

响应：

```json
{
  "success": true,
  "data": {
    "requestId": "media-001",
    "cancelRequested": true
  }
}
```

限制：

- 下载阶段可以取消；
- 已经完成的 MediaStore写入不回滚；
- 系统文件选择器显示后无法保证通过 JS直接关闭；
- 用户关闭选择器后返回 `cancelled`。

### 17.5 `media.removeSandboxImage`

可选方法，用于删除 SDK私有沙盒中的业务图片：

```json
{
  "method": "media.removeSandboxImage",
  "params": {
    "assetId": "asset_01J8ZXYZ"
  }
}
```

该方法：

- 只能删除 WishFox SDK自己创建的 assetId；
- 不删除已经写入系统相册的图片；
- 不接受文件路径；
- assetId不存在时按幂等成功处理。

### 17.6 图片错误码

- `INVALID_IMAGE_URL`
- `IMAGE_HOST_NOT_ALLOWED`
- `UNSUPPORTED_IMAGE_TYPE`
- `FILE_TOO_LARGE`
- `DOWNLOAD_FAILED`
- `DOWNLOAD_TIMEOUT`
- `IMAGE_VALIDATION_FAILED`
- `SANDBOX_WRITE_FAILED`
- `GALLERY_WRITE_FAILED`
- `UNSUPPORTED_NO_PERMISSION`
- `USER_CANCELLED`
- `DUPLICATE_REQUEST`

## 18. 诊断

### 18.1 `diagnostics.report`

用于 H5主动报告白屏、不可恢复渲染错误或关键流程异常。

请求：

```json
{
  "method": "diagnostics.report",
  "params": {
    "level": "error",
    "code": "H5_RENDER_FAILED",
    "message": "首页根节点渲染失败",
    "route": "/home",
    "extra": {
      "component": "HomeRoot"
    }
  }
}
```

参数：

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `level` | string | 是 | `info`、`warning`、`error` |
| `code` | string | 是 | 最大64字符 |
| `message` | string | 是 | 最大500字符 |
| `route` | string | 否 | 最大512字符 |
| `extra` | object | 否 | 序列化后最大4KB |

禁止放入：

- token；
- Cookie；
- 手机号；
- 支付密文；
- 图片 Base64；
- 完整微信 scheme；
- 大段 HTML。

原生可以对诊断上报限流。

## 19. 通用错误码

| 错误码 | 说明 | H5建议处理 |
|---|---|---|
| `OK` | 成功 | 正常处理 |
| `INVALID_ARGUMENT` | 参数错误 | 修正调用，不重试 |
| `UNSUPPORTED_PROTOCOL_VERSION` | 协议不兼容 | 展示升级/降级页 |
| `NOT_SUPPORTED` | 当前 SDK不支持 | 隐藏或降级功能 |
| `NOT_READY` | Bridge或页面未 ready | 等待 ready后重试 |
| `BRIDGE_NOT_AVAILABLE` | Bridge不可用 | 展示错误页 |
| `ORIGIN_NOT_ALLOWED` | 当前页面来源不可信 | 停止调用并上报 |
| `UNAUTHORIZED` | 未登录或登录失效 | 触发登录 |
| `USER_CANCELLED` | 用户取消 | 恢复页面，不自动重试 |
| `BUSY` | 同类流程正在执行 | 禁止重复点击 |
| `DUPLICATE_REQUEST` | requestId重复 | 使用原请求结果 |
| `ACTIVITY_UNAVAILABLE` | 宿主 Activity不可用 | 页面恢复后重试 |
| `NETWORK_ERROR` | 网络失败 | 提示并允许重试 |
| `TIMEOUT` | 操作超时 | 查询状态后再决定重试 |
| `INTERNAL_ERROR` | 原生内部异常 | 提示并上报 |
| `WECHAT_NOT_INSTALLED` | 微信未安装 | Toast提示 |
| `SCHEME_REJECTED` | scheme未通过校验 | 不重试，检查服务端 |
| `FILE_TOO_LARGE` | 图片超过限制 | 压缩或换图 |
| `UNSUPPORTED_NO_PERMISSION` | 无权限模式不支持 | 保留沙盒结果或用系统选择器 |

错误对象建议：

```typescript
class WishFoxBridgeError extends Error {
  code: string;
  method: string;
  requestId: string;
  data?: unknown;
}
```

## 20. 超时策略

| 方法 | JS建议超时 |
|---|---:|
| `bridge.ready` | 5秒 |
| `bridge.getCapabilities` | 5秒 |
| `environment.get` | 5秒 |
| `ui.toast` | 3秒 |
| `navigation.updateState` | 3秒 |
| `navigation.openInternal` | 10秒 |
| `navigation.openExternal` | 10秒 |
| `layout.setMode` | 10秒 |
| `auth.getState` | 5秒 |
| `auth.login` | 不设置普通超时，由用户交互结束 |
| `auth.refreshSession` | 原生交换15秒，JS等待20秒 |
| `auth.logout` | 当前未实现 |
| `payment.start` | 仅等待 accepted，15秒 |
| `payment.query` | 15秒 |
| `miniProgram.openScheme` | 10秒 |
| `media.saveImage` | 仅等待 accepted，10秒 |
| `media.previewVideo` / `media.previewImage` | 仅等待 accepted，10秒；后续状态见第34章 |
| `media.closePreview` | 10秒 |
| 图片最终事件 | 按文件大小，不使用普通 Promise超时 |

`payment.start` 和 `media.saveImage` 的 Promise只表示任务是否被接受，最终状态必须监听事件。

## 21. 幂等与并发

### 21.1 requestId

- 每个 Bridge请求唯一；
- 同 session内不得复用；
- 原生短期缓存已处理 requestId；
- 重复 ID返回 `DUPLICATE_REQUEST` 或原结果；
- 页面刷新后仍可能收到 pending operation结果。

### 21.2 支付 idempotencyKey

- 同一业务订单只能使用一个 idempotencyKey；
- 网络超时后先 query，不能直接生成新 key重试；
- 原生和服务端都必须做幂等；
- 用户连点支付按钮只能进入一个原生支付流程。

### 21.3 媒体并发

- 同时最多两个图片任务；
- 同 requestId禁止重复；
- 同 URL是否复用由原生缓存策略决定；
- H5页面关闭不自动取消已经明确提交的相册保存任务；
- 最终结果保存在 session中，页面恢复后可补发。

## 22. H5 初始化示例

```javascript
async function startWishFoxPage() {
  try {
    const ready = await window.WishFoxSDK.ready({
      h5Version: '2026.09.16.1',
      supportedProtocolVersions: ['1.0'],
      route: window.location.pathname + window.location.hash,
      documentId: crypto.randomUUID()
    });

    configureByCapabilities(ready.capabilities);
    applyEnvironment(ready.environment);
    applyAuthState(ready.authState);

    if (ready.restoreState) {
      restoreRouteAndLayout(ready.restoreState);
    }
  } catch (error) {
    renderBridgeUnavailable(error);
  }
}
```

## 23. 登录与刷新调用示例

以下示例只保存内存变量；并发首次初始化也复用同一个 Promise。实际 HTTP 客户端收到**明确的短时凭证过期码**后，将 memoryToken 清空，再调用 ensureSession，不要把所有 401/403 都当作可刷新错误。

```javascript
let memoryToken = null;
let refreshAt = 0;
let initialization = null;

async function ensureSession() {
  if (memoryToken && performance.now() < refreshAt) return memoryToken;
  if (initialization) return initialization;
  initialization = (async () => {
    const state = await WishFoxSDK.auth.getState();
    const result = state.status === 'authenticated'
      ? await WishFoxSDK.auth.refreshSession({ reason: 'bootstrap_or_expired' })
      : await WishFoxSDK.auth.login({ reason: 'user_action', exchangeH5Session: true });
    const early = Math.min(30, result.expiresIn * 0.2);
    memoryToken = result.sessionToken;
    refreshAt = performance.now() + (result.expiresIn - early) * 1000;
    return memoryToken;
  })();
  try {
    return await initialization;
  } catch (error) {
    memoryToken = null;
    refreshAt = 0;
    // AUTH_REQUIRED：展示重新登录按钮，由用户触发 auth.login。
    // USER_CANCELLED：停留当前页面，不自动再次弹登录框。
    // 网络错误：提示重试，不无限递归。
    throw error;
  } finally {
    initialization = null;
  }
}

WishFoxSDK.auth.onChanged(function (event) {
  if (event.data.status === 'anonymous' || event.data.sessionStatus === 'none') {
    memoryToken = null;
    refreshAt = 0;
  }
});
```

短时 Token 对应原生登录世代；页面恢复时查询状态，账号变化时清理所有业务缓存。勿自动重放支付/订单等副作用请求。

## 24. 支付调用示例

```javascript
async function startPayment(paymentIntentId) {
  const idempotencyKey = crypto.randomUUID();

  const unsubscribe = window.WishFoxSDK.on(
    'payment.statusChanged',
    event => {
      if (event.data.paymentIntentId !== paymentIntentId) return;

      updatePaymentUI(event.data);
      if (event.data.final) {
        unsubscribe();
      }
    }
  );

  try {
    await window.WishFoxSDK.payment.start({
      paymentIntentId,
      idempotencyKey
    });
  } catch (error) {
    unsubscribe();
    throw error;
  }
}
```

## 25. 小程序调用示例

```javascript
async function openMiniProgram(sceneId) {
  const { scheme } = await requestSchemeFromServer(sceneId);
  const requestId = crypto.randomUUID();

  const result = await window.WishFoxSDK.miniProgram.openScheme({
    requestId,
    scheme
  });

  if (result.status === 'accepted_to_launch') {
    markSceneAsLaunched(requestId);
  }
}
```

H5必须把完整 scheme视为敏感数据，不记录到埋点和控制台。

## 26. 图片保存调用示例

```javascript
async function savePoster(url) {
  const requestId = crypto.randomUUID();

  const unsubscribe = window.WishFoxSDK.on('media.saveResult', event => {
    if (event.data.requestId !== requestId) return;
    unsubscribe();

    const result = event.data;
    if (result.sandboxStatus !== 'saved') {
      showError('图片保存失败');
      return;
    }

    if (result.galleryStatus === 'saved') {
      showSuccess('图片已保存到系统相册');
    } else if (result.galleryStatus === 'user_selected') {
      showSuccess('图片已保存到选择的位置');
    } else if (result.galleryStatus === 'unsupported_no_permission') {
      showInfo('图片已保存到应用内，当前系统无法无权限写入公共相册');
    } else {
      showInfo('图片已保存到应用内');
    }
  });

  try {
    await window.WishFoxSDK.media.saveImage({
      requestId,
      source: {
        type: 'https_url',
        value: url
      },
      fileName: `wishfox-${Date.now()}.png`,
      mimeType: 'image/png',
      saveToGallery: true,
      legacyGalleryMode: 'system_picker'
    });
  } catch (error) {
    unsubscribe();
    throw error;
  }
}
```

## 27. 页面路由和双栏示例

```javascript
async function openMessageDetail(messageId) {
  const env = await window.WishFoxSDK.environment.get();

  if (env.orientation === 'landscape') {
    const result = await window.WishFoxSDK.layout.setMode({
      mode: 'split',
      secondary: {
        route: `/message/detail/${encodeURIComponent(messageId)}`,
        initialState: { messageId }
      }
    });

    if (result.actualMode === 'split') return;
  }

  router.push(`/message/detail/${messageId}`);
  await window.WishFoxSDK.navigation.updateState({
    route: router.currentRoute.value.fullPath,
    title: '消息详情',
    canGoBack: true,
    hasUnsavedChanges: false
  });
}
```

## 28. H5 旋转恢复要求

H5必须做到：

1. Activity未重建时，收到 `environment.changed` 只调整布局，不刷新页面；
2. Activity重建后，根据 `bridge.ready.data.restoreState` 恢复 route；
3. 表单草稿放在 IndexedDB/localStorage或服务端草稿，不只放内存；
4. 支付、媒体保存和 scheme使用 requestId恢复，不因页面重载重复发起；
5. Secondary降级到 stacked时保留详情 route；
6. 不把 H5 reload当成退出支付；
7. 新 document ready后重新注册所有事件监听；
8. 页面销毁时取消自己的监听器，防止重复处理事件。

`restoreState` 示例：

```json
{
  "sessionId": "session-xxx",
  "reason": "activity_recreated",
  "primaryRoute": "/message",
  "secondaryRoute": "/message/detail/123",
  "layoutMode": "split",
  "secondaryVisible": true,
  "pendingPaymentIntentId": "pi_xxx",
  "pendingSchemeRequestId": null,
  "pendingMediaRequestId": "media-001"
}
```

## 29. 安全要求

H5必须遵守：

- 只部署到 SDK白名单 HTTPS域名；
- 设置严格 CSP；
- 禁止不可信 iframe；
- 禁止把 Bridge对象转交给第三方脚本；
- 不将 token、sessionToken、支付密文或完整 scheme写入日志；
- sessionToken只在内存中短期存在，不存入通用 Bridge 响应重放缓存；
- 不通过 URL query传长期 token；
- 不向 `media.saveImage` 传任意第三方 URL；
- 支付金额和订单状态以服务端为准；
- 所有 Bridge请求使用不可预测 requestId；
- 不在页面卸载时盲目重试支付和媒体保存；
- 对未知事件和未知字段保持向前兼容；
- 对未知方法错误执行功能降级。

推荐 CSP示例需结合实际 CDN调整：

```text
default-src 'self';
script-src 'self';
style-src 'self' 'unsafe-inline';
img-src 'self' https://static.wishfoxs.com data: blob: https://appassets.androidplatform.net;
connect-src 'self' https://api-game.wishfoxs.com;
frame-src 'none';
object-src 'none';
base-uri 'self';
```

## 30. 兼容和版本策略

### 30.1 版本号

- 当前协议：`1.0`；
- 新增可选字段不提升大版本；
- 删除字段、改变字段含义或改变方法语义必须提升大版本；
- H5可同时支持多个协议版本；
- 原生在 ready时选择双方都支持的最高版本。

### 30.2 Capability优先

不要这样判断：

```javascript
if (apiLevel >= 29) {
  // 假设一定能保存图片
}
```

应这样判断：

```javascript
if (capabilities.mediaGalleryDirect) {
  // 直接写相册
} else if (capabilities.mediaGalleryPicker) {
  // 使用系统选择器
} else {
  // 只保存沙盒
}
```

### 30.3 未知字段

- H5忽略 Response/Event中的未知字段；
- 原生忽略 Request params中的允许扩展字段；
- 不得因为增加字段导致整个请求失败；
- 关键字段类型错误仍返回 `INVALID_ARGUMENT`。

## 31. 联调检查表

### Bridge

- [ ] Primary ready成功；
- [ ] Secondary ready成功；
- [ ] 旧 WebView fallback成功；
- [ ] 非白名单 Origin被拒绝；
- [ ] 重复 requestId被拒绝；
- [ ] 页面销毁后迟到响应不再派发。

### 生命周期和旋转

- [ ] 横转竖不重载未重建 Activity中的 WebView；
- [ ] 竖转横正确恢复 Split；
- [ ] Activity重建后恢复 route；
- [ ] 支付期间旋转不重复创建订单；
- [ ] 图片下载期间旋转仍能收到最终结果；
- [ ] 外部微信返回后收到 resumed/returned事件。

### 登录和支付

- [ ] 未登录支付先登录；
- [ ] 用户取消登录返回 USER_CANCELLED；
- [ ] token不出现在 H5日志和 URL；
- [ ] 重复支付按钮只产生一个流程；
- [ ] 支付结果以服务端为准；
- [ ] 页面刷新后 payment.query可恢复。

### 图片

- [ ] API 29+无存储权限写系统相册；
- [ ] API 21～28返回 unsupported_no_permission；
- [ ] system_picker可选择目标；
- [ ] 用户取消选择器返回 cancelled；
- [ ] 沙盒保存成功与相册保存状态分开判断；
- [ ] 超大图片被拒绝；
- [ ] 非白名单图片 URL被拒绝。

## 32. 方法与事件总览

### H5 → Native 方法

```text
bridge.ready
bridge.getCapabilities
environment.get
auth.getState
auth.login
auth.refreshSession
auth.logout（规划，当前不支持）
ui.toast
ui.close
navigation.updateState
navigation.openInternal
navigation.openExternal
navigation.resolveBack
layout.setMode
layout.closeSecondary
payment.start
payment.query
miniProgram.openScheme
media.saveImage
media.previewVideo
media.previewImage
media.closePreview
media.cancel
media.removeSandboxImage
diagnostics.report
```

### Native → H5 事件

```text
environment.changed
lifecycle.changed
media.previewChanged
auth.changed
navigation.backRequested
layout.changed
layout.secondaryClosed
payment.statusChanged
miniProgram.launchResult
miniProgram.returned
media.saveProgress
media.saveResult
```

## 33. 最终约定

1. H5业务代码只调用 `window.WishFoxSDK`，不直接调用原始 Native对象；
2. 所有方法返回 Promise；
3. 长耗时操作的 Promise只表示任务接受，最终结果由事件返回；
4. H5根据 capabilities决定是否展示功能；
5. 原生不向 H5暴露长期 token；
6. 支付必须使用服务端 paymentIntentId和 idempotencyKey；
7. 微信小程序只接受受控 `weixin://` scheme；
8. 图片保存必须分别检查 sandboxStatus和 galleryStatus；
9. API 21～28无权限时不能声称已自动写入公共相册；
10. 横竖屏变化通过事件适配，不能依赖页面刷新；
11. Activity重建后使用 restoreState恢复；
12. 未知能力、未知方法和旧 SDK环境必须安全降级。

## 34. 商单原生媒体预览协议

### 34.1 能力、接入与发布边界

三个新增 Native方法：`media.previewVideo`、`media.previewImage`、`media.closePreview`；统一事件 `media.previewChanged`。它们不改变 H5主副窗口/URL/滚动位置，不调用 `navigation.openInternal`。SDK在最上层打开原生预览，关闭后回到原 H5；原生没有视频旋转入口，也不改变宿主方向。

当前代码的 `bridge.getCapabilities` 返回以下实际媒体能力；未出现的能力不能假定已实现：

```json
{
  "mediaPreviewImage": true,
  "mediaPreviewVideo": true,
  "mediaPreviewClose": true,
  "mediaPreviewMode": "streaming_native",
  "videoDiskCache": false,
  "maxPreviewImageBytes": 20971520
}
```

`mediaPreviewVideo`在未启用硬件加速的宿主为 false。能力表示入口可用，不保证所有视频编码都可播放。`mediaPreviewMode=streaming_native`表示视频 URL直接交给原生播放器，在线缓冲、达到可播条件即播放，不等待完整文件下载。`videoDiskCache=false`表示 SDK不建立视频磁盘缓存，不代表没有系统播放器内存缓冲或网络流量。已移除旧的 `maxPreviewVideoBytes`能力；不要把图片20 MiB限制用于视频，当前视频没有SDK累计流量/文件大小硬限额。

支持基线为 HTTPS MP4/H.264/AAC，建议 moov前置（faststart）并支持 Range/206；URL必须为最终受控 CDN直链。视频禁止跨域跳转，即使目标域名也在白名单内；其网络/跳转错误通常统一为 `MEDIA_PLAYBACK_FAILED`，不是图片下载器的逐跳校验错误。设备/文件不支持通过 error事件反馈；不假定 HLS/DASH/DRM/直播可用。弱网仍会发生缓冲，不能承诺零等待。

随 SDK提供 `foxsdk/src/main/assets/wishfox-media-bridge.js`。H5工程复制并自行打包该脚本，原生不会自动注入。脚本复用已经存在的 `WishFoxSDK.invoke/__dispatch`，没有完整 JS SDK时提供媒体专用 Promise传输。旧 WebView需业务方提供 Promise polyfill；不要求 `_blank`、`window.open`、浏览器视频全屏 API或新标签页。

当前 Native预览扩展已经实现局部 `bridge.ready`，仅返回 selectedProtocolVersion/sessionId/webViewId/bridgeMode/capabilities，其余全量协议字段待主 Bridge补齐。预览联调使用 `WishFoxSDK.media.getPreviewCapabilities()`；文档前面的全量登录/支付/保存协议是总体方案，不代表本次已经实现所有方法。

### 34.2 `media.previewVideo`

完整请求：

```json
{
  "version": "1.0",
  "type": "request",
  "id": "business-video-001",
  "method": "media.previewVideo",
  "timestamp": 1789600000000,
  "params": {
    "url": "https://media.example.com/orders/123/video.mp4",
    "autoPlay": true,
    "muted": false
  }
}
```

| 参数 | 类型 | 必填/默认 | 规则 |
|---|---|---|---|
| `url` | string | 必填 | HTTPS绝对直链≤4096字符；Origin必须在原生媒体白名单；可用短期签名参数 |
| `autoPlay` | boolean | 否/true | ready且宿主前台时自动开始；经历暂停/失焦后不会自动续播 |
| `muted` | boolean | 否/false | true为静音且不请求音频焦点；false需要获得焦点才播放 |

不接受由 H5指定屏幕方向、任意请求头/Cookie/token、外部文件路径、播放器类名；不提供裁剪/拉伸、自动循环或后台播放参数。未知字段忽略，已知字段类型错误返回 INVALID_ARGUMENT。

响应：

```json
{
  "version": "1.0",
  "type": "response",
  "id": "business-video-001",
  "method": "media.previewVideo",
  "success": true,
  "code": "OK",
  "message": "OK",
  "timestamp": 1789600000100,
  "data": {
    "accepted": true,
    "previewId": "generated-uuid",
    "type": "video",
    "fitMode": "contain",
    "orientation": "landscape"
  }
}
```

`previewId`由原生生成，只用于此预览；`orientation`是打开时快照，原生遵循宿主后续窗口变化；`fitMode=contain`意味着完整显示和必要黑边。accepted仅说明校验通过并创建了预览层，视频在线准备/缓冲/播放情况订阅事件获取；不新增下载任务ID或本地文件路径。

宿主暂停时原生记录位置并释放网络播放器，回前台需用户点击原生播放按钮，随后重连、准备并尝试定位；不保证暂停帧始终保留。错误层上的“重试”也由用户点击，沿用同一个previewId及原URL，不再次调用JS打开方法。若短期签名已过期，应先关闭预览，再从业务接口获取新URL，以新请求ID打开。既不自动完整下载兜底，也不无限自动重试。

### 34.3 `media.previewImage`

```json
{
  "version": "1.0",
  "type": "request",
  "id": "business-cover-001",
  "method": "media.previewImage",
  "timestamp": 1789600000000,
  "params": { "url": "https://images.example.com/orders/123/cover.jpg" }
}
```

`url`规则与视频一致，响应结构相同但 `type=image`。首期单图，参数没有 images/index；静态 JPEG/PNG/WebP为建议格式，动画图片仅保证位图预览。下载上限20 MiB，原生按最大2048×2048请求尺寸降采样，保证基础显示和1～4倍双指缩放（相对于 contain比例），不承诺原图像素级超清。单击或基础比例上下拖动可关闭，放大后单指平移，双指过程不误触关闭。无存储权限、不写相册；保存图片仍是独立 `media.saveImage`方案。

### 34.4 `media.closePreview`

```json
{
  "version": "1.0",
  "type": "request",
  "id": "preview-close-001",
  "method": "media.closePreview",
  "timestamp": 1789600000200,
  "params": { "previewId": "generated-uuid" }
}
```

只有创建者 WebView能关闭自己的 previewId。成功响应 `data={"accepted":true}`，随后发送 `closed`事件；已被用户关闭、ID不匹配、不同 WebView请求关闭，返回 `PREVIEW_NOT_FOUND`。它只关闭原生媒体，不关闭 Secondary和 H5 Overlay；H5业务页返回与 `ui.close`保持各自语义。

JS便利方法：`WishFoxSDK.media.closePreview(previewId)`。不要传 `{previewId}`给此便利方法；通用 invoke则传对象。

### 34.5 事件 `media.previewChanged`

```json
{
  "version": "1.0",
  "type": "event",
  "event": "media.previewChanged",
  "timestamp": 1789600000500,
  "webViewId": "secondary",
  "data": {
    "previewId": "generated-uuid",
    "state": "playing",
    "reason": "buffer_ready",
    "positionMs": 0
  }
}
```

| state | 说明 | 典型 reason |
|---|---|---|
| loading | 首次视频连接/图片下载，或用户手动重连视频 | open / reconnect |
| ready | 视频在线准备完成（不是完整下载完成，也不保证首帧）/图片解码完成 | prepared / loaded |
| buffering | 有播放意图但正在等待可播放数据；可反复发生 | starting / network / waiting_for_data |
| playing | 收到缓冲结束/渲染开始通知，或检测到播放位置推进 | buffer_ready / progress |
| paused | 视频暂停；图片无此状态 | user / host_paused / audio_focus_loss / audio_focus_denied / surface_lost |
| ended | 视频播放结束，仍显示预览，可重播 | completed |
| error | 图片下载或视频准备/缓冲/播放/解码失败；保留错误层，视频允许手动重试 | 下表 MEDIA_*错误码 |
| closed | 当前预览已终止，不能继续控制 | back_button / system_back / gesture / gesture_or_tap / js_close / host_stopped / host_destroyed / source_navigation / source_destroyed / detached / open_failed |

`positionMs`为状态发生时的最近播放位置（非高频进度通知），图片始终0，视频 completed后归零等待重播。error和ended都不是关闭；error后可因用户重试再次收到loading/ready；closed是此 previewId最后的生命周期状态。打开响应先入队，随后 loading/ready等事件；业务应先订阅再调用方法。

典型序列为 `loading → ready → buffering → playing → buffering → playing → ended → closed`；autoPlay=false时停在ready等待原生按钮，暂停不会产生“播放停滞”超时。缓冲百分比仅用于原生UI，当前事件不携带bufferPercent；不要把buffering作为错误或作为重新调用previewVideo的理由，也不要把playing当首帧精确计时/完播/奖励证明。

事件仅发给来源 document；来源导航/销毁会关闭预览，销毁后不保证 closed能送达，不向新文档补发旧事件。暂停发生在准备完成前时，H5可能先收到 paused再收到 ready，应以当前状态和宿主前台约束处理，不假设固定线性顺序。

### 34.6 错误与边界

| code/reason | 阶段 | H5处理 |
|---|---|---|
| INVALID_ARGUMENT | 请求 | 校正URL/boolean/参数对象类型 |
| MEDIA_URL_NOT_ALLOWED | 请求/图片下载 | 检查入口CDN白名单或图片重定向；不要自动外跳 |
| BUSY | 请求 | 已有预览，等待关闭，禁止循环重试 |
| HOST_NOT_RESUMED | 请求 | 页面不可见/宿主不可用，等待用户回前台再点击 |
| HARDWARE_ACCELERATION_REQUIRED | 请求/加载 | 展示不支持提示，不修改宿主窗口配置 |
| PREVIEW_NOT_FOUND | 关闭 | 已关闭或不属于来源，收敛本地状态 |
| PREVIEW_OPEN_FAILED | 请求 | 创建窗口失败 |
| MEDIA_TOO_LARGE | 图片下载 | 图片超过20 MiB；降低图片尺寸/大小 |
| MEDIA_DOWNLOAD_FAILED | 图片下载 | 网络、HTTP、签名或缓存写入失败，允许用户关闭后重新打开 |
| MEDIA_REDIRECT_FAILED | 图片下载 | Location缺失或超过3次重定向 |
| MEDIA_TIMEOUT | 图片下载/视频准备或恢复定位 | 图片读循环超过时限，或视频准备/恢复定位超过30秒；允许用户重试 |
| MEDIA_BUFFER_TIMEOUT | 视频缓冲 | 有播放意图且前台时，连续缓冲30秒未恢复；原生已释放播放器，可手动重试 |
| MEDIA_DECODE_FAILED | 图片 | 编码不支持/内容损坏 |
| MEDIA_PLAYBACK_FAILED | 视频 | 网络/HTTP/签名/跨域跳转/编码/容器/解码器/播放器错误；不保证系统提供可细分的HTTP错误 |

音频焦点拒绝通过 `paused/audio_focus_denied`报告，不误报视频成功播放，也不强行启动后台服务。

一次请求 envelope≤32K字符，ID非空且≤64字符。超大/不可解析消息或无有效ID时 Native丢弃，JS层按10秒超时处理。格式正确的媒体请求10秒只等待 accepted；图片下载另有120秒读循环截止、15秒连接/读取超时。视频准备30秒、重连恢复定位30秒、每段连续缓冲30秒分别计时；暂停/释放会清理缓冲计时器，正常持续播放没有“总时长120秒”限制。视频不经过图片下载器，不能套用图片网络超时参数。超时不得自动重复打开。

同一文档最近128个 request ID缓存响应，重复ID只重发缓存，不重新创建预览；已关闭的旧请求仍可能收到旧 accepted，应使用新ID表达用户再次打开。缓存随文档导航清除；H5不得依赖缓存永久幂等。本轮互斥范围是单个 H5 Overlay；宿主不应在多 resumed Activity同时打开预览。

### 34.7 商单调用示例

```javascript
// 先加载 wishfox-media-bridge.js；旧WebView预先加载Promise polyfill。
var activePreviewId = null;
var opening = false;
var dispose = WishFoxSDK.media.onPreviewChanged(function (event) {
  var value = event.data;
  if (activePreviewId && value.previewId !== activePreviewId) return;
  if (value.state === 'closed') activePreviewId = null;
  if (value.state === 'error') console.warn('预览失败', value.reason); // 不打印签名URL
});

function showOrderVideo(item) {
  if (opening || activePreviewId) return;
  opening = true;
  WishFoxSDK.media.previewVideo({url: item.videoUrl, autoPlay: true, muted: false})
    .then(function (result) { activePreviewId = result.previewId; opening = false; })
    .catch(function (error) { opening = false; console.warn(error.code); });
}

function showOrderCover(item) {
  if (opening || activePreviewId) return;
  opening = true;
  WishFoxSDK.media.previewImage({url: item.coverUrl})
    .then(function (result) { activePreviewId = result.previewId; opening = false; })
    .catch(function (error) { opening = false; console.warn(error.code); });
}
// 业务组件卸载：dispose(); SDK在来源document销毁时关闭仍存在的预览。
```

不在 visibilitychange时重复打开，不把 playing当作商单任务完成或奖励证明，业务完成状态仍由业务服务端判定。暂停/关闭不能被 H5当作支付或登录结果。

### 34.8 联调核对

先验证 capability和真实域名配置，再验证视频/图片成功及失败事件；横竖屏各测两种视频画幅、单击/拖动/多指/返回、进入后台和回前台、来源文档导航、重复调用BUSY、关闭后再开。流式播放额外验证：大文件未下完即出首帧、弱网buffering与恢复、30秒停滞超时、暂停不超时、手动重试同previewId、过期URL关闭后刷新、反复预览没有SDK视频文件积累。真机验证编解码/TLS/API35焦点行为与内存增长，单纯 Bridge mock测试不代表设备兼容性已通过。
