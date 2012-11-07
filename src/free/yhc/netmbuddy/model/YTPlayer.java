/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of YTMPlayer.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.model.Utils.eAssert;
import static free.yhc.netmbuddy.model.Utils.logD;
import static free.yhc.netmbuddy.model.Utils.logI;
import static free.yhc.netmbuddy.model.Utils.logW;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.VideoPlayerActivity;
import free.yhc.netmbuddy.model.YTDownloader.DnArg;
import free.yhc.netmbuddy.model.YTDownloader.DownloadDoneReceiver;

public class YTPlayer implements
MediaPlayer.OnBufferingUpdateListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener,
MediaPlayer.OnInfoListener,
MediaPlayer.OnVideoSizeChangedListener,
MediaPlayer.OnSeekCompleteListener,
// To support video
SurfaceHolder.Callback {
    // State Flags - Package private.
    static final int    MPSTATE_FLAG_IDLE       = 0x0;
    static final int    MPSTATE_FLAG_SEEKING    = 0x1;
    static final int    MPSTATE_FLAG_BUFFERING  = 0x2;


    private static final String WLTAG               = "YTPlayer";
    private static final int    PLAYER_ERR_RETRY    = Policy.YTPLAYER_RETRY_ON_ERROR;

    private static final Comparator<NrElem> sNrElemComparator = new Comparator<NrElem>() {
        @Override
        public int
        compare(NrElem o1, NrElem o2) {
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
        public int
        compare(Video o1, Video o2) {
            return o1.title.compareTo(o2.title);
        }
    };

    private static File     sCacheDir = new File(Policy.APPDATA_CACHEDIR);

    private static YTPlayer sInstance = null;

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------
    private final DB                    mDb         = DB.get();
    private final YTPlayerUI            mUi         = new YTPlayerUI(this); // for UI control
    private final AutoStop              mAutoStop   = new AutoStop();
    private final StartVideoRecovery    mStartVideoRecovery = new StartVideoRecovery();
    private final VideoListManager      mVlm;

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    private WakeLock            mWl         = null;
    private WifiLock            mWfl        = null;
    private MediaPlayer         mMp         = null;
    private MPState             mMpS        = MPState.INVALID; // state of mMp;
    private int                 mMpSFlag    = MPSTATE_FLAG_IDLE;
    private boolean             mMpSurfAttached = false;
    private SurfaceHolder       mSurfHolder = null; // To support video
    private boolean             mSurfReady  = false;
    private boolean             mVSzReady   = false;
    private int                 mMpVol      = Policy.DEFAULT_VIDEO_VOLUME; // Current volume of media player.
    private YTHacker            mYtHack     = null;
    private NetLoader           mLoader     = null;
    // assign dummy instance to remove "if (null != mYtDnr)"
    private YTDownloader        mYtDnr      = new YTDownloader();

    // ------------------------------------------------------------------------
    // Runtime Status
    // ------------------------------------------------------------------------
    private int                     mErrRetry = PLAYER_ERR_RETRY;
    private YTPState                mYtpS   = YTPState.IDLE;
    private PlayerState             mStoredPState = null;

    // ------------------------------------------------------------------------
    // Listeners
    // ------------------------------------------------------------------------
    private KBLinkedList<VideosStateListener>   mVStateLsnrl = new KBLinkedList<VideosStateListener>();
    private KBLinkedList<PlayerStateListener>   mPStateLsnrl = new KBLinkedList<PlayerStateListener>();

    public interface VideosStateListener {
        /**
         *  playing videos in the queue is started
         */
        void onStarted();
        /**
         * playing videos in the queue is stopped
         */
        void onStopped(StopState state);
        /**
         * video-queue of the player is changed.
         */
        void onChanged();
    }

    public interface PlayerStateListener {
        void onStateChanged(MPState from, int fromFlag,
                            MPState to,   int toFlag);
        void onBufferingChanged(int percent);
    }


    // see "http://developer.android.com/reference/android/media/MediaPlayer.html"
    public static enum MPState {
        INVALID,
        IDLE,
        INITIALIZED,
        PREPARING,
        PREPARED_AUDIO, // This is same with MediaPlayer's PREPARED state
        PREPARED,       // MediaPlayer is prepared + SurfaceHolder for video is prepared.
        STARTED,
        STOPPED,
        PAUSED,
        PLAYBACK_COMPLETED,
        END,
        ERROR
    }

    public static enum StopState {
        DONE,
        FORCE_STOPPED,
        NETWORK_UNAVAILABLE,
        UNKNOWN_ERROR
    }

    private static enum YTPState {
        IDLE,
        SUSPENDED,
    }

    public static class Video {
        public final String   title;
        public final String   videoId;
        public final int      volume;
        public final int      playtime; // This is to workaround not-correct value returns from getDuration() function
                                        //   of youtube player (Seconds).
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

    private static class PlayerState {
        MPState mpState     = MPState.INVALID;
        int     mpStateFlag = MPSTATE_FLAG_IDLE;
        Video   vidobj      = null;
        int     pos         = -1;
        int     vol         = -1;
    }


    public static class ToolButton {
        public int                  drawable    = 0;
        public View.OnClickListener onClick     = null;
        public ToolButton(int aDrawable, View.OnClickListener aOnClick) {
            drawable= aDrawable;
            onClick = aOnClick;
        }
    }

    public static class TelephonyMonitor extends BroadcastReceiver {
        @Override
        public void
        onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals("android.intent.action.PHONE_STATE"))
                return;

            String exst = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            if (null == exst) {
                logW("TelephonyMonitor Unexpected broadcast message");
                return;
            }

            if (TelephonyManager.EXTRA_STATE_IDLE.equals(exst)) {
                YTPlayer.get().ytpResumePlaying();
            } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(exst)
                       || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(exst)) {
                if (!YTPlayer.get().ytpIsSuspended())
                    YTPlayer.get().ytpSuspendPlaying();
            } else {
                logW("TelephonyMonitor Unexpected extra state : " + exst);
                return; // ignore others.
            }
        }
    }

    // This class also for future use.
    public static class NetworkMonitor extends BroadcastReceiver {
        @Override
        public void
        onReceive(Context context, Intent intent) {
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

    public static class WiredHeadsetMonitor extends BroadcastReceiver {
        // See "http://developer.android.com/reference/android/content/Intent.html#ACTION_HEADSET_PLUG"
        private static final int WHSTATE_PLUG   = 1;
        private static final int WHSTATE_UNPLUG = 0;

        @Override
        public void
        onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals(Intent.ACTION_HEADSET_PLUG))
                return;

            int state = intent.getIntExtra("state", -1);
            switch (state) {
            case WHSTATE_UNPLUG:
            case WHSTATE_PLUG:
                Utils.getUiHandler().post(new Runnable() {
                    @Override
                    public void
                    run() {
                        YTPlayer.get().playerPause();
                    }
                });
                break;

            default:
                logW("Unknown WiredHeadset State : " + state);
                break;
            }
        }
    }

    private class AutoStop implements Runnable {
        @Override
        public void
        run() {
            stopVideos();
        }
    }

    private class StartVideoRecovery implements Runnable {
        private Video _mV = null;

        void
        cancel() {
            Utils.getUiHandler().removeCallbacks(this);
        }

        // USE THIS FUNCTION
        void
        executeRecoveryStart(Video v, long delays) {
            eAssert(Utils.isUiThread());
            cancel();
            _mV = v;
            if (delays > 0)
                Utils.getUiHandler().postDelayed(this, delays);
            else
                Utils.getUiHandler().post(this);
        }

        void
        executeRecoveryStart(Video aV) {
            executeRecoveryStart(aV, 0);
        }

        // DO NOT run this explicitly.
        @Override
        public
        void run() {
            eAssert(Utils.isUiThread());
            if (null != _mV)
                startVideo(_mV, true);
        }
    }


    private static class VideoListManager {
        private Video[]     _mVs = null; // video array
        private int         _mVi = -1; // video index
        private OnListChangedListener _mListener = null;

        interface OnListChangedListener {
            void onChanged(VideoListManager vm);
        }

        VideoListManager(OnListChangedListener listener) {
            _mListener = listener;
        }

        void
        setOnListChangedListener(OnListChangedListener listener) {
            eAssert(Utils.isUiThread());
            _mListener = listener;
        }

        void
        clearOnListChangedListener() {
            eAssert(Utils.isUiThread());
            _mListener = null;
        }

        void
        notifyToListChangedListener() {
            if (null != _mListener)
                _mListener.onChanged(this);
        }

        boolean
        hasActiveVideo() {
            eAssert(Utils.isUiThread());
            return null != getActiveVideo();
        }

        boolean
        hasNextVideo() {
            eAssert(Utils.isUiThread());
            return hasActiveVideo()
                   && _mVi < (_mVs.length - 1);
        }

        boolean
        hasPrevVideo() {
            eAssert(Utils.isUiThread());
            return hasActiveVideo() && 0 < _mVi;
        }

        void
        reset() {
            eAssert(Utils.isUiThread());
            _mVs = null;
            _mVi = -1;
            notifyToListChangedListener();
        }

        void
        setVideoList(Video[] vs) {
            eAssert(Utils.isUiThread());
            _mVs = vs;
            if (null == _mVs || 0 >= _mVs.length)
                reset();
            else if(_mVs.length > 0)
                _mVi = 0;
            notifyToListChangedListener();
        }

        Video[]
        getVideoList() {
            eAssert(Utils.isUiThread());
            return _mVs;
        }

        void
        appendVideo(Video vids[]) {
            eAssert(Utils.isUiThread());
            Video[] newvs = new Video[_mVs.length + vids.length];
            System.arraycopy(_mVs, 0, newvs, 0, _mVs.length);
            System.arraycopy(vids, 0, newvs, _mVs.length, vids.length);
            // assigning reference is atomic operation in JAVA!
            _mVs = newvs;
            notifyToListChangedListener();
        }

        int
        getActiveVideoIndex() {
            return _mVi;
        }

        Video
        getActiveVideo() {
            eAssert(Utils.isUiThread());
            if (null != _mVs && 0 <= _mVi && _mVi < _mVs.length)
                return _mVs[_mVi];
            return null;
        }

        Video
        getNextVideo() {
            eAssert(Utils.isUiThread());
            if (!hasNextVideo())
                return null;
            return _mVs[_mVi + 1];
        }

        boolean
        moveTo(int index) {
            eAssert(Utils.isUiThread());
            if (index < 0 || index >= _mVs.length)
                return false;
            _mVi = index;
            return true;
        }

        boolean
        moveToFist() {
            eAssert(Utils.isUiThread());
            return moveTo(0);
        }

        boolean
        moveToNext() {
            eAssert(Utils.isUiThread());
            return moveTo(_mVi + 1);
        }

        boolean
        moveToPrev() {
            eAssert(Utils.isUiThread());
            return moveTo(_mVi - 1);
        }
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    static {
        IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        WiredHeadsetMonitor receiver = new WiredHeadsetMonitor();
        Utils.getAppContext().registerReceiver(receiver, receiverFilter);
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
        // NOTE
        // DO NOT write CODE like "if (oldS == mMpS) return;"
        // most case, this is MediaPlay's state.
        // So, even if state is same, this might be the state of different MediaPlayer instance.
        // (Ex. videoA : IDLE -> videB : IDLE).
        MPState oldS = mMpS;
        mMpS = newState;

        // If main state is changed, sub-state should be reset to IDLE
        mMpSFlag = MPSTATE_FLAG_IDLE;
        onMpStateChanged(oldS, mMpSFlag, mMpS, mMpSFlag);
    }

    private MPState
    mpGetState() {
        return mMpS;
    }

    private void
    mpSetStateFlag(int newStateFlag) {
        logD("MusicPlayer : StateFlag : " + mMpSFlag + " => " + newStateFlag);
        int old = mMpSFlag;
        mMpSFlag = newStateFlag;
        onMpStateChanged(mMpS, old, mMpS, mMpSFlag);
    }

    private int
    mpGetStateFlag() {
        return mMpSFlag;
    }

    private void
    mpSetStateFlagBit(int mask) {
        mpSetStateFlag(Utils.bitSet(mMpSFlag, mask, mask));
    }

    private void
    mpClearStateFlagBit(int mask) {
        mpSetStateFlag(Utils.bitClear(mMpSFlag, mask));
    }

    private void
    initMediaPlayer(MediaPlayer mp) {
        mp.setOnBufferingUpdateListener(this);
        mp.setOnCompletionListener(this);
        mp.setOnVideoSizeChangedListener(this);
        mp.setOnSeekCompleteListener(this);
        mp.setOnErrorListener(this);
        mp.setOnInfoListener(this);
        mp.setScreenOnWhilePlaying(false);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(this);
    }

    private void
    mpNewInstance() {
        mMp = new MediaPlayer();
        mMpSurfAttached = false;
        mMpVol = Policy.DEFAULT_VIDEO_VOLUME;
        initMediaPlayer(mMp);
        mpSetState(MPState.IDLE);
        mpSetStateFlag(MPSTATE_FLAG_IDLE);
    }

    private MediaPlayer
    mpGet() {
        return mMp;
    }

    private void
    mpSetDataSource(String path) throws IOException {
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case IDLE:
            mMp.setDataSource(path);
            mpSetState(MPState.INITIALIZED);
            return;
        }

        logI("MP [" + mpGetState().name() + "] : setDataSource ignored : ");
    }

    private void
    mpPrepareAsync() {
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case INITIALIZED:
        case STOPPED:
            mpSetState(MPState.PREPARING);
            mMp.prepareAsync();
            return;
        }
        logI("MP [" + mpGetState().name() + "] : prepareAsync ignored : ");
    }

    private void
    mpRelease() {
        if (null == mMp || MPState.END == mpGetState())
            return;


        if (MPState.ERROR != mMpS && mMp.isPlaying())
            mMp.stop();

        // Why run at another thread?
        // Sometimes mMp.release takes too long time or may never return.
        // Even in this case, ANR is very annoying to user.
        // So, this is a kind of workaround for these cases.
        final MediaPlayer mp = mMp;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mpUnsetVideoSurface();
                mp.release();
            }
        }).start();
        mMp = null;
        mpSetState(MPState.END);
    }

    private void
    mpReset() {
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case IDLE:
        case INITIALIZED:
        case PREPARED_AUDIO:
        case PREPARED:
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
        case ERROR:
            mMp.reset();
            mpSetState(MPState.IDLE);
            return;
        }
        logI("MP [" + mpGetState().name() + "] : reset ignored : ");
    }

    private void
    mpSetVideoSurface(SurfaceHolder sholder) {
        if (null == mMp)
            return;

        mMp.setDisplay(sholder);
        mMpSurfAttached = (null != sholder);
    }

    private void
    mpUnsetVideoSurface() {
        if (null == mMp)
            return;

        mMp.setDisplay(null);
        mMpSurfAttached = false;
    }

    private void
    mpSetVolume(int vol) {
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case IDLE:
        case INITIALIZED:
        case PREPARED_AUDIO:
        case PREPARED:
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
            float volf = vol/100.0f;
            mMpVol = vol;
            mMp.setVolume(volf, volf);
            return;
        }

        logI("MP [" + mpGetState().name() + "] : setVolume ignored : ");
    }

    private int
    mpGetVolume() {
        switch(mpGetState()) {
        case INVALID:
        case END:
            logI("MP [" + mpGetState().name() + "] : mpGetVolume ignored : ");
            return Policy.DEFAULT_VIDEO_VOLUME;
        }
        return mMpVol;
    }

    private int
    mpGetCurrentPosition() {
        if (null == mMp)
            return 0;

        // NOTE : Android BUG
        // Android document says that 'getCurrentPosition()' can be called at 'idle' or 'initialized' state.
        // But, in ICS, experimentally, this leads MediaPlayer to 'error' state.
        // So, 'getCurrentPosition()' SHOULD NOT be called at 'idle' and 'initialized' state.
        switch (mpGetState()) {
        case PREPARED_AUDIO:
        case PREPARED:
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
            return mMp.getCurrentPosition();
        }
        logI("MP [" + mpGetState().name() + "] : getCurrentPosition ignored : ");
        return 0;
    }

    private int
    mpGetDuration() {
        if (null == mMp)
            return 0;

        switch (mpGetState()) {
        case PREPARED_AUDIO:
        case PREPARED:
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
            return mMp.getDuration();
        }
        logI("MP [" + mpGetState().name() + "] : getDuration ignored : ");
        return 0;
    }

    private int
    mpGetVideoWidth() {
        if (null == mMp)
            return 0;

        switch (mpGetState()) {
        case IDLE:
        case INITIALIZED:
        case PREPARED_AUDIO:
        case PREPARED:
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
            return mMp.getVideoWidth();
        }
        logI("MP [" + mpGetState().name() + "] : getVideoWidth ignored : ");
        return 0;

    }

    private int
    mpGetVideoHeight() {
        if (null == mMp)
            return 0;

        switch (mpGetState()) {
        case IDLE:
        case INITIALIZED:
        case PREPARED_AUDIO:
        case PREPARED:
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
            return mMp.getVideoHeight();
        }
        logI("MP [" + mpGetState().name() + "] : getVideoHeight ignored : ");
        return 0;
    }

    private boolean
    mpIsSurfaceAttached() {
        return mMpSurfAttached;
    }


    private boolean
    mpIsPlaying() {
        //logD("MPlayer - isPlaying");
        if (null == mMp)
            return false;

        return mMp.isPlaying();
    }

    private void
    mpPause() {
        logD("MPlayer - pause");
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case STARTED:
        case PAUSED:
            mMp.pause();
            mpSetState(MPState.PAUSED);
            return;
        }
        logI("MP [" + mpGetState().name() + "] : pause ignored : ");
    }

    private void
    mpSeekTo(int pos) {
        logD("MPlayer - seekTo : " + pos);
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case PREPARED_AUDIO:
        case PREPARED:
        case STARTED:
        case PAUSED:
        case PLAYBACK_COMPLETED:
            mpSetStateFlagBit(MPSTATE_FLAG_SEEKING);
            mMp.seekTo(pos);
            return;
        }
        logI("MP [" + mpGetState().name() + "] : seekTo ignored : ");
    }

    private void
    mpStart() {
        logD("MPlayer - start");
        if (null == mMp)
            return;

        if (ytpIsSuspended())
            return;

        switch (mpGetState()) {
        case PREPARED:
        case STARTED:
        case PAUSED:
        case PLAYBACK_COMPLETED:
            mMp.start();
            mpSetState(MPState.STARTED);
            return;
        }
        logI("MP [" + mpGetState().name() + "] : start ignored : ");
    }

    private void
    mpStop() {
        logD("MPlayer - stop");
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case PREPARING:
        case PREPARED_AUDIO:
        case PREPARED:
        case STARTED:
        case PAUSED:
        case STOPPED:
        case PLAYBACK_COMPLETED:
            mMp.stop();
            mpSetState(MPState.STOPPED);
            return;
        }
        logI("MP [" + mpGetState().name() + "] : stop ignored : ");
    }

    // ========================================================================
    //
    // Video Surface Control
    //
    // ========================================================================
    private boolean
    isVideoMode() {
        return null != mSurfHolder;
    }

    private void
    setSurfaceReady(boolean ready) {
        mSurfReady = ready;
    }

    private void
    setVideoSizeReady(boolean ready) {
        mVSzReady = ready;
    }

    private boolean
    isSurfaceReady() {
        return mSurfReady;
    }

    private boolean
    isVideoSizeReady() {
        return mVSzReady;
    }
    // ========================================================================
    //
    // Suspending/Resuming Control
    //
    // ========================================================================
    private void
    ytpSuspendPlaying() {
        eAssert(Utils.isUiThread());
        playerPause();
        mYtpS = YTPState.SUSPENDED;
    }

    private void
    ytpResumePlaying() {
        eAssert(Utils.isUiThread());
        mYtpS = YTPState.IDLE;
    }

    private boolean
    ytpIsSuspended() {
        eAssert(Utils.isUiThread());
        return YTPState.SUSPENDED == mYtpS;
    }

    // ========================================================================
    //
    // General Control
    //
    // ========================================================================
    private void
    onMpStateChanged(MPState from, int fromFlag,
                     MPState to,   int toFlag) {
        Iterator<PlayerStateListener> iter = mPStateLsnrl.iterator();
        while (iter.hasNext())
            iter.next().onStateChanged(from, fromFlag, to, toFlag);

        switch (to) {
        case PAUSED:
        case INVALID:
            releaseLocks();
            break;

        case STARTED:
            acquireLocks();
            break;

        case STOPPED:
            if (null != mLoader)
                mLoader.close();
            break;
        }
    }

    private boolean
    isPreparedCompletely() {
        return (!isVideoMode() && MPState.PREPARED_AUDIO == mpGetState())
                || (isVideoMode() && MPState.PREPARED_AUDIO == mpGetState()
                                  && isSurfaceReady()
                                  && isVideoSizeReady());
    }

    private int
    getVideoQualityScore() {
        switch (Utils.getPrefQuality()) {
        case LOW:
            return YTHacker.getQScorePreferLow(YTHacker.YTQUALITY_SCORE_LOWEST);

        case NORMAL:
            return YTHacker.getQScorePreferHigh(YTHacker.YTQUALITY_SCORE_MIDLOW);

        case HIGH:
            return YTHacker.getQScorePreferHigh(YTHacker.YTQUALITY_SCORE_MIDHIGH);
        }
        eAssert(false);
        return YTHacker.getQScorePreferLow(YTHacker.YTQUALITY_SCORE_LOWEST);
    }

    private static String
    getCachedVideoFilePath(String ytvid, Utils.PrefQuality quality) {
        // Only mp4 is supported by YTHacker.
        // WebM and Flv is not supported directly in Android's MediaPlayer.
        // So, Mpeg is only option we can choose.
        return Policy.APPDATA_CACHEDIR + ytvid + "-" + quality.name() + ".mp4";
    }

    private static String
    getYtVideoIdOfCachedFile(String path) {
        int idStartI = path.lastIndexOf('/') + 1;
        int idEndI   = path.lastIndexOf('-');
        eAssert(11 == path.substring(idStartI, idEndI).length());
        return path.substring(idStartI, idEndI);
    }

    private static File
    getCachedVideo(String ytvid) {
        return new File(getCachedVideoFilePath(ytvid, Utils.getPrefQuality()));
    }

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

    private void
    cachingVideo(final String vid) {
        File cacheFile = getCachedVideo(vid);
        if ((cacheFile.exists() && cacheFile.canRead())
            // previous operation is same with current request. And it is still running.
            // So, ignore current request.
            || cacheFile.getAbsolutePath().equals(mYtDnr.getCurrentTargetFile()))
            return;

        mYtDnr.close();

        mYtDnr = new YTDownloader();
        YTDownloader.DownloadDoneReceiver rcvr = new DownloadDoneReceiver() {
            @Override
            public void
            downloadDone(YTDownloader downloader, DnArg arg, Err err) {
                if (mYtDnr != downloader) {
                    downloader.close();
                    return;
                }

                int retryTag = (Integer)downloader.getTag();
                if (!(Err.NO_ERR == err || Err.YTNOT_SUPPORTED_VIDFORMAT == err)
                    && Utils.isNetworkAvailable()
                    && retryTag > 0) {
                    // retry.
                    retryTag--;
                    downloader.setTag(retryTag);
                    downloader.download(vid, getCachedVideo(vid), getVideoQualityScore(), 500);
                } else
                    downloader.close();
                // Ignore other cases even if it is fails.
            }
        };

        mYtDnr.open("", rcvr);
        // to retry in case of YTHTTPGET.
        mYtDnr.setTag(Policy.NETOWRK_CONN_RETRY);
        mYtDnr.download(vid, getCachedVideo(vid), getVideoQualityScore(),
                        Policy.YTPLAYER_CACHING_DELAY);
    }

    private void
    stopCaching() {
        mYtDnr.close();
    }

    private void
    stopCaching(final String ytvid) {
        // If current downloading video is same with current active video
        //   it's wasting operation. So, stop it!
        String dningFile = mYtDnr.getCurrentTargetFile();
        if (null == dningFile)
            return;

        String dnvid = getYtVideoIdOfCachedFile(dningFile);
        if (dnvid.equals(ytvid))
            mYtDnr.close();
    }

    private void
    cleanCache(boolean allClear) {
        if (!mVlm.hasActiveVideo())
            allClear = true;

        HashSet<String> skipSet = new HashSet<String>();
        // DO NOT delete cache directory itself!
        skipSet.add(sCacheDir.getAbsolutePath());
        if (!allClear) {
            // delete all cached videos except for
            //   current and next video.
            for (Utils.PrefQuality pq : Utils.PrefQuality.values()) {
                skipSet.add(new File(getCachedVideoFilePath(mVlm.getActiveVideo().videoId, pq)).getAbsolutePath());
                Video nextVid = mVlm.getNextVideo();
                if (null != nextVid)
                    skipSet.add(new File(getCachedVideoFilePath(nextVid.videoId, pq)).getAbsolutePath());
            }
        }
        Utils.removeFileRecursive(sCacheDir, skipSet);
    }

    private void
    prepareNext() {
        if (!mVlm.hasNextVideo()) {
            stopCaching();
            return;
        }

        cachingVideo(mVlm.getNextVideo().videoId);
    }

    private void
    preparePlayerAsync() {
        final MediaPlayer mp = mpGet();

        Utils.getUiHandler().post(new Runnable() {
            private int retry = 20;
            @Override
            public void
            run() {
                if (mp != mpGet())
                    return; // ignore preparing for old media player.

                if (retry < 0) {
                    logW("YTPlayer : video surface is never created! Preparing will be stopped.");
                    mpStop();
                    return;
                }

                if (!isVideoMode()
                    || isSurfaceReady()) {
                    mpSetVideoSurface(mSurfHolder);
                    mpPrepareAsync();
                } else {
                    --retry;
                    Utils.getUiHandler().postDelayed(this, 100);
                }
            }
        });
    }

    private void
    prepareVideoStreamingFromYtHack(YTHacker ythack) {
        YTHacker.YtVideo ytv = ythack.getVideo(getVideoQualityScore());
        if (null == ytv) {
            // Video format is not supported...
            // Just skip it with toast!
            mUi.notifyToUser(Utils.getResText(R.string.err_ytnot_supported_vidformat));
            startNext();
            return;
        }

        try {
            mpSetDataSource(ytv.url);
        } catch (IOException e) {
            logW("YTPlayer SetDataSource IOException : " + e.getMessage());
            mStartVideoRecovery.executeRecoveryStart(mVlm.getActiveVideo(), 500);
            return;
        }

        preparePlayerAsync();
    }

    private void
    prepareVideoStreaming(final String videoId) {
        logI("Prepare Video Streaming : " + videoId);

        YTHacker lastHacker = RTState.get().getLastSuccessfulHacker();
        if (null != lastHacker
            && videoId.equals(lastHacker.getYtVideoId())
            && (System.currentTimeMillis() - lastHacker.getHackTimeStamp()) < Policy.YTHACK_REUSE_TIMEOUT) {
            eAssert(lastHacker.hasHackedResult());
            // Let's try to reuse it.
            prepareVideoStreamingFromYtHack(lastHacker);
            return;
        }


        YTHacker.YtHackListener listener = new YTHacker.YtHackListener() {
            @Override
            public void
            onPreHack(YTHacker ythack, String ytvid, Object user) {
            }

            @Override
            public void
            onPostHack(final YTHacker ythack, Err result, final NetLoader loader,
                       String ytvid, Object user) {
                if (mYtHack != ythack) {
                    // Another try is already done.
                    // So, this response should be ignored.
                    logD("YTPlayer Old Youtube connection is finished. Ignored");
                    loader.close();
                    return;
                }

                mYtHack = null;
                if (null != mLoader)
                    mLoader.close();
                mLoader = loader;

                if (Err.NO_ERR != result) {
                    logW("YTPlayer YTHack Fails : " + result.name());
                    switch (result) {
                    case YTHTTPGET:
                    case IO_NET:
                        mStartVideoRecovery.executeRecoveryStart(mVlm.getActiveVideo());
                        break;

                    default:
                        mUi.notifyToUser(Utils.getResText(result.getMessage()));
                        startNext(); // Move to next video.
                    }
                    return;
                }

                prepareVideoStreamingFromYtHack(ythack);
                RTState.get().setLastSuccessfulHacker(ythack);
            }

            @Override
            public void
            onHackCancelled(YTHacker ythack, String ytvid, Object user) {
                if (mYtHack != ythack) {
                    logD("Old Youtube connection is cancelled. Ignored");
                    return;
                }

                mYtHack = null;
            }
        };
        mYtHack = new YTHacker(videoId, null, listener);
        mYtHack.startAsync();
    }

    private void
    prepareCachedVideo(File cachedVid) {
        logI("Prepare Cached video: " + cachedVid.getAbsolutePath());
        // We have cached one.
        // So play in local!
        try {
            mpSetDataSource(cachedVid.getAbsolutePath());
        } catch (IOException e) {
            // Something wrong at cached file.
            // Clean cache and try again - next time as streaming!
            cleanCache(true);
            logW("YTPlayer SetDataSource to Cached File IOException : " + e.getMessage());
            mStartVideoRecovery.executeRecoveryStart(mVlm.getActiveVideo());
            return;
        }

        preparePlayerAsync();
    }

    private void
    startVideo(Video v, boolean recovery) {
        if (null != v)
            startVideo(v.videoId, v.volume, recovery);
    }

    private void
    startVideo(final String videoId, final int volume, boolean recovery) {
        eAssert(0 <= volume && volume <= 100);

        // Reset flag regarding video size.
        setVideoSizeReady(false);

        // Clean recovery try
        mStartVideoRecovery.cancel();

        // Whenever start videos, try to clean cache.
        cleanCache(false);

        if (recovery) {
            mErrRetry--;
            if (mErrRetry <= 0) {
                if (Utils.isNetworkAvailable()) {
                    if (mVlm.hasNextVideo()) {
                        if (mVlm.hasActiveVideo()) {
                            Video v = mVlm.getActiveVideo();
                            logW("YTPlayer Recovery video play Fails");
                            logW("    ytvid : " + v.videoId);
                            logW("    title : " + v.title);
                        }
                        startNext(); // move to next video.
                    } else
                        stopPlay(StopState.UNKNOWN_ERROR);
                } else
                    stopPlay(StopState.NETWORK_UNAVAILABLE);

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

        // Update DB at this moment.
        // It's not perfectly right moment but it's fair enough
        new Thread(new Runnable() {
            @Override
            public void run() {
                mDb.updateVideo(DB.ColVideo.VIDEOID, videoId,
                                DB.ColVideo.TIME_PLAYED, System.currentTimeMillis());
            }

        }).start();

        // NOTE
        // With early-caching, in case of first video - actually not-cached video,
        //   player tries to streaming main video and simultaneously it also tries to caching next video.
        // There are two concern points regarding early-caching.
        //
        // The 1st is "System performance drop"
        // The 2nd is "Network bandwidth burden"
        //
        // 1st one can be resolve by dropping caching thread's priority.
        // (Priority of YTDownloader thread, is already set as THREAD_PRIORITY_BACKGROUND.)
        //
        // 2nd one is actually, main concern point.
        // But usually, in Wifi environment, network bandwidth is large enough versus video-bits-rate.
        // In mobile network environment, network condition is very unstable.
        // So, in general, user doens't try to high-quality-video.
        //
        // Above two reasons, caching is started as soon as video is started.
        prepareNext();
        File cachedVid = getCachedVideo(videoId);
        if (cachedVid.exists() && cachedVid.canRead())
            prepareCachedVideo(cachedVid);
        else {
            if (!Utils.isNetworkAvailable())
                mStartVideoRecovery.executeRecoveryStart(new Video(videoId, "", volume, 0), 1000);
            else
                prepareVideoStreaming(videoId);
        }
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
    startAt(int index) {
        if (!mVlm.hasActiveVideo())
            return; // do nothing

        if (mVlm.moveTo(index))
            startVideo(mVlm.getActiveVideo(), false);
        else
            stopPlay(StopState.DONE);
    }

    private void
    stopPlay(StopState st) {
        logD("YTPlayer stopPlay : " + st.name());
        if (null != mYtHack)
            mYtHack.forceCancel();

        if (StopState.DONE == st
            && Utils.isPrefRepeat()) {
            if (mVlm.moveToFist()) {
                startVideo(mVlm.getActiveVideo(), false);
                return;
            }
        }

        // Play is already stopped.
        // So, auto stop should be inactive here.
        disableAutostop();

        mpStop();
        mpRelease();
        releaseLocks();
        mVlm.reset();
        mYtDnr.close();
        mErrRetry = PLAYER_ERR_RETRY;

        // This should be called before changing title because
        //   title may be changed in onMpStateChanged().
        // We need to overwrite title message.
        mpSetState(MPState.INVALID);

        Iterator<VideosStateListener> iter = mVStateLsnrl.iterator();
        while (iter.hasNext())
            iter.next().onStopped(st);
    }

    private void
    disableAutostop() {
        Utils.getUiHandler().removeCallbacks(mAutoStop);
    }

    private void
    storePlayerState() {
        if (null == mMp)
            return;

        // NOTE
        // Even if last stored player state is not restored and used yet,
        //   new store is requested.
        // So, play need to keep last state.
        int storedPos = 0;
        int storedVol = Policy.DEFAULT_VIDEO_VOLUME;
        if (mVlm.hasActiveVideo()) {
            Long vol = (Long)mDb.getVideoInfo(mVlm.getActiveVideo().videoId, DB.ColVideo.VOLUME);
            if (null != vol)
                storedVol = vol.intValue();
        }

        if (haveStoredPlayerState()) {
            storedPos = mStoredPState.pos;
            storedVol = mStoredPState.vol;
        }
        clearStoredPlayerState();

        mStoredPState = new PlayerState();
        mStoredPState.mpState = mpGetState();
        mStoredPState.vidobj = mVlm.getActiveVideo();
        switch(mpGetState()) {
        case STARTED:
        case PAUSED:
        case PREPARED_AUDIO:
        case PREPARED:
            mStoredPState.pos = mpGetCurrentPosition();
            mStoredPState.vol = mpGetVolume();
            break;

        default:
            mStoredPState.pos = storedPos;
            mStoredPState.vol = storedVol;
        }
    }

    private void
    restorePlayerState() {
        if (!haveStoredPlayerState())
            return;

        if (mVlm.getActiveVideo() == mStoredPState.vidobj) {
            mpSeekTo(mStoredPState.pos);
            mpSetVolume(mStoredPState.vol);
        }

        clearStoredPlayerState();
    }

    private void
    clearStoredPlayerState() {
        mStoredPState = null;
    }

    private boolean
    haveStoredPlayerState() {
        return mStoredPState != null;
    }

    private boolean
    isStoredPlayerStatePaused() {
        switch (mStoredPState.mpState) {
        case PAUSED:
        case PREPARED_AUDIO:
        case PREPARED:
            return true;
        }
        return false;
    }

    // ============================================================================
    //
    // Override for "SurfaceHolder.Callback"
    //
    // ============================================================================
    @Override
    public void
    surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder != mSurfHolder)
            logW("MPlayer - surfaceCreated with invalid holder");

        logD("MPlayer - surfaceChanged : " + format + ", " + width + ", " + height);
    }

    @Override
    public void
    surfaceCreated(SurfaceHolder holder) {
        if (holder != mSurfHolder)
            logW("MPlayer - surfaceCreated with invalid holder");

        logD("MPlayer - surfaceCreated");
        if (isSurfaceReady())
            logW("MPlayer - surfaceCreated is called at [surfaceReady]");

        setSurfaceReady(true);

        if (isPreparedCompletely())
            onPreparedCompletely();
    }

    @Override
    public void
    surfaceDestroyed(SurfaceHolder holder) {
        if (holder != mSurfHolder)
            logW("MPlayer - surfaceCreated with invalid holder");

        logD("MPlayer - surfaceDestroyed");
        if (!isSurfaceReady())
            logW("MPlayer - surfaceDestroyed is called at [NOT-surfaceReady]");

        setSurfaceReady(false);
    }

    // ============================================================================
    //
    // Override for "MediaPlayer.*"
    //
    // ============================================================================
    @Override
    public void
    onBufferingUpdate (MediaPlayer mp, int percent) {
        logD("MPlayer - onBufferingUpdate : " + percent + " %");
        // See comments around MEDIA_INFO_BUFFERING_START in onInfo()
        //mpSetState(MPState.BUFFERING);
        Iterator<PlayerStateListener> iter = mPStateLsnrl.iterator();
        while (iter.hasNext())
            iter.next().onBufferingChanged(percent);
    }

    @Override
    public void
    onCompletion(MediaPlayer mp) {
        logD("MPlayer - onCompletion");
        mpSetState(MPState.PLAYBACK_COMPLETED);
        startNext();
    }

    private void
    onPreparedCompletely() {
        logD("MPlayer - onPreparedInternal");
        boolean autoStart = true;
        if (haveStoredPlayerState()) {
            autoStart = !isStoredPlayerStatePaused();
            restorePlayerState();
        }
        clearStoredPlayerState();

        mpSetState(MPState.PREPARED);

        if (autoStart)
            mpStart(); // auto start
    }

    @Override
    public void
    onPrepared(MediaPlayer mp) {
        // OnPreparedListener is very difficult to handle because of it's async-nature.
        // We may request some other works to media player even in "preparing" state
        //   - ex. Stop and start new preparation after create new media player instance.
        // Especially, in this case, player should be able to tell that which callback is for which request.
        // To do this, ytplayer should compare that current media player is prepared one or not.
        if (mp != mpGet()) {
            // ignore.
            logD("MPlayer - old invalid player is prepared.");
            return;
        }

        mpSetState(MPState.PREPARED_AUDIO);
        logD("MPlayer - onPrepared - (PREPARED_AUDIO)");

        if (isPreparedCompletely())
            onPreparedCompletely();
    }

    @Override
    public void
    onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        logD("MPlayer - onVideoSizeChanged");
        setVideoSizeReady(true);

        if (isPreparedCompletely())
            onPreparedCompletely();
    }

    @Override
    public void
    onSeekComplete(MediaPlayer mp) {
        logD("MPlayer - onSeekComplete");
        if (mp != mpGet())
            return;

        mpClearStateFlagBit(MPSTATE_FLAG_SEEKING);
    }

    @Override
    public boolean
    onError(MediaPlayer mp, int what, int extra) {
        boolean tryAgain = true;
        MPState origState = mpGetState();
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

        if (tryAgain
            && mVlm.hasActiveVideo()
            && (MPState.INITIALIZED == origState
                || MPState.PREPARING == origState)) {
            logD("MPlayer - Try to recover!");
            startVideo(mVlm.getActiveVideo(), true);
        } else {
            if (!haveStoredPlayerState()) {
                logI("MPlayer - not-recoverable error : " + what + "/" + extra);
                stopPlay(StopState.UNKNOWN_ERROR);
            }
        }

        return true; // DO NOT call onComplete Listener.
    }

    @Override
    public boolean
    onInfo(MediaPlayer mp, int what, int extra) {
        logD("MPlayer - onInfo : " + what);
        switch (what) {
        case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
            break;

        case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            break;

        case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            break;

        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            mpSetStateFlagBit(MPSTATE_FLAG_BUFFERING);
            break;

        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            mpClearStateFlagBit(MPSTATE_FLAG_BUFFERING);
            break;

        case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            break;

        case MediaPlayer.MEDIA_INFO_UNKNOWN:
            break;

        default:
        }
        return false;
    }

    // ============================================================================
    //
    // Package interfaces (Usually for YTPLayerUI)
    //
    // ============================================================================
    boolean
    hasNextVideo() {
        return mVlm.hasNextVideo();
    }

    void
    startNextVideo() {
        startNext();
    }

    boolean
    hasPrevVideo() {
        return mVlm.hasPrevVideo();
    }

    void
    startPrevVideo() {
        startPrev();
    }

    void
    startVideoAt(int videoListIndex) {
        startAt(videoListIndex);
    }

    Video
    getActiveVideo() {
        return mVlm.getActiveVideo();
    }

    Video[]
    getVideoList() {
        return mVlm.getVideoList();
    }

    int
    getActiveVideoIndex() {
        return mVlm.getActiveVideoIndex();
    }

    MPState
    playerGetState() {
        return mpGetState();
    }

    int
    playerGetStateFlag() {
        return mpGetStateFlag();
    }

    void
    playerStart() {
        if (isVideoPlaying()
            && MPState.PREPARING != mpGetState())
            mpStart();
    }

    void
    playerPause() {
        if (isVideoPlaying()
            && MPState.PREPARING != mpGetState())
            mpPause();
    }

    void
    playerStop() {
        mpStop();
    }

    /**
     *
     * @param pos
     *   milliseconds.
     */
    void
    playerSeekTo(int pos) {
        mpSeekTo(pos);
    }


    /**
     * Get duration(milliseconds) of current active video
     * @return
     */
    int
    playerGetDuration() {
        return mpGetDuration();
    }

    /**
     * Get current position(milliseconds) from start
     * @return
     */
    int
    playerGetPosition() {
        return mpGetCurrentPosition();
    }

    int
    playerGetVolume() {
        return mpGetVolume();
    }


    /**
     * Set volume of video-on-play
     * @param vol
     */
    void
    playerSetVolume(int vol) {
        eAssert(0 <= vol && vol <= 100);
        mpSetVolume(vol);
    }

    // ============================================================================
    //
    // Public interfaces
    //
    // ============================================================================
    private YTPlayer() {
        mVlm = new VideoListManager(new VideoListManager.OnListChangedListener() {
            @Override
            public void onChanged(VideoListManager vm) {
                eAssert(Utils.isUiThread());
                Iterator<VideosStateListener> iter = mVStateLsnrl.iterator();
                while (iter.hasNext())
                    iter.next().onChanged();
            }
        });
    }

    public static YTPlayer
    get() {
        if (null == sInstance)
            sInstance = new YTPlayer();
        return sInstance;
    }

    public void
    addVideosStateListener(Object key, VideosStateListener listener) {
        eAssert(null != listener);
        mVStateLsnrl.remove(key);
        mVStateLsnrl.add(key, listener);
    }

    public void
    removeVideosStateListener(Object key) {
        mVStateLsnrl.remove(key);
    }

    public void
    addPlayerStateListener(Object key, PlayerStateListener listener) {
        eAssert(null != listener);
        mPStateLsnrl.remove(key);
        mPStateLsnrl.add(key, listener);
    }

    public void
    removePlayerStateListener(Object key) {
        mPStateLsnrl.remove(key);
    }

    public void
    setSurfaceHolder(SurfaceHolder holder) {
        if (null != mSurfHolder
            && mSurfHolder != holder)
            unsetSurfaceHolder(mSurfHolder);
        mSurfHolder = holder;
        if (null != holder) {
            holder.addCallback(this);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    public void
    unsetSurfaceHolder(SurfaceHolder holder) {
        if (null != mSurfHolder
            && mSurfHolder == holder) {
            mSurfHolder.removeCallback(this);
            mSurfHolder = null;
            setSurfaceReady(false);
        }
    }

    public void
    detachVideoSurface(SurfaceHolder holder) {
        if (null != mSurfHolder
                && mSurfHolder == holder) {
            mpUnsetVideoSurface();
        }
    }

    public ToolButton
    getVideoToolButton() {
        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasActiveVideo()
                    || null == mUi.getActivity())
                        return;
                    backupPlayerState();
                    playerStop();
                    mUi.getActivity().startActivity(new Intent(mUi.getActivity(), VideoPlayerActivity.class));
            }
        };

        return new ToolButton(R.drawable.ic_media_video, onClick);
    }

    public Err
    setController(Activity  activity,
                  ViewGroup playerv,
                  ViewGroup playerLDrawer,
                  SurfaceView surfacev,
                  ToolButton toolBtn) {
        Err err = mUi.setController(activity, playerv, playerLDrawer, surfacev, toolBtn);

        if (!mVlm.hasActiveVideo())
            return err;

        // controller is set again.
        // Than new surface may have to be used.
        // This is completely DIRTY and RISKY...
        // Until to find any other better way...
        // (I HATE THIS KIND OF HACK!!! :-( )
        if (!haveStoredPlayerState()
            && null != surfacev
            && null != mSurfHolder
            && surfacev.getHolder() == mSurfHolder
            && !mpIsSurfaceAttached()) {
            // Video surface should be set again.
            // This is Android MediaPlayer's constraints.
            // Video SHOULD BE re-started!
            backupPlayerState();
            mpStop();
        }

        if (haveStoredPlayerState())
            startVideo(mVlm.getActiveVideo(), false);

        return err;
    }

    public void
    unsetController(Activity  activity) {
        mUi.unsetController(activity);
    }

    public void
    changeVideoVolume(final String title, final String videoId) {
        mUi.changeVideoVolume(title, videoId);
    }

    public boolean
    hasActiveVideo() {
        return mVlm.hasActiveVideo();
    }

    public boolean
    isVideoPlaying() {
        return mVlm.hasActiveVideo()
               && MPState.ERROR != mMpS
               && MPState.END != mMpS;
    }

    public boolean
    isPlayerSeeking(int stateFlag) {
        return Utils.bitIsSet(mpGetStateFlag(), MPSTATE_FLAG_SEEKING);
    }

    public boolean
    isPlayerBuffering(int stateFlag) {
        return Utils.bitIsSet(mpGetStateFlag(), MPSTATE_FLAG_BUFFERING);
    }

    /**
     * Get volume of video-on-play
     * @return
     *   -1 : for error
     */
    public int
    getVideoVolume() {
        if (isVideoPlaying())
            return mpGetVolume();
        return DB.INVALID_VOLUME;
    }

    public void
    restartFromCurrentPosition() {
        if (!mVlm.hasActiveVideo())
            return;
        backupPlayerState();
        startVideo(mVlm.getActiveVideo(), false);
    }

    public void
    startVideos(final Video[] vs) {
        eAssert(Utils.isUiThread());

        if (null == vs || vs.length <= 0)
            return;

        acquireLocks();
        clearStoredPlayerState();
        // removes auto stop that is set before.
        disableAutostop();
        mVlm.setVideoList(vs);

        if (mVlm.moveToFist()) {
            startVideo(mVlm.getActiveVideo(), false);
            Iterator<VideosStateListener> iter = mVStateLsnrl.iterator();
            while (iter.hasNext())
                iter.next().onStarted();
        }
    }

    public void
    startVideos(final Cursor c,
                final int coliUrl,      final int coliTitle,
                final int coliVolume,   final int coliPlaytime,
                final boolean shuffle) {
        eAssert(Utils.isUiThread());

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
    backupPlayerState() {
        storePlayerState();
    }

    public void
    appendToPlayQ(Video[] vids) {
        if (mVlm.hasActiveVideo())
            mVlm.appendVideo(vids);
        else
            startVideos(vids);
    }

    /**
     * player will be stopped after 'millis'
     * @param millis
     *   0 for disable autostop
     */
    public void
    setAutoStop(long millis) {
        Utils.getUiHandler().removeCallbacks(mAutoStop);
        if (mVlm.hasActiveVideo()) {
            if (millis > 0)
                Utils.getUiHandler().postDelayed(mAutoStop, millis);
        }
    }

    public void
    stopVideos() {
        stopPlay(StopState.FORCE_STOPPED);
    }

    public String
    getPlayVideoYtId() {
        if (isVideoPlaying())
            return mVlm.getActiveVideo().videoId;
        return null;
    }

    public int
    getVideoWidth() {
        return mpGetVideoWidth();
    }

    public int
    getVideoHeight() {
        return mpGetVideoHeight();
    }

    public int
    getProgressPercent() {
        int progPercent = 0;
        if (mVlm.hasActiveVideo() && mpGetDuration() > 0)
            progPercent = (int)(mpGetCurrentPosition() * 100L / mpGetDuration());

        return progPercent;
    }
}
