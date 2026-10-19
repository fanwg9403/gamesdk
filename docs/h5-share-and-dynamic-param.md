# H5 增量接入：图片分享与原生 dynamicParam

更新日期：2026-10-19。本清单与当前 SDK 的五个分享目标保持一致；完整图片分享协议及示例见 [H5 图片直接分享接入](h5-image-sharing.md)。

## 一、本次只增加两个业务方法

H5 已有完整 Bridge，继续使用现有请求、响应、事件机制。**无需复制或加载 SDK assets 中的 JS，不新增传输层，不替换原有 Bridge。**

必接方法：`media.shareImage` 和 `auth.getDynamicParam`。图片保存继续调用已对接的 `media.saveImage`；分享结果通过现有事件机制新增订阅 `media.shareResult`。`media.shareProgress`、`media.getShareCapabilities` 均为可选增强。

以下以已有的 `bridge.request({method,params})` 和 `bridge.on(event,handler)` 举例；事件订阅函数名如不同，使用 H5 工程已有的对应方法，不重新实现 Bridge。响应分发需保留 data/message/userNotified。

## 二、方法和事件清单

| 业务用途 | Native method | 返回值 |
| --- | --- | --- |
| 原生 userId（已对接） | `auth.getUserId` | `{userId: string或null}` |
| 原生动态参数（新增） | `auth.getDynamicParam` | `{dynamicParam: string或null}` |
| 图片分享（新增） | `media.shareImage` | `{requestId,status:'accepted'}` |
| 保存图片（沿用） | `media.saveImage` | `{requestId,status:'accepted'}` |
| 取消任务（沿用，扩展支持分享） | `media.cancel` | `{requestId,cancelRequested}` |
| 分享能力查询（可选） | `media.getShareCapabilities` | `{targets:{目标:能力}}` |

沿用原有 `media.saveProgress`、`media.saveResult` 监听。新增 `media.shareResult`，可选监听 `media.shareProgress`。下文按完整事件的 `event.data` 取业务数据，按实际前端事件封装调整。

`auth.getUserId` 已补充资料缺失时的加载逻辑：已登录时优先读取匹配当前账号的资料，否则请求现有用户信息接口（原生最多15秒，H5 请求超时建议至少20秒）。未登录返回 null；网络失败或接口缺少 ID 返回错误，不能静默当成 null。详见 [获取原生用户 ID](h5-auth-get-user-id.md)。这与 `auth.getDynamicParam` 的本地配置读取互不依赖。

## 三、获取 dynamicParam

### 3.1 在现有业务 API 文件中增加封装

```javascript
function getNativeDynamicParam() {
  return bridge.request({ method: 'auth.getDynamicParam', params: {} });
}

const { dynamicParam } = await getNativeDynamicParam();

// 分包：string；母包：null。不要用 localStorage 的旧值兜底。
if (dynamicParam === null) {
  // 当前安装包没有动态参数，清空页面此前保存的动态参数状态。
} else {
  // dynamicParam 就是原始 JSON 字符串，需要透传时直接使用该字符串。
}
```

### 3.2 沿用现有获取 userId 的 Bridge

```typescript
const result = await bridge.request({
  method: 'auth.getDynamicParam',
  params: {},
});
const dynamicParam: string | null = result.dynamicParam;
```

`bridge.request` 指项目已有的传输封装，需返回响应 data 并为每次请求生成唯一消息 id；不要将同一个固定 id 用于重复读取。

### 3.3 返回示例

分包的响应 data（JSON 协议中必须对字符串内部引号转义）：

```json
{
  "dynamicParam": "{\"gameChannel\":\"1\",\"gameId\":\"1\",\"packChannel\":\"4\",\"packName\":\"团长1\",\"shareId\":\"6\"}"
}
```

母包响应 data：

```json
{"dynamicParam":null}
```

实现直接读取 `WishFoxSdk.getConfig().getDynamicParam()`，不重新解析 APK、不从本地持久化缓存取历史值。原始串中的空白、字段顺序、Unicode 转义等内容都不在 Bridge 中改写。示例并不定义业务字段，后端以后增减字段无需因此修改 Bridge。

