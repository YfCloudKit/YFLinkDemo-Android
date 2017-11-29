package com.demo.yflinkdemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.demo.yflinkdemo.extra.Const;
import com.demo.yflinkdemo.widget.DeviceUtil;
import com.demo.yflinkdemo.widget.Log;
import com.demo.yflinkdemo.widget.PermissionUtils;
import com.yunfan.auth.YfAuthentication;
import com.yunfan.player.widget.YfCloudPlayer;
import com.yunfan.yflinkkit.YfLinkerKit;
import com.yunfan.yflinkkit.bean.LinkerMember;
import com.yunfan.yflinkkit.callback.QueryLinkCallback;

import java.io.IOException;
import java.util.List;

public class LinkSetActivity extends AppCompatActivity {
    private static final String TAG = LinkSetActivity.class.getSimpleName();
//
//    /**
//     * 房间id（预留）
//     */
//    private EditText mEditTextRoomId;
    /**
     * 主播id
     */
    private EditText mEditTextHostId;
    /**
     * 副播id
     */
    private EditText mEditTextViceHostId;
    private int mRole = Const.ROLE_HOST;
    private boolean DEBUG = false;
    private int mLinkType = Const.VIDEO;
    private boolean mUDPPush = Const.UDP_PUSH;
    private YfLinkerKit mYfLinkerKit;
    private YfCloudPlayer mYfCloudPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_set);
        DeviceUtil.fullScreen(this);
        PermissionUtils.askLinkPermission(this, mInitViewRunnable);
    }

    private Runnable mInitViewRunnable = new Runnable() {
        @Override
        public void run() {
//            mEditTextRoomId = (EditText) findViewById(R.id.et_room_id);
            mEditTextHostId = (EditText) findViewById(R.id.et_host_id);
            mEditTextViceHostId = (EditText) findViewById(R.id.et_vice_host_id);
//            mEditTextRoomId.setText(Const.DEFAULT_ROOM_ID);
            mEditTextHostId.setText(Const.DEFAULT_HOST_ID);
            mEditTextViceHostId.setText(Const.DEFAULT_VICE_HOST_ID);
            mEditTextHostId.requestFocus();
            mEditTextHostId.setSelection(mEditTextHostId.getText().toString().length());

            Switch switchAudio = (Switch) findViewById(R.id.switch_audio);
//            Const.LINK_TYPE = Const.VIDEO;
            switchAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    Const.LINK_TYPE = isChecked ? Const.AUDIO : Const.VIDEO;
                    mLinkType = isChecked ? Const.AUDIO : Const.VIDEO;
                }
            });
            Switch switchUDP = (Switch) findViewById(R.id.switch_udp);
//            Const.LINK_TYPE = Const.VIDEO;
            switchUDP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    Const.LINK_TYPE = isChecked ? Const.AUDIO : Const.VIDEO;
                    mUDPPush = isChecked;
                }
            });
            RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_group);
            radioGroup.setOnCheckedChangeListener(mOnCheckedListener);

            RadioButton host= (RadioButton) findViewById(R.id.rb_host);
            RadioButton vice= (RadioButton) findViewById(R.id.rb_vice_host);
            RadioButton audience= (RadioButton) findViewById(R.id.rb_audience);
            host.setOnClickListener(mOnClickListener);
            vice.setOnClickListener(mOnClickListener);
            audience.setOnClickListener(mOnClickListener);
