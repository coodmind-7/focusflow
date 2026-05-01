# FocusFlow 开发进度

> 最后更新：2026-05-01 (Step 13 已完成)

---

## Step 1: 项目骨架 ✅ 已完成

创建 Gradle 构建系统、Hilt Application、MainActivity、Navigation、Theme。**20 个文件**。

- `gradle/libs.versions.toml` — Compose BOM 2024.02.00, Hilt 2.50, Room 2.6.1, Navigation 2.7.7, DataStore 1.0.0
- `build.gradle.kts` (根 + app) — minSdk 26, targetSdk 34, Compose enabled
- `AndroidManifest.xml` — ForegroundService + 通知/震动权限
- `FocusFlowApplication.kt` / `MainActivity.kt` — Hilt entry, edgeToEdge + setContent
- Theme: Color.kt (亮/暗色板 + 8 色 ChartColors), Type.kt, Theme.kt
- Navigation: FocusFlowNavHost (timer/stats 两路由), BottomNavBar (2 项)
- 三个 Screen 占位: TimerScreen, StatsScreen, TaskListScreen（TaskListScreen 在 Step 7 中从导航移除，文件保留）

---

## Step 2: 任务标签模块 ✅ 已完成

完整 CRUD 流程：Room Entity + DAO → Repository → Use Case → ViewModel → Compose UI。**18 个文件**。

- Room: FocusFlowDatabase, TaskLabelEntity + 映射函数, TaskLabelDao (CRUD + reorder), Converters
- DI: AppModule (Room + DAO), RepositoryModule (Repository 绑定)
- Domain: TaskLabel, TaskRepository (interface), 5 use cases (Get/Create/Update/Delete/Reorder)
- Data: TaskRepositoryImpl
- UI: TaskViewModel, TaskListScreen (SwipeToDismiss + 上下移动，Step 7 移至计时页), TaskEditDialog, ColorPicker (8 色预设)

---

## Step 3: 计时器核心 ✅ 已完成

正计时/倒计时逻辑 + ForegroundService。**16 个新建 + 5 个修改**。

- Domain: TimerMode (COUNT_UP/COUNT_DOWN), TimerRecord, TimerRepository (interface)
- Use cases: StartTimerUseCase, StopTimerUseCase, GetTodayRecordsUseCase
- Data: TimerRecordEntity + 映射, TimerRecordDao, TimerRepositoryImpl
- Service: TimerManager (@Singleton, SystemClock.elapsedRealtime, SharedFlow), TimerForegroundService (前台通知)
- UI: TimerViewModel, TimerScreen, TaskChipBar (Step 7 删除), TimerDisplay, TimerButton (Step 7 删除), DailySummaryBar

---

## Step 4: 统计模块 ✅ 已完成

数据聚合 + 自定义环形图。**5 个新建 + 1 个修改**。

- Domain: StatsData (TaskTimeSlice + StatsData), GetStatsUseCase (按日期范围 + 任务分组)
- UI: StatsViewModel (日/周/月 FilterChip + 日期导航), StatsScreen, DonutChart (Canvas 自定义 + 扇区展开动画)

---

## Step 5: UI 打磨 ✅ 已完成

动效、暗色模式适配。**7 个修改**。

- Color.kt / Theme.kt — 暗色主题完善 (SurfaceVariant, Outline, DarkOnPrimary, DarkAccent)
- TimerDisplay — 数字弹跳动画 + 冒号闪烁（Step 9 中移除，改为静态显示）
- DonutChart — 扇区依次展开 (stagger 150ms 延迟) + 暗色模式空环适配
- FocusFlowNavHost — 页面切换 fadeIn/fadeOut (250ms)
- TimerScreen — 计时中背景色 primary@4% alpha 渐变

---

## Step 6: 任务级计时偏好 ✅ 已完成

计时模式/目标时长从全局临时状态下沉为任务级别持久化属性。**9 个修改**。

