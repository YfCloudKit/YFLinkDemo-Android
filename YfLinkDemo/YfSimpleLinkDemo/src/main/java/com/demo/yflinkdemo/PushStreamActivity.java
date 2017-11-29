package com.demo.yflinkdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.yflinkdemo.extra.Utils;
import com.demo.yflinkdemo.widget.DeviceUtil;
import com.demo.yflinkdemo.widget.Log;
import com.demo.yflinkdemo.widget.LogRecorder;
import com.demo.yflinkdemo.widget.ScaleGLSurfaceView;
import com.yunfan.encoder.effect.filter.AlphaBlendFilter;
import com.yunfan.encoder.filter.YfBlurBeautyFilter;
import com.yunfan.encoder.widget.RecordMonitor;
import com.yunfan.encoder.widget.YfEncoderKit;
import com.yunfan.net.K2Pagent;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfPlayerKit;
import com.yunfan.yflinkkit.YfStreamerKit;

/**
 * 主播与观众连麦，该Activity为主播端
 */
public class PushStreamActivity extends AppCompatActivity implements RecordMonitor {
    protected static final String TAG = "YfRecorder_Live";
    // 默认的直播发起url
//    public static String URL_LIVE = "rtmp://192.168.3.138/mytv/";
//    public static String URL_LIVE = "rtmp://live.live3721.com/mytv/grand110";
//    public static String URL_LIVE = "rtmp://push.yftest.yflive.net/live/test111";
//    public static String URL_LIVE = "rtmp://yfstream.livestar.com/live/test111";
//        public static String URL_LIVE = "rtmp://";
    //114.215.182.69:1935
    //172.17.11.233:1936/test/yunfan

    //设置保存截图等文件的文件夹
    public static String CACHE_DIRS = Environment.getExternalStorageDirectory().getPath() + "/yunfanencoder";
    protected YfEncoderKit yfEncoderKit;
    protected boolean setBeauty = false;
    protected boolean setLogo = false;
    protected boolean dataShowing = false;
    protected boolean enableAudio = true;
    protected boolean autoFocus = true;//默认自动对焦
    private boolean startRecoderAuto = true;
    private ScaleGLSurfaceView mGLSurfaceView;
    private LinearLayout actionbarLayout, infoLayout;
    private TextView textBitrate, textBuffer;
    private ActionBar actionBar;
    private int surfaceWidth, surfaceHeight;
    private boolean mLandscape = false;
    private boolean mEnableFilter = true;
    protected static final int VIDEO_WIDTH = 640;
    protected static final int VIDEO_HEIGHT = 368;
    protected static final int VIDEO_FRAME_RATE = 24;

    protected int PREVIEW_WIDTH;
    protected int PREVIEW_HEIGHT;
    protected int VIDEO_BITRATE = 600;

    protected LogRecorder logRecoder = new LogRecorder();
    private Handler infoShower = new Handler();
    private int mCurrentBitrate, mCurrentBufferMs;
    private static final String URL_LIVE = "URL_LIVE";
    private static final String SECOND_URL_LIVE = "SECOND_URL_LIVE";
    private static final String PLAYER_UDP = "PLAYER_UDP";
    private static final String STREAMER_UDP = "STREAMER_UDP";
    private static final String HARD_ENCODER = "HARD_ENCODER";
    private static final String HARD_AEC = "HARD_AEC";
    private static final String LINK_BUFFER = "LINK_BUFFER";
    /**
     * 观众流推流地址
     */
    private String mAudienceUrl;
    /**
     * 观众流推流地址-udp
     */
    private String mUDPUrl;


    private boolean mEnableLink = false;
    private boolean mEnableHWEncoder = true;
    private String mDebugUrl;
    private boolean ENABLE_PLAYER_UDP, ENABLE_STREAMER_UDP,ENABLE_HARDWARE_AEC;
    private final int BEAUTY_INDEX = 1, LOGO_INDEX = 2;
    private int mLinkBufferMs;
    private YfBlurBeautyFilter mBeautyFilter;
    private AlphaBlendFilter mLogoFilter;
    private boolean mEnableAudioPlay;
    /**
     * 主播的播放地址，根据该地址创建连麦房间
     */
    private String mHostPlayUrl;
    private K2Pagent mK2Pagent;