//            host.setOnFocusChangeListener(mOnFocusChangeListener);
//            vice.setOnFocusChangeListener(mOnFocusChangeListener);
//            audience.setOnFocusChangeListener(mOnFocusChangeListener);

            Button buttonEnter = (Button) findViewById(R.id.btn_enter);
            Button buttonEnterOld = (Button) findViewById(R.id.btn_enter_old);
            if (DEBUG) buttonEnterOld.setVisibility(View.VISIBLE);
            buttonEnter.setOnClickListener(mOnClickListener);
            buttonEnterOld.setOnClickListener(mOnClickListener);
            YfAuthentication.getInstance().authenticate(Const.AccessKey, Const.Token, null);//鉴权
        }
    };

    private ProgressDialog mProgressDialog;
    View.OnClickListener mOnClickListener = new View.OnClickListener() {


        @Override
        public void onClick(View v) {
            Intent intent;
            switch (v.getId()) {
                case R.id.btn_enter:
                    showDialog();
                    queryStreamInfo();
                    break;
                case R.id.btn_enter_old:
                    intent = new Intent(LinkSetActivity.this, MainActivity.class);
                    startActivity(intent);
                    break;
                case R.id.rb_host:
                    mRole = Const.ROLE_HOST;
                    ((LinearLayout) mEditTextHostId.getParent()).setVisibility(View.VISIBLE);
                    ((LinearLayout) mEditTextViceHostId.getParent()).setVisibility(View.INVISIBLE);
                    break;
                case R.id.rb_vice_host:
                    mRole = Const.ROLE_VICE_HOST;
                    ((LinearLayout) mEditTextHostId.getParent()).setVisibility(View.VISIBLE);
                    ((LinearLayout) mEditTextViceHostId.getParent()).setVisibility(View.VISIBLE);
                    break;
                case R.id.rb_audience:
                    mRole = Const.ROLE_AUDIENCE;
                    ((LinearLayout) mEditTextHostId.getParent()).setVisibility(View.VISIBLE);
                    ((LinearLayout) mEditTextViceHostId.getParent()).setVisibility(View.INVISIBLE);
                    break;
            }
        }
    };

    private void showDialog() {
        mProgressDialog = new ProgressDialog(LinkSetActivity.this);
        mProgressDialog.setTitle("正在查询，请稍候...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void dismissDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    private void enterLinkOrPlayer() {
        Intent intent;
        if (!YfAuthentication.getInstance().isAuthenticateSucceed()) {
            Toast.makeText(LinkSetActivity.this, "鉴权失败", Toast.LENGTH_SHORT).show();
            return;
        }
//                    String roomId = mEditTextRoomId.getText().toString();
        String hostId = mEditTextHostId.getText().toString();
        String viceHostId = mEditTextViceHostId.getText().toString();
        if (TextUtils.isEmpty(hostId) || TextUtils.isEmpty(viceHostId)) {
            Toast.makeText(LinkSetActivity.this, "主播ID和副播ID不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        closePlayer();
        intent = new Intent(LinkSetActivity.this, VideoLinkActivity.class);
        intent.putExtra(Const.KEY_LINK_TYPE, mLinkType);
        intent.putExtra(Const.KEY_HOST_ID, hostId);
        intent.putExtra(Const.KEY_VICE_HOST_ID, viceHostId);
        intent.putExtra(Const.KEY_ROLE, mRole);
        intent.putExtra(Const.KEY_UDP_PUSH, mUDPPush);
        startActivity(intent);
    }

    private void queryStreamInfo() {
        mYfLinkerKit = new YfLinkerKit(LinkSetActivity.this);
        mYfLinkerKit.getStreamInfo(Const.PLAY_URL_BASE + mEditTextHostId.getText().toString()
                , new QueryLinkCallback() {
                    @Override
                    public void onSuccess(boolean isOpen, List<LinkerMember> memberList) {
                        Log.d(TAG, String.format("queryLink onSuccess isOpen: %s, srtmp: %s, roomID: %s, ip: %s,members:%s", isOpen, "", "", "", memberList == null ? 0 : memberList.size()));
                        if (isOpen) {//主播已开启连麦
                            if (mRole == Const.ROLE_HOST) {
//                                showToast("该房间正在连麦，请更换房间id和副播id");
                                enterLinkOrPlayer();
                            } else {
                                enterLinkOrPlayer();
                            }
                            dismissDialog();

                            //可能主播只是退出房间但是socket没断，暂时通过预加载来判断主播状态
//                            tryToOpenHostUrl();
                        } else {
                            if (mRole == Const.ROLE_HOST) {
                                enterLinkOrPlayer();
                            } else {
                                showToast("主播未开启连麦");
                            }
                            dismissDialog();
                        }
                    }

                    @Override
                    public void onFailed(int code) {
                        Log.d(TAG, "queryLink onFailed code: " + code);
                        dismissDialog();
                        showToast("查询失败：" + code);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.d(TAG, "queryLink onError: " + e.getMessage());
                        dismissDialog();
                        showToast("查询异常：" + e.getMessage());
                    }
                });
    }

    private void tryToOpenHostUrl() {
        if (mYfCloudPlayer == null)
            mYfCloudPlayer = YfCloudPlayer.Factory.createPlayer(LinkSetActivity.this, YfCloudPlayer.MODE_SOFT);
        try {
            mYfCloudPlayer.setDataSource(Const.PLAY_URL_BASE + mEditTextHostId.getText().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
//        mYfCloudPlayer.setHTTPTimeOutUs(1000 * 1000);
        mYfCloudPlayer.enableHttpDNS(true);
        mYfCloudPlayer.setOnErrorListener(new YfCloudPlayer.OnErrorListener() {
            @Override
            public boolean onError(YfCloudPlayer mp, int what, int extra) {
                Log.d(TAG, "onPrepared: ");
                if (mRole == Const.ROLE_HOST) {
                    enterLinkOrPlayer();
                } else {
                    showToast("主播未推流或者推流异常，请核验房间id是否正确或者主播是否正常推流");
                }
                dismissDialog();
                closePlayer();
                return false;
            }
        });
        mYfCloudPlayer.setOnPreparedListener(new YfCloudPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(YfCloudPlayer mp) {
                Log.d(TAG, "onPrepared: ");
                if (mRole == Const.ROLE_HOST) {
                    showToast("目前已有人使用该房间推流，请更换房间id");
                } else {
                    enterLinkOrPlayer();
                }
                dismissDialog();
                closePlayer();
            }
        });
        mYfCloudPlayer.prepareAsync();
    }

    private void closePlayer() {
        if (mYfCloudPlayer != null) {
            mYfCloudPlayer.stop();
            mYfCloudPlayer.release();
            mYfCloudPlayer = null;
        }
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LinkSetActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


//    boolean mHasFocus;
//    private RadioGroup.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {
//        @Override
//        public void onFocusChange(View v, boolean hasFocus) {
////            if(hasFocus){
//                ((RadioButton)v).setChecked(hasFocus);
////            }
//        }
//    };

//    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//
//        }
//    };
    private RadioGroup.OnCheckedChangeListener mOnCheckedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.rb_host:
                    mRole = Const.ROLE_HOST;
                    ((LinearLayout) mEditTextHostId.getParent()).setVisibility(View.VISIBLE);
                    ((LinearLayout) mEditTextViceHostId.getParent()).setVisibility(View.INVISIBLE);
                    break;
                case R.id.rb_vice_host:
                    mRole = Const.ROLE_VICE_HOST;
                    ((LinearLayout) mEditTextHostId.getParent()).setVisibility(View.VISIBLE);
                    ((LinearLayout) mEditTextViceHostId.getParent()).setVisibility(View.VISIBLE);
                    break;
                case R.id.rb_audience:
                    mRole = Const.ROLE_AUDIENCE;
                    ((LinearLayout) mEditTextHostId.getParent()).setVisibility(View.VISIBLE);
                    ((LinearLayout) mEditTextViceHostId.getParent()).setVisibility(View.INVISIBLE);
                    break;
                default:
                    mRole = Const.ROLE_HOST;
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mYfLinkerKit != null) mYfLinkerKit.stopThread();
        closePlayer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(requestCode, grantResults, mInitViewRunnable, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LinkSetActivity.this, "", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
