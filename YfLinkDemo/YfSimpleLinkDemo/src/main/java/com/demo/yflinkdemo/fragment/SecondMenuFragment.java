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
import com.demo.yflinkdemo.PushStreamLinkerActivity;
import com.demo.yflinkdemo.R;


public class SecondMenuFragment extends Fragment {
    private static final String TAG = SecondMenuFragment.class.getSimpleName();
    private static final String ARG_PARAM1 = "enablePullUdp";
    private static final String ARG_PARAM2 = "enablePushUdp";
    private static final String ARG_PARAM3 = "hardEncoder";
    private boolean mEnablePullUdp;
    private boolean mEnablePushUdp;
    private boolean mHardwareEncoder;
    private EditText mStreamPath1;
    private EditText mStreamPath2;
    private EditText mLinkDelay;
    private TextView mTextViewPush1;
    private TextView mTextViewPush2;
    private TextView mTextViewAudience1;
    private TextView mTextViewAudience2;

    public SecondMenuFragment() {
    }

    public static SecondMenuFragment newInstance(boolean enablePullUdp, boolean enablePushUdp,
                                                 boolean isHardware) {
        SecondMenuFragment fragment = new SecondMenuFragment();
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
        return inflater.inflate(R.layout.fragment_second_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mStreamPath1 = (EditText) view.findViewById(R.id.stream_url_1);//主播端连麦，主播1的流名
        mStreamPath1.setText("test101");

        mStreamPath2 = (EditText) view.findViewById(R.id.stream_url_2);//主播端连麦，主播2的流名
        mStreamPath2.setText("test102");

        mLinkDelay = (EditText) view.findViewById(R.id.link_buffer);
        mLinkDelay.setText("400");
        mTextViewPush1 = (TextView) view.findViewById(R.id.push1);
        mTextViewPush2 = (TextView) view.findViewById(R.id.push2);
        mTextViewAudience1 = (TextView) view.findViewById(R.id.audience1);
        mTextViewAudience2 = (TextView) view.findViewById(R.id.audience2);
        mTextViewPush1.setOnClickListener(mOnClickListener);
        mTextViewPush2.setOnClickListener(mOnClickListener);
        mTextViewAudience1.setOnClickListener(mOnClickListener);
        mTextViewAudience2.setOnClickListener(mOnClickListener);

    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.push1:
                    startPushStreamLink(1);
                    break;
                case R.id.push2:
                    startPushStreamLink(2);
                    break;
                case R.id.audience1:
                    startAudienceActivity(1);
                    break;
                case R.id.audience2:
                    startAudienceActivity(2);
                    break;
                default:
                    break;
            }
        }
    };

    private void startAudienceActivity(int index) {
        String streamName = "";
        String anotherStreamName = "";
        if (index == 1) {
            streamName = mStreamPath1.getText().toString();
            anotherStreamName = mStreamPath2.getText().toString();
        }
        if (index == 2) {
            streamName = mStreamPath2.getText().toString();
            anotherStreamName = mStreamPath1.getText().toString();
        }

        AudienceActivity.startActivity(getActivity(),
                streamName, anotherStreamName,
                mEnablePullUdp,
                mEnablePushUdp, mHardwareEncoder);
    }


    public void startPushStreamLink(int index) {
        final int delay = mLinkDelay.getText() == null || mLinkDelay.getText().equals("") ? 300
                : Integer.parseInt(mLinkDelay.getText().toString());
        PushStreamLinkerActivity.startActivity(getActivity(),
                index == 1 ? mStreamPath1.getText().toString() : mStreamPath2.getText().toString(),
                index == 1 ? mStreamPath2.getText().toString() : mStreamPath1.getText().toString(),
                mEnablePullUdp,
                mEnablePushUdp, mHardwareEncoder, delay);
    }

    public void setPrams(boolean enablePlayerUdp, boolean enableStreamUdp, boolean enableHardEncoder) {
        Log.d(TAG, "setPrams: " + enablePlayerUdp + " " + enableStreamUdp + " " + enableHardEncoder);
        mEnablePullUdp = enablePlayerUdp;
        mEnablePushUdp = enableStreamUdp;
        mHardwareEncoder = enableHardEncoder;
    }
}

