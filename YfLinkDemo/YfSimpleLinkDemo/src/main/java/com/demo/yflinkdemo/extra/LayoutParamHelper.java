package com.demo.yflinkdemo.extra;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.demo.yflinkdemo.widget.DeviceUtil;
import com.demo.yflinkdemo.widget.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 37917 on 2017/11/27 0027.
 */

public class LayoutParamHelper {
    private final static String TAG = "Yf_LayoutParamHelper";

    boolean mScreenLandscape;
    int mSmallScreenWidth;
    int mSmallScreenHeight;
    int screenWidth, screenHeight;
    private List<YfGravity> mGravity;

    public LayoutParamHelper(Context context, float largePixel, float smallPixel) {
        mScreenLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        screenWidth = DeviceUtil.getScreenWidth(context);
        screenHeight = DeviceUtil.getScreenHeight(context);
        if (mScreenLandscape) {
            mSmallScreenHeight = (int) (smallPixel / largePixel * screenHeight);
            mSmallScreenWidth = mSmallScreenHeight * 16 / 9;
        } else {
            mSmallScreenWidth = (int) (smallPixel / largePixel * screenWidth);
            mSmallScreenHeight = mSmallScreenWidth / 9 * 16;
        }
        Log.d(TAG,"small screen size:"+mSmallScreenWidth+","+mSmallScreenHeight);
        mGravity = new ArrayList<>();
        if (mScreenLandscape)
            mGravity.add(new YfGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, false));
        mGravity.add(new YfGravity(Gravity.RIGHT | Gravity.BOTTOM, false));
        mGravity.add(new YfGravity(Gravity.LEFT | Gravity.BOTTOM, false));
        mGravity.add(new YfGravity(Gravity.LEFT | Gravity.TOP, false));
        mGravity.add(new YfGravity(Gravity.RIGHT | Gravity.TOP, false));
        mGravity.add(new YfGravity(Gravity.CENTER, false));
    }

    class YfGravity {
        int gravity;
        boolean inUsed;

        public YfGravity(int gravity, boolean inUsed) {
            this.gravity = gravity;
            this.inUsed = inUsed;
        }
    }

    public FrameLayout.LayoutParams getLayoutParams(boolean recorder, boolean fullScreen) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (mScreenLandscape) {//横屏
            if (fullScreen) {//全屏（大小窗口模式），推流窗口恒为中间小窗口，宽高为width，height
                if (recorder) {//推流端为小窗口

                    lp.width = mSmallScreenWidth;
                    lp.height = mSmallScreenHeight;
                    lp.gravity = getUsableGravity();
                } else {//播放端为大窗口
                    return lp;
                }
            } else {//双半屏模式，推流窗口和播放窗口各占据一半
                lp.gravity = recorder ? Gravity.LEFT|Gravity.CENTER_VERTICAL : Gravity.RIGHT|Gravity.CENTER_VERTICAL;//推流在左，播放在右
                lp.width = screenWidth / 2;
                lp.height = lp.width * 9 / 16;
                Log.d(TAG,"double screen mode:"+lp.width+","+lp.height);
            }
        } else {//竖屏
            if (fullScreen) {//大窗口
                return lp;
            } else {//小窗口
                lp.width = mSmallScreenWidth;
                lp.height = mSmallScreenHeight;
                lp.gravity = getUsableGravity();
            }
        }
        return lp;
    }

    public int getUsableGravity() {
        for (int i = 0; i < mGravity.size(); i++) {
            if (!mGravity.get(i).inUsed) {
                mGravity.get(i).inUsed = true;
                return mGravity.get(i).gravity;
            }
        }
        return Gravity.RIGHT | Gravity.BOTTOM;
    }

    public void onLayoutReleased(int gravity){
        for (int i = 0; i < mGravity.size(); i++) {
            if (mGravity.get(i).gravity == gravity) {
                mGravity.get(i).inUsed = false;
                break;
            }
        }
    }

}
