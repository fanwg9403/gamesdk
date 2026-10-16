# H5 图片直接分享接入

更新日期：2026-10-16。本文对应当前 Android SDK 实现，是图片分享的完整对接说明；两项新增功能的总清单见 [图片分享与原生 dynamicParam](h5-share-and-dynamic-param.md)。

## 本次同步要点

- 分享目标固定为微信好友、微信朋友圈、QQ好友、抖音、小红书，共五项。
- QQ空间能力已移除。H5 删除对应按钮、`qzone` 映射及调用分支，不再显示“升级 QQ 后使用”的提示。继续传 `target:'qzone'` 会被拒绝并返回 `INVALID_ARGUMENT`，不会唤起应用。
- 沿用现有 Bridge 与 `media.saveImage`；新增分享方法为 `media.shareImage`，不需要复制 `wishfox-auth-bridge.js` 或 `wishfox-media-bridge.js`。
- 图片分享采用 Android `ACTION_SEND` + 显式 `ComponentName`，图片通过临时授权的 `content://` URI 交接。H5 不拼接 Scheme，也不向目标应用传网络 URL。

## 1. 调用链路

`media.saveImage` 保存至 SDK 沙盒 → 等待 `media.saveResult` → 使用返回的 `assetId` 调用 `media.shareImage` → 等待 `media.shareResult`。

SDK 使用显式 Intent 指定组件，不弹系统分享选择器，不增加第三方开放 SDK，不要求新增后端接口。平台可能在自己的应用内要求登录、选择好友或确认发布。

保存图片的 `source.type` 沿用现有协议 **`https_url`**，不是 `url`。`saveToGallery:false` 即可分享，不需要相册权限；单独的保存按钮才传 `true`。

## 2. 沿用 H5 现有 Bridge

H5 已有完整 Bridge，本次只增加 `media.shareImage` 方法映射和分享结果监听，不复制 SDK assets 中的 JS，不重写请求/响应分发。`media.getShareCapabilities` 为可选预检，不是必接方法。

通过现有 `bridge.request({method,params})` 与事件订阅接入以下原生方法和事件：

| 方法 | 说明 |
| --- | --- |
| `media.saveImage(params)` | 保持原参数；Promise 返回 accepted，最终监听保存结果 |
| `media.shareImage({requestId,target,assetId})` | Promise 返回 accepted，最终监听分享结果 |
| `media.getShareCapabilities()` | 可选；在下载之前查询应用安装和入口声明情况 |
| `media.cancel(requestId)` | 取消下载/保存或尚未交接的分享；已唤起外部应用不能撤销 |
| `media.removeSandboxImage(assetId)` | 删除原沙盒图片；分享结果返回后再调用 |
| `media.saveProgress` / `media.saveResult` | 沿用已对接事件，业务数据在 `event.data` |
| `media.shareProgress` / `media.shareResult` | 新增分享事件；进度事件可选，结果事件必接 |

事件订阅、取消订阅使用 H5 现有封装；本文件以 `bridge.on(event,handler)` 返回取消订阅函数为例。

## 3. 分享目标

| target | 目标 | SDK 固定组件 |
| --- | --- | --- |
| `wechat_friend` | 微信好友 | `com.tencent.mm/com.tencent.mm.ui.tools.ShareImgUI` |
| `wechat_timeline` | 微信朋友圈 | `com.tencent.mm/com.tencent.mm.ui.tools.ShareToTimeLineUI` |
| `qq_friend` | QQ好友 | `com.tencent.mobileqq/com.tencent.mobileqq.activity.JumpActivity` |
| `douyin` | 抖音图片接收入口 | `com.ss.android.ugc.aweme/com.ss.android.ugc.aweme.share.ui.SystemShareNewActivity` |
| `xiaohongshu` | 小红书图片接收入口 | `com.xingin.xhs/com.xingin.xhs.routers.RouterPageActivity` |

组件表封装在原生内，H5不传包名或 Activity。目标组件必须已安装、启用、对外开放且声明支持相应图片的 `ACTION_SEND`；检查通过也不等于图片导入或发布成功。

测试反馈：用户已在 DEMO 中反馈以上五项分享均可用；H5 页面仍需联调自身的参数、事件订阅和生命周期。该结果仅覆盖当时测试环境，不代表所有客户端版本。SDK 每次分享仍检查组件启用状态、导出属性、权限及图片 Intent 声明。

