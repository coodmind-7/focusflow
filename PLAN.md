# FocusFlow Android 项目实施计划

## Context
基于 ARCHITECTURE.md 从零创建 FocusFlow Android 应用——一款完全本地化、离线运行的任务计时与时间分配统计工具。采用 Clean Architecture + MVVM 分层，Jetpack Compose UI。

## 核心约束
- **完全本地化**：无网络请求，无云服务，纯离线运行
- 所有数据存储在 Room + DataStore 本地
- Min SDK 26，Target SDK 34

## 项目文件树

```
FocusFlow/
├── build.gradle.kts                          # 根构建脚本
├── settings.gradle.kts                       # 模块声明
├── gradle.properties                         # Gradle 配置
├── gradle/
│   └── libs.versions.toml                    # 版本目录
├── app/
│   ├── build.gradle.kts                      # App 模块构建脚本
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/
│       │   │   ├── strings.xml
│       │   │   ├── colors.xml
│       │   │   └── themes.xml
│       │   ├── mipmap-hdpi/  (ic_launcher placeholder)
│       │   └── drawable/     (ic_timer, ic_stats, ic_task XML vectors)
│       └── java/com/focusflow/app/
│           ├── FocusFlowApplication.kt       # @HiltAndroidApp
│           ├── MainActivity.kt               # @AndroidEntryPoint, setContent
│           ├── navigation/
│           │   └── FocusFlowNavHost.kt       # NavHost + BottomNavBar
│           ├── di/
│           │   ├── AppModule.kt              # Room, DataStore 提供
│           │   └── RepositoryModule.kt       # Repository 绑定
│           ├── data/
│           │   ├── local/
│           │   │   ├── FocusFlowDatabase.kt  # Room Database
│           │   │   ├── dao/
│           │   │   │   ├── TaskLabelDao.kt
│           │   │   │   └── TimerRecordDao.kt
│           │   │   ├── entity/
│           │   │   │   ├── TaskLabelEntity.kt
│           │   │   │   └── TimerRecordEntity.kt
│           │   │   └── converter/
│           │   │       └── Converters.kt     # TypeConverters
│           │   └── repository/
│           │       ├── TaskRepositoryImpl.kt
│           │       └── TimerRepositoryImpl.kt
│           ├── domain/
│           │   ├── model/
│           │   │   ├── TaskLabel.kt          # Domain model
│           │   │   ├── TimerRecord.kt
│           │   │   └── TimerMode.kt          # Enum: COUNT_UP, COUNT_DOWN
│           │   ├── repository/
│           │   │   ├── TaskRepository.kt     # Interface
│           │   │   └── TimerRepository.kt    # Interface
│           │   └── usecase/
│           │       ├── task/
│           │       │   ├── GetTasksUseCase.kt
│           │       │   ├── CreateTaskUseCase.kt
│           │       │   ├── UpdateTaskUseCase.kt
│           │       │   ├── DeleteTaskUseCase.kt
│           │       │   └── ReorderTasksUseCase.kt
│           │       └── timer/
│           │           ├── StartTimerUseCase.kt
│           │           ├── StopTimerUseCase.kt
│           │           ├── GetTodayRecordsUseCase.kt
│           │           └── GetStatsUseCase.kt
│           ├── service/
│           │   └── TimerForegroundService.kt  # 前台 Service
│           └── ui/
│               ├── theme/
│               │   ├── Color.kt
│               │   ├── Type.kt
│               │   ├── Theme.kt
│               │   └── Dimens.kt
│               ├── components/
│               │   ├── BottomNavBar.kt
│               │   ├── TaskChipBar.kt        # 横向滚动的任务标签栏
│               │   ├── TimerDisplay.kt       # 大号时间数字
│               │   ├── TimerButton.kt        # 圆形开始/暂停按钮
│               │   ├── DailySummaryBar.kt    # 今日时间进度条
│               │   ├── DonutChart.kt         # 自定义 Canvas 环形图
│               │   └── ColorPicker.kt        # 8 色预设选择器
│               ├── timer/
│               │   ├── TimerScreen.kt
│               │   └── TimerViewModel.kt
│               ├── stats/
│               │   ├── StatsScreen.kt
│               │   └── StatsViewModel.kt
│               └── task/
│                   ├── TaskListScreen.kt
│                   ├── TaskEditDialog.kt
│                   └── TaskViewModel.kt
```

## 实施步骤（按 ARCHITECTURE.md 推荐顺序）

### Step 1: 项目骨架
创建 Gradle 构建系统、Hilt Application、MainActivity、Navigation、Theme。
- `gradle/libs.versions.toml` — 版本目录（Compose BOM, Hilt, Room, Navigation, DataStore）
- `build.gradle.kts`（根 + app）
- `settings.gradle.kts`
- `AndroidManifest.xml` — 声明 ForegroundService + 通知权限
- `FocusFlowApplication.kt`
- `MainActivity.kt`
- 主题文件（Color, Type, Theme）
- 导航骨架（3 个 Screen 占位 + BottomNavBar）

### Step 2: 任务标签模块 (feature:task)
完整 CRUD 流程。
- Room Entity + DAO（TaskLabelEntity, TaskLabelDao）
- Domain model + Repository interface
- Repository implementation
- Use cases（GetTasks, CreateTask, UpdateTask, DeleteTask, ReorderTasks）
- TaskViewModel + TaskListScreen + TaskEditDialog
- ColorPicker 组件

