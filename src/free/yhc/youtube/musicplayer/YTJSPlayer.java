package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logD;
import static free.yhc.youtube.musicplayer.model.Utils.logI;
import static free.yhc.youtube.musicplayer.model.Utils.logW;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.DB.ColMusic;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.NanoHTTPD;
import free.yhc.youtube.musicplayer.model.Utils;

// See Youtube player Javascript API document
public class YTJSPlayer {
    private static final String WLTAG = "YTJSPlayer";
    private static final int    WEBSERVER_PORT  = 8999;
    private static final String YTPLAYER_SCRIPT = "ytplayer.html";


    private static final int YTPSTATE_UNSTARTED     = -1;
    private static final int YTPSTATE_ENDED         = 0;
    private static final int YTPSTATE_PLAYING       = 1;
    private static final int YTPSTATE_PAUSED        = 2;
    private static final int YTPSTATE_BUFFERING     = 3;
    private static final int YTPSTATE_VIDEO_CUED    = 5;

    private static final Comparator<NrElem> sNrElemComparator = new Comparator<NrElem>() {
        @Override
        public int compare(NrElem o1, NrElem o2) {
            if (o1.n > o2.n)
                return 1;
            else if (o1.n < o2.n)
                return -1;
            else
                return 0;
        }
    };
    private static final Comparator<Music> sMusicTitleComparator = new Comparator<Music>() {
        @Override
        public int compare(Music o1, Music o2) {
            return o1.title.compareTo(o2.title);
        }
    };

    private static YTJSPlayer sInstance = null;


    private final Resources     mRes        = Utils.getAppContext().getResources();
    private final DB            mDb         = DB.get();

    // ------------------------------------------------------------------------
    // Final Runnables
    // ------------------------------------------------------------------------
    private final UpdateProgress        mUpdateProg = new UpdateProgress();

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------
    private WakeLock            mWl         = null;
    private WifiLock            mWfl        = null;

    private WebView             mWv         = null; // WebView instance.
    private State               mWvS        = State.INVALID; // state of mVv;
    private VideoView           mVv         = null;

    // ------------------------------------------------------------------------
    // UI Control.
    // ------------------------------------------------------------------------
    private Context             mVContext   = null;
    private LinearLayout        mPlayerv    = null;
    private ProgressBar         mProgbar    = null;

    // ------------------------------------------------------------------------
    // MediaPlayer Runtime Status
    // ------------------------------------------------------------------------
    private Music[]             mMusics     = null;
    private int                 mMusici     = -1;


    // see "http://developer.android.com/reference/android/media/MediaPlayer.html"
    public enum State {
        INVALID,
        IDLE,
        INITIALIZED,
        PREPARING,
        PREPARED,
        STARTED,
        STOPPED,
        PAUSED,
        PLAYBACK_COMPLETED,
        ERROR
    }

    public static class Music {
        public String   title;
        public String   url;
        public Music(String aUrl, String aTitle) {
            url = aUrl;
            title = aTitle;
        }
    }

    private static class NrElem {
        public int      n;
        public Object   tag;
        NrElem(int aN, Object aTag) {
            n = aN;
            tag = aTag;
        }
    }

    private class UpdateProgress implements Runnable {
        private ProgressBar progbar = null;
        private int         lastProgress = -1;

        void start(ProgressBar aProgbar) {
            end();
            progbar = aProgbar;
            lastProgress = -1;
            run();
        }

        void end() {
            progbar = null;
            lastProgress = -1;
            Utils.getUiHandler().removeCallbacks(this);
        }

        @Override
        public void run() {
            if (null != progbar && progbar == mProgbar) {
                int duration = wvVvGetDuration();
                int curProgress = (duration > 0)? wvVvGetCurrentPosition() * 100 / wvVvGetDuration()
                                                : 0;
                if (curProgress > lastProgress)
                    mProgbar.setProgress(curProgress);

                lastProgress = curProgress;
                Utils.getUiHandler().postDelayed(this, 1000);
            }
        }
    }

    private class WVClient extends WebViewClient {
        private VideoView
        findVideoView(ViewGroup v) {
            for (int i = 0; i < v.getChildCount(); i++) {
                View cv = v.getChildAt(i);
                if (cv instanceof VideoView)
                    return (VideoView)cv;
                else if (cv instanceof ViewGroup)
                    return findVideoView((ViewGroup)cv);
            }
            return null;
        }

