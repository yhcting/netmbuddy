package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;


public class YTDownloader {
    // See youtube api documentation.
    private static final int    YTVID_LENGTH = 11;

    private static final int    YTQSCORE_INVALID        = -1;

    // TODO
    // This is from experimental result and hacking script code.
    // Need to verify below tags again!
    private static final int    YTSTREAMTAG_INVALID     = -1;

    private static final int    YTSTREAMTAG_MPEG_1080p  = 37;
    private static final int    YTSTREAMTAG_MPEG_720p   = 22;
    private static final int    YTSTREAMTAG_MPEG_480p   = 35;
    private static final int    YTSTREAMTAG_MPEG_360p   = 18;

    private static final int    YTSTREAMTAG_3GPP_240p   = 36;
    private static final int    YTSTREAMTAG_3GPP_144p   = 17;

    private static final int    YTSTREAMTAG_WEBM_1080p  = 46;
    private static final int    YTSTREAMTAG_WEBM_720p   = 45;
    private static final int    YTSTREAMTAG_WEBM_480p   = 44;
    private static final int    YTSTREAMTAG_WEBM_360p   = 43;

    private static final int    YTSTREAMTAG_FLV_360p    = 34;
    private static final int    YTSTREAMTAG_FLV_240p    = 5;

    private static final Pattern    sYtUrlStreamMapPattern
        = Pattern.compile(".*\"url_encoded_fmt_stream_map\": \"([^\"]+)\".*");

    private BGHandler mBgHandler      = null;

    private static class YtVideoElem {
        String  url         = "";
        String  tag         = "";
        String  type        = "";
        String  quality     = "";
        // Quality score of this video
        // This value is guesses from 'tag'
        // -1 means "invalid, so, DO NOT use this video".
        int     qscore      = YTQSCORE_INVALID;

        private YtVideoElem() {} // DO NOT CREATE DIRECTLY

        private static int
        getQuailityScore(String tag) {
            int v = -1;
            try {
                v = Integer.parseInt(tag);
            } catch (NumberFormatException e) {
                eAssert(false);
            }

            switch (v) {
            case YTSTREAMTAG_MPEG_1080p:
                return 100;
            case YTSTREAMTAG_MPEG_720p:
                return 70;
            case YTSTREAMTAG_MPEG_480p:
                return 50;
            case YTSTREAMTAG_MPEG_360p:
                return 30;
            case YTSTREAMTAG_3GPP_240p:
                return 20;
            case YTSTREAMTAG_3GPP_144p:
                return 10;
            default:
                // TODO
                // Check it!
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
            }
            eAssert(!ve.url.isEmpty()
                    && !ve.tag.isEmpty()
                    && !ve.type.isEmpty()
                    && !ve.quality.isEmpty());
            ve.qscore = getQuailityScore(ve.tag);
            return ve;
        }

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
        YtVideoElem[]  vids;
    }

    private class BGThread extends HandlerThread {
        BGThread() {
            super("YTDownloader.BGThread",Process.THREAD_PRIORITY_BACKGROUND);
        }
        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }
    }

    private class BGHandler extends Handler {
        BGHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void
        handleMessage(Message msg) {
            switch (msg.what) {
            }
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

    private static YtVideoHtmlResult
    parseYtVideoHtml(BufferedReader brdr)
            throws YTMPException {
        YtVideoHtmlResult result = new YtVideoHtmlResult();
        String line = "";
        while (null != line) {
            try {
                line = brdr.readLine();
            } catch (IOException e) {
                throw new YTMPException(Err.IO_UNKNOWN);
            }

            if (null == line)
                break;

            if (line.contains("\"url_encoded_fmt_stream_map\":")) {
                Matcher m = sYtUrlStreamMapPattern.matcher(line);
                if (!m.matches())
                    eAssert(false);

                line = m.group(1);
                String[] vidElemUrls = line.split(",");
                result.vids = new YtVideoElem[vidElemUrls.length];
                int i = 0;
                for (String s : vidElemUrls)
                    result.vids[i++] = YtVideoElem.parse(s);
            }
        }
        return result;
    }

    private Err
    doDownload(String proxy, String ytvid, File outf) {
        NetLoader loader = new NetLoader().open(proxy);
        NetLoader.HttpRespContent content;
        try {
            // Read and parse html web page of video.
            content = loader.getHttpContent(new URI("http://" + getYtHost() + "/" + getYtUri(ytvid)), true);
            eAssert(content.type.toLowerCase().startsWith("text/html"));
            YtVideoHtmlResult ytr = parseYtVideoHtml(new BufferedReader(new InputStreamReader(content.stream)));
            loader.close();

            // Do real download - poorest quaility for testing!
            eAssert(ytr.vids.length > 0);
            YtVideoElem ve = null;
            for (YtVideoElem e : ytr.vids) {
                if (YTQSCORE_INVALID != e.qscore) {
                    if (null == ve)
                        ve = e;
                    else if (e.qscore < ve.qscore)
                        ve = e;
                }
            }
            eAssert(null != ve);

            loader.open(proxy);
            content = loader.getHttpContent(new URI(ve.url), true);
            try {
                File f = new File("/sdcard/ytdntest.mp4");
    //            Long iBytesMax = Long.parseLong(mResponse.getFirstHeader("Content-Length").getValue());
                FileOutputStream fos = new FileOutputStream(f);
                byte[] bytes = new byte[4*4096];
                Integer iBytesRead = 1;

                while (iBytesRead>0) {
                    iBytesRead = content.stream.read(bytes);
                    try {fos.write(bytes,0,iBytesRead);} catch (IndexOutOfBoundsException ioob) {}
                } // while
                fos.close();
            } catch (Exception e) {
                eAssert(false);
            }
            loader.close();
        } catch (URISyntaxException e) {
            eAssert(false); //Should not happened
        } catch (YTMPException e) {
            return e.getError();
        }
        return Err.NO_ERR;
    }

    public YTDownloader() {
    }

    /**
     *
     * @param ytvid
     *   11-character-long youtube video id
     */
    public void
    download(String proxy, String ytvid, File outf) {
        /*
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                doDownload("", "dHeqQu8a1h0", new File("/sdcard/testytdn.mp4"));
            }
        });
        */
    }

    public void
    open() {
        HandlerThread hThread = new BGThread();
        hThread.start();
        mBgHandler = new BGHandler(hThread.getLooper());
    }

    public void
    close() {
        // TODO
        // Stop running thread!
        // Need to check that below code works as expected perfectly.
        // "interrupting thread" is quite annoying and unpredictable job!
        if (null != mBgHandler) {
            mBgHandler.getLooper().getThread().interrupt();
        }
    }
}
