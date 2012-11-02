package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.model.Utils.eAssert;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import free.yhc.netmbuddy.R;

public class YTPlayerUI {
    private static final int    SEEKBAR_MAX         = 1000;

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------
    private final Resources         mRes        = Utils.getAppContext().getResources();
    private final DB                mDb         = DB.get();
    private final UpdateProgress    mUpdateProg = new UpdateProgress();
    private final YTPlayer          mMp;


    // ------------------------------------------------------------------------
    // UI Control.
    // ------------------------------------------------------------------------
    private Activity            mVActivity      = null;
    private LinearLayout        mPlayerv        = null;
    private LinearLayout        mPlayerLDrawer  = null;

    // To support video
    private SurfaceView         mSurfacev       = null;
    // For extra Button
    private YTPlayer.ToolButton mToolBtn        = null;

    private class UpdateProgress implements Runnable {
        private static final int UPDATE_INTERVAL_MS = 1000;

        private SeekBar     seekbar = null;
        private TextView    curposv = null;
        private TextView    maxposv = null;
        private int         lastProgress = -1;
        private int         lastSecondaryProgress = -1; // For secondary progress

        private void
        resetProgressView() {
            if (null != seekbar) {
                maxposv.setText(Utils.secsToMinSecText(mMp.playerGetDuration() / 1000));
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
                maxposv.setText(Utils.secsToMinSecText(mMp.playerGetDuration() / 1000));
                update(mMp.playerGetDuration(), lastProgress);
                updateSecondary(lastSecondaryProgress);
            }
        }

        void
        start() {
            //logI("Progress Start");
            maxposv.setText(Utils.secsToMinSecText(mMp.playerGetDuration() / 1000));
            update(mMp.playerGetDuration(), lastProgress);
            updateSecondary(lastSecondaryProgress);
            run();
        }

        void
        resume() {
            Utils.getUiHandler().removeCallbacks(this);
            Utils.getUiHandler().postDelayed(this, UPDATE_INTERVAL_MS);
        }

        void
        pause() {
            Utils.getUiHandler().removeCallbacks(this);
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
                int curPv = (durms > 0)? (int)(curms * (long)SEEKBAR_MAX / durms)
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
            update(mMp.playerGetDuration(), mMp.playerGetPosition());
            Utils.getUiHandler().postDelayed(this, UPDATE_INTERVAL_MS);
        }
    }

