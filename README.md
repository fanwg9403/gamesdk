# WishFoxSdk 🦊
一个基于 MVI 架构模式，使用 Kotlin + Flow + Retrofit + OkHttp 构建的现代化 Android SDK 开发框架。

## 📖 简介
FoxSdk 提供了完整的网络请求、状态管理、UI基类和生命周期安全的功能封装，帮助开发者快速构建高质量的 Android 应用。

## 🏗️ 项目架构
### MVI 架构图
```test
View (Activity/Fragment) → Intent → ViewModel → Repository → Network
       ↑                      ↓
       ←── ViewState ←──←──←──┘
```

### 项目结构
```
foxsdk/
├── data/                           # 数据层
│   ├── network/                    # 网络相关
│   │   ├── interceptor/
│   │   │   ├── FoxSdkLoggingInterceptor.kt     # 网络请求日志打印格式化
│   │   │   └── FoxSdkHeaderInterceptor.kt      # 网络请求公共请求头设置
│   │   ├── FoxSdkRetrofitManager.kt            # 网络请求管理
│   │   ├── FoxSdkNetworkExecutor.kt            # 网络请求处理
│   │   └── FoxSdkApiService.kt                 # 网络请求接口地址
│   ├── repository/                 # 数据仓库
│   │   ├── FoxSdkBaseRepository.kt
│   │   └── FSUserRepository.kt
│   └── model/                      # 数据模型
│       ├── FoxSdkBaseResponse.kt
│       ├── network/
│       │   └── FoxSdkNetworkResult.kt
│       ├── paging/
│       │   ├── FoxSdkPageRequest.kt
│       │   └── FoxSdkPageResponse.kt
│       └── entity/
│           └── FSUserProfile.kt
├── domain/                         # 领域层
│   ├── usecase/
│   │   └── FSGetUserFollowsUseCase.kt
│   └── intent/
│       ├── FoxSdkViewIntent.kt
│       └── FSUserFollowsIntent.kt
├── ui/                             # UI层
│   ├── base/
│   │   ├── FoxSdkBaseMviViewModel.kt
│   │   ├── FoxSdkBaseMviActivity.kt
│   │   └── FoxSdkBaseMviFragment.kt
│   ├── viewstate/
│   │   ├── FoxSdkViewState.kt
│   │   ├── FoxSdkUiEffect.kt
│   │   └── FSUserFollowsViewState.kt
│   ├── view/
│   │   ├── activity/
│   │   │   └── FSUserFollowsActivity.kt
│   │   ├── adapter/
│   │   │   └── FSUserFollowsAdapter.kt
│   │   └── widgets/
│           └── FSLoadingDialog.kt
│   └── viewmodel/
│       └── FSUserFollowsViewModel.kt
├── di/                             # 依赖注入层
│   ├── FoxSdkRepositoryContainer.kt
│   └── FoxSdkViewModelFactory.kt
├── util/                           # 工具类
│   ├── FoxSdkSPUtils.kt
│   └── FoxSdkUtils.kt
└── core/                           # 核心配置
    ├── WishFoxSdk.kt
    └── FoxSdkConfig.kt
```

## 📋 命名规范
### 1. 文件命名规则
#### 公共基础类（前缀：FoxSdk）
- 网络框架类：`FoxSdkNetworkExecutor.kt`、`FoxSdkBaseRepository.kt`
- UI基类：`FoxSdkBaseMviActivity.kt`、`FoxSdkBaseMviFragment.kt`
- 核心工具类：`FoxSdkLogger.kt`、`FoxSdkExtensions.kt`

#### 业务相关类（前缀：FS）
- 数据模型：`FSUser.kt`、`FSUserProfile.kt`
- ViewModel：`FSUserListViewModel.kt`
- ViewState：`FSUserListViewState.kt`
- Intent：`FSUserListIntent.kt`
- Activity/Fragment：`FSUserListActivity.kt`、`FSUserProfileFragment.kt`
- Repository：`FSUserRepository.kt`

### 2. 资源文件命名规则
#### 布局文件（前缀：fs_）
    fs_activity_user_list.xml
    fs_fragment_user_profile.xml  
    fs_dialog_loading.xml
    fs_item_user.xml

#### 控件ID（前缀：fs_）
     <TextView
        android:id="@+id/fs_tv_user_name"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
        
    <ProgressBar
        android:id="@+id/fs_progress_bar"
         android:layout_width="wrap_content"
         android:layout_height="wrap_content" />

#### 图片资源（前缀：fs_）
    fs_icon_test_bg.webp

#### 颜色资源（前缀：fs_）
    <color name="fs_color_333333">#333333</color>
    <color name="fs_color_212121">#212121</color>

### 3. 代码中的命名规范
#### Repository 使用方法
```kotlin
class FSUserRepository : FoxSdkBaseRepository() {

    private val apiService: FoxSdkApiService by lazy {
        FoxSdkRetrofitManager.getApiService()
    }

    /**
     * 获取用户信息
     */
    suspend fun getUserProfile(): FoxSdkNetworkResult<FSUserProfile> {
        return executeCall {
            apiService.getUserProfile()
        }
    }

    /**
     * 获取用户信息（带缓存）
     */
    suspend fun getUserProfileWithCache(
        userId: String,
        forceRefresh: Boolean = false
    ): FoxSdkNetworkResult<FSUserProfile> {
        return executeCachedCall(
            cacheKey = "user_$userId",
            shouldRefresh = forceRefresh,
            cacheProvider = { key -> userCache[key] },
            networkCall = { apiService.getUserProfile() },
            cacheUpdater = { key, data -> userCache[key] = data }
        )
    }
}
```

## 🚀 快速开始
### 1. 初始化 SDK
```Kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = FoxSdkConfig(
            appId = "your_app_id",
            appKey = "your_app_key",
            baseUrl = "https://api.example.com",
            enableLog = BuildConfig.DEBUG,
            timeout = 30000L
        )
        
        FoxSdk.initialize(this, config)
    }
}
```

### 2. 创建 Activity
```Kotlin
class FSUserFollowsActivity : FoxSdkBaseMviActivity<FSUserFollowsViewState, FSUserFollowsIntent, FsActivityUserFollowsBinding>() {

    override val viewModel: FSUserFollowsViewModel by viewModels { FoxSdkViewModelFactory() }

    override fun createBinding(): FsActivityUserFollowsBinding {
        return FsActivityUserFollowsBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 初始化视图
        binding.fsRefresh.apply {
            setRefreshHeader(ClassicsHeader(this@FSUserFollowsActivity))
            setRefreshFooter(ClassicsFooter(this@FSUserFollowsActivity))
            setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
                override fun onRefresh(refreshLayout: RefreshLayout) {
                    if (!viewModel.viewState.value.isRefreshing &&
                        !viewModel.viewState.value.isLoadingMore &&
                        !viewModel.viewState.value.isLoading
                    ) {
                        dispatch(FSUserFollowsIntent.Refresh(USER_ID))
                    }
                }

                override fun onLoadMore(refreshLayout: RefreshLayout) {
                    if (!viewModel.viewState.value.isRefreshing &&
                        !viewModel.viewState.value.isLoadingMore &&
                        !viewModel.viewState.value.isLoading
                    ) {
                        dispatch(FSUserFollowsIntent.LoadMore(USER_ID))
                    }
                }
            })
        }
    }

    override fun renderState(state: FSUserFollowsViewState) {
        // 渲染状态
        if (!state.isRefreshing)
            binding.fsRefresh.finishRefresh()

        if (!state.isLoadingMore)
            binding.fsRefresh.finishLoadMore()

        if (state.users.isEmpty() && state.moreUsers.isEmpty()) {
            // 空布局
        } else if (state.users.isNotEmpty()) {
            userFollowsAdapter.submitList(state.users)
        } else {
            userFollowsAdapter.addAll(state.moreUsers)
        }

        if (!state.hasMore)
            binding.fsRefresh.finishLoadMoreWithNoMoreData()
    }
}
```

### 3. 创建 ViewModel
```Kotlin
class FSUserFollowsViewModel(
    private val userRepository: FSUserRepository
) : FoxSdkBaseMviViewModel<FSUserFollowsViewState, FSUserFollowsIntent, FoxSdkUiEffect>() {

    private var currentPage = 1
    private var hasMoreData = true

    override fun initialState(): FSUserFollowsViewState = FSUserFollowsViewState()

    override suspend fun handleIntent(intent: FSUserFollowsIntent) {
        when (intent) {
            is FSUserFollowsIntent.LoadInitial -> loadUserFollows(intent.userId, true)
            is FSUserFollowsIntent.Refresh -> loadUserFollows(intent.userId)
            is FSUserFollowsIntent.LoadMore -> loadMoreUsers(intent.userId)
        }
    }

    private fun loadUserFollows(userId: String, isInitial: Boolean = false) {
        currentPage = 1
        hasMoreData = true

        executePageRequest(
            pageRequest = FoxSdkPageRequest(
                page = currentPage,
                pageSize = PageConstants.DEFAULT_PAGE_SIZE,
                isInitial = isInitial
            ),
            request = { page, size ->
                userRepository.getUserFollows(userId, page, size)
            },
            onSuccess = { data, page, hasMore, totalCount ->
                updateState {
                    copy(
                        users = data,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        hasMore = hasMore,
                        totalCount = totalCount
                    )
                }
                currentPage = page
                hasMoreData = hasMore
            },
            onError = { error, page, code ->
                updateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error
                    )
                }
                sendEffect(FoxSdkUiEffect.ShowToast(error))
            }
        )
    }
}
```

