package free.yhc.youtube.musicplayer.model;

public class Policy {
    public static class Constants {
        public static final String  REPORT_RECEIVER         = "yhcting77@gmail.com";
        public static final String  APPDATA_DIR             = "/sdcard/ytmplayer/";
        public static final String  EXTERNAL_DBFILE         = APPDATA_DIR + "ytmplayer.db";

        // --------------------------------------------------------------------
        // Initialize App.
        // --------------------------------------------------------------------
        public static final int     APPINIT_TIMEOUT         = 15000; // 15 sec


        // --------------------------------------------------------------------
        // Youtube
        // --------------------------------------------------------------------
        public static final int     DEFAULT_VIDEO_VOLUME    = 50;
        public static final int     YTSEARCH_MAX_RESULTS    = 25; // 1 ~ 50

        // --------------------------------------------------------------------
        // Network access
        // --------------------------------------------------------------------
        public static final int     WEBSERVER_PORT          = 59923;
        // Too long : user waits too long time to get feedback.
        // Too short : fails on bad network condition.
        public static final int     NETWORK_CONN_TIMEOUT    = 5000;
        public static final int     NETOWRK_CONN_RETRY      = 3;
    }
}
