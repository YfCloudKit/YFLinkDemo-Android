package com.demo.yflinkdemo.widget;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

public class PermissionUtils {
    public static final int REQUEST_CODE = 0x01;

    public static final String[] sPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };

    public static void askLinkPermission(Activity context, Runnable
            runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ActivityCompat.checkSelfPermission(context, sPermissions[0]);
            if (result == PackageManager.PERMISSION_GRANTED) {
                runnable.run();
            } else {
                ActivityCompat.requestPermissions(context, sPermissions, REQUEST_CODE);
            }
        } else {
            runnable.run();
        }
    }

    public static void onRequestPermissionsResult(int requestCode, int[] grantResults, Runnable
            runnable, Runnable denyRun) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                runnable.run();
            } else {
                denyRun.run();
            }
        }
    }

}