package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logD;
import static free.yhc.youtube.musicplayer.model.Utils.logI;
import static free.yhc.youtube.musicplayer.model.Utils.logW;

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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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
    private static final String WLTAG               = "YTPlayer";
    private static final int    PLAYER_ERR_RETRY    = 3;

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
    private YTVideoConnector    mYtConn     = null;

    // ------------------------------------------------------------------------
    // UI Control.
    // ------------------------------------------------------------------------
    private Context             mVContext   = null;
    private LinearLayout        mPlayerv    = null;

    // ------------------------------------------------------------------------
    // Player Runtime Status
    // ------------------------------------------------------------------------
    private VideoListManager        mVlm    = new VideoListManager();
    private int                     mErrRetry = PLAYER_ERR_RETRY;

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

    private static enum StopState {
        DONE,
        FORCE_STOPPED,
        NETWORK_UNAVAILABLE,
        UNKNOWN_ERROR
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
                logI("YTPlayer : Network connected : " + networkInfo.getType());
                switch (networkInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    logI("YTPlayer : Network connected : WIFI");
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    logI("YTPlayer : Network connected : MOBILE");
                    break;
                }
            } else
                logI("YTPlayer : Network lost");
        }
    }



    private class UpdateProgress implements Runnable {
        private SeekBar     seekbar = null;
        private TextView    curposv = null;
        private TextView    maxposv = null;
        private int         lastProgress = -1;
        private int         lastProgress2 = -1; // For secondary progress

        private String
        progressToTimeText(int progress) {
            return Utils.secsToMinSecText(mpGetDuration() / 1000 * lastProgress / 100);
        }

        private void
        resetProgressView() {
            if (null != seekbar) {
                maxposv.setText(Utils.secsToMinSecText(mpGetDuration() / 1000));
                update(1, 0);
                update2(0);
            }
            lastProgress = -1;
            lastProgress2 = -1;
        }

        void setProgressView(ViewGroup progv) {
            eAssert(Utils.isUiThread());
            eAssert(null != progv.findViewById(R.id.music_player_progress));
            curposv = (TextView)progv.findViewById(R.id.music_player_curpos);
            maxposv = (TextView)progv.findViewById(R.id.music_player_maxpos);
            seekbar = (SeekBar)progv.findViewById(R.id.music_player_seekbar);
            resetProgressView();
        }

        void start() {
            //logI("Progress Start");
            maxposv.setText(Utils.secsToMinSecText(mpGetDuration() / 1000));
            update(mpGetDuration(), lastProgress);
            update2(lastProgress2);
            run();
        }

        void stop() {
            //logI("Progress End");
            Utils.getUiHandler().removeCallbacks(this);
            resetProgressView();
        }

        void update(int duration, int currentPos) {
            // ignore aDuration.
            // Sometimes youtube player returns incorrect duration value!
            if (null != seekbar) {
                int curProgress = (duration > 0)? currentPos * 100 / duration
                                                : 0;
                if (curProgress > lastProgress) {
                    seekbar.setProgress(curProgress);
                    curposv.setText(progressToTimeText(curProgress));
                }

                lastProgress = curProgress;
            }
        }

        /**
         * Update secondary progress
         * @param percent
         */
        void update2(int percent) {
            // Update secondary progress
            if (null != seekbar) {
                if (lastProgress2 != percent)
                    seekbar.setSecondaryProgress(percent);
                lastProgress2 = percent;
            }
        }

        @Override
        public void run() {
            update(mpGetDuration(), mpGetCurrentPosition());
            Utils.getUiHandler().postDelayed(this, 1000);
        }
    }

    private static class VideoListManager {
        private Video[]     vs  = null; // video array
        private int         vi  = -1; // video index

        boolean hasActiveVideo() {
            return null != getActiveVideo();
        }

        boolean hasNextVideo() {
            return hasActiveVideo()
                   && vi < (vs.length - 1);
        }

        boolean hasPrevVideo() {
            return hasActiveVideo() && 0 < vi;
        }

        void reset() {
            vs = null;
            vi = -1;
        }

        void setVideoList(Video[] aVs) {
            vs = aVs;
            if (null == vs || 0 >= vs.length)
                reset();
            else if(vs.length > 0)
                vi = 0;
        }

        Video getActiveVideo() {
            if (null != vs && 0 <= vi && vi < vs.length)
                return vs[vi];
            return null;
        }

        boolean moveToFist() {
            if (hasActiveVideo()) {
                    vi = 0;
                    return true;
            }
            return false;
        }

        boolean moveToNext() {
            if (hasActiveVideo()
                && vi < (vs.length - 1)) {
                vi++;
                return true;
            }
            return false;
        }

        boolean moveToPrev() {
            if (hasActiveVideo()
                && vi > 0) {
                vi--;
                return true;
            }
            return false;
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
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WLTAG);
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
        mMp = null;
        mpSetState(MPState.END);
    }

    private void
    mpReset() {
        mMp.reset();
        mpSetState(MPState.IDLE);
    }

    private void
    mpSetVolume(int vol) {
        float volf = vol/100.0f;
        mMp.setVolume(volf, volf);
    }

    private int
    mpGetCurrentPosition() {
        switch (mpGetState()) {
        case ERROR:
        case END:
            return 0;
        }
        return mMp.getCurrentPosition();
    }

    private int
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

    private boolean
    mpIsPlaying() {
        //logD("MPlayer - isPlaying");
        return mMp.isPlaying();
    }

    private void
    mpPause() {
        logD("MPlayer - pause");
        mMp.pause();
        mpSetState(MPState.PAUSED);
    }

    private void
    mpSeekTo(int pos) {
        logD("MPlayer - seekTo");
        mMp.seekTo(pos);
    }

    private void
    mpStart() {
        logD("MPlayer - start");
        mMp.start();
        mpSetState(MPState.STARTED);
    }

    private void
    mpStop() {
        if (null == mMp)
            return;

        logD("MPlayer - stop");
        mMp.stop();
        mpSetState(MPState.STOPPED);
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
            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    stopPlay(StopState.NETWORK_UNAVAILABLE);
                }
            });
            return;
        }

        CharSequence videoTitle = "";
        if (mVlm.hasActiveVideo())
            videoTitle = mVlm.getActiveVideo().title;

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

        if (!mVlm.hasActiveVideo()) {
            controlv.setVisibility(View.GONE);
            return;
        }

        controlv.setVisibility(View.VISIBLE);
        ImageView nextv = (ImageView)controlv.findViewById(R.id.music_player_btnnext);
        ImageView prevv = (ImageView)controlv.findViewById(R.id.music_player_btnprev);
        ImageView playv = (ImageView)controlv.findViewById(R.id.music_player_btnplay);

        switch (to) {
        case BUFFERING:
        case PAUSED:
        case STARTED:
            if (mVlm.hasNextVideo())
                pvEnableButton(nextv, R.drawable.ic_media_next);
            else
                pvDisableButton(nextv);

            if (mVlm.hasPrevVideo())
                pvEnableButton(prevv, R.drawable.ic_media_prev);
            else
                pvDisableButton(prevv);
            break;

        default:
            pvDisableButton(prevv);
            pvDisableButton(nextv);
        }

        switch (to) {
        case PREPARED:
        case PAUSED:
            pvEnableButton(playv, R.drawable.ic_media_play);
            // Set next state be moved to on click as 'Tag'
            playv.setTag(MPState.STARTED);
            break;

        case STARTED:
            pvEnableButton(playv, R.drawable.ic_media_pause);
            // Set next state be moved to on click as 'Tag'
            playv.setTag(MPState.PAUSED);
            break;

        case BUFFERING:
        case INITIALIZED:
        case PREPARING:
            pvEnableButton(playv, R.drawable.ic_media_stop);
            // Set next state be moved to on click as 'Tag'
            playv.setTag(MPState.STOPPED);
            break;

        default:
            playv.setTag(null);
            controlv.setVisibility(View.GONE);
        }
    }

    private void
    pvConfigureProgress(ViewGroup progressv, MPState from, MPState to) {

        if (null == progressv)
            return;

        switch (to) {
        case PREPARED:
            mUpdateProg.start();
            break;

        case PLAYBACK_COMPLETED:
            // Workaround of Youtube player.
            // Sometimes Youtube player doesn't update progress 100% before playing is ended.
            // So, update to 100% in force at this ended state.
            mUpdateProg.update(1, 1);
            // Missing 'break' is intentional.

        case INITIALIZED:
        case PREPARING:
        case STARTED:
        case PAUSED:
        case BUFFERING:
            ; // do nothing progress is now under update..
            break;

        default:
            mUpdateProg.stop();
            mUpdateProg.update(1, 0);
        }
    }

    private void
    pvConfigureAll(ViewGroup playerv, MPState from, MPState to) {
        if (null == playerv)
            return; // nothing to do

        pvConfigureTitle((TextView)playerv.findViewById(R.id.music_player_title),
                          from, to);
        pvConfigureProgress((ViewGroup)playerv.findViewById(R.id.music_player_progress),
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
                // See pvConfigControl() for details.
                MPState nextst = (MPState)v.getTag();
                if (null == nextst)
                    return; // Nothing to do.

                switch (nextst) {
                case STARTED:
                    mpStart();
                    break;

                case PAUSED:
                    mpPause();
                    // prevent clickable during transition player state.
                    break;

                case STOPPED:
                    // This doesn't means "Stop only this video",
                    //   but means stop playing vidoes - previous user request.
                    stopVideos();
                    break;

                default:
                    ; // do nothing.
                }
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.music_player_btnprev);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPrev();
                pvDisableControlButton(playerv);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.music_player_btnnext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNext();
                pvDisableControlButton(playerv);
            }
        });
    }

    private void
    pvInit(ViewGroup playerv) {
        ViewGroup progv = (ViewGroup)playerv.findViewById(R.id.music_player_progress);
        SeekBar sb = (SeekBar)progv.findViewById(R.id.music_player_seekbar);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mpSeekTo(seekBar.getProgress() * mpGetDuration() / 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
            }
        });
        mUpdateProg.setProgressView(progv);
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
        case INVALID:
            releaseLocks();
            break;

        case STARTED:
            acquireLocks();
            break;
        }
    }

    private void
    startVideo(Video v, boolean recovery) {
        if (null != v)
            startVideo(v.videoId, v.volume, recovery);
    }

    private void
    startVideo(final String videoId, int volume, boolean recovery) {
        eAssert(0 <= volume && volume <= 100);

        if (recovery) {
            mErrRetry--;
            if (mErrRetry <= 0) {
                stopPlay(StopState.UNKNOWN_ERROR);
                return;
            }
        } else
            mErrRetry = PLAYER_ERR_RETRY;

        // Stop if player is already running.
        mpStop();
        mpRelease();
        mpNewInstance();
        mpReset();
        mpSetVolume(volume);

        YTVideoConnector.YtVideoConnListener listener = new YTVideoConnector.YtVideoConnListener() {
            @Override
            public void
            onPreConn(YTVideoConnector ytconn, String ytvid, Object user) {
            }

            @Override
            public void
            onPostConn(YTVideoConnector ytconn, Err result, String ytvid, Object user) {
                mYtConn = null;

                if (Err.NO_ERR != result) {
                    logW("YTVideoConnector Fails : " + result.name());
                    startVideo(mVlm.getActiveVideo(), true);
                    return;
                }

                YTVideoConnector.YtVideo ytv = ytconn.getVideo(YTVideoConnector.YTQUALITY_SCORE_LOWEST);
                try {
                    mpSetDataSource(Uri.parse(ytv.url));
                } catch (IOException e) {
                    eAssert(false);
                }
                mpPrepareAsync();
            }

            @Override
            public void
            onConnCancelled(YTVideoConnector ytconn, String ytvid, Object user) {
                mYtConn = null;
            }
        };
        mYtConn = new YTVideoConnector(videoId, volume, listener);
        mYtConn.execute();
    }

    private void
    startNext() {
        if (!mVlm.hasActiveVideo())
            return; // do nothing

        if (mVlm.moveToNext())
            startVideo(mVlm.getActiveVideo(), false);
        else
            stopPlay(StopState.DONE);
    }

    private void
    startPrev() {
        if (!mVlm.hasActiveVideo())
            return; // do nothing

        if (mVlm.moveToPrev())
            startVideo(mVlm.getActiveVideo(), false);
        else
            stopPlay(StopState.DONE);
    }

    private void
    stopPlay(StopState st) {
        logD("VideoView - playDone : forceStop (" + st.name() + ")");
        if (null != mYtConn)
            mYtConn.forceCancel();

        if (StopState.DONE == st
            && Utils.isPrefRepeat()) {
            if (mVlm.moveToFist()) {
                startVideo(mVlm.getActiveVideo(), false);
                return;
            }
        }

        mpStop();
        mpRelease();
        releaseLocks();
        mVlm.reset();
        mErrRetry = PLAYER_ERR_RETRY;

        // This should be called before changing title because
        //   title may be changed in onMpStateChanged().
        // We need to overwrite title message.
        mpSetState(MPState.INVALID);

        TextView titlev = (TextView)mPlayerv.findViewById(R.id.music_player_title);
        if (null != mPlayerv) {
            switch (st) {
            case DONE:
                pvSetTitle(titlev, mRes.getText(R.string.msg_playing_done), false);
                break;

            case FORCE_STOPPED:
                pvSetTitle(titlev, mRes.getText(R.string.msg_playing_stopped), false);
                break;

            case NETWORK_UNAVAILABLE:
                pvSetTitle(titlev, mRes.getText(R.string.err_network_unavailable), false);
                break;

            case UNKNOWN_ERROR:
                pvSetTitle(titlev, mRes.getText(R.string.msg_playing_err_unknown), false);
                break;
            }
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

        if (null == vs || vs.length <= 0)
            return;

        acquireLocks();

        mVlm.setVideoList(vs);
        if (mVlm.moveToFist())
            startVideo(mVlm.getActiveVideo(), false);
    }

    public void
    startVideos(final Cursor c,
                final int coliUrl,      final int coliTitle,
                final int coliVolume,   final int coliPlaytime,
                final boolean shuffle) {
        eAssert(Utils.isUiThread());
        eAssert(null != mPlayerv);

        new Thread(new Runnable() {
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
        }).start();
    }

    public void
    stopVideos() {
        stopPlay(StopState.FORCE_STOPPED);
    }

    public void
    setVideoVolume(int vol) {
        eAssert(0 <= vol && vol <= 100);
        // Implement this
        if (isVideoPlaying())
            mpSetVolume(vol);
    }

    // ============================================================================
    //
    //
    //
    // ============================================================================

    public boolean
    isVideoPlaying() {
        return mVlm.hasActiveVideo();
    }

    // ============================================================================
    //
    // Override for "MediaPlayer.*"
    //
    // ============================================================================
    @Override
    public void
    onBufferingUpdate (MediaPlayer mp, int percent) {
        //logD("MPlayer - onBufferingUpdate : " + percent + " %");
        // See comments aroudn MEDIA_INFO_BUFFERING_START in onInfo()
        //mpSetState(MPState.BUFFERING);
        mUpdateProg.update2(percent);
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
        boolean tryAgain = true;
        mpSetState(MPState.ERROR);
        switch (what) {
        case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
            logD("MPlayer - onError : NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
            tryAgain = false;
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

        if (tryAgain && mVlm.hasActiveVideo()) {
            logD("MPlayer - Try to recover!");
            startVideo(mVlm.getActiveVideo(), true);
        } else
            stopPlay(StopState.UNKNOWN_ERROR);

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
            // NOTE
            // In case of progressive download, media player tries to buffering continuously.
            // Even if media is playing, media info keep notifying 'buffering'
            // This is not expected behavior of player.
            // So, just ignore buffering state.
            //mpSetState(MPState.BUFFERING);
            break;

        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            // TODO
            // Check is there any exceptional case regarding buffering???
            //mpSetState(MPState.STARTED);
            mUpdateProg.update2(100);
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