    public static void startActivity(Context context, String pushUrl, String secondUrl, boolean playerUDP, boolean streamerUDP, boolean hardEncoder,boolean hardwareAEC, int bufferMs) {
        Intent i = new Intent(context, PushStreamActivity.class);
        i.putExtra(URL_LIVE, pushUrl);
        i.putExtra(SECOND_URL_LIVE, secondUrl);
        i.putExtra(PLAYER_UDP, playerUDP);
        i.putExtra(STREAMER_UDP, streamerUDP);
        i.putExtra(HARD_ENCODER, hardEncoder);
        i.putExtra(HARD_AEC, hardwareAEC);
        i.putExtra(LINK_BUFFER, bufferMs);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLandscape = false;
        mEnableFilter = true;
        mEnableFilter = mEnableFilter && YfEncoderKit.canUsingFilter();
        ENABLE_PLAYER_UDP = getIntent().getBooleanExtra(PLAYER_UDP, false);
        ENABLE_STREAMER_UDP = getIntent().getBooleanExtra(STREAMER_UDP, false);
        ENABLE_HARDWARE_AEC = getIntent().getBooleanExtra(HARD_AEC, false);
        mLinkBufferMs = getIntent().getIntExtra(LINK_BUFFER, 400);
        YfPlayerKit.enableRotation(true);//主播端使用textureview播放视频，可以简单解决surfaceview被glsurfaceview覆盖的问题
        mAudienceUrl = MainActivity.PUSH_HOST + getDefaultUrl();//第一条流推流地址
        mHostPlayUrl = MainActivity.PULL_HOST + getDefaultUrl();


        if (mEnableFilter) {
            PREVIEW_WIDTH = 1280;
            PREVIEW_HEIGHT = 720;

        } else {
            PREVIEW_WIDTH = 640;
            PREVIEW_HEIGHT = 480;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkEncoderPermission();
        } else {
            initView();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_encoder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_start:
                startRecorder(true);
                break;
            case R.id.action_stop:
                stopRecorder();
                break;
            case R.id.action_close_capture:
                yfEncoderKit.captureCurrentFrame(System.currentTimeMillis() + "", new YfEncoderKit.OnPictureSaveListener() {
                    @Override
                    public void onSaved(final String result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PushStreamActivity.this, "截图成功:" + result, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                break;
            case R.id.action_switch:
                yfEncoderKit.switchCamera();
                break;
            case R.id.action_manual_focus:
                if (!autoFocus) {
                    autoFocus = true;
                    yfEncoderKit.autoFocus();
                } else {
                    actionBar.show();
                    autoFocus = false;
                }
                break;
            case R.id.action_torch:
                yfEncoderKit.setFlash(!yfEncoderKit.isFlashOn());
                break;

            case R.id.action_enable_audio:
                enableAudio = !enableAudio;
                yfEncoderKit.enablePushAudio(enableAudio);
                Toast.makeText(this, (enableAudio ? "恢复" : "暂停") + "推送音频流", Toast.LENGTH_LONG).show();
                break;
            case R.id.action_set_beauty:
                if (!setBeauty) {
                    if (mBeautyFilter == null) {
                        mBeautyFilter = new YfBlurBeautyFilter(this);
                        mBeautyFilter.setIndex(BEAUTY_INDEX);
                    }
                    yfEncoderKit.addFilter(mBeautyFilter);
                } else {
                    yfEncoderKit.removeFilter(BEAUTY_INDEX);
                }
                setBeauty = !setBeauty;
                break;
            case R.id.action_set_logo:
                if (!setLogo) {
                    if (mLogoFilter == null) {
                        mLogoFilter = new AlphaBlendFilter();
                        mLogoFilter.setIndex(LOGO_INDEX);
                        float landscapeMarginRight = 0.1f;//横屏模式下logo的marginright所占宽度的比例
                        float portMarginRight = 0.05f;//竖屏模式下logo的marginright所占宽度的比例
                        float landsapeMarginTop = 0.05f;//横屏模式下logo的marginTop所占宽度的比例
                        float portMarginTop = 0.1f;//竖屏模式下logo的marginTop所占宽度的比例
                        float landscapeLogoHeight = 0.2f;//横屏模式下logo的高度所占屏幕高度的比例
                        float logoWidth = 454, logoHeight = 160;//计算logo的比例
                        if (mLandscape) {
                            /**
                             * 配置logo的源及在画面中的位置，请注意屏幕的横屏竖屏模式及屏幕比例
                             * 需要注意的是在有内置虚拟键的情况下屏幕比例并不是16:9
                             * 这里不对该情况进行处理，仅考虑一般的16:9的情况。
                             * @param bitmap logo源
                             * @param widthPercent logo的宽度占屏幕宽度的比例（0~1）
                             * @param heightPercent logo的高度占屏幕高度的比例（0~1），譬如宽度设置为0.2f，那么在通常16:9竖屏的情况下，宽高比1：1的logo这里就应该是 0.2f * 9 / 16
                             * @param xPercent logo左边缘相对屏幕左边缘的距离比（0~1），通常情况下，该值与widthPercent之和不应大于1，否则logo则无法完全显示在屏幕内
                             * @param yPercent logo上边缘相对屏幕上边缘的距离比（0~1），通常情况下，该值与heightPercent之和不应大于1，否则logo则无法完全显示在屏幕内
                             *                 清楚上述四个参数后，可以根据个人需求配置图片大小及位置。
                             * @return
                             */
                            mLogoFilter.config(BitmapFactory.decodeResource(getResources(), R.mipmap.logo), landscapeLogoHeight * 9 / 16 * logoWidth / logoHeight, landscapeLogoHeight, 1 - landscapeLogoHeight * 9 / 16 * logoWidth / logoHeight - landscapeMarginRight, landsapeMarginTop);
                        } else {
                            mLogoFilter.config(BitmapFactory.decodeResource(getResources(), R.mipmap.logo), landscapeLogoHeight * logoWidth / logoHeight, landscapeLogoHeight * 9 / 16, 1 - landscapeLogoHeight * logoWidth / logoHeight - portMarginRight, portMarginTop);

                        }
                    }

                    yfEncoderKit.addFilter(mLogoFilter);
                } else {
                    yfEncoderKit.removeFilter(LOGO_INDEX);
                }
                setLogo = !setLogo;
                break;
            case R.id.action_flip_camera:
                yfEncoderKit.enableFlipFrontCamera(!yfEncoderKit.isFlipFrontCameraEnable());
                break;
            case R.id.action_show_data:
                if (dataShowing) {
                    infoShower.removeCallbacks(updateDisplay);
                    infoLayout.setVisibility(View.GONE);
                } else {
                    infoShower.removeCallbacks(updateDisplay);
                    infoShower.postDelayed(updateDisplay, 1000);
                    infoLayout.setVisibility(View.VISIBLE);
                }
                dataShowing = !dataShowing;
                break;
            case R.id.action_show_history:
                showLogs();
                break;
            case R.id.action_audio_play:
                mEnableAudioPlay = !mEnableAudioPlay;
                yfEncoderKit.enableAudioPlay(mEnableAudioPlay);
                break;
            case R.id.action_enable_link:
                enableLink(!mEnableLink);
                break;
            case R.id.action_link:
                //just for debug
                mEnableLink = true;
                mDebugUrl = MainActivity.LINKER_PULL_HOST + getIntent().getStringExtra(SECOND_URL_LIVE) /*+ ".flv"*/;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkSdcardPermission(mDebugUrl);
                } else {
                    openVideo(mDebugUrl);
                }
                break;
            case R.id.action_stop_link:
                if (mYfStreamerKit != null)
                    mYfStreamerKit.stopLink();
                closePlayer();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "on resume!");
        if (yfEncoderKit != null)
            yfEncoderKit.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (yfEncoderKit != null)
            yfEncoderKit.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        enableLink(false);
        destroyRecorder();
    }

    private void toggleActionBar() {
        if (actionBar.isShowing()) {
            actionBar.hide();
        } else {
            actionBar.show();
        }
    }

    int OUTPUT_WIDTH = 240, OUTPUT_HEIGHT = 144;

    protected void initView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_recorder);
        infoLayout = (LinearLayout) findViewById(R.id.cache_info_layout);
        textBitrate = (TextView) findViewById(R.id.current_bitrate);
        textBuffer = (TextView) findViewById(R.id.current_buffer_size_ms);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionbarLayout = (LinearLayout) findViewById(R.id.actionbar_layout);
        mGLSurfaceView = (ScaleGLSurfaceView) findViewById(R.id.surface);
        mGLSurfaceView.initScaleGLSurfaceView(new ScaleGLSurfaceView.OnScareCallback() {
            @Override
            public int getCurrentZoom() {
                return yfEncoderKit.getCurrentZoom();
            }

            @Override
            public int getMaxZoom() {
                return yfEncoderKit.getMaxZoom();
            }

            @Override
            public boolean onScale(int zoom) {
                return yfEncoderKit.manualZoom(zoom);
            }
        });
        mGLSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP) {
                    if (autoFocus) {
                        toggleActionBar();
                        return false;
                    }
                    float xPercent = event.getX() / ((float) mGLSurfaceView.getWidth());
                    float yPercent = event.getY() / ((float) mGLSurfaceView.getHeight());
                    Rect focusRect = ScaleGLSurfaceView.calculateTapArea(xPercent, yPercent, 1f);
                    yfEncoderKit.manualFocus(focusRect);

                }
                return false;
            }
        });
        setSurfaceSize(mLandscape, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        mYfPlayerKit = (YfPlayerKit) findViewById(R.id.surface_view);
        resizeYfPlayerKit();
        initRecorder(mGLSurfaceView);

    }

    private void resizeYfPlayerKit() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(360, 640);
        int screenWidth = DeviceUtil.getScreenWidth(this);
        lp.width = (int) ((float) OUTPUT_HEIGHT / 360 * screenWidth);
        lp.height = lp.width / 9 * 16;
        lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        mYfPlayerKit.setLayoutParams(lp);
        mYfPlayerKit.setVideoLayout(YfPlayerKit.VIDEO_LAYOUT_MATCH_PARENT);
    }

    /**
     * @param landscape 是否为横屏模式，预览宽度，预览高度
     */
    private void setSurfaceSize(boolean landscape, int width, int height) {
        mLandscape = landscape;
        LayoutParams lp = mGLSurfaceView.getLayoutParams();
        int realScreenWidth = Utils.getScreenWidth(this);
        Log.d(TAG, "realScreenWidth:" + realScreenWidth + "," + landscape);
        if (landscape) {
            surfaceWidth = realScreenWidth * 16 / 9;
            surfaceHeight = surfaceWidth * height / width;
        } else {
            surfaceHeight = realScreenWidth * 16 / 9;
            //考虑到高度可能被内置虚拟按键占用，因此为了保证预览界面为16:9，不能直接获取高度。
            surfaceWidth = surfaceHeight * height / width;
        }
        lp.width = surfaceWidth;
        lp.height = surfaceHeight;
        Log.d(TAG, "计算出来的宽高:" + surfaceWidth + "___" + surfaceHeight);
        mGLSurfaceView.setLayoutParams(lp);
    }

    private void initRecorder(GLSurfaceView s) {
        Log.d(TAG, "初始化编码器");
        //初始化编码工具：context、截图/录制视频等文件保存的根目录、允许开启滤镜、摄像头输出宽度、摄像头输出高度
        //允许开启滤镜模式下后台只能推送音频、且无法使用软编
        mEnableHWEncoder = getIntent().getBooleanExtra(HARD_ENCODER, false);
        yfEncoderKit = new YfEncoderKit(this, CACHE_DIRS, PREVIEW_WIDTH, PREVIEW_HEIGHT, VIDEO_WIDTH, VIDEO_HEIGHT, mEnableHWEncoder, VIDEO_FRAME_RATE);
        yfEncoderKit.setContinuousFocus()//设置连续自动对焦
                .setLandscape(mLandscape)//设置是否横屏模式（默认竖屏）
                .enableFlipFrontCamera(false)//设置前置摄像头是否镜像处理，默认为true
                .setRecordMonitor(this)//设置回调
                .setDefaultCamera(false)//设置默认打开后置摄像头---不设置也默认打开后置摄像头
                .openCamera(s);//设置预览窗口
        mBeautyFilter = new YfBlurBeautyFilter(this);
        mBeautyFilter.setIndex(BEAUTY_INDEX);
        yfEncoderKit.addFilter(mBeautyFilter);//默认打开滤镜
        setBeauty = true;
        if (mLandscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        Log.d(TAG, "当前角度：" + getWindowManager().getDefaultDisplay().getRotation());
    }


    private String getDefaultUrl() {
        return getIntent().getStringExtra(URL_LIVE);
    }

    protected void startRecorder(boolean changeUI) {
        if (yfEncoderKit == null || yfEncoderKit.isRecording()) {//不允许推流过程中进行参数设置
            return;
        }
        if (changeUI)
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionbarLayout.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        Log.d(TAG, "开始录制");
        //设置编码参数：直播/录制、是否硬编、码率、宽、高
        yfEncoderKit.changeMode(YfEncoderKit.MODE_LIVE, VIDEO_BITRATE);
//        yfEncoderKit.setMaxReconnectCount(5);//自动重连次数，0代表不自动重连
//        yfEncoderKit.setAdjustQualityAuto(true, 300);//打开码率自适应，最低码率300k
        yfEncoderKit.setBufferSizeBySec(1);//最多缓存1秒的数据，超过1秒则丢帧
        mUDPUrl = mAudienceUrl;
        yfEncoderKit.enableUDP(ENABLE_STREAMER_UDP);
        yfEncoderKit.setLiveUrl(mUDPUrl);
        int x = VIDEO_HEIGHT - MainActivity.OUT_WIDTH;
        int y = VIDEO_WIDTH - MainActivity.OUT_HEIGHT;
        yfEncoderKit.setRemoteFramePosition(x - MainActivity.DELTA_X, y - MainActivity.DELTA_Y);
        yfEncoderKit.startRecord();
        if (dataShowing) {
            infoShower.removeCallbacks(updateDisplay);
            infoShower.postDelayed(updateDisplay, 500);
        }
        mForceStop = false;
    }


    Runnable updateDisplay = new Runnable() {
        @Override
        public void run() {
            textBuffer.setText("buffer-ms:" + mCurrentBufferMs);
            textBitrate.setText("bitrate:" + mCurrentBitrate);
            infoShower.removeCallbacks(this);
            infoShower.postDelayed(this, 500);
        }
    };


    protected void stopRecorder() {
        mForceStop = true;
        if (dataShowing) {
            infoShower.removeCallbacks(updateDisplay);
        }
        yfEncoderKit.stopRecord();
    }

    private void destroyRecorder() {
        Log.d(TAG, "销毁编码器");
        if (yfEncoderKit != null) {
            if (currentState == YfEncoderKit.STATE_RECORDING)
                stopRecorder();
            yfEncoderKit.release();
            yfEncoderKit = null;
        }

    }

    boolean onServerConnected;
    boolean mForceStop;

    @Override
    public void onServerConnected() {
        onServerConnected = true;
        if (yfEncoderKit != null) {
            Toast.makeText(this, "推流成功，编码方式:" + (yfEncoderKit.getEncodeMode() ? "硬编" : "软编"), Toast.LENGTH_SHORT).show();
            logRecoder.writeLog("推流成功，编码方式:" + (yfEncoderKit.getEncodeMode() ? "硬编" : "软编"));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionbarLayout.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        }
    }

    @Override
    public void onError(int mode, int err, String msg) {

        actionbarLayout.setBackgroundColor(getResources().getColor(R.color.blue));
        Log.i(TAG, "####### error: " + err + " " + msg);
        Toast.makeText(this, "err: no=" + err + " msg=" + msg, Toast.LENGTH_SHORT).show();
        onServerConnected = false;
        if (!mForceStop) {
            startRecorder(false);
            if (mLinking) {
                yfEncoderKit.prepareForLink();
            }
        }


    }

    private int currentState;
    private NetworkConnectChangedReceiver receiver;

    @Override
    public void onStateChanged(int mode, int oldState, int newState) {
        if (onServerConnected && newState == YfEncoderKit.STATE_RECORDING) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionbarLayout.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
            maybeRegisterReceiver();//监听wifi连接状况
        } else {
            if (startRecoderAuto && newState == YfEncoderKit.STATE_PREPARED) {
                startRecoderAuto = false;
            }

        }
        Log.i(TAG,
                "####### state changed: "
                        + YfEncoderKit.getRecordStateString(oldState) + " -> "
                        + YfEncoderKit.getRecordStateString(newState));
    }


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

    @Override
    public void onFragment(int mode, String fragPath,boolean success) {
        Log.i(TAG, "####### fragment: " + fragPath);

    }

    @Override
    public void onInfo(int what, double arg1, double arg2, Object obj) {
        if (what == YfEncoderKit.INFO_IP) {
            Log.d(TAG, "实际推流的IP地址:" + intToIp((int) arg1));
            logRecoder.writeLog("IP:" + intToIp((int) arg1));
        }
    }

    public static String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "."
                + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }



    @Override
    protected void onStop() {
        super.onStop();
        if (yfEncoderKit != null)
            yfEncoderKit.onStop(true);
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
                    Log.e(TAG, "isConnected:" + isConnected);
                    if (isConnected) {

                        logRecoder.writeLog("WIFI已连接");
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                logRecoder.writeLog("WIFI已断开");
                            }
                        });
                    }
                }
            }
        }
    }

    private static final int CODE_FOR_OPEN_CAMERA = 100;

    @TargetApi(Build.VERSION_CODES.M)
    private void checkEncoderPermission() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.CAMERA) | checkSelfPermission(Manifest.permission.RECORD_AUDIO) | checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, CODE_FOR_OPEN_CAMERA);
        } else {
            initView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CODE_FOR_OPEN_CAMERA) {
            if (permissions[0].equals(Manifest.permission.CAMERA)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[1].equals(Manifest.permission.RECORD_AUDIO)
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && permissions[2].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED
                    && permissions[3].equals(Manifest.permission.READ_PHONE_STATE)
                    && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                //用户同意使用camera
                initView();
            } else {
                //用户不同意，自行处理即可
                finish();
            }
        }
    }

    public void showLogs() {
        android.util.Log.d(TAG, "查看消息");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Infos").setItems(
                logRecoder.getLogs(), null).setPositiveButton("清空", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logRecoder.clearLogs();
                dialog.dismiss();
            }
        }).setNegativeButton(
                "关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    private AlertDialog mAlertDialog;

    private YfStreamerKit mYfStreamerKit;

    private void initLinkListener() {
        mYfStreamerKit = new YfStreamerKit(mHostPlayUrl, this);
        mYfStreamerKit.setStreamerCallBack(new YfStreamerKit.StreamerCallBack() {

            @Override
            public void onLinkerRequest() {
                Log.d(TAG, "onLinkerRequest");
                showRequestLinkDialog();
            }

            @Override
            public void onLinkerQuit() {
//                Log.d(TAG, "onLinkerQuit");
                showToast("粉丝已退出连麦");
//                // TODO: 2016/10/19 关闭播放器
                closePlayer();
            }

            @Override
            public void onLinkerConnected(final String fansPlayUrl) {
                Log.d(TAG, "onLinkerConnected fansPlayUrl：" + fansPlayUrl);
                showToast("粉丝推流成功，开始播放！");
                //粉丝推流成功开始播放
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            checkSdcardPermission(fansPlayUrl);
                        } else {
                            openVideo(fansPlayUrl);
                        }
                    }
                });
            }

            @Override
            public void onEnableLinkResult(boolean success) {
                mEnableLink = success;
                showToast(String.format("开启连麦 %s", success ? "成功" : "失败"));
                Log.d(TAG, String.format("开启连麦 %s", success ? "成功" : "失败"));
            }

        });
    }


    /**
     * 关闭播放器
     */
    private void closePlayer() {
        Log.d(TAG, "closePlayer");
        yfEncoderKit.stopHandleLinkStream();//编码器停止处理连麦粉丝流
        mLinking = false;
        if (mYfPlayerKit != null) {
            if (mYfPlayerKit.isPlaying())
                mYfPlayerKit.release(true);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mYfPlayerKit.setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * 主播开启/关闭连麦功能
     */
    private void enableLink(boolean enable) {
        Log.d(TAG, "enable link:" + enable);
        if (enable) {
            initLinkListener();
            mYfStreamerKit.enableLinkRequest();
        } else {
            showToast("关闭连麦功能");
            closePlayer();//关闭播放器
            if (mYfStreamerKit != null) {
                mYfStreamerKit.stopLink();//通知连麦粉丝主播已退出连麦
                mYfStreamerKit.exit();//通知服务器解散房间
                mYfStreamerKit = null;
            }

            mEnableLink = false;
        }

    }


    private void showRequestLinkDialog() {
        Log.d(TAG, "showRequestLinkDialog");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog = new AlertDialog.Builder(PushStreamActivity.this)
                        .setMessage("收到连麦请求,是否与之连麦？")
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mYfStreamerKit.responseLinkRequest(false);
                            }
                        })
                        .setPositiveButton("立即连麦", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mYfStreamerKit.responseLinkRequest(true);
                            }
                        })
                        .create();
                if (!mAlertDialog.isShowing()) {
                    mAlertDialog.show();
                }
            }
        });
    }

    private YfPlayerKit mYfPlayerKit;
    private String cachePath;
    boolean surfaceCreated = false;
    boolean needToReopenVideo = false;
    private boolean mLinking;

    private void openVideo(String path) {
        Log.d(TAG, "粉丝的播放地址是:" + path);
        if (path == null) {
            return;
        }
        cachePath = path;
        mYfPlayerKit.setVisibility(View.VISIBLE);
        mYfPlayerKit.enableBufferState(false);
        mYfPlayerKit.setDelayTimeMs(mLinkBufferMs, mLinkBufferMs);
        mYfPlayerKit.setHardwareDecoder(false);
        mYfPlayerKit.setAudioTrackStreamType(AudioManager.STREAM_VOICE_CALL);
        mYfPlayerKit.setOnNativeAudioDecodedListener(new YfCloudPlayer.OnNativeAudioDataDecoded() {
            @Override
            public void onAudioDataDecoded(YfCloudPlayer mp, byte[] data, int length, long pts) {
                if ((mEnableLink ) && yfEncoderKit != null)
                    yfEncoderKit.onRemoteAudioAvailable(data, length);//将音频数据传入编码器

            }
        });

        mYfPlayerKit.setOnNativeVideoDecodedListener(new YfCloudPlayer.OnNativeVideoDataDecoded() {
            @Override
            public void onVideoDataDecoded(YfCloudPlayer mp, byte[] data, int width, int height, long pts) {
                if (mEnableLink && yfEncoderKit != null)
                    yfEncoderKit.onRemoteFrameAvailable(data, width, height);//将视频数据传入编码器
            }
        });
        mYfPlayerKit.setOnPreparedListener(new YfCloudPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(YfCloudPlayer mp) {
                if (mEnableLink && yfEncoderKit != null) {
                    Log.d(TAG, "size:" + mp.getVideoWidth() + "," + mp.getVideoHeight());
                    yfEncoderKit.setAECMode(ENABLE_HARDWARE_AEC);
                    yfEncoderKit.prepareForLink();
                    if (mYfStreamerKit != null)
                        mYfStreamerKit.notifyLinkSuccessfully();//通知连麦用户切换音频
                    mLinking = true;
                }
            }
        });
        mYfPlayerKit.setOnErrorListener(new YfCloudPlayer.OnErrorListener() {
            @Override
            public boolean onError(YfCloudPlayer mp, int what, int extra) {
                if (yfEncoderKit != null)
                    yfEncoderKit.stopHandleLinkStream();//播放已经异常，通知编码器停止处理播放的数据
                if (surfaceCreated)//只有当surface存在的时候才能打开视频
                    openVideo(cachePath);//简单粗暴地重连
                else
                    needToReopenVideo = true;//设置标志位，在surface初始化后打开视频
                return false;
            }
        });
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
        mYfPlayerKit.enableUDP(ENABLE_PLAYER_UDP);
        mYfPlayerKit.setOnInfoListener(new YfCloudPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(YfCloudPlayer mp, int what, int extra) {
                if(what==YfPlayerKit.INFO_CODE_VIDEO_RENDERING_START){
//                    AudioManager audioManager=(AudioManager) PushStreamActivity.this.getSystemService(Context.AUDIO_SERVICE);
//                    audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
//                            AudioManager.AUDIOFOCUS_GAIN);
//
//                    // Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
//                    // required to be in this mode when playout and/or recording starts for
//                    // best possible VoIP performance.
//                    // TODO(henrika): we migh want to start with RINGTONE mode here instead.
//                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                }
                return false;
            }
        });
        mYfPlayerKit.setVideoPath(path);
        mYfPlayerKit.start();
    }


    protected void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PushStreamActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static final int CODE_FOR_WRITE_PERMISSION = 101;

    @TargetApi(Build.VERSION_CODES.M)
    private void checkSdcardPermission(String playUrl) {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_FOR_WRITE_PERMISSION);
        } else {
            openVideo(playUrl);
        }
    }
}
