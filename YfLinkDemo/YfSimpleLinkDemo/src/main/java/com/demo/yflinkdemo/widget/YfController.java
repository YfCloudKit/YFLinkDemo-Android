package com.demo.yflinkdemo.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.demo.yflinkdemo.R;
import com.demo.yflinkdemo.extra.Utils;


/**
 * Created by xjx on 2016/5/24.
 */
public class YfController extends RelativeLayout implements View.OnClickListener {
    private static final String TAG = "YfController";
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    public TextView mCurrentTime, mEndTime;
    public ImageView mPauseButton, mMenu, mClose;
    public SeekBar mProgress;

    private boolean mShowing;
    private YfControl mPlayer;
    private int sDefaultTimeout = 3000;
    private boolean mDragging;

    private long mStartTime;

    public YfController(Context context) {
        super(context);
    }

    public YfController(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.widget_yfcontroller, this, true);
        mCurrentTime = (TextView) findViewById(R.id.timestamp_past);
        mEndTime = (TextView) findViewById(R.id.timestamp_duaration);
        mProgress = (SeekBar) findViewById(R.id.seekBar);
        mProgress.setOnSeekBarChangeListener(mSeekListener);
        mProgress.setMax(1000);
        mPauseButton = (ImageView) findViewById(R.id.play_pause);
        mPauseButton.setOnClickListener(this);
        mMenu = (ImageView) findViewById(R.id.more_info);
        mMenu.setOnClickListener(this);
        mClose = (ImageView) findViewById(R.id.close);
        mClose.setOnClickListener(this);

    }


    public void setPlayer(YfControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_pause:
                doPauseResume();
                show(sDefaultTimeout);
                break;
            case R.id.close:
                mPlayer.close();
                break;
            case R.id.more_info:
                mPlayer.showMoreMenu(v);
                break;
        }
    }

    public void setDefaultTimeOutMs(int timeOut) {
        sDefaultTimeout = timeOut;
    }

    public boolean isShowing() {
        return mShowing;
    }

    public void show(int timeout) {
        if (!mShowing) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            setVisibility(VISIBLE);
            mShowing = true;
        }
        updatePausePlay();

        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            Message msg = mHandler.obtainMessage(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            mDragging = true;

            mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            long newposition = 0;
            long duration = mPlayer.getDuration();
            newposition = (duration * progress) / 1000L;
            mPlayer.seekTo((int) newposition);
            if (mCurrentTime != null)
                mCurrentTime.setText(Utils.stringForTime((int) newposition));
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            updatePausePlay();
            show(sDefaultTimeout);

            mHandler.sendEmptyMessage(SHOW_PROGRESS);

        }
    };

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            mPauseButton.setSelected(false);
        } else {
            mPlayer.start();
            mPauseButton.setSelected(true);
        }
    }

    public void hide() {
        if (mShowing) {
            try {
                mHandler.removeMessages(SHOW_PROGRESS);
                setVisibility(GONE);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            mShowing = false;
        }
    }

    private void updatePausePlay() {
        mPauseButton.setSelected(mPlayer.isPlaying());
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    pos = setProgress();
                    if (!mDragging && mShowing && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    };

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = 0;
        int duration = 0;
        if (mProgress != null) {
            position = mPlayer.getCurrentPosition();
            duration = mPlayer.getDuration();
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(Utils.stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(Utils.stringForTime(position));

        return position;
    }


    public interface YfControl {
        void start();

        void pause();

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos);

        boolean isPlaying();

        int getBufferPercentage();

        void close();

        void showMoreMenu(View v);

    }
}
