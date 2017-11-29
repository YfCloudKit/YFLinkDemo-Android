package com.demo.yflinkdemo.extra;

import android.os.Environment;

/**
 * Created by yunfan on 2017/11/14.
 */

public class Const {
    public static String PUSH_URL_BASE = "rtmp://push-zk.yftest.yflive.net/live/";
    public static String PLAY_URL_BASE = "rtmp://rtmp-zk.yftest.yflive.net/live/";
    public static final String KEY_LINK_TYPE = "KEY_LINK_TYPE";
    public static final String KEY_HOST_ID = "KEY_HOST_ID";
    public static final String KEY_VICE_HOST_ID = "KEY_VICE_ID";
    public static final String KEY_ROLE = "KEY_ROLE";
    public static final String KEY_UDP_PUSH = "KEY_UDP_PUSH";
    public static final int ROLE_HOST = 1;
    public static final int ROLE_VICE_HOST = 2;
    public static final int ROLE_AUDIENCE = 3;
    public static final String DEFAULT_ROOM_ID = "test";
    public static final String DEFAULT_HOST_ID = "shenzhen800";
    public static final String DEFAULT_VICE_HOST_ID = "shenzhen801";

    public static final String AccessKey = "65592eeddc3d646db903a7367d58792268804f09";
    public static final String Token = "9e7299afc12d793a23d913d88be6fa6383f5876e";

    public static final String CACHE_DIRS = Environment.getExternalStorageDirectory().getPath()
            + "/yunfanencoder";

    public static int AUDIO = 11;
    public static int VIDEO = 12;
    public static int TV_LAYOUT_HALF_SCREEN = 192;
    public static int TV_LAYOUT_FULL_SCREEN = 193;
    public static boolean UDP_PUSH = false;
    public static int LINK_TYPE = VIDEO;


}
