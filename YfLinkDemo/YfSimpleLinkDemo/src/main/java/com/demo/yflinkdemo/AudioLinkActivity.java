package com.demo.yflinkdemo;

import android.media.AudioManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.yflinkdemo.extra.Const;
import com.demo.yflinkdemo.widget.Log;
import com.yunfan.encoder.widget.RecordMonitor;
import com.yunfan.encoder.widget.YfEncoderKit;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfPlayerKit;

public class AudioLinkActivity extends BaseLinkActivity {
    private static final String TAG = "Yf_AudioLinkActivity";
    private int mLinkType;
    private String mRoomId;
    private String mHostId;
    private String mViceHostId;
    private int mRole;
    private String mHostPushUrl;
    private String mViceHostPushUrl;
    private String mHostPlayUrl;
    private String mViceHostPlayUrl;
    private YfEncoderKit yfEncoderKit;
    private TextView mTvDetail;
    private YfPlayerKit mYfPlayerKit;
    private boolean surfaceCreated;
    private String mUrlInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_link);
        initData();
        initView();
        initRecorder();
        initPlayer();
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_exit:
                    showStopRecordDialog();
                    break;
//                case R.id.btn_start_push:
//                    startRecord();
//                    break;
//                case R.id.btn_start_play:
//                    yfEncoderKit.enableAEC(true);
//                    openVideo();
//                    break;
            }
        }
    };

    private void initPlayer() {
        mYfPlayerKit.setSurfaceCallBack(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "on player surfaceCreated");
                surfaceCreated = true;
                mYfPlayerKit.setVolume(1, 1);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "on player surfaceDestroyed");
                surfaceCreated = false;
                mYfPlayerKit.setVolume(0, 0);
            }
        });
        mYfPlayerKit.enableBufferState(false);
        mYfPlayerKit.setDelayTimeMs(400, 400);
        mYfPlayerKit.setHTTPTimeOutUs(1000 * 1000);
        mYfPlayerKit.setHardwareDecoder(true);
        mYfPlayerKit.setAudioTrackStreamType(AudioManager.STREAM_VOICE_CALL);
        mYfPlayerKit.setOnNativeAudioDecodedListener(new YfCloudPlayer.OnNativeAudioDataDecoded() {
            @Override
            public void onAudioDataDecoded(YfCloudPlayer mp, byte[] data, int length, long pts) {
                if (yfEncoderKit != null)
                    yfEncoderKit.onRemoteAudioAvailable(data, length);//将音频数据传入编码器
            }
        });

        mYfPlayerKit.setOnPreparedListener(new YfCloudPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(YfCloudPlayer mp) {
                android.util.Log.d(TAG, "onPrepared: ");
                Toast.makeText(AudioLinkActivity.this, "onPrepared", Toast.LENGTH_SHORT).show();
                if (yfEncoderKit != null) {
                    Log.d(TAG, "size:" + mp.getVideoWidth() + "," + mp.getVideoHeight());
                }
            }
        });
        mYfPlayerKit.setOnErrorListener(new YfCloudPlayer.OnErrorListener() {
            @Override
            public boolean onError(YfCloudPlayer mp, int what, int extra) {
                android.util.Log.d(TAG, "onError: ");
                if (yfEncoderKit != null)
                    yfEncoderKit.stopHandleLinkStream();//播放已经异常，通知编码器停止处理播放的数据
                if (surfaceCreated)//只有当surface存在的时候才能打开视频
                    openVideo();//简单粗暴地重连
                return false;
            }
        });
    }

    private void openVideo() {
        android.util.Log.d(TAG, "openVideo: ");
        mYfPlayerKit.setVideoPath(mRole == Const.ROLE_HOST ? mViceHostPlayUrl : mHostPlayUrl);
        mYfPlayerKit.start();
    }

    private void pauseVideo() {
        if (mYfPlayerKit != null) {
            mYfPlayerKit.pause();
        }
    }

    private void releasePlayer() {
        if (mYfPlayerKit != null) {
            mYfPlayerKit.stopPlayback();
            mYfPlayerKit.release(true);
        }
    }

    private void initRecorder() {
        yfEncoderKit = new YfEncoderKit(this, Const.CACHE_DIRS);
        yfEncoderKit.setRecordMonitor(mMonitor);
    }

    private void initData() {
        mLinkType = getIntent().getIntExtra(Const.KEY_LINK_TYPE,Const.AUDIO);
        mHostId = getIntent().getStringExtra(Const.KEY_HOST_ID);
        mViceHostId = getIntent().getStringExtra(Const.KEY_VICE_HOST_ID);
        mRole = getIntent().getIntExtra(Const.KEY_ROLE, Const.ROLE_HOST);
        Log.d(TAG, "mRole: " + mRole + " mHostId: " + mHostId + " mViceHostId:" + mViceHostId);
        mHostPushUrl = Const.PUSH_URL_BASE + mHostId;
        mViceHostPushUrl = Const.PUSH_URL_BASE + mViceHostId;
        mHostPlayUrl = Const.PLAY_URL_BASE + mHostId;
        mViceHostPlayUrl = Const.PLAY_URL_BASE + mViceHostId;
    }

    private void initView() {
        mYfPlayerKit = (YfPlayerKit) findViewById(R.id.yf_player_kit);

        findViewById(R.id.btn_exit).setOnClickListener(mOnClickListener);
        TextView tvInfo = (TextView) findViewById(R.id.tv_info);
        mTvDetail = (TextView) findViewById(R.id.tv_detail);

//        Button btnStartPush = (Button) findViewById(R.id.btn_start_push);
//        Button btnStartPlay = (Button) findViewById(R.id.btn_start_play);
//        btnStartPush.setOnClickListener(mOnClickListener);
//        btnStartPlay.setOnClickListener(mOnClickListener);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("房间： ").append(mRoomId).append(SPACE)
                .append("主播： ").append(mHostId).append(SPACE)
                .append("角色： ").append(mRole == Const.ROLE_HOST ?
                "主播" : (mRole == Const.ROLE_VICE_HOST ? "副播" : "观众"));
        tvInfo.setText(stringBuilder.toString());
        stringBuilder.delete(0, stringBuilder.length());
        stringBuilder.append("当前推流地址：")
                .append(mRole == Const.ROLE_HOST ? mHostPushUrl : mViceHostPushUrl)
                .append("\n")
                .append("当前播放地址：")
                .append(mRole == Const.ROLE_HOST ? mViceHostPlayUrl : mHostPlayUrl);
        mUrlInfo = stringBuilder.toString();
    }

    private void startRecord() {
        if (yfEncoderKit == null || yfEncoderKit.isRecording()) {
            return;
        }
        Log.d(TAG, "startRecorderInternal");
        yfEncoderKit.changeMode(YfEncoderKit.MODE_LIVE, -1);
        yfEncoderKit.setBufferSizeBySec(1);
        yfEncoderKit.enableUDP(false);
        yfEncoderKit.enableHttpDNS(false);
        yfEncoderKit.setLiveUrl(mRole == Const.ROLE_HOST ? mHostPushUrl : mViceHostPushUrl);
        yfEncoderKit.startRecord();
    }

    private void stopRecord() {
        yfEncoderKit.stopRecord();
    }

    private void destroyRecorder() {
        Log.d(TAG, "销毁编码器");
        if (yfEncoderKit != null) {
            stopRecord();
            yfEncoderKit.release();
            yfEncoderKit = null;
        }
    }

    private boolean onServerConnected;
    private boolean mForceStop;
    private int mCurrentSpeed;
    private RecordMonitor mMonitor = new RecordMonitor() {
        @Override
        public void onServerConnected() {
            onServerConnected = true;
            if (yfEncoderKit != null) {
                Toast.makeText(AudioLinkActivity.this, "推流成功，编码方式:" +
                        (yfEncoderKit.getEncodeMode() ? "硬编" : "软编"), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(int mode, int err, String msg) {
            Log.i(TAG, "####### error: " + err + " " + msg);
            Toast.makeText(AudioLinkActivity.this, "err: no=" + err + " msg=" + msg,
                    Toast.LENGTH_SHORT).show();
            onServerConnected = false;
            if (!mForceStop) {
                startRecord();
            }
        }

        @Override
        public void onStateChanged(int mode, int oldState, int newState) {
            if (onServerConnected && newState == YfEncoderKit.STATE_RECORDING) {
                maybeRegisterReceiver();//监听wifi连接状况
            }
            Log.i(TAG, "####### state changed: "
                    + YfEncoderKit.getRecordStateString(oldState) + " -> "
                    + YfEncoderKit.getRecordStateString(newState));
        }

        @Override
        public void onFragment(int mode, String fragPath, boolean success) {

        }

        @Override
        public void onInfo(int what, double arg1, double arg2, Object obj) {
            switch (what) {
                case YfEncoderKit.INFO_IP:
                    Log.d(TAG, "实际推流的IP地址:" + intToIp((int) arg1));
                    break;
                case YfEncoderKit.INFO_DROP_FRAMES:
                    Log.d(TAG, "frames had been dropped");
                    break;
                case YfEncoderKit.INFO_PUSH_SPEED:
                    mCurrentSpeed = (int) arg1;

                    mTvDetail.setText(String.format("%s\n当前推流速度：%s ", mUrlInfo, mCurrentSpeed));
                    break;
                case YfEncoderKit.INFO_FRAME:
//                    mCurrentFPS = (int) arg1;
//                    mAvgCostTimeMS = (int) arg2;
                    break;
                case YfEncoderKit.INFO_PREVIEW_SIZE_CHANGED:
//                    Log.d(TAG, "on preview size changed:" + arg1 + "," + arg2);
//                    Log.d(TAG, "on preview size changed:" + (float) arg1 / arg1 + "," + (float) PREVIEW_WIDTH / PREVIEW_HEIGHT);
                    break;
                case YfEncoderKit.INFO_BITRATE_CHANGED:
                    android.util.Log.d(TAG, "INFO_BITRATE_CHANGED: " + arg1);
//                    mCurrentBitrate = (int) arg1;
                    break;
                case YfEncoderKit.INFO_CURRENT_BUFFER:
//                    mCurrentBufferMs = (int) arg1;
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (yfEncoderKit != null) {
            yfEncoderKit.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseVideo();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (yfEncoderKit != null) {
            yfEncoderKit.onStop(true);
        }
        mYfPlayerKit.stopPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyRecorder();
        releasePlayer();
    }
}
