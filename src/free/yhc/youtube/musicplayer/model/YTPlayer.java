package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logD;
import static free.yhc.youtube.musicplayer.model.Utils.logI;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.R;

public class YTPlayer implements
MediaPlayer.OnBufferingUpdateListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener,
MediaPlayer.OnInfoListener,
MediaPlayer.OnVideoSizeChangedListener,
MediaPlayer.OnSeekCompleteListener {
    private static final String WLTAG   = "YTPlayer";

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

    private static final Comparator<Video> sVideoTitleComparator = new Comparator<Video>() {
        @Override
        public int compare(Video o1, Video o2) {
            return o1.title.compareTo(o2.title);
        }
    };

    private static YTPlayer sInstance = null;

    private final Resources     mRes        = Utils.getAppContext().getResources();
    private final DB            mDb         = DB.get();

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------
    private final UpdateProgress        mUpdateProg = new UpdateProgress();

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    private WakeLock            mWl         = null;
    private WifiLock            mWfl        = null;
    private MediaPlayer         mMp         = null;
    private MPState             mMpS        = MPState.INVALID; // state of mMp;

    // ------------------------------------------------------------------------
    // UI Control.
    // ------------------------------------------------------------------------
    private Context             mVContext   = null;
    private LinearLayout        mPlayerv    = null;

    // ------------------------------------------------------------------------
    // Player Runtime Status
    // ------------------------------------------------------------------------
    private Video[]                 mVideos = null;
    private int                     mVideoi = -1;

    // see "http://developer.android.com/reference/android/media/MediaPlayer.html"
    private static enum MPState {
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
        BUFFERING, // Not in mediaplayer state but useful
        ERROR
    }

    public static class Video {
        String   title;
        String   videoId;
        int      volume;
        int      playtime; // This is to workaround not-correct value returns from getDuration() function
                           //   of youtube player.
        public Video(String aVideoId, String aTitle,
                     int aVolume, int aPlaytime) {
            videoId = aVideoId;
            title = aTitle;
            playtime = aPlaytime;
            volume = aVolume;
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

    // This class also for future use.
    public static class NetworkMonitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION))
                return;

            NetworkInfo networkInfo =
                (NetworkInfo)intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                logI("YTJSPlayer : Network connected : " + networkInfo.getType());
                switch (networkInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    logI("YTJSPlayer : Network connected : WIFI");
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    logI("YTJSPlayer : Network connected : MOBILE");
                    break;
                }
            } else
                logI("YTJSPlayer : Network lost");
        }
    }


    private class UpdateProgress {
        private ProgressBar progbar = null;
        private int         lastProgress = -1;
        private int         duration = 0;

        void setProgbar(ProgressBar aProgbar) {
            eAssert(Utils.isUiThread());
            progbar = aProgbar;
            if (null != progbar)
                progbar.setProgress(lastProgress);
        }

        void start(ProgressBar aProgbar) {
            //logI("Progress Start");
            progbar = aProgbar;
            lastProgress = -1;
            duration = 0;
        }

        void end() {
            //logI("Progress End");
            progbar = null;
            lastProgress = -1;
            duration = 0;
        }

        // FIXME
        // This is to workaround incorrect return value of youtube player's getDuration() function.
        // So, if Youtube player gives right value, this function and all related codes should be removed.
        void setDuration(int newDuration) {
            duration = newDuration;
        }

        void update(int aDuration, int currentPos) {
            // ignore aDuration.
            // Sometimes youtube player returns incorrect duration value!
            //logI("Progress Update(" + (null == progbar? "X": "O") + ") : " + currentPos + " / " + duration);
            if (null != progbar) {
                int curProgress = (duration > 0)? currentPos * 100 / duration
                                                : 0;
                if (curProgress > lastProgress)
                    progbar.setProgress(curProgress);

                lastProgress = curProgress;
            }
        }
    }
    // ========================================================================
    //
    //
    //
    // ========================================================================
    private void
    acquireLocks() {
        if (null != mWl)
            return; // already locked nothing to do

        eAssert(null == mWfl);
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
        if (null == mWl)
            return;

        eAssert(null != mWfl);
        mWl.release();
        mWfl.release();

        mWl = null;
        mWfl = null;
    }


    // ============================================================================
    //
    //
    //
    // ============================================================================
    private Video[]
    getVideos(Cursor c,
              int coliTitle,  int coliUrl,
              int coliVolume, int coliPlaytime,
              boolean shuffle) {
        if (!c.moveToFirst())
            return new Video[0];

        Video[] vs = new Video[c.getCount()];

        int i = 0;
        if (!shuffle) {
            do {
                vs[i++] = new Video(c.getString(coliUrl),
                                    c.getString(coliTitle),
                                    c.getInt(coliVolume),
                                    c.getInt(coliPlaytime));
            } while (c.moveToNext());
            Arrays.sort(vs, sVideoTitleComparator);
        } else {
            // This is shuffled case!
            Random r = new Random(System.currentTimeMillis());
            NrElem[] nes = new NrElem[c.getCount()];
            do {
                nes[i++] = new NrElem(r.nextInt(),
                                      new Video(c.getString(coliUrl),
                                                c.getString(coliTitle),
                                                c.getInt(coliVolume),
                                                c.getInt(coliPlaytime)));
            } while (c.moveToNext());
            Arrays.sort(nes, sNrElemComparator);
            for (i = 0; i < nes.length; i++)
                vs[i] = (Video)nes[i].tag;
        }
        return vs;
    }

    // ========================================================================
    //
    // Media Player Interfaces
    //
    // ========================================================================
    private boolean
    mpIsAvailable() {
        return null != mMp
               && MPState.END != mpGetState()
               && MPState.INVALID != mpGetState();
    }
    private void
    mpSetState(MPState newState) {
        logD("MusicPlayer : State : " + mMpS.name() + " => " + newState.name());
        MPState old = mMpS;
        mMpS = newState;
        onMpStateChanged(old, newState);
    }

    private MPState
    mpGetState() {
        return mMpS;
    }

    private void
    initMediaPlayer(MediaPlayer mp) {
        mp.setOnBufferingUpdateListener(this);
        mp.setOnCompletionListener(this);
        mp.setOnPreparedListener(this);
        mp.setOnVideoSizeChangedListener(this);
        mp.setOnSeekCompleteListener(this);
        mp.setOnErrorListener(this);
        mp.setOnInfoListener(this);
        mp.setScreenOnWhilePlaying(false);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private void
    mpNewInstance() {
        mMp = new MediaPlayer();
        mpSetState(MPState.INVALID);
        initMediaPlayer(mMp);
    }

    private MediaPlayer
    mpGet() {
        return mMp;
    }

    private void
    mpSetDataSource(Uri uri) throws IOException {
        mMp.setDataSource(Utils.getAppContext(), uri);
        mpSetState(MPState.INITIALIZED);
    }

    private void
    mpPrepareAsync() {
        mpSetState(MPState.PREPARING);
        mMp.prepareAsync();
    }

    private void
    mpRelease() {
        if (null == mMp || MPState.END == mpGetState())
            return;


        if (MPState.ERROR != mMpS && mMp.isPlaying())
            mMp.stop();

        mMp.release();
        mpSetState(MPState.END);
    }

    private void
    mpReset() {
        mMp.reset();
        mpSetState(MPState.IDLE);
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
        mpSetState(MPState.PAUSED);
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
        mpSetState(MPState.STARTED);
    }

    public void
    mpStop() {
        if (null == mMp)
            return;

        logD("MPlayer - stop");
        mMp.stop();
    }


    // ========================================================================
    //
    // Player View Handling
    //
    // ========================================================================
    private void
    pvDisableButton(ImageView btn) {
        btn.setVisibility(View.INVISIBLE);
    }

    private void
    pvEnableButton(ImageView btn, int image) {
        btn.setImageResource(image);
        btn.setVisibility(View.VISIBLE);
    }

    private void
    pvSetTitle(TextView titlev, CharSequence title, boolean buffering) {
        if (null == titlev || null == title)
            return;

        if (buffering)
            titlev.setText("(" + mRes.getText(R.string.buffering) + ") " + title);
        else
            titlev.setText(title);
    }

    private void
    pvConfigureTitle(TextView titlev, MPState from, MPState to) {
        if (null == titlev)
            return;

        if (!Utils.isNetworkAvailable()) {
            pvSetTitle(titlev, mRes.getText(R.string.msg_network_unavailable), false);
            return;
        }

        CharSequence videoTitle = "";
        if (isVideoPlaying())
            videoTitle = mVideos[mVideoi].title;

        switch (to) {
        case BUFFERING: {
            eAssert(null != videoTitle);
            pvSetTitle(titlev, videoTitle, true);
        } break;

        case PREPARED:
        case PAUSED:
        case STARTED:
            eAssert(null != videoTitle);
            if (null != videoTitle)
                pvSetTitle(titlev, videoTitle, false);
            break;

        case ERROR:
            pvSetTitle(titlev, mRes.getText(R.string.msg_ytplayer_err), false);
            break;

        default:
            pvSetTitle(titlev, mRes.getText(R.string.msg_preparing_mplayer), false);
        }
    }


    private void
    pvDisableControlButton(ViewGroup playerv) {
        pvDisableButton((ImageView)playerv.findViewById(R.id.music_player_btnplay));
        pvDisableButton((ImageView)playerv.findViewById(R.id.music_player_btnnext));
        pvDisableButton((ImageView)playerv.findViewById(R.id.music_player_btnprev));
    }

    private void
    pvConfigureControl(ViewGroup controlv, MPState from, MPState to) {
        if (null == controlv)
            return;

        if (null == mVideos) {
            controlv.setVisibility(View.GONE);
            return;
        }

        controlv.setVisibility(View.VISIBLE);
        switch (to) {
        case BUFFERING:
        case PAUSED:
        case STARTED:
            if (mVideos.length - 1 <= mVideoi)
                pvDisableButton((ImageView)controlv.findViewById(R.id.music_player_btnnext));
            else
                pvEnableButton((ImageView)controlv.findViewById(R.id.music_player_btnnext),
                               R.drawable.ic_media_next);

            if (0 >= mVideoi)
                pvDisableButton((ImageView)controlv.findViewById(R.id.music_player_btnprev));
            else
                pvEnableButton((ImageView)controlv.findViewById(R.id.music_player_btnprev),
                               R.drawable.ic_media_prev);
            break;
        }

        switch (to) {
        case BUFFERING:
            pvEnableButton((ImageView)controlv.findViewById(R.id.music_player_btnplay),
                    R.drawable.ic_media_stop);
            break;

        case PREPARED:
        case PAUSED:
            pvEnableButton((ImageView)controlv.findViewById(R.id.music_player_btnplay),
                           R.drawable.ic_media_play);
            break;

        case STARTED:
            pvEnableButton((ImageView)controlv.findViewById(R.id.music_player_btnplay),
                           R.drawable.ic_media_pause);
            break;

        default:
            controlv.setVisibility(View.GONE);
        }
    }

    private void
    pvConfigureProgressBar(ProgressBar pbar, MPState from, MPState to) {

        if (null == pbar)
            return;

        switch (to) {
        case PREPARED:
            mUpdateProg.start(pbar);
            break;

        case PLAYBACK_COMPLETED:
            // Workaround of Youtube player.
            // Sometimes Youtube player doesn't update progress 100% before playing is ended.
            // So, update to 100% in force at this ended state.
            mUpdateProg.update(1, 1);
            // Missing 'break' is intentional.

        case STARTED:
            if (isVideoPlaying())
                mUpdateProg.setDuration(mVideos[mVideoi].playtime);
        case PAUSED:
        case BUFFERING:
            ; // do nothing progress is now under update..
            break;

        default:
            mUpdateProg.end();
            pbar.setProgress(0);
        }
    }

    private void
    pvConfigureAll(ViewGroup playerv, MPState from, MPState to) {
        if (null == playerv)
            return; // nothing to do

        pvConfigureTitle((TextView)playerv.findViewById(R.id.music_player_title),
                          from, to);
        pvConfigureProgressBar((ProgressBar)playerv.findViewById(R.id.music_player_progressbar),
                               from, to);
        pvConfigureControl((ViewGroup)playerv.findViewById(R.id.music_player_control),
                           from, to);
    }

    private void
    pvSetupControlButton(final ViewGroup playerv) {
        ImageView btn = (ImageView)playerv.findViewById(R.id.music_player_btnplay);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mpGetState()) {
                case PAUSED:
                    mpStart();
                    break;

                case STARTED:
                    mpPause();
                    // prevent clickable during transition player state.
                    break;

                case BUFFERING:
                    mpStop();
                    break;

                default:
                    ; // do nothing.
                }
                pvDisableControlButton(playerv);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.music_player_btnprev);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mpStop();
                startPrev();
                pvDisableControlButton(playerv);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.music_player_btnnext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mpStop();
                startNext();
                pvDisableControlButton(playerv);
            }
        });
    }

    private void
    pvInit(ViewGroup playerv) {
        mUpdateProg.setProgbar((ProgressBar)playerv.findViewById(R.id.music_player_progressbar));
        pvSetupControlButton(playerv);
        pvConfigureAll(playerv, MPState.INVALID, mpGetState());
    }

    // ========================================================================
    //
    // General Control
    //
    // ========================================================================
    private void
    onMpStateChanged(MPState from, MPState to) {
        if (from == to)
            return;

        pvConfigureAll(mPlayerv, from, to);
        switch (to) {
        case PAUSED:
            releaseLocks();
            break;

        case STARTED:
            acquireLocks();
            break;
        }
    }

    private void
    startVideo(final String videoId, int volume) {
        eAssert(0 <= volume && volume <= 100);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Implement!!!!
                YTVideoConnector.YtVideo ytv = null;
                YTVideoConnector ytconn = new YTVideoConnector();
                try {
                    ytconn.connect(videoId);
                    ytv = ytconn.getVideo(YTVideoConnector.YTQUALITY_SCORE_LOWEST);
                } catch (YTMPException e) {
                    eAssert(false);
                }

                final String vurl = ytv.url;
                // Running media player is OK here???

                Utils.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mpSetDataSource(Uri.parse(vurl));
                        } catch (IOException e) {
                            eAssert(false);
                        }
                        mpPrepareAsync();
                    }
                });
            }
        }).start();
    }

    private void
    startNext() {
        if (null == mVideos)
            return; // do nothing

        mVideoi++;
        if (mVideoi >= mVideos.length) {
            mVideoi = mVideos.length;
            playDone();
        } else
            startVideo(mVideos[mVideoi].videoId, mVideos[mVideoi].volume);
    }

    private void
    startPrev() {
        if (null == mVideos)
            return; // do nothing

        mVideoi--;
        if (mVideoi < 0) {
            mVideoi = -1;
            playDone();
        } else
            startVideo(mVideos[mVideoi].videoId, mVideos[mVideoi].volume);
    }

    private void
    playDone() {
        logD("VideoView - playDone");
        if (Utils.isPrefRepeat())
            startVideos(mVideos);
        else {
            if (null != mPlayerv) {
                pvSetTitle((TextView)mPlayerv.findViewById(R.id.music_player_title),
                            mRes.getText(R.string.msg_playing_done), false);
            }
            releaseLocks();
        }
    }


    // ============================================================================
    //
    //
    //
    // ============================================================================
    private YTPlayer() {
    }

    public static YTPlayer
    get() {
        if (null == sInstance)
            sInstance = new YTPlayer();
        return sInstance;
    }

    public Err
    setController(Context context, ViewGroup playerv) {
        if (context == mVContext && mPlayerv == playerv)
            // controller is already set for this context.
            // So, nothing to do. just return!
            return Err.NO_ERR;

        mVContext = context;
        mPlayerv = (LinearLayout)playerv;

        if (null == mPlayerv)
            return Err.NO_ERR;

        eAssert(null != mPlayerv.findViewById(R.id.music_player_layout_magic_id));
        pvInit(playerv);

        return Err.NO_ERR;
    }

    public void
    unsetController(Context context) {
        if (context == mVContext) {
            mPlayerv = null;
            mVContext = null;
        }
    }

    public void
    startVideos(final Video[] vs) {
        eAssert(Utils.isUiThread());
        eAssert(null != mPlayerv);

        if (vs.length <= 0)
            return;

        // Stop if player is already running.
        mpStop();
        mpRelease();
        mpNewInstance();
        mpReset();

        acquireLocks();

        mVideos = vs;
        mVideoi = 0;

        startVideo(mVideos[mVideoi].videoId, mVideos[mVideoi].volume);
    }

    public void
    startVideos(final Cursor c,
                final int coliUrl,      final int coliTitle,
                final int coliVolume,   final int coliPlaytime,
                final boolean shuffle) {
        eAssert(Utils.isUiThread());
        eAssert(null != mPlayerv);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final Video[] vs = getVideos(c, coliTitle, coliUrl, coliVolume, coliPlaytime, shuffle);
                Utils.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        startVideos(vs);
                    }
                });
                c.close();
            }
        });
    }

    public void
    stopVideos() {
        mpStop();
        mVideos = null;
        mVideoi = -1;
    }

    public void
    setVideoVolume(int vol) {
        eAssert(0 <= vol && vol <= 100);
        // Implement this
        eAssert(false);
    }

    // ============================================================================
    //
    //
    //
    // ============================================================================

    public boolean
    isVideoPlaying() {
        if (null != mVideos
            && 0 <= mVideoi
            && mVideoi < mVideos.length)
            return true;
        return false;
    }

    // ============================================================================
    //
    // Override for "MediaPlayer.*"
    //
    // ============================================================================
    @Override
    public void
    onBufferingUpdate (MediaPlayer mp, int percent) {
        logD("MPlayer - onBufferingUpdate");
        mpSetState(MPState.BUFFERING);
    }

    @Override
    public void
    onCompletion(MediaPlayer mp) {
        logD("MPlayer - onCompletion");
        mpSetState(MPState.PLAYBACK_COMPLETED);
        startNext();
    }

    @Override
    public void
    onPrepared(MediaPlayer mp) {
        logD("MPlayer - onPrepared");
        mpSetState(MPState.PREPARED);
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
        mpSetState(MPState.ERROR);
        switch (what) {
        case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
            logD("MPlayer - onError : NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
            break;

        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
            logD("MPlayer - onError : MEDIA_ERROR_SERVER_DIED");
            break;

        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
            logD("MPlayer - onError : UNKNOWN");
            break;

        default:
            logD("MPlayer - onError");
        }

        return true; // DO NOT call onComplete Listener.
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
            mpSetState(MPState.BUFFERING);
            break;

        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            // TODO
            // Check is there any exceptional case regarding buffering???
            mpSetState(MPState.STARTED);
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
