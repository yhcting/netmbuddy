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

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;
import android.os.AsyncTask;
import free.yhc.netmbuddy.utils.Utils;

//
// This is main class for HACKING Youtube protocol.
//
public class YTHacker {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTHacker.class);

    public static final String HTTP_UASTRING
        = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.168 Chrome/18.0.1025.168 Safari/535.19";

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

    // TODO Need to verify below tags again
    // This is from experimental result and hacking script code.
    @SuppressWarnings("unused")
    private static final int YTSTREAMTAG_INVALID     = -1;

    private static final int YTSTREAMTAG_MPEG_1080p  = 37;
    private static final int YTSTREAMTAG_MPEG_720p   = 22;
    private static final int YTSTREAMTAG_MPEG_480p   = -1; // ???? What is itag value for mpeg4 480p???
    private static final int YTSTREAMTAG_MPEG_360p   = 18;

    private static final int YTSTREAMTAG_3GPP_240p   = 36;
    private static final int YTSTREAMTAG_3GPP_144p   = 17;

    @SuppressWarnings("unused")
    private static final int YTSTREAMTAG_WEBM_1080p  = 46;
    @SuppressWarnings("unused")
    private static final int YTSTREAMTAG_WEBM_720p   = 45;
    @SuppressWarnings("unused")
    private static final int YTSTREAMTAG_WEBM_480p   = 44;
    @SuppressWarnings("unused")
    private static final int YTSTREAMTAG_WEBM_360p   = 43;

    @SuppressWarnings("unused")
    private static final int YTSTREAMTAG_FLV_480p    = 35;
    @SuppressWarnings("unused")
    private static final int YTSTREAMTAG_FLV_360p    = 34;
    @SuppressWarnings("unused")
    private static final int YTSTREAMTAG_FLV_240p    = 5;

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

    private final NetLoader mLoader = new NetLoader();
    private final String mYtvid;
    private final Object mUser;
    private final YtHackListener mListener;
    // NOTE
    // mBgTask used as "private final" to avoid synchronizing issue.
    // If not, 'mBgTask' should be initialized with 'null'.
    // And, code for using 'mBgTask' always should be like this.
    //     if (null != mBgTask)
    //         mBgTask.xxxxx
    private final AsyncTask<Void, Void, Err> mBgTask;

    private YtVideoHtmlResult mYtr = null;
    private boolean mCancelled = false;

    public interface YtHackListener {
        void onPreHack(YTHacker ythack, String ytvid, Object user);
        void onHackCancelled(YTHacker ythack, String ytvid, Object user);
        void onPostHack(YTHacker ythack, Err result, NetLoader loader, String ytvid, Object user);
    }

    public enum Err {
        NO_ERR,
        IO_NET,
        NETWORK_UNAVAILABLE,
        PARSE_HTML,
        INTERRUPTED,
        UNKNOWN,   // err inside module
    }

    public static class LocalException extends java.lang.Exception {
        static final long serialVersionUID = 0; // to make compiler be happy

        private final Err _mErr;

        public LocalException(Err err) {
            _mErr = err;
        }

        public Err
        error() {
            return _mErr;
        }
    }

    public static class YtVideo {
        public final String url;
        public final String type; // mime
        YtVideo(String aUrl, String aType) {
            url = aUrl;
            type = aType;
        }
    }

    private static class YtVideoElem {
        String  url = "";
        String  tag = "";
        String  type = "";
        String  quality = "";
        // Quality score of this video
        // This value is guesses from 'tag'
        // -1 means "invalid, so, DO NOT use this video".
        int qscore = YTQSCORE_INVALID;

        private YtVideoElem() {} // DO NOT CREATE DIRECTLY

        private static int
        getQuailityScore(String tag) {
            int v = -1;
            try {
                v = Integer.parseInt(tag);
            } catch (NumberFormatException e) {
                eAssert(false);
            }

            // WebM and Flv is not supported directly in Android's MediaPlayer.
            // So, Mpeg is only option we can choose.
            switch (v) {
            case YTSTREAMTAG_MPEG_1080p:
                return YTQUALITY_SCORE_HIGHEST;
            case YTSTREAMTAG_MPEG_720p:
                return YTQUALITY_SCORE_HIGH;
            case YTSTREAMTAG_MPEG_480p:
                return YTQUALITY_SCORE_MIDHIGH;
            case YTSTREAMTAG_MPEG_360p:
                return YTQUALITY_SCORE_MIDLOW;
            case YTSTREAMTAG_3GPP_240p:
                return YTQUALITY_SCORE_LOW;
            case YTSTREAMTAG_3GPP_144p:
                return YTQUALITY_SCORE_LOWEST;
            default:
                // TODO : Check it for additional formats!
                // Webm and Flv should be considered here?
                // Webm and Flv format is NOT used!
                return YTQSCORE_INVALID;
            }
        }


        static YtVideoElem
        parse(String ytString) {
            YtVideoElem ve = new YtVideoElem();
            try {
                ytString = URLDecoder.decode(ytString, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                eAssert(false);
            }

            String   sig = null;
            String[] elems = ytString.split("\\\\u0026");
            for (String e : elems) {
                if (e.startsWith("itag="))
                    ve.tag = e.substring("itag=".length());
                else if (e.startsWith("url="))
                    ve.url = e.substring("url=".length());
                else if (e.startsWith("type="))
                    ve.type = e.substring("type=".length());
                else if (e.startsWith("quality="))
                    ve.quality = e.substring("quality=".length());
                else if (e.startsWith("sig="))
                    sig = e.substring("sig=".length());
            }

            if (ve.url.isEmpty()
                || ve.tag.isEmpty()
                || ve.type.isEmpty()
                || ve.quality.isEmpty())
                return null; // Not supported video.

            if (null != sig)
                ve.url += "&signature=" + sig;
            else
                if (DBG) P.w("NO SIGNATURE in URL STRING!!!");
            ve.qscore = getQuailityScore(ve.tag);
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

    private static class YtVideoHtmlResult {
        long tmstamp = 0; // System time in milli.
        String generate_204_url = ""; // url including generate 204
        YtVideoElem[] vids = new YtVideoElem[0];
    }

    private static Err
    map(NetLoader.Err err) {
        switch (err) {
        case NO_ERR:
            return Err.NO_ERR;

        case IO_NET:
        case HTTPGET:
            return Err.IO_NET;

        case INTERRUPTED:
            return Err.INTERRUPTED;

        default:
            return Err.UNKNOWN;
        }
    }

    private static String
    getYtUri(String ytvid) {
        eAssert(YTVID_LENGTH == ytvid.length());
        return "watch?v=" + ytvid;
    }

    private static String
    getYtHost() {
        return "www.youtube.com";
    }

    private static boolean
    verifyYtVideoHtmlResult(YtVideoHtmlResult ytr) {
        return ytr.vids.length > 0
               && Utils.isValidValue(ytr.generate_204_url);
    }

    private static YtVideoHtmlResult
    parseYtVideoHtml(BufferedReader brdr)
            throws LocalException {
        YtVideoHtmlResult result = new YtVideoHtmlResult();
        String line = "";
        while (null != line) {
            try {
                line = brdr.readLine();
            } catch (IOException e) {
                throw new LocalException(Err.IO_NET);
            }

            if (null == line)
                break;

            if (line.contains("/generate_204")) {
                Matcher m = sYtUrlGenerate204Pattern.matcher(line);
                if (m.matches()) {
                    line = m.group(1);
                    line = line.replaceAll("\\\\u0026", "&");
                    line = line.replaceAll("\\\\", "");
                    result.generate_204_url = line;
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
                    result.vids = al.toArray(new YtVideoElem[al.size()]);
                }
            }
        }
        result.tmstamp = System.currentTimeMillis();
        return result;
    }

    private void
    preExecute() {
        mLoader.open(HTTP_UASTRING);
        if (null != mListener)
            mListener.onPreHack(this, mYtvid, mUser);
    }

    private Err
    doMainWork() {
        if (!Utils.isNetworkAvailable())
            return Err.NETWORK_UNAVAILABLE;

        NetLoader.HttpRespContent content;
        Err err = Err.NO_ERR;
        YtVideoHtmlResult ytr = null;
        try {
            do {
                // Read and parse html web page of video.
                content = mLoader.getHttpContent(Uri.parse(getYtVideoPageUrl(mYtvid)));
                if (HttpUtils.SC_OK != content.stcode) {
                    err = Err.IO_NET;
                    break;
                }
                eAssert(content.type.toLowerCase().startsWith("text/html"));
                ytr = parseYtVideoHtml(new BufferedReader(new InputStreamReader(content.stream)));
                if (!verifyYtVideoHtmlResult(ytr)) {
                    // this is invalid result value.
                    // Ignore this result.
                    // If not, this may cause mis-understanding that hacking is successful.
                    // Note that "hasHackedResult()" uses "null != mYtr".
                    ytr = null;
                    err = Err.PARSE_HTML;
                    break;
                }

                // NOTE
                // HACK youtube protocol!
                // Do dummy 'GET' request with generate_204 url.
                content = mLoader.getHttpContent(Uri.parse(ytr.generate_204_url));
                if (HttpUtils.SC_NO_CONTENT != content.stcode) {
                    if (HttpUtils.SC_OK == content.stcode)
                        // This is unexpected! One of following reasons may lead to this state
                        // - Youtube server doing something bad.
                        // - Youtube's video request protocol is changed.
                        // - Something unexpected.
                        err = Err.PARSE_HTML;
                    else
                        err = Err.IO_NET;
                    // 'mYtr' is NOT available in this case!
                    ytr = null;
                    break;
                }
                // Now all are ready to download!
            } while (false);
        } catch (NetLoader.LocalException e){
            err = map(e.error());
        } catch (LocalException e) {
            err = e.error();
        }

        if (Err.NO_ERR != err)
            mLoader.close();

        mYtr = ytr;
        return err;
    }

    private void
    postExecute(Err result) {
        if (mCancelled) {
            mLoader.close();

            if (null != mListener)
                mListener.onHackCancelled(this, mYtvid, mUser);
        } else {
            if (Err.NO_ERR == result && hasHackedResult())
                RTState.get().cachingYtHacker(this);

            if (null != mListener)
                mListener.onPostHack(this, result, mLoader, mYtvid, mUser);
        }
    }

    /**
     * NOTE
     * This is based on experimental result.
     * There is no official API regarding getting thumbnail via Youtube video id.
     * So, it is NOT 100% guaranteed that correct url is returned.
     * But, I'm strongly sure that return value is valid and correct based on my experience.
     * That's the reason why this function is member of 'YTHacker'(Not YTSearchHeler).
     * @param ytvid Youtube video id
     */
    public static String
    getYtVideoThumbnailUrl(String ytvid) {
        return "http://i.ytimg.com/vi/" + ytvid + "/default.jpg";
    }

    public static String
    getYtVideoPageUrl(String ytvid) {
        return "http://" + getYtHost() + "/" + getYtUri(ytvid);
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

    public YTHacker(String ytvid, Object user,
                    YtHackListener hackListener) {
        // loader should "opened loader"
        mYtvid = ytvid;
        mUser = user;
        mListener = hackListener;
        mBgTask = new AsyncTask<Void, Void, Err>() {
            @Override
            protected void
            onPreExecute() {
                preExecute();
            }

            @Override
            protected Err
            doInBackground(Void... dummy) {
                return doMainWork();
            }

            @Override
            protected void
            onPostExecute(Err result) {
                postExecute(result);
            }

            @Override
            public void
            onCancelled() {
                if (null != mListener)
                    mListener.onHackCancelled(YTHacker.this, mYtvid, mUser);
            }
        };
    }

    public NetLoader
    getNetLoader() {
        return mLoader;
    }

    public boolean
    hasHackedResult() {
        return null != mYtr;
    }

    public String
    getYtvid() {
        return mYtvid;
    }

    public long
    getHackTimeStamp() {
        eAssert(hasHackedResult());
        return mYtr.tmstamp;
    }

    /**
     *
     * @param quality Quality value. See YTQUALITY_SCORE_XXX
     * @param exact true : exact matching is required.
     *              false : best-fit is found.
     */
    public YtVideo
    getVideo(int quality, boolean exact) {
        eAssert(0 <= quality && quality <= 100);
       if (null == mYtr)
            return null;

        // Select video that has closest quality score
        YtVideoElem ve = null;
        int curgap = -1;
        for (YtVideoElem e : mYtr.vids) {
            if (YTQSCORE_INVALID != e.qscore) {
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
            return new YtVideo(ve.url, ve.type);
    }

    public Err
    start() {
        preExecute();
        Err result = doMainWork();
        postExecute(result);
        return result;
    }

    public void
    startAsync() {
        mBgTask.execute();
    }

    public void
    forceCancel() {
        mCancelled = true;
        mLoader.close();
        mBgTask.cancel(true);
    }
}
