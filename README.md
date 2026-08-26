# Cherry Shift Schedule Calendar（排班日历）

一个面向**倒班 / 轮班**人群的 Android 排班日历应用。它根据内置排班数据（班组 × 白班/夜班），在日历上直观展示每天值班班组，并支持**按班组自动设置闹钟**、**定时自动开关勿扰**、**午睡勿扰**等功能。

## 功能特性

- **月历视图**：农历 + 节假日 + 公历，圆角卡片式日格，今日高亮，周末区分
- **班组标签**：每天展示白班/夜班班组彩色标签；8 列网格（周数并入首列），整月一屏放满、无需滚动
- **月度切换**：左右滑动切换月份，一键回到「今天」
- **班组统计**：按所选班组统计当月工作天数
- **班次闹钟**：根据班组与班次自动计算未来一周需响铃的日期，一键调起系统闹钟
- **勿扰自动化**：白班/夜班可设不同勿扰时段，到点自动开关（含跨天）
- **午睡勿扰**：设定时长临时勿扰，结束自动关闭
- **国产 ROM 兼容**：针对 realme/OPPO/vivo/小米/华为做防杀进程与白名单引导

## 技术栈

- 语言：Java
- 构建：Gradle 8.11.1 / AGP 8.9.1（Kotlin DSL）
- minSdk 26 / targetSdk 35
- 农历库：[cn.6tail:lunar](https://github.com/6tail/lunar)
- UI：AppCompat / Material Components / RecyclerView

## 目录结构
(见下方代码块)

## 构建

```bash
./gradlew assembleDebug     # Debug
./gradlew assembleRelease   # Release（R8 + 签名）
```

> Release 签名读取本机 `keystore.properties`（已在 `.gitignore`，不随仓库提交）。
> `sc.jks` 请妥善保管，切勿公开。

## 安装

- 产物：`app/build/outputs/apk/debug/app-debug.apk`、`app/build/outputs/apk/release/app-release.apk`
- 亦可安装仓库内 `release/app-release.apk`

## 权限说明

- 精确闹钟 `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`
- 通知 `POST_NOTIFICATIONS`
- 勿扰控制 `ACCESS_NOTIFICATION_POLICY`
- 开机自启 `RECEIVE_BOOT_COMPLETED`
- 前台服务 `FOREGROUND_SERVICE`
- 忽略电池优化 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

> **注意**：在国产 ROM（realme/OPPO/vivo/小米/华为）上请开启「自启动」「允许完全后台/不限制省电」「锁定最近任务」，否则勿扰定时可能在应用被杀后失效。

## 版本

versionName = 2026.1.1
versionCode = 24

## 说明

- `shift.csv` 为 **GBK 编码**（Excel 导出），读取时保持 GBK。
- 排班数据为静态内置，更新时替换 `shift.csv` 后重新构建。
