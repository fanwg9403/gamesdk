# H5 获取 SDK 用户 ID

H5 可通过认证 Bridge 主动读取 SDK 的业务 `FSUserInfo.userId`。已登录但资料缺失时，SDK 会调用现有用户信息接口补取；不以登录 `openId` 或安装包中分享人的 ID 代替：

```ts
const result = await bridge.request({
  method: 'auth.getUserId',
  params: {},
});

const userId: string | null = result.userId;
```

成功时 Bridge 返回 `OK`，数据结构为 `{ "userId": "..." }`。当前未登录时返回 `{ "userId": null }`；已登录且有可确认属于当前账号的资料时立即返回，否则等待用户信息接口。该方法只返回用户 ID，不返回登录 Token。

用户资料补取最多等待15秒；请将现有 H5 Bridge 对 `auth.getUserId` 的请求超时配置为20秒或更长（具体配置方式沿用项目现有封装，不新增协议参数）。同一原生页面、同一账号的并发补取会合并，页面关闭/换文档时取消；切换账号或环境后丢弃旧请求结果。

错误不能当作 `userId:null`：`NETWORK_ERROR` / `TIMEOUT` 表示请求失败，可由用户重试；`USER_INFO_UNAVAILABLE` 表示用户信息接口失败或没有有效的 `user_info.id`；`AUTH_REQUIRED` 表示凭证不可用；`AUTH_STATE_CHANGED` 表示等待期间账号或环境发生变化；`RATE_LIMITED` 表示稍后再试。读取不会自动弹登录框或触发 `auth.changed`。

排查日志：`FoxSdk[H5Auth]` 的 `getUserId` 显示数据来源 `anonymous` / `user_profile` / `fetch_user_profile` 及完成错误码；`FoxSdk[H5]` 的 `auth read received/replying` 显示方法、请求 ID、返回码和值是否存在，不输出 Token。

继续使用 H5 工程现有的 `bridge.request` 封装。只读调用不触发 `auth.changed`。

新增同类方法 `auth.getDynamicParam`，返回 `{ "dynamicParam": string | null }`，读取当前安装 APK 的原始动态 JSON 字符串。完整增量接入见 [图片分享与动态参数读取](h5-share-and-dynamic-param.md)。

`dynamicParam` 不依赖用户资料，也不补取网络数据。Android Studio 编译的母包无参数时，DEMO 和 H5 均返回空是正常行为；分包应与 DEMO 原始 JSON 展示一致。`FoxSdk[H5Auth]` 的 `getDynamicParam` 日志记录 `source=sdk_config`、是否存在及字符串长度，可与 `FoxSdk[DynamicParam]` 初始化读取结果对照。
