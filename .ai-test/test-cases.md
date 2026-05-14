# FocusFlow 测试用例表

> 自动生成于 2026-05-02 | 共 11 个测试用例（高置信度 6 + 中置信度 4 + 低置信度 1）

| ID | 用例名称 | 风险模式 | 严重度 | 置信度 | 复杂度 | 预估耗时 |
|----|---------|---------|--------|--------|--------|---------|
| UA-ASYNC-001 | 快速切换任务时计时器状态错乱 | 竞态条件 | 严重 | 高 | 高 | 15min |
| UA-ASYNC-002 | App 前后台快速切换导致计时器意外暂停或继续 | 并发控制缺失 | 一般 | 中 | 中 | 10min |
| UA-ASYNC-003 | 跨午夜计时导致日期归属错误 | 时间依赖 | 一般 | 高 | 中 | 10min |
| UA-DATA-004 | 导入备份数据后任务 ID 映射错乱 | 外部依赖 | 一般 | 高 | 中 | 12min |
| UA-DATA-005 | 导入 JSON 中途失败导致数据半损坏 | 错误处理缺失 | 严重 | 高 | 中 | 8min |
| UA-INPUT-006 | GoalEditDialog 输入无效值静默修正导致用户困惑 | 无输入校验 | 轻微 | 高 | 低 | 5min |
| UA-INPUT-007 | TaskEditDialog 自定义分钟数字段输入非法值 | 无输入校验 | 轻微 | 高 | 低 | 5min |
| UA-UI-008 | 无任务时统计页面显示空状态 | 空值未处理 | 轻微 | 高 | 低 | 5min |
| UA-ASYNC-009 | 删除正在计时的任务后孤儿记录处理 | 状态混乱 | 一般 | 中 | 中 | 8min |
| UA-ASYNC-010 | todayTotalSeconds 计算中 currentRecordSavedDuration 数据竞争 | 状态混乱 | 轻微 | 中 | 中 | 10min |
| UA-UI-011 | 倒计时模式下 targetDuration 为零时的 TimerDisplay 行为 | 空值未处理 | 严重 | 中 | 中 | 8min |

## 代码证据索引

| 用例 ID | 关键文件 | 行号 | 函数 |
|---------|---------|------|------|
| UA-ASYNC-001 | TimerViewModel.kt | 236-290 | startTimer() |
| UA-ASYNC-002 | TimerViewModel.kt | 209-231 | onAppPause() / onAppResume() |
| UA-ASYNC-003 | TimerViewModel.kt | 272-274 | startTimer() (注释) |
| UA-DATA-004 | BackupManager.kt | 74-106 | importFromJson() |
| UA-DATA-005 | BackupManager.kt | 65-125 | importFromJson() |
| UA-INPUT-006 | GoalEditDialog.kt | 82-85 | onClick (confirmButton) |
| UA-INPUT-007 | TaskEditDialog.kt | 114-118 | onValueChange (OutlinedTextField) |
| UA-UI-008 | StatsScreen.kt | 142-158 | StatsScreen() |
| UA-ASYNC-009 | TimerViewModel.kt | 383-406 | deleteTask() |
| UA-ASYNC-010 | TimerViewModel.kt | 112-118 | todayTotalSeconds Flow |
| UA-UI-011 | TimerDisplay.kt | 37-41 | TimerDisplay() |

## 待确认场景（低置信度）

1. **Room WAL 模式并发写入冲突** — 推断依据：AppModule.kt:26 未显式设置 journal mode。建议确认数据库配置并在压力测试中验证。
