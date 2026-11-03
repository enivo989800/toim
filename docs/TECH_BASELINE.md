# 技术基线

本工程采用 Kotlin + Jetpack Compose 的单 Activity 入口与分层/分包结构。Android 官方将 Compose 作为推荐的原生 UI 工具包，并建议至少拆分 UI 与 Data 职责、用数据模型驱动界面、避免把业务状态堆在 Activity 中。

## 版本选型

| 组件 | 版本 |
|---|---:|
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.13 |
| Kotlin | 2.3.21 |
| Compose BOM | 2026.09.00 |
| Activity Compose | 1.13.0 |
| Navigation Compose | 2.10.1 |
| Compile / Target SDK | 36 |
| Minimum SDK | 26 |
| JVM target | 17 |

Kotlin 2.3.21 的官方兼容表覆盖 Gradle 8.13 与 AGP 8.13 系列。Compose BOM 用于统一 Compose 组件版本，Activity Compose 与 Navigation Compose 采用对应的 AndroidX 稳定版本。

## 官方参考

- Android Compose 快速入门：<https://developer.android.com/develop/ui/compose/setup>
- Android 应用架构指南：<https://developer.android.com/topic/architecture>
- Android Gradle Plugin 版本与兼容性：<https://developer.android.com/build/releases/about-agp>
- Kotlin Gradle 插件兼容矩阵：<https://kotlinlang.org/docs/gradle-configure-project.html>
- Compose BOM：<https://developer.android.com/develop/ui/compose/bom>
- AndroidX 稳定版发布信息：<https://developer.android.com/jetpack/androidx/versions/stable-channel>
- Gradle Wrapper 生成说明：<https://docs.gradle.org/current/userguide/gradle_wrapper.html>

## 设计原则

- Activity 只负责承载 UI。
- 导航和页面按职责拆分。
- 页面暂时只展示骨架，不直接访问媒体存储。
- 后续图片数据源通过独立仓储/数据层接入，避免页面直接依赖平台文件 API。
