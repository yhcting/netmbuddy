package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logD;

import java.io.IOException;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class MusicPlayer implements
MediaPlayer.OnBufferingUpdateListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener,
MediaPlayer.OnInfoListener,
MediaPlayer.OnVideoSizeChangedListener,
MediaPlayer.OnSeekCompleteListener {
    private static final String WLTAG = "MusicPlayer";
    private static final int    NO_BUFFERING    = -1;
    private static final int    RETRY_HANG_ON_BUFFERING     = 3;
    private static final int    TIMEOUT_HANG_ON_BUFFERING   = 5 * 1000;


    private static MusicPlayer instance = null;

    private final Resources     mRes        = Utils.getAppContext().getResources();

    // Runnables
    private final UpdateProgress        mUpdateProg             = new UpdateProgress();
    private final RetryHangOnBuffering  mRetryHangOnBuffering   = new RetryHangOnBuffering();

    private WakeLock            mWl          = null;
    private WifiLock            mWfl         = null;

    private MediaPlayer         mMp         = null;
    private State               mMpS        = State.INVALID; // state of mMp;

    // UI Control.
    private Context             mVContext   = null;
    private View                mPlayerv    = null;
    private ProgressBar         mProgbar    = null;

    // MediaPlayer Runtime Status
    private int                 mBuffering  = NO_BUFFERING;

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
        END,
        ERROR
    }

    public static class Music {
        public String   title;
        public Uri      uri;
        public Music(Uri aUri, String aTitle) {
            uri = aUri;
            title = aTitle;
        }
    }

    private class UpdateProgress implements Runnable {
        private ProgressBar progbar = null;

        void start(ProgressBar aProgbar) {
            end();
            progbar = aProgbar;
            run();
        }

        void end() {
            progbar = null;
            Utils.getUiHandler().removeCallbacks(this);
        }

        @Override
        public void run() {
            if (null != progbar && progbar == mProgbar) {
                mProgbar.setProgress(mpGetCurrentPosition() * 100 / mpGetDuration());
                Utils.getUiHandler().postDelayed(this, 1000);
            }
        }
    }

    private class RetryHangOnBuffering implements Runnable {
        private int         retry       = 0;
        private MediaPlayer mp          = null;

        void start(MediaPlayer aMp) {
            logD("MPlayer : HangOnBuffering - start");
            end();
            retry = RETRY_HANG_ON_BUFFERING;
            mp = aMp;
            Utils.getUiHandler().postDelayed(this, TIMEOUT_HANG_ON_BUFFERING);
        }

        void end() {
            logD("MPlayer : HangOnBuffering - end");
            mp = null;
            Utils.getUiHandler().removeCallbacks(this);
            retry = 0;
        }

        boolean isActive() {
            return null != mp && retry > 0;
        }

        @Override
        public void run() {
            if (retry-- > 0 && null != mp && mp == mpGet()) {
                if (!isMediaPlayerAvailable()) {
                    end();
                    return;
                }

                logD("MusicPlayer : try to recover from HangOnBuffering ***");

                // Check it still preparing for recovery.
                if (State.PREPARING != mpGetState()) {
                    // Below code is fully hack of MusicPlayer's mechanism to recover from exceptional state.

                    // save current state of this module.
                    int retrysv = retry;

                    // fully restart music player from current music.
                    startMusicPlayerAsync();
                    // Now, state of this module is fully changed from before 'startMusicPlayerAysnc()' is called.
                    // So, state should be recovered properly.
                    retry = retrysv;


                    // NOTE!
                    // mpStop() should not be used because mpStop() leads to onStateChanged().
                    // But, this is exceptional retry.
                    // So, onStateChanged() should not be called for intermediate state (STOPPED).
                    // At STOPPED state, mRetryHangOnBuffering.end() is called.
                    // And this should be avoided!
                }

                Utils.getUiHandler().postDelayed(this, TIMEOUT_HANG_ON_BUFFERING);
            } else {
                logD("MPlayer : HangOnBuffering - Done");
                setPlayerViewTitle(mRes.getText(R.string.msg_mplayer_err_unknown));
            }
        }
    }

    private MusicPlayer() {
    }

    @Override
    protected void
    finalize() {
        mMp.release();
    }

    private void
    acquireLocks() {
        eAssert(null == mWl && null == mWfl);
        mWl = ((PowerManager)Utils.getAppContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WLTAG);
        // Playing youtube requires high performance wifi for high quality media play.
        mWfl = ((WifiManager)Utils.getAppContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WLTAG);
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
    // Media Player Interfaces
    //
    // ========================================================================
    private boolean
    isMediaPlayerAvailable() {
        return null != mMp
               && State.END != mpGetState()
               && State.INVALID != mpGetState();
    }
    private void
    mpSetState(State newState) {
        logD("MusicPlayer : State : " + mMpS.name() + " => " + newState.name());
        State old = mMpS;
        mMpS = newState;
        onMpStateChanged(old, newState);
    }

    private State
    mpGetState() {
        return mMpS;
    }

    private void
    initMediaPlayer(MediaPlayer mp) {
        // can be called only once.
        mp.setOnBufferingUpdateListener(this);
        mp.setOnCompletionListener(this);
        mp.setOnPreparedListener(this);
        mp.setScreenOnWhilePlaying(true);
        mp.setOnVideoSizeChangedListener(this);
        mp.setOnSeekCompleteListener(this);
        mp.setOnErrorListener(this);
        mp.setOnInfoListener(this);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private void
    mpNewInstance() {
        mMp = new MediaPlayer();
        mpSetState(State.INVALID);
        initMediaPlayer(mMp);
    }

    private MediaPlayer
    mpGet() {
        return mMp;
    }

    private void
    mpSetDataSource(Uri uri) throws IOException {
        mMp.setDataSource(Utils.getAppContext(), uri);
        mpSetState(State.INITIALIZED);
    }

    private void
    mpPrepareAsync() {
        mpSetState(State.PREPARING);
        mMp.prepareAsync();
    }

    private void
    mpRelease() {
        if (null == mMp)
            return;

        if (mMp.isPlaying())
            mMp.stop();

        mMp.release();
        mpSetState(State.END);
    }

    private void
    mpReset() {
        mMp.reset();
        mpSetState(State.IDLE);
    }

    public int
    mpGetCurrentPosition() {
        switch (mpGetState()) {
        case ERROR:
        case END:
            return 0;
        }
        return mMp.getCurrentPosition();
    }

    public int
    mpGetDuration() {
        switch(mpGetState()) {
        case PREPARED:
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
            return mMp.getDuration();
        }
        return 0;
    }

    public boolean
    mpIsPlaying() {
        //logD("MPlayer - isPlaying");
        return mMp.isPlaying();
    }

    public void
    mpPause() {
        logD("MPlayer - pause");
        mMp.pause();
        mpSetState(State.PAUSED);
    }

    public void
    mpSeekTo(int pos) {
        logD("MPlayer - seekTo");
        mMp.seekTo(pos);
    }

    public void
    mpStart() {
        logD("MPlayer - start");
        mMp.start();
        mpSetState(State.STARTED);
    }

    public void
    mpStop() {
        logD("MPlayer - stop");
        mMp.stop();
    }

    // ========================================================================
    //
    // General Control
    //
    // ========================================================================
    private void
    onMpStateChanged(State from, State to) {
        if (from == to)
            return;

        configurePlayerViewAll();
        switch (mpGetState()) {
        case STOPPED:
        case PLAYBACK_COMPLETED:
        case IDLE:
        case END:
        case ERROR:
            mRetryHangOnBuffering.end();
        }
    }


    private void
    playMusicAsync() {
        while (mMusici < mMusics.length) {
            try {
                // onPrepare
                mpSetDataSource(mMusics[mMusici].uri);
            } catch (IOException e) {
                mMusici++;
                continue;
            }
            mpPrepareAsync();
            return;
        }

        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void run() {
                playDone();
            }
        });
    }

    private void
    startMusicPlayerAsync() {
        mpRelease();
        mpNewInstance();
        mpReset();

        // Reset all mMp related runtime variable.
        mBuffering  = NO_BUFFERING;
        mRetryHangOnBuffering.end();

        playMusicAsync();
    }

    private void
    playDone() {
        logD("MPlayer - playDone");
        setPlayerViewTitle(mRes.getText(R.string.msg_playing_done));
        releaseLocks();
    }

    // ========================================================================
    //
    // Player View Handling
    //
    // ========================================================================

    /**
     *
     * @param title
     * @param buffering
     *   buffering percent. '-1' means no-bufferring.
     */
    private void
    setPlayerViewTitle(CharSequence title, int buffering) {
        if (null == mPlayerv || null == title)
            return;

        TextView tv = (TextView)mPlayerv.findViewById(R.id.title);
        if (buffering < 0)
            tv.setText(title);
        else
            tv.setText("(" + mRes.getText(R.string.buffering) + "-" + buffering + "%) " + title);
    }

    private void
    setPlayerViewTitle(CharSequence title) {
        setPlayerViewTitle(title, -1);
    }


    private void
    setPlayerViewPlayButton(int icon, boolean clickable) {
        if (null == mPlayerv)
            return;

        ImageView play = (ImageView)mPlayerv.findViewById(R.id.play);
        play.setImageResource(icon);
        play.setClickable(clickable);
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

        switch (mpGetState()) {
        case ERROR:
            setPlayerViewTitle(mRes.getText(R.string.msg_mplayer_err_unknown));
            break;

        case PAUSED:
        case STARTED:
            eAssert(null != musicTitle);
            if (null != musicTitle)
                setPlayerViewTitle(musicTitle, mBuffering);
            break;

        default:
            setPlayerViewTitle(mRes.getText(R.string.msg_preparing_mplayer));
        }
    }

    private void
    configurePlayerViewButton() {
        if (null == mPlayerv)
            return;

        switch (mpGetState()) {
        case PAUSED:
            setPlayerViewPlayButton(R.drawable.ic_media_play, true);
            break;

        case STARTED:
            setPlayerViewPlayButton(R.drawable.ic_media_pause, true);
            break;

        default:
            setPlayerViewPlayButton(R.drawable.ic_block, false);
        }
    }

    private void
    configurePlayerViewProgressBar() {
        if (null == mProgbar)
            return;

        switch (mpGetState()) {
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
        configurePlayerViewButton();
        configurePlayerViewProgressBar();
    }


    private void
    initPlayerView() {
        ImageView play = (ImageView)mPlayerv.findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mpGetState()) {
                case STARTED:
                    mpPause();
                    break;

                case PAUSED:
                    mpStart();
                    break;

                default:
                    if (null != mVContext)
                        UiUtils.showTextToast(mVContext, R.string.msg_mplayer_err_not_allowed);
                }
            }
        });
        mProgbar.setTag(false); // tag value means "keep progress or not."
        configurePlayerViewAll();
    }
    // ========================================================================
    //
    // Public interface
    //
    // ========================================================================
    public static MusicPlayer
    get() {
        if (null == instance)
            instance = new MusicPlayer();
        return instance;
    }

    public Err
    init() {
        return Err.NO_ERR;
    }

    public Err
    setController(Context context, View playerv) {
        if (context == mVContext && mPlayerv == playerv)
            // controller is already set for this context.
            // So, nothing to do. just return!
            return Err.NO_ERR;

        mVContext = context;
        mPlayerv = playerv;
        mProgbar = null;

        if (null == mPlayerv)
            return Err.NO_ERR;

        eAssert(null != mPlayerv.findViewById(R.id.music_player_layout_magic_id));
        mProgbar = (ProgressBar)mPlayerv.findViewById(R.id.progressbar);
        initPlayerView();

        return Err.NO_ERR;
    }

    public void
    unsetController(Context context) {
        eAssert(null == mVContext || context == mVContext);
        mProgbar = null;
        mPlayerv = null;
        mVContext = null;

    }

    /**
     * Should be called after {@link #setController(Context, View)}
     * @param ms
     */
    public void
    startMusicsAsync(Music[] ms) {
        eAssert(Utils.isUiThread());
        eAssert(null != mPlayerv);

        releaseLocks();
        acquireLocks();

        mMusics = ms;
        mMusici = 0;

        startMusicPlayerAsync();
    }

    public boolean
    isMusicPlaying() {
        return null != mMusics && mMusici < mMusics.length;
    }

    // ============================================================================
    //
    //
    //
    // ============================================================================



    // ============================================================================
    //
    // Override for "MediaPlayer.*"
    //
    // ============================================================================
    @Override
    public void
    onBufferingUpdate (MediaPlayer mp, int percent) {
        logD("MPlayer - onBufferingUpdate");
        if (mRetryHangOnBuffering.isActive())
            mRetryHangOnBuffering.end();
        mBuffering = percent;
        configurePlayerViewTitle();
    }

    @Override
    public void
    onCompletion(MediaPlayer mp) {
        logD("MPlayer - onCompletion");
        mpSetState(State.PLAYBACK_COMPLETED);
        mMusici++;
        mpReset();
        playMusicAsync();
    }

    @Override
    public void
    onPrepared(MediaPlayer mp) {
        logD("MPlayer - onPrepared");
        mpSetState(State.PREPARED);
        mpStart(); // auto start
    }

    @Override
    public void
    onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        logD("MPlayer - onVideoSizeChanged");
    }

    @Override
    public void
    onSeekComplete(MediaPlayer mp) {
        logD("MPlayer - onSeekComplete");
    }

    @Override
    public boolean
    onError(MediaPlayer mp, int what, int extra) {
        mpSetState(State.ERROR);
        logD("MPlayer - onError");
        switch (what) {
        case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
            break;

        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
            break;

        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
            break;

        default:
        }
        return false;
    }

    @Override
    public boolean
    onInfo(MediaPlayer mp, int what, int extra) {
        logD("MPlayer - onInfo");
        switch (what) {
        case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
            break;

        case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            break;

        case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            break;

        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            mBuffering = 0;
            configurePlayerViewTitle();
            // NOTE
            // WHY below code is required?
            // Sometimes, mediaplayer is hung on starting buffering at some devices.
            // In this case we need to retry to recover this unexpected state.
            mRetryHangOnBuffering.start(mpGet());
            break;

        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            mBuffering = NO_BUFFERING;
            configurePlayerViewTitle();
            mRetryHangOnBuffering.end();
            break;

        case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            break;

        case MediaPlayer.MEDIA_INFO_UNKNOWN:
            break;

        default:
        }
        return false;
    }
}
