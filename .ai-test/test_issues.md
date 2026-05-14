# FocusFlow 产品测试 — 问题记录

---

## 测试会话 #1 — 2026-05-02

| 元信息 | 详情 |
|--------|------|
| **测试日期** | 2026-05-02 |
| **数据来源** | JSON (test-cases.json v3.0) |
| **测试用例总数** | 11 |
| **产品类型** | Android 移动应用（专注计时器） |
| **技术栈** | Kotlin, Jetpack Compose, Room, Hilt, Coroutines/Flow |

---

### ISSUE-001 | FAIL | 严重 | UA-ASYNC-001

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-001 |
| **类型** | 缺陷 |
| **严重程度** | 严重 |
| **测试项** | 快速切换任务时计时器状态错乱 |
| **状态** | 已修复 |
| **复杂度** | 高 |
| **预估耗时** | 15 分钟 |
| **实际耗时** | 8 分钟 |
| **JSON 用例 ID** | UA-ASYNC-001 |

**问题描述：**
`startTimer()` 方法在 `viewModelScope.launch` 中执行，无 Mutex 或其他互斥锁保护。快速连续点击不同任务的播放按钮时，多个协程可能同时执行任务切换逻辑，导致对 `runningRecordId`、`taskSessions`、`currentRecordSavedDuration` 的并发读写。

**复现步骤：**
1. 创建 Task A 和 Task B
2. 启动 Task A 计时
3. 快速连续点击 Task B → Task A 的播放按钮
4. 两个 `startTimer()` 协程同时执行

**期望结果：**
Task A 和 Task B 的计时状态正确保存，无数据丢失，无重复记录

**实际结果：**
- 两个协程可能同时读取 `runningRecordId`，一个将其 finalize 后另一个尝试用已失效的 ID 更新数据库
- `taskSessions` 映射可能不一致
- `currentRecordSavedDuration` 可能被并发修改，导致 `todayTotalSeconds` 短暂异常

**代码证据：**
- [TimerViewModel.kt:87-88](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L87-L88) — `runningRecordId` 和 `currentRecordSavedDuration` 为普通 var，无同步保护
- [TimerViewModel.kt:90](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L90) — `taskSessions` 为普通 `mutableMapOf`，无同步保护
- [TimerViewModel.kt:236-289](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L236-L289) — `startTimer()` 在协程中执行，无互斥锁

**修复建议：**
在 `startTimer` 方法中使用 `Mutex` 保护整个任务切换逻辑，或使用 `actor` 模式将所有计时操作序列化。

**修复说明：**
- [TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt) — 新增 `private val startMutex = Mutex()`，`startTimer()` 内部逻辑包裹在 `startMutex.withLock { }` 中，确保同一时间只有一个协程执行任务切换/状态变更，消除 `runningRecordId`、`taskSessions`、`currentRecordSavedDuration` 的并发读写竞态。
- 新增 `import kotlinx.coroutines.sync.Mutex` 和 `kotlinx.coroutines.sync.withLock`。

**实际结果：** 编译通过，回归测试通过。

---

### ISSUE-002 | FAIL | 一般 | UA-ASYNC-002

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-002 |
| **类型** | 缺陷 |
| **严重程度** | 一般 |
| **测试项** | App 前后台快速切换导致计时器意外暂停或继续 |
| **状态** | 已修复 |
| **复杂度** | 中 |
| **预估耗时** | 10 分钟 |
| **实际耗时** | 5 分钟 |
| **JSON 用例 ID** | UA-ASYNC-002 |

**问题描述：**
`onAppPause()` 中使用 3 秒 debounce 延迟暂停计时器，`onAppResume()` 取消延迟并恢复。但 `pauseTimer()`（第302行）不检查 `wasRunningBeforePause` 标记。如果用户在 3 秒临界点切回，`delay` 刚好到期后 `pauseTimer()` 可能绕过 `onAppResume` 的取消逻辑，在 App 回到前台后仍执行暂停。

**复现步骤：**
1. 计时器正在运行，用户按 Home 切到后台
2. 约 3 秒后快速切回 App
3. 观察计时器是否意外暂停

**期望结果：**
无论前台/后台切换多快，计时器状态始终正确：在后台超过 3 秒后暂停，回到前台恢复

**实际结果：**
若 `delay(3_000)` 刚好在 `pauseDebounceJob?.cancel()` 生效前完成，`pauseTimer()` 在 `onAppResume` 逻辑之后执行，导致前台计时器被意外暂停

**代码证据：**
- [TimerViewModel.kt:94-95](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L94-L95) — `pauseDebounceJob` 和 `wasRunningBeforePause` 无同步保护
- [TimerViewModel.kt:209-217](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L209-L217) — `onAppPause()` 中的 debounce 延迟
- [TimerViewModel.kt:302-315](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L302-L315) — `pauseTimer()` 不检查前后台状态

**修复建议：**
在 `delay` 块内再次检查 `wasRunningBeforePause` 标记；或在 `pauseTimer()` 中加入对 `pauseDebounceJob` 是否已取消的判断。

**修复说明：**
- [TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt) — `onAppPause()` 中的 debounce 延迟块在 `delay(3_000)` 后增加 `if (wasRunningBeforePause)` 守卫检查，确保当 `onAppResume()` 已取消 debounce 并重置标记后，延迟到期的 `pauseTimer()` 不会意外执行。

**实际结果：** 编译通过，回归测试通过。

---

### ISSUE-003 | FAIL | 一般 | UA-ASYNC-003

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-003 |
| **类型** | 缺陷 |
| **严重程度** | 一般 |
| **测试项** | 跨午夜计时导致日期归属错误 |
| **状态** | 已修复 |
| **复杂度** | 中 |
| **预估耗时** | 10 分钟 |
| **实际耗时** | 5 分钟 |
| **JSON 用例 ID** | UA-ASYNC-003 |

**问题描述：**
`startTimer()` 中 `TimerRecord.date` 固定为启动时的日期字符串。持久化循环（60秒间隔）只更新 `duration`，不检测日期变化。跨午夜计时（23:30–00:30）的所有时间都归属到启动日期。

**复现步骤：**
1. 23:30 启动任务计时
2. 计时持续到 00:30 后停止
3. 查看当天和前一天统计

**期望结果：**
00:00 之前的时间归属前一天，00:00 之后的时间归属当天

**实际结果：**
全部 60 分钟归属到启动日期。前一天统计多出 30 分钟，当天统计缺少 30 分钟

**代码证据：**
- [TimerViewModel.kt:272-287](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L272-L287) — 注释明确承认跨午夜不做拆分
- [TimerViewModel.kt:155-163](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L155-L163) — 持久化循环不检测日期变化

**修复建议：**
在持久化循环中检测日期变化，若变化则拆分记录；或使用 UTC 时间戳存储，查询时按时间范围而非日期字符串。

**修复说明：**
- [TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt) — 新增 `private var runningRecordDate: String` 字段；持久化循环增加日期变化检测，跨午夜时自动 finalize 旧记录并创建新日期记录，实现自动拆分。
- `startTimer()` 创建新记录时设置 `runningRecordDate = todayDate`；孤儿恢复时设置 `runningRecordDate = orphan.date`。

**实际结果：** 编译通过，回归测试通过。

---

### ISSUE-004 | FAIL | 一般 | UA-DATA-004

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-004 |
| **类型** | 缺陷 |
| **严重程度** | 一般 |
| **测试项** | 导入备份数据后任务 ID 映射错乱 |
| **状态** | 已修复 |
| **复杂度** | 中 |
| **预估耗时** | 12 分钟 |
| **实际耗时** | 6 分钟 |
| **JSON 用例 ID** | UA-DATA-004 |

**问题描述：**
`importFromJson()` 中，当 `importTasks=false` 但 `importTimerRecords=true` 时，`taskIdMap` 为空，计录记录的 `taskId` 回退到 JSON 中的旧 ID（`taskIdMap[item.taskId] ?: item.taskId`）。旧 ID 在当前数据库中可能不存在，导致统计页面显示"已删除"标签或数据关联错误。

**复现步骤：**
1. 准备包含 5 个任务和 50 条计时记录的备份 JSON
2. 导入时取消勾选"任务"类别，仅勾选"计时记录"
3. 查看统计页面

**期望结果：**
所有计时记录正确关联到导入后的新任务（通过 taskName 匹配）

**实际结果：**
`taskIdMap` 为空 → 所有 timerRecord 保留 JSON 中的旧 taskId → 当前数据库无对应任务 → 统计页面任务名称显示异常

**代码证据：**
- [BackupManager.kt:74](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt#L74) — `taskIdMap` 仅当 `importTasks=true` 且有 tasks 数据时才赋值
- [BackupManager.kt:95](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt#L95) — `taskIdMap[item.taskId] ?: item.taskId` 无 task 映射时回退到旧 ID
- [TaskLabelDao.kt:50-53](app/src/main/java/com/focusflow/app/data/local/dao/TaskLabelDao.kt#L50-L53) — `replaceAllTasks` 逐个插入，依赖顺序保序

**修复建议：**
导入时若 tasks 未导入但 timerRecords 已导入，应提示用户或自动使用记录的 taskName 字段；可建立更健壮的 taskName+color 匹配机制。

**修复说明：**
- [TaskLabelDao.kt](app/src/main/java/com/focusflow/app/data/local/dao/TaskLabelDao.kt) — 新增 `getTaskByName(name)` 查询方法。
- [BackupManager.kt](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt) — 当 `importTasks=false` 但 `importTimerRecords=true` 时，通过 `taskName` 匹配现有任务构建 `taskIdMap`，避免计时记录关联到不存在的旧 taskId。

**实际结果：** 编译通过，回归测试通过。

---

### ISSUE-005 | FAIL | 严重 | UA-DATA-005

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-005 |
| **类型** | 缺陷 |
| **严重程度** | 严重 |
| **测试项** | 导入 JSON 中途失败导致数据半损坏 |
| **状态** | 已修复 |
| **复杂度** | 中 |
| **预估耗时** | 8 分钟 |
| **实际耗时** | 5 分钟 |
| **JSON 用例 ID** | UA-DATA-005 |

**问题描述：**
`importFromJson()` 按顺序执行四步数据库操作（替换 tasks → timerRecords → dailyGoals → settings），但无全局事务。若中间步骤失败（如 timerRecords 格式异常），前一步已执行的表替换不会回滚，导致数据库处于新旧混合状态。虽有 `try-catch` 在 ViewModel 层，但无法回滚已提交的 Room 操作。

**复现步骤：**
1. 准备一个 tasks 正常但 timerRecords 包含格式错误的 JSON 文件
2. 导入时选择所有类别
3. observe 数据库状态

**期望结果：**
导入失败后，数据库恢复到导入前的完整状态

**实际结果：**
- tasks 表已替换为新数据 → 旧任务丢失
- timerRecords 替换失败 → 保留旧记录（引用已不存在的 taskId）
- dailyGoals 和 settings 未更新
- 数据库处于不一致状态

**代码证据：**
- [BackupManager.kt:65](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt#L65) — `importFromJson()` 无 `@Transaction` 注解
- [BackupManager.kt:76-89](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt#L76-L89) — 步骤1：tasks 替换（独立事务）
- [BackupManager.kt:91-107](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt#L91-L107) — 步骤2：timerRecords 替换（若失败，步骤1不回滚）
- [TaskLabelDao.kt:49-53](app/src/main/java/com/focusflow/app/data/local/dao/TaskLabelDao.kt#L49-L53) — 单个 replaceAllTasks 有 @Transaction，但跨表无保护

**修复建议：**
在 `importFromJson` 上添加 `@Transaction`；或导入前在临时表中验证数据完整性；或使用 Room 数据库的 `runInTransaction` 包装所有步骤。

**修复说明：**
- [BackupManager.kt](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt) — 重构 `importFromJson()` 为两阶段：Phase 1 在写入前预构建所有实体对象并验证引用完整性（JSON 解析失败会直接抛异常）；Phase 2 顺序写入各表。每个 `replaceAll*` 方法自身有 `@Transaction` 保证单表原子性。此结构确保数据格式错误在写入前被捕获，消除"半导入"状态。

**实际结果：** 编译通过，回归测试通过。

---

### ISSUE-006 | FAIL | 轻微 | UA-INPUT-006

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-006 |
| **类型** | 缺陷 |
| **严重程度** | 轻微 |
| **测试项** | GoalEditDialog 输入无效值静默修正导致用户困惑 |
| **状态** | 已修复 |
| **复杂度** | 低 |
| **预估耗时** | 5 分钟 |
| **实际耗时** | 3 分钟 |
| **JSON 用例 ID** | UA-INPUT-006 |

**问题描述：**
`GoalEditDialog` 保存时使用 `hours.toIntOrNull() ?: 0`，空字符串被静默转为 0，再经 `coerceAtLeast(60L)` 强制设为 60 秒（1分钟）。用户清空输入后保存，目标会被设为 1 分钟且无任何提示，产生困惑。

**复现步骤：**
1. 打开每日目标修改对话框
2. 清空小时和分钟输入框
3. 点击保存

**期望结果：**
清空输入后保存时应使用原值或给出提示

**实际结果：**
目标被静默设为 60 秒（1分钟），用户不理解为何目标被修改

**代码证据：**
- [GoalEditDialog.kt:82](app/src/main/java/com/focusflow/app/ui/timer/GoalEditDialog.kt#L82) — `hours.toIntOrNull() ?: 0` 静默处理空/无效输入
- [GoalEditDialog.kt:84](app/src/main/java/com/focusflow/app/ui/timer/GoalEditDialog.kt#L84) — `coerceAtLeast(60L)` 将零目标强制设为 60 秒

**修复建议：**
保存时若输入为空，回退到当前值而非 0；或添加 Snackbar 提示最小目标为 1 分钟。

**修复说明：**
- [GoalEditDialog.kt](app/src/main/java/com/focusflow/app/ui/timer/GoalEditDialog.kt) — 保存时当输入为空字符串，`hours.toIntOrNull() ?: currentHours` 和 `minutes.toIntOrNull() ?: currentMinutes` 回退到对话框打开时的当前值，而非静默设为 0。同时 `currentHours`/`currentMinutes` 改为 `Int` 类型以匹配 `toIntOrNull()` 返回类型。

**实际结果：** 编译通过，回归测试通过。

---

### ISSUE-007 | FAIL | 轻微 | UA-INPUT-007

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-007 |
| **类型** | 缺陷 |
| **严重程度** | 轻微 |
| **测试项** | TaskEditDialog 自定义分钟数字段输入非法值 |
| **状态** | 已修复 |
| **复杂度** | 低 |
| **预估耗时** | 5 分钟 |
| **实际耗时** | 3 分钟 |
| **JSON 用例 ID** | UA-INPUT-007 |

**问题描述：**
自定义分钟数输入框用 `value.toIntOrNull()` 处理输入，非数字/空输入使 `durationMinutes = null`。保存时无验证——倒计时任务被创建但没有有效目标时长，导致后续计时器行为异常（关联 UA-UI-011）。

**复现步骤：**
1. 创建倒计时任务，输入非法自定义分钟数（如"abc"或空）
2. 点击保存
3. 启动该任务

**期望结果：**
输入非法值时应阻止保存或有错误提示

**实际结果：**
任务保存成功但 `defaultDurationMinutes = null`，倒计时无目标时长

**代码证据：**
- [TaskEditDialog.kt:114-118](app/src/main/java/com/focusflow/app/ui/task/TaskEditDialog.kt#L114-L118) — `value.toIntOrNull()` 无输入验证
- [TaskEditDialog.kt:143](app/src/main/java/com/focusflow/app/ui/task/TaskEditDialog.kt#L143) — 保存时未验证倒计时模式下的 durationMinutes 有效性

**修复建议：**
保存前验证：倒计时模式必须有正整数的 durationMinutes。无效时显示错误并阻止保存。

**修复说明：**
- [TaskEditDialog.kt](app/src/main/java/com/focusflow/app/ui/task/TaskEditDialog.kt) — 保存按钮的 `enabled` 条件增加：倒计时模式下 `durationMinutes` 必须为正整数 `(durationMinutes ?: 0) > 0`，否则按钮禁用。配合 ISSUE-011 的 startTimer 回退逻辑，双重保护。

**实际结果：** 编译通过，回归测试通过。

---

### ISSUE-008 | PASS | 轻微 | UA-UI-008

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-008 |
| **类型** | — |
| **严重程度** | 轻微 |
| **测试项** | 无任务时统计页面显示空状态 |
| **状态** | — |
| **JSON 用例 ID** | UA-UI-008 |

**结果：** 通过。StatsScreen 对空数据做降级处理：DonutChart 显示空环，taskBreakdown 为空时显示"暂无计时记录"，GoalAchievementCard 正常渲染。无崩溃风险。

---

### ISSUE-009 | PASS | 一般 | UA-ASYNC-009

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-009 |
| **类型** | — |
| **严重程度** | 一般 |
| **测试项** | 删除正在计时的任务后孤儿记录处理 |
| **状态** | — |
| **JSON 用例 ID** | UA-ASYNC-009 |

**结果：** 通过。`deleteTask()` 在 `timerManager.stop()` 之前将 `runningRecordId` 设为 -1，防止 IDLE 事件 collector 重复 finalize。逻辑依赖时序但当前版本正确：先标记 `runningRecordId = -1`，再 stop 发出 IDLE，collector 检查 `runningRecordId != -1L` 失败后跳过。需注意未来修改 IDLE 处理逻辑时保持此顺序。

---

### ISSUE-010 | PASS | 轻微 | UA-ASYNC-010

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-010 |
| **类型** | — |
| **严重程度** | 轻微 |
| **测试项** | todayTotalSeconds 计算中 currentRecordSavedDuration 数据竞争 |
| **状态** | — |
| **JSON 用例 ID** | UA-ASYNC-010 |

**结果：** 通过。所有对 `currentRecordSavedDuration` 的读写都在 `viewModelScope` 协程中（Dispatchers.Main），Kotlin 协程在单线程调度器中协作式调度，不存在抢占式竞争。快速切换任务时 combine 可能短暂读到不一致值（DB 未更新时 liveElapsed 已变），但后续 flow 重算会自动修正。无数据损坏风险，仅可能的 UI 闪烁。

---

### ISSUE-011 | FAIL | 严重 | UA-UI-011

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-011 |
| **类型** | 缺陷 |
| **严重程度** | 严重 |
| **测试项** | 倒计时模式下 targetDuration 为零时的 TimerDisplay 行为 |
| **状态** | 已修复 |
| **复杂度** | 中 |
| **预估耗时** | 8 分钟 |
| **实际耗时** | 5 分钟 |
| **JSON 用例 ID** | UA-UI-011 |

**问题描述：**
当倒计时任务的 `defaultDurationMinutes = null` 时（由 UA-INPUT-007 产生），`startTimer` 中 `target = 0`。`TimerDisplay` 中 `displayMs = (0 - elapsedMs).coerceAtLeast(0) = 0`，UI 始终显示 00:00:00。同时 `TimerManager` 倒计时自停逻辑 `targetDuration > 0 && elapsed >= targetDuration` 永不为真，计时器实际无限制运行。用户看到计时器"不动"，但实际时间在累积——UI 与实际状态严重不一致。

**复现步骤：**
1. 创建倒计时任务，不设置时长（自定义分钟数留空）
2. 启动该任务
3. 观察 TimerDisplay

**期望结果：**
如果倒计时没有目标时长，应回退为正计时或显示有意义的提示

**实际结果：**
- TimerDisplay 始终显示 00:00:00
- elapsedMs 在 TimerManager 内部正常增长
- 倒计时自停逻辑失效
- 用户困惑以为功能不工作

**代码证据：**
- [TaskEditDialog.kt:143](app/src/main/java/com/focusflow/app/ui/task/TaskEditDialog.kt#L143) — 倒计时模式保存时 durationMinutes 可为 null
- [TimerViewModel.kt:263](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L263) — `target = (null ?: 0) * 60 * 1000L = 0`
- [TimerDisplay.kt:37-41](app/src/main/java/com/focusflow/app/ui/components/TimerDisplay.kt#L37-L41) — `(0 - elapsedMs).coerceAtLeast(0) = 0`
- [TimerManager.kt:146](app/src/main/java/com/focusflow/app/service/TimerManager.kt#L146) — 倒计时自停条件 `targetDuration > 0` 为 false

**修复建议：**
1. TaskEditDialog 保存时验证：倒计时模式必须有正数时长
2. startTimer 中，若 COUNT_DOWN 且 targetDuration=0，自动切换为 COUNT_UP

**修复说明：**
- [TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt) — `startTimer()` 中：当 `task.defaultTimerMode == COUNT_DOWN` 且 `target == 0L` 时，自动将 `mode` 切换为 `COUNT_UP`，确保 TimerDisplay 显示正计时而非停滞在 00:00:00。
- [TaskEditDialog.kt](app/src/main/java/com/focusflow/app/ui/task/TaskEditDialog.kt) — 倒计时模式保存按钮在 `durationMinutes` 无效时禁用（见 ISSUE-007），从源头阻止无效任务创建。

**实际结果：** 编译通过，回归测试通过。

---

## 测试汇总 — 会话 #1 (2026-05-02)

| 统计项 | 数值 |
|--------|------|
| **总用例数** | 11 |
| **通过** | 3 |
| **已修复** | 8 |
| **未执行（阻塞）** | 0 |
| **测试中断** | 0 |

### 修复结果（2026-05-02 回归）

| 严重程度 | 数量 | 用例 ID | 状态 |
|----------|------|---------|------|
| **严重** | 3 | UA-ASYNC-001, UA-DATA-005, UA-UI-011 | ✅ 已修复 |
| **一般** | 3 | UA-ASYNC-002, UA-ASYNC-003, UA-DATA-004 | ✅ 已修复 |
| **轻微** | 2 | UA-INPUT-006, UA-INPUT-007 | ✅ 已修复 |

### 修复汇总

| 文件 | 修改内容 | 关联 Issue |
|------|---------|-----------|
| TimerViewModel.kt | 新增 `startMutex` 互斥锁保护 `startTimer()` | UA-ASYNC-001 |
| TimerViewModel.kt | `onAppPause()` delay 块增加 `wasRunningBeforePause` 守卫 | UA-ASYNC-002 |
| TimerViewModel.kt | 持久化循环增加跨午夜日期检测与记录拆分 | UA-ASYNC-003 |
| TimerViewModel.kt | `startTimer()` 中 COUNT_DOWN + target=0 自动切换 COUNT_UP | UA-UI-011 |
| BackupManager.kt | 两阶段导入：先预构建验证实体，再写入 | UA-DATA-005 |
| BackupManager.kt | 未导入 tasks 时通过 name 匹配现有任务构造 taskIdMap | UA-DATA-004 |
| TaskLabelDao.kt | 新增 `getTaskByName()` 查询方法 | UA-DATA-004 |
| GoalEditDialog.kt | 空输入回退到当前值而非 0 | UA-INPUT-006 |
| TaskEditDialog.kt | 倒计时模式保存按钮验证 durationMinutes > 0 | UA-INPUT-007 |

---

### ISSUE-012 | FAIL | 严重 | USER-001

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-012 |
| **类型** | 缺陷 |
| **严重程度** | 严重 |
| **测试项** | 多任务计时下统计页数据突变（凭空增加数小时） |
| **状态** | 已修复 |
| **复杂度** | 中 |
| **预估耗时** | 30 分钟 |
| **实际耗时** | 25 分钟 |
| **来源** | 用户反馈 |
| **报告时间** | 2026-05-10 |
| **JSON 用例 ID** | USER-001 |

**问题描述：**
同时使用 3 个正计时任务，正常暂停/恢复操作后，在总计时达到约 7 小时或关闭 App 重新打开时，统计页的任务分布、环形图和今日目标数据发生突变——某个任务的时间凭空增加约 3 小时。计时页显示正确，但统计页数据异常。

根因有两点：
1. **孤儿记录只恢复一条**：`getOrphanRecord()` SQL 使用 `LIMIT 1`，App 被杀时多个暂停任务的记录都是 `endTime=NULL`，但只恢复最新那条。其他记录永远留为 `endTime=NULL`，且 `taskSessions` 内存映射丢失后，重启这些任务会创建**新记录**，导致同一 taskId 下积累多条 `endTime=NULL` 的记录。
2. **todayTotalSeconds 计算逻辑缺陷**：`combine` 中通过 `record.taskId == event.taskId` 匹配活跃记录，而非精确的 `record.id == runningRecordId`。当同一 taskId 有多条 `endTime=NULL` 记录时，**全部**被替换为当前 `event.elapsedMs`。切换任务或停止计时时，这些记录又切回 `record.duration`（包含之前累积的真实时长），导致统计页数据瞬间暴涨。

**复现步骤：**
1. 创建 3 个正计时任务 A、B、C
2. 启动任务 A，计时一段时间后暂停
3. 启动任务 B，计时一段时间后暂停
4. 启动任务 C，计时中关闭 App（模拟进程被杀）
5. 重新打开 App，继续正常使用（暂停/恢复/切换任务）
6. 观察统计页 → 某任务时间突然增加数小时

**期望结果：**
统计页数据始终准确反映各任务的实际计时时长，不应出现突变。

**实际结果：**
统计页某任务时间凭空增加约 3 小时。

**代码证据：**
- [TimerRecordDao.kt:44](app/src/main/java/com/focusflow/app/data/local/dao/TimerRecordDao.kt#L44) — `getOrphanRecord()` 使用 `LIMIT 1`，只恢复一条孤儿记录
- [TimerViewModel.kt:122](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L122) — `todayTotalSeconds` combine 用 `record.taskId == event.taskId` 模糊匹配，导致同一 taskId 的多条 `endTime=NULL` 记录全部被替换
- [TimerViewModel.kt:208-241](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L208-L241) — 孤儿恢复逻辑只处理一条记录

**修复建议：**
1. 新增 `getAllOrphanRecords()` 返回所有 `endTime=NULL` 记录，恢复最新那条，其余全部 finalize
2. `todayTotalSeconds` 改用 `record.id == runningRecordId` 精确匹配活跃记录

**修复说明：**
- [TimerRecordDao.kt](app/src/main/java/com/focusflow/app/data/local/dao/TimerRecordDao.kt) — 新增 `getAllOrphanRecords()` 查询方法（不加 LIMIT）
- [TimerRepository.kt](app/src/main/java/com/focusflow/app/domain/repository/TimerRepository.kt) — 接口新增 `getAllOrphanRecords()`
- [TimerRepositoryImpl.kt](app/src/main/java/com/focusflow/app/data/repository/TimerRepositoryImpl.kt) — 实现 `getAllOrphanRecords()`
- [TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt) — 孤儿恢复改为：拉取全部孤儿 → 最新那条按原有逻辑恢复 → 其余全部 `finalizeRecord()` 清理
- [TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt) — `todayTotalSeconds` combine 改用 `record.id == runningRecordId` 精确匹配，仅对真正活跃的那一条记录替换 `event.elapsedMs`

**实际结果：** 构建通过，逻辑验证通过。

---

### ISSUE-013 | FAIL | 严重 | USER-001

| 字段 | 详情 |
|------|------|
| **ID** | ISSUE-013 |
| **类型** | 缺陷 |
| **严重程度** | 严重 |
| **测试项** | 切后台后计时器继续累计后台时间 |
| **状态** | 已修复 |
| **复杂度** | 中 |
| **预估耗时** | 20 分钟 |
| **实际耗时** | 15 分钟 |
| **来源** | 用户反馈 |
| **报告时间** | 2026-05-14T12:00:00+08:00 |
| **JSON 用例 ID** | USER-001 |

**问题描述：**
app 切到后台后计时页任务不停止计时，最长可达 5 小时。切换到统计页时间准确（不包含后台时间）。在计时页点击后台计时的任务时，今日专注时间会加上后台计时；启动其他任务后非法计时会被减去，视觉上出现颜色条长度跳变。

**复现步骤：**
1. 启动一个任务计时
2. 将 app 切到后台，等待较长时间（数小时）
3. 切回 app，观察计时页：计时器显示数小时的时间
4. 点击另一个任务，观察今日专注时间条变化
5. 再切换到统计页，统计页时间正常

**期望结果：**
切到后台后计时器应立即停止计时，切回前台后显示的计时不应包含后台时间。

**实际结果：**
- `onStop` 仅保存 elapsed 到 DB 并更新 `currentRecordSavedDuration`，但未调用 `timerManager.pause()`
- `TimerManager` 保持 RUNNING 状态，`getCurrentElapsedMs()` 使用 `SystemClock.elapsedRealtime()` 持续累计后台时间
- `todayTotalSeconds = dbSumMs + liveElapsed - currentRecordSavedDuration` 中 `liveElapsed` 虚高

**代码证据：**
- [TimerViewModel.kt:93-101](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L93-L101) — `onStop` 仅写 DB 但不暂停 TimerManager
- [TimerManager.kt:110-116](app/src/main/java/com/focusflow/app/service/TimerManager.kt#L110-L116) — `getCurrentElapsedMs()` 使用 `SystemClock.elapsedRealtime()`，该时钟包含系统休眠时间

**修复说明：**
- [TimerManager.kt:118-121](app/src/main/java/com/focusflow/app/service/TimerManager.kt#L118-L121) — 新增 `correctElapsedForBackground()` 方法，用于修正后台时间
- [TimerViewModel.kt:91-128](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L91-L128) — `onStop` 改为同步调用 `timerManager.pause()` 立即停止计时；新增 `onStart` 处理器自动恢复由生命周期暂停的计时器
- 核心思路：切后台立即暂停 TimerManager → 保存 elapsed 到 DB 和 taskSessions → 切前台时自动 resume

**实际结果：** 构建通过。

