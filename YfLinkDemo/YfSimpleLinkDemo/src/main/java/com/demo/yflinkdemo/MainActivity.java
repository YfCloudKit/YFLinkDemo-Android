package com.demo.yflinkdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.demo.yflinkdemo.adapter.MenuPagerAdapter;
import com.demo.yflinkdemo.fragment.FirstMenuFragment;
import com.demo.yflinkdemo.fragment.SecondMenuFragment;
import com.yunfan.auth.YfAuthentication;
import com.yunfan.encoder.widget.YfEncoderKit;
import com.yunfan.player.widget.YfPlayerKit;

import java.util.ArrayList;

import me.relex.circleindicator.CircleIndicator;

public class MainActivity extends AppCompatActivity {
    public static final String AccessKey = "65592eeddc3d646db903a7367d58792268804f09";
    public static final String Token = "9e7299afc12d793a23d913d88be6fa6383f5876e";
    private EditText mStreamPath0, mLinkPath0, mLinkDelay, mStreamPath1, mStreamPath2;
    public static String UDP_IP = "183.129.179.53/";
    //    public static String PUSH_HOST = "rtmp://yfstream.livestar.com/live/";
//    public static String PULL_HOST = "http://yfrtmp.livestar.com/live/";
//    public static String PUSH_HOST = "rtmp://183.129.179.53/yfrtmpup.yflive.net/live/";
//    public static String PULL_HOST = "http://183.129.179.53/yfflv.yflive.net/live/";
//    public static String LINKER_PUSH_HOST = "rtmp://183.129.179.53/yfrtmpup.yflive.net/live/";
//    public static String LINKER_PULL_HOST = "http://183.129.179.53/yfflv.yflive.net/live/";


    public static String PUSH_HOST = "rtmp://push-zk.yftest.yflive.net/live/";
    public static String PULL_HOST = "rtmp://rtmp-zk.yftest.yflive.net/live/";
    public static String LINKER_PUSH_HOST = "rtmp://push-zk.yftest.yflive.net/live/";
    public static String LINKER_PULL_HOST = "rtmp://rtmp-zk.yftest.yflive.net/live/";
    public static String LINKER_PULL_HOST_HTTP = "http://flv-zk.yftest.yflive.net/live/";

    public static int OUT_WIDTH = 144;
    public static int OUT_HEIGHT = 240;
    public static int DELTA_X = 80;//[0,368 - MainActivity.OUT_WIDTH];
    public static int DELTA_Y = 160;//[0,640 - MainActivity.OUT_HEIGHT]

//    public static String PUSH_HOST = "rtmp://publish.langlive.com/live/";
//    public static String PULL_HOST = "http://video.langlive.com/live/";
//    public static String LINKER_PUSH_HOST = "rtmp://publish.langlive.com/live/";
//    public static String LINKER_PULL_HOST = "http://video.langlive.com/live/";

    private TextView mTextViewPush1, mTextViewPush0;
    private Switch enablePlayerUdp, enableStreamerUdp, enableHardEncoder,enableHWAEC;
    private TextView mTextViewPush2;
    private TextView mTextViewPull0;
    private TextView mTextViewAudience0;
    private TextView mTextViewAudience1;
    private TextView mTextViewAudience2;
    private FirstMenuFragment mFirstMenuFragment;
    private SecondMenuFragment mSecondMenuFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//
        enablePlayerUdp = (Switch) findViewById(R.id.enablePlayerUdp);
        enableStreamerUdp = (Switch) findViewById(R.id.enableStreamerUdp);
        enableHardEncoder = (Switch) findViewById(R.id.hardEncoder);
        enableHWAEC = (Switch) findViewById(R.id.hardAEC);
        YfAuthentication.getInstance().authenticate(AccessKey,Token, null);//鉴权
        enablePlayerUdp.setOnCheckedChangeListener(mOnCheckedChangeListener);
        enableStreamerUdp.setOnCheckedChangeListener(mOnCheckedChangeListener);
        enableHardEncoder.setOnCheckedChangeListener(mOnCheckedChangeListener);
        enableHWAEC.setOnCheckedChangeListener(mOnCheckedChangeListener);


        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
        ArrayList<Fragment> fragmentArrayList = new ArrayList<>();
        mFirstMenuFragment = FirstMenuFragment.newInstance(
                enablePlayerUdp.isChecked(), enableStreamerUdp.isChecked(), enableHardEncoder.isChecked());
        mSecondMenuFragment = SecondMenuFragment.newInstance(
                enablePlayerUdp.isChecked(), enableStreamerUdp.isChecked(), enableHardEncoder.isChecked());

        fragmentArrayList.add(mFirstMenuFragment);
        fragmentArrayList.add(mSecondMenuFragment);
        viewPager.setAdapter(new MenuPagerAdapter(getSupportFragmentManager(), fragmentArrayList));
        indicator.setViewPager(viewPager);

        ((TextView)findViewById(R.id.version)).setText("v"+ BuildConfig.VERSION_NAME+"_"+ YfPlayerKit.getVersion()+"_"+YfEncoderKit.getSDKVersion());
    }

    private boolean mEnableHardEncoder;
    private boolean mEnableStreamUdp;
    private boolean mEnablePlayerUdp;
    private boolean mEnableHWAEC;
    CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.enablePlayerUdp:
                    mEnablePlayerUdp = isChecked;
                    break;
                case R.id.enableStreamerUdp:
                    mEnableStreamUdp = isChecked;
                    break;
                case R.id.hardEncoder:
                    mEnableHardEncoder = isChecked;
                    break;
                case R.id.hardAEC:
                    mEnableHWAEC = isChecked;
                    break;
            }
            mFirstMenuFragment.setPrams(mEnablePlayerUdp, mEnableStreamUdp, mEnableHardEncoder,mEnableHWAEC);
            mSecondMenuFragment.setPrams(mEnablePlayerUdp, mEnableStreamUdp, mEnableHardEncoder,mEnableHWAEC);
        }
    };

}
