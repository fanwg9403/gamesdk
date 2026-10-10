# H5 获取 SDK 用户 ID

H5 可通过认证 Bridge 主动读取 SDK 当前保存的 `FSUserInfo.userId`：

```ts
const result = await bridge.request({
  method: 'auth.getUserId',
  params: {},
});

const userId: string | null = result.userId;
```

成功时 Bridge 返回 `OK`，数据结构为 `{ "userId": "..." }`。当前未登录或 SDK 尚未加载到用户资料时，`userId` 为 `null`。该方法只返回用户 ID，不返回登录 Token。