## 4. 协议

请求：

```json
{
  "method": "media.shareImage",
  "params": {
    "requestId": "share-unique-001",
    "target": "wechat_friend",
    "assetId": "asset_0123456789abcdef0123456789abcdef"
  }
}
```

三个参数必须为字符串。requestId 最大64字符，每次新操作使用新 ID，保存与分享也使用不同 ID。assetId 只能使用保存结果返回值，不传 sandboxUri、图片 URL 或文件路径。

立即成功响应的 data：

```json
{"requestId":"share-unique-001","status":"accepted"}
```

进度事件 `media.shareProgress` 的 data：

```json
{"requestId":"share-unique-001","target":"wechat_friend","stage":"preparing"}
```

stage 为 `preparing` 或 `launching`。

结果事件 `media.shareResult` 的 data：

```json
{
  "requestId": "share-unique-001",
  "target": "wechat_friend",
  "status": "launched",
  "mode": "explicit_intent",
  "publicationStatus": "unknown",
  "error": null,
  "userNotified": false
}
```

status 为 `launched` / `failed` / `cancelled`。`launched` 只表示启动调用返回，不证明图片导入、发送或发布成功。不弹“分享成功”，不据此发奖励。

失败时 error 为 `{code,message}`。接受前失败通过 Promise reject 返回，不再产生结果事件；接受后失败通过结果事件返回。拒绝响应 data 包含 `message` 和 `userNotified`。

### 可选能力查询

调用 `bridge.request({method:'media.getShareCapabilities',params:{}})`，响应 data 为 `{targets:{...}}`，仅包含上表五个 target。每一项字段如下：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `appName` | string | 目标应用名称，例如“微信” |
| `available` | boolean | 查询时入口是否可用 |
| `mode` | string | 固定 `explicit_intent` |
| `code` | string | 可用为 `OK`，否则为具体错误码 |
| `message` | string | 可用为空串，否则为面向用户的提示 |
| `packageName` | string | 目标包名，仅供诊断，不作为 H5 启动参数 |
| `versionName` | string | 查询到的版本，无法获取时为 `unknown` |
| `publicationResultAvailable` | boolean | 固定 `false`，无法获取实际发布结果 |

```javascript
const { targets } = await bridge.request({
  method: 'media.getShareCapabilities', params: {}
});
const capability = targets[selectedTarget];
if (!capability) {
  // SDK 不支持此 target，隐藏该入口，不继续保存或分享。
} else if (!capability.available) {
  showError(capability.message); // 能力查询自身不弹原生提示。
} else {
  // 执行下文保存→分享流程。
}
```

`available` 是查询时按 `image/png` 检查的入口状态；分享时 SDK 会按图片实际 MIME 再次检查，因此预检通过后也必须处理失败。不要长期缓存能力结果。旧 SDK 若不支持查询方法，应按已有 Bridge 错误机制降级，不能把查询失败当成“未安装应用”。

## 5. 用户提示与重复点击

- SDK 在图片保存/分享准备期间显示原生处理提示，结束自动收起。
- 同一图片来源正在保存时拒绝重复保存；分享任务同一时间只处理一个，成功启动后有1.5秒防抖。
- 原生显示未安装、格式错误、网络失败、空间不足等友好提示，并通过 `userNotified` 告知 H5。
- H5仍应在按钮处理期间禁用按钮，并在 `finally` 恢复；重复调用使用同一 requestId 不会再次分享。
- `userNotified:true` 时 H5不再重复 Toast；`false` 时可在页面前台展示 error.message。
- 现有 Bridge 的 Promise reject 应保留响应 data，不能只保留错误码；若已保留无需改动，通过 `error.data.userNotified` 判断原生是否提示。
- 后台不弹提示、不突然拉起外部应用；尚未完成的分享在宿主暂停时结束。结果可能在 H5恢复后处理。
- 失败不会自动弹系统分享选择器；保留用户可主动点击的“保存图片”入口。

## 6. 可直接改造的 H5 示例

以下示例使用 H5已有的 `bridge.request` 和 `bridge.on`，按项目实际函数名调整。约定 request 成功返回响应 `data`、失败 reject 保留 `code` 和 `data`；on 返回取消订阅函数，回调参数为包含 `data` 的完整事件。低版本 WebView 由 H5构建转译并提供所需 polyfill；`showError`、`setShareButtonBusy` 由业务页面实现。以下状态应放在每个页面实例内，页面重新挂载时重新创建，不作为全局单例。