    // ========================================================================
    //
    // Notification Handling
    //
    // ========================================================================
    private void
    notiConfigure(YTPlayer.MPState from, int fromFlag,
                  YTPlayer.MPState to,   int toFlag) {
        NotiManager nm = NotiManager.get();
        if (!mMp.hasActiveVideo()) {
            nm.removeNotification();
            return;
        }
        String title = mMp.getActiveVideo().title;

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

        case IDLE:
        case INITIALIZED:
        case PREPARED_AUDIO:
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
    pvConfigureTitle(TextView titlev,
                     YTPlayer.MPState from, int fromFlag,
                     YTPlayer.MPState to,   int toFlag) {
        if (null == titlev)
            return;

        CharSequence videoTitle = "";
        if (mMp.hasActiveVideo())
            videoTitle = mMp.getActiveVideo().title;

        switch (to) {
        case PREPARED:
        case PAUSED:
        case STARTED:
            eAssert(null != videoTitle);
            if (mMp.isPlayerBuffering(toFlag)
                || mMp.isPlayerSeeking(toFlag))
                videoTitle = "(" + mRes.getText(R.string.buffering) + ") " + videoTitle;

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
                pvSetTitle(titlev, "");
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
    pvConfigureControl(ViewGroup controlv,
                       YTPlayer.MPState from, int fromFlag,
                       YTPlayer.MPState to,   int toFlag) {
        if (null == controlv)
            return;

        if (!mMp.hasActiveVideo()) {
            controlv.setVisibility(View.GONE);
            return;
        }

        controlv.setVisibility(View.VISIBLE);
        ImageView nextv = (ImageView)controlv.findViewById(R.id.mplayer_btnnext);
        ImageView prevv = (ImageView)controlv.findViewById(R.id.mplayer_btnprev);
        ImageView playv = (ImageView)controlv.findViewById(R.id.mplayer_btnplay);
        ImageView volv  = (ImageView)controlv.findViewById(R.id.mplayer_btnvol);

        switch (to) {
        case PREPARED_AUDIO:
        case PREPARED:
        case PAUSED:
        case STARTED:
            pvEnableButton(volv, 0);
            // break is missed intentionally.
        case IDLE:
        case INITIALIZED:
        case PREPARING:
            if (mMp.hasNextVideo())
                pvEnableButton(nextv, 0);
            else
                pvDisableButton(nextv);

            if (mMp.hasPrevVideo())
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
            playv.setTag(YTPlayer.MPState.STARTED);
            break;

        case STARTED:
            pvEnableButton(playv, R.drawable.ic_media_pause);
            // Set next state be moved to on click as 'Tag'
            playv.setTag(YTPlayer.MPState.PAUSED);
            break;

        case IDLE:
        case INITIALIZED:
        case PREPARED_AUDIO:
        case PREPARING:
            pvEnableButton(playv, R.drawable.ic_media_stop);
            // Set next state be moved to on click as 'Tag'
            playv.setTag(YTPlayer.MPState.STOPPED);
            break;

        default:
            playv.setTag(null);
            controlv.setVisibility(View.GONE);
        }
    }

    private void
    pvConfigureProgress(ViewGroup progressv,
                        YTPlayer.MPState from, int fromFlag,
                        YTPlayer.MPState to,   int toFlag) {

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

        case STARTED:
            mUpdateProg.resume();
            break;

        case PAUSED:
            mUpdateProg.pause();
            break;

        case INITIALIZED:
        case PREPARING:
        case PREPARED_AUDIO:
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
            || !mMp.hasActiveVideo())
            return; // nothing to do
        eAssert(null != mVActivity);

        ListView lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_list);
        SlidingDrawer drawer = (SlidingDrawer)playerLDrawer.findViewById(R.id.mplayer_ldrawer);
        playerLDrawer.setVisibility(View.VISIBLE);
        YTPlayerVidArrayAdapter adapter = new YTPlayerVidArrayAdapter(mVActivity, mMp.getVideoList());
        adapter.setActiveItem(mMp.getActiveVideoIndex());
        lv.setAdapter(adapter);
        drawer.close();
    }

    private void
    pvDisableLDrawer(ViewGroup playerLDrawer) {
        if (null == playerLDrawer
            || View.GONE == playerLDrawer.getVisibility())
            return; // nothing to do
        eAssert(null != mVActivity);
        ListView lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_list);
        lv.setAdapter(null);
        SlidingDrawer drawer = (SlidingDrawer)playerLDrawer.findViewById(R.id.mplayer_ldrawer);
        drawer.close();
        playerLDrawer.setVisibility(View.GONE);
    }

