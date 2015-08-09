import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;


public class YTHackTest {
    //
    // This is main class for HACKING Youtube protocol.
    //
    private static final boolean DBG = true;

    public static final String HTTP_UASTRING
        = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36";

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
    private YtVideoHtmlResult mYtr = null;

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
        public final boolean video; // is video type?
        YtVideo(String url, boolean video) {
            this.url = url;
            this.video = video;
        }
    }

    private static class P {
    	static void
    	w(String s) {
    		System.out.println(s);
    	}

    	static void
    	v(String s) {
    		System.out.println(s);
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

    public static class ElemType {
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

    public static class ElemSize {
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

    public static class YtVideoElem {
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
                assert(false);
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

    public static class YtVideoHtmlResult {
        long tmstamp = 0; // System time in milli.
        boolean playable = true; // video is playable on specified 'UA String'
        String generate_204_url = ""; // url including generate 204
        YtVideoElem[] vids = new YtVideoElem[0];
    }

    private static String
    getYtUri(String ytvid) {
        assert(YTVID_LENGTH == ytvid.length());
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

    //=======================================================================
    //
    //
    //
    // ======================================================================
    public static class HttpRespContent {
        public int stcode; // status code
        public InputStream stream;
        public String type;
        HttpRespContent(int aStcode, InputStream aStream, String aType) {
            stcode = aStcode;
            stream = aStream;
            type = aType;
        }
    }

    private static HttpClient
    newHttpClient(@SuppressWarnings("unused") String proxyHost,
                  @SuppressWarnings("unused") int port,
                  String uastring) {
        // TODO Proxy is NOT supported yet. These are ignored.
    	// to test on proxy
    	HttpHost proxy = new HttpHost("168.219.61.252", 8080);
        HttpClient hc = new DefaultHttpClient();
        hc.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        HttpParams params = hc.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 500);
        HttpConnectionParams.setSoTimeout(params, 500);
        if (null != uastring)
            HttpProtocolParams.setUserAgent(hc.getParams(), uastring);
        params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);

        // Set scheme registry
        SchemeRegistry registry = hc.getConnectionManager().getSchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        return hc;
    }

    public HttpRespContent
    getHttpContent(HttpClient client, URI uri)
        throws LocalException  {
        int retry = 3;
        while (0 < retry--) {
            try {
                HttpGet httpGet = new HttpGet(uri.toString());
                if (DBG) P.v("executing request: " + httpGet.getRequestLine().toString());
                //logI("uri: " + httpGet.getURI().toString());
                //logI("target: " + httpTarget.getHostName());
                HttpResponse httpResp = client.execute(httpGet);
                if (DBG) P.v("NetLoader HTTP response status line : " + httpResp.getStatusLine().toString());

                int statusCode = httpResp.getStatusLine().getStatusCode();

                InputStream contentStream = null;
                String contentType = null;
                if (204 != statusCode) {
                    HttpEntity httpEntity = httpResp.getEntity();

                    if (null == httpEntity) {
                        if (DBG) P.w("Unexpected NULL entity");
                        assert(false);
                    }
                    contentStream = httpEntity.getContent();
                    try {
                        contentType = httpResp.getFirstHeader("Content-Type").getValue().toLowerCase();
                    } catch (NullPointerException e) {
                        // Unexpected response data.
                        if (DBG) P.v("NetLoader IOException : " + e.getMessage());
                        throw new LocalException(Err.IO_NET);
                    }
                }

                switch (statusCode) {
                case 200:
                case 204:
                    // This is expected response. let's move forward
                    break;

                default:
                    // Unexpected response
                    if (DBG) {
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(contentStream));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            P.w(line);
                        }
                        reader.close();
                        P.w("Unexpected Response  status code : " + httpResp.getStatusLine().getStatusCode());

                    }
                }

                return new HttpRespContent(statusCode, contentStream, contentType);
            } catch (ClientProtocolException e) {
                if (DBG) P.v("NetLoader ClientProtocolException : " + e.getMessage());
                throw new LocalException(Err.UNKNOWN);
            } catch (IllegalArgumentException e) {
                if (DBG) P.v("Illegal Argument Exception : " + e.getMessage() + "\n"
                     + "URI : " + uri.toString());
                throw new LocalException(Err.IO_NET);
            } catch (UnknownHostException e) {
                if (DBG) P.v("NetLoader UnknownHostException : Maybe timeout?" + e.getMessage());

                if (0 >= retry)
                    throw new LocalException(Err.IO_NET);

                // continue next retry after some time.
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    throw new LocalException(Err.IO_NET);
                }
            } catch (IOException e) {
                if (DBG) P.v("NetLoader IOException : " + e.getMessage());
                throw new LocalException(Err.IO_NET);
            } catch (Exception e) {
                if (DBG) P.v("NetLoader IllegalStateException : " + e.getMessage());
                throw new LocalException(Err.UNKNOWN);
            }
        }
        assert(false);
        return null;
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
    verifyYtVideoHtmlResult(YtVideoHtmlResult ytr) {
        return ytr.vids.length > 0
               && null != ytr.generate_204_url
               && !ytr.generate_204_url.isEmpty();
    }

    private static YtVideoHtmlResult
    parseYtVideoHtml(BufferedReader brdr)
            throws LocalException {
    	String htmlText = "";
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

            htmlText += line + "\n";

            if (line.contains("\"player-unavailable\"")) {
            	// This is unavailable video on the specified UA string
            	result.playable = false;
            	if (DBG) P.v("This video is NOT playable");
            	break;
            } else if (line.contains("/generate_204")) {
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
        if (DBG) P.v(htmlText);
        result.tmstamp = System.currentTimeMillis();
        return result;
    }

    public Err
    startHack() {
        Err err = Err.NO_ERR;
        YtVideoHtmlResult ytr = null;
        HttpClient hc = newHttpClient("", 0, HTTP_UASTRING);
        try {
            do {
                // Read and parse html web page of video.
            	HttpRespContent content = getHttpContent(hc, URI.create(getYtVideoPageUrl(mYtvid)));
                if (200 != content.stcode) {
                    err = Err.IO_NET;
                    break;
                }
                assert(content.type.toLowerCase().startsWith("text/html"));
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
                content = getHttpContent(hc, URI.create(ytr.generate_204_url));
                if (204 != content.stcode) {
                    if (200 == content.stcode)
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
                // This is good moment to calculate quality score of each available elements.
                for (YtVideoElem ve : ytr.vids)
                    ve.qscore = getPolicyQualityScore(ve);

            } while (false);
        } catch (LocalException e) {
            err = e.error();
        }

        mYtr = ytr;
        return err;
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

    public YTHackTest(String ytvid) {
        // loader should "opened loader"
        mYtvid = ytvid;
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
        assert(hasHackedResult());
        return mYtr.tmstamp;
    }

    public YtVideoElem[]
    getVideoElems() {
    	return mYtr.vids;
    }

    /**
     *
     * @param quality Quality value. See YTQUALITY_SCORE_XXX
     * @param exact true : exact matching is required.
     *              false : best-fit is found.
     */
    public YtVideo
    getVideo(int quality, boolean exact) {
        assert(0 <= quality && quality <= 100);
        if (null == mYtr)
            return null;

        // Select video that has closest quality score
        YtVideoElem ve = null;
        int curgap = -1;
        for (YtVideoElem e : mYtr.vids) {
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
}
