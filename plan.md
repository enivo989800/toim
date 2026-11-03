# ToImg 第一阶段工程初始化计划（F0 → F5）

> 重要说明：当前工作区没有检索到此前讨论所引用的 `app.md` 原文件。因此本计划先按“第一阶段只搭框架、不实现真实图片操作”的已知目标，建立 F0—F5 临时执行映射。它不是对原 `app.md` 具体条目的复述；拿到原文件后，需要逐项核对并同步差异。

## 目标

创建可由 Android Studio 识别的单模块 Android 工程，完成基础配置、应用入口、导航骨架、占位页面、架构边界和静态验证。第一阶段不接入真实图片读取、写入、移动或删除操作。

## F0 — 工程基线

- 工程名：ToImg。
- 包名：`com.toimg.app`。
- 应用版本：`0.1.0`。
- 语言：Kotlin；界面框架：Jetpack Compose。
- 建立目录结构、Gradle 配置、版本目录和 Android Manifest。

## F1 — 构建与平台配置

- 配置 Android Gradle Plugin、Gradle Wrapper 版本声明和 Kotlin 插件。
- 配置 `compileSdk = 36`、`targetSdk = 36`、`minSdk = 26`。
- 配置 Compose BOM、Material 3、Navigation Compose。
- 使用 JDK 17 作为 Java/Kotlin 编译目标。

## F2 — 应用启动骨架

- 建立 `MainActivity`。
- 配置 Edge-to-Edge。
- 建立统一的浅色/深色主题与基础 UI 组件。

## F3 — 页面与导航骨架

- 首页（Home）。
- 图片选择页（Picker）。
- 图片工作区（Workspace）。
- 结果页（Result）。
- 设置页（Settings）。
- 页面间完成基本导航及返回操作。

## F4 — 功能边界预留

- 按 `feature`、`core`、`domain`、`data` 的方向预留后续扩展边界。
- 所有真实图片能力暂留空，不提前锁定文件移动、画布拖动或其他细节的业务实现。
- 本阶段不声明媒体读取权限，不触碰用户图片文件。

## F5 — 检查、记录与交付

- 检查工程目录、Gradle 配置、Manifest XML、页面路由和测试文件。
- 尝试构建；若当前执行环境缺少 Android SDK/Gradle，则记录为环境阻塞，不伪报构建通过。
- 编写 README 与使用说明。
- 打包为 `ToImg-Android-Studio-0.1.0.zip` 并验证压缩包完整性。

## 验收标准

- Android Studio 工程结构完整。
- 应用入口、主题和页面路由文件齐全。
- 五个页面均可通过导航骨架到达。
- UI 占位文案明确说明目前不会读取或移动真实图片。
- 工程检查脚本通过；Android 真机/模拟器构建结果需在具备 Android SDK 的开发机上最终确认。

## 执行结果（本轮）

- F0：完成。
- F1：构建配置已写入；官方 Wrapper 二进制暂缺，需在本机生成。
- F2：完成。
- F3：完成。
- F4：完成框架边界预留；真实图片能力未实现（符合第一阶段不深入功能细节的目标）。
- F5：静态检查和 XML 检查通过；Android 构建未运行，原因记录在 `docs/REVIEW.md`。

最终复核见 `docs/REVIEW.md`。
