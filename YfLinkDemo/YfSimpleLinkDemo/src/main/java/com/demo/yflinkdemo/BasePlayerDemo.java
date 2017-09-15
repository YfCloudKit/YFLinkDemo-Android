package com.demo.yflinkdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;

import com.demo.yflinkdemo.extra.CustomMediaController;
import com.yunfan.player.widget.YfPlayerKit;

import java.util.HashMap;


/**
 * Created by xjx-pc on 2016/1/8 0008.
 */
public abstract class BasePlayerDemo extends AppCompatActivity {
    private String TAG = "Yf_BasePlayerDemo";

    protected static final String PULL_PATH = "play_path";
    protected static final String PULL_PATH_ANOTHER = "play_path_another";
    protected static final String PUSH_PATH = "push_path";
    protected static final String PLAYER_UDP = "PLAYER_UDP";
    protected static final String STREAMER_UDP = "STREAMER_UDP";
    protected static final String HARD_ENCODER = "HARD_ENCODER";
    protected static final String LINK_BUFFER = "LINK_BUFFER";
    protected boolean mIsFullScreen;
    protected boolean mInitEnd = false;
    protected PopupWindow mMenu;

    private NetworkConnectChangedReceiver mReceiver;
    protected CustomMediaController mMediaController;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mIsFullScreen = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? true : false;
//        startListener();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mReceiver = new NetworkConnectChangedReceiver();
        registerReceiver(mReceiver, filter);
        mMediaController = new CustomMediaController(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.switch_camera:
                Log.d(TAG, "switch_camera");
                switchCamera();
                break;
            case R.id.action_ask_for_link:
                Log.d(TAG, "requestLink");
                requestLink();
                break;
            case R.id.action_exit_link:
                Log.d(TAG, "exitLink");
                exitLink(true);
                break;
            case R.id.action_link_debug:
                mDebug=true;
                linkToStreamer();
                break;
            case R.id.action_open_link_debug:
                mDebug=true;
                openSecondStreamDEBUG();
                break;
            case R.id.action_open_another_stream:
                openAnotherStream();
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    protected FrameLayout.LayoutParams getLayoutParams(boolean fullScreen) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        if (fullScreen) {
            lp.gravity = Gravity.CENTER;
            Log.d(TAG, "设置全屏" + lp.width + "___" + lp.height);
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            lp.gravity = Gravity.NO_GRAVITY;
            lp.width = dm.widthPixels;
            lp.height = (int) ((lp.width * 9) / 16 + 0.5f);
            Log.d(TAG, "设置窗口模式宽高" + lp.width + "___" + lp.height);
        }
        return lp;
    }


    private OrientationEventListener mOrientationListener; // 屏幕方向改变监听器
    private int mStartRotation;

    /**
     * 开启监听器
     */
    private final void startListener() {
        mOrientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int rotation) {

                if (mStartRotation == -2) {//初始化角度
                    mStartRotation = rotation;
                }
                //变化角度大于30时，开启自动旋转，并关闭监听
                int r = Math.abs(mStartRotation - rotation);
                r = r > 180 ? 360 - r : r;
                if (r > 30) {
                    //开启自动旋转，响应屏幕旋转事件
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    this.disable();
                }
            }

        };
        mOrientationListener.enable();
    }

    @Override
    protected void onDestroy() {
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
        Log.i(TAG, "JCloseChannel");
        super.onDestroy();
    }


    public abstract int getOrientation();

    public abstract void onWifiConnected();


    protected abstract void linkToStreamer();

    protected abstract void openSecondStreamDEBUG();

    /**
     * 打开另一条主播流
     */
    protected abstract void openAnotherStream();

    protected abstract void exitLink(boolean notify);


    protected abstract void switchCamera();

    protected abstract void requestLink();

    protected boolean mDebug=false;

    public class NetworkConnectChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                    Log.e(TAG, "isConnected" + isConnected);
                    if (isConnected) {
                        onWifiConnected();
                    } else {

                    }
                }
            }
        }
    }

    protected HashMap<String, Object> createInfoMap(String title, String info) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("title", title);
        map.put("content", info);
        return map;
    }
}