```javascript
const request = (method, params) => bridge.request({ method, params });
const on = (event, handler) => bridge.on(event, handler);
let shareBusy = false;
let pageActive = true;
const pendingTasks = new Map();
const allowedTargets = new Set([
  'wechat_friend', 'wechat_timeline', 'qq_friend', 'douyin', 'xiaohongshu'
]);

function cancelNativeTask(requestId) {
  Promise.resolve().then(() => request('media.cancel', { requestId })).catch(() => {});
}

function newRequestId(prefix) {
  return prefix + '-' + Date.now() + '-' + Math.random().toString(36).slice(2);
}

// 先订阅结果再调用，避免遗漏快速返回的事件。
function runMediaTask(subscribe, invoke, requestId, timeoutMs) {
  return new Promise((resolve, reject) => {
    if (!pageActive) {
      reject(new Error('页面已关闭'));
      return;
    }
    let done = false;
    let timer;
    let stop = () => {};
    function finish(error, result) {
      if (done) return;
      done = true;
      clearTimeout(timer);
      stop();
      pendingTasks.delete(requestId);
      if (error) reject(error); else resolve(result);
    }
    stop = subscribe(event => {
      if (event?.data?.requestId !== requestId) return;
      finish(null, event.data);
    });
    pendingTasks.set(requestId, () => {
      const error = new Error('页面已关闭');
      error.code = 'CANCELLED';
      finish(error);
      cancelNativeTask(requestId);
    });
    timer = setTimeout(() => {
      const error = new Error('处理时间较长，请稍后重试');
      error.code = 'TIMEOUT';
      finish(error);
      cancelNativeTask(requestId);
    }, timeoutMs);
    Promise.resolve().then(() => {
      if (!done && pageActive) return invoke();
    }).catch(error => {
      // Bridge 回应超时时请求可能仍在原生执行，主动取消防止随后意外唤端。
      if (error.code === 'TIMEOUT') cancelNativeTask(requestId);
      finish(error);
    });
  });
}

function requireResult(result, success) {
  if (success) return result;
  const error = new Error(result.error?.message || '操作未完成，请稍后重试');
  error.code = result.error?.code;
  error.userNotified = result.userNotified;
  throw error;
}

async function sharePoster(target, imageUrl, mimeType, fileName) {
  if (!pageActive || shareBusy) return;
  if (!allowedTargets.has(target)) {
    showError('不支持此分享目标');
    return;
  }
  shareBusy = true;
  setShareButtonBusy(true);
  try {
    const saveId = newRequestId('save');
    const saved = await runMediaTask(handler => on('media.saveResult', handler), () =>
      request('media.saveImage', {
        requestId: saveId,
        source: { type: 'https_url', value: imageUrl },
        fileName,
        mimeType, // image/png、image/jpeg 或 image/webp，必须与图片实际内容一致
        saveToGallery: false,
        legacyGalleryMode: 'none'
      }), saveId, 150000);
    requireResult(saved, saved.sandboxStatus === 'saved');
    if (!pageActive) return;

    const shareId = newRequestId('share');
    const shared = await runMediaTask(handler => on('media.shareResult', handler), () =>
      request('media.shareImage', { requestId: shareId, target, assetId: saved.assetId }),
      shareId, 45000);
    requireResult(shared, shared.status === 'launched');
    // 已发起目标入口，不显示“发布成功”。
    // 可缓存 saved.assetId 供同一海报再次分享；释放时再调用 removeSandboxImage。
  } catch (error) {
    if (pageActive && document.visibilityState !== 'hidden'
        && !error.userNotified && !error.data?.userNotified) {
      showError(error.data?.message || error.message || '操作未完成，请稍后重试');
    }
  } finally {
    shareBusy = false;
    if (pageActive) setShareButtonBusy(false);
  }
}

function disposePosterSharing() {
  pageActive = false;
  shareBusy = false;
  Array.from(pendingTasks.values()).forEach(cancel => cancel());
}

// 页面按钮示例，posterUrl 为业务现有图片地址：
// sharePoster('wechat_friend', posterUrl, 'image/jpeg', 'poster.jpg');
// SPA 路由离开/组件卸载：调用 disposePosterSharing()。
```

