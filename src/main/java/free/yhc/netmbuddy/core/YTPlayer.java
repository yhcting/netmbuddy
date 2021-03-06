/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015, 2016
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.exception.UnsupportedFormatException;
import free.yhc.abaselib.util.AUtil;
import free.yhc.baselib.util.FileUtil;
import free.yhc.netmbuddy.Err;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.VideoPlayerActivity;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.task.YTDownloadTask;
import free.yhc.netmbuddy.task.YTHackTask;
import free.yhc.netmbuddy.utils.Util;

public class YTPlayer implements
MediaPlayer.OnBufferingUpdateListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener,
MediaPlayer.OnInfoListener,
MediaPlayer.OnVideoSizeChangedListener,
MediaPlayer.OnSeekCompleteListener,
// To support video
SurfaceHolder.Callback,
// To support title TTS
TextToSpeech.OnInitListener,
SharedPreferences.OnSharedPreferenceChangeListener,
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTPlayer.class, Logger.LOGLV_DEFAULT);

    public static final ColVideo[] sVideoProjectionToPlay
        = new ColVideo[] { ColVideo.VIDEOID,
                           ColVideo.TITLE,
                           ColVideo.VOLUME };

    private static final int COLI_VID_YTVID = 0;
    private static final int COLI_VID_TITLE = 1;
    private static final int COLI_VID_VOLUME = 2;

    // State Flags - Package private.
    static final int MPSTATE_FLAG_IDLE = 0x0;
    static final int MPSTATE_FLAG_SEEKING = 0x1;
    static final int MPSTATE_FLAG_BUFFERING = 0x2;

    private static final String WLTAG = "YTPlayer";
    private static final int PLAYER_ERR_RETRY = PolicyConstant.YTPLAYER_RETRY_ON_ERROR;

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
            return o1.v.title.compareTo(o2.v.title);
        }
    };

    private static File sCacheDir = new File(PolicyConstant.APPDATA_CACHEDIR);
    private static YTPlayer sInstance = null;

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------
    private final DB mDb = DB.get();
    private final TaskManager mTm = TaskManager.get();
    private final YTPlayerUI mUi = new YTPlayerUI(this); // for UI control
    private final AutoStop mAutoStop = new AutoStop();
    private final StartVideoRecovery mStartVideoRecovery = new StartVideoRecovery();
    private final YTPlayerVideoListManager mVlm;

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    private WakeLock mWl = null;
    private WifiLock mWfl = null;
    private MediaPlayer mMp = null;
    // Video Player Session Id.
    // Whenever new video - even if it is same video with previous one - is started,
    //   session id is increased.
    private long mMpSessId = 0;
    private MPState mMpS = MPState.INVALID; // state of mMp;
    private int mMpSFlag = MPSTATE_FLAG_IDLE;
    private boolean mMpSurfAttached = false;
    private SurfaceHolder mSurfHolder = null; // To support video
    private boolean mSurfReady = false;
    private boolean mVSzReady = false;
    private int mMpVol = PolicyConstant.DEFAULT_VIDEO_VOLUME; // Current volume of media player.
    // mYtHackTask and mYtCachingTask are set as invalid initial instance to avoid checking 'null'
    private YTHackTask mYtHackTask;
    private YTDownloadTask mYtCachingTask;
    private TextToSpeech mTts = null;
    private TTSState mTtsState = TTSState.NOTUSED;

    // ------------------------------------------------------------------------
    // Runtime Status
    // ------------------------------------------------------------------------
    private int mErrRetry = PLAYER_ERR_RETRY;
    private YTPState mYtpS = YTPState.IDLE;
    private PlayerState mStoredPState = null;

    // ------------------------------------------------------------------------
    // Listeners
    // ------------------------------------------------------------------------
    private LinkedHashSet<VideosStateListener> mVStateLsnrl = new LinkedHashSet<>();
    private LinkedHashSet<PlayerStateListener> mPStateLsnrl = new LinkedHashSet<>();

    private final YTHackTask.EventListener<YTHackTask, Void> mYtHackTaskListener
            = new YTHackTask.EventListener<YTHackTask, Void>() {
        @Override
        public void
        onPostRun(@NonNull YTHackTask task,
                  Void result,
                  Exception ex) {
            if (null == ex) {
                prepareVideoStreamingFromYtHack(task);
            } else if (ex instanceof IOException) {
                if (DBG) P.i("Fail YTHackTask! recovery!");
                mStartVideoRecovery.executeRecoveryStart(mVlm.getActiveVideo());
            } else {
                int errmsg;
                if (ex instanceof InterruptedException)
                    errmsg = Err.INTERRUPTED.getMessage();
                else if (ex instanceof UnsupportedFormatException)
                    errmsg = Err.PARSER_UNKNOWN.getMessage();
                else
                    errmsg = Err.UNKNOWN.getMessage();
                // NOTE : Dirty!!!
                // But, extremely exceptional case that model referencing UI code
                mUi.notifyToUser(AUtil.getResString(errmsg));
            }
            startNext(); // Move to next video.
        }
    };

    private enum TaskType {
        CACHING,
        HACK,
    }

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
        void onPlayQChanged();
    }

    public interface PlayerStateListener {
        void onStateChanged(MPState from, int fromFlag,
                            MPState to,   int toFlag);
        void onBufferingChanged(int percent);
    }

    public interface OnDBUpdatedListener {
        /**
         * When DB is changed by YTPlayer.
         * So, other UI module may need to update look and feel accordingly.
         */
        void onDbUpdated(DBUpdateType type);
    }

    // see "http://developer.android.com/reference/android/media/MediaPlayer.html"
    public enum MPState {
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

    public enum StopState {
        DONE,
        FORCE_STOPPED,
        NETWORK_UNAVAILABLE,
        FAIL_PLAYING,
        UNKNOWN_ERROR
    }

    public enum DBUpdateType {
        VOLUME,
        PLAYLIST,
    }

    private enum YTPState {
        IDLE,
        SUSPENDED,
    }

    private enum TTSState {
        NOTUSED,
        PREPARING,
        READY,
    }

    public static class Video {
        public final DMVideo v;
        public final int startpos; // Starting position(milliseconds) of this video.

        public Video(String ytvid, String title, int volume, int startpos) {
            v = new DMVideo();
            v.ytvid = ytvid;
            v.title = title;
            v.volume = volume;
            this.startpos = startpos;
        }

        public Video(@NonNull DMVideo v, int startpos) {
            this.v = v;
            this.startpos = startpos;
        }

        public String infoString() {
            P.bug(null != v.ytvid && null != v.title);
            return String.format("%s(%s)", v.ytvid, v.title);
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
                if (DBG) P.w("Unexpected broadcast message");
                return;
            }

            if (TelephonyManager.EXTRA_STATE_IDLE.equals(exst)) {
                YTPlayer.get().ytpResumePlaying();
            } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(exst)
                       || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(exst)) {
                if (!YTPlayer.get().ytpIsSuspended())
                    YTPlayer.get().ytpSuspendPlaying();
            } else {
                if (DBG) P.w("Unexpected extra state : " + exst);
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

            ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (null != ni
                && ni.isConnected()) {
                if (DBG) P.v("Network connected : " + ni.getType());
                switch (ni.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    if (DBG) P.v("Network connected : WIFI");
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    if (DBG) P.v("Network connected : MOBILE");
                    break;
                }
            } else
                if (DBG) P.v("Network lost");
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
                AppEnv.getUiHandler().post(new Runnable() {
                    @Override
                    public void
                    run() {
                        YTPlayer.get().playerPause();
                    }
                });
                break;

            default:
                if (DBG) P.w("Unknown WiredHeadset State : " + state);
                break;
            }
        }
    }

    private class AutoStop implements Runnable {
        // ABSOLUTE time set when autoStop is triggered.
        // ex. 2012.Nov.11 10h 30m ...
        private long _mTm = 0;

        AutoStop() {
        }

        long
        getTime() {
            return _mTm;
        }

        /**
         *
         * @param millis <= 0 for unset autostop.
         */
        void
        set(long millis) {
            unset();
            if (mVlm.hasActiveVideo()
                && millis > 0) {
                _mTm = System.currentTimeMillis() + millis;
                mUi.updateStatusAutoStopSet(true, _mTm);
                AppEnv.getUiHandler().postDelayed(this, millis);
            }
        }

        void
        unset() {
            mUi.updateStatusAutoStopSet(false, 0);
            _mTm = 0;
            AppEnv.getUiHandler().removeCallbacks(this);
        }

        boolean
        isSet() {
            return _mTm > 0;
        }

        @Override
        public void
        run() {
            stopVideos();
            _mTm = 0;
        }
    }

    private class StartVideoRecovery implements Runnable {
        private Video _mV = null;

        void
        cancel() {
            AppEnv.getUiHandler().removeCallbacks(this);
        }

        // USE THIS FUNCTION
        void
        executeRecoveryStart(Video v, long delays) {
            P.bug(AUtil.isUiThread());
            cancel();
            _mV = v;
            if (delays > 0)
                AppEnv.getUiHandler().postDelayed(this, delays);
            else
                AppEnv.getUiHandler().post(this);
        }

        void
        executeRecoveryStart(Video aV) {
            executeRecoveryStart(aV, 0);
        }

        // DO NOT run this explicitly.
        @Override
        public void
        run() {
            P.bug(AUtil.isUiThread());
            if (null != _mV)
                startVideo(_mV, true);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////

    static {
        IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        WiredHeadsetMonitor receiver = new WiredHeadsetMonitor();
        AppEnv.getAppContext().registerReceiver(receiver, receiverFilter);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    private void
    acquireLocks() {
        if (null != mWl)
            return; // already locked nothing to do

        P.bug(null == mWfl);
        mWl = ((PowerManager)AppEnv.getAppContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WLTAG);
        // Playing youtube requires high performance wifi for high quality media play.
        mWfl = ((WifiManager)AppEnv.getAppContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WLTAG);
        mWl.acquire();
        mWfl.acquire();
    }

    private void
    releaseLocks() {
        if (null == mWl)
            return;

        P.bug(null != mWfl);
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
    private void
    mpSetState(MPState newState) {
        if (DBG) P.v("State : " + mMpS.name() + " => " + newState.name());
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
        if (DBG) P.v("StateFlag : " + mMpSFlag + " => " + newStateFlag);
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
        mpSetStateFlag(Util.bitSet(mMpSFlag, mask, mask));
    }

    private void
    mpClearStateFlagBit(int mask) {
        mpSetStateFlag(Util.bitClear(mMpSFlag, mask));
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
        P.bug(AUtil.isUiThread());
        mMp = new MediaPlayer();
        if (DBG) P.v("MP" + System.identityHashCode(mMp) + " newInstance");
        mMpSessId++;
        mMpSurfAttached = false;
        mMpVol = PolicyConstant.DEFAULT_VIDEO_VOLUME;
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
            if (DBG) P.v("MP" + System.identityHashCode(mMp) + " setDataSource");
            mMp.setDataSource(path);
            mpSetState(MPState.INITIALIZED);
            return;

        default: // ignored
        }

        if (DBG) P.v("MP [" + mpGetState().name() + "] : setDataSource ignored : ");
    }

    private void
    mpPrepareAsync() {
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case INITIALIZED:
        case STOPPED:
            mpSetState(MPState.PREPARING);
            if (DBG) P.v("MP" + System.identityHashCode(mMp) + " prepareAsync");
            mMp.prepareAsync();
            return;

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : prepareAsync ignored : ");
    }

    private void
    mpRelease() {
        if (null == mMp || MPState.END == mpGetState())
            return;


        if (MPState.ERROR != mMpS && mMp.isPlaying()) {
            if (DBG) P.v("MP" + System.identityHashCode(mMp) + " stop");
            mMp.stop();
        }

        // Why run at another thread?
        // Sometimes mMp.release takes too long time or may never return.
        // Even in this case, ANR is very annoying to user.
        // So, this is a kind of workaround for these cases.
        final MediaPlayer mp = mMp;
        new Thread(new Runnable() {
            @Override
            public void
            run() {
                mpUnsetVideoSurface();
                if (DBG) P.v("MP" + System.identityHashCode(mp) + " release");
                mp.release();
            }
        }).start();
        mMp = null;
        mpSetState(MPState.END);
    }

    private void
    mpReset() {
        if (DBG) P.v("MPlayer - reset");
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
            if (DBG) P.v("MP" + System.identityHashCode(mMp) + " reset");
            mMp.reset();
            mpSetState(MPState.IDLE);
            return;

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : reset ignored : ");
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

        default: // ignored
        }

        if (DBG) P.v("MP [" + mpGetState().name() + "] : setVolume ignored : ");
    }

    private int
    mpGetVolume() {
        switch(mpGetState()) {
        case INVALID:
        case END:
            if (DBG) P.v("MP [" + mpGetState().name() + "] : mpGetVolume ignored : ");
            return PolicyConstant.DEFAULT_VIDEO_VOLUME;

        default: // ignored
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

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : getCurrentPosition ignored : ");
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

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : getDuration ignored : ");
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

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : getVideoWidth ignored : ");
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

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : getVideoHeight ignored : ");
        return 0;
    }

    private boolean
    mpIsSurfaceAttached() {
        return mMpSurfAttached;
    }

    @SuppressWarnings("unused")
    private boolean
    mpIsPlaying() {
        return null != mMp && mMp.isPlaying();
    }

    private void
    mpPause() {
        if (DBG) P.v("MPlayer - pause");
        if (null == mMp)
            return;

        switch (mpGetState()) {
        case STARTED:
        case PAUSED:
            if (DBG) P.v("MP" + System.identityHashCode(mMp) + " pause");
            mMp.pause();
            mpSetState(MPState.PAUSED);
            return;

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : pause ignored : ");
    }

    private void
    mpSeekTo(int pos) {
        if (DBG) P.v("MPlayer - seekTo : " + pos);
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

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : seekTo ignored : ");
    }

    private void
    mpStart() {
        if (DBG) P.v("MPlayer - start");
        if (null == mMp)
            return;

        if (ytpIsSuspended())
            return;

        switch (mpGetState()) {
        case PREPARED:
        case STARTED:
        case PAUSED:
        case PLAYBACK_COMPLETED:
            if (DBG) P.v("MP" + System.identityHashCode(mMp) + " start");
            mMp.start();
            mpSetState(MPState.STARTED);
            return;

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : start ignored : ");
    }

    private void
    mpStop() {
        if (DBG) P.v("MPlayer - stop");
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
            if (DBG) P.v("MP" + System.identityHashCode(mMp) + " stop");
            mMp.stop();
            mpSetState(MPState.STOPPED);
            return;

        default: // ignored
        }
        if (DBG) P.v("MP [" + mpGetState().name() + "] : stop ignored : ");
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
        P.bug(AUtil.isUiThread());
        playerPause();
        mYtpS = YTPState.SUSPENDED;
    }

    private void
    ytpResumePlaying() {
        P.bug(AUtil.isUiThread());
        mYtpS = YTPState.IDLE;
    }

    private boolean
    ytpIsSuspended() {
        P.bug(AUtil.isUiThread());
        return YTPState.SUSPENDED == mYtpS;
    }

    // ========================================================================
    //
    // TTS Control
    //
    // ========================================================================
    private void
    ttsSetState(TTSState newState) {
        mTtsState = newState;
    }

    private TTSState
    ttsGetState() {
        return mTtsState;
    }

    private boolean
    ttsIsReady() {
        return TTSState.READY == ttsGetState();
    }

    private void
    ttsOpen() {
        if (TTSState.NOTUSED != ttsGetState()) {
            if (DBG) P.i("TTS already opened");
            return;
        }
        ttsSetState(TTSState.PREPARING);
        mTts = new TextToSpeech(AppEnv.getAppContext(), this);
    }

    private void
    ttsSpeak(final String text, final String ytvid, final Runnable followingAction) {
        if (!ttsIsReady())
            return;

        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) { }
            @Override
            public void onError(String utteranceId) { }
            @Override
            public void onDone(final String utteranceId) {
                AppEnv.getUiHandler().postDelayed(new Runnable() {
                     @Override
                     public void
                     run() {
                         Video v = mVlm.getActiveVideo();
                         // NOTE : IMPORTANT
                         // ttsSpeak->ytvid is NOT available here!
                         // This is doublely - 2nd level - nested function.
                         if (null != v
                         && utteranceId.equals(v.v.ytvid))
                             followingAction.run();
                     }
                }, PolicyConstant.YTPLAYER_TTS_MARGIN_TIME);
            }
        });
        try {
            Thread.sleep(PolicyConstant.YTPLAYER_TTS_MARGIN_TIME);
        } catch (InterruptedException ignored) {}
        HashMap<String, String> param = new HashMap<>();
        param.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ytvid);
        mTts.speak(text, TextToSpeech.QUEUE_FLUSH, param);
    }

    private void
    ttsStop() {
        if (TTSState.READY == ttsGetState()
            && null != mTts)
            mTts.stop();
    }

    private void
    ttsClose() {
        if (TTSState.NOTUSED == ttsGetState()) {
            if (DBG) P.i("TTS already closed");
            return;
        }
        ttsSetState(TTSState.NOTUSED);
        mTts.shutdown();
        mTts = null;
    }

    // Implements TextToSpeech.OnInitListener.
    @Override
    public void
    onInit(int status) {
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS) {
            // set to current locale.
            int result = mTts.setLanguage(AppEnv.getAppContext().getResources().getConfiguration().locale);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported.
                if (DBG) P.w("Language is not available.");
                // code to show toast here is... really acceptable in terms of software design?
                //UxUtil.showTextToast(AppEnv.getAppContext(), R.string.msg_couldnt_use_title_tts);
                ttsClose();
            } else {
                // The TTS engine has been successfully initialized.
                // Allow the user to press the button for the app to speak again.
                // Read to use TTS
                ttsSetState(TTSState.READY);
            }
        } else {
            // Initialization failed.
            if (DBG) P.w("Could not initialize TextToSpeech.");
            // code to show toast here is... really acceptable in terms of software design?
            //UxUtil.showTextToast(AppEnv.getAppContext(), R.string.msg_couldnt_use_title_tts);
            ttsClose();
        }
    }

    // ========================================================================
    //
    // General Control
    //
    // ========================================================================
    private void
    onMpStateChanged(MPState from, int fromFlag,
                     MPState to, int toFlag) {
        for (PlayerStateListener l : mPStateLsnrl)
            l.onStateChanged(from, fromFlag, to, toFlag);
        switch (to) {
        case PAUSED:
        case INVALID:
            releaseLocks();
            break;

        case STARTED:
            acquireLocks();
            break;

        case STOPPED:
            mYtHackTask.removeEventListener(mYtHackTaskListener);
            mTm.cancelTask(mYtHackTask, null);
            break;

        default: // ignored
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
        Util.PrefQuality pq = Util.getPrefQuality();
        //Keep below code for future refactoring.(It is always 'false')
        //noinspection ConstantConditions
        if (null == pq)
            return YTHackTask.getQScorePreferLow(mapPrefToQScore(Util.PrefQuality.LOW));
        int qscore = mapPrefToQScore(pq);
        switch (pq) {
        case LOW:
        case MIDLOW:
            return YTHackTask.getQScorePreferLow(qscore);

        case NORMAL:
        case HIGH:
        case VERYHIGH:
            return YTHackTask.getQScorePreferHigh(qscore);
        }
        P.bug(false);
        return YTHackTask.getQScorePreferLow(qscore);
    }

    private static String
    getCachedVideoFilePath(String ytvid, Util.PrefQuality quality) {
        // Only mp4 is supported by YTHackTask.
        // WebM and Flv is not supported directly in Android's MediaPlayer.
        // So, Mpeg is only option we can choose.
        return PolicyConstant.APPDATA_CACHEDIR + ytvid + "-" + quality.name() + ".mp4";
    }

    @NonNull
    private static String
    getYtvidOfCachedFile(String path) {
        int idStartI = path.lastIndexOf('/') + 1;
        int idEndI   = path.lastIndexOf('-');
        P.bug(11 == path.substring(idStartI, idEndI).length());
        return path.substring(idStartI, idEndI);
    }

    @NonNull
    static File
    getCachedVideo(String ytvid) {
        return new File(getCachedVideoFilePath(ytvid, Util.getPrefQuality()));
    }

    private void
    cachingVideo(final String ytvid, long delayms) {
        P.bug(AUtil.isUiThread());
        File cacheFile = getCachedVideo(ytvid);
        if ((cacheFile.exists() && cacheFile.canRead()) // already cached or under downloading
             || (mYtCachingTask.tmId().equals(YTDownloadTask.tmId(cacheFile))
                /* order is important. 'isReady' should be checked prior 'isRunning'
                 *   because thread state is changed from 'ready' to 'started'.
                 */
                 && (mYtCachingTask.isReady() || mYtCachingTask.isRunning())))
            return;
        mTm.cancelTask(mYtCachingTask, null);

        YTDownloadTask.Builder<YTDownloadTask.Builder> ytdnb
                = new YTDownloadTask.Builder<>(cacheFile, ytvid, getVideoQualityScore());
        final YTDownloadTask ytdnTask = ytdnb.create();
        AppEnv.getUiHandler().postDelayed(new Runnable() {
            public void
            run() {
                if (!mTm.addTask(
                        ytdnTask,
                        ytdnTask.tmId(),
                        TaskType.CACHING,
                        null))
                    // Same operation is already under running!
                    // So, ignore current request.
                    if (DBG) P.w("Caching request ignored(Already running): "
                                 + ytdnTask.getYtvid());
            }
        }, delayms);
        /* Retry mechanism when caching operation fails, is not implemented.
         * (We may need to add listener and check where success or not and do something...)
         * TODO: Is this(retry) really usful?
         */
    }

    private void
    stopCaching() {
        mTm.cancelTask(mYtCachingTask, null);
    }

    private void
    cleanCache(boolean allClear) {
        if (!mVlm.hasActiveVideo())
            allClear = true;

        HashSet<String> skipSet = new HashSet<>();
        if (!allClear) {
            // delete all cached videos except for
            //   current and next video.
            for (Util.PrefQuality pq : Util.PrefQuality.values()) {
                skipSet.add(new File(getCachedVideoFilePath(mVlm.getActiveVideo().v.ytvid, pq)).getAbsolutePath());
                Video nextVid = mVlm.getNextVideo();
                if (null != nextVid)
                    skipSet.add(new File(getCachedVideoFilePath(nextVid.v.ytvid, pq)).getAbsolutePath());
            }
        }
        FileUtil.cleanDirectory(sCacheDir, skipSet);
    }

    private void
    prepareNext() {
        if (!mVlm.hasNextVideo()) {
            stopCaching();
            return;
        }
        cachingVideo(mVlm.getNextVideo().v.ytvid, PolicyConstant.YTPLAYER_CACHING_DELAY);
    }

    private void
    preparePlayerAsync() {
        final MediaPlayer mp = mpGet();

        AppEnv.getUiHandler().post(new Runnable() {
            private int retry = 20;

            @Override
            public void
            run() {
                if (mp != mpGet())
                    return; // ignore preparing for old media player.

                if (retry < 0) {
                    if (DBG) P.w("YTPlayer : video surface is never created! Preparing will be stopped.");
                    mpStop();
                    return;
                }

                if (!isVideoMode()
                        || isSurfaceReady()) {
                    mpSetVideoSurface(mSurfHolder);
                    mpPrepareAsync();
                } else {
                    --retry;
                    AppEnv.getUiHandler().postDelayed(this, 100);
                }
            }
        });
    }

    private void
    prepareVideoStreamingFromYtHack(YTHackTask ythack) {
        YTHackTask.YtVideo ytv = ythack.getVideo(getVideoQualityScore(), false);
        if (null == ytv) {
            // Video format is not supported...
            // Just skip it with toast!
            mUi.notifyToUser(AUtil.getResString(R.string.err_ytnot_supported_vidformat));
            startNext();
            return;
        }

        try {
            mpSetDataSource(ytv.url);
        } catch (IOException e) {
            if (DBG) P.w("YTPlayer SetDataSource IOException : " + e.getMessage());
            mStartVideoRecovery.executeRecoveryStart(mVlm.getActiveVideo(), 500);
            return;
        }

        preparePlayerAsync();
    }

    private void
    prepareVideoStreaming(final String ytvid) {
        P.bug(AUtil.isUiThread());
        if (DBG) P.v("ytid : " + ytvid);

        YTHackTask hack = RTState.get().getCachedYtHack(ytvid);
        if (null != hack
                && ytvid.equals(hack.getYtvid())
                && (System.currentTimeMillis() - hack.getHackTimeStamp()) < PolicyConstant.YTHACK_REUSE_TIMEOUT) {
            P.bug(hack.hasHackedResult());
            // Let's try to reuse it.
            prepareVideoStreamingFromYtHack(hack);
            return;
        }

        YTHackTask.Builder<YTHackTask.Builder> hb
                = new YTHackTask.Builder<>(ytvid);
        final YTHackTask hackTask = hb.create();
        hackTask.addEventListener(AppEnv.getUiHandlerAdapter(), mYtHackTaskListener);
        if (mTm.addTask(
                hackTask,
                "YTPlayer.HACK:" + ytvid,
                TaskType.HACK,
                null))
            mYtHackTask = hackTask;
        else
            P.w("YTHackTask already running. Request is ignored. ID:" + ytvid);
    }

    private void
    prepareCachedVideo(File cachedVid) {
        if (DBG) P.v("video file path: " + cachedVid.getAbsolutePath());
        // We have cached one.
        // So play in local!
        try {
            mpSetDataSource(cachedVid.getAbsolutePath());
        } catch (IOException e) {
            // Something wrong at cached file.
            // Clean cache and try again - next time as streaming!
            cleanCache(true);
            if (DBG) P.w("YTPlayer SetDataSource to Cached File IOException : " + e.getMessage());
            mStartVideoRecovery.executeRecoveryStart(mVlm.getActiveVideo());
            return;
        }

        preparePlayerAsync();
    }

    private void
    startVideo(Video v, boolean recovery) {
        if (null != v)
            startVideo(v.v.ytvid, v.v.title, (int) v.v.volume, recovery);
    }

    private void
    startVideo(final String ytvid, final String title, final int volume, boolean recovery) {
        P.bug(0 <= volume && volume <= 100);

        // Reset flag regarding video size.
        setVideoSizeReady(false);

        // Clean recovery try
        mStartVideoRecovery.cancel();

        // Whenever start videos, try to clean cache.
        cleanCache(false);

        if (recovery) {
            mErrRetry--;
            if (mErrRetry <= 0) {
                if (Util.isNetworkAvailable()) {
                    mUi.notifyToUser(AUtil.getResString(R.string.err_play_video));
                    if (mVlm.hasNextVideo()) {
                        if (mVlm.hasActiveVideo()) {
                            Video v = mVlm.getActiveVideo();
                            if (DBG) P.w("YTPlayer: Recovery play fails: " + v.infoString());
                        }
                        startNext(); // move to next video.
                    } else
                        stopPlay(StopState.FAIL_PLAYING);
                } else
                    stopPlay(StopState.NETWORK_UNAVAILABLE);

                return;
            }
        } else
            mErrRetry = PLAYER_ERR_RETRY;

        mTm.cancelTask(mYtHackTask, null);

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
            public void
            run() {
                // Updating 'Recently played video' is NOT FATAL operation.
                // Updating not only seldom fails, but not fatal.
                // So, exception is ignored for this operation.
                try {
                    mDb.updateVideoTimePlayed(ytvid, System.currentTimeMillis());
                } catch (Exception ignored) { }
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
        // (Priority of YTDownloadTask thread, is already set as MIDLOW.)
        //
        // 2nd one is actually, main concern point.
        // But usually, in Wifi environment, network bandwidth is large enough versus video-bits-rate.
        // In mobile network environment, network condition is very unstable.
        // So, in general, user doens't try to high-quality-video.
        //
        // Above two reasons, caching is started as soon as video is started.
        prepareNext();

        Runnable action = new Runnable() {
            @Override
            public void
            run() {
                File cachedVid = getCachedVideo(ytvid);
                if (cachedVid.exists() && cachedVid.canRead())
                    prepareCachedVideo(cachedVid);
                else {
                    if (!Util.isNetworkAvailable())
                        mStartVideoRecovery.executeRecoveryStart(new Video(ytvid, title, volume, 0), 1000);
                    else
                        prepareVideoStreaming(ytvid);
                }
            }
        };

        if (Util.isPrefHeadTts()) {
            String text = AUtil.getResString(R.string.tts_title_head_pre) + " "
                          + title + " "
                          + AUtil.getResString(R.string.tts_title_head_post);
            ttsSpeak(text, ytvid, action);
        } else
            action.run();
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
        if (DBG) P.v("YTPlayer stopPlay : " + st.name());
        mTm.cancelTask(mYtHackTask, null);
        ttsStop();

        if (StopState.DONE == st
            && Util.isPrefRepeat()) {
            if (mVlm.moveToFist()) {
                startVideo(mVlm.getActiveVideo(), false);
                return;
            }
        }

        if (StopState.FORCE_STOPPED == st)
            mUi.setPlayerVisibility(View.GONE);

        // Play is already stopped.
        // So, auto stop should be inactive here.
        mAutoStop.unset();

        mpStop();
        mpRelease();
        releaseLocks();
        mVlm.reset();
        stopCaching();
        mErrRetry = PLAYER_ERR_RETRY;

        // This should be called before changing title because
        //   title may be changed in onMpStateChanged().
        // We need to overwrite title message.
        mpSetState(MPState.INVALID);
        for (VideosStateListener l : mVStateLsnrl)
            l.onStopped(st);
    }

    private void
    storePlayerState() {
        if (null == mpGet()
            && !mVlm.hasActiveVideo())
            return; // nothing to store

        // NOTE
        // Even if last stored player state is not restored and used yet,
        //   new store is requested.
        // So, play need to keep last state.
        int storedPos = 0;
        int storedVol = PolicyConstant.DEFAULT_VIDEO_VOLUME;
        if (mVlm.hasActiveVideo()) {
            Long vol = (Long)mDb.getVideoInfo(mVlm.getActiveVideo().v.ytvid, ColVideo.VOLUME);
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

        default: // ignored
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
        if (holder != mSurfHolder) {
            if (DBG) P.w("MPlayer - surfaceCreated with invalid holder");
        }

        if (DBG) P.v("MPlayer - surfaceChanged : " + format + ", " + width + ", " + height);
    }

    @Override
    public void
    surfaceCreated(SurfaceHolder holder) {
        if (holder != mSurfHolder) {
            if (DBG) P.w("MPlayer - surfaceCreated with invalid holder");
        }

        if (DBG) P.v("MPlayer - surfaceCreated");
        if (isSurfaceReady()) {
            if (DBG) P.w("MPlayer - surfaceCreated is called at [surfaceReady]");
        }

        setSurfaceReady(true);

        if (isPreparedCompletely())
            onPreparedCompletely();
    }

    @Override
    public void
    surfaceDestroyed(SurfaceHolder holder) {
        if (holder != mSurfHolder) {
            if (DBG) P.w("MPlayer - surfaceCreated with invalid holder");
        }

        if (DBG) P.v("MPlayer - surfaceDestroyed");
        if (!isSurfaceReady()) {
            if (DBG) P.w("MPlayer - surfaceDestroyed is called at [NOT-surfaceReady]");
        }

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
        if (DBG) P.v("MPlayer - onBufferingUpdate : " + percent + " %");
        // See comments around MEDIA_INFO_BUFFERING_START in onInfo()
        //mpSetState(MPState.BUFFERING);
        for (PlayerStateListener l : mPStateLsnrl)
            l.onBufferingChanged(percent);
    }

    @Override
    public void
    onCompletion(MediaPlayer mp) {
        if (DBG) P.v("MPlayer - onCompletion");
        mpSetState(MPState.PLAYBACK_COMPLETED);
        Runnable action = new Runnable() {
            @Override
            public void
            run() {
                startNext();
            }
        };

        if (mVlm.hasActiveVideo()
            && Util.isPrefTailTts()) {
            Video v  = mVlm.getActiveVideo();
            String text = AUtil.getResString(R.string.tts_title_tail_pre) + " "
                          + v.v.title + " "
                          + AUtil.getResString(R.string.tts_title_tail_post);
            ttsSpeak(text, v.v.ytvid, action);
        } else
            action.run();
    }

    private void
    onPreparedCompletely() {
        if (DBG) P.v("MPlayer - onPreparedInternal");
        boolean autoStart = true;
        if (haveStoredPlayerState()) {
            autoStart = !isStoredPlayerStatePaused();
            restorePlayerState();
        } else if (mVlm.hasActiveVideo())
            mpSeekTo(mVlm.getActiveVideo().startpos);
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
            if (DBG) P.v("MPlayer - old invalid player is prepared.");
            return;
        }

        mpSetState(MPState.PREPARED_AUDIO);
        if (DBG) P.v("MPlayer - onPrepared - (PREPARED_AUDIO)");

        if (isPreparedCompletely())
            onPreparedCompletely();
    }

    @Override
    public void
    onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (DBG) P.v("MPlayer - onVideoSizeChanged");
        setVideoSizeReady(true);

        if (isPreparedCompletely())
            onPreparedCompletely();
    }

    @Override
    public void
    onSeekComplete(MediaPlayer mp) {
        if (DBG) P.v("MPlayer - onSeekComplete");
        if (mp != mpGet())
            return;

        mpClearStateFlagBit(MPSTATE_FLAG_SEEKING);
    }

    @Override
    public boolean
    onError(MediaPlayer mp, int what, int extra) {
        boolean tryAgain = true;
        if (DBG) P.v("MP" + System.identityHashCode(mp) + " error");
        MPState origState = mpGetState();
        mpSetState(MPState.ERROR);
        switch (what) {
        case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
            if (DBG) P.v("MPlayer - onError : NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
            tryAgain = false;
            break;

        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
            if (DBG) P.v("MPlayer - onError : MEDIA_ERROR_SERVER_DIED");
            break;

        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
            if (DBG) P.v("MPlayer - onError : UNKNOWN");
            break;

        default:
            if (DBG) P.v("MPlayer - onError");
        }

        if (tryAgain
            && mVlm.hasActiveVideo()
            && (MPState.INITIALIZED == origState
                || MPState.PREPARING == origState)) {
            Video v = mVlm.getActiveVideo();
            if (DBG) P.i("MPlayer - Try to recover: " + v.infoString());
            startVideo(v, true);
        } else {
            if (!haveStoredPlayerState()) {
                if (DBG) P.v("MPlayer - not-recoverable error : " + what + "/" + extra);
                stopPlay(StopState.UNKNOWN_ERROR);
            }
        }

        return true; // DO NOT call onComplete Listener.
    }

    @Override
    public boolean
    onInfo(MediaPlayer mp, int what, int extra) {
        if (DBG) P.v("MPlayer - onInfo : " + what);
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
    // Override for "SharedPreferences"
    //
    // ============================================================================
    @Override
    public void
    onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!AUtil.getResString(R.string.cstitle_tts).equals(key))
            return; // don't care others

        if (Util.isPrefTtsEnabled())
            ttsOpen();
        else
            ttsClose();
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

    void
    removeVideo(String ytvid) {
        int avi = mVlm.getActiveVideoIndex();
        avi = mVlm.findVideoExcept(avi, ytvid);
        if (mVlm.isValidVideoIndex(avi)) {
            startAt(avi);
            mVlm.removeVideo(ytvid);
        } else {
            // remove first, and then stop to avoid 'repeat' in case that 'repeat' preference is set.
            mVlm.removeVideo(ytvid);
            // There is no video to play after remove.
            // Just stop playing.
            stopPlay(StopState.DONE);
        }
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
        mTm.cancelTask(mYtHackTask, null);
        mpStop();
    }

    /**
     * Get duration(milliseconds) of current active video
     */
    int
    playerGetDuration() {
        return mpGetDuration();
    }

    /**
     * Get current position(milliseconds) from start
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
     */
    void
    playerSetVolume(int vol) {
        P.bug(0 <= vol && vol <= 100);
        mpSetVolume(vol);
    }

    boolean
    isAutoStopSet() {
        return mAutoStop.isSet();
    }

    /**
     *
     * @return
     *   absolute time (NOT time gap since now.)
     */
    long
    getAutoStopTime() {
        return mAutoStop.getTime();
    }

    // ============================================================================
    //
    // Public interfaces
    //
    // ============================================================================
    private YTPlayer() {
        UnexpectedExceptionHandler.get().registerModule(this);
        // mYtHackTask and mYtCachingTask are set as invalid initial instance to avoid checking 'null'
        YTHackTask.Builder<YTHackTask.Builder> hb
                = new YTHackTask.Builder<>("");
        mYtHackTask = hb.create();
        YTDownloadTask.Builder<YTDownloadTask.Builder> dnb
                = new YTDownloadTask.Builder<>(new File(""), "", 0);
        mYtCachingTask = dnb.create();
        mVlm = new YTPlayerVideoListManager(new YTPlayerVideoListManager.OnListChangedListener() {
            @Override
            public void
            onChanged(YTPlayerVideoListManager vm) {
                P.bug(AUtil.isUiThread());
                mUi.updateLDrawerList();
                for (VideosStateListener l : mVStateLsnrl)
                    l.onPlayQChanged();
            }
        });

        // Check TTS usage
        if (Util.isPrefTtsEnabled())
            ttsOpen();

        PreferenceManager.getDefaultSharedPreferences(AppEnv.getAppContext())
                         .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    public static YTPlayer
    get() {
        if (null == sInstance)
            sInstance = new YTPlayer();
        return sInstance;
    }

    public static int
    mapPrefToQScore(Util.PrefQuality prefq) {
        if (null == prefq) {
            P.bug(false);
            return YTHackTask.YTQUALITY_SCORE_LOWEST;
        }

        switch (prefq) {
        case LOW:
            return YTHackTask.YTQUALITY_SCORE_LOWEST;

        case MIDLOW:
            return YTHackTask.YTQUALITY_SCORE_LOW;

        case NORMAL:
            return YTHackTask.YTQUALITY_SCORE_MIDLOW;

        case HIGH:
            return YTHackTask.YTQUALITY_SCORE_HIGH;

        case VERYHIGH:
            return YTHackTask.YTQUALITY_SCORE_HIGHEST;
        }
        P.bug(false);
        return YTHackTask.YTQUALITY_SCORE_LOWEST;
    }

    /**
     * Closing cursor is caller's responsibility.
     * @param c Cursor that is created by using "sVideoProjectionToPlay"
     *          Closing cursor is this function's responsibility.
     */
    public static Video[]
    getVideos(Cursor c, boolean shuffle) {
        if (!c.moveToFirst())
            return new Video[0];

        Video[] vs = new Video[c.getCount()];
        int i = 0;
        do {
            vs[i++] = new Video(c.getString(COLI_VID_YTVID),
            c.getString(COLI_VID_TITLE),
            c.getInt(COLI_VID_VOLUME),
            0);
        } while (c.moveToNext());

        if (!shuffle)
            Arrays.sort(vs, sVideoTitleComparator);
        else {
            // This is shuffled case!
            Random r = new Random(System.currentTimeMillis());
            NrElem[] nes = new NrElem[vs.length];
            for (i = 0; i < nes.length; i++)
                nes[i] = new NrElem(r.nextInt(), vs[i]);
            Arrays.sort(nes, sNrElemComparator);
            for (i = 0; i < nes.length; i++)
                vs[i] = (Video)nes[i].tag;
        }
        return vs;
    }

    public void
    addVideosStateListener(VideosStateListener listener) {
        P.bug(null != listener);
        mVStateLsnrl.add(listener);
    }

    public void
    removeVideosStateListener(VideosStateListener listener) {
        mVStateLsnrl.remove(listener);
    }

    public void
    addPlayerStateListener(PlayerStateListener listener) {
        P.bug(null != listener);
        mPStateLsnrl.add(listener);
    }

    public void
    removePlayerStateListener(PlayerStateListener listener) {
        mPStateLsnrl.remove(listener);
    }


    public void
    addOnDbUpdatedListener(YTPlayer.OnDBUpdatedListener listener) {
        mUi.addOnDbUpdatedListener(listener);
    }

    public void
    removeOnDbUpdatedListener(YTPlayer.OnDBUpdatedListener listener) {
        mUi.removeOnDbUpdatedListener(listener);
    }

    public void
    setSurfaceHolder(SurfaceHolder holder) {
        if (null != mSurfHolder
            && mSurfHolder != holder)
            unsetSurfaceHolder(mSurfHolder);
        mSurfHolder = holder;
        if (null != holder)
            holder.addCallback(this);
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
            public void
            onClick(View v) {
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

    public void
    setController(Activity  activity,
                  ViewGroup playerv,
                  ViewGroup playerLDrawer,
                  SurfaceView surfacev,
                  ToolButton toolBtn) {
        mUi.setController(activity, playerv, playerLDrawer, surfacev, toolBtn);

        if (!mVlm.hasActiveVideo())
            return;

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
    }

    public void
    unsetController(Activity  activity) {
        mUi.unsetController(activity);
    }

    public void
    changeVideoVolume(final String title, final String ytvid) {
        mUi.changeVideoVolume(title, ytvid);
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
    isPlayerSeeking() {
        return Util.bitIsSet(mpGetStateFlag(), MPSTATE_FLAG_SEEKING);
    }

    public boolean
    isPlayerBuffering() {
        return Util.bitIsSet(mpGetStateFlag(), MPSTATE_FLAG_BUFFERING);
    }

    /**
     * Get volume of video-on-play
     * @return -1 : for error
     */
    public int
    getVideoVolume() {
        if (isVideoPlaying())
            return mpGetVolume();
        return DB.INVALID_VOLUME;
    }

    /**
     *
     * @param pos milliseconds.
     */
    public void
    playerSeekTo(int pos) {
        mpSeekTo(pos);
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
        P.bug(AUtil.isUiThread());

        if (null == vs || vs.length <= 0)
            return;

        acquireLocks();
        clearStoredPlayerState();
        // removes auto stop that is set before.
        mAutoStop.unset();
        mVlm.setVideoList(vs);

        if (mVlm.moveToFist()) {
            startVideo(mVlm.getActiveVideo(), false);
            for (VideosStateListener l : mVStateLsnrl)
                l.onStarted();
            mUi.setPlayerVisibility(View.VISIBLE);
        }
    }

    /**
     * @param c Cursor that is created by using "sVideoProjectionToPlay"
     *          Closing cursor is this function's responsibility.
     */
    public void
    startVideos(final Cursor c, final boolean shuffle) {
        P.bug(AUtil.isUiThread());

        new Thread(new Runnable() {
            @Override
            public void
            run() {
                final Video[] vs = getVideos(c, shuffle);
                AppEnv.getUiHandler().post(new Runnable() {
                    @Override
                    public void
                    run() {
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
     * @param millis 0 for unset autostop
     */
    public void
    setAutoStop(long millis) {
        mAutoStop.set(millis);
    }

    public void
    unsetAutoStop() {
        mAutoStop.unset();
    }

    public void
    stopVideos() {
        if (mVlm.hasActiveVideo())
            stopPlay(StopState.FORCE_STOPPED);
    }

    /**
     * Player session id.
     * Even if same video is re-played, session id is different.
     */
    public long
    getPlayerSessionId() {
        return mMpSessId;
    }

    public MPState
    getPlayerState() {
        return mMpS;
    }

    public String
    getActiveVideoYtId() {
        if (isVideoPlaying())
            return mVlm.getActiveVideo().v.ytvid;
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

    @SuppressWarnings("unused")
    public int
    getProgressPercent() {
        int progPercent = 0;
        if (mVlm.hasActiveVideo() && mpGetDuration() > 0)
            progPercent = (int)(mpGetCurrentPosition() * 100L / mpGetDuration());

        return progPercent;
    }
}
