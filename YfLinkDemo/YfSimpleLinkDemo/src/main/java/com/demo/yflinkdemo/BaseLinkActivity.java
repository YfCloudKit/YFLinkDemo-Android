package com.demo.yflinkdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.demo.yflinkdemo.widget.DeviceUtil;
import com.demo.yflinkdemo.widget.Log;

/**
 * Created by yunfan on 2017/11/16.
 */

public class BaseLinkActivity extends AppCompatActivity {
    protected String TAG = BaseLinkActivity.class.getSimpleName();
    private NetworkConnectChangedReceiver receiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DeviceUtil.fullScreen(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    protected final String SPACE = "\n";

    protected void maybeRegisterReceiver() {
        Log.d(TAG, "maybeRegisterReceiver" + receiver);
        if (receiver != null) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        receiver = new NetworkConnectChangedReceiver();
        registerReceiver(receiver, filter);
    }


    public static String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "."
                + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    public class NetworkConnectChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                    Log.e("", "isConnected:" + isConnected);
//                    if (isConnected) {
//                        Log("WIFI已连接");
//                    } else {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                logRecoder.writeLog("WIFI已断开");
//                            }
//                        });
//                    }
                }
            }
        }
    }

    protected void showStopRecordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("退出？");
        builder.setNegativeButton("是",
                mAlertDialogClickListener);
        builder.setPositiveButton("否",
                mAlertDialogClickListener);
        builder.show();
    }

    protected DialogInterface.OnClickListener mAlertDialogClickListener
            = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    exitLink();
                    break;
            }
        }
    };
    protected void exitLink(){
        finish();
    }

    @Override
    public void onBackPressed() {
        showStopRecordDialog();
    }
}