        @Override
        public boolean
        shouldOverrideUrlLoading(WebView wView, String url) {
            logI("WebView : shouldOverrideUrlLoading : " + url);
            return true;
        }

        @Override
        public void
        onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            logI("WebView : onPageStarted : " + url);
        }

        @Override
        public void
        onLoadResource(WebView view, String url) {
            logI("WebView : onLoadResource : " + url);
            if (url.startsWith("http://s.youtube.com/s")) {
                logI("WebView : onLoadResource(Youtube contents?) : " + url);
            }
        }

        @Override
        public void
        onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            logI("WebView : onPageFinished : " + url);
        }

        @Override
        public void
        onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            logI("URL : " + failingUrl + "\nOh no! " + description);
        }
    }

    private class WCClient extends WebChromeClient {
        @Override
        public View
        getVideoLoadingProgressView() {
            logI("WebView : getVideoLoadingProgressView");
            return super.getVideoLoadingProgressView();
        }

        @Override
        public void
        onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            logI("WebView : onShowCustomView");
        }

        @Override
        public void
        onHideCustomView() {
            super.onHideCustomView();
            logI("WebView : onHideCustomView");
        }

        @Override
        public boolean
        onJsAlert(WebView view, String url, String message, JsResult result) {
            logI("JSMsg : " + message);
            return true;
        }
    }

    private void
    setWebSettings(WebView wv) {
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setSavePassword(true);
        ws.setPluginsEnabled(true);

        ws.setUserAgent(1);
    }


    // ========================================================================
    //
    //
    //
    // ========================================================================
    private void
    acquireLocks() {
        eAssert(null == mWl && null == mWfl);
        mWl = ((PowerManager)Utils.getAppContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WLTAG);
        // Playing youtube requires high performance wifi for high quality media play.
        mWfl = ((WifiManager)Utils.getAppContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WLTAG);
        mWl.acquire();
        mWfl.acquire();
    }

    private void
    releaseLocks() {
        if (null != mWl)
                mWl.release();

        if (null != mWfl)
            mWfl.release();

        mWl = null;
        mWfl = null;
    }


    // ========================================================================
    //
    // WebView Interfaces
    //
    // ========================================================================

    private void
    wvNew() {
        if (null != mWv)
            return;

        wvSetState(State.INVALID);
        WebView wv = (WebView)mPlayerv.findViewById(R.id.webview);
        wv.setWebViewClient(new WVClient());
        wv.setWebChromeClient(new WCClient());
        setWebSettings(wv);

        mWv = wv;
        wvSetState(State.INITIALIZED);
    }

    private void
    wvLoad(String url) {
        wvSetState(State.PREPARING);
        mWv.loadUrl(url + "&autoplay=1");
    }

    private boolean
    wvIsAvailable() {
        return null != mWv && State.INVALID != wvGetState();
    }

    private void
    wvSetState(State newState) {
        logD("MusicPlayer : State : " + mWvS.name() + " => " + newState.name());
        State old = mWvS;
        mWvS = newState;
        onWvStateChanged(old, newState);
    }

    private State
    wvGetState() {
        return mWvS;
    }

    private void
    wvInitVideoView(VideoView vv) {
        mVv = vv;
        eAssert(null != mPlayerv);
        mVv.setMediaController((MediaController)mPlayerv.findViewById(R.id.music_player_controller));
    }


    private void
    wvDestroy() {
        if (null != mWv)
            mWv.destroy();

        wvSetState(State.INVALID);

    }

    // ------------------------------------------------------------------------
    // WebView VideoView interfaces
    // ------------------------------------------------------------------------
    public int
    wvVvGetCurrentPosition() {
        switch (wvGetState()) {
        case ERROR:
            return 0;
        }
        return mVv.getCurrentPosition();
    }

    public int
    wvVvGetDuration() {
        switch(wvGetState()) {
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
            return mVv.getDuration();
        }
        return 0;
    }

    public boolean
    wvVvIsPlaying() {
        //logD("VideoView - isPlaying");
        return mVv.isPlaying();
    }

    public void
    wvVvPause() {
        logD("VideoView - pause");
        mVv.pause();
        wvSetState(State.PAUSED);
    }

    public void
    wvVvSeekTo(int pos) {
        logD("VideoView - seekTo");
        mVv.seekTo(pos);
    }

    public void
    wvVvStart() {
        logD("VideoView - start");
        mVv.start();
        wvSetState(State.STARTED);
    }

    public void
    wvVvStop() {
        logD("VideoView - stop");
        mVv.stopPlayback();
    }

    // ========================================================================
    //
    // General Control
    //
    // ========================================================================
    private void
    onWvStateChanged(State from, State to) {
        if (from == to)
            return;

        configurePlayerViewAll();
    }


    private void
    playMusicAsync() {
        wvDestroy();

        if (mMusici < 0 || mMusici >= mMusics.length) {
            playDone();
            return;
        }

        wvNew();
        wvLoad(mMusics[mMusici].url);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                int n = mDb.updateMusic(ColMusic.URL, mMusics[mMusici].url,
                                        ColMusic.TIME_PLAYED, System.currentTimeMillis());
                logD("MusicPlayer : TIME_PLAYED updated : " + n);
            }
        });
    }

    private void
    playDone() {
        logD("VideoView - playDone");
        setPlayerViewTitle(mRes.getText(R.string.msg_playing_done));
        releaseLocks();
    }

    // ========================================================================
    //
    // Player View Handling
    //
    // ========================================================================

    private void
    setPlayerViewTitle(CharSequence title) {
        if (null == mPlayerv || null == title)
            return;

        TextView tv = (TextView)mPlayerv.findViewById(R.id.music_player_title);
        tv.setText(title);
    }

    private void
    configurePlayerViewTitle() {
        if (null == mPlayerv)
            return;

        CharSequence musicTitle = null;
        if (null != mMusics
                && 0 <= mMusici
                && mMusici < mMusics.length) {
            musicTitle = mMusics[mMusici].title;
        }

        switch (wvGetState()) {
        case ERROR:
            setPlayerViewTitle(mRes.getText(R.string.msg_mplayer_err_unknown));
            break;

        case PAUSED:
        case STARTED:
            eAssert(null != musicTitle);
            if (null != musicTitle)
                setPlayerViewTitle(musicTitle);
            break;

        default:
            setPlayerViewTitle(mRes.getText(R.string.msg_preparing_mplayer));
        }
    }

    private void
    configurePlayerViewController() {
        if (null == mPlayerv)
            return;

        switch (wvGetState()) {
        case PAUSED:
        case STARTED:
            mPlayerv.findViewById(R.id.music_player_controller).setVisibility(View.VISIBLE);
            break;

        default:
            mPlayerv.findViewById(R.id.music_player_controller).setVisibility(View.GONE);
        }
    }

    private void
    configurePlayerViewProgressBar() {
        if (null == mProgbar)
            return;

        switch (wvGetState()) {
        case STARTED:
            mUpdateProg.start(mProgbar);
            break;

        case PAUSED:
            mUpdateProg.end();
            break;

        default:
            mUpdateProg.end();
            mProgbar.setProgress(0);
        }
    }

    private void
    configurePlayerViewAll() {
        configurePlayerViewTitle();
        configurePlayerViewProgressBar();
        configurePlayerViewController();
    }


    private void
    initPlayerView() {
        configurePlayerViewAll();
    }

    // ========================================================================
    //
    // Java script player interface
    //
    // ========================================================================
    public void
    jsLog(String msg) {
        logI(msg);
    }

    public void
    jsOnPlayerStateChanged(int state) {
        logI("OnPlayerStateChanged : " + state);
    }

    public void
    jsOnPlayerError(int errCode) {
        logI("OnPlayerError : " + errCode);
    }

    public void
    jsOnTryLoadPlayer() {
        logI("OnTryLoadPlayer");
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void run() {
                mWv.requestFocus();
                mWv.invalidate();
            }
        });
    }

    // ========================================================================
    //
    // Public interface
    //
    // ========================================================================
    private YTJSPlayer() {
    }

    public static YTJSPlayer
    get() {
        if (null == sInstance)
            sInstance = new YTJSPlayer();
        return sInstance;
    }

    @Override
    protected void
    finalize() {
        wvDestroy();
    }

    public Err
    init() {
        /*
        File fScript = new File(Utils.getAppContext().getFilesDir().getAbsolutePath()
                                + "/" + YTPLAYER_SCRIPT);
        if (!fScript.exists())
            Utils.copyAssetFile(YTPLAYER_SCRIPT);
        */
        new File(Utils.getAppContext().getFilesDir().getAbsolutePath() + "/" + YTPLAYER_SCRIPT).delete();
        Utils.copyAssetFile(YTPLAYER_SCRIPT);

        // NOTE
        // script for chromeless player should be loaded from webserver
        // (See youtube documentation for details.)
        // Start simple webserver
        try {
            new NanoHTTPD(WEBSERVER_PORT, Utils.getAppContext().getFilesDir());
        } catch (IOException e) {
            logI("Fail to start Nanohttpd");
        }
        return Err.NO_ERR;
    }

    public Err
    setController(Context context, View playerv) {
        if (context == mVContext && mPlayerv == playerv)
            // controller is already set for this context.
            // So, nothing to do. just return!
            return Err.NO_ERR;

        mVContext = context;
        mPlayerv = (LinearLayout)playerv;
        mProgbar = null;

        if (null == mPlayerv)
            return Err.NO_ERR;

        eAssert(null != mPlayerv.findViewById(R.id.music_player_layout_magic_id));
        mProgbar = (ProgressBar)mPlayerv.findViewById(R.id.music_player_progressbar);
        initPlayerView();

        return Err.NO_ERR;
    }

    public void
    unsetController(Context context) {
        if (null != mVContext && context != mVContext)
            logW("MusicPlayer : Unset Controller at different context...");

        mProgbar = null;
        mPlayerv = null;
        mVContext = null;

    }

    public void
    playTest() {
        wvNew();
        mWv.addJavascriptInterface(this, "Android");
        mWv.loadUrl("http://127.0.0.1:" + WEBSERVER_PORT + "/" + YTPLAYER_SCRIPT);
    }


    /**
     * Should be called after {@link #setController(Context, View)}
     * @param ms
     */
    public void
    startMusicsAsync(Music[] ms) {
        playTest();
        /*
        eAssert(Utils.isUiThread());
        eAssert(null != mPlayerv);

        releaseLocks();
        acquireLocks();

        mMusics = ms;
        mMusici = 0;

        playMusicAsync();
        */
    }



    /**
     * This may takes some time. it depends on size of cursor.
     * @param c
     *   Musics from this cursor will be sorted order by it's title.
     *   So, this function doesn't require ordered cursor.
     *   <This is for performance reason!>
     * @param coliTitle
     * @param coliUrl
     * @param shuffle
     * @return
     */
    private Music[]
    getMusics(Cursor c, int coliTitle, int coliUrl, boolean shuffle) {
        if (!c.moveToFirst())
            return new Music[0];

        Music[] ms = new Music[c.getCount()];
        int i = 0;
        if (!shuffle) {
            do {
                ms[i++] = new Music(c.getString(coliUrl), c.getString(coliTitle));
            } while (c.moveToNext());
            Arrays.sort(ms, sMusicTitleComparator);
        } else {
            // This is shuffled case!
            Random r = new Random(System.currentTimeMillis());
            NrElem[] nes = new NrElem[c.getCount()];
            do {
                nes[i++] = new NrElem(r.nextInt(),
                                      new Music(c.getString(coliUrl), c.getString(coliTitle)));
            } while (c.moveToNext());
            Arrays.sort(nes, sNrElemComparator);
            for (i = 0; i < nes.length; i++)
                ms[i] = (Music)nes[i].tag;
        }
        return ms;
    }

    /**
     *
     * @param c
     *   closing cursor is in charge of this function.
     * @param coliUrl
     * @param coliTitle
     * @param shuffle
     */
    public void
    startMusicsAsync(final Cursor c, final int coliUrl, final int coliTitle, final boolean shuffle) {
        playTest();
        /*
        eAssert(Utils.isUiThread());
        eAssert(null != mPlayerv);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final Music[] ms = getMusics(c, coliTitle, coliUrl, shuffle);
                Utils.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        startMusicsAsync(ms);
                    }
                });
                c.close();
            }
        });
        */
    }

    // ============================================================================
    //
    //
    //
    // ============================================================================

    public boolean
    isMusicPlaying() {
        // TODO implement this.
        return false;
    }


}
