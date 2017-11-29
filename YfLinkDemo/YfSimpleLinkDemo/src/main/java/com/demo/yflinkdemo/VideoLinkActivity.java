package com.demo.yflinkdemo;

import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.yflinkdemo.extra.Const;
import com.demo.yflinkdemo.extra.LayoutParamHelper;
import com.demo.yflinkdemo.extra.MultiPlayerHelper;
import com.demo.yflinkdemo.extra.Utils;
import com.demo.yflinkdemo.widget.Log;
import com.demo.yflinkdemo.widget.LogRecorder;
import com.demo.yflinkdemo.widget.ScaleGLSurfaceView;
import com.yunfan.encoder.filter.YfBlurBeautyFilter;
import com.yunfan.encoder.widget.RecordMonitor;
import com.yunfan.encoder.widget.YfEncoderKit;
import com.yunfan.player.widget.MediaInfo;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.player.widget.YfPlayerKit;
import com.yunfan.player.widget.YfTrackInfo;
import com.yunfan.yflinkkit.YfLinkerKit;
import com.yunfan.yflinkkit.YfStreamerKit;
import com.yunfan.yflinkkit.bean.LinkerMember;
import com.yunfan.yflinkkit.callback.QueryLinkCallback;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 新版连麦界面，主播、连麦粉丝和观众的界面均在此Activity
 * <p>
 * 连麦主要流程：
 * ********
 * 1.主播端推流，开启连麦功能
 * 2.连麦粉丝申请连麦（连麦粉丝可提前开启回声消除{@link YfEncoderKit#enableAEC(boolean enable)}）
 * 3.主播同意连麦
 * 4.连麦粉丝推流，推流成功后发送推流成功通知,在播放器音频回调{@link YfCloudPlayer.OnNativeAudioDataDecoded}
 * 中处理远端音频{@link YfEncoderKit#onRemoteAudioAvailable(byte[] data, int length)}
 * 5.主播收到连麦粉丝推流成功的通知并开始播放,此时主播开启回声消除{@link YfEncoderKit#enableAEC(boolean enable)},
 * 在播放器音频回调{@link YfCloudPlayer.OnNativeAudioDataDecoded}中处理远端音频
 * {@link YfEncoderKit#onRemoteAudioAvailable(byte[] data, int length)}
 * 6.观众退出连麦后，主播可关闭回声消除
 * *******
 * <p>
 * 观众端
 * 由于目前调度功能不完善，暂时使用每500ms轮训查询房间信息
 */
public class VideoLinkActivity extends BaseLinkActivity {
    private static final String TAG = "yf_VideoLinkActivity";
    private String mRoomId;
    private int mLinkType;
    private String mHostId;
    private String mViceHostId;
    private int mRole;
    private boolean mUDPPush;
    private String mHostPushUrl;
    private String mViceHostPushUrl;
    private String mHostPlayUrl;
    private String mViceHostPlayUrl;
    private ScaleGLSurfaceView mGLSurfaceView;
    private View mRootView;
    //    private YfPlayerKit mYfPlayerKit;
    private YfEncoderKit yfEncoderKit;
    private YfBlurBeautyFilter mBeautyFilter;
    private boolean onServerConnected;
    protected LogRecorder logRecoder = new LogRecorder();
    private NetworkConnectChangedReceiver receiver;


    //==============audience==================
    private SurfaceView[] surfaceView = new SurfaceView[3];
    private YfCloudPlayer[] yfPlayer = new YfCloudPlayer[3];
    private String[] path = new String[3];

    //==============const===============
    private final int BEAUTY_INDEX = 1;
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 368;
    private static final int VIDEO_WIDTH_LINKER = 160;
    private static final int VIDEO_HEIGHT_LINKER = 96;
    private static final int VIDEO_FRAME_RATE = 15;
    private int OUTPUT_WIDTH = 160, OUTPUT_HEIGHT = 96;
    private int PREVIEW_WIDTH = 1280;
    private int PREVIEW_HEIGHT = 720;
    private int PREVIEW_WIDTH_LINKER = 1280;
    private int PREVIEW_HEIGHT_LINKER = 720;
    private int VIDEO_BITRATE = 600;
    private int VIDEO_BITRATE_LINKER = 200;
    private boolean mEnableLink = true;
    private String cachePath;
    private boolean needToReopenVideo;
    private TextView mTvInfo;
    private TextView mTvDetail;
    private TextView mAudioLinkHint;
    private Button mBtnStartPush;
    private Button mBtnStartPlay;
    private boolean enableAEC;
    private boolean mPlayerPrepared;
    private boolean mInLinking;

    private boolean mLandscape;
    private int mTVLayoutMode = Const.TV_LAYOUT_HALF_SCREEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        YfPlayerKit.enableRotation(true);
        setContentView(R.layout.activity_video_link);
        getData();
        initView();
    }

    private void getData() {
        mLinkType = getIntent().getIntExtra(Const.KEY_LINK_TYPE, Const.VIDEO);
        mHostId = getIntent().getStringExtra(Const.KEY_HOST_ID);
        mViceHostId = getIntent().getStringExtra(Const.KEY_VICE_HOST_ID);
        mRole = getIntent().getIntExtra(Const.KEY_ROLE, Const.ROLE_HOST);
        mUDPPush = getIntent().getBooleanExtra(Const.KEY_UDP_PUSH, Const.UDP_PUSH);
        Log.d(TAG, "mRole: " + mRole + " mHostId: " + mHostId + " mViceHostId:" + mViceHostId);
        mHostPushUrl = Const.PUSH_URL_BASE + mHostId;
        mViceHostPushUrl = Const.PUSH_URL_BASE + mViceHostId;
        mHostPlayUrl = Const.PLAY_URL_BASE + mHostId;
        mViceHostPlayUrl = Const.PLAY_URL_BASE + mViceHostId;
    }


    private void initView() {
        mLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        LayoutParamHelper layoutParamHelper = new LayoutParamHelper(this, VIDEO_HEIGHT, OUTPUT_HEIGHT);
        mMultiPlayerHelper = new MultiPlayerHelper(layoutParamHelper);
        if (mRole == Const.ROLE_AUDIENCE) {//观众布局
            ViewStub viewStub = (ViewStub) findViewById(R.id.stub_audience);
            mRootView = viewStub.inflate();
            addPlayerAndOpenVideo(true, mHostPlayUrl);
            mYfLinkerKit = new YfLinkerKit(this);
            timer.schedule(task, 500, 500); // 1s后执行task,经过1s再次执行
        } else {
            if (mRole == Const.ROLE_HOST) {//主播布局
                ViewStub viewStub = (ViewStub) findViewById(R.id.stub_host);
                mRootView = viewStub.inflate();
                if (mLinkType == Const.VIDEO) {
                    mGLSurfaceView = new ScaleGLSurfaceView(this);
                    ((FrameLayout) mRootView).addView(mGLSurfaceView, layoutParamHelper.getLayoutParams(true, mLandscape ? mTVLayoutMode == Const.TV_LAYOUT_FULL_SCREEN : true));
//                    setSurfaceSize(false, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                }
                initLinkListener();
            } else if (mRole == Const.ROLE_VICE_HOST) {//副播布局
                ViewStub viewStub = (ViewStub) findViewById(R.id.stub_vice_host);
                mRootView = viewStub.inflate();
//                mGLSurfaceView = (ScaleGLSurfaceView) mRootView.findViewById(R.id.surface_view);
                openVideo(mHostPlayUrl, addPlayer(mLandscape ? mTVLayoutMode == Const.TV_LAYOUT_FULL_SCREEN : true));
                if (mLinkType == Const.VIDEO) {
                    mGLSurfaceView = new ScaleGLSurfaceView(this);
                    ((FrameLayout) mRootView).addView(mGLSurfaceView, layoutParamHelper.getLayoutParams(true, mLandscape ? mTVLayoutMode == Const.TV_LAYOUT_FULL_SCREEN : false));
                }
//                mYfPlayerKit = (YfPlayerKit) mRootView.findViewById(R.id.player_kit);
//                mYfPlayerKit.setVideoLayout(YfPlayerKit.VIDEO_LAYOUT_FILL_PARENT);
                initLinker();
            }
            Button btnSwitch = (Button) findViewById(R.id.btn_switch);
            btnSwitch.setVisibility(View.VISIBLE);
            btnSwitch.setOnClickListener(mOnClickListener);
            initRecorder(mGLSurfaceView);
            if (mRole == Const.ROLE_HOST) {
                startRecorder();
            }
//            openVideo(mRole == Const.ROLE_HOST ? mViceHostPlayUrl : mHostPlayUrl, 0);
        }
        Button btnExit = (Button) findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(mOnClickListener);
        mTvInfo = (TextView) findViewById(R.id.tv_info);
        mTvDetail = (TextView) findViewById(R.id.tv_detail);
        mAudioLinkHint = (TextView) findViewById(R.id.audio_link_hint);
//        mBtnStartPush = (Button) findViewById(R.id.btn_start_push);
//        mBtnStartPlay = (Button) findViewById(R.id.btn_start_play);
//        mBtnStartPush.setOnClickListener(mOnClickListener);
//        mBtnStartPlay.setOnClickListener(mOnClickListener);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("房间： ").append(mHostId).append(SPACE)
                .append("角色： ").append(mRole == Const.ROLE_HOST ?
                "主播" : (mRole == Const.ROLE_VICE_HOST ? "副播" : "观众"));
        mTvInfo.setText(stringBuilder.toString());
    }

    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            queryStreamInfo();
        }
    };

    private YfStreamerKit mYfStreamerKit;

    private void initLinkListener() {
        mYfStreamerKit = new YfStreamerKit(mHostPlayUrl, this);
        mYfStreamerKit.setStreamerCallBack(new YfStreamerKit.StreamerCallBack() {

            @Override
            public void onLinkerRequest() {
                Log.d(TAG, "onLinkerRequest");
                mYfStreamerKit.responseLinkRequest(true);
            }

            @Override
            public void onLinkerQuit() {
//                Log.d(TAG, "onLinkerQuit");
//                Toast.makeText(VideoLinkActivity.this, "粉丝已退出连麦", Toast.LENGTH_SHORT).show();
                removePlayer(0);

            }

            @Override
            public void onLinkerConnected(final String fansPlayUrl) {
                Log.d(TAG, "onLinkerConnected fansPlayUrl：" + fansPlayUrl);
                //粉丝推流成功开始播放
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (yfEncoderKit == null) {
                            return;
                        }
                        addPlayerAndOpenVideo(false, fansPlayUrl);
                    }
                });
            }

            @Override
            public void onEnableLinkResult(boolean success) {
                mEnableLink = success;
                Log.d(TAG, String.format("开启连麦 %s", success ? "成功" : "失败"));
            }

        });
        mYfStreamerKit.enableLinkRequest();
    }

    private YfLinkerKit mYfLinkerKit;

    private void initLinker() {
        mYfLinkerKit = new YfLinkerKit(this);
        mYfLinkerKit.setLinkerCallBack(new YfLinkerKit.LinkerCallBack() {
            @Override
            public void onStreamerResponse(boolean accept) {
                if (accept) {
                    mInLinking = true;
                    //主播同意连麦请求
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //开始推流,推流成功后发送通知onServerConnect
                            Log.w(TAG, "get msg to stream");
                            startRecorder();
                        }
                    });
                }
            }

            @Override
            public void onStreamerQuit() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mRole == Const.ROLE_AUDIENCE)
                            timer.cancel();
                        mYfLinkerKit.exit();//退出连麦服务器
                        mYfLinkerKit = null;
                        finish();
                    }
                });
            }


            @Override
            public void onError(int errorCode, String errorMsg) {
            }

            @Override
            public void onStreamLinkSuccess() {
                Log.d(TAG, "onStreamLinkSuccess");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
    }

    private void queryStreamInfo() {
        if (mYfLinkerKit != null)
            mYfLinkerKit.getStreamInfo(mHostPlayUrl
                    , new QueryLinkCallback() {
                        @Override
                        public synchronized void onSuccess(boolean isOpen, List<LinkerMember> memberList) {
                            Log.d(TAG, String.format("queryLink onSuccess isOpen: %s, srtmp: %s, roomID: %s, ip: %s,members:%s", isOpen, "", "", "", memberList == null ? 0 : memberList.size()));
                            if (isOpen) {//主播已开启连麦
                                if (mRole == Const.ROLE_VICE_HOST) {
                                    if (mInLinking) {
                                        startRecorder();//断网重推
                                    } else {
                                        mYfLinkerKit.requestToLink();
                                    }

                                } else if (mRole == Const.ROLE_AUDIENCE) {
                                    if (memberList != null) {
                                        for (int i = 0; i < mMultiPlayerHelper.getPlayerSize(); i++) {
                                            if (mMultiPlayerHelper.getDataSource(i).equals(mHostPlayUrl)) {
                                                continue;
                                            }
                                            boolean remove = true;
                                            for (int m = 0; m < memberList.size(); m++) {
                                                if (memberList.get(m).getRtmp().equals(mMultiPlayerHelper.getDataSource(i))) {
                                                    remove = false;
                                                }
                                            }
                                            if (remove) {
                                                Log.d(TAG, "remove player:" + mMultiPlayerHelper.getDataSource(i) + "," + mMultiPlayerHelper.getPlayerSize());
                                                removePlayer(i);
//                                                mMultiPlayerHelper.removeAndReleasePlayer((FrameLayout) mRootView, i);//移除已经退出连麦的流
                                            }
                                        }
                                        for (int i = 0; i < memberList.size(); i++) {
                                            Log.d(TAG, "add player:" + memberList.get(i).getRtmp() + "," + mMultiPlayerHelper.getPlayerSize() + "," + memberList.size() + "," + Thread.currentThread());
                                            addPlayerAndOpenVideo(false, memberList.get(i).getRtmp());//添加新连麦的流
                                        }
                                    } else {
                                        for (int i = 1; i < mMultiPlayerHelper.getPlayerSize(); i++) {
                                            removePlayer(i);
                                        }
                                    }
                                }
                            } else if (mRole == Const.ROLE_AUDIENCE) {
                                mYfLinkerKit.stopThread();
                                mYfLinkerKit = null;
                                finish();
                            }
                        }

                        @Override
                        public synchronized void onFailed(int code) {
                            Log.d(TAG, "queryLink onFailed code: " + code);
                        }

                        @Override
                        public synchronized void onError(Exception e) {
                            Log.d(TAG, "queryLink onError: " + e.getMessage());
                        }
                    });
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_switch:
                    yfEncoderKit.switchCamera();
                    break;
                case R.id.btn_exit:
                    showStopRecordDialog();
                    break;
//                case R.id.btn_start_push:
//                    startRecorder();
//                    break;
//                case R.id.btn_start_play:
//
//                    openVideo(mRole == Const.ROLE_HOST ? mViceHostPlayUrl : mHostPlayUrl, 0);
//                    break;
            }
        }
    };

    private MultiPlayerHelper mMultiPlayerHelper;

    private void addPlayerAndOpenVideo(final boolean fullScreen, final String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mMultiPlayerHelper.containDataSource(path)) {
                    openVideo(path, addPlayer(fullScreen));
                } else if (mRole == Const.ROLE_HOST) {
                    openVideo(path, 0);
                }
            }
        });
    }

    private int mAddedPlayerIndex = -1;//最后一个添加的播放器的编号

    private int addPlayer(boolean fullScreen) {
        Log.d(TAG, "add player:" + ++mAddedPlayerIndex);
        if (mAddedPlayerIndex == 0) {
            return mMultiPlayerHelper.buildAPlayerInOnePlace(this, (FrameLayout) mRootView,
                    fullScreen, !(mRole == Const.ROLE_AUDIENCE), mOnPreparedListener, mOnErrorListener,
                    mOnNativeAudioDataDecoded, 0);
        } else {
            return mMultiPlayerHelper.buildAPlayerInOnePlace(this, (FrameLayout) mRootView,
                    fullScreen, !(mRole == Const.ROLE_AUDIENCE), null, null,
                    null, mAddedPlayerIndex);
        }


//        mYfPlayerKit.setSurfaceCallBack(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                Log.d(TAG, "on player surfaceCreated");
//                surfaceCreated = true;
//                mYfPlayerKit.setVolume(1, 1);
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                Log.d(TAG, "on player surfaceDestroyed");
//                surfaceCreated = false;
//                mYfPlayerKit.setVolume(0, 0);
//            }
//        });
    }

    private void removePlayer(final int index) {
        Log.d(TAG, "removePlayer: " + --mAddedPlayerIndex);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMultiPlayerHelper.removeAndReleasePlayer((FrameLayout) mRootView, index);
                if (mMultiPlayerHelper.getPlayerSize() == 0 && mRole == Const.ROLE_HOST && yfEncoderKit != null) {
                    yfEncoderKit.enableAEC(false);
                }
                mAudioLinkHint.setVisibility(View.GONE);
            }
        });

    }

    YfCloudPlayer.OnPreparedListener mOnPreparedListener = new YfCloudPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(YfCloudPlayer mp) {
            Log.d(TAG, "onPrepared: ");
            mPlayerPrepared = true;
            //主播的流确实播放起来之后再查询并申请连麦
            if (mRole == Const.ROLE_HOST && yfEncoderKit != null) {
                yfEncoderKit.enableAEC(true);
            }
            MediaInfo mediaInfo = mp.getMediaInfo();

            YfTrackInfo trackInfos[] = mediaInfo.getTrackInfo();
            boolean hasVideo = false;
            for (YfTrackInfo trackInfo : trackInfos) {
                if (trackInfo.getTrackType() == YfTrackInfo.MEDIA_TRACK_TYPE_VIDEO)
                    hasVideo = true;
            }
            android.util.Log.d(TAG, "hasVideo: " + hasVideo);
            if (!hasVideo) {
                mAudioLinkHint.setVisibility(View.VISIBLE);
                if (mRole != Const.ROLE_HOST) {
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    mAudioLinkHint.setLayoutParams(lp);
                }
            }
            if (mRole == Const.ROLE_VICE_HOST)
                queryStreamInfo();
        }
    };
    YfCloudPlayer.OnErrorListener mOnErrorListener = new YfCloudPlayer.OnErrorListener() {
        @Override
        public boolean onError(YfCloudPlayer mp, int what, int extra) {
            if (yfEncoderKit != null)
                yfEncoderKit.enableAEC(false);
            mPlayerPrepared = false;
            Log.d(TAG, "onError: ");
            if (Utils.isForeground(VideoLinkActivity.this)) {
                if (mRole == Const.ROLE_HOST) {
                    openVideo(mViceHostPlayUrl, 0);//简单粗暴地重连
                } else {
                    openVideo(mHostPlayUrl, 0);
                }
            } else
                needToReopenVideo = true;//设置标志位，在surface初始化后打开视频
            return false;
        }
    };
    YfCloudPlayer.OnNativeAudioDataDecoded mOnNativeAudioDataDecoded = new YfCloudPlayer.OnNativeAudioDataDecoded() {
        @Override
        public void onAudioDataDecoded(YfCloudPlayer mp, byte[] data, int length, long index) {
            if ((mEnableLink) && yfEncoderKit != null && data != null)
                yfEncoderKit.onRemoteAudioAvailable(data, length);//将音频数据传入编码器，这里用不需要的pts将播放器编号回传出来了

        }
    };

    /**
     * @param landscape 是否为横屏模式，预览宽度，预览高度
     */
    private void setSurfaceSize(boolean landscape, int width, int height) {
        ViewGroup.LayoutParams lp = mGLSurfaceView.getLayoutParams();
        int realScreenWidth = Utils.getScreenWidth(this);
        Log.d(TAG, "realScreenWidth:" + realScreenWidth + "," + landscape);
        int surfaceWidth, surfaceHeight;
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
        if (mLinkType == Const.VIDEO) {
            if (mRole == Const.ROLE_HOST) {
                yfEncoderKit = new YfEncoderKit(this, Const.CACHE_DIRS, mLandscape ? PREVIEW_WIDTH_LINKER : PREVIEW_WIDTH,
                        mLandscape ? PREVIEW_HEIGHT_LINKER : PREVIEW_HEIGHT, VIDEO_WIDTH, VIDEO_HEIGHT, true, VIDEO_FRAME_RATE);
            } else {
                yfEncoderKit = new YfEncoderKit(this, Const.CACHE_DIRS, PREVIEW_WIDTH_LINKER,
                        PREVIEW_HEIGHT_LINKER, VIDEO_WIDTH_LINKER, VIDEO_HEIGHT_LINKER, false, VIDEO_FRAME_RATE);
            }
            yfEncoderKit.setContinuousFocus()//设置连续自动对焦
                    .setLandscape(mLandscape)//设置是否横屏模式（默认竖屏）
                    .enableFlipFrontCamera(false)//设置前置摄像头是否镜像处理，默认为true
                    .setRecordMonitor(mMonitor)//设置回调
                    .setDropVideoFrameOnly(true)
                    .setDefaultCamera(true)//设置默认打开前置摄像头---不设置默认打开后置摄像头
                    .openCamera(s);//设置预览窗口
            if (!mLandscape) {
                mBeautyFilter = new YfBlurBeautyFilter(this);
                mBeautyFilter.setIndex(BEAUTY_INDEX);
                yfEncoderKit.addFilter(mBeautyFilter);//默认打开滤镜
            }
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            yfEncoderKit = new YfEncoderKit(this, Const.CACHE_DIRS);
            yfEncoderKit.setRecordMonitor(mMonitor);
        }
        if (mRole != Const.ROLE_HOST) {
            yfEncoderKit.enableAEC(true);
        }

    }

    private boolean mForceStop;
    private RecordMonitor mMonitor = new RecordMonitor() {
        @Override
        public void onServerConnected() {
            onServerConnected = true;
            if (yfEncoderKit != null) {
                Toast.makeText(VideoLinkActivity.this, "推流成功，编码方式:" +
                        (yfEncoderKit.getEncodeMode() ? "硬编" : "软编"), Toast.LENGTH_SHORT).show();
            }
            if (mRole == Const.ROLE_VICE_HOST) {
                if (mYfLinkerKit != null) {
                    mYfLinkerKit.notifyLinkToStreamer(mViceHostPlayUrl);
//                    yfEncoderKit.enableAEC(true);
                }
            }
        }

        @Override
        public void onError(int mode, int err, String msg) {
            Log.i(TAG, "####### error: " + err + " " + msg);
            Toast.makeText(VideoLinkActivity.this, "err: no=" + err + " msg=" + msg,
                    Toast.LENGTH_SHORT).show();
            onServerConnected = false;
            if (!mForceStop) {
                startRecorder();//重新推流
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
//            Log.d(TAG, "onInfo: " + what);
            switch (what) {
                case YfEncoderKit.INFO_IP:
                    Log.d(TAG, "实际推流的IP地址:" + intToIp((int) arg1));
                    break;
                case YfEncoderKit.INFO_DROP_FRAMES:
                    Log.d(TAG, "frames had been dropped");
                    break;
                case YfEncoderKit.INFO_PUSH_SPEED:
                    mCurrentSpeed = (int) arg1;
                    break;
                case YfEncoderKit.INFO_FRAME:
                    mCurrentFPS = (int) arg1;
                    mAvgCostTimeMS = (int) arg2;
                    break;
                case YfEncoderKit.INFO_PREVIEW_SIZE_CHANGED:
                    Log.d(TAG, "on preview size changed:" + arg1 + "," + arg2);
                    Log.d(TAG, "on preview size changed:" + (float) arg1 / arg1 + "," + (float) PREVIEW_WIDTH / PREVIEW_HEIGHT);
                    break;
                case YfEncoderKit.INFO_BITRATE_CHANGED:
                    android.util.Log.d(TAG, "INFO_BITRATE_CHANGED: " + arg1);
                    mCurrentBitrate = (int) arg1;
                    break;
                case YfEncoderKit.INFO_CURRENT_BUFFER:
                    mCurrentBufferMs = (int) arg1;
                    break;
            }
        }
    };

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


    protected void startRecorder() {
        if (yfEncoderKit == null || yfEncoderKit.isRecording()) {//不允许推流过程中进行参数设置
            return;
        }
        Log.d(TAG, "开始录制");
        //设置编码参数：直播/录制、是否硬编、码率、宽、高
        mCurrentBitrate = mRole == Const.ROLE_HOST ? VIDEO_BITRATE : VIDEO_BITRATE_LINKER;
        yfEncoderKit.changeMode(YfEncoderKit.MODE_LIVE, mRole == Const.ROLE_HOST ? VIDEO_BITRATE : VIDEO_BITRATE_LINKER);
        yfEncoderKit.setBufferSizeBySec(1);//最多缓存1秒的数据，超过1秒则丢帧
        yfEncoderKit.setLiveUrl(mRole == Const.ROLE_HOST ? mHostPushUrl : mViceHostPushUrl);
        yfEncoderKit.enableUDP(mUDPPush);
        yfEncoderKit.enableHttpDNS(true);
        if (mLinkType == Const.VIDEO && mRole == Const.ROLE_HOST)
            yfEncoderKit.setAdjustQualityAuto(true, 300);
        yfEncoderKit.startRecord();
//        if (dataShowing) {
        infoShower.removeCallbacks(updateDisplay);
        infoShower.postDelayed(updateDisplay, 500);
//        }
        mForceStop = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "on resume!");

        if (mRole != Const.ROLE_AUDIENCE) {
            if (yfEncoderKit != null) {
                yfEncoderKit.onResume();
            }
        }
        mMultiPlayerHelper.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (yfEncoderKit != null)
            yfEncoderKit.onPause();
        mMultiPlayerHelper.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (yfEncoderKit != null)
            yfEncoderKit.onStop(true);
        mMultiPlayerHelper.onStop();
    }

    @Override
    protected void exitLink() {
        //副播主动退出需要发送类型为4带stop的消息，如果是主播主动断开只需要关闭socket
        super.exitLink();
        if (mYfLinkerKit != null) mYfLinkerKit.stopLink();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        if (mRole == Const.ROLE_HOST && mYfStreamerKit != null) {
            mYfStreamerKit.stopLink();
            mYfStreamerKit.exit(false);//退出但不解散房间，防止连麦观众没收到退出的消息
            mYfStreamerKit.setStreamerCallBack(null);
        }
        if (mYfLinkerKit != null) {
            if (mRole == Const.ROLE_VICE_HOST) {
                mYfLinkerKit.exit();
            }
            if (mRole == Const.ROLE_AUDIENCE) {
                mYfLinkerKit.stopThread();
            }
        }
        destroyRecorder();
        mMultiPlayerHelper.onDestroy();
    }

    private void openVideo(String path, int index) {
        Log.d(TAG, "openVideo:" + path + " index:" + index);
        if (path == null || index < 0) {
            return;
        }
        cachePath = path;
        mMultiPlayerHelper.setVideoPath(path, index);
        mMultiPlayerHelper.start(index);
    }


    private void destroyRecorder() {
        Log.d(TAG, "销毁编码器");
        if (yfEncoderKit != null) {
            if (yfEncoderKit.isRecording())
                stopRecorder();
            yfEncoderKit.release();
            yfEncoderKit = null;
        }
    }

    protected void stopRecorder() {
        infoShower.removeCallbacks(updateDisplay);
        if (yfEncoderKit != null) yfEncoderKit.stopRecord();
    }


    private Handler infoShower = new Handler();
    private int mCurrentBitrate, mCurrentBufferMs, mCurrentSpeed, mCurrentFPS, mAvgCostTimeMS;
    private final String LINE = "\n";
    private final Runnable updateDisplay = new Runnable() {
        @Override
        public void run() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("buffer-ms:").append(mCurrentBufferMs).append(LINE)
                    .append("bitrate:").append(mCurrentBitrate).append(LINE)
                    .append("speed:").append(mCurrentSpeed).append(LINE)
                    .append("fps:").append(mCurrentFPS).append(LINE)
                    .append("cost:").append(mAvgCostTimeMS).append(LINE);
            mTvDetail.setText(stringBuilder);

            infoShower.removeCallbacks(this);
            infoShower.postDelayed(this, 500);
        }
    };


}