- TaskLabel: +`defaultTimerMode: TimerMode`, +`defaultDurationMinutes: Int?`
- TaskLabelEntity: +两列 (TEXT + nullable INTEGER), Room Migration 1→2
- TaskEditDialog: 模式选择 (正/倒计时 FilterChip) + 7 档时长预设 + 自定义输入
- TaskViewModel.saveTask 签名扩展 (timerMode, durationMinutes)

---

## Step 7: 计时页交互重构 ✅ 已完成

将任务管理与计时执行统一到计时页，简化操作路径。**6 个修改 + 2 个删除**。

### 改动概览

| 文件 | 动作 | 说明 |
|------|------|------|
| `TimerViewModel.kt` | **重写** | 合并 TaskViewModel 的任务 CRUD 逻辑，startTimer(task)/stopTimer() |
| `TimerScreen.kt` | **重写** | TopAppBar [+] 创建任务入口，任务列表 + ▶/⏸ 按钮，长按编辑工具栏 |
| `TimerDisplay.kt` | 更新 | +mode 参数支持倒计时递减显示，+taskName/taskColor 参数 |
| `TimerManager.kt` | 更新 | 倒计时到达 0 时自动 stop |
| `BottomNavBar.kt` | 更新 | 移除「任务」Tab，保留「计时」「统计」 |
| `FocusFlowNavHost.kt` | 更新 | 移除 `composable("tasks")` 路由 |
| `TimerButton.kt` | **删除** | 不再需要独立的大圆形按钮 |
| `TaskChipBar.kt` | **删除** | 不再需要横向滚动 chip 条 |

### 架构要点

- TimerViewModel 注入全部 9 个 use case (5 个 task + 3 个 timer + 1 个 stats)，统一管理计时页全部业务逻辑
- `uiState` 通过嵌套 `combine` 合并 tasks、timer、dialog、longPress 多维度状态
- `todayTotalSeconds` 通过 `combine(DB sum Flow, timerEvents Flow)` 融合实时数据
- `TaskListScreen.kt` / `TaskViewModel.kt` 保留未删除，后续如需独立任务管理页可恢复

---

## Step 8: 关键 Bug 修复 ✅ 已完成

真机测试后发现两个阻断性 bug，均已修复。**2 个修改**。

### Bug 1: 点击创建任务按钮无反应

**根因**: `TimerManager.kt` 中 `timerEvents` 使用 `MutableSharedFlow`（无初始值），`combine` 要求所有输入 Flow 都至少发射一次才产出结果，导致 `uiState` 被"卡住"。

**修复**: 将 `MutableSharedFlow` 改为 `MutableStateFlow`，确保 collect 时即能拿到值。

### Bug 2: 桌面图标不显示

**根因**: `AndroidManifest.xml` 中 `android:icon="@drawable/ic_launcher_foreground"` 直接引用了前景矢量 drawable，跳过了 adaptive icon 包装层。

**修复**: 改为 `android:icon="@mipmap/ic_launcher"` + `android:roundIcon="@mipmap/ic_launcher"`。

---

## Step 9: 计时体验优化 + 每日目标系统 ✅ 已完成

根据产品需求完成 5 项体验改进。**7 个修改 + 6 个新增**。

### 改动概览