### 3.4 页面状态与错误处理

```javascript
// 动态参数只放在当前页面内存。读取失败与成功返回 null 是不同状态。
const nativeParams = { status: 'idle', dynamicParam: null };

async function loadNativeDynamicParam() {
  nativeParams.status = 'loading';
  nativeParams.dynamicParam = null; // 先移除旧值，避免失败时继续使用上次结果。
  try {
    const result = await getNativeDynamicParam();
    if (result.dynamicParam !== null && typeof result.dynamicParam !== 'string') {
      throw new Error('动态参数响应格式异常');
    }
    nativeParams.dynamicParam = result.dynamicParam;
    nativeParams.status = 'ready';
    return result.dynamicParam;
  } catch (error) {
    nativeParams.status = 'failed';
    // 页面展示可重试状态；旧SDK的 METHOD_NOT_SUPPORTED 应提示当前版本不支持。
    throw error;
  }
}
```

在可信业务页面完成 Bridge ready 且处于前台后调用，不需要先登录。离开页面时忽略在途回调；SPA 切换使用页面自己的生命周期标记。若业务需要拆字段展示，可自行 JSON.parse，但不要用 JSON.parse 后 stringify 的结果替换要原样透传的字符串。

`userId` 为当前登录人，dynamicParam 内的 `shareId` 等字段来自安装包，二者用途不同。H5不能自行把当前登录人覆盖写入安装包动态参数。

## 四、图片分享需要增加的业务代码

### 4.1 按钮到 target 的映射

仅展示以下五个入口。QQ空间能力已移除，H5 须删除对应按钮、`qzone` 映射、旧调用及“升级 QQ 后使用”的提示。当前 SDK 不返回该能力，继续传 `qzone` 会返回 `INVALID_ARGUMENT`。

```javascript
const shareTargets = {
  微信好友: 'wechat_friend',
  微信朋友圈: 'wechat_timeline',
  QQ好友: 'qq_friend',
  抖音: 'douyin',
  小红书: 'xiaohongshu'
};
```

SDK 使用 `ACTION_SEND` + 显式 `ComponentName` 传递图片，不弹系统分享选择器。H5 只传 target 和 assetId，不拼接 Scheme、不传目标组件名、不调用平台开放 SDK。微信/QQ 的选择好友、朋友圈/抖音/小红书的编辑与发布确认仍在目标 App 内由用户完成。

### 4.2 每次点击的处理顺序

1. 设置分享按钮 busy，忽略重复点击。
2. 可选调用 `media.getShareCapabilities()`；target 缺失时隐藏入口，`available:false` 时展示对应 message。查询失败不等于未安装。查询结果不长期缓存，实际分享仍可能失败。
3. 若没有可复用 assetId，先监听 `media.saveResult`，再调用 `media.saveImage`。
4. 等待 `sandboxStatus === 'saved'`，取 `assetId`；保存失败立即结束，不能继续分享。
5. 先监听 `media.shareResult`，再调用 `media.shareImage`；Android 10+ 原生先写入相册再唤端，Android 9 及以下静默跳过无权限的相册步骤，继续通过沙盒图片 URI 分享。无需申请权限，也不弹相册提示。
6. 收到终态或错误时取消监听、解除 busy；超时主动 `media.cancel`。
7. 页面卸载/SPA 路由离开时取消在途任务、移除订阅，不在旧页面继续唤端。

保存参数：

```javascript
await bridge.request({ method: 'media.saveImage', params: {
  requestId: saveRequestId, // 保存与分享必须用不同的唯一ID
  source: { type: 'https_url', value: posterUrl },
  fileName: posterFileName, // 例如 poster.jpg
  mimeType: posterMimeType, // JPEG 为 image/jpeg；PNG 为 image/png；WebP 为 image/webp
  saveToGallery: false, // 此步骤仅下载；media.shareImage 内部统一完成相册保存
  legacyGalleryMode: 'none'
}});
// 上面的 await 只等待 accepted，assetId 必须从后续 media.saveResult 事件获取。
```

