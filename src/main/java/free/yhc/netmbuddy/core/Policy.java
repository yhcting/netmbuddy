/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.netmbuddy.core;

import android.os.Environment;
import free.yhc.netmbuddy.utils.Utils;

public class Policy {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(Policy.class);

    public static final String APPBASENAME = "netmbuddy";
    public static final String EXTSTORAGE_DIR = Environment.getExternalStorageDirectory().getPath() + "/";
    public static final String APPDATA_DIR = EXTSTORAGE_DIR + APPBASENAME + "/";
    public static final String APPDATA_TMPDIR = APPDATA_DIR + "tmp/";
    public static final String APPDATA_LOGDIR = APPDATA_DIR + "logs/";
    public static final String APPDATA_CACHEDIR = APPDATA_DIR + "cache/";
    // Downloaded video directory
    public static final String APPDATA_VIDDIR = APPDATA_DIR + "videos/";
    public static final String APPDATA_ERRLOG = APPDATA_LOGDIR + "last_error";
    public static final String EXTERNAL_DBFILE = APPDATA_DIR + APPBASENAME + ".db";

    // --------------------------------------------------------------------
    // Share
    // --------------------------------------------------------------------
    public static final String SHARE_FILE_EXTENTION = APPBASENAME;

    // --------------------------------------------------------------------
    // Youtube
    // --------------------------------------------------------------------
    public static final int DEFAULT_VIDEO_VOLUME = 50;
    public static final int YTSEARCH_MAX_RESULTS = 20; //YTApiFacade.MAX_RESULTS_PER_PAGE; // 1 ~ 50

    // Performance for loading thumbnail
    public static final int YTSEARCH_MAX_LOAD_THUMBNAIL_THREAD = 4;
    public static final int YTIMPORT_MAX_LOAD_THUMBNAIL_THREAD = 10;

    // --------------------------------------------------------------------
    // Youtube Hack
    // --------------------------------------------------------------------
    // Really 5 minutes is correct and enough value?
    // For safety, the smaller is the better, but for good UX, the longer is the better (Trade-off).
    // The problem is, there is no way to know exact expire time even if there is 'expire' token is html source.
    // (It's totally dependent on server-side-implementation.)
    // So, this is just experimental value!
    public static final int YTHACK_REUSE_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    public static final int YTHACK_CACHE_SIZE = 10; // large enough to hit cache for current active video.

    // --------------------------------------------------------------------
    // Youtube Player
    // --------------------------------------------------------------------
    public static final int YTPLAYER_RETRY_ON_ERROR = 3;

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
    public static final long YTPLAYER_CACHING_DELAY = 10000; // 10 seconds.
    public static final long YTPLAYER_DOUBLE_TOUCH_INTERVAL = 500;

    // Time before/after TTS start/end.
    public static final long YTPLAYER_TTS_MARGIN_TIME = 300; // ms

    // --------------------------------------------------------------------
    // Network access
    // --------------------------------------------------------------------
    // Too long : user waits too long time to get feedback.
    // Too short : fails on bad network condition.
    public static final int NETWORK_CONN_TIMEOUT = 5000;
    public static final int NETOWRK_CONN_RETRY = 3;

    // --------------------------------------------------------------------
    // Video Player
    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // Searching
    // --------------------------------------------------------------------
    public static final float SIMILARITY_THRESHOLD_VERYLOW = 0.007f;
    public static final float SIMILARITY_THRESHOLD_LOW = 0.05f;
    public static final float SIMILARITY_THRESHOLD_NORMAL = 0.1f;
    public static final float SIMILARITY_THRESHOLD_HIGH = 0.4f;
    public static final float SIMILARITY_THRESHOLD_VERYHIGH = 0.7f;
    public static final int MAX_SIMILAR_TITLES_RESULT = 99999999; // actually no-limitation.
    // --------------------------------------------------------------------
    // Usage Report
    // --------------------------------------------------------------------
    public static final String REPORT_RECEIVER = "yhcting77dev0@gmail.com";

    // --------------------------------------------------------------------
    // Values dependent on Preference.
    // --------------------------------------------------------------------
}