`disposePosterSharing()` 必须接入页面卸载/SPA 路由离开钩子，不要只依赖浏览器 unload。它会移除结果订阅、取消在途任务，并阻止保存结束后继续唤端。不要因普通的切到目标 App 就销毁仍存在的 H5 页面实例；已交接给目标应用的分享无法撤销，也没有用户取消发布的可靠回调。

网络图片由 Android 原生下载，不继承 H5 浏览器的 Cookie、登录请求头或代理设置。使用手机当前网络可访问的 HTTPS 图片地址；不要传 HTML 页面地址。`DOWNLOAD_TIMEOUT` 表示这次原生下载超时，不代表手机完全没有网络。PNG、JPEG、WebP 均按实际图片内容传 MIME，不要只根据 URL 后缀判断，也不要把所有图片固定填为 PNG。

相同图片可复用 `media.saveResult.assetId`；图片改变时重新保存。`IMAGE_NOT_FOUND` 时清空旧 assetId，由用户重试触发重新保存。若不再复用，分享结果返回后调用 `media.removeSandboxImage({assetId})`；不要在分享准备阶段删除原图。`saveToGallery:false` 只保存到 SDK 沙盒，用户看不到相册图片；手动发布的“保存图片”按钮须沿用已有 `saveToGallery:true` 流程及低版本相册处理。

## 7. 错误码

| code | 原生提示/处理 |
| --- | --- |
| `APP_NOT_INSTALLED` | 请先安装对应应用后使用分享功能 |
| `APP_DISABLED` | 请启用对应应用后重试 |
| `TARGET_DISABLED` | 外部图片分享入口已关闭，保存图片后在应用内手动发布 |
| `TARGET_UNSUPPORTED` | 未提供可用的图片分享入口，保存图片后手动发布 |
| `TARGET_ACCESS_DENIED` | 不允许外部应用打开此分享页面，保存图片后手动发布 |
| `DOWNLOAD_FAILED` / `DOWNLOAD_TIMEOUT` | 检查网络或稍后重试 |
| `SANDBOX_WRITE_FAILED` / `GALLERY_WRITE_FAILED` / `CACHE_FAILED` | 检查手机剩余空间后重试 |
| `IMAGE_NOT_FOUND` | 图片失效，重新加载后分享 |
| `INVALID_IMAGE_URL` | 刷新页面重新获取图片 |
| `IMAGE_VALIDATION_FAILED` / `UNSUPPORTED_IMAGE_TYPE` | 图片损坏或格式不支持 |
| `FILE_TOO_LARGE` | 图片过大 |
| `BUSY` / `DUPLICATE_REQUEST` | 请勿重复点击 |
| `PREPARE_TIMEOUT` | 图片处理超时，稍后重试 |
| `HOST_NOT_RESUMED` | 返回游戏页面重新操作 |
| `SHARE_CACHE_FULL` | 缓存已满，稍后重试或手动分享 |
| `IMAGE_URI_FAILED` / `LAUNCH_FAILED` | 稍后重试或保存图片后手动分享 |
| `CANCELLED` | 已取消操作 |
| `USER_CANCELLED` | 用户取消保存，恢复按钮 |
| `INVALID_ARGUMENT` | 参数缺失或分享目标不在五项白名单中，修正调用 |
| `UNSUPPORTED_NO_PERMISSION` | 当前系统不能直接写入相册，沿用原有系统文件保存流程 |

## 8. 原生文件与日志

专用 Provider authority 为 `${applicationId}.wishfox.share.fileprovider`，只开放 `cacheDir/wishfox_share/`。原生对已保存的图片创建独立副本并授予临时读取权限；不传 file URI，不把 WebView 的 sandboxUri 传给其他 App。

分享图片最大20MiB、最多6400万像素；副本在下次分享准备时清理超过24小时的文件，分享目录达到128MiB后明确拒绝，避免无限占用空间。缓存也可能被操作系统清理，不作为永久存储。页面关闭不立即删除已交接的副本。

日志标签 `FoxSdk[H5Share]`，记录目标、目标版本、组件、图片 MIME/字节数及结果码。不得把 `launched` 日志作为实际发送成功证据。

目标应用内的选好友、编辑和发送/发布由用户操作。H5 联调应覆盖五个目标、未安装应用、入口不可用、图片下载失败、重复点击、页面退出，以及原生提示去重。协议中 `publicationStatus` 始终为 `unknown`；不要把返回游戏、`launched` 或目标 App 关闭当作发布成功。