| 文件 | 动作 | 说明 |
|------|------|------|
| `TimerForegroundService.kt` | 修改 | `formatTime()` 去除秒数，始终两位小时占位 `00:05` |
| `TimerDisplay.kt` | **重写** | 移除弹跳动画、冒号闪烁、所有动效。时钟完全静态显示 |
| `TimerScreen.kt` | **重写** | 长按操作栏圆角适配；DailySummaryBar 传入 goal 参数；目标达成庆祝 |
| `DailySummaryBar.kt` | **重写** | 标签改为"今日目标专注"；达成标记 ✓；右侧时间可点击 |
| `TimerViewModel.kt` | **重写** | 注入 `UserPreferences` + `RecordDailyGoalUseCase`；combine 三层嵌套；每日目标逻辑 |
| `StatsViewModel.kt` | **重写** | 注入 `GetDailyGoalsUseCase`；`combine→flatMapLatest` 重构 |
| `StatsScreen.kt` | **重写** | 新增 `GoalAchievementCard`：达成率 + 最近 5 条记录 |
| `GoalEditDialog.kt` | **新增** | 小时+分钟分栏输入（最低 1 分钟） |
| `UserPreferences.kt` | **新增** | `@Singleton` DataStore 封装，`dailyGoalSeconds: Flow<Long>`（默认 8h） |
| `DailyGoalEntity.kt` | **新增** | Room Entity：date PK, goalSeconds, achievedSeconds, achieved |
| `DailyGoalDao.kt` | **新增** | upsert, getByDate, getAllDesc, getTotalAchievedCount, getByDateRange |
| `RecordDailyGoalUseCase.kt` | **新增** | 每次停止计时时 upsert，首次达成返回 true（触发庆祝） |
| `GetDailyGoalsUseCase.kt` | **新增** | 封装 `DailyGoalDao` 查询 |
| `FocusFlowDatabase.kt` | 修改 | 新增 `MIGRATION_2_3`，entities 增加 `DailyGoalEntity` |
| `AppModule.kt` | 修改 | 新增 `provideDailyGoalDao()` |

### 架构要点

- `TimerViewModel.combine` 采用三层嵌套：`combine(combine(combine(3), combine(3)), combine(4))`
- `StatsViewModel` 使用 `combine(_period, _referenceDate) → flatMapLatest → combine(statsFlow, goalsFlow)`
- `ChronoUnit.DAYS.between()` 替代 `LocalDate.datesUntil()`（minSdk=26 兼容）
- 同一日多次达成不会重复庆祝（`!alreadyAchieved && nowAchieved`）

---

## Step 10: 计时交互重构 + UI 打磨 ✅ 已完成

解决 3 个产品体验问题。**4 个修改**。

### 问题 1: 任务栏运行态阴影边框

移除 `CardDefaults.cardElevation()`，仅保留 `primary.copy(alpha=0.08f)` 背景色区分运行态。

### 问题 2: 暂停/继续计时

4 个文件联动：
- `TimerManager.kt` — `start()` 增加 `initialElapsedMs` 参数，`pause()` 返回当前累计时长
- `StopTimerUseCase.kt` — 新增 `updateDuration()` 方法，暂停时只写 duration 不设 endTime
- `TimerViewModel.kt` — 新增 `TaskTimerSession` + `taskSessions` 管理每任务独立进度；支持恢复/切换/新建三种路径；新增 `pauseTimer()`、`resetTask()`
- `TimerScreen.kt` — ▶/⏸ 播放暂停切换替代 ▶/⏹

交互变更：

| 操作 | 之前 | 之后 |
|------|------|------|
| 点击 ▶ | 从 0 开始，按钮变 ⏹ | 从 0 或上次进度开始，按钮变 ⏸ |
| 点击 ⏸ | 无此按钮 | 暂停，进度写入 DB |
| 再次点击 ▶ | 从 0 开始（新记录） | 从暂停进度继续（同一 DB 记录） |
| 点击其他任务 ▶ | 被阻止 | 自动暂停当前 → 启动/恢复目标 |
| 长按暂停的任务 | 被阻止 | 显示 `[重新开始] [编辑] [上移] [下移] [删除]` |
| 点击重新开始 | 无此功能 | 关闭旧记录，清除进度 |

### 问题 3: 目标达成庆祝动画

从顶部滑入横幅替换为全屏半透明遮罩 + 居中弹出卡片（`scaleIn` + `fadeIn`），3 秒自动消失。

### 架构要点

- 每任务计时进度通过 `taskSessions: Map<Long, TaskTimerSession>` 在 ViewModel 内存中维护
- `_pausedTaskIds` 独立于 `uiState` combine 链，避免进一步增加嵌套层数
- 暂停时调用 `updateDuration()` 仅更新 duration，endTime 保持 NULL；完成/重置时同时设置 endTime + duration
- 倒计时自动停止路径：`TimerManager.startTicking()` → `stop()` → IDLE 事件 → ViewModel init 保存记录 + 检查目标

