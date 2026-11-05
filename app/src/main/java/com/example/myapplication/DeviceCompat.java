package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

/**
 * 不同 OEM 设备的兼容性处理 + 白名单引导
 */
public class DeviceCompat {

    // ==================== OEM 检测 ====================

    /**
     * 是否是 realme/OPPO 设备（ColorOS）
     */
    public static boolean isRealmeOrOppo() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String brand = Build.BRAND.toLowerCase();
        return manufacturer.contains("realme") || manufacturer.contains("oppo")
            || brand.contains("realme") || brand.contains("oppo");
    }

    /**
     * 是否是 vivo 设备
     */
    public static boolean isVivo() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("vivo");
    }

    /**
     * 是否是华为/荣耀设备
     */
    public static boolean isHuaweiOrHonor() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("huawei") || manufacturer.contains("honor");
    }

    /**
     * 是否是小米/红米设备
     */
    public static boolean isXiaomi() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi")
            || Build.BRAND.toLowerCase().contains("mi");
    }

    /**
     * 是否是需要引导白名单的国产 ROM 设备
     */
    public static boolean needsWhitelistGuide() {
        return isRealmeOrOppo() || isVivo() || isHuaweiOrHonor() || isXiaomi();
    }

    /**
     * 获取当前设备品牌名称（用于显示）
     */
    public static String getBrandName() {
        if (isRealmeOrOppo()) {
            String brand = Build.BRAND.toLowerCase();
            if (brand.contains("realme")) return "真我";
            if (brand.contains("oppo")) return "OPPO";
            return "真我/OPPO (ColorOS)";
        }
        if (isVivo()) return "vivo (OriginOS)";
        if (isHuaweiOrHonor()) return "华为/荣耀 (HarmonyOS)";
        if (isXiaomi()) return "小米/红米 (HyperOS)";
        return "Android";
    }

    // ==================== 白名单设置跳转 ====================

    /**
     * 打开电池优化忽略设置页面
     */
    public static Intent openBatteryOptimization(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // 某些 ROM 可能不支持
        }
        return null;
    }

    /**
     * 请求忽略电池优化（直接弹授权对话框）
     * 需要在 Manifest 中声明 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 权限
     */
    public static void requestIgnoreBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                // 回退到手动设置页
                Intent fallback = openBatteryOptimization(context);
                if (fallback != null) context.startActivity(fallback);
            }
        }
    }

    /**
     * 打开应用详情设置页（包含自启动、后台运行等选项）
     * 这是通用方案，适用于所有 Android 版本和品牌
     */
    public static Intent openAppDetailSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            return intent;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 尝试打开厂商专属的自启动管理页面
     * 返回 null 时回退到 openAppDetailSettings()
     */
    public static Intent openAutoLaunchSettings(Context context) {
        String brand = Build.BRAND.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        try {
            // 小米 / 红米 — 自启动管理
            if (manufacturer.contains("xiaomi")) {
                Intent intent = new Intent().setComponent(
                    new android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    ));
                if (intent.resolveActivity(context.getPackageManager()) != null) return intent;
            }

            // OPPO / realme — 自启动管理
            if (brand.contains("oppo") || manufacturer.contains("realme")) {
                try {
                    Intent intent = new Intent().setComponent(
                        new android.content.ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        ));
                    if (intent.resolveActivity(context.getPackageManager()) != null) return intent;
                } catch (Exception ignored) {}

                // 新版 ColorOS 可能用这个
                try {
                    Intent intent = new Intent().setComponent(
                        new android.content.ComponentName(
                            "com.oplus.safecenter",
                            "com.oplus.safecenter.permission.startup.StartupAppListActivity"
                        ));
                    if (intent.resolveActivity(context.getPackageManager()) != null) return intent;
                } catch (Exception ignored) {}
            }

            // vivo — 自启动管理
            if (manufacturer.contains("vivo")) {
                Intent intent = new Intent().setComponent(
                    new android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    ));
                if (intent.resolveActivity(context.getPackageManager()) != null) return intent;
            }

            // 华为 / 荣耀 — 自启动管理
            if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                Intent intent = new Intent().setComponent(
                    new android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                    ));
                if (intent.resolveActivity(context.getPackageManager()) != null) return intent;
            }
        } catch (Exception e) {
            // 任何组件找不到都静默处理
        }

        return null;  // 回退到通用应用详情页
    }

    /**
     * 检查当前是否已忽略电池优化
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        try {
            android.os.PowerManager pm =
                (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 引导信息生成 ====================

    /**
     * 生成针对当前设备的白名单引导说明文字列表
     * 每项格式: [标题] 描述
     */
    public static java.util.List<String> getWhitelistSteps(Context context) {
        java.util.List<String> steps = new java.util.ArrayList<>();
        String pkg = context.getPackageName();

        if (isRealmeOrOppo()) {
            steps.add("【自启动】（最重要）\n" +
                "设置 → 应用 → 自启动 → 找到本应用 → 打开开关\n" +
                "（老版本：设置 → 权限与隐私 → 自启动管理）");
            steps.add("【后台不限制】\n" +
                "设置 → 电池 → 找到本应用 / 后台活动管理 → 选择「不限制」");
            steps.add("【关联启动】\n" +
                "设置 → 权限与隐私 → 关联启动管理 → 允许本应用被拉起");
            steps.add("【锁定最近任务】\n" +
                "最近任务界面长按本应用卡片 → 「🔒 锁定」，防止被划掉");
        } else if (isXiaomi()) {
            steps.add("【省电策略】\n" +
                "长按此应用图标 → 应用信息 → 省电与电池 → 选择「无限制」");
            steps.add("【自启动】\n" +
                "设置 → 应用管理 → 权限 → 自启动管理 → 开启此应用");
            steps.add("【锁定最近任务】\n" +
                "在最近任务卡片上点击「🔒」锁住此应用");
        } else if (isVivo()) {
            steps.add("【电池优化】\n" +
                "设置 → 电池 → 后台耗电管理 → 将此应用切换为「允许后台运行」");
            steps.add("【自启动】\n" +
                "设置 → 更多设置 → 权限管理 → 自启动 → 开启此应用");
        } else if (isHuaweiOrHonor()) {
            steps.add("【电池优化】\n" +
                "设置 → 电池 → 更多电池设置 → 关闭本应用的智能省电模式\n" +
                "(或在应用信息 → 电池 → 选择「不限制」)");
            steps.add("【自动启动】\n" +
                "设置 → 应用管理 → 此应用 → 自动启动 → 开启");
            steps.add("【锁定最近任务】\n" +
                "在最近任务界面下拉此应用卡片出现锁头图标后松开");
        } else {
            // 通用 Android / Pixel 等
            steps.add("【忽略电池优化】\n" +
                "设置 → 应用 → 特殊访问权限 → 忽略电池优化 → 允许此应用");
        }

        return steps;
    }
}
