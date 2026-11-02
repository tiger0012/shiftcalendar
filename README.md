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
- 构建：Gradle 8.11.1 / AGP 8.9.1（Kotlin DSL），Java 21
- minSdk 26 / targetSdk 35
- 农历库：[cn.6tail:lunar](https://github.com/6tail/lunar) 1.7.7（内置节假日数据至 2026-10-10，**无 2027 条目**，见下文「2027 数据现状」）
- UI：AppCompat / Material Components / RecyclerView

## 关键文件

| 路径 | 说明 |
|---|---|
| `app/src/main/assets/shift.csv` | GBK 编码排班数据（4439 行），核心数据源 |
| `app/src/main/java/com/example/myapplication/MainActivity.java` | 主逻辑（~1330 行）：CSV 解析、农历、节日名、周数补全 |
| `app/build.gradle.kts` | 第 69 行 `implementation("cn.6tail:lunar:1.7.7")` 是节假日/农历依赖 |
| `release/app-release.apk` | 发布包（每次构建后同步） |
| `local.properties` | 指向 `/home/tiger-loong/Android/Sdk`（已 gitignore） |

## 构建

```bash
cd /run/media/tiger-loong/文档/app/shiftcalendar
./gradlew assembleDebug --no-daemon     # Debug
./gradlew assembleRelease --no-daemon   # Release（R8 + 签名，约 54 秒）
```

> Release 签名读取本机 `keystore.properties`（已在 `.gitignore`，不随仓库提交）。
> `sc.jks` 请妥善保管，切勿公开。

## 安装

- 产物：`app/build/outputs/apk/debug/app-debug.apk`、`app/build/outputs/apk/release/app-release.apk`
- 亦可安装仓库内 `release/app-release.apk`（每次构建后同步过去）

## 权限说明

- 精确闹钟 `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`
- 通知 `POST_NOTIFICATIONS`
- 勿扰控制 `ACCESS_NOTIFICATION_POLICY`
- 开机自启 `RECEIVE_BOOT_COMPLETED`
- 前台服务 `FOREGROUND_SERVICE`
- 忽略电池优化 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

> **注意**：在国产 ROM（realme/OPPO/vivo/小米/华为）上请开启「自启动」「允许完全后台/不限制省电」「锁定最近任务」，否则勿扰定时可能在应用被杀后失效。

## shift.csv 格式（最常改的坑区）

- **GBK 编码**（Excel 导出）。app 的 `readShiftGroupsFromCSV()` 硬编码 GBK 解码；若从 Excel 重新导出成 UTF-8，必须同步改代码里的解码，否则中文全乱。
- **两种日期格式并存**：2024–2026 段用 `yyyy/M/d`（如 `2025/1/1`），2027 段用 `yyyy-M-d`（如 `2027-1-1`，无零填充）。app 先按斜杠解析、失败再回退横杠——**编辑时各段保持原格式**。
- 列定义（0 起；表头含垃圾列，总宽 35，但 app 只读这些）：

| 列 | 名 | 说明 |
|---|---|---|
| 1 | Date | 日期（见两种格式） |
| 2 | Team | 班组号 |
| 3 | DayNight | Day / Night |
| 8 | Weeknum | 年周数；`0`/空 = 占位，app 用 `computeYearWeeknum()` 自动补全 |
| 11 | Holiday | `Y` = 红底（团队实际放假日） |

- app 通过**列名** `Weeknum`、`Holiday` 定位这两列（大小写不敏感），Date/Team/DayNight 按**列位置**读取——**不要改列顺序，不要改这两个列头名**。
- **Weekday（列 9）约定：`0`=周日 … `6`=周六**（不是 ISO 周一=1）。已全量校验 4438 行零错误。
- Weeknum 约定：含 1 月 1 日的周（周日起始）为第 1 周，一年 52 周；跨年周取**后写入一侧**的年份编号 → 2026-12-27~31 显示为"2027 第 1 周"。
- **每个节假日日期有 4 行数据**（Day 2 行 + Night 2 行）；增删假期要改该日期全部 4 行的 Holiday 列。
- **Spring Festival（列 12）app 不读取**，纯数据列，可忽略。
- 排班数据为静态内置；当前范围 2024-12-29 → 2027-12-31，4438 个数据行。更新时替换 `shift.csv` 后重新构建。

## 节假日语义（2026-11-01 定稿）

- **红底（Holiday=Y）= 团队实际放假日，不是官方完整假期区间**。
  - 2025 标 13 天（官方区间 28 天）、2026 标 13 天（官方区间 33 天）；CSV 标的 26 个日期全部是合法官方休息日，无错标。
  - 小瑕疵（有意保留）：2026 清明标在 4/4，实际清明当天是 4/5（4/4 是区间首日）。
  - 调休补班日不体现（CSV 无此列，app 也不读）。
- 节日名显示：`HolidayUtil.getHoliday()`（lunar 库）；该日期库内无数据时（当前 2027 全缺）回退 `MainActivity.get2027HolidayNames()` 硬编码表（约 621 行）。
  - 表的 13 个 key **必须与 CSV 的 Holiday=Y 日期一一对应**；增删假期日期时两边同步。
  - lunar 发布含 2027 数据的版本后此表即成死代码——**删掉重新打包**。

## 2027 数据现状（截至 2026-11-01）

- `cn.6tail:lunar:1.7.7` 是 Maven Central 最新版（2025-11-05 发布，含 2026 数据）；内置节假日到 2026-10-10，**2027 条目为零**。
- 作者惯例：**国务院通知发布后数日内发新版**（2026 通知 2025-11-04 发布 → v1.7.7 2025-11-05 上线）。2027 通知预计 2026 年 11 月中下旬发布。
- 已用库验证的 2027 锚点：02-06 = 正月初一（02-05 = 除夕），06-09 = 端午（五月初五），09-15 = 中秋（八月十五）；固定日期 1/1 元旦、4/5 清明、5/1 劳动节、10/1 国庆。
- 2027 的 Holiday=Y 目前是 **13 个预估日期**：1/1、2/5–8、4/5、5/1–2、6/9、9/15、10/1–3 —— **发布官方通知后必须对照核对**（cron 只自动升依赖，不会自动改 CSV）。
- `holiday-cn`（NateScarlet/holiday-cn，自动抓取国务院通知）：**默认分支是 `master` 不是 `main`**；`2027.json` 当前为空，通知发布后自动填充。

## 自动监听任务（每日 09:30 定时）

systemd **user** timer（非系统 cron，无需 root）：

- 单元：`~/.config/systemd/user/lunar2027watch.{service,timer}`（每日 09:30，`Persistent=true` 关机错过开机补跑）
- 脚本：`/home/tiger-loong/金门大桥/lunar2027watch.sh`
- 日志/状态：`/home/tiger-loong/金门大桥/lunar-update.log`、`lunar-2027-{state,PENDING,DONE}`

流程：查 Maven Central 上 `cn.6tail:lunar` 最新版 → 若 > 1.7.7，下载 jar 并**实际运行 HolidayUtil 验证含 2027 数据** → 满足则备份 `build.gradle.kts` → 升级依赖 → `./gradlew assembleRelease --no-daemon` → APK 复制到 `release/` 和工作区 → 桌面通知 → 写 DONE 标记。

安全性：验证不过/构建失败自动回滚；改动前必留备份；DONE 后脚本永久空转。手动触发：`FORCE=1 bash /home/tiger-loong/金门大桥/lunar2027watch.sh`。

常用命令：

```bash
systemctl --user status lunar2027watch.timer       # 状态
systemctl --user list-timers | grep lunar          # 下次触发时间
tail -20 /home/tiger-loong/金门大桥/lunar-update.log
```

坑：

- **user 级 timer 只在用户会话内活动**：09:30 时机器关机/无会话，则下次登录后补跑（Persistent）。
- `lunar2027-loop.sh`（工作区）是废弃的常驻循环版（已被 timer 取代，进程已停），文件可删。
- 任务完成后还有**手动收尾**：① CSV 2027 的 Holiday 区间对照官方公告微调 ② 删除 `get2027HolidayNames()` 回退表后重新打包。

## 版本

versionName = 2026.11.01
versionCode = 29

## 坑速查（均为实际踩过）

1. **GBK 编码**：CSV 若从 Excel 重新导出，保持 GBK 或同步改代码解码。
2. **两种日期格式并存**（斜杠/横杠），不要统一；app 先试斜杠。
3. **Weekday 是 0=周日**（非 ISO）；Weeknum=0 是占位不是错。
4. **列头名是契约**：app 按列名找 `Weeknum`/`Holiday`，按列位取 Date/Team/DayNight——别改顺序、别改列头。
5. **节假日日期 4 行**（Day 2 + Night 2），增删要全改。
6. **红底 = 团队实际放假日**（非官方完整区间），2026-11-01 定的口径，看数据时别"好心"按官方区间补全。
7. **`get2027HolidayNames()` 的 key 必须与 CSV Holiday=Y 一一对应**；lunar 升到含 2027 数据的版本后删掉该表。
8. **文档盘是 NTFS 盘**（`/dev/sda1` 挂 `/run/media/tiger-loong/文档`），重启后可能未自动挂载；cron 依赖它，未挂载当日跳过（日志有记录）。
9. **holiday-cn 默认分支是 `master`** 不是 `main`；raw.githubusercontent 可能超时，用 GitHub API（`contents`/`git/blobs`）取文件。
10. **git 未提交**：10 个修改文件（含 `shift.csv`、`MainActivity.java`、`build.gradle.kts`、`release/app-release.apk`）+ 1 个未跟踪（`.workbuddy/memory/MEMORY.md`），待 commit。
11. **agent（DSH）环境**：`web_search` 不可用，用 `web_fetch`/curl 直连；`/tmp` 跨命令不持久，临时文件放工作区；写文档盘需批准（cron 跑在用户 systemd 下、不受沙箱限制，不受影响）。