## ⚙️ 配置说明
### Gradle 依赖
```Kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.recyclerview)

    // 网络框架
    implementation(libs.bundles.network.utils)
    implementation(libs.gson)
}
```

### 限制
原则上，尽量不用第三方库，目前SDK中已集成常用库，若需要增加第三方库的使用，必须向组长申请。

### 混淆配置
项目已配置完整的混淆规则，确保以下内容不被混淆：
●所有 FoxSdk 和 FS 前缀的类
●MVI 架构相关类（ViewModel、ViewState、Intent）
●网络框架相关类
●数据模型类
●UI 基类和自定义 View

## 📦 发布到 Maven Local
### 1. 配置发布信息
在 SDK 模块的 `build.gradle.kts` 中添加：
```Kotlin
plugins {
    alias(libs.plugins.maven.publish)
}

android {
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.sohuglobal"
            artifactId = "foxsdk"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("FoxSdk")
                description.set("MVI + Kotlin + Flow Android SDK")
                url.set("https://www.sohuglobal.com")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("wishfoxteam")
                        name.set("Wish Fox Team")
                        email.set("fanweiguang@vcglobal.com.cn")
                    }
                }
            }
        }
    }
}
```

### 2. 执行发布命令
#### 方法一：通过 Gradle 面板
1.打开 Android Studio
2.在右侧 Gradle 面板中展开项目
3.导航至: `Tasks` → `publishing` → `publishToMavenLocal`
4.双击执行任务

#### 方法二：通过命令行
```bash
# Windows
./gradlew publishToMavenLocal

# macOS/Linux
./gradlew publishToMavenLocal
```

### 3. 验证发布结果
发布成功后，在本地 Maven 仓库中检查：
```bash
# 检查文件是否存在
ls ~/.m2/repository/com/fox/sdk/foxsdk/1.0.0/

# 应该看到以下文件：
# - foxsdk-1.0.0.aar
# - foxsdk-1.0.0-sources.jar
# - foxsdk-1.0.0-javadoc.jar
# - foxsdk-1.0.0.pom
```

### 4. 在使用方项目中引用
在项目的 `build.gradle.kts` 中添加：
```kotlin
repositories {
    mavenLocal()  // 本地仓库
    google()
    mavenCentral()
}

dependencies {
    implementation("com.fox.sdk:foxsdk:1.0.0")
}
```

## 🛠️ 开发注意事项
### 1. 严格的目录结构
●所有文件必须按照项目结构放置在指定目录
●不允许在错误的目录中创建文件
●新增模块时需先创建对应的目录结构

### 2. 命名规范检查
在提交代码前检查：
```bash
# 检查是否有不符合命名规范的文件
find . -name "*.kt" | grep -v "FoxSdk\|FS" | grep -v "Test.kt"

# 检查资源文件命名
find . -name "*.xml" | grep -v "fs_"
```

### 3. 混淆规则维护
●新增公共类时必须在 `proguard-rules.pro` 中添加保护规则
●业务类会自动被保护，无需额外配置
●定期检查混淆后的 APK，确保功能正常

### 4. 网络请求规范
```kotlin
// 使用封装的方法执行网络请求
executeNetworkRequest(
    request = { repository.fsGetData() },
    showLoading = true,
    onSuccess = { data -> /* 处理成功 */ },
    onError = { error, code -> /* 处理错误 */ }
)

// 分页请求
executePagedRequest(
    pageRequest = FoxSdkPageRequest(page = 1),
    request = { page, size -> repository.fsGetPagedData(page, size) },
    onSuccess = { data, page, hasMore, totalCount -> /* 处理分页数据 */ }
)
```

### 5. 生命周期安全
●所有 Flow 收集都使用 `repeatOnLifecycle(Lifecycle.State.STARTED)`
●Loading 对话框会自动在页面销毁时关闭
●ViewModel 中避免直接持有 View 引用

