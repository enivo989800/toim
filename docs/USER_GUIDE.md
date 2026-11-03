# ToImg 第一阶段工程使用说明书

## 1. 解压与准备

1. 下载并解压 `ToImg-Android-Studio-0.1.0.zip`。
2. 确认本机已安装 JDK 17、Android Studio 和 Gradle 8.13（或兼容的更新版）。
3. 安装 Android SDK Platform 36。

## 2. 初始化 Gradle Wrapper

本次交付环境没有 Gradle 安装，因此未生成官方 Wrapper 的三个运行文件。第一次在本机使用前，需要在已安装 Gradle 8.13 或更高兼容版本的终端中进入工程根目录并执行：

```bash
gradle wrapper --gradle-version 8.13
```

执行后工程应出现：

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`

然后启动 Android Studio，选择 **Open**，打开解压后的 `ToImg-Android-Studio` 根目录（不要只选 `app` 子目录），等待 Gradle Sync。首次同步需要网络下载插件和依赖。

再使用以下命令构建：

```bash
./gradlew assembleDebug
./gradlew test
```

Windows：

```bat
gradlew.bat assembleDebug
gradlew.bat test
```

## 3. 运行应用

1. Android Studio 完成 Gradle Sync。
2. 打开 Device Manager，创建 Android 设备模拟器；或连接已开启 USB 调试的 Android 手机。
3. 在工具栏选择 `app` 运行配置和目标设备。
4. 点击 Run。

## 4. 当前可体验的页面

- 首页：点击“开始图片流程”。
- 选择图片页：点击“进入工作区预览”。
- 图片工作区：点击“预览完成页面”。
- 结果页：点击“返回首页”。
- 设置页：任意页面右上角点击“设置”。

这些按钮目前只验证页面导航，不会访问或移动手机里的真实图片。

## 5. 本阶段安全边界

当前版本没有申请媒体读取权限，也未接入系统照片选择器。请不要把这版当作可实际整理或移动图片的成品应用。

## 6. 问题定位

- Gradle Sync 失败：先确认 Wrapper 文件已生成、网络可访问 Google Maven 和 Maven Central、Gradle JDK 为 17。
- 找不到 Android SDK 36：通过 Android Studio 的 SDK Manager 安装 Android 16 / API 36 SDK Platform 与 Build Tools。
- 模拟器无法启动：检查 Device Manager 中的系统镜像是否已安装，或连接实体设备运行。
- 想核对开发阶段：阅读工程根目录 `plan.md` 和 `docs/REVIEW.md`。
