# 项目长期记忆

## 项目概况
- **项目名**：shiftcalendar（排班日历 App）
- **包名**：com.example.myapplication
- **平台**：Android（真我 GT8 Pro / ColorOS 为主要测试设备）

## 关键技术决策
- 勿扰模式使用系统 `NotificationManager.setInterruptionFilter()`，不用 AudioManager（会触发 ColorOS 硬锁媒体音量）
- AlarmManager 用 `setAlarmClock`（最高优先级，Doze 下也精确触发），不用 `setExactAndAllowWhileIdle`（Doze 下延迟数十秒）
- 杀进程后保活方案：双通道闹钟（ForegroundService + Broadcast），ColorOS 会拦截 Service 冷启动但不拦截广播

## ColorOS 坑点
- `OplusAppStartupManager` 会拦截杀进程后的 ForegroundService/Activity 冷启动（日志：`prevent start ... Type ssfa`）
- `setExactAndAllowWhileIdle` 在 Doze 下会延迟到 maintenance window
- 厂商推送通道（系统级 `com.heytap.mcs`）vs 本地 NotificationManager 本质不同
- 需要引导用户设白名单：电池优化=不限制 + 自启动=允许

## 文件结构
- `DndForegroundService.java` — 按需服务，ACTION_DND_OFF 时拉起关勿扰
- `DndAlarmReceiver.java` — 常规勿扰广播接收器（START/END + ENABLE_DND extra）
- `NapAlarmReceiver.java` — 午睡勿扰广播接收器
- `AlarmSettingsActivity.java` — 勿扰设置入口
- `MainActivity.setDndAlarms()` — 闹钟调度中心
- `DeviceCompat.java` — 白名单引导工具类
- `DndCloseActivity.java` — 透明 Activity（备用关闭路径）
- `DndTimeoutReceiver.java` — 通知超时兜底