---

## Step 11: 三大 Bug 修复 + 手动备份功能 ✅ 已完成

> 实施日期：2026-04-30

### 问题与修复

**问题 1: 删除任务后统计中数据丢失身份**
- 根因: `GetStatsUseCase` 通过 `taskRepository.getTasks()` 关联任务名和颜色，任务删除后关联失败
- 修复: `TimerRecordEntity` 冗余存储 `taskName`/`taskColor` 快照，`GetStatsUseCase` 改为直接读取快照，移除 `TaskRepository` 依赖

**问题 2: 计时运行中今日目标时间不更新**
- 根因: `todayTotalSeconds` 完全从 DB 的 `duration` 字段求和，运行中记录 `duration=0`
- 修复: `combine(DB sum, timerEvents.elapsedMs)` 融合实时数据，公式 `(dbSum + liveElapsed - currentRecordSavedDuration) / 1000`

**问题 3: 卸载重装后旧数据自动恢复**
- 根因: `android:allowBackup="true"`，Android 自动备份到 Google Drive
- 修复: 改为 `false`，新增手动备份/还原功能替代

### 改动概览

#### Phase 0: 依赖新增

| 文件 | 说明 |
|------|------|
| `gradle/libs.versions.toml` | 新增 kotlinx-serialization-json 1.6.2 + serialization 插件 |
| `build.gradle.kts` (根 + app) | 注册 serialization 插件 + library 依赖 |

#### Phase 1 (P0): 修复 todayTotalSeconds 实时计算

| 文件 | 说明 |
|------|------|
| `TimerViewModel.kt` | 新增 `currentRecordSavedDuration`；`todayTotalSeconds` 改为 `combine(dbSum, timerEvents)` |

#### Phase 2 (P1): 任务名/颜色快照

| 文件 | 说明 |
|------|------|
| `TimerRecordEntity.kt` | 新增 `taskName`、`taskColor` 列及映射 |
| `TimerRecord.kt` | 同步新增字段 |
| `FocusFlowDatabase.kt` | 新增 `MIGRATION_3_4`，版本 3→4 |
| `AppModule.kt` | 添加 `MIGRATION_3_4` |
| `TimerViewModel.kt` | `startTimer()` 创建记录时写入 `taskName/taskColor` |
| `GetStatsUseCase.kt` | 移除 `TaskRepository`，从快照直接读取 |

#### Phase 3 (P2): 关闭自动备份

`AndroidManifest.xml`: `android:allowBackup="true"` → `"false"`

#### Phase 4: 手动备份/还原功能

**DAO 层新增查询:** `TaskLabelDao.getAllTasksSuspend/deleteAllTasks/insertAllTasks`, `TimerRecordDao.getAllRecords/deleteAllRecords/insertAllRecords`, `DailyGoalDao.getAllGoals/deleteAllGoals/insertAllGoals`

**Repository 接口扩展:** `TaskRepository`/`TimerRepository` 新增 `getAll*Once/deleteAll*/insertAll*` 方法

**新增文件:**

| 文件 | 说明 |
|------|------|
| `data/backup/BackupData.kt` | `@Serializable` 数据模型 |
| `data/backup/BackupManager.kt` | `@Singleton`，JSON 编解码，按类别导出/导入，任务 ID 重映射 |
| `ui/settings/SettingsViewModel.kt` | `@HiltViewModel`，管理导出/导入状态和文件操作（SAF） |
| `ui/settings/SettingsScreen.kt` | Compose UI：CheckBox 选择类别 + 导出/导入按钮 + Snackbar |

**导航变更:** `BottomNavBar.kt` 新增第 3 个 Tab "设置"；`FocusFlowNavHost.kt` 新增 `composable("settings")` 路由

### 备份 JSON 格式

