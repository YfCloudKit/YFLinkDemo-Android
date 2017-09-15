package com.demo.yflinkdemo;

import com.demo.yflinkdemo.extra.CustomMediaController;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.yflinkdemo.widget.DeviceUtil;
import com.yunfan.net.K2Pagent;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfPlayerKit;

import static com.demo.yflinkdemo.BasePlayerDemo.HARD_ENCODER;
import static com.demo.yflinkdemo.BasePlayerDemo.PLAYER_UDP;
import static com.demo.yflinkdemo.BasePlayerDemo.PULL_PATH;
import static com.demo.yflinkdemo.BasePlayerDemo.PULL_PATH_ANOTHER;
import static com.demo.yflinkdemo.BasePlayerDemo.STREAMER_UDP;

public class AudienceActivity extends AppCompatActivity implements YfCloudPlayer.OnPreparedListener, YfCloudPlayer.OnErrorListener, YfCloudPlayer.OnCompletionListener, YfCloudPlayer.OnBufferingUpdateListener, YfCloudPlayer.OnInfoListener {
    private static final String TAG = "Yf_AudienceActivity";
    private CustomMediaController mMediaController;
    private YfPlayerKit mVideoView;
    private FrameLayout mVideoViewLayout;
    private ImageView mLoadingView;
    private RotateAnimation mAnim;
    private String mAudiencePath;
    private Handler mGetCacheHandler;
    private boolean mEnableBufferState;
    private boolean mInitEnd;
    private String mCurrentPath;
    private long mStartPlayTime;
    private K2Pagent mK2Pagent;
    private boolean mEnablePlayerUdp;
    private boolean needToReopenVideo;
    private TextView mVb;
    private TextView mAb;
    private TextView mVd;
    private TextView mAd;
    private YfPlayerKit mSecondPlayer;
    private String mAnotherAudiencePath;
    private boolean mOpenAnotherStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        YfPlayerKit.enableRotation(false);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_audience);
        mMediaController = new CustomMediaController(this);
        findViews();
        initVideoView();
        init();
    }

    private void findViews() {
        mVideoView = (YfPlayerKit) findViewById(R.id.surface_view);
        mVideoViewLayout = (FrameLayout) findViewById(R.id.videoView_layout);
        mLoadingView = (ImageView) findViewById(R.id.loading_img);

        mVb = (TextView) findViewById(R.id.video_buffer);
        mAb = (TextView) findViewById(R.id.audio_buffer);
        mVd = (TextView) findViewById(R.id.video_frame_duaration);
        mAd = (TextView) findViewById(R.id.audio_frame_duaration);
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

    private boolean surfaceCreated;
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

    private void init() {
        mEnablePlayerUdp = getIntent().getBooleanExtra(PLAYER_UDP, false);

        mAudiencePath = MainActivity.PULL_HOST + getIntent().getStringExtra(PULL_PATH);
        mAnotherAudiencePath = MainActivity.PULL_HOST + getIntent().getStringExtra(PULL_PATH_ANOTHER);
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

    private void startPlayBack(String path) {
        mStartPlayTime = SystemClock.elapsedRealtime();
        String tempPath = path;
        if (mEnablePlayerUdp && mK2Pagent == null) {
            mK2Pagent = new K2Pagent(K2Pagent.USER_MODE_PUSH, K2Pagent.NET_MODE_UDP, tempPath, 5000,
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
            tempPath = mK2Pagent.getUrl();
        }
        mVideoView.setOnNativeAudioDecodedListener(new YfCloudPlayer.OnNativeAudioDataDecoded() {
            @Override
            public void onAudioDataDecoded(YfCloudPlayer mp, byte[] data, int length, long pts) {
            }
        });
        mVideoView.setVideoPath(tempPath);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG, "!audioManager.isWiredHeadsetOn()" + !audioManager.isWiredHeadsetOn());
        audioManager.setSpeakerphoneOn(!audioManager.isWiredHeadsetOn());
        mVideoView.start();
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
            mVideoView.setAudioTrackStreamType(AudioManager.STREAM_MUSIC);
            startPlayBack(path);
        }
        mInitEnd = true;
    }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_audience, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open_another_stream:
                mOpenAnotherStream = !mOpenAnotherStream;
                if (mOpenAnotherStream) {
                    openAnotherStream();
                } else {
                    if (mSecondPlayer != null) {
                        mVideoViewLayout.removeView(mSecondPlayer);
                        mSecondPlayer.stopPlayback();
                        mSecondPlayer.release(true);
                        mSecondPlayer = null;
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    int OUTPUT_WIDTH = 240, OUTPUT_HEIGHT = 144;

    private void openAnotherStream() {
        Log.d(TAG, "openAnotherStream:" + mAnotherAudiencePath);
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
        mVideoViewLayout.addView(mSecondPlayer);
        mSecondPlayer.getSurfaceView().getHolder().setFormat(PixelFormat.TRANSPARENT);
        mSecondPlayer.getSurfaceView().setZOrderOnTop(true);

        if (mEnablePlayerUdp && mK2Pagent == null) {
            mK2Pagent = new K2Pagent(K2Pagent.USER_MODE_PUSH, K2Pagent.NET_MODE_UDP, mAnotherAudiencePath, 5000,
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
            mAnotherAudiencePath = mK2Pagent.getUrl();
        }
        mSecondPlayer.setHardwareDecoder(false);
        mSecondPlayer.setBufferSizeByMs(6000);
        mSecondPlayer.setAudioTrackStreamType(AudioManager.STREAM_MUSIC);
//        mVideoView.setDelayTimeMs(mLinkBufferMs, mLinkBufferMs);
//        mVideoView.enableBufferState(false);
//        mVideoView.setBufferSize(15 * 1024 * 1024);
        mSecondPlayer.setVideoPath(mAnotherAudiencePath);
        mSecondPlayer.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.start();
            Log.d(TAG, "mVideoView.resume() ");
            mVideoView.setVolume(1, 1);
        }
        if (mSecondPlayer != null) {
            mSecondPlayer.start();
            mSecondPlayer.setVolume(1, 1);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closePlayer();
    }

    private void closePlayer() {
        if (mVideoView != null) {
            if (mVideoView.isPlaying())
                mVideoView.release(true);
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
    public void onPrepared(YfCloudPlayer mp) {

    }

    @Override
    public boolean onError(YfCloudPlayer mp, int what, int extra) {
        switch (what){
            case YfPlayerKit.ERROR_CODE_AUTH_FAILED:
                Toast.makeText(AudienceActivity.this, "播放鉴权失败", Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
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
    public void onBufferingUpdate(YfCloudPlayer mp, int percent) {

    }

    @Override
    public boolean onInfo(YfCloudPlayer mp, int what, int extra) {
        Log.d(TAG, "onInfo from YfPlayerKitDemo:" + what + "__" + extra);
        switch (what) {
            case YfPlayerKit.INFO_CODE_BUFFERING_START:
                toggleLoading(true);
                break;
            case YfPlayerKit.INFO_CODE_BUFFERING_END:
                toggleLoading(false);
                break;
            case YfPlayerKit.INFO_CODE_VIDEO_RENDERING_START:
                toggleLoading(false);
                Log.d(TAG, "打开完成，花费时间为：" + (SystemClock.elapsedRealtime() - mStartPlayTime));
                break;
        }
        return false;
    }

    public static void startActivity(Context context, String pullPath, String anotherStreamPath,
                                     boolean playerUDP, boolean streamerUDP,
                                     boolean hardEncoder) {
        Intent i = new Intent(context, AudienceActivity.class);
        i.putExtra(PULL_PATH, pullPath);
        i.putExtra(PULL_PATH_ANOTHER, anotherStreamPath);
        i.putExtra(PLAYER_UDP, playerUDP);
        i.putExtra(STREAMER_UDP, streamerUDP);
        i.putExtra(HARD_ENCODER, hardEncoder);
        context.startActivity(i);
    }
}
