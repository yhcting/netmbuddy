/******************************************************************************
 * Copyright (C) 2016
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

package free.yhc.netmbuddy.task;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.adapter.HandlerAdapter;
import free.yhc.baselib.async.HelperHandler;
import free.yhc.baselib.async.ThreadEx;
import free.yhc.baselib.async.TmTask;
import free.yhc.baselib.exception.UnsupportedFormatException;
import free.yhc.baselib.net.NetConnHttp;
import free.yhc.baselib.net.NetReadTask;
import free.yhc.netmbuddy.core.PolicyConstant;
import free.yhc.netmbuddy.core.RTState;
import free.yhc.netmbuddy.utils.ReportUtil;
import free.yhc.netmbuddy.utils.Util;

public class YTHackTask extends TmTask<Void> {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTHackTask.class, Logger.LOGLV_DEFAULT);

    public static final int YTQUALITY_SCORE_MAXIMUM = 100;
    public static final int YTQUALITY_SCORE_HIGHEST = 100;
    public static final int YTQUALITY_SCORE_HIGH    = 80;
    public static final int YTQUALITY_SCORE_MIDHIGH = 60;
    public static final int YTQUALITY_SCORE_MIDLOW  = 40;
    public static final int YTQUALITY_SCORE_LOW     = 20;
    public static final int YTQUALITY_SCORE_LOWEST  = 0;
    public static final int YTQUALITY_SCORE_MINIMUM = 0;

    // See youtube api documentation.
    private static final int YTVID_LENGTH = 11;
    private static final int YTQSCORE_INVALID = -1;
    private static final int YTITAG_INVALID = -1;

    private static final Pattern sYtUrlStreamMapPattern
            = Pattern.compile(".*\"url_encoded_fmt_stream_map\":\\s*\"([^\"]+)\".*");

    // [ Small talk... ]
    // Why "generate_204"?
    // 204 is http response code that means "No Content".
    // Interestingly, if GET is requested to url that includes "generate_204",
    //   204 (No Content) response comes.
    // So, this URL is a kind of special URL that creates 204 response
    //   and notify to server that preparing real-contents.
    private static final Pattern sYtUrlGenerate204Pattern
            = Pattern.compile(".*\"(http(s)?:.+/generate_204[^\"]*)\".*");


    private final String mYtvid;

    private YtVideoPageInfo mYtvpi = null;
    private Object mOpaque = null;

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    public static class YtVideo {
        public final String url;
        public final boolean video; // is video type?
        YtVideo(String url, boolean video) {
            this.url = url;
            this.video = video;
        }
    }

    // See Youtube web(html) interface
    private enum ElemQuality {
        SMALL,
        MEDIUM,
        HD720;

        static ElemQuality
        parse(String quality) {
            try {
                return ElemQuality.valueOf(quality.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static class ElemType {
        enum StreamType {
            AUDIO,
            VIDEO;

            static StreamType
            parse(String type) {
                try {
                    return StreamType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        enum StreamFormat {
            MP4,
            FLV,
            x3GPP,
            WEBM;

            static StreamFormat
            parse (String format) {
                switch (format) {
                case "mp4":
                    return MP4;
                case "x-flv":
                    return FLV;
                case "webm":
                    return WEBM;
                case "3gpp":
                    return x3GPP;
                }
                return null;
            }
        }

        private static final Pattern _mPat = Pattern.compile(
                "^"
                        + "([\\w\\-]+)"  // type (video / audio) - group(1)
                        + "(?:\\%2F|/)"  // delimiter
                        + "([\\w\\-]+)"  // format (mp4, 3gp ...) - group(2)
                        + "(?:"			 // section for codecs
                        + "(?:\\%3B|;)\\+codecs\\%3D" // delimiter
                        + "\\%22" 	 // start of codec string
                        + "(.*)" // codec string. - group(3) [optional]
                        + "\\%22"    // end of codec string
                        + ")?.*$"		 // remains
        );


        StreamType type;
        StreamFormat format;
        String[] codecs;


        private ElemType() { }

        static ElemType
        parse(String typeElem) {
            typeElem = typeElem.trim();
            Matcher m = _mPat.matcher(typeElem);
            if (!m.matches())
                return null;
            String ty = m.group(1);
            String fmt = m.group(2);
            String codecstr = m.group(3);
            String[] codecs = null;
            if (null != codecstr)
                codecs = codecstr.split("\\%2C\\+");
            StreamType type = StreamType.parse(ty);
            StreamFormat format = StreamFormat.parse(fmt);
            if (null == type
                    || null == format)
                return null;
            ElemType et = new ElemType();
            et.type = type;
            et.format = format;
            et.codecs = codecs;
            return et;
        }
    }

    private static class ElemSize {
        int w;
        int h;
        ElemSize(int w, int h) {
            this.w = w;
            this.h = h;
        }

        static ElemSize
        parse(String sizeElem) {
            sizeElem = sizeElem.trim();
            String[] s = sizeElem.split("x");
            if (2 != s.length)
                return null;
            try {
                int w = Integer.parseInt(s[0]);
                int h = Integer.parseInt(s[1]);
                return new ElemSize(w, h);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private static class YtVideoElem {
        String  url = "";
        int tag = YTITAG_INVALID;
        ElemType type = null;
        ElemSize size = null;
        ElemQuality quality = null;
        // Quality score of this video
        // This value is guesses from 'tag'
        // -1 means "invalid, so, DO NOT use this video".
        int qscore = YTQSCORE_INVALID;

        private YtVideoElem() {} // DO NOT CREATE DIRECTLY

        static YtVideoElem
        parse(String ytString) {
            YtVideoElem ve = new YtVideoElem();
            try {
                ytString = URLDecoder.decode(ytString, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                P.bug(false);
            }

            String sig = null;
            String[] elems = ytString.split("\\\\u0026");
            for (String e : elems) {
                if (e.startsWith("itag=")) {
                    try {
                        ve.tag = Integer.parseInt(e.substring("itag=".length()));
                    } catch (NumberFormatException ignored) { }
                } else if (e.startsWith("url="))
                    ve.url = e.substring("url=".length());
                else if (e.startsWith("type=")) {
                    ve.type = ElemType.parse(e.substring("type=".length()));
                    if (null == ve.type)
                        P.w("Unknown element format : type : " + e);
                } else if (e.startsWith("quality=")) {
                    ve.quality = ElemQuality.parse(e.substring("quality=".length()));
                    if (null == ve.quality)
                        P.w("Unknown element format : quality : " + e);
                } else if (e.startsWith("size=")) {
                    ve.size = ElemSize.parse(e.substring("size=".length()));
                    if (null == ve.size)
                        P.w("Unknown element format : size : " + e);
                } else if (e.startsWith("sig="))
                    sig = e.substring("sig=".length());

            }

            // Mandatory fields : url, tag and type.
            if (ve.url.isEmpty()
                    || YTITAG_INVALID == ve.tag
                    || null == ve.type)
                return null; // Not supported video.

            if (null != sig)
                ve.url += "&signature=" + sig;
            else
            if (DBG) P.w("NO SIGNATURE in URL STRING!!!");


            ve.qscore = getPolicyQualityScore(ve);
            return ve;
        }

        @SuppressWarnings("unused")
        static String
        dump(YtVideoElem e) {
            return "[Video Elem]\n"
                    + "  itag=" + e.tag + "\n"
                    + "  url=" + e.url + "\n"
                    + "  type=" + e.type + "\n"
                    + "  quality=" + e.quality + "\n"
                    + "  qscore=" + e.qscore;
        }
    }

    private static class YtVideoPageInfo {
        long tmstamp = 0; // System time in milli.
        // video is playable on specified 'UA String'(based on html-parsing)
        boolean playable = true;
        String generate_204_url = ""; // url including generate 204
        YtVideoElem[] vids = new YtVideoElem[0];
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    private static String
    getYtUrl(String ytvid) {
        P.bug(YTVID_LENGTH == ytvid.length());
        return "watch?v=" + ytvid;
    }

    private static String
    getYtHost() {
        return "www.youtube.com";
    }

    private static int
    getPolicyQualityScore(YtVideoElem ve) {
        if (null != ve.quality) {
            switch (ve.quality) {
            case SMALL: return YTQUALITY_SCORE_LOW;
            case MEDIUM: return YTQUALITY_SCORE_MIDLOW;
            case HD720: return YTQUALITY_SCORE_HIGH;
            }
        }

        if (null != ve.size) {
            if (ve.size.h <= 144) {
                return YTQUALITY_SCORE_LOWEST;
            } else if (ve.size.h <= 240) {
                return YTQUALITY_SCORE_LOW;
            } else if (ve.size.h <= 360) {
                return YTQUALITY_SCORE_MIDLOW;
            } else if (ve.size.h <= 480) {
                return YTQUALITY_SCORE_MIDHIGH;
            } else if (ve.size.h <= 720) {
                return YTQUALITY_SCORE_HIGH;
            } else if (ve.size.h <= 1080) {
                return YTQUALITY_SCORE_HIGHEST;
            } else {
                return YTQUALITY_SCORE_HIGHEST;
            }
        }

        // Audio stream without any quality information, is considered 'lowest'
        if (ElemType.StreamType.AUDIO == ve.type.type)
            return YTQUALITY_SCORE_LOWEST;

        P.w("Fail to decide quality score : set as 'lowest'");
        return YTQUALITY_SCORE_LOWEST;
    }

    private static boolean
    isPlayableOnDevice(YtVideoElem ve) {
        // NOTE:
        // - Audio type element is NOT considered yet on YTPlayer.
        // - 'flv' are not supported on Android device by default.
        //
        // See : "http://developer.android.com/guide/appendix/media-formats.html" for details
        return ve.type.type == ElemType.StreamType.VIDEO
                && (ve.type.format == ElemType.StreamFormat.MP4
                || ve.type.format == ElemType.StreamFormat.x3GPP);
    }

    private static boolean
    verifyYtVideoPageInfo(YtVideoPageInfo ytvpi) {
        return ytvpi.vids.length > 0
                && Util.isValidValue(ytvpi.generate_204_url);
    }

    private static YtVideoPageInfo
    parseYtVideoPageHtml(BufferedReader brdr) throws IOException {
        YtVideoPageInfo ytvpi = new YtVideoPageInfo();
        String line = "";
        while (null != line) {
            line = brdr.readLine();
            if (null == line)
                break;

            if (line.contains("\"player-unavailable\"")) {
                // Ignore below checking.
                // In some use-cases, player may be unavailable for this device.
                // (In the web page source, 'div' 'player-unavailable' is observed.)
                // But, I found that video is still playable!.
                // Something changed in Youtube web page source.
                // Anyway, this is just workaround and works for most cases.
                //
                // This is unavailable video on the specified UA string
                //result.playable = false;
                //break;
            } else if (line.contains("/generate_204")) {
                Matcher m = sYtUrlGenerate204Pattern.matcher(line);
                if (m.matches()) {
                    line = m.group(1);
                    line = line.replaceAll("\\\\u0026", "&");
                    line = line.replaceAll("\\\\", "");
                    ytvpi.generate_204_url = line;
                }
            } else if (line.contains("\"url_encoded_fmt_stream_map\":")) {
                Matcher m = sYtUrlStreamMapPattern.matcher(line);
                if (m.matches()) {
                    line = m.group(1);
                    String[] vidElemUrls = line.split(",");
                    ArrayList<YtVideoElem> al = new ArrayList<>(vidElemUrls.length);
                    for (String s : vidElemUrls) {
                        YtVideoElem ve = YtVideoElem.parse(s);
                        if (null != ve)
                            al.add(ve);
                    }
                    ytvpi.vids = al.toArray(new YtVideoElem[al.size()]);
                }
            }
        }
        ytvpi.tmstamp = System.currentTimeMillis();
        return ytvpi;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void
    onEarlyPostRun (Void result, Exception ex) {
        if (null == ex) {
            P.bug(hasHackedResult());
            AppEnv.getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    // This should be called at UI handler thread
                    RTState.get().cachingYtHack(YTHackTask.this);
                }
            });
        }
    }

    public YTHackTask(
            @NonNull String name,
            @NonNull HandlerAdapter owner,
            int priority,
            boolean interruptOnCancel,
            @NonNull String ytvid) {
        super(name, owner, priority, interruptOnCancel);
        mYtvid = ytvid;
    }

    public static class Builder<B extends Builder>
            extends TmTask.Builder<B, YTHackTask> {
        private final String mYtvid;
        public Builder(@NonNull String ytvid) {
            super();
            mName = tmId(ytvid);
            mOwner = HelperHandler.get();
            mPriority = ThreadEx.TASK_PRIORITY_NORM;
            mInterruptOnCancel = true;
            mYtvid = ytvid;
        }

        @Override
        @NonNull
        public YTHackTask
        create() {
            return new YTHackTask(mName, mOwner, mPriority, mInterruptOnCancel, mYtvid);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    /**
     * NOTE
     * This is based on experimental result.
     * There is no official API regarding getting thumbnail via Youtube video id.
     * So, it is NOT 100% guaranteed that correct url is returned.
     * But, I'm strongly sure that return value is valid and correct based on my experience.
     * That's the reason why this function is member of 'YTHackTask'(Not YTDataAdapter).
     * @param ytvid Youtube video id
     */
    public static String
    getYtVideoThumbnailUrl(String ytvid) {
        // These days, https is used by default
        return "https://i.ytimg.com/vi/" + ytvid + "/default.jpg";
    }

    public static String
    getYtVideoPageUrl(String ytvid) {
        // These days, https is used by default.
        return "https://" + getYtHost() + "/" + getYtUrl(ytvid);
    }

    public static int
    getQScorePreferLow(int qscore) {
        int score = qscore - 1;
        return score < YTQUALITY_SCORE_MINIMUM?
                YTQUALITY_SCORE_MINIMUM:
                score;
    }

    public static int
    getQScorePreferHigh(int qscore) {
        int score = qscore + 1;
        return score > YTQUALITY_SCORE_MAXIMUM?
                YTQUALITY_SCORE_MAXIMUM:
                score;
    }

    //=========================================================================
    //
    //=========================================================================
    @NonNull
    public static String
    tmId(@NonNull String ytvid) {
        return YTHackTask.class.getSimpleName() + ":" + ytvid;
    }

    @NonNull
    public String
    tmId() {
        return tmId(mYtvid);
    }

    public void
    setOpaque(Object opaque) {
        mOpaque = opaque;
    }

    public Object
    getOpaque() {
        return mOpaque;
    }

    public boolean
    hasHackedResult() {
        return null != mYtvpi;
    }

    @NonNull
    public String
    getYtvid() {
        return mYtvid;
    }

    public long
    getHackTimeStamp() {
        P.bug(hasHackedResult());
        return mYtvpi.tmstamp;
    }

    /**
     *
     * @param quality Quality value. See YTQUALITY_SCORE_XXX
     * @param exact true : exact matching is required.
     *              false : best-fit is found.
     */
    public YtVideo
    getVideo(int quality, boolean exact) {
        P.bug(0 <= quality && quality <= 100);
        if (null == mYtvpi
                || !mYtvpi.playable)
            return null;

        // Select video that has closest quality score
        YtVideoElem ve = null;
        int curgap = -1;
        for (YtVideoElem e : mYtvpi.vids) {
            if (YTQSCORE_INVALID != e.qscore
                    && isPlayableOnDevice(e)) {
                int qgap = quality - e.qscore;
                qgap = qgap < 0? -qgap: qgap;
                if (null == ve || qgap < curgap) {
                    ve = e;
                    curgap = qgap;
                }
            }
        }

        if (null == ve
                || (exact && 0 != curgap))
            return null;
        else
            return new YtVideo(ve.url,
                               ve.type.type == ElemType.StreamType.VIDEO);
    }
    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected Void
    doAsync() throws IOException, InterruptedException, UnsupportedFormatException {
        if (DBG) P.v("Hack: " + mYtvid);
        if (!Util.isNetworkAvailable())
            throw new ConnectException("Network unavailable");

        YtVideoPageInfo ytvpi;
        URL url = new URL(getYtVideoPageUrl(mYtvid));
        NetConnHttp conn = Util.createNetConnHttp(url, PolicyConstant.YTHACK_UASTRING);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        NetReadTask.Builder<NetReadTask.Builder> nrb = new NetReadTask.Builder<>(conn, baos);
        try {
            nrb.create().startSync();
        } catch (InterruptedException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ytvpi = parseYtVideoPageHtml(new BufferedReader(new InputStreamReader(bais)));
        if (!verifyYtVideoPageInfo(ytvpi)) {
            // this is invalid result value.
            // Ignore this result.
            // If not, this may cause mis-understanding that hacking is successful.
            // Note that "hasHackedResult()" uses "null != mYtr".
            if (DBG) P.w("Parse yt video page fails: Page");
            throw new UnsupportedFormatException();
        }
        if (DBG) ReportUtil.storeYtPage(baos.toString());

        // NOTE
        // HACK youtube protocol!
        // Do dummy 'GET' request with generate_204 url.
        try {
            url = new URL(ytvpi.generate_204_url);
        } catch (MalformedURLException e) {
            if (DBG) {
                P.w("Invalid generate_204_url");
                P.w("204 url: " + ytvpi.generate_204_url);
                P.w(baos.toString());
            }
            throw new UnsupportedFormatException();
        }

        conn = Util.createNetConnHttp(url, PolicyConstant.YTHACK_UASTRING);
        baos = new ByteArrayOutputStream();
        nrb = new NetReadTask.Builder<>(conn, baos);
        try {
            nrb.create().startSync();
        } catch (InterruptedException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // TODO check : if 204 gives valid result... it's unexpected!!

        // Now all are ready to download!
        // This is good moment to calculate quality score of each available elements.
        for (YtVideoElem ve : ytvpi.vids)
            ve.qscore = getPolicyQualityScore(ve);

        mYtvpi = ytvpi;
        return null;
    }
}