```json
{
  "version": 1,
  "exportDate": "2026-05-01T...",
  "tasks": [{ "id": 1, "name": "阅读", "color": "#FF6B6B", "sortOrder": 0, "createdAt": 1714521600000, "defaultTimerMode": "COUNT_UP", "defaultDurationMinutes": null }],
  "timerRecords": [{ "id": 1, "taskId": 1, "date": "2026-05-01", "startTime": 1714521600000, "endTime": null, "duration": 3600000, "mode": "COUNT_UP", "targetDuration": 0, "taskName": "阅读", "taskColor": "#FF6B6B" }],
  "dailyGoals": [{ "date": "2026-05-01", "goalSeconds": 28800, "achievedSeconds": 18000, "achieved": false }],
  "dailyGoalSeconds": 28800
}
```

---

## Step 12: 统计实时性修复 ✅ 已完成

> 实施日期：2026-04-30

解决统计页面不显示进行中/暂停中计时数据的问题。**4 个修改 + 2 个配置更新**。

### 问题

1. 统计页面只显示 `endTime != null` 的已完成记录，暂停或运行中的积累时间无法查看
2. 运行中计时的时间仅在内存中（`duration=0`），未写入数据库，切换 app 或崩溃就丢失
3. 切换到统计 tab 时不会触发数据刷新，底部导航 ViewModel 保留导致 `init` 只跑一次

### 修复

**修复 1: 统计过滤条件放宽**

