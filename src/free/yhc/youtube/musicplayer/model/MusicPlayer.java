package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logD;

import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.R;

public class MusicPlayer implements
MediaController.MediaPlayerControl,
MediaPlayer.OnBufferingUpdateListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener,
MediaPlayer.OnInfoListener,
MediaPlayer.OnVideoSizeChangedListener,
MediaPlayer.OnSeekCompleteListener {
    private static final String WLTAG = "MusicPlayer";

    private static MusicPlayer instance = null;

    private WakeLock            wl = null;
    private WifiLock            wfl = null;

    private MediaPlayer         mMp     = null;
    private State               mMpS    = State.INVALID; // state of mMp;
    private MediaController     mMc     = null;
    private Context             mMcContext = null;
    private View                mPlayerv = null;

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

    private MusicPlayer() {
    }

    private void
    acquireLocks() {
        eAssert(null == wl && null == wfl);
        wl = ((PowerManager)Utils.getAppContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WLTAG);
        wfl = ((WifiManager)Utils.getAppContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WLTAG);
        wl.acquire();
        wfl.acquire();
    }

    private void
    releaseLocks() {
        if (null != wl)
                wl.release();

        if (null != wfl)
            wfl.release();

        wl = null;
        wfl = null;
    }

    // ========================================================================
    //
    // Media Player Interfaces
    //
    // ========================================================================
    private boolean
    isMpAvailable() {
        return null != mMp && State.END != mMpS;
    }

    private void
    mpSetState(State newState) {
        logD("MusicPlayer : State : " + mMpS.name() + " => " + newState.name());
        mMpS = newState;
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
    // ========================================================================
    //
    //
    //
    // ========================================================================

    private void
    enablePlayerViewClickable(boolean enable, CharSequence title) {
        if (null == mPlayerv)
            return;

        if (null != title)
            ((TextView)mPlayerv.findViewById(R.id.music_title)).setText(title);

        if (enable) {
            ImageView more = (ImageView)mPlayerv.findViewById(R.id.btn_more);
            more.setImageResource(R.drawable.ic_more);
            more.setClickable(true);
        } else {
            ImageView more = (ImageView)mPlayerv.findViewById(R.id.btn_more);
            more.setImageResource(R.drawable.ic_block);
            more.setClickable(false);
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

            enablePlayerViewClickable(false, Utils.getAppContext().getResources().getText(R.string.msg_preparing_mplayer));
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
    playDone() {
        logD("MPlayer - playDone");
        enablePlayerViewClickable(false, Utils.getAppContext().getResources().getText(R.string.msg_playing_done));
        releaseLocks();
    }

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
        eAssert(null != playerv.findViewById(R.id.music_player_layout_magic_id));

        if (context == mMcContext && mPlayerv == playerv)
            // controller is already set for this context.
            // So, nothing to do. just return!
            return Err.NO_ERR;

        mMcContext = context;
        mPlayerv = playerv;

        if (null == mPlayerv)
            return Err.NO_ERR;

        mMc = new MediaController(context);
        mMc.setMediaPlayer(this);
        mMc.setAnchorView(playerv);

        ImageView more = (ImageView)playerv.findViewById(R.id.btn_more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mMc && isMpAvailable()) {
                    mMc.show();
                }
            }
        });

        if (isMusicPlaying())
            enablePlayerViewClickable(true, mMusics[mMusici].title);
        else
            enablePlayerViewClickable(false, null);

        return Err.NO_ERR;
    }

    public void
    unsetController(Context context) {
        eAssert(null == mMcContext || context == mMcContext);
        mPlayerv = null;
        mMcContext = null;
        if (null != mMc)
            mMc.setEnabled(false);
        mMc = null;
    }

    /**
     * Should be called after {@link #setController(Context, View)}
     * @param ms
     */
    public void
    startMusicsAsync(Music[] ms) {
        eAssert(Utils.isUiThread());
        eAssert(null != mMc);

        releaseLocks();

        logD("MPlayer - release is called");
        mpRelease();
        mpNewInstance();
        mpReset();
        mMusics = ms;
        mMusici = 0;

        acquireLocks();

        enablePlayerViewClickable(false, null);
        playMusicAsync();
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
        //logD("MPlayer - onBufferingUpdate");
    }

    @Override
    public void
    onCompletion(MediaPlayer mp) {
        logD("MPlayer - onCompletion");
        mpSetState(State.PLAYBACK_COMPLETED);
        mMusici++;
        mpReset();
        enablePlayerViewClickable(false, null);
        playMusicAsync();
    }

    @Override
    public void
    onPrepared(MediaPlayer mp) {
        logD("MPlayer - onPrepared");
        mpSetState(State.PREPARED);

        if (null != mMc)
            mMc.setEnabled(true);

        if (null != mMusics)
            enablePlayerViewClickable(true, mMusics[mMusici].title);

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
        return false;
    }

    @Override
    public boolean
    onInfo(MediaPlayer mp, int what, int extra) {
        logD("MPlayer - onInfo");
        return false;
    }
    // ============================================================================
    //
    // Override for "MediaController.MediaPlayerControl"
    //
    // ============================================================================
    @Override
    public boolean
    canPause() {
        logD("MPlayer - canPause");
        return true;
    }

    @Override
    public boolean
    canSeekBackward() {
        logD("MPlayer - canSeekBackward");
        return false;
    }

    @Override
    public boolean
    canSeekForward() {
        logD("MPlayer - canSeekForward");
        return false;
    }

    @Override
    public int
    getBufferPercentage() {
        return 0;
    }

    @Override
    public int
    getCurrentPosition()        { return mpGetCurrentPosition(); }
    @Override
    public int getDuration()    { return mpGetDuration(); }
    @Override
    public boolean isPlaying()  { return mpIsPlaying(); }
    @Override
    public void pause()         { mpPause(); }
    @Override
    public void seekTo(int pos) { mpSeekTo(pos); }
    @Override
    public void start()         { mpStart();}
}
