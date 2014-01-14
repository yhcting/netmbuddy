/******************************************************************************
 *    Copyright (C) 2012, 2013, 2014 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of NetMBuddy.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.	If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy.model;

import android.os.Environment;
import free.yhc.netmbuddy.utils.Utils;

public class Policy {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(Policy.class);

    public static final String  APPBASENAME             = "netmbuddy";
    public static final String  EXTSTORAGE_DIR          = Environment.getExternalStorageDirectory().getPath() + "/";
    public static final String  APPDATA_DIR             = EXTSTORAGE_DIR + APPBASENAME + "/";
    public static final String  APPDATA_TMPDIR          = APPDATA_DIR + "tmp/";
    public static final String  APPDATA_LOGDIR          = APPDATA_DIR + "logs/";
    public static final String  APPDATA_CACHEDIR        = APPDATA_DIR + "cache/";
    // Downloaded video directory
    public static final String  APPDATA_VIDDIR          = APPDATA_DIR + "videos/";
    public static final String  APPDATA_ERRLOG          = APPDATA_LOGDIR + "last_error";
    public static final String  EXTERNAL_DBFILE         = APPDATA_DIR + APPBASENAME + ".db";

    // --------------------------------------------------------------------
    // Share
    // --------------------------------------------------------------------
    public static final String  SHARE_FILE_EXTENTION    = APPBASENAME;

    // --------------------------------------------------------------------
    // Youtube
    // --------------------------------------------------------------------
    public static final int     DEFAULT_VIDEO_VOLUME    = 50;
    public static final int     YTSEARCH_MAX_RESULTS    = 20; // 1 ~ 50
    // Using ViewPager requires # of threads twice of this value.
    public static final int     YTSEARCH_NR_PAGE_INDEX_BUTTONS     = 10;

    // Performance for loading thumbnail
    public static final int     YTSEARCH_LOAD_THUMBNAIL_INTERVAL    = 100; // 100ms
    public static final int     YTSEARCH_MAX_LOAD_THUMBNAIL_THREAD  = 4;
    public static final int     YTIMPORT_MAX_LOAD_THUMBNAIL_THREAD  = 10;

    // --------------------------------------------------------------------
    // Youtube Hack
    // --------------------------------------------------------------------
    // Really 5 minutes is correct and enough value?
    // For safety, the smaller is the better, but for good UX, the longer is the better (Trade-off).
    // The problem is, there is no way to know exact expire time even if there is 'expire' token is html source.
    // (It's totally dependent on server-side-implementation.)
    // So, this is just experimental value!
    public static final int     YTHACK_REUSE_TIMEOUT    = 5 * 60 * 1000; // 5 minutes
    public static final int     YTHACK_CACHE_SIZE       = 10; // large enough to hit cache for current active video.

    // --------------------------------------------------------------------
    // Youtube Player
    // --------------------------------------------------------------------
    public static final int     YTPLAYER_RETRY_ON_ERROR = 3;

    // NOTE
    // Below is now DEPRECATED comments.
    //
    // It is not deleted for future REFERENCE.
    // caching-ahead next video will started when
    //   "current buffering percent - current progress percent" reaches this value.
    // Most of time for downloading is waiting network response.
    // But in case of high quality video, it may be big burden to network traffic.
    // And MediaPlayer has buffer for progressive download.
    // So, if percent is too large, MediaPlayer never reaches buffering ahead enough because of buffer size.
    // So, tuning this value is NOT easy.
    //public static final int     YTPLAYER_CACHING_TRIGGER_POINT = 30; // percent.

    // NOTE
    // At the beginning of streaming, device is very busy.
    // So, caching need to be started with delay.
    public static final long    YTPLAYER_CACHING_DELAY  = 10000; // 10 seconds.
    public static final long    YTPLAYER_DOUBLE_TOUCH_INTERVAL  = 500;

    // Time before/after TTS start/end.
    public static final long    YTPLAYER_TTS_SPARE_TIME = 300; // ms

    // --------------------------------------------------------------------
    // Network access
    // --------------------------------------------------------------------
    public static final int     PROXY_PORT              = 59923;
    public static final String  HTTP_UASTRING
        = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.168 Chrome/18.0.1025.168 Safari/535.19";
    // Too long : user waits too long time to get feedback.
    // Too short : fails on bad network condition.
    public static final int     NETWORK_CONN_TIMEOUT    = 5000;
    public static final int     NETOWRK_CONN_RETRY      = 3;

    // --------------------------------------------------------------------
    // Video Player
    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // Searching
    // --------------------------------------------------------------------
    public static final float   SIMILARITY_THRESHOLD_VERYLOW    = 0.007f;
    public static final float   SIMILARITY_THRESHOLD_LOW        = 0.05f;
    public static final float   SIMILARITY_THRESHOLD_NORMAL     = 0.1f;
    public static final float   SIMILARITY_THRESHOLD_HIGH       = 0.4f;
    public static final float   SIMILARITY_THRESHOLD_VERYHIGH   = 0.7f;
    public static final int     MAX_SIMILAR_TITLES_RESULT   = 99999999; // actually no-limitation.
    // --------------------------------------------------------------------
    // Usage Report
    // --------------------------------------------------------------------
    public static final String REPORT_RECEIVER          = "yhcting77@gmail.com";
    public static final String TIME_STAMP_FILE_SUFFIX   = "____tmstamp___";
}
