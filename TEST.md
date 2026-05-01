# FocusFlow 测试报告

> 测试日期: 2026-04-30 | JDK: LibericaJDK-17 | 构建: Gradle 8.4

---

## 一、测试环境搭建

### 1.1 新增测试依赖 (`app/build.gradle.kts`)

```kotlin
// 单元测试
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.9")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("androidx.room:room-testing:2.6.1")
testImplementation("androidx.arch.core:core-testing:2.2.0")

// 集成测试
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("com.google.dagger:hilt-android-testing:2.50")
androidTestImplementation("androidx.room:room-testing:2.6.1")

// Compose UI 测试
androidTestImplementation(platform(libs.compose.bom))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.compose.ui:ui-test-manifest")
```

### 1.2 Android API Mock 配置

```kotlin
testOptions {
    unitTests.isReturnDefaultValues = true  // 解决 SystemClock.elapsedRealtime() 未 mock 问题
}
```

### 1.3 Room Schema 导出配置

```kotlin
// build.gradle.kts
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
sourceSets {
    getByName("androidTest").assets.srcDirs("$projectDir/schemas")
}
```

### 1.4 测试目录结构

```
app/src/
├── test/java/com/focusflow/app/
│   ├── service/TimerManagerTest.kt          (P0, 24 用例)
│   ├── data/backup/BackupManagerTest.kt     (P1, 8 用例)
│   ├── data/local/entity/EntityMapperTest.kt(P2, 10 用例)
│   ├── domain/usecase/timer/
│   │   ├── GetStatsUseCaseTest.kt           (P1, 8 用例)
│   │   └── RecordDailyGoalUseCaseTest.kt    (P2, 5 用例)
│   └── ui/stats/StatsViewModelTest.kt       (P3, 9 用例)
└── androidTest/java/com/focusflow/app/
    ├── DaoTest.kt                           (P2, 24 用例, 需设备)
    └── MigrationTest.kt                     (P0, 4 用例, 需设备)
```

---

## 二、测试用例清单 (共 92 个)

### P0 - TimerManager 状态机 (24 用例)

| ID | 测试场景 | 状态 |
|----|---------|------|
| TM-01 | IDLE → start → RUNNING | ✅ PASS |
| TM-02 | start 设置正确的 taskId/name/mode/targetDuration | ✅ PASS |
| TM-03 | start 发射 RUNNING 状态 TimerEvent | ✅ PASS |
| TM-04 | RUNNING 状态下 elapsedMs 递增 | ✅ PASS |
| TM-05 | RUNNING → pause → PAUSED | ✅ PASS |
| TM-06 | pause 冻结 elapsedMs | ✅ PASS |
| TM-07 | pause 返回冻结时的 elapsedMs | ✅ PASS |
| TM-08 | PAUSED → resume → RUNNING | ✅ PASS |
| TM-09 | resume 从暂停点继续计时 | ✅ PASS |
| TM-10 | RUNNING → stop → IDLE | ✅ PASS |
| TM-11 | stop 返回最终 TimerEvent（含 IDLE 状态） | ✅ PASS |
| TM-12 | stop 后 elapsedMs 保持最终值 | ✅ FIXED |
| TM-13 | PAUSED → stop → IDLE | ✅ PASS |
| TM-14 | PAUSED → stop 保留暂停时的 elapsedMs | ✅ PASS |
| TM-15 | IDLE → pause 无效果 | ✅ PASS |
| TM-16 | IDLE → resume 无效果 | ✅ PASS |
| TM-17 | RUNNING → resume 无效果 | ✅ PASS |
| TM-18 | COUNT_DOWN 到达目标 → 自动 stop | ✅ PASS |
| TM-19 | COUNT_DOWN elapsedMs 在目标附近 | ✅ PASS |
| TM-20 | 切换任务：pause 旧任务 → start 新任务 | ✅ PASS |
| TM-21 | 同一任务重复 start 重置 elapsed | ✅ PASS |
| TM-22 | 暂停后 resume 恢复 | ✅ PASS |
| TM-23 | elapsedMs 精度验证 | ✅ FIXED |
| TM-24 | 多次 pause/resume 累加正确 | ✅ FIXED |

### P0 - Room 迁移 (4 用例, 需设备)

| ID | 测试场景 | 状态 |
|----|---------|------|
| MG-01 | v1→v2: 添加 defaultTimerMode / defaultDurationMinutes 列 | ✅ PASS |
| MG-02 | v2→v3: 创建 daily_goals 表 | ✅ PASS |
| MG-03 | v3→v4: 添加 taskName / taskColor 列到 timer_records | ✅ PASS |
| MG-04 | v1→v4 全链路数据完整性 | ✅ PASS |

### P1 - GetStatsUseCase (8 用例)

| ID | 测试场景 | 状态 |
|----|---------|------|
| UC-01 | 按 taskId 分组、使用 taskName 快照 | ✅ PASS |
| UC-01b | taskName 为空显示"已删除"兜底文案 | ✅ PASS |
| UC-02 | duration ≤ 0 的记录被过滤 | ✅ PASS |
| UC-03 | 按秒数降序排列 | ✅ PASS |
| UC-04 | totalSeconds 正确求和 | ✅ PASS |
| UC-05 | 单任务多记录合并 | ✅ PASS |
| UC-06 | 空记录返回 EMPTY stats | ✅ PASS |
| — | 多任务混合统计 | ✅ PASS |

### P1 - BackupManager (8 用例)

| ID | 测试场景 | 状态 |
|----|---------|------|
| BK-01 | 全量导出（Tasks + Records + Goals + Settings） | ✅ PASS |
| BK-02 | 选择性导出（排除 Records 和 Goals） | ✅ PASS |
| BK-03 | 空数据导出有效 JSON | ✅ PASS |
| BK-04 | 导入时 ID 重映射 | ✅ FIXED |
| BK-05 | 选择性导入 | ✅ PASS |
| BK-06 | 空数组不崩溃 | ✅ FIXED |
| BK-07 | DailyGoal.achieved 布尔值保留 | ✅ FIXED |
| BK-08 | TimerMode 序列化往返 | ✅ PASS |

### P2 - RecordDailyGoalUseCase (5 用例)

| ID | 测试场景 | 状态 |
|----|---------|------|
| RG-01 | 首次达成返回 true | ✅ PASS |
| RG-02 | 未达成返回 false | ✅ PASS |
| RG-03 | 已达成再次调用返回 false | ✅ PASS |
| RG-04 | 已达成后 achieved 标志持久保留 | ✅ PASS |
| RG-05 | goalSeconds=0 时任意正数即达成 | ✅ PASS |

### P2 - EntityMapper (10 用例)

| ID | 测试场景 | 状态 |
|----|---------|------|
| EM-01 | TaskLabelEntity → TaskLabel 全字段映射 | ✅ PASS |
| EM-02 | 无效 TimerMode 字符串兜底为 COUNT_UP | ✅ PASS |
| EM-03 | null durationMinutes 正确传递 | ✅ PASS |
| EM-04 | TaskLabel → TaskLabelEntity 全字段映射 | ✅ PASS |
| EM-05 | toEntity null durationMinutes | ✅ PASS |
| EM-06 | TaskLabel round-trip 无损 | ✅ PASS |
| EM-07 | TimerRecordEntity → TimerRecord 全字段映射 | ✅ PASS |
| EM-08 | null endTime 正确传递 | ✅ PASS |
| EM-09 | TimerRecord → TimerRecordEntity 全字段映射 | ✅ PASS |
| EM-10 | TimerRecord round-trip 无损 | ✅ PASS |

### P2 - Room DAO (24 用例, 需设备)

| ID | 测试场景 | 状态 |
|----|---------|------|
| DB-01 | TaskLabelDao 插入并查询，验证排序 | ✅ PASS |
| DB-02 | TaskLabelDao 更新任务名称/颜色 | ✅ PASS |
| DB-03 | TaskLabelDao 删除任务 | ✅ PASS |
| DB-04 | TaskLabelDao 重排任务顺序 | ✅ FIXED |
| DB-05 | TaskLabelDao getMaxSortOrder | ✅ PASS |
| DB-06 | TaskLabelDao getAllTasksSuspend | ✅ PASS |
| DB-07 | TaskLabelDao deleteAllTasks | ✅ PASS |
| DB-08 | TaskLabelDao insertAllTasks 批量 | ✅ PASS |
| DB-09 | TimerRecordDao 按日期查询 | ✅ PASS |
| DB-10 | TimerRecordDao 日期范围过滤 | ✅ PASS |
| DB-11 | TimerRecordDao getRunningRecord (endTime=null) | ✅ PASS |
| DB-12 | TimerRecordDao updateRecord | ✅ FIXED |
| DB-13 | TimerRecordDao getRecordById 不存在返回 null | ✅ PASS |
| DB-14 | TimerRecordDao getAllRecords 按 startTime ASC | ✅ PASS |
| DB-15 | TimerRecordDao deleteAllRecords | ✅ PASS |
| DB-16 | TimerRecordDao insertAllRecords 批量 | ✅ PASS |
| DB-17 | DailyGoalDao upsert 插入新记录 | ✅ PASS |
| DB-18 | DailyGoalDao upsert 替换同日期记录 | ✅ PASS |
| DB-19 | DailyGoalDao getByDateRange | ✅ PASS |
| DB-20 | DailyGoalDao getTotalAchievedCount | ✅ PASS |
| DB-21 | DailyGoalDao getAllGoals 升序 | ✅ PASS |
| DB-22 | DailyGoalDao getAllDesc 降序 | ✅ PASS |
| DB-23 | DailyGoalDao deleteAllGoals | ✅ PASS |
| DB-24 | DailyGoalDao insertAllGoals 批量 | ✅ PASS |

### P3 - StatsViewModel (9 用例)

| ID | 测试场景 | 状态 |
|----|---------|------|
| SV-01 | DAY 周期显示单日 | ✅ FIXED |
| SV-02 | navigatePrev 到前一天 | ✅ FIXED |
| SV-03 | today 时 navigateNext 禁用 | ✅ PASS |
| SV-04 | WEEK 周期显示周一至周日范围 | ✅ FIXED |
| SV-05 | MONTH 周期显示年月格式 | ✅ FIXED |
| SV-06 | achievedDaysInRange 正确计数 | ✅ FIXED |
| SV-07 | 空 goals 返回 0 achieved | ✅ PASS |
| SV-08 | 切换 period 重新计算 | ✅ FIXED |
| SV-09 | onEnter 计时未运行时不操作 | ✅ PASS |

---

## 三、当前测试状态

### 执行统计

| 层级 | 总数 | PASS | FAIL | 待运行 |
|------|------|------|------|--------|
| 单元测试 (test/) | 64 | 64 | 0 | 0 |
| 集成测试 (androidTest/) | 28 | 28 | 0 | 0 |

### StatsViewModel 6 个用例修复（2026-04-30）

**根因**: 测试中 `stateIn(SharingStarted.WhileSubscribed(5000))` 需要一个活跃 subscriber 才会触发上游 Flow 计算。测试仅通过 `.value` 读取 StateFlow，不会创建 subscription，导致永远返回初始值 `StatsUiState()`（`displayDate=""` 等空字段）。

**修复**: 在每个需要读取 UI 状态的测试中添加 `backgroundScope.launch { viewModel.uiState.collect {} }`，然后 `advanceUntilIdle()` 等待 Flow 发射。

---

### 已修复的问题

#### 修复 1：TimerManager 时间精度测试 (TM-04, TM-23, TM-24)

**根因**: `SystemClock.elapsedRealtime()` 硬编码在 `TimerManager` 中，单元测试 `isReturnDefaultValues=true` 使其始终返回 0L。

**修复方案**: 抽象 `TimeProvider` 接口作为可注入依赖。

| 操作 | 文件 |
|------|------|
| 新建 `TimeProvider` 接口 + `SystemTimeProvider` 实现 | [TimeProvider.kt](app/src/main/java/com/focusflow/app/service/TimeProvider.kt) |
| `TimerManager` 构造函数注入 `TimeProvider` | [TimerManager.kt](app/src/main/java/com/focusflow/app/service/TimerManager.kt) |
| Hilt Module 添加 `TimeProvider → SystemTimeProvider` 绑定 | [AppModule.kt](app/src/main/java/com/focusflow/app/di/AppModule.kt) |
| 测试使用 `System.nanoTime()/1_000_000` 作为 Fake TimeProvider | [TimerManagerTest.kt](app/src/test/java/com/focusflow/app/service/TimerManagerTest.kt) |
| `stop()` 中补充 `savedElapsedMs = finalElapsed`（修复 TM-12） | [TimerManager.kt](app/src/main/java/com/focusflow/app/service/TimerManager.kt) |

#### 修复 2：BackupManager import 测试 (BK-04, BK-06, BK-07)

**根因**: BK-04/06/07 的 `importFromJson()` 默认 `importSettings=true` 会调用 `userPreferences.setDailyGoalSeconds()`，但 `userPreferences` 是严格 mock（`mockk()`），未 stub 导致 MockKException。此外 MockK 1.13.9 中 `coVerify { capture(slot) }` 配合 relaxed mock 存在已知问题。

**修复方案**: `userPreferences` 改为 relaxed mock，capture 在 `coEvery` 阶段执行而非 `coVerify`。

| 操作 | 文件 |
|------|------|
| `userPreferences` 从 `mockk()` 改为 `mockk(relaxed = true)` | [BackupManagerTest.kt](app/src/test/java/com/focusflow/app/data/backup/BackupManagerTest.kt) |
| BK-04 capture 从 `coVerify` 迁移到 `coEvery` 阶段 | 同上 |
| BK-07 capture 从 `coVerify` 迁移到 `coEvery` 阶段 | 同上 |

#### 修复 3：代码审查 #1 — TimerViewModel runBlocking 阻塞主线程（2026-04-30）

**根因**: `DefaultLifecycleObserver.onStop()` 在主线程回调中使用 `runBlocking` 执行数据库更新，可能导致 ANR。

**修复方案**: `runBlocking` 替换为 `viewModelScope.launch`，因为 `viewModelScope` 在 `onStop()` 时仍然活跃（只在 `onCleared()` 中取消）。

| 操作 | 文件 |
|------|------|
| `runBlocking { updateDuration(...) }` → `viewModelScope.launch { updateDuration(...) }` | [TimerViewModel.kt](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt) |

#### 修复 4：代码审查 #3 — BackupManager 导入事务原子性（2026-04-30）

**根因**: `deleteAllTasks()` 和逐条 `insertTask()` 不在同一事务中，导入中断会导致数据全部丢失。

**修复方案**: 在 DAO 层添加 `@Transaction` 标注的 `replaceAll*` 方法（一次性完成删除+插入），移除 BackupManager 中的 `FocusFlowDatabase` 直接依赖。

| 操作 | 文件 |
|------|------|
| 新增 `replaceAllTasks()`（@Transaction，返回新 ID 列表） | [TaskLabelDao.kt](app/src/main/java/com/focusflow/app/data/local/dao/TaskLabelDao.kt) |
| 新增 `replaceAllRecords()`（@Transaction） | [TimerRecordDao.kt](app/src/main/java/com/focusflow/app/data/local/dao/TimerRecordDao.kt) |
| 新增 `replaceAllGoals()`（@Transaction） | [DailyGoalDao.kt](app/src/main/java/com/focusflow/app/data/local/dao/DailyGoalDao.kt) |
| `importFromJson()` 改用 `replaceAll*` 替代 `deleteAll + insert*` | [BackupManager.kt](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt) |
| 测试用例同步更新为验证 `replaceAll*` 调用 | [BackupManagerTest.kt](app/src/test/java/com/focusflow/app/data/backup/BackupManagerTest.kt) |

#### 修复 5：StatsViewModel 测试缺少 Flow subscriber（2026-04-30）

**根因**: `stateIn(SharingStarted.WhileSubscribed(5000))` 需要活跃 subscriber 才触发上游 Flow，测试直接 `.value` 读取不会创建订阅。

**修复方案**: 6 个失败测试中添加 `backgroundScope.launch { viewModel.uiState.collect {} }` 创建活跃订阅。

| 操作 | 文件 |
|------|------|
| SV-01/02/04/05/06/08 测试方法中添加 collector | [StatsViewModelTest.kt](app/src/test/java/com/focusflow/app/ui/stats/StatsViewModelTest.kt) |
| SV-06 `totalDaysInRange` 期望值 4→1（DAY 周期仅 1 天） | 同上 |

#### 修复 6：代码审查 #2 — StatsViewModel 架构分层修复（2026-04-30）

**根因**: `StatsViewModel.onEnter()` 直接注入 `TimerRepository` 并调用 `getRunningRecord()` / `updateRecord()`，破坏了 Clean Architecture 分层（ViewModel 应只依赖 UseCase）。

**修复方案**: 新建 `SyncRunningRecordUseCase` 封装该逻辑，ViewModel 改为依赖 UseCase。

| 操作 | 文件 |
|------|------|
| 新建 `SyncRunningRecordUseCase` | [SyncRunningRecordUseCase.kt](app/src/main/java/com/focusflow/app/domain/usecase/timer/SyncRunningRecordUseCase.kt) |
| ViewModel 移除 `TimerRepository` 注入，改用 UseCase | [StatsViewModel.kt](app/src/main/java/com/focusflow/app/ui/stats/StatsViewModel.kt) |
| 测试同步更新 mock | [StatsViewModelTest.kt](app/src/test/java/com/focusflow/app/ui/stats/StatsViewModelTest.kt) |

#### 修复 7：MigrationTest 重写（2026-04-30）

**根因**: 原 `MigrationTest` 依赖 `MigrationTestHelper` 需要 Room schema JSON 文件（`exportSchema=true` 时自动生成），但旧版本（v1/v2/v3）schema 从未导出。且反引号测试方法名中的空格导致 D8 DEX 编译失败（DEX 版本 < 040 不支持类名含空格）。

**修复方案**: ① 用 `SQLiteDatabase` + 手动执行迁移 SQL 重写所有 MigrationTest 方法，移除对 `MigrationTestHelper` 的依赖；② 将所有 androidTest 方法名中的空格替换为下划线。

| 操作 | 文件 |
|------|------|
| 重写全部 4 个迁移测试方法（手动 SQLite + execSQL） | [MigrationTest.kt](app/src/androidTest/java/com/focusflow/app/MigrationTest.kt) |
| 所有反引号方法名空格→下划线 | [MigrationTest.kt](app/src/androidTest/java/com/focusflow/app/MigrationTest.kt), [DaoTest.kt](app/src/androidTest/java/com/focusflow/app/DaoTest.kt) |

#### 修复 8：androidTest 编译修复（2026-04-30）

**根因**: ① Compose UI 测试依赖缺少版本号（BOM 未覆盖 androidTest 配置）；② `exportSchema=false` 导致 Room 不生成 schema 文件。

**修复方案**: ① 为 androidTest 添加 `implementation(platform(libs.compose.bom))`；② 启用 `exportSchema=true` + 配置 `ksp { arg("room.schemaLocation", ...) }`。

| 操作 | 文件 |
|------|------|
| 添加 BOM platform 到 androidTest | [build.gradle.kts](app/build.gradle.kts) |
| 启用 Room schema 导出 + 配置 KSP + sourceSets | [build.gradle.kts](app/build.gradle.kts), [FocusFlowDatabase.kt](app/src/main/java/com/focusflow/app/data/local/FocusFlowDatabase.kt) |

#### 修复 9：DaoTest 两个用例修复（2026-04-30）

**根因**: ① `reorderTasks_updates_all_sort_orders` — `getAllTasks()` 返回 `ORDER BY sortOrder ASC`，重排后 sortOrder 较小者在前，原断言 `tasks[0].sortOrder == 1` 不成立；② `updateRecord_changes_duration_and_endTime` — `assertEquals(500, endTime)` 中 500 是 `Int`，而 `endTime` 字段类型为 `Long`，类型不匹配。

**修复方案**: ① 交换断言值：`tasks[0].sortOrder == 0`, `tasks[1].sortOrder == 1`；② 使用 Long 字面量 `500L` 和 `400L`。

| 操作 | 文件 |
|------|------|
| 修复 reorderTasks 排序断言 | [DaoTest.kt](app/src/androidTest/java/com/focusflow/app/DaoTest.kt) |
| 修复 updateRecord Long 类型断言 | 同上 |

---

## 四、代码审查发现的问题

| # | 位置 | 问题 | 严重度 | 状态 |
|---|------|------|--------|------|
| 1 | [TimerViewModel.kt:165-168](app/src/main/java/com/focusflow/app/ui/timer/TimerViewModel.kt#L165-L168) | `DefaultLifecycleObserver.onStop()` 中使用 `runBlocking` 阻塞主线程，可能导致 ANR | 高 | ✅ 已修复 |
| 2 | [StatsViewModel.kt:47-52](app/src/main/java/com/focusflow/app/ui/stats/StatsViewModel.kt#L47-L52) | `onEnter()` 直接操作 Repository 而非通过 UseCase，破坏 Clean Architecture 分层 | 中 | ✅ 已修复 |
| 3 | [BackupManager.kt:77-89](app/src/main/java/com/focusflow/app/data/backup/BackupManager.kt#L77-L89) | `deleteAllTasks()` 和逐条 `insertTask()` 不在同一事务中，导入中断会导致数据丢失 | 高 | ✅ 已修复 |
| 4 | [TimerManager.kt:70](app/src/main/java/com/focusflow/app/service/TimerManager.kt#L70) | `SystemClock.elapsedRealtime()` 硬编码，导致单元测试无法精确验证时间 | 中 | ✅ 已修复 |

---

## 五、运行命令

```bash
# 设置 JDK 环境
export JAVA_HOME="/c/Program Files/BellSoft/LibericaJDK-17"
export PATH="$JAVA_HOME/bin:$PATH"

# 运行所有单元测试
cd d:/Professional_Programs/AI/FocusFlow
./gradlew test

# 运行指定集成测试
./gradlew app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.focusflow.app.data.local.MigrationTest --no-daemon

# 运行所有集成测试 (需要模拟器/设备)
./gradlew connectedAndroidTest

# 查看测试报告
open app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 六、待办

- [x] ~~修复 TimerManager 时间精度测试~~（已完成）
- [x] ~~修复 BackupManager import 测试的 MockK capture 问题~~（已完成）
- [x] ~~修复 TimerManager.stop() 未持久化 `savedElapsedMs`~~（已完成）
- [x] ~~修复 StatsViewModel 6 个测试：添加 Flow subscriber 触发 `stateIn` 计算~~（已完成，2026-04-30）
- [x] ~~修复代码审查 #1: `runBlocking` 阻塞主线程~~（已完成，2026-04-30）
- [x] ~~修复代码审查 #3: 导入事务原子性~~（已完成，2026-04-30）
- [x] ~~修复代码审查 #2: `onEnter()` 直接操作 Repository~~（已完成，2026-04-30）
- [x] ~~运行 DaoTest TaskLabelDao 部分（DB-01~DB-08，需设备）~~（已完成，2026-04-30）
- [x] ~~运行 DaoTest TimerRecordDao 部分（DB-09~DB-16，需设备）~~（已完成，2026-04-30）
- [x] ~~运行 DaoTest DailyGoalDao 部分（DB-17~DB-24，需设备）~~（已完成，2026-04-30）
