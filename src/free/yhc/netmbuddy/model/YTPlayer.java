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
import java.util.Random;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.model.DB.ColVideo;
import free.yhc.netmbuddy.model.YTDownloader.DnArg;
import free.yhc.netmbuddy.model.YTDownloader.DownloadDoneReceiver;

public class YTPlayer implements
MediaPlayer.OnBufferingUpdateListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener,
MediaPlayer.OnInfoListener,
MediaPlayer.OnVideoSizeChangedListener,
MediaPlayer.OnSeekCompleteListener {
    private static final String WLTAG               = "YTPlayer";
    private static final int    PLAYER_ERR_RETRY    = Policy.YTPLAYER_RETRY_ON_ERROR;
    private static final int    SEEKBAR_MAX         = 1000;

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

    private final Resources     mRes        = Utils.getAppContext().getResources();
    private final DB            mDb         = DB.get();

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------
    private final UpdateProgress    mUpdateProg = new UpdateProgress();
    private final AutoStop          mAutoStop   = new AutoStop();
    private final StartVideoRecovery    mStartVideoRecovery = new StartVideoRecovery();

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    private WakeLock            mWl         = null;
    private WifiLock            mWfl        = null;
    private MediaPlayer         mMp         = null;
    private MPState             mMpS        = MPState.INVALID; // state of mMp;
    private int                 mMpVol      = Policy.DEFAULT_VIDEO_VOLUME; // Current volume of media player.
    private YTHacker            mYtHack     = null;
    private NetLoader           mLoader     = null;
    // assign dummy instance to remove "if (null != mYtDnr)"
    private YTDownloader        mYtDnr      = new YTDownloader();

    // ------------------------------------------------------------------------
    // UI Control.
    // ------------------------------------------------------------------------
    private Context             mVContext   = null;
    private LinearLayout        mPlayerv    = null;
    private LinearLayout        mPlayerLDrawer   = null;

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

    private class AutoStop implements Runnable {
        @Override
        public void
        run() {
            stopVideos();
        }
    }

    private class StartVideoRecovery implements Runnable {
        private Video v = null;

        void
        cancel() {
            Utils.getUiHandler().removeCallbacks(this);
        }

        // USE THIS FUNCTION
        void
        executeRecoveryStart(Video aV, long delays) {
            eAssert(Utils.isUiThread());
            cancel();
            v = aV;
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
            if (null != v)
                startVideo(v, true);
        }
    }

    private class UpdateProgress implements Runnable {
        private SeekBar     seekbar = null;
        private TextView    curposv = null;
        private TextView    maxposv = null;
        private int         lastProgress = -1;
        private int         lastSecondaryProgress = -1; // For secondary progress

        private void
        resetProgressView() {
            if (null != seekbar) {
                maxposv.setText(Utils.secsToMinSecText(mpGetDuration() / 1000));
                update(1, 0);
                updateSecondary(0);
            }
            lastProgress = 0;
            lastSecondaryProgress = 0;
        }

        int
        getSecondaryProgressPercent() {
            int percent =  lastSecondaryProgress * 100 / SEEKBAR_MAX;
            if (percent > 100)
                percent = 100;
            if (percent < 0)
                percent = 0;
            return percent;
        }

        void
        setProgressView(ViewGroup progv) {
            eAssert(Utils.isUiThread());
            eAssert(null != progv.findViewById(R.id.mplayer_progress));
            curposv = (TextView)progv.findViewById(R.id.mplayer_curpos);
            maxposv = (TextView)progv.findViewById(R.id.mplayer_maxpos);
            seekbar = (SeekBar)progv.findViewById(R.id.mplayer_seekbar);
            if (null != seekbar) {
                maxposv.setText(Utils.secsToMinSecText(mpGetDuration() / 1000));
                update(mpGetDuration(), lastProgress);
                updateSecondary(lastSecondaryProgress);
            }
        }

        void
        start() {
            //logI("Progress Start");
            maxposv.setText(Utils.secsToMinSecText(mpGetDuration() / 1000));
            update(mpGetDuration(), lastProgress);
            updateSecondary(lastSecondaryProgress);
            run();
        }

        void
        stop() {
            //logI("Progress End");
            Utils.getUiHandler().removeCallbacks(this);
            resetProgressView();
        }

        void
        update(int durms, int curms) {
            // ignore aDuration.
            // Sometimes youtube player returns incorrect duration value!
            if (null != seekbar) {
                int curPv = (durms > 0)? (int)((long)curms * (long)SEEKBAR_MAX / durms)
                                               : 0;
                seekbar.setProgress(curPv);
                curposv.setText(Utils.secsToMinSecText(curms / 1000));
                lastProgress = curPv;
            }
        }

        /**
         * Update secondary progress
         * @param percent
         */
        void
        updateSecondary(int percent) {
            // Update secondary progress
            if (null != seekbar) {
                int pv = percent * SEEKBAR_MAX / 100;
                seekbar.setSecondaryProgress(pv);
                lastSecondaryProgress = pv;
            }
        }

        @Override
        public
        void run() {
            update(mpGetDuration(), mpGetCurrentPosition());
            Utils.getUiHandler().postDelayed(this, 1000);
        }
    }

    private static class VideoListManager {
        private Video[]     vs  = null; // video array
        private int         vi  = -1; // video index
        private OnListChangedListener lcListener = null;

        interface OnListChangedListener {
            void onListChanged(VideoListManager vlm);
        }

        void
        setOnListChangedListener(OnListChangedListener listener) {
            eAssert(Utils.isUiThread());
            lcListener = listener;
        }

        void
        clearOnListChangedListener() {
            eAssert(Utils.isUiThread());
            lcListener = null;
        }

        void
        notifyToListChangedListener() {
            if (null != lcListener)
                lcListener.onListChanged(this);
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
                   && vi < (vs.length - 1);
        }

        boolean
        hasPrevVideo() {
            eAssert(Utils.isUiThread());
            return hasActiveVideo() && 0 < vi;
        }

        void
        reset() {
            eAssert(Utils.isUiThread());
            vs = null;
            vi = -1;
            notifyToListChangedListener();
        }

        void
        setVideoList(Video[] aVs) {
            eAssert(Utils.isUiThread());
            vs = aVs;
            if (null == vs || 0 >= vs.length)
                reset();
            else if(vs.length > 0)
                vi = 0;
            notifyToListChangedListener();
        }

        Video[]
        getVideoList() {
            eAssert(Utils.isUiThread());
            return vs;
        }

        void
        appendVideo(Video v) {
            eAssert(Utils.isUiThread());
            Video[] newvs = new Video[vs.length + 1];
            System.arraycopy(vs, 0, newvs, 0, vs.length);
            newvs[newvs.length - 1] = v;
            // assigning reference is atomic operation in JAVA!
            vs = newvs;
            notifyToListChangedListener();
        }

        int
        getActiveVideoIndex() {
            return vi;
        }

        Video
        getActiveVideo() {
            eAssert(Utils.isUiThread());
            if (null != vs && 0 <= vi && vi < vs.length)
                return vs[vi];
            return null;
        }

        Video
        getNextVideo() {
            eAssert(Utils.isUiThread());
            if (!hasNextVideo())
                return null;
            return vs[vi + 1];
        }

        boolean
        moveToFist() {
            eAssert(Utils.isUiThread());
            if (hasActiveVideo()) {
                    vi = 0;
                    return true;
            }
            return false;
        }

        boolean
        moveToNext() {
            eAssert(Utils.isUiThread());
            if (hasActiveVideo()
                && vi < (vs.length - 1)) {
                vi++;
                return true;
            }
            return false;
        }

        boolean
        moveToPrev() {
            eAssert(Utils.isUiThread());
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
        mMpVol = Policy.DEFAULT_VIDEO_VOLUME;
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
    mpSetDataSource(String path) throws IOException {
        mMp.setDataSource(path);
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

        // Why run at another thread?
        // Sometimes mMp.release takes too long time or may never return.
        // Even in this case, ANR is very annoying to user.
        // So, this is a kind of workaround for these cases.
        final MediaPlayer mp = mMp;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mp.release();
            }
        }).start();
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
        if (null == mMp)
            return;

        float volf = vol/100.0f;
        mMpVol = vol;
        mMp.setVolume(volf, volf);
    }

    private int
    mpGetVolume() {
        switch(mMpS) {
        case END:
        case ERROR:
        case PREPARING:
            return Policy.DEFAULT_VIDEO_VOLUME;
        }
        return mMpVol;
    }

    private int
    mpGetCurrentPosition() {
        if (null == mMp)
            return 0;

        switch (mpGetState()) {
        case ERROR:
        case END:
            return 0;
        }
        return mMp.getCurrentPosition();
    }

    private int
    mpGetDuration() {
        if (null == mMp)
            return 0;

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
        if (null == mMp)
            return false;

        return mMp.isPlaying();
    }

    private void
    mpPause() {
        logD("MPlayer - pause");
        if (null == mMp)
            return;

        mMp.pause();
        mpSetState(MPState.PAUSED);
    }

    private void
    mpSeekTo(int pos) {
        logD("MPlayer - seekTo : " + pos);
        if (null == mMp)
            return;

        mMp.seekTo(pos);
    }

    private void
    mpStart() {
        logD("MPlayer - start");
        if (null == mMp)
            return;

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
    // Notification Handling
    //
    // ========================================================================
    private void
    notiConfigure(MPState from, MPState to) {
        NotiManager nm = NotiManager.get();
        if (!mVlm.hasActiveVideo()) {
            nm.removeNotification();
            return;
        }
        String title = mVlm.getActiveVideo().title;

        switch (to) {
        case PREPARED:
        case PAUSED:
            nm.putNotification(NotiManager.NotiType.START, title);
            break;

        case STARTED:
            nm.putNotification(NotiManager.NotiType.PAUSE, title);
            break;

        case ERROR:
            nm.putNotification(NotiManager.NotiType.ALERT, title);
            break;

        case BUFFERING:
        case INITIALIZED:
        case PREPARING:
            nm.putNotification(NotiManager.NotiType.STOP, title);
            break;

        default:
            nm.putNotification(NotiManager.NotiType.BASE, title);
        }
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
        if (0 != image)
            btn.setImageResource(image);
        btn.setVisibility(View.VISIBLE);
    }

    private void
    pvSetTitle(TextView titlev, CharSequence title) {
        if (null == titlev || null == title)
            return;
        titlev.setText(title);
    }

    private void
    pvConfigureTitle(TextView titlev, MPState from, MPState to) {
        if (null == titlev)
            return;

        CharSequence videoTitle = "";
        if (mVlm.hasActiveVideo())
            videoTitle = mVlm.getActiveVideo().title;

        switch (to) {
        case BUFFERING: {
            eAssert(null != videoTitle);
            pvSetTitle(titlev, "(" + mRes.getText(R.string.buffering) + ") " + videoTitle);
        } break;

        case PREPARED:
        case PAUSED:
        case STARTED:
            eAssert(null != videoTitle);
            if (null != videoTitle)
                pvSetTitle(titlev, videoTitle);
            break;

        case ERROR:
            pvSetTitle(titlev, mRes.getText(R.string.msg_ytplayer_err));
            break;

        default:
            if (Utils.isValidValue(videoTitle))
                pvSetTitle(titlev, "(" + mRes.getText(R.string.preparing) + ") " + videoTitle);
            else
                pvSetTitle(titlev, mRes.getText(R.string.msg_preparing_mplayer));
        }
    }


    private void
    pvDisableControlButton(ViewGroup playerv) {
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btnplay));
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btnnext));
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btnprev));
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btnvol));
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
        ImageView nextv = (ImageView)controlv.findViewById(R.id.mplayer_btnnext);
        ImageView prevv = (ImageView)controlv.findViewById(R.id.mplayer_btnprev);
        ImageView playv = (ImageView)controlv.findViewById(R.id.mplayer_btnplay);
        ImageView volv  = (ImageView)controlv.findViewById(R.id.mplayer_btnvol);

        switch (to) {
        case BUFFERING:
        case PAUSED:
        case STARTED:
            pvEnableButton(volv, 0);

            if (mVlm.hasNextVideo())
                pvEnableButton(nextv, 0);
            else
                pvDisableButton(nextv);

            if (mVlm.hasPrevVideo())
                pvEnableButton(prevv, 0);
            else
                pvDisableButton(prevv);
            break;

        default:
            pvDisableButton(volv);
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
    pvEnableLDrawer(ViewGroup playerLDrawer) {
        if (null == playerLDrawer
            || !mVlm.hasActiveVideo())
            return; // nothing to do
        eAssert(null != mVContext);

        ListView lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_ldrawer_content);
        SlidingDrawer drawer = (SlidingDrawer)playerLDrawer.findViewById(R.id.mplayer_ldrawer);
        playerLDrawer.setVisibility(View.VISIBLE);
        lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_ldrawer_content);
        YTPlayerVidArrayAdapter adapter = new YTPlayerVidArrayAdapter(mVContext, mVlm.getVideoList());
        adapter.setActiveItem(mVlm.getActiveVideoIndex());
        lv.setAdapter(adapter);
        drawer.close();
    }

    private void
    pvDisableLDrawer(ViewGroup playerLDrawer) {
        if (null == playerLDrawer
            || View.GONE == playerLDrawer.getVisibility())
            return; // nothing to do
        eAssert(null != mVContext);
        ListView lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_ldrawer_content);
        lv.setAdapter(null);
        SlidingDrawer drawer = (SlidingDrawer)playerLDrawer.findViewById(R.id.mplayer_ldrawer);
        drawer.close();
        playerLDrawer.setVisibility(View.GONE);
    }

    private void
    pvConfigureLDrawer(ViewGroup playerLDrawer, MPState from, MPState to) {
        if (!mVlm.hasActiveVideo()) {
            pvDisableLDrawer(playerLDrawer);
            return;
        }

        ListView lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_ldrawer_content);
        YTPlayerVidArrayAdapter adapter = (YTPlayerVidArrayAdapter)lv.getAdapter();
        if (null != adapter
            && mVlm.getActiveVideoIndex() != adapter.getActiveItemPos()) {
            adapter.setActiveItem(mVlm.getActiveVideoIndex());
            adapter.notifyDataSetChanged();
        }
    }

    private void
    pvConfigureAll(ViewGroup playerv, ViewGroup playerLDrawer,
                   MPState from, MPState to) {
        if (null == playerv) {
            eAssert(null == playerLDrawer);
            return; // nothing to do
        }

        pvConfigureTitle((TextView)playerv.findViewById(R.id.mplayer_title),
                          from, to);
        pvConfigureProgress((ViewGroup)playerv.findViewById(R.id.mplayer_progress),
                           from, to);
        pvConfigureControl((ViewGroup)playerv.findViewById(R.id.mplayer_control),
                           from, to);
        pvConfigureLDrawer(playerLDrawer, from, to);
    }

    private void
    pvSetupControlButton(final ViewGroup playerv) {
        ImageView btn = (ImageView)playerv.findViewById(R.id.mplayer_btnplay);
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

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnprev);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPrev();
                pvDisableControlButton(playerv);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnnext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNext();
                pvDisableControlButton(playerv);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnvol);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mVlm.hasActiveVideo())
                    return;
                changeVideoVolume(mVlm.getActiveVideo().title,
                                  mVlm.getActiveVideo().videoId);
            }
        });

    }

    private void
    pvInit(ViewGroup playerv, ViewGroup playerLDrawer) {
        ViewGroup progv = (ViewGroup)playerv.findViewById(R.id.mplayer_progress);
        SeekBar sb = (SeekBar)progv.findViewById(R.id.mplayer_seekbar);
        sb.setMax(SEEKBAR_MAX);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void
            onStopTrackingTouch(SeekBar seekBar) {
                mpSeekTo((int)((long)seekBar.getProgress() * (long)mpGetDuration() / SEEKBAR_MAX));
            }

            @Override
            public void
            onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void
            onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
        });
        mUpdateProg.setProgressView(progv);

        if (null != playerLDrawer) {
            mVlm.setOnListChangedListener(new VideoListManager.OnListChangedListener() {
                @Override
                public void
                onListChanged(VideoListManager vlm) {
                    if (null == mPlayerLDrawer)
                        return;

                    if (mVlm.hasActiveVideo()) {
                        ListView lv = (ListView)mPlayerLDrawer.findViewById(R.id.mplayer_ldrawer_content);
                        YTPlayerVidArrayAdapter adapter = (YTPlayerVidArrayAdapter)lv.getAdapter();
                        if (null != adapter) {
                            adapter.setVidArray(mVlm.getVideoList());
                            adapter.notifyDataSetChanged();
                        }
                    } else
                        pvDisableLDrawer(mPlayerLDrawer);
                }
            });

            SlidingDrawer drawer = (SlidingDrawer)playerLDrawer.findViewById(R.id.mplayer_ldrawer);
            drawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
                @Override
                public void
                onDrawerOpened() {
                    if (!mVlm.hasActiveVideo())
                        return;

                    ListView lv = (ListView)mPlayerLDrawer.findViewById(R.id.mplayer_ldrawer_content);
                    int topPos = mVlm.getActiveVideoIndex() - 1;
                    if (topPos < 0)
                        topPos = 0;
                    lv.setSelectionFromTop(topPos, 0);
                }
            });
        }

        // Enable drawer by default.
        // If there is no active video, drawer will be disabled at the configure function.
        pvEnableLDrawer(playerLDrawer);

        pvSetupControlButton(playerv);
        pvConfigureAll(playerv, playerLDrawer, MPState.INVALID, mpGetState());
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

        pvConfigureAll(mPlayerv, mPlayerLDrawer, from, to);
        notiConfigure(from, to);
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

    private int
    getVideoQualityScore() {
        switch (Utils.getPrefQuality()) {
        case LOW:
            return YTHacker.getQScorePreferLow(YTHacker.YTQUALITY_SCORE_LOWEST);

        case NORMAL:
            return YTHacker.getQScorePreferHigh(YTHacker.YTQUALITY_SCORE_MIDLOW);
        }
        eAssert(false);
        return YTHacker.getQScorePreferLow(YTHacker.YTQUALITY_SCORE_LOWEST);
    }

    private boolean
    isVideoPlaying() {
        return mVlm.hasActiveVideo()
               && MPState.ERROR != mMpS
               && MPState.END != mMpS;
    }

    private File
    getCachedVideo(String ytvid) {
        // Only mp4 is supported by YTHacker.
        // WebM and Flv is not supported directly in Android's MediaPlayer.
        // So, Mpeg is only option we can choose.
        return new File(Policy.APPDATA_CACHEDIR + ytvid + ".mp4");
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
                if (Err.NO_ERR != err
                    && Utils.isNetworkAvailable()
                    && retryTag > 0) {
                    // retry.
                    retryTag--;
                    downloader.setTag(retryTag);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {}
                    downloader.download(vid, getCachedVideo(vid), getVideoQualityScore());
                } else
                    downloader.close();
                // Ignore other cases even if it is fails.
            }
        };

        mYtDnr.open("", rcvr);
        // to retry in case of YTHTTPGET.
        mYtDnr.setTag(Policy.NETOWRK_CONN_RETRY);
        // NOTE
        // Only mp4 is supported at YTHacker!
        // So, YTDownloader also supports only mpeg4
        mYtDnr.download(vid, getCachedVideo(vid), getVideoQualityScore());
    }

    private void
    cleanCache(boolean allClear) {
        if (!mVlm.hasActiveVideo())
            return;

        HashSet<String> skipSet = new HashSet<String>();
        if (!allClear) {
            // delete all cached videos except for
            //   current and next video.
            // DO NOT delete cache directory itself!
            skipSet.add(sCacheDir.getAbsolutePath());
            skipSet.add(getCachedVideo(mVlm.getActiveVideo().videoId).getAbsolutePath());
            Video nextVid = mVlm.getNextVideo();
            if (null != nextVid)
                skipSet.add(getCachedVideo(nextVid.videoId).getAbsolutePath());
        }
        Utils.removeFileRecursive(sCacheDir, skipSet);
    }

    /**
     * player will be stopped after 'millis'
     * @param millis
     *   0 for disable autostop
     */
    private void
    setAutoStop(long millis) {
        Utils.getUiHandler().removeCallbacks(mAutoStop);
        if (millis > 0)
            Utils.getUiHandler().postDelayed(mAutoStop, millis);
    }

    private void
    prepareNext() {
        if (!mVlm.hasNextVideo())
            return;

        cachingVideo(mVlm.getNextVideo().videoId);
    }

    private void
    prepareVideoStreaming(final String videoId) {
        logI("Prepare Video Streaming : " + videoId);

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
                    logW("YTPlayer YTVideoConnector Fails : " + result.name());
                    mStartVideoRecovery.executeRecoveryStart(mVlm.getActiveVideo());
                    return;
                }

                YTHacker.YtVideo ytv = ythack.getVideo(getVideoQualityScore());
                try {
                    mpSetDataSource(Uri.parse(ytv.url));
                } catch (IOException e) {
                    logW("YTPlayer SetDataSource IOException : " + e.getMessage());
                    mStartVideoRecovery.executeRecoveryStart(mVlm.getActiveVideo(), 500);
                    return;
                }

                // Update DB at this moment.
                // It's not perfectly right moment but it's fair enough
                // But, this case can be ignored.
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mDb.updateVideo(DB.ColVideo.VIDEOID, videoId,
                                        DB.ColVideo.TIME_PLAYED, System.currentTimeMillis());
                    }

                }).start();
                mpPrepareAsync();
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
        }
        mpPrepareAsync();

        // In this case, player doens't need to buffering video.
        // So, player doens't need to wait until buffering is 100% done.
        // Therefore let's prepare next video immediately.
        prepareNext();
    }

    private void
    startVideo(Video v, boolean recovery) {
        if (null != v)
            startVideo(v.videoId, v.volume, recovery);
    }

    private void
    startVideo(final String videoId, final int volume, boolean recovery) {
        eAssert(0 <= volume && volume <= 100);

        // Clean recovery try
        mStartVideoRecovery.cancel();

        // Whenever start videos, try to clean cache.
        cleanCache(false);

        if (recovery) {
            mErrRetry--;
            if (mErrRetry <= 0) {
                if (Utils.isNetworkAvailable())
                    stopPlay(StopState.UNKNOWN_ERROR);
                else
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
    stopPlay(StopState st) {
        logD("VideoView - playDone : forceStop (" + st.name() + ")");
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
        setAutoStop(0);

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

        if (null != mPlayerv) {
            TextView titlev = (TextView)mPlayerv.findViewById(R.id.mplayer_title);
            switch (st) {
            case DONE:
                pvSetTitle(titlev, mRes.getText(R.string.msg_playing_done));
                break;

            case FORCE_STOPPED:
                pvSetTitle(titlev, mRes.getText(R.string.msg_playing_stopped));
                break;

            case NETWORK_UNAVAILABLE:
                pvSetTitle(titlev, mRes.getText(R.string.err_network_unavailable));
                break;

            case UNKNOWN_ERROR:
                pvSetTitle(titlev, mRes.getText(R.string.msg_playing_err_unknown));
                break;
            }
        }
    }
    // ============================================================================
    //
    // Package interfaces
    //
    // ============================================================================
    void
    pauseVideo() {
        if (isVideoPlaying()
            && MPState.PREPARING != mpGetState())
            mpPause();
    }

    void
    startVideo() {
        if (isVideoPlaying()
            && MPState.PREPARING != mpGetState())
            mpStart();
    }

    // ============================================================================
    //
    // Public interfaces
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
    setController(Context context, ViewGroup playerv, ViewGroup playerLDrawer) {
        // update notification by force
        notiConfigure(MPState.INVALID, mpGetState());

        if (context == mVContext && mPlayerv == playerv)
            // controller is already set for this context.
            // So, nothing to do. just return!
            return Err.NO_ERR;

        mVContext = context;
        mPlayerv = (LinearLayout)playerv;
        mPlayerLDrawer = (LinearLayout)playerLDrawer;

        if (null == mPlayerv) {
            eAssert(null == mPlayerLDrawer);
            return Err.NO_ERR;
        }

        eAssert(null != mPlayerv.findViewById(R.id.mplayer_layout_magic_id));
        pvInit(playerv, playerLDrawer);

        return Err.NO_ERR;
    }

    public void
    unsetController(Context context) {
        if (context == mVContext) {
            mPlayerv = null;
            mVContext = null;
            mPlayerLDrawer = null;
            mVlm.clearOnListChangedListener();
        }
    }

    public void
    startVideos(final Video[] vs) {
        eAssert(Utils.isUiThread());
        eAssert(null != mPlayerv);

        if (null == vs || vs.length <= 0)
            return;

        acquireLocks();
        setAutoStop(Utils.getPrefAutoStopMillis());

        mVlm.setVideoList(vs);
        pvEnableLDrawer(mPlayerLDrawer);

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

    /**
     *
     * @param v
     * @return
     *   false if error, otherwise true
     */
    public boolean
    appendToCurrentPlayQ(Video v) {
        if (!mVlm.hasActiveVideo())
            return false;

        mVlm.appendVideo(v);
        // Video list is changed.
        // So, control button need to be changed due to 'next' button.
        if (null != mPlayerv)
            pvConfigureControl((ViewGroup)mPlayerv.findViewById(R.id.mplayer_control),
                               mpGetState(), mpGetState());

        return true;
    }

    public void
    stopVideos() {
        stopPlay(StopState.FORCE_STOPPED);
    }

    public void
    changeVideoVolume(final String title, final String videoId) {
        if (null == mVContext)
            return;

        final boolean runningVideo;
        // Retrieve current volume
        int curvol = Policy.DEFAULT_VIDEO_VOLUME;
        if (isVideoPlaying()
            && mVlm.getActiveVideo().videoId.equals(videoId)) {
            runningVideo = true;
            curvol = mpGetVolume();
        } else {
            runningVideo = false;
            Long i = (Long)mDb.getVideoInfo(videoId, ColVideo.VOLUME);
            if (null != i)
                curvol = i.intValue();
        }

        ViewGroup diagv = (ViewGroup)UiUtils.inflateLayout(mVContext, R.layout.mplayer_vol_dialog);
        AlertDialog.Builder bldr = new AlertDialog.Builder(mVContext);
        bldr.setView(diagv);
        bldr.setTitle(Utils.getAppContext().getResources().getText(R.string.volume)
                      + " : " + title);
        final AlertDialog aDiag = bldr.create();

        final SeekBar sbar = (SeekBar)diagv.findViewById(R.id.seekbar);
        sbar.setMax(100);
        sbar.setProgress(curvol);
        sbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void
            onStopTrackingTouch(SeekBar seekBar) { }
            @Override
            public void
            onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void
            onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (runningVideo)
                    mpSetVolume(progress);
            }
        });

        final int oldVolume = curvol;
        aDiag.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void
            onDismiss(DialogInterface dialog) {
                int newVolume = sbar.getProgress();
                if (oldVolume == newVolume)
                    return;
                // Save to database and update adapter
                // NOTE
                // Should I consider about performance?
                // Not yet. do something when performance is issued.
                mDb.updateVideo(DB.ColVideo.VIDEOID, videoId,
                                DB.ColVideo.VOLUME, newVolume);
            }
        });
        aDiag.show();
    }

    public String
    getPlayVideoYtId() {
        if (isVideoPlaying())
            return mVlm.getActiveVideo().videoId;
        return null;
    }

    /**
     * Set volume of video-on-play
     * @param vol
     */
    public void
    setVideoVolume(int vol) {
        eAssert(0 <= vol && vol <= 100);
        // Implement this
        if (isVideoPlaying())
            mpSetVolume(vol);
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

    public boolean
    hasActiveVideo() {
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
        if (mUpdateProg.getSecondaryProgressPercent() < Policy.YTPLAYER_CACHING_TRIGGER_POINT
            && Policy.YTPLAYER_CACHING_TRIGGER_POINT <= percent)
            // This is the first moment that buffering is reached to 100%
            prepareNext();

        mUpdateProg.updateSecondary(percent);
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
            // Check is there any exceptional case regarding buffering???
            mUpdateProg.updateSecondary(100);
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
