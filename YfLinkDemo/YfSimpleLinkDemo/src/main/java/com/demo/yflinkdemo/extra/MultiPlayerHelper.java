package com.demo.yflinkdemo.extra;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.demo.yflinkdemo.widget.Log;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfPlayerKit;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 37917 on 2017/11/20 0020.
 */

public class MultiPlayerHelper {
    private final static String TAG = "Yf_MultiPlayerHelper";
    private List<YfPlayerKit> mYfPlayerKitList = new ArrayList<>();
    private boolean mVisible=true, mEnableLink;
//    private int mWidth, mHeight;
    private LayoutParamHelper mHelper;
    public MultiPlayerHelper(LayoutParamHelper helper) {
//        mWidth = linkerWidth;
//        mHeight = linkerHeight;
        mHelper=helper;
//        mGravity = new ArrayList<>();
//        mGravity.add(new YfGravity(Gravity.RIGHT | Gravity.BOTTOM, false));
//        mGravity.add(new YfGravity(Gravity.LEFT | Gravity.BOTTOM, false));
//        mGravity.add(new YfGravity(Gravity.LEFT | Gravity.TOP, false));
//        mGravity.add(new YfGravity(Gravity.RIGHT | Gravity.TOP, false));
//        mGravity.add(new YfGravity(Gravity.CENTER, false));
    }

    public int buildAPlayerInOnePlace(Context context, FrameLayout parent, boolean fullScreen, boolean streamVoiceCall, final YfCloudPlayer.OnPreparedListener preparedListener, final YfCloudPlayer.OnErrorListener errorListener, final YfCloudPlayer.OnNativeAudioDataDecoded nativeAudioDataDecoded, final int index) {
        if(!mVisible){
            return -1;
        }
        final YfPlayerKit yfPlayerKit = new YfPlayerKit(context);
        yfPlayerKit.enableUDP(true);
        yfPlayerKit.setVideoLayout(YfPlayerKit.VIDEO_LAYOUT_MATCH_PARENT);
        yfPlayerKit.enableBufferState(false);
        yfPlayerKit.setDelayTimeMs(400, 400);
        yfPlayerKit.enableHttpDNS(false);
        yfPlayerKit.setHTTPTimeOutUs(1500*1000);
        yfPlayerKit.setHardwareDecoder(true);
        yfPlayerKit.setAudioTrackStreamType(streamVoiceCall ? AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC);
        yfPlayerKit.setOnNativeAudioDecodedListener(new YfCloudPlayer.OnNativeAudioDataDecoded() {
            @Override
            public void onAudioDataDecoded(YfCloudPlayer mp, byte[] data, int length, long pts) {
                if (nativeAudioDataDecoded != null) {
                    nativeAudioDataDecoded.onAudioDataDecoded(mp, data, length, mYfPlayerKitList.indexOf(yfPlayerKit));
                }
            }
        });
        yfPlayerKit.setOnPreparedListener(new YfCloudPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(YfCloudPlayer mp) {
                Log.d(TAG, "MultiPlayer onPrepared: " + preparedListener);
                if (preparedListener != null) {
                    preparedListener.onPrepared(mp);
                }
            }
        });
        yfPlayerKit.setOnErrorListener(new YfCloudPlayer.OnErrorListener() {
            @Override
            public boolean onError(YfCloudPlayer mp, int what, int extra) {
                Log.d(TAG, "MultiPlayer onError: " + errorListener);
                return errorListener == null ? false : errorListener.onError(mp, what, mYfPlayerKitList.indexOf(yfPlayerKit));
            }
        });
        yfPlayerKit.setSurfaceCallBack(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "on player surfaceCreated");
                yfPlayerKit.setVolume(1, 1);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "on player surfaceDestroyed");
                yfPlayerKit.setVolume(0, 0);
            }
        });
        mYfPlayerKitList.add(yfPlayerKit);
        parent.addView(yfPlayerKit, mHelper.getLayoutParams(false,fullScreen));
        return mYfPlayerKitList.size() - 1;
    }

//    class YfGravity {
//        int gravity;
//        boolean inUsed;
//
//        public YfGravity(int gravity, boolean inUsed) {
//            this.gravity = gravity;
//            this.inUsed = inUsed;
//        }
//    }

//    private List<YfGravity> mGravity;

//    private FrameLayout.LayoutParams getLayoutParams(boolean landscape,boolean fullScreen) {
//        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        if (landscape) {
//            if (fullScreen) {//全屏（大小窗口模式），推流窗口恒为中间小窗口，宽高为width，height
//                lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
//                lp.width = mWidth;
//                lp.height = mHeight;
//            } else {//双半屏模式，推流窗口和播放窗口各占据一半
//                lp.gravity = mRole == Const.ROLE_HOST ? Gravity.LEFT : Gravity.RIGHT;//主播在左，副播在右
//                lp.width = width / 2;
//                lp.height = width * 9 / 16;
//            }
//        } else {
//            if (fullScreen) {
//                return lp;
//            } else {
//                lp.width = mWidth;
//                lp.height = mHeight;
//                for (int i = 0; i < mGravity.size(); i++) {
//                    if (!mGravity.get(i).inUsed) {
//                        lp.gravity = mGravity.get(i).gravity;
//                        mGravity.get(i).inUsed = true;
//                        break;
//                    }
//                }
//                return lp;
//            }
//        }
//    }

    public void removeAndReleasePlayer(FrameLayout parent, int index) {
        if (index < mYfPlayerKitList.size()) {
            parent.removeView(mYfPlayerKitList.get(index));
            mHelper.onLayoutReleased(((FrameLayout.LayoutParams) mYfPlayerKitList.get(index).getLayoutParams()).gravity);
            mYfPlayerKitList.get(index).release(false);
            mYfPlayerKitList.remove(index);
        }
    }

    public int getPlayerSize() {
        return mYfPlayerKitList.size();
    }

    public void enableLink(boolean enable) {
        mEnableLink = enable;
    }

    public void release(int index) {
        mYfPlayerKitList.get(index).release(false);
    }

    public void setVideoPath(String path, int index) {
        mYfPlayerKitList.get(index).setVideoPath(path);
    }

    public String getDataSource(int index) {
        return mYfPlayerKitList.get(index).getDataSource();
    }

    public boolean containDataSource(String path) {
        for (YfPlayerKit player : mYfPlayerKitList) {
            if (player.getDataSource().equals(path))
                return true;
        }
        return false;
    }

    public void start(int index) {
        mYfPlayerKitList.get(index).start();
    }

    public void pause(int index) {
        mYfPlayerKitList.get(index).pause();
    }

    public void startWhileSurfaceCreated() {

    }

    public void onResume() {
        for (YfPlayerKit player : mYfPlayerKitList) {
            player.start();
            player.setVolume(1, 1);
        }
        mVisible = true;
    }

    public void onStop() {
        for (YfPlayerKit player : mYfPlayerKitList) {
            player.setVolume(0, 0);
        }
//        mVisible = false;
    }

    public void onPause() {
        for (YfPlayerKit player : mYfPlayerKitList) {
            player.pause();
        }
    }

    public void onDestroy() {
        mVisible = false;
        for (YfPlayerKit player : mYfPlayerKitList) {
            player.release(false);
        }
        mYfPlayerKitList.clear();
    }

}
