package com.demo.yflinkdemo;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.yflinkdemo.extra.Utils;
import com.demo.yflinkdemo.widget.DeviceUtil;
import com.demo.yflinkdemo.widget.YfController;
import com.yunfan.encoder.filter.YfBlurBeautyFilter;
import com.yunfan.encoder.widget.RecordMonitor;
import com.yunfan.encoder.widget.YfEncoderKit;
import com.yunfan.net.K2Pagent;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfPlayerKit;
import com.yunfan.yflinkkit.YfLinkerKit;
import com.yunfan.yflinkkit.callback.QueryLinkCallback;

import static com.demo.yflinkdemo.PushStreamActivity.CACHE_DIRS;
import static com.demo.yflinkdemo.PushStreamActivity.intToIp;

/**
 * 主播与观众连麦，该Activity为观众端
 */
public class PlayStreamActivity extends BasePlayerDemo implements YfCloudPlayer.OnCompletionListener,
        YfCloudPlayer.OnErrorListener, YfCloudPlayer.OnPreparedListener, YfCloudPlayer.OnInfoListener,
        YfCloudPlayer.OnBufferingUpdateListener, YfController.YfControl, RecordMonitor {
    private String TAG = "Yf_PlayStreamActivity";
    /**
     * 主播的原始播放地址，根据该地址查询主播是否开启连麦
     */
    private String mAudiencePath = "";
    /**
     * 当前的播放地址
     */
    private String mCurrentPath = "";
    /**
     * 粉丝流的播放地址，用来通知主播通过该地址打开粉丝流
     */
    private String mFansPlayPath = "";
    /**
     * 粉丝流的推流地址
     */
    private String mFansPushPath = "";
    /**
     * 当前的缓冲策略
     */
    private boolean mEnableBufferState = true;
    private YfPlayerKit mVideoView;
    private GLSurfaceView mSecondSurface;
    private FrameLayout mVideoViewLayout;
    private View mRootView;
    private long mBufferSpend = 0;
    private long mBufferTimes = -1;
    private long mTimeSpend = 0;
    private int mLinkBufferMs;
    private TextView mVd, mAd, mVb, mAb;
    private Handler mGetCacheHandler;
    private long mStartPlayTime;
    private ImageView mLoadingView;
    private Animation mAnim;
    private boolean mEnablePlayerUdp, mEnableStreamerUdp, mEnableHWEncoder;
    private K2Pagent mK2Pagent;
    private String mAnotherStreamPath;
    //    private SurfaceView mSurfaceView;
    private YfPlayerKit mSecondPlayer;
    private SurfaceView mSurfaceView;

    public static void startActivity(Context context, String pullPath, String pushPath, boolean playerUDP, boolean streamerUDP, boolean hardEncoder, int delay) {
        Intent i = new Intent(context, PlayStreamActivity.class);
        i.putExtra(PULL_PATH, pullPath);
        i.putExtra(PUSH_PATH, pushPath);
        i.putExtra(PLAYER_UDP, playerUDP);
        i.putExtra(STREAMER_UDP, streamerUDP);
        i.putExtra(HARD_ENCODER, hardEncoder);
        i.putExtra(LINK_BUFFER, delay);
        context.startActivity(i);
    }

    protected YfEncoderKit yfEncoderKit;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        YfPlayerKit.enableRotation(false);
        mTimeSpend = SystemClock.elapsedRealtime();
        mRootView = LayoutInflater.from(this).inflate(
                R.layout.activity_yfplayerkit_demo, null);
        setContentView(mRootView);
        mEnablePlayerUdp = getIntent().getBooleanExtra(PLAYER_UDP, false);
        mEnableStreamerUdp = getIntent().getBooleanExtra(STREAMER_UDP, false);
        mEnableHWEncoder = getIntent().getBooleanExtra(HARD_ENCODER, false);
        mLinkBufferMs = getIntent().getIntExtra(LINK_BUFFER, 400);
        findViews();
        initVideoView();
        init();

    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy!");
        super.onDestroy();
        releaseRecord();
        releaseYfLinkerKit(true);
    }

    private void findViews() {
        mVideoView = (YfPlayerKit) findViewById(R.id.surface_view);
        mVb = (TextView) findViewById(R.id.video_buffer);
        mAb = (TextView) findViewById(R.id.audio_buffer);
        mVd = (TextView) findViewById(R.id.video_frame_duaration);
        mAd = (TextView) findViewById(R.id.audio_frame_duaration);
        mVideoViewLayout = (FrameLayout) findViewById(R.id.videoView_layout);
        mLoadingView = (ImageView) findViewById(R.id.loading_img);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        mMediaController.setSupportActionBar(actionBar);
    }


    private void initVideoView() {
        mVideoView.selectAudio(1);
        mVideoView.setHardwareDecoder(false);
        mVideoView.setBufferSizeByMs(6000);

//        mVideoView.setDelayTimeMs(mLinkBufferMs, mLinkBufferMs);
//        mVideoView.enableBufferState(false);
//        mVideoView.setBufferSize(15 * 1024 * 1024);
        mVideoView.setSurfaceCallBack(mSHCallback);
        mVideoView.setVideoLayout(YfPlayerKit.VIDEO_LAYOUT_FILL_PARENT);
        mVideoView.setMediaController(mMediaController);
    }

    private void init() {
        mAudiencePath = MainActivity.PULL_HOST + getIntent().getStringExtra(PULL_PATH);
        mFansPlayPath = MainActivity.LINKER_PULL_HOST + getIntent().getStringExtra(PUSH_PATH) ;
        mFansPushPath = MainActivity.LINKER_PUSH_HOST + getIntent().getStringExtra(PUSH_PATH);
        mAnotherStreamPath = MainActivity.PULL_HOST + getIntent().getStringExtra(PUSH_PATH);
        //初始化载入动画
        mAnim = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mAnim.setRepeatCount(Animation.INFINITE); // 设置INFINITE，对应值-1，代表重复次数为无穷次
        mAnim.setDuration(1000); // 设置该动画的持续时间，毫秒单位
        mAnim.setInterpolator(new LinearInterpolator()); // 设置一个插入器，或叫补间器，用于完成从动画的一个起始到结束中间的补间部分
        toggleLoading(true);


        //刷新展示视频缓冲等
        mGetCacheHandler = new Handler();
        mGetCacheHandler.removeCallbacks(updateCacheDisplay);
        mGetCacheHandler.postDelayed(updateCacheDisplay, 500);


        //开始播放
        if (mAudiencePath != null) {
            openVideo(mAudiencePath, true);
        }
    }


    /**
     * 播放视频
     *
     * @param path              播放地址
     * @param enableBufferState 是否允许进入缓冲
     */
    public void openVideo(String path, boolean enableBufferState) {
        mEnableBufferState = enableBufferState;
        Log.d(TAG, "播放地址是:" + path);
        mCurrentPath = path;
        if (!TextUtils.isEmpty(path)) {
            mVideoView.setOnPreparedListener(this);
            mVideoView.setOnErrorListener(this);
            mVideoView.setOnCompletionListener(this);
            mVideoView.setOnBufferingUpdateListener(this);
            mVideoView.setOnInfoListener(this);
            mVideoView.enableBufferState(enableBufferState);
            if(mEnableBufferState){
                mVideoView.setAudioTrackStreamType(AudioManager.STREAM_MUSIC);
            }else {
                mVideoView.setAudioTrackStreamType(AudioManager.STREAM_VOICE_CALL);
            }
            startPlayBack(path);
        }
        mInitEnd = true;
    }


    public int getOrientation() {
        return mIsFullScreen ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public void start() {
        mVideoView.start();
    }

    @Override
    public void pause() {
        mVideoView.pause();
    }

    @Override
    public int getDuration() {
        return mVideoView.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mVideoView == null ? 0 : mVideoView.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mVideoView.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mVideoView.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return mVideoView.getBufferPercentage();
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void showMoreMenu(View v) {
        mMenu.showAtLocation(mRootView, Gravity.BOTTOM, 0, 50);
    }


    @Override
    public void onWifiConnected() {
    }

    private void initLinker() {
        mYfLinkerKit = new YfLinkerKit(PlayStreamActivity.this);
        mYfLinkerKit.setLinkerCallBack(new YfLinkerKit.LinkerCallBack() {
            @Override
            public void onStreamerResponse(boolean accept) {
                showToast(accept ? "主播同意你的连麦,开始推流..." : "主播拒绝了你的连麦。。。");
                Log.d(TAG, "onStreamerResponse: " + accept);
                if (accept) {
                    //主播同意连麦请求
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //开始推流,推流成功后发送通知onServerConnect
                            linkToStreamer();
                        }
                    });
                } else {
                    //主播拒绝
                }
            }

            @Override
            public void onStreamerQuit() {
                showToast("主播关闭连麦！");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        exitLink(false);
                    }
                });

            }


            @Override
            public void onError(int errorCode, String errorMsg) {
                showToast(errorMsg);
            }

            @Override
            public void onStreamLinkSuccess() {
                Log.d(TAG, "onStreamLinkSuccess");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mVideoView.selectAudio(2);
                        mVideoView.setDelayTimeMs(mLinkBufferMs, mLinkBufferMs);
                        openVideo(mCurrentPath+"?audio-channel-id=1", false);
                    }
                });
            }
        });
    }

    boolean setupSurface, startWhenPrepared, mForceStop;
    int PREVIEW_WIDTH = 1280, PREVIEW_HEIGHT = 720;
    int OUTPUT_WIDTH = 240, OUTPUT_HEIGHT = 144;

    private YfLinkerKit mYfLinkerKit;
    private YfBlurBeautyFilter mBeautyFilter;

    @Override
    public void linkToStreamer() {
        if (mSecondSurface != null) {
            mVideoViewLayout.removeView(mSecondSurface);
        }
        mSecondSurface = new GLSurfaceView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(360, 640);
        int screenWidth = DeviceUtil.getScreenWidth(this);
        int screenHeight = DeviceUtil.getScreenHeight(this);
        lp.width = (int) ((float) OUTPUT_HEIGHT * 11 / 10 / 360 * screenWidth);
        lp.height = lp.width / 9 * 16;
//        lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        lp.setMargins(screenWidth - lp.width - MainActivity.DELTA_X * screenWidth / 360 + 16,
                screenHeight - lp.height - MainActivity.DELTA_Y * screenHeight / 640,
                screenWidth - MainActivity.DELTA_X * screenWidth / 360,
                screenHeight - MainActivity.DELTA_Y * screenHeight / 640);
        mSecondSurface.setLayoutParams(lp);
        mSecondSurface.setZOrderOnTop(true);
        mVideoViewLayout.addView(mSecondSurface);
        yfEncoderKit = new YfEncoderKit(this, CACHE_DIRS, PREVIEW_WIDTH, PREVIEW_HEIGHT, OUTPUT_WIDTH, OUTPUT_HEIGHT, mEnableHWEncoder, 24);
        yfEncoderKit.setContinuousFocus()//设置连续自动对焦
                .enableFlipFrontCamera(true)//设置前置摄像头是否镜像处理，默认为false
                .setRecordMonitor(this)//设置回调
                .setDefaultCamera(true)//设置默认打开后置摄像头---不设置也默认打开后置摄像头
                .openCamera(mSecondSurface);//设置预览窗口
        mBeautyFilter = new YfBlurBeautyFilter(this);
        yfEncoderKit.addFilter(mBeautyFilter);//默认打开滤镜
        if (setupSurface) {
            startRecorder();
        } else {
            startWhenPrepared = true;
        }
    }

    @Override
    protected void openSecondStreamDEBUG() {
        //just for debug
        mVideoView.selectAudio(2);
//        mVideoView.setBufferSizeByMs(6000);
        mVideoView.setDelayTimeMs(mLinkBufferMs, mLinkBufferMs);
        openVideo(mCurrentPath, false);
    }

    /**
     * 打开另一条主播流
     */
    @Override
    protected void openAnotherStream() {
        Log.d(TAG, "openAnotherStream:" + mAnotherStreamPath);
        if (mSecondPlayer != null) {
            mVideoViewLayout.removeView(mSecondPlayer);
        }
        mSecondPlayer = new YfPlayerKit(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(360, 640);
        int screenWidth = DeviceUtil.getScreenWidth(this);
        lp.width = (int) ((float) OUTPUT_HEIGHT / 360 * screenWidth);
        lp.height = lp.width / 9 * 16;
        lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        mSecondPlayer.setLayoutParams(lp);
        mSecondPlayer.getSurfaceView().getHolder().setFormat(PixelFormat.TRANSPARENT);
        mSecondPlayer.getSurfaceView().setZOrderOnTop(true);
        mVideoViewLayout.addView(mSecondPlayer);
        mSecondPlayer.enableUDP(mEnablePlayerUdp);
        mSecondPlayer.selectAudio(1);
        mSecondPlayer.setHardwareDecoder(false);
        mSecondPlayer.setBufferSizeByMs(6000);
//        mVideoView.setDelayTimeMs(mLinkBufferMs, mLinkBufferMs);
//        mVideoView.enableBufferState(false);
//        mVideoView.setBufferSize(15 * 1024 * 1024);
        mSecondPlayer.setVideoPath(mAnotherStreamPath);
        mSecondPlayer.start();

    }


    /**
     * 退出连麦 释放资源
     */
    @Override
    protected void exitLink(boolean notify) {
        Log.d(TAG, "exitLink!");
        releaseYfLinkerKit(notify);
        releaseRecord();
        mVideoView.setBufferSizeByMs(6000);
        mVideoView.selectAudio(1);
        openVideo(mAudiencePath, true);//播放原始地址
    }

    /**
     * 停止推流并释放推流资源
     */
    private void releaseRecord() {
        if (mSecondSurface != null)
            mSecondSurface.setVisibility(View.GONE);//隐藏推流预览窗口
        if (yfEncoderKit != null) {
            mForceStop = true;
            yfEncoderKit.stopRecord();//停止推流
            yfEncoderKit.release();//释放编码器资源
            yfEncoderKit = null;
        }
    }

    private void releaseYfLinkerKit(boolean notify) {
        Log.d(TAG, "releaseYfLinkerKit");
        if (mYfLinkerKit == null) {
            return;
        }
        if (notify)//如果是主播发起的停止连麦通知，就不需要发这个通知给主播了
            mYfLinkerKit.stopLink();//通知主播停止连麦
        mYfLinkerKit.exit();//退出连麦服务器
        mYfLinkerKit = null;
    }

    /**
     * 通知主播推流成功，将粉丝连麦流的播放地址发送给主播
     */
    private void notifyPushSuccess() {
        mYfLinkerKit.notifyLinkToStreamer(mFansPlayPath);
    }

    /**
     * 请求连麦主播
     */
    protected void requestLinkInternal() {
        showToast("请求连麦中，等待主播回应。。。");
        mYfLinkerKit.requestToLink();
    }

    /**
     * 查询主播是否开启连麦
     */
    @Override
    protected void requestLink() {
        Log.d(TAG, "queryLink");
        if (mYfLinkerKit == null)
            initLinker();
        mYfLinkerKit.getStreamInfo(mAudiencePath
                , new QueryLinkCallback() {
                    @Override
                    public void onSuccess(boolean isOpen) {
                        Log.d(TAG, String.format("queryLink onSuccess isOpen: %s, srtmp: %s, roomID: %s, ip: %s", isOpen, "", "", ""));
                        if (isOpen) {//主播已开启连麦
//                            showToast("主播已开启连麦功能");
                            requestLinkInternal();
                        } else {
                            showToast("主播未开启连麦功能。。。");
                        }
                    }

                    @Override
                    public void onFailed(int code) {
                        Log.d(TAG, "queryLink onFailed code: " + code);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.d(TAG, "queryLink onError: " + e.getMessage());
                    }
                });
    }

    @Override
    protected void switchCamera() {
        if (yfEncoderKit != null) {
            yfEncoderKit.switchCamera();
        }
    }

    protected void startRecorder() {
        if (yfEncoderKit == null || yfEncoderKit.isRecording()) {//不允许推流过程中进行参数设置
            return;
        }
        Log.d(TAG, "开始录制");
        //设置编码参数：直播/录制、是否硬编、码率、宽、高
        yfEncoderKit.changeMode(YfEncoderKit.MODE_LIVE, 300);
        yfEncoderKit.setBufferSizeBySec(1);//最多缓存1秒的数据，超过1秒则丢帧
        if (mEnableStreamerUdp && mK2Pagent == null) {
            mK2Pagent = new K2Pagent(K2Pagent.USER_MODE_PUSH, K2Pagent.NET_MODE_UDP, mFansPushPath, 5000,
                    new byte[]{12}, new K2Pagent.K2PagentCallback() {
                @Override
                public void onPostSpeed(double send, double recv) {
                    Log.d(TAG, "onPostSpeed: " + send + " ---"+recv);
                }

                @Override
                public void onPostInfo(String info) {
                    Log.d(TAG, "onPostInfo: " + info);
                }
            });
            mFansPushPath = mK2Pagent.getUrl();
        }
        yfEncoderKit.setLiveUrl(mFansPushPath);
        yfEncoderKit.enableAEC(true);
        yfEncoderKit.startRecord();
        mForceStop = false;
    }


    private void onPlayError() {
        Log.d(TAG, "wifi状态：" + Utils.isWiFiActive(this) + "————播放状态：" + mVideoView.isPlaying());
        mVideoView.pause();
        if (!Utils.isWiFiActive(this) && !mVideoView.isPlaying()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("继续播放吗?");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startPlayBack(mCurrentPath);
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();

        }
    }

    private void startPlayBack(String path) {
        mStartPlayTime = SystemClock.elapsedRealtime();
        mVideoView.enableUDP(mEnablePlayerUdp);
        mVideoView.setOnNativeAudioDecodedListener(new YfCloudPlayer.OnNativeAudioDataDecoded() {
            @Override
            public void onAudioDataDecoded(YfCloudPlayer mp, byte[] data, int length, long pts) {
                if (yfEncoderKit != null) {
                    yfEncoderKit.onSecondAudioDecoded(data, length);
                }
            }
        });
        mVideoView.setVideoPath(path);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG, "!audioManager.isWiredHeadsetOn()" + !audioManager.isWiredHeadsetOn());
        audioManager.setSpeakerphoneOn(!audioManager.isWiredHeadsetOn());
        mVideoView.start();
    }

    private boolean mBackPressed;

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        mGetCacheHandler.removeCallbacks(updateCacheDisplay);
        setResult(RESULT_OK);
        mBackPressed = true;
        if (mVideoView != null) {
            mVideoView.release(true);
        }
        Log.d(TAG, "这个activity已经onDestroy");
        if (mK2Pagent != null) {
            mK2Pagent.destroy();
        }
        if (yfEncoderKit != null) {
            yfEncoderKit.release();
            yfEncoderKit = null;
        }
        if(mSecondPlayer!=null){
            mSecondPlayer.release(true);
        }
        super.onBackPressed();
    }

    protected void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PlayStreamActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPrepared(YfCloudPlayer mp) {
    }

    boolean surfaceCreated = false;
    boolean needToReopenVideo = false;
    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated!!" + holder);
            surfaceCreated = true;
            if (mVideoView != null) {
                if (needToReopenVideo) {
                    needToReopenVideo = false;
                    openVideo(mCurrentPath, mEnableBufferState);
                }
                mVideoView.setVolume(1, 1);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged!!" + holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed!!" + holder);
            surfaceCreated = false;
        }
    };

    private void toggleLoading(boolean show) {
        if (show) {
            mLoadingView.startAnimation(mAnim);
            mLoadingView.setVisibility(View.VISIBLE);
        } else {
            mLoadingView.clearAnimation();
            mLoadingView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCompletion(YfCloudPlayer mp) {
        //直播流收到播放完成的回调当异常处理
        if (surfaceCreated)//只有当surface存在的时候才能打开视频
            openVideo(mCurrentPath, mEnableBufferState);//简单粗暴地重连
        else
            needToReopenVideo = true;//设置标志位，在surface初始化后打开视频
    }

    @Override
    public boolean onInfo(YfCloudPlayer mp, int what, int extra) {
        Log.d(TAG, "onInfo from YfPlayerKitDemo:" + what + "__" + extra);
        switch (what) {
            case YfPlayerKit.INFO_CODE_BUFFERING_START:
                mBufferSpend = SystemClock.elapsedRealtime();
                mBufferTimes++;
                toggleLoading(true);
                break;
            case YfPlayerKit.INFO_CODE_BUFFERING_END:
                toggleLoading(false);
                mBufferSpend = SystemClock.elapsedRealtime() - mBufferSpend;
                break;
            case YfPlayerKit.INFO_CODE_VIDEO_RENDERING_START:
                toggleLoading(false);
                Log.d(TAG, "打开完成，花费时间为：" + (SystemClock.elapsedRealtime() - mStartPlayTime));
                break;
        }
        return false;
    }

    @Override
    public boolean onError(YfCloudPlayer mp, int what, int extra) {
        Log.d(TAG, "onError from YfPlayerKitDemo:" + what + "__" + extra);
        if(what==YfPlayerKit.ERROR_CODE_AUTH_FAILED){
            Toast.makeText(PlayStreamActivity.this, "播放鉴权失败", Toast.LENGTH_SHORT).show();
        }else {
            if (surfaceCreated)//只有当surface存在的时候才能打开视频
                openVideo(mCurrentPath, mEnableBufferState);//简单粗暴地重连
            else
                needToReopenVideo = true;//设置标志位，在surface初始化后打开视频
        }
        return false;
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mVideoView != null)
            if (mBackPressed) {
                mVideoView.release(true);
                mVideoView = null;
            } else {
                mVideoView.setVolume(0, 0);
            }
        if (mSecondPlayer != null)
            if (mBackPressed) {
                mSecondPlayer.release(true);
                mSecondPlayer = null;
            } else {
                mSecondPlayer.setVolume(0, 0);
            }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.start();
            Log.d(TAG, "mVideoView.resume() ");
            mVideoView.setVolume(1, 1);
        }

    }

    Runnable updateCacheDisplay = new Runnable() {
        @Override
        public void run() {
            mVd.setText("v-sec:" + mVideoView.getVideoCachedDuration());
            mAd.setText("A-sec:" + mVideoView.getAudioCachedDuration());
            mVb.setText("v-buf:" + mVideoView.getVideoCachedBytes());
            mAb.setText("A-buf:" + mVideoView.getAudioCachedBytes());
            mGetCacheHandler.removeCallbacks(updateCacheDisplay);
            mGetCacheHandler.postDelayed(updateCacheDisplay, 500);
        }
    };

    @Override
    public void onBufferingUpdate(YfCloudPlayer mp, int percent) {

    }

    ///////////////////////////////////////////推流模块的监听///////////////////////////////////////////////////////

    boolean onServerConnected;

    @Override
    public void onServerConnected() {
        onServerConnected = true;
        Toast.makeText(this, "推流成功", Toast.LENGTH_SHORT).show();
        if (!mDebug)
            notifyPushSuccess();
    }

    @Override
    public void onError(int mode, int err, String msg) {
        onServerConnected = false;
        if (err == YfEncoderKit.ERR_AUTH_FAILED) {
            com.demo.yflinkdemo.widget.Log.i(TAG, "####### error: 鉴权失败");
            Toast.makeText(this, "err:推流鉴权失败" + msg, Toast.LENGTH_SHORT).show();
        } else if (!mForceStop)
            startRecorder();
    }

    @Override
    public void onStateChanged(int mode, int oldState, int newState) {
        if (newState == YfEncoderKit.STATE_PREPARED) {
            setupSurface = true;
            if (startWhenPrepared) {
                startRecorder();
            }
        }
    }

    @Override
    public void onFragment(int mode, String fragPath,boolean success) {

    }


    @Override
    public void onInfo(int what, double arg1, double arg2, Object obj) {
        if (what == YfEncoderKit.INFO_IP) {
            Log.d(TAG, "实际推流的IP地址:" + intToIp((int) arg1));
        }
    }


}