### 6. 架构说明：UseCase 层的选择
#### 当前架构决策：不包含 UseCase 层
经过实际项目验证，我们决定在当前的 MVI 架构中不包含 UseCase 层，原因如下：

#### 简化架构
```kotlin
// ✅ 当前架构（推荐）
View → ViewModel → Repository → Network

// ❌ 复杂架构（不采用）
View → ViewModel → UseCase → Repository → Network
```

#### 代码对比
```kotlin
// 当前方式：ViewModel 直接调用 Repository
class FSUserFollowsViewModel(
    private val userRepository: FSUserRepository
) : FoxSdkBaseMviViewModel<...>() {
    
    private suspend fun loadUsers() {
        executeNetworkRequest(
            request = { userRepository.fsGetUsers(1, 20) },
            onSuccess = { users -> /* 处理数据 */ }
        )
    }
}

// 对比：如果使用 UseCase（不采用）
class FSGetUserFollowsUseCase(
    private val userRepository: FSUserRepository
) {
    suspend operator fun invoke(): FoxSdkNetworkResult<List<FSUser>> {
        return userRepository.fsGetUsers(1, 20)
    }
}

class FSUserFollowsViewModel(
    private val getUserFollowsUseCase: FSGetUserFollowsUseCase
) : FoxSdkBaseMviViewModel<...>() {
    
    private suspend fun loadUsers() {
        executeNetworkRequest(
            request = { getUsersUseCase() },
            onSuccess = { users -> /* 处理数据 */ }
        )
    }
}
```

#### 决策依据
1.减少复杂度：UseCase 层在简单业务场景中是过度设计
2.维护性：减少了一层抽象，代码更易理解和维护
3.性能：减少函数调用层级
4.适用场景：当前业务逻辑主要在 ViewModel 中处理足够清晰

#### 何时考虑添加 UseCase
如果未来出现以下情况，可以考虑引入 UseCase：
●复杂的业务规则组合
●多个 Repository 的协调操作
●需要复用的复杂业务逻辑

### 7. 横竖屏适配切换
```kotlin
class MainActivity : FoxSdkBaseMviActivity() {

    // 可以重写基类方法为特定Activity设置不同方向
    override fun getScreenOrientation(): Int {
        return if (isSpecificCondition()) {
            FoxSdkConfig.ORIENTATION_LANDSCAPE
        } else {
            super.getScreenOrientation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 其他初始化代码
    }
}
```
如果需要在运行时动态切换布局：
```kotlin
class DynamicLayoutActivity : FoxSdkBaseMviActivity() {

    private var isLandscape = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateLayout()
    }

    private fun updateLayout() {
        val layoutRes = if (isLandscape) {
            R.layout.activity_main_land
        } else {
            R.layout.activity_main_port
        }
        setContentView(layoutRes)
    }

    fun toggleOrientation() {
        isLandscape = !isLandscape
        updateLayout()
    }
}
```
布局示例：
**竖屏布局** `res/layout/activity_main.xml`
**横屏布局** `res/layout-land/activity_main.xml`

### 8. 三方库版本管理
统一用 gradle -> `libs.versions.toml` 管理

## 🐛 常见问题
**Q: 如何添加新的 API 接口？**   
A:   
1.在 `FoxSdkApiService.kt` 中添加接口定义   
2.在对应的 Repository 中添加方法（FS 前缀）   
3.在 ViewModel 中通过 Intent 调用

**Q: 如何自定义屏幕方向？**   
A: 重写 Activity 的 `screenOrientationConfig` 属性

**Q: 网络请求日志不显示？**   
A: 检查 `FoxSdkConfig.enableLog` 是否为 true

**Q: 混淆后出现 ClassNotFoundException？**   
A: 检查 `proguard-rules.pro` 中是否添加了对应的 keep 规则

**Q: publishToMavenLocal 失败？**   
A: 检查步骤：   
1.确认在 SDK 模块中配置了 maven-publish 插件   
2.确认 groupId、artifactId、version 配置正确   
3.检查本地 Maven 仓库路径权限

## 📄 许可证
```VHDL
MIT License

Copyright (c) 2025 Wish Fox Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

## 🤝 贡献
欢迎提交 Issue 和 Pull Request！

## 📞 支持
如有问题，请联系：   
●Email: fanweiguang@vcglobal.com.cn   
●GitHub Issues: http://192.168.150.252:9980/wishfox/mobile/android/wishfoxsdk# 

------------------------------------------------------------------------------
版本: 1.0.0   
最后更新: 2025-10-13   
兼容性: Android 5.0+ (API 21+)