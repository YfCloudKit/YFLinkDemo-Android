/**
 * @版权 : 深圳云帆世纪科技有限公司
 * @作者 : 刘群山
 * @日期 : 2015年4月20日
 */
package com.demo.yflinkdemo.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class DeviceUtil {

    private static Boolean sArmV7 = null;
    private static Boolean sNeon = null;
    private static Boolean sX86 = null;

    public static int getScreenWidth(Context context) {
        final Point p = new Point();
        final Display d = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        d.getSize(p);
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE?Math.max(p.x,p.y):Math.min(p.x,p.y);
    }

    public static int getScreenHeight(Context context) {
        final Point p = new Point();
        final Display d = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        d.getSize(p);
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE?Math.min(p.x,p.y):Math.max(p.x,p.y);
    }

    public static String getRecorderSoName() {
        if (sNeon == null) {
            initCpuAbi();
        }

        if (sNeon) {
            return "muxer-7neon";
        } else if (sArmV7) {
            return "muxer-7";
        } else if (sX86) {
            throw new RuntimeException("Not support x86 platform");
        } else {
            return "muxer-6";
        }
    }

    private static void initCpuAbi() {
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(
                    "getprop ro.product.cpu.abi");
            input = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));

            String abi = input.readLine();

            if (abi.equalsIgnoreCase("armeabi-v7a")) {
                sX86 = false;
                sArmV7 = true;
                sNeon = containFeature("neon");
            } else if (abi.contains("arm")) {
                sX86 = false;
                sArmV7 = false;
                sNeon = false;
            } else {
                sX86 = true;
                sArmV7 = false;
                sNeon = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean containFeature(String feature) {
        try {
            String cpuInfo = getProperty("/proc/cpuinfo", "Features");
            if (cpuInfo != null && cpuInfo.contains(feature)) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static String getProperty(String file, String key) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(file));

            return p.getProperty(key);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 通过设置全屏，设置状态栏透明
     *
     * @param activity
     */
    public static void fullScreen(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
                    Window window = activity.getWindow();
                    View decorView = window.getDecorView();
                    //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
                    int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                    decorView.setSystemUiVisibility(option);
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(Color.TRANSPARENT);
                    //导航栏颜色也可以正常设置
                    //window.setNavigationBarColor(Color.TRANSPARENT);
                } else {
                    Window window = activity.getWindow();
                    WindowManager.LayoutParams attributes = window.getAttributes();
                    int flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
                    int flagTranslucentNavigation = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
                    attributes.flags |= flagTranslucentStatus;
                    //attributes.flags |= flagTranslucentNavigation;
                    window.setAttributes(attributes);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