拿到保存结果后的分享参数：

```javascript
await bridge.request({ method: 'media.shareImage', params: {
  requestId: shareRequestId,
  target: 'wechat_friend',
  assetId: savedAssetId
}});
// 同样只等待 accepted，后续监听 media.shareResult。
```

包含事件等待、超时取消、错误提示和 finally 恢复按钮的完整示例见 [图片直接分享文档第6节](h5-image-sharing.md#6-可直接改造的-h5-示例)。按页面实例集成该节代码，补充页面自己的 `showError` 与 `setShareButtonBusy`，并在组件卸载/路由离开时调用 `disposePosterSharing()`。调用示例：`sharePoster('wechat_friend', posterUrl, 'image/jpeg', 'poster.jpg')`。

图片地址须能被手机原生网络访问，不能依赖 H5 Cookie 或额外浏览器请求头。MIME 须与图片内容一致，不能把 JPEG 固定填为 PNG。`media.shareImage` 内部复用保存逻辑，按系统版本处理相册后唤端，不需要 H5 额外再保存一次。独立“保存图片”按钮继续用 `saveToGallery:true`。必须升级原生 SDK 才能获得新行为。

分享进度新增 `saving_gallery`；结果新增 `galleryStatus` 和 `galleryUri`。以 `status:'launched'` 判断已发起唤端，不额外要求 `galleryStatus:'saved'`，也不代表发布成功。Android 10+ 自动写入 `Pictures/WishFox`；Android 9 及以下可返回 `galleryStatus:'unsupported_no_permission'`、`galleryUri:null`、`error:null` 并正常唤端，H5 不弹相册提示、不阻断流程。旧系统的沙盒图片不保证出现在相册中。

### 4.3 错误与结果展示

- Native拒绝请求：Promise reject；错误信息在 `error.message` / `error.data.message`。
- Native接受任务后失败：结果事件中的 `data.error`。
- `userNotified === true`：原生已显示友好提示，H5不重复 Toast；仍需恢复按钮。
- `userNotified === false`：页面前台时展示业务错误提示或重试入口。
- `launched`：仅表示发起目标入口；不要显示“分享成功”，不要因此发奖励。
- 分享失败保留“保存图片”按钮供用户主动使用，不自动弹系统选择器。
- 原生保存/分享已提供 loading；H5至少需禁用重复点击，不必再叠加第二个全屏 loading。
- `TARGET_DISABLED`：目标入口被禁用；`TARGET_UNSUPPORTED`：没有匹配的图片入口；`TARGET_ACCESS_DENIED`：入口不允许外部启动。展示 SDK 返回的 message，不统一提示用户升级应用。
- `IMAGE_NOT_FOUND`：清空失效 assetId，用户重试时重新保存图片。`DOWNLOAD_TIMEOUT`：当前原生下载超时，保留重试与更换图片的处理能力。
- 返回游戏不等于分享成功，也无法判断用户是否在目标 App 内取消发布。`publicationStatus` 固定为 `unknown`。

图片分享与读取 dynamicParam 是两个独立功能；不要为了分享图片而要求解析 dynamicParam。是否把动态参数用于 H5业务接口，由业务接口约定决定。

## 五、前端交付检查清单

- 在现有业务 API 封装里新增 `media.shareImage` 与 `auth.getDynamicParam`；无需同步任何 assets 脚本。
- 增加 `auth.getDynamicParam` 调用，使用 `string | null`，母包清空旧状态。
- 分享五个按钮绑定正确 target，移除旧 QQ空间入口，并接入保存→分享的异步事件链。
- 两个阶段使用不同唯一 requestId，先订阅再调用。
- 接受前拒绝、接受后失败、超时、取消均恢复 UI。
- 保留 `userNotified`、不重复提示、不把 launched 当作发布成功。
- 小红书可能进入图片/视频选择页，抖音也需兼容手动选图；检查 Android 10+ 相册写入、Android 9 及以下静默降级后继续唤端、无权限弹窗、无相册 Toast，以及目标应用的照片访问权限。