[GetStatsUseCase.kt](app/src/main/java/com/focusflow/app/domain/usecase/timer/GetStatsUseCase.kt#L16) — `.filter { it.endTime != null && it.duration > 0 }` → `.filter { it.duration > 0 }`。暂停时已写入 duration 的记录直接进入统计。

**修复 2: 定期自动保存运行中耗时**

[TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L134) — 新增后台协程，每 1 小时检查计时是否运行中，若是则 `updateDuration()` 写入数据库。用户无需手动暂停即可在统计中看到接近实时的数据。

**修复 3: 切换到统计页面时触发保存**

- [StatsViewModel.kt](app/src/main/java/com/focusflow/app/ui/stats/StatsViewModel.kt#L39) — 注入 `TimerManager` + `TimerRepository`，新增公开 `onEnter()` 方法，将当前运行耗时写入 DB
- [StatsScreen.kt](app/src/main/java/com/focusflow/app/ui/stats/StatsScreen.kt#L48) — 新增 `DisposableEffect` + `LifecycleEventObserver`，监听 `ON_RESUME` 生命周期，每次 tab 切换回来调用 `viewModel.onEnter()`

**修复 4: App 进入后台时自动保存**

[TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L162) — 注册 `ProcessLifecycleOwner.get().lifecycle.addObserver()`，在 `onStop` 时将运行耗时写入数据库。

### 改动概览

| 文件 | 动作 | 说明 |
|------|------|------|
| `GetStatsUseCase.kt` | 修改 | 过滤条件去掉 `endTime != null`，暂停记录也能统计 |
| `TimerViewModel.kt` | 修改 | 新增 1 小时间隔定期保存 + `ProcessLifecycleOwner` 后台保存 |
| `StatsViewModel.kt` | 修改 | 注入 `TimerManager` + `TimerRepository`，新增 `onEnter()` |
| `StatsScreen.kt` | 修改 | 新增生命周期监听，`ON_RESUME` 时调用 `onEnter()` |
| `gradle/libs.versions.toml` | 修改 | 新增 `lifecycle-process` library |
| `app/build.gradle.kts` | 修改 | 新增 `implementation(libs.lifecycle.process)` |

### 数据刷新流程

```
切换统计 tab → ON_RESUME → onEnter() → DB write (Room) → Flow emit → UI recompose
                                                                    ↑
                          1h 定时保存 ───────────────────────────────┤
                          后台 onStop ───────────────────────────────┘
```

---

## Step 13: 备份数据完整性修复 ✅ 已完成

> 实施日期：2026-05-01

修复备份流程中 TimerRecord 的 taskName/taskColor 快照字段在导出和导入两端均丢失的问题。**1 个修改**。

### 问题

Step 11 在 `TimerRecordEntity` 中冗余存储 `taskName`/`taskColor` 快照，确保删除任务后统计中计时记录仍保留身份信息。但备份流程存在两处遗漏：

- **导出端**: `toBackupItem()` 未将 `taskName`/`taskColor` 写入 JSON，导出文件中这两个字段始终为空字符串
- **导入端**: `importFromJson()` 构建 `TimerRecordEntity` 时未恢复 `taskName`/`taskColor`

用户场景：删除某任务 → 导出备份 → 一段时间后导入 → 该任务的历史计时记录在统计图表中变成无名空白条目。

### 修复

| 文件 | 说明 |
|------|------|
| `BackupManager.kt` | `toBackupItem()` 写入 `taskName`/`taskColor`；`importFromJson()` 恢复这两个字段 |

---

## 项目结构

```
app/src/main/java/com/focusflow/app/
├── FocusFlowApplication.kt
├── MainActivity.kt
├── data/
│   ├── backup/
│   │   ├── BackupData.kt
│   │   └── BackupManager.kt
│   ├── local/
│   │   ├── FocusFlowDatabase.kt
│   │   ├── converter/Converters.kt
│   │   ├── dao/
│   │   │   ├── TaskLabelDao.kt
│   │   │   ├── TimerRecordDao.kt
│   │   │   └── DailyGoalDao.kt
│   │   ├── entity/
│   │   │   ├── TaskLabelEntity.kt
│   │   │   ├── TimerRecordEntity.kt
│   │   │   └── DailyGoalEntity.kt
│   │   └── preferences/
│   │       └── UserPreferences.kt
│   └── repository/
│       ├── TaskRepositoryImpl.kt
│       └── TimerRepositoryImpl.kt
├── di/
│   ├── AppModule.kt
│   └── RepositoryModule.kt
├── domain/
│   ├── model/
│   │   ├── TaskLabel.kt
│   │   ├── TimerMode.kt
│   │   ├── TimerRecord.kt
│   │   └── StatsData.kt
│   ├── repository/
│   │   ├── TaskRepository.kt
│   │   └── TimerRepository.kt
│   └── usecase/
│       ├── task/
│       │   ├── GetTasksUseCase.kt
│       │   ├── CreateTaskUseCase.kt
│       │   ├── UpdateTaskUseCase.kt
│       │   ├── DeleteTaskUseCase.kt
│       │   └── ReorderTasksUseCase.kt
│       └── timer/
│           ├── StartTimerUseCase.kt
│           ├── StopTimerUseCase.kt
│           ├── GetTodayRecordsUseCase.kt
│           ├── GetStatsUseCase.kt
│           ├── RecordDailyGoalUseCase.kt
│           └── GetDailyGoalsUseCase.kt
├── navigation/
│   └── FocusFlowNavHost.kt
├── service/
│   ├── TimerForegroundService.kt
│   └── TimerManager.kt
└── ui/
    ├── theme/
    │   ├── Color.kt
    │   ├── Type.kt
    │   └── Theme.kt
    ├── components/
    │   ├── BottomNavBar.kt
    │   ├── ColorPicker.kt
    │   ├── DailySummaryBar.kt
    │   ├── DonutChart.kt
    │   └── TimerDisplay.kt
    ├── task/
    │   ├── TaskEditDialog.kt
    │   ├── TaskListScreen.kt (保留，未在导航中使用)
    │   └── TaskViewModel.kt (保留，未使用)
    ├── timer/
    │   ├── GoalEditDialog.kt
    │   ├── TimerScreen.kt
    │   └── TimerViewModel.kt
    ├── stats/
    │   ├── StatsScreen.kt
    │   └── StatsViewModel.kt
    └── settings/
        ├── SettingsScreen.kt
        └── SettingsViewModel.kt
```
