package com.demo.yflinkdemo.extra;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.view.Display;
import android.view.WindowManager;

import com.yunfan.player.utils.Log;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;


/**
 * Created by xjx-pc on 2016/1/19 0019.
 */
public class Utils {
    private static final String TAG = "Yf_Utils";
    static StringBuilder mFormatBuilder = new StringBuilder();
    static Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

    public static String getStreamName(String originUrl) {
        return originUrl.substring(originUrl.lastIndexOf("/") + 1);
    }



    //    mFormatBuilder
//    mFormatter
    public static boolean isWiFiActive(Context inContext) {
        Context context = inContext.getApplicationContext();
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getTypeName().equals("WIFI") && info[i].isConnected()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int getScreenWidth(Context context) {
        final Point p = new Point();
        final Display d = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        d.getSize(p);
        if (p.x > p.y) {
            return p.y;
        } else {
            return p.x;
        }
    }

    public static int getScreenHeight(Context context) {
        final Point p = new Point();
        final Display d = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        d.getSize(p);
        if (p.x > p.y) {
            return p.x;
        } else {
            return p.y;
        }
    }

    public static String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public static String getMac(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String mac = wm.getConnectionInfo().getMacAddress();
        Log.d(TAG, "mac:" + mac);
        return mac;
    }

    /**
     * 将字符串转为时间戳
     *
     * @param user_time
     * @return
     */
    public static long dateToTimestamp(String user_time) {
        String re_time = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d;
        try {
            d = sdf.parse(user_time);
            Log.d(TAG, "原始时间：" + user_time);
            long l = d.getTime();
            Log.d(TAG, "get TIME：" + l);
            String str = String.valueOf(l);
            re_time = str.substring(0, 10);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return Long.valueOf(re_time);
    }

//    public static String getBase64(String str) {
//        byte[] b = null;
//        String s = null;
//        try {
//            b = str.getBytes("utf-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        if (b != null) {
//            s = new BASE64Encoder().encode(b);
//        }
//        return s;
//    }
//    public static native String yf_base64_encode(byte[] in);


}