    private void
    pvConfigureLDrawer(ViewGroup playerLDrawer,
                      YTPlayer.MPState from, int fromFlag,
                      YTPlayer.MPState to,   int toFlag) {
        if (null == playerLDrawer)
            return;

        if (!mMp.hasActiveVideo()) {
            pvDisableLDrawer(playerLDrawer);
            return;
        }

        ListView lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_list);
        YTPlayerVidArrayAdapter adapter = (YTPlayerVidArrayAdapter)lv.getAdapter();
        if (null != adapter
            && mMp.getActiveVideoIndex() != adapter.getActiveItemPos()) {
            adapter.setActiveItem(mMp.getActiveVideoIndex());
        }
    }

    private void
    pvConfigureAll(ViewGroup playerv, ViewGroup playerLDrawer,
                   YTPlayer.MPState from, int fromFlag,
                   YTPlayer.MPState to,   int toFlag) {
        if (null == playerv) {
            eAssert(null == playerLDrawer);
            return; // nothing to do
        }

        pvConfigureTitle((TextView)playerv.findViewById(R.id.mplayer_title),
                          from, fromFlag, to, toFlag);
        pvConfigureProgress((ViewGroup)playerv.findViewById(R.id.mplayer_progress),
                            from, fromFlag, to, toFlag);
        pvConfigureControl((ViewGroup)playerv.findViewById(R.id.mplayer_control),
                           from, fromFlag, to, toFlag);
        pvConfigureLDrawer(playerLDrawer, from, fromFlag, to, toFlag);
    }

    private void
    pvSetupControlButton(final ViewGroup playerv) {
        ImageView btn = (ImageView)playerv.findViewById(R.id.mplayer_btnplay);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // See pvConfigControl() for details.
                YTPlayer.MPState nextst = (YTPlayer.MPState)v.getTag();
                if (null == nextst)
                    return; // Nothing to do.

                switch (nextst) {
                case STARTED:
                    mMp.playerStart();
                    break;

                case PAUSED:
                    mMp.playerPause();
                    // prevent clickable during transition player state.
                    break;

                case STOPPED:
                    // This doesn't means "Stop only this video",
                    //   but means stop playing vidoes - previous user request.
                    mMp.stopVideos();
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
                mMp.startPrevVideo();
                pvDisableControlButton(playerv);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnnext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMp.startNextVideo();
                pvDisableControlButton(playerv);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnvol);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMp.hasActiveVideo())
                    return;
                changeVideoVolume(mMp.getActiveVideo().title,
                                  mMp.getActiveVideo().videoId);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnextra);
        if (null == mToolBtn)
            btn.setVisibility(View.INVISIBLE);
        else {
            btn.setImageResource(mToolBtn.drawable);
            btn.setOnClickListener(mToolBtn.onClick);
        }
    }

    private void
    pvInit(ViewGroup playerv, ViewGroup playerLDrawer, SurfaceView surfacev) {
        mMp.addPlayerStateListener(this, new YTPlayer.PlayerStateListener() {
            @Override
            public void onStateChanged(YTPlayer.MPState from, int fromFlag,
                                       YTPlayer.MPState to,   int toFlag) {
                pvConfigureAll(mPlayerv, mPlayerLDrawer, from, fromFlag, to, toFlag);
                notiConfigure(from, fromFlag, to, toFlag);
            }

            @Override
            public void onBufferingChanged(int percent) {
                mUpdateProg.updateSecondary(percent);
            }
        });

        ViewGroup progv = (ViewGroup)playerv.findViewById(R.id.mplayer_progress);
        SeekBar sb = (SeekBar)progv.findViewById(R.id.mplayer_seekbar);
        sb.setMax(SEEKBAR_MAX);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void
            onStopTrackingTouch(SeekBar seekBar) {
                mMp.playerSeekTo((int)(seekBar.getProgress() * (long)mMp.playerGetDuration() / SEEKBAR_MAX));
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
            ListView lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_list);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void
                onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (!mMp.hasActiveVideo())
                        return;
                    mMp.startVideoAt(position);
                }
            });

            mMp.addVideosStateListener(this, new YTPlayer.VideosStateListener() {
                @Override
                public void onStopped(YTPlayer.StopState state) {
                    boolean      needToNotification = true;
                    CharSequence msg = "";
                    switch (state) {
                    case DONE:
                        needToNotification = false;
                        msg = mRes.getText(R.string.msg_playing_done);
                        break;

                    case FORCE_STOPPED:
                        needToNotification = false;
                        msg = mRes.getText(R.string.msg_playing_stopped);
                        break;

                    case NETWORK_UNAVAILABLE:
                        msg = mRes.getText(R.string.err_network_unavailable);
                        break;

                    case UNKNOWN_ERROR:
                        msg = mRes.getText(R.string.msg_playing_err_unknown);
                        break;
                    }

                    if (null != mPlayerv) {
                        TextView titlev = (TextView)mPlayerv.findViewById(R.id.mplayer_title);
                        pvSetTitle(titlev, mRes.getText(R.string.msg_playing_done));
                    }

                    if (needToNotification)
                        NotiManager.get().putNotification(NotiManager.NotiType.ALERT, (String)msg);
                }

                @Override
                public void onStarted() {
                    if (null != mPlayerv)
                        pvEnableLDrawer(mPlayerLDrawer);
                }

                @Override
                public void onChanged() {
                    // Control button should be re-checked due to 'next' and 'prev' button.
                    if (null != mPlayerv)
                        pvConfigureControl((ViewGroup)mPlayerv.findViewById(R.id.mplayer_control),
                                           YTPlayer.MPState.INVALID, YTPlayer.MPSTATE_FLAG_IDLE,
                                           mMp.playerGetState(), mMp.playerGetStateFlag());

                    if (null == mPlayerLDrawer)
                        return;

                    if (mMp.hasActiveVideo()) {
                        ListView lv = (ListView)mPlayerLDrawer.findViewById(R.id.mplayer_list);
                        YTPlayerVidArrayAdapter adapter = (YTPlayerVidArrayAdapter)lv.getAdapter();
                        if (null != adapter)
                            adapter.setVidArray(mMp.getVideoList());
                    } else
                        pvDisableLDrawer(mPlayerLDrawer);
                }
            });

            final SlidingDrawer drawer = (SlidingDrawer)playerLDrawer.findViewById(R.id.mplayer_ldrawer);
            drawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
                @Override
                public void
                onDrawerOpened() {
                    if (!mMp.hasActiveVideo())
                        return;

                    ListView lv = (ListView)mPlayerLDrawer.findViewById(R.id.mplayer_list);
                    int topPos = mMp.getActiveVideoIndex() - 1;
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

        // Set progress state to right position.
        mUpdateProg.update(mMp.playerGetDuration(), mMp.playerGetPosition());

        pvConfigureAll(playerv, playerLDrawer,
                       YTPlayer.MPState.INVALID, YTPlayer.MPSTATE_FLAG_IDLE,
                       mMp.playerGetState(), mMp.playerGetStateFlag());
    }

    // ============================================================================
    //
    // Package interfaces (for YTPlayer)
    //
    // ============================================================================
    YTPlayerUI(YTPlayer ytplayer) {
        mMp = ytplayer;
    }

    Err
    setController(Activity  activity,
                  ViewGroup playerv,
                  ViewGroup playerLDrawer,
                  SurfaceView surfacev,
                  YTPlayer.ToolButton toolBtn) {
        // update notification by force
        notiConfigure(YTPlayer.MPState.INVALID, YTPlayer.MPSTATE_FLAG_IDLE,
                      mMp.playerGetState(), mMp.playerGetStateFlag());

        if (activity == mVActivity && mPlayerv == playerv)
            // controller is already set for this context.
            // So, nothing to do. just return!
            return Err.NO_ERR;

        mVActivity = activity;
        mPlayerv = (LinearLayout)playerv;
        mPlayerLDrawer = (LinearLayout)playerLDrawer;
        mSurfacev = surfacev;
        mToolBtn = toolBtn;

        if (null == mPlayerv) {
            eAssert(null == mPlayerLDrawer);
            return Err.NO_ERR;
        }

        eAssert(null != mPlayerv.findViewById(R.id.mplayer_layout_magic_id));
        pvInit(playerv, playerLDrawer, surfacev);

        return Err.NO_ERR;
    }

    void
    unsetController(Activity activity) {
        if (activity == mVActivity) {
            mPlayerv = null;
            mVActivity = null;
            mPlayerLDrawer = null;
            mSurfacev = null;
        }
    }

    Activity
    getActivity() {
        return mVActivity;
    }

    SurfaceView
    getSurfaceView() {
        return mSurfacev;
    }

    void
    notifyToUser(String msg) {
        if (null != mVActivity)
            UiUtils.showTextToast(mVActivity, msg);
    }

    void
    changeVideoVolume(final String title, final String videoId) {
        if (null == mVActivity)
            return;

        final boolean runningVideo;
        // Retrieve current volume
        int curvol = Policy.DEFAULT_VIDEO_VOLUME;
        if (mMp.isVideoPlaying()
            && mMp.getActiveVideo().videoId.equals(videoId)) {
            runningVideo = true;
            curvol = mMp.playerGetVolume();
        } else {
            runningVideo = false;
            Long i = (Long)mDb.getVideoInfo(videoId, DB.ColVideo.VOLUME);
            if (null != i)
                curvol = i.intValue();
        }

        ViewGroup diagv = (ViewGroup)UiUtils.inflateLayout(mVActivity, R.layout.mplayer_vol_dialog);
        AlertDialog.Builder bldr = new AlertDialog.Builder(mVActivity);
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
                    mMp.playerSetVolume(progress);
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

}
