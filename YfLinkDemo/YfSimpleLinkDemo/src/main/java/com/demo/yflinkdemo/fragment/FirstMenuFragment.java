package com.demo.yflinkdemo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.yflinkdemo.AudienceActivity;
import com.demo.yflinkdemo.PlayStreamActivity;
import com.demo.yflinkdemo.PushStreamActivity;
import com.demo.yflinkdemo.R;
import com.yunfan.auth.YfAuthentication;

import static com.demo.yflinkdemo.MainActivity.AccessKey;
import static com.demo.yflinkdemo.MainActivity.Token;


public class FirstMenuFragment extends Fragment {
    private static final String TAG = FirstMenuFragment.class.getSimpleName();
    private static final String ARG_PARAM1 = "enablePullUdp";
    private static final String ARG_PARAM2 = "enablePushUdp";
    private static final String ARG_PARAM3 = "hardEncoder";
    private EditText mStreamPath0;
    private EditText mLinkPath0;
    private EditText mLinkDelay;
    private TextView mTextViewPush0;
    private TextView mTextViewPull0;
    private TextView mTextViewAudience0;
    private boolean mEnablePullUdp;
    private boolean mEnablePushUdp;
    private boolean mHardwareEncoder;
    private boolean mHardAEC;

    public FirstMenuFragment() {
        // Required empty public constructor
    }

    public static FirstMenuFragment newInstance(boolean enablePullUdp, boolean enablePushUdp,
                                                boolean isHardware) {
        FirstMenuFragment fragment = new FirstMenuFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, enablePushUdp);
        args.putBoolean(ARG_PARAM2, enablePullUdp);
        args.putBoolean(ARG_PARAM3, isHardware);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mEnablePullUdp = getArguments().getBoolean(ARG_PARAM1);
            mEnablePushUdp = getArguments().getBoolean(ARG_PARAM2);
            mHardwareEncoder = getArguments().getBoolean(ARG_PARAM3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStreamPath0 = (EditText) view.findViewById(R.id.stream_url_0);//主播与粉丝连麦，主播0的流名
        mStreamPath0.setText("test100");

        mLinkPath0 = (EditText) view.findViewById(R.id.link_url_0);//主播与粉丝连麦，连麦粉丝0的流名
        mLinkPath0.setText("test200");

        mLinkDelay = (EditText) view.findViewById(R.id.link_buffer);
        mLinkDelay.setText("400");
        mTextViewPush0 = (TextView) view.findViewById(R.id.push0);
        mTextViewPull0 = (TextView) view.findViewById(R.id.pull0);
        mTextViewAudience0 = (TextView) view.findViewById(R.id.audience0);//普通观众端
        mTextViewPush0.setOnClickListener(mOnClickListener);
        mTextViewPull0.setOnClickListener(mOnClickListener);
        mTextViewAudience0.setOnClickListener(mOnClickListener);
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.push0:
                    startPushStream();
                    break;
                case R.id.pull0:
                    openStream();
                    break;
                case R.id.audience0:
                    startAudienceActivity(0);
                    break;
                default:
                    break;
            }
        }
    };

    private void startAudienceActivity(int index) {

        String streamName = "";
        String anotherStreamName = "";
        if (index == 0) streamName = mStreamPath0.getText().toString();
        AudienceActivity.startActivity(getActivity(),
                streamName, anotherStreamName,
                mEnablePullUdp,
                mEnablePushUdp, mHardwareEncoder);
    }


    public void startPushStream() {
        final int delay = mLinkDelay.getText() == null || mLinkDelay.getText().equals("") ? 300
                : Integer.parseInt(mLinkDelay.getText().toString());
        PushStreamActivity.startActivity(getActivity(),
                mStreamPath0.getText().toString(),
                mLinkPath0.getText().toString(),
                mEnablePullUdp,
                mEnablePushUdp, mHardwareEncoder,mHardAEC, delay);
    }

    public void openStream() {
        final int delay = mLinkDelay.getText() == null || mLinkDelay.getText().equals("") ? 300
                : Integer.parseInt(mLinkDelay.getText().toString());
        PlayStreamActivity.startActivity(getActivity(),
                mStreamPath0.getText().toString(),
                mLinkPath0.getText().toString(),
                mEnablePullUdp,
                mEnablePushUdp, mHardwareEncoder,mHardAEC, delay);
    }

    public void setPrams(boolean enablePlayerUdp, boolean enableStreamUdp, boolean enableHardEncoder,boolean enableHWAEC) {
        Log.d(TAG, "setPrams: " + enablePlayerUdp + " " + enableStreamUdp + " " + enableHardEncoder+" " + enableHWAEC);
        mEnablePullUdp = enablePlayerUdp;
        mEnablePushUdp = enableStreamUdp;
        mHardwareEncoder = enableHardEncoder;
        mHardAEC = enableHWAEC;
    }
}
