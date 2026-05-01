# FocusFlow 构建与部署指南

> 在 VS Code + 命令行环境下从零搭建 Android 构建环境，编译并安装到真机。

---

## 环境信息

| 项目 | 值 |
|------|-----|
| 操作系统 | Windows 11 Pro |
| JDK | BellSoft Liberica JDK 17.0.19 |
| JDK 路径 | `C:\Program Files\BellSoft\LibericaJDK-17` |
| Android SDK 路径 | `C:\Users\ZJQ05\AppData\Local\Android\Sdk` |
| Gradle 版本 | 8.4（通过 wrapper 自动下载） |
| 编译 SDK | 36 |
| 测试设备 | AP4XUT4704000778 |

---

## 1. 安装 JDK 17

```bash
winget install BellSoft.LibericaJDK.17 --accept-source-agreements --accept-package-agreements
```

安装路径：`C:\Program Files\BellSoft\LibericaJDK-17`

---

## 2. 设置环境变量

每次打开新终端先执行（可写入 `~/.bashrc` 永久生效）：

```bash
export JAVA_HOME="C:/Program Files/BellSoft/LibericaJDK-17"
export ANDROID_HOME="C:/Users/ZJQ05/AppData/Local/Android/Sdk"
```

---

## 3. 工程适配修改

原始项目 `compileSdk = 34`，但本地 SDK 只有 API 36 平台。所做修改：

### 3.1 settings.gradle.kts

修复 Gradle API 名称错误：

```diff
- dependencyResolution {
+ dependencyResolutionManagement {
```

### 3.2 app/build.gradle.kts

适配本地 SDK 版本：

```diff
- compileSdk = 34
+ compileSdk = 36
- targetSdk = 34
+ targetSdk = 36
```

### 3.3 gradle.properties

抑制 compileSdk 版本警告：

```
android.suppressUnsupportedCompileSdk=36
```

### 3.4 gradle/wrapper/gradle-wrapper.properties

使用腾讯镜像加速 Gradle 下载：

```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.4-bin.zip
validateDistributionUrl=false
```

### 3.5 TimerViewModel.kt

修复 6 路 `combine` 类型推断失败（kotlinx-coroutines 1.7.x 限制）：

```kotlin
// 原代码：单次 combine 6 个 Flow → vararg 回退导致编译错误
// 修复：嵌套两次 Triple combine（3 + 3 = 2）
val uiState = combine(
    combine(tasks, _selectedTaskId, timerManager.timerState) { t, sid, ts -> Triple(t, sid, ts) },
    combine(timerManager.timerEvents, _mode, _targetDuration) { e, m, td -> Triple(e, m, td) }
) { (t, sid, ts), (e, m, td) -> ... }
```

---

## 4. 编译

```bash
cd d:/Professional_Programs/AI/FocusFlow
./gradlew assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`（~16MB）

---

## 5. 安装到手机

### 5.1 手机端准备

1. **设置 → 关于手机** → 连续点击「版本号」7 次 → 进入开发者模式
2. **设置 → 系统 → 开发者选项** → 开启「USB 调试」
3. 数据线连接电脑
4. 手机弹出"允许 USB 调试？"→ 勾选「始终允许」→ 确定

### 5.2 确认连接

```bash
"$ANDROID_HOME/platform-tools/adb" devices
# 输出：
# List of devices attached
# AP4XUT4704000778  device    ← 显示 device 表示已授权
```

### 5.3 安装

```bash
"$ANDROID_HOME/platform-tools/adb" install "d:/Professional_Programs/AI/FocusFlow/app/build/outputs/apk/debug/app-debug.apk"
# 输出：Performing Streamed Install → Success
```

### 5.4 启动应用

```bash
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.focusflow.app/.MainActivity
```

### 5.5 查看日志

```bash
"$ANDROID_HOME/platform-tools/adb" logcat -s FocusFlow
```

---

## 常用命令速查

| 操作 | 命令 |
|------|------|
| 编译 | `./gradlew assembleDebug` |
| 清理重编 | `./gradlew clean assembleDebug` |
| 安装到手机 | `adb install app/build/outputs/apk/debug/app-debug.apk` |
| 卸载 | `adb uninstall com.focusflow.app` |
| 启动应用 | `adb shell am start -n com.focusflow.app/.MainActivity` |
| 停止应用 | `adb shell am force-stop com.focusflow.app` |
| 查看连接设备 | `adb devices` |
| 截图 | `adb exec-out screencap -p > screenshot.png` |

---

## 编译警告说明

以下警告不影响构建，可忽略：

- `List: ImageVector is deprecated` — BottomNavBar 使用了旧版 List 图标，新版 Compose BOM 中需改用 `Icons.AutoMirrored.Filled.List`，但当前 BOM 版本尚不支持
- `This declaration needs opt-in` — StatsViewModel 中 `flatMapLatest` 需显式标记 `@ExperimentalCoroutinesApi`