### Step 3: 计时器核心 (feature:timer)
正计时/倒计时逻辑 + ForegroundService。
- TimerRecordEntity + TimerRecordDao
- Domain model（TimerRecord, TimerMode）+ Repository interface
- TimerRepositoryImpl
- TimerForegroundService（前台 Service + 通知栏显示计时状态）
- Use cases（StartTimer, StopTimer, GetTodayRecords）
- TimerViewModel + TimerScreen
- UI 组件：TaskChipBar, TimerDisplay, TimerButton, DailySummaryBar

### Step 4: 统计模块 (feature:stats)
数据聚合 + 自定义环形图。
- GetStatsUseCase（按日/周/月聚合数据）
- StatsViewModel + StatsScreen
- DonutChart 自定义 Canvas 组件（扇区依次展开动画）

### Step 5: UI 打磨
动效、暗色模式适配、细节优化。
- 计时按钮 pulse 缩放动画
- 数字弹跳动画
- 饼图扇区展开动画
- 暗色主题颜色方案

## 关键实现细节

### ForegroundService
- 启动时创建通知渠道，显示"计时进行中"
- 通知栏显示当前任务名 + 已用时间
- 使用 `SystemClock.elapsedRealtime()` 保证时间精度
- Service 绑定到 ViewModel 通过 SharedFlow 通信

### 计时逻辑
- 正计时：从 00:00 开始累加
- 倒计时：从 targetDuration 开始递减，到 0 时震动 + 通知
- 状态机：IDLE → RUNNING → PAUSED → STOPPED

### 数据关系
- TimerRecord 通过 taskId 关联 TaskLabel
- 按 date (yyyy-MM-dd) 分区查询，支持按日统计

## 验证方式
1. 每个 Step 完成后确认项目可编译
2. Step 3 完成后手动测试计时流程：开始→暂停→结束→查看记录
3. Step 4 完成后验证统计页饼图数据与计时记录一致
4. Step 5 完成后在 Android 8.0+ 设备/模拟器上验证暗色模式和动效
5. Step 6 完成后验证：创建倒计时任务 → 计时页自动切换模式/时长 → 编辑任务可修改默认值

### Step 6: 任务级计时偏好
将计时模式（正/倒计时）和目标时长从全局临时状态下沉为任务级别的持久化属性。

**问题诊断：** 计时模式（正/倒计时）和倒计时时长存储在 `TimerViewModel` 的临时 StateFlow 中，不与任务关联。导致切换任务时不联动、重启应用后丢失、编辑任务时无法设置默认计时偏好。

**数据模型改动：**
- `TaskLabel` 领域模型增加 `defaultTimerMode: TimerMode` 和 `defaultDurationMinutes: Int?`
- `TaskLabelEntity` Room 实体增加对应列（String + nullable INTEGER）
- Room Database v1 → v2，`MIGRATION_1_2` 添加两列并设默认值

**UI 改动：**
- `TaskEditDialog` — 增加正计时/倒计时 FilterChip 选择 + 时长预设 Chip 行 + 自定义分钟数输入
- `TaskViewModel.saveTask()` — 签名扩展接收 `TimerMode` 和 `durationMinutes`
- `TimerViewModel.selectTask()` — 选中任务时自动同步 `_mode` 和 `_targetDuration`
- `TaskListScreen` 任务卡片 — 显示计时模式子标题（"正计时"/"倒计时 · 25分钟"）
- `TaskChipBar` 任务标签 — 倒计时任务显示 "↓25m" 标识

**行为逻辑：**
- 任务创建/编辑时可设定默认模式和目标时长
- 计时页选中任务 → 自动应用该任务的默认模式/时长
- 用户仍可在计时页临时覆盖（本次会话有效，不写回任务）
- 正计时任务 `defaultDurationMinutes` 为 null，不显示时长预设

**涉及文件（9 个修改）：**

```
FocusFlow/
├── app/src/main/java/com/focusflow/app/
│   ├── domain/model/
│   │   └── TaskLabel.kt              ✅ +2 字段 (defaultTimerMode, defaultDurationMinutes)
│   ├── data/local/
│   │   ├── entity/
│   │   │   └── TaskLabelEntity.kt    ✅ +2 列 + 更新 toDomain/toEntity 映射
│   │   └── FocusFlowDatabase.kt      ✅ version 1→2, MIGRATION_1_2
│   ├── di/
│   │   └── AppModule.kt             ✅ 注册 MIGRATION_1_2
│   ├── ui/
│   │   ├── task/
│   │   │   ├── TaskEditDialog.kt    ✅ 重写：模式选择 + 时长设置 + 自定义输入
│   │   │   ├── TaskViewModel.kt     ✅ saveTask 签名扩展
│   │   │   └── TaskListScreen.kt    ✅ 卡片显示模式子标题
│   │   ├── timer/
│   │   │   └── TimerViewModel.kt    ✅ selectTask 自动同步模式/时长
│   │   └── components/
│   │       └── TaskChipBar.kt       ✅ 倒计时任务显示 ↓Nmin 标识
```
