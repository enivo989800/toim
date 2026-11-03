# ToImg — Android Studio 工程（第一阶段框架）

## 当前状态

这是第一阶段 Android 工程骨架：Kotlin + Jetpack Compose + Material 3 + Navigation Compose。已经建立启动入口、主题、基础组件以及首页、图片选择、图片工作区、结果、设置五个页面。

**当前版本不读取、写入、删除或移动设备中的真实图片。** 页面按钮只用于验证导航流程。

## 开发环境

- Android Studio：建议使用当前稳定版。
- JDK：17。
- Android SDK：API 36。
- `minSdk`：26。
- Android Gradle Plugin：8.13.2。
- Gradle：8.13。
- Kotlin：2.3.21。
- Compose BOM：2026.09.00。

版本来源以 Android Developers、Kotlin 官方发布页及 Gradle 官方兼容资料为准，详见 `docs/TECH_BASELINE.md`。

## 初始化 Gradle Wrapper（首次打开前）

本次生成环境没有安装 Gradle，也没有 Android SDK，且无法从容器网络下载官方 Wrapper JAR。因此交付包含 `gradle-wrapper.properties` 的版本声明，但未包含 Gradle 官方生成的 `gradlew`、`gradlew.bat` 和 `gradle-wrapper.jar`。首次打开前，需在安装了 Gradle 的本机终端进入工程根目录并生成：

```bash
gradle wrapper --gradle-version 8.13
```

生成后再在 Android Studio 选择 **Open** 打开 `ToImg-Android-Studio` 根目录（即包含 `settings.gradle.kts` 的目录），等待 Gradle Sync。首次同步需要网络下载插件和依赖，并确认安装了 Android API 36 SDK。Gradle 官方说明 Wrapper 文件由 `gradle :wrapper` 任务生成，不是独立下载包。

## 页面流程

`首页 → 选择图片 → 图片工作区 → 结果页 → 返回首页`

设置页可以从各页面右上角进入。当前所有页面都是框架占位页。

## 目录结构

```text
ToImg-Android-Studio/
├── app/
│   ├── src/main/java/com/toimg/app/
│   │   ├── core/ui/             # 主题与通用组件
│   │   ├── feature/             # 五个功能页面
│   │   ├── navigation/          # 路由与导航图
│   │   └── MainActivity.kt
│   └── src/androidTest/         # 首页启动 UI 测试
├── gradle/                      # 版本目录与 Wrapper 配置
├── docs/                        # 技术基线和范围说明
├── plan.md
└── README.md
```

## 现阶段未实现

- 系统照片选择器 / 媒体访问。
- 图片预览、缩略图加载。
- 图片移动、复制、删除或写入。
- 权限申请、异常回滚和持久化。
- 最终产品 UI 细节。

这些内容留待按原始 `app.md` 的后续阶段逐项实现。

完整操作步骤见 `docs/USER_GUIDE.md`。
