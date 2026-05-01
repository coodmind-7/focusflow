# Step 14: 统计页双闪修复 + 实时数据显示 ✅ 已完成

> 实施日期：2026-05-01

解决切换到统计页时图表闪烁两次的问题，同时保证数据实时显示且不丢失。

---

## 问题

切换统计 Tab 时图表渲染两次：

1. **第一次渲染**：Room Flow 初始发射，此时 DB 中运行中的记录 `duration` 为旧值（或 0）
2. `onEnter()` 写库更新 `duration` → Room 二次发射
3. **第二次渲染**：图表数据变化，用户看到闪变

根因：`onEnter()` 的 DB 写入通过 Room 反应式流反馈回同一个 `combine` 链，形成"读→写→再读→再渲染"的循环。

---

## 修复方案

将实时计时数据直接纳入 `combine` 链，`onEnter()` 仍负责持久化，但通过 `distinctUntilChanged` 拦截写库触发的无效重渲染。

### 核心公式

```
实时统计 = DB已存时长 + (timerEvents.elapsedMs - 已存时长) / 1000
         = DB已存时长 + delta
```

写库后 `_runningRecordSavedMs` 同步更新为最新值 → `delta = 0` → 两次 combine 计算结果一致 → `distinctUntilChanged` 拦截。

---

## 改动概览

| 文件 | 动作 | 说明 |
|------|------|------|
| `StatsViewModel.kt` | **重写** | 5 处关键修改 |

### 5 处修改明细

1. **`timerManager.timerEvents` 纳入内层 `combine`**（第 3 个输入源）
   - 原来：`combine(statsFlow, goalsFlow)`
   - 现在：`combine(statsFlow, goalsFlow, timerEvents)`
   - 效果：一切换到统计页就能看到实时耗时，无需等待 DB 写入

2. **新增 `_runningRecordSavedMs: MutableStateFlow<Long>`**
   - 记录 DB 中运行记录的 `duration`，用于防止 combine 中 DB 数据与 live 数据重复累加

3. **新增 `mergeLiveElapsed()` 方法**
   - 仅在 `RUNNING` 或 `PAUSED` 状态下工作
   - 仅在所选日期范围包含今天时工作
   - 公式：`deltaSeconds = liveSeconds - savedSeconds`
   - 将 delta 加到对应 task 的切片统计中

4. **`onEnter()` 三段式快照-写库-同步**
   ```
   onEnter() → 快照 oldDuration → updateRecord(currentElapsed) → savedMs = currentElapsed
   ```
   - 写库前：combine 用 `oldDuration` 修正 → 结果正确
   - 写库后：combine 用 `currentElapsed` 修正 → delta=0 → 结果相同
   - `distinctUntilChanged()` 拦截住第二次相同的发射

5. **`.distinctUntilChanged()` 添加在 `stateIn` 之前**
   - `StatsUiState` 是 data class，`==` 比较所有字段
   - 写库触发的二次发射因结果相同被过滤
   - `timerEvents` 每 200ms tick，但 `elapsedMs / 1000` 每秒才变一次，其余 4/5 的 tick 也被过滤

---

## 数据刷新流程（修改后）

```
切换统计 tab
    │
    ├─→ combine(Room, goals, timerEvents) 立即发射（含实时数据）──→ UI 渲染 ✓ 只此一次
    │
    └─→ ON_RESUME → onEnter()
         ├─ 快照 savedMs
         ├─ updateRecord() 写库（持久化）
         └─ 更新 savedMs → combine 重算 → 结果相同 → distinctUntilChanged 拦截
                                                                  ↑
                  1h 定时保存 ────────────────────────────────────┤
                  后台 onStop ────────────────────────────────────┘
```

---

## 数据安全

- **`onEnter()` 仍然写库**：每次切到统计页都会将当前耗时写入 DB
- **1 小时间隔保存**（`TimerViewModel`）：计时运行中每小时自动写一次
- **后台 `onStop` 保存**（`TimerViewModel`）：App 进入后台时自动写一次
- **崩溃/杀进程**：最坏丢失最后一次写库之后到崩溃之间的增量（最多 1 小时 / 1 次 session）
