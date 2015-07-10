/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015
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

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import free.yhc.netmbuddy.DiagAsyncTask;
import free.yhc.netmbuddy.Err;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.YTPlayer.DBUpdateType;
import free.yhc.netmbuddy.core.YTPlayer.Video;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;
import free.yhc.netmbuddy.utils.YTUtils;
import free.yhc.netmbuddy.widget.SlidingDrawer;

public class YTPlayerUI implements
OnSharedPreferenceChangeListener {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTPlayerUI.class);

    private static final int SEEKBAR_MAX = 1000;

    private static YTPlayerUI sInstance = null;
    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------
    private final Resources mRes = Utils.getAppContext().getResources();
    private final DB mDb = DB.get();
    private final UpdateProgress mUpdateProg = new UpdateProgress();
    private final YTPlayer mMp;
    private final TimeTickReceiver mTTRcvr = new TimeTickReceiver();


    // ------------------------------------------------------------------------
    // UI Control.
    // ------------------------------------------------------------------------
    private Activity mVActivity = null;
    private KBLinkedList<YTPlayer.OnDBUpdatedListener> mDbUpdatedListenerl = new KBLinkedList<>();
    private LinearLayout mPlayerv = null;
    private LinearLayout mPlayerLDrawer = null;

    // To support video
    private SurfaceView mSurfacev = null;
    // For extra Button
    private YTPlayer.ToolButton mToolBtn = null;

    public static class TimeTickReceiver extends BroadcastReceiver {
        private YTPlayerUI _mYtpui = null;

        void
        setYTPlayerUI(YTPlayerUI ytpui) {
            _mYtpui = ytpui;
        }

        @Override
        public void
        onReceive(Context context, Intent intent) {
            // To minimize time spending in 'Broadcast Receiver'.
            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void
                run() {
                    YTPlayer mp = YTPlayer.get();
                    if (null != _mYtpui)
                        _mYtpui.updateStatusAutoStopSet(mp.isAutoStopSet(), mp.getAutoStopTime());
                }
            });
        }
    }

    private enum PlayBtnState {
        START,
        PAUSE,
        STOP,
        // Temporal state to notify that user triggers 'pause'.
        // This state will be overwritten to the other state immediately
        //   at state machine that configures control buttons.
        USER_PAUSE,
    }

    private class UpdateProgress implements Runnable {
        private static final int UPDATE_INTERVAL_MS = 1000;

        private SeekBar _mSeekbar = null;
        private TextView _mCurposv = null;
        private TextView _mMaxposv = null;
        private int _mLastProgress = -1;
        private int _mLastSecondaryProgress = -1; // For secondary progress

        private void
        resetProgressView() {
            if (null != _mSeekbar) {
                _mMaxposv.setText(Utils.secsToMinSecText(mMp.playerGetDuration() / 1000));
                update(1, 0);
                updateSecondary(0);
            }
            _mLastProgress = 0;
            _mLastSecondaryProgress = 0;
        }

        @SuppressWarnings("unused")
        int
        getSecondaryProgressPercent() {
            int percent =  _mLastSecondaryProgress * 100 / SEEKBAR_MAX;
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
            _mCurposv = (TextView)progv.findViewById(R.id.mplayer_curpos);
            _mMaxposv = (TextView)progv.findViewById(R.id.mplayer_maxpos);
            _mSeekbar = (SeekBar)progv.findViewById(R.id.mplayer_seekbar);
            if (null != _mSeekbar) {
                _mMaxposv.setText(Utils.secsToMinSecText(mMp.playerGetDuration() / 1000));
                update(mMp.playerGetDuration(), _mLastProgress);
                updateSecondary(_mLastSecondaryProgress);
            }
        }

        void
        start() {
            //logI("Progress Start");
            _mMaxposv.setText(Utils.secsToMinSecText(mMp.playerGetDuration() / 1000));
            update(mMp.playerGetDuration(), _mLastProgress);
            updateSecondary(_mLastSecondaryProgress);
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
            if (null != _mSeekbar) {
                int curPv = (durms > 0)? (int)(curms * (long)SEEKBAR_MAX / durms)
                                               : 0;
                _mSeekbar.setProgress(curPv);
                _mCurposv.setText(Utils.secsToMinSecText(curms / 1000));
                _mLastProgress = curPv;
            }
        }

        /**
         * Update secondary progress
         */
        void
        updateSecondary(int percent) {
            // Update secondary progress
            if (null != _mSeekbar) {
                int pv = percent * SEEKBAR_MAX / 100;
                _mSeekbar.setSecondaryProgress(pv);
                _mLastSecondaryProgress = pv;
            }
        }

        @Override
        public void
        run() {
            update(mMp.playerGetDuration(), mMp.playerGetPosition());
            Utils.getUiHandler().postDelayed(this, UPDATE_INTERVAL_MS);
        }
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    private void
    registerTimeTickReceiver() {
        if (null == mVActivity)
            return;

        mTTRcvr.setYTPlayerUI(this);
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        mVActivity.registerReceiver(mTTRcvr, filter);
    }

    private void
    unregisterTimeTickReceiver() {
        if (null == mVActivity)
            return;

        mVActivity.unregisterReceiver(mTTRcvr);
    }

    // ========================================================================
    //
    // Notification Handling
    //
    // ========================================================================
    private void
    notiConfigure(@SuppressWarnings("unused") YTPlayer.MPState from,
                  @SuppressWarnings("unused") int fromFlag,
                  YTPlayer.MPState to,
                  @SuppressWarnings("unused") int toFlag) {
        NotiManager nm = NotiManager.get();
        if (!mMp.hasActiveVideo()) {
            nm.removePlayerNotification();
            return;
        }
        String title = mMp.getActiveVideo().v.title;

        NotiManager.NotiType ntype;
        switch (to) {
        case PREPARED:
        case PAUSED:
            ntype = NotiManager.NotiType.START;
            break;

        case STARTED:
            ntype = NotiManager.NotiType.PAUSE;
            break;

        case ERROR:
            ntype = NotiManager.NotiType.ALERT;
            break;

        case IDLE:
        case INITIALIZED:
        case PREPARED_AUDIO:
        case PREPARING:
        case PLAYBACK_COMPLETED:
            ntype = NotiManager.NotiType.STOP;
            break;

        default:
            ntype = NotiManager.NotiType.BASE;
        }
        nm.putPlayerNotification(ntype, title);
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
                     @SuppressWarnings("unused") YTPlayer.MPState from,
                     @SuppressWarnings("unused") int fromFlag,
                     YTPlayer.MPState to,
                     @SuppressWarnings("unused") int toFlag) {
        if (null == titlev)
            return;

        CharSequence videoTitle = "";
        if (mMp.hasActiveVideo())
            videoTitle = mMp.getActiveVideo().v.title;

        switch (to) {
        case PREPARED:
        case PAUSED:
        case STARTED:
            eAssert(null != videoTitle);
            if (mMp.isPlayerBuffering()
                || mMp.isPlayerSeeking())
                videoTitle = "(" + mRes.getText(R.string.buffering) + ") " + videoTitle;

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

    @SuppressWarnings("unused")
    private void
    pvDisableControlButton(ViewGroup playerv) {
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btnplay));
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btnnext));
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btnprev));
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btnmore));
        pvDisableButton((ImageView)playerv.findViewById(R.id.mplayer_btntool));
    }

    private void
    pvConfigureControl(ViewGroup controlv,
                       @SuppressWarnings("unused") YTPlayer.MPState from,
                       @SuppressWarnings("unused") int fromFlag,
                       YTPlayer.MPState to,
                       @SuppressWarnings("unused") int toFlag) {
        if (null == controlv)
            return;

        if (!mMp.hasActiveVideo()) {
            controlv.setVisibility(View.GONE);
            return;
        }


        controlv.setVisibility(View.VISIBLE);
        final ImageView nextv = (ImageView)controlv.findViewById(R.id.mplayer_btnnext);
        final ImageView prevv = (ImageView)controlv.findViewById(R.id.mplayer_btnprev);
        final ImageView playv = (ImageView)controlv.findViewById(R.id.mplayer_btnplay);
        final ImageView morev = (ImageView)controlv.findViewById(R.id.mplayer_btnmore);
        final ImageView toolv = (ImageView)controlv.findViewById(R.id.mplayer_btntool);

        // --------------------------------------------------------------------
        // configure prev/next/more
        // --------------------------------------------------------------------
        switch (to) {
        case PREPARED_AUDIO:
        case PREPARED:
        case PAUSED:
        case STARTED:
            pvEnableButton(morev, 0);
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
            pvDisableButton(morev);
            pvDisableButton(prevv);
            pvDisableButton(nextv);
        }

        // --------------------------------------------------------------------
        // configure play/pause
        // --------------------------------------------------------------------
        switch (to) {
        case PREPARED:
        case PAUSED: {
            PlayBtnState st = (PlayBtnState)playv.getTag();
            if (PlayBtnState.USER_PAUSE == st) {
                pvEnableButton(playv, R.drawable.ic_media_stop);
                playv.setTag(PlayBtnState.STOP);

                // [ Implementing Usecase]
                // - Single touch while playing video pauses video.
                // - Double-touch while playing video stops videos.
                final long sessionId = mMp.getPlayerSessionId();
                final Activity activity = mVActivity;
                Utils.getUiHandler().postDelayed(new Runnable() {
                    @Override
                    public void
                    run() {
                        // Strict check to know that player UI is still at the same screen and state
                        //   with the moment when user-pause is triggered.
                        if (mMp.getPlayerSessionId() == sessionId
                            && YTPlayer.MPState.PAUSED == mMp.getPlayerState()
                            && PlayBtnState.STOP == playv.getTag()
                            && activity == mVActivity) {
                            playv.setImageResource(R.drawable.ic_media_play);
                            playv.setTag(PlayBtnState.START);
                        }
                    }
                }, Policy.YTPLAYER_DOUBLE_TOUCH_INTERVAL);

            } else {
                pvEnableButton(playv, R.drawable.ic_media_play);
                playv.setTag(PlayBtnState.START);
            }
        } break;

        case STARTED:
            pvEnableButton(playv, R.drawable.ic_media_pause);
            // Set next state be moved to on click as 'Tag'
            playv.setTag(PlayBtnState.PAUSE);
            break;

        case IDLE:
        case INITIALIZED:
        case PREPARED_AUDIO:
        case PREPARING:
        case PLAYBACK_COMPLETED:
            pvEnableButton(playv, R.drawable.ic_media_stop);
            // Set next state be moved to on click as 'Tag'
            playv.setTag(PlayBtnState.STOP);
            break;

        default:
            playv.setTag(null);
            controlv.setVisibility(View.GONE);
        }

        // --------------------------------------------------------------------
        // configure tool
        // --------------------------------------------------------------------
        switch (to) {
        case INVALID:
        case END:
            pvDisableButton(toolv);
        break;

        default:
            if (null != mToolBtn)
                pvEnableButton(toolv, 0);
        }
    }

    private void
    pvConfigureProgress(ViewGroup progressv,
                        @SuppressWarnings("unused") YTPlayer.MPState from,
                        @SuppressWarnings("unused") int fromFlag,
                        YTPlayer.MPState to,
                        @SuppressWarnings("unused") int toFlag) {

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
            // do nothing progress is now under update..
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
        YTPlayerVideoListAdapter adapter = new YTPlayerVideoListAdapter(mVActivity, mMp.getVideoList());
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
                       @SuppressWarnings("unused") YTPlayer.MPState from,
                       @SuppressWarnings("unused") int fromFlag,
                       @SuppressWarnings("unused") YTPlayer.MPState to,
                       @SuppressWarnings("unused") int toFlag) {
        if (null == playerLDrawer)
            return;

        if (!mMp.hasActiveVideo()) {
            pvDisableLDrawer(playerLDrawer);
            return;
        }

        ListView lv = (ListView)playerLDrawer.findViewById(R.id.mplayer_list);
        YTPlayerVideoListAdapter adapter = (YTPlayerVideoListAdapter)lv.getAdapter();
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
    pvMoreControlDetailInfo(long vid) {
        UiUtils.showVideoDetailInfo(mVActivity, vid);
    }

    private void
    pvMoreControlSetBookmark(final long vid) {
        final int posms = mMp.playerGetPosition();

        if (0 == posms) {
            UiUtils.showTextToast(mVActivity, R.string.msg_fail_set_bookmark);
            return;
        }

        final String title = Utils.getResString(R.string.set_bookmark)
                             + " : "
                             + Utils.secsToMinSecText(posms / 1000)
                             + Utils.getResString(R.string.seconds);

        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void
            prepare(Dialog dialog, EditText edit) { }

            @Override
            public void
            onOk(Dialog dialog, EditText edit) {
                String bmname = edit.getText().toString();
                if (bmname.contains("" + DB.BOOKMARK_DELIMITER)) {
                    String msg = Utils.getResString(R.string.msg_forbidden_characters) + "\n"
                                 + "    " + DB.BOOKMARK_DELIMITER;
                    UiUtils.showTextToast(mVActivity, msg);
                    UiUtils.buildOneLineEditTextDialog(mVActivity,
                                                       title,
                                                       bmname,
                                                       "",
                                                       this)
                           .show();
                } else
                    mDb.addBookmark(vid, bmname, posms);
            }
        };

        UiUtils.buildOneLineEditTextDialogWithHint(mVActivity,
                                                   title,
                                                   R.string.enter_bookmark_name,
                                                   action)
               .show();
    }

    private void
    pvMoreControlAddToWithYtid(final UiUtils.OnPostExecuteListener listener,
                               final Object user,
                               final long plid,
                               final YTPlayer.Video video) {
        final int volume;
        if (mMp.getActiveVideo() == video)
            volume = mMp.playerGetVolume();
        else
            volume = Policy.DEFAULT_VIDEO_VOLUME;

        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                listener.onPostExecute(result, user);
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                DMVideo v = new DMVideo();
                v.copy(video.v);
                eAssert(Utils.isValidValue(v.ytvid));
                if (!YTUtils.fillYtDataAndThumbnail(v))
                    return Err.IO_NET;
                v.setPreferenceData(volume, "");
                DB.Err dberr = DB.get().insertVideoToPlaylist(plid, v);
                return Err.map(dberr);
            }
        };

        new DiagAsyncTask(mVActivity,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          Utils.getResString(R.string.adding),
                          true)
            .run();
    }

    private void
    pvMoreControlAddTo(Long vid, final Video video) {
        final UiUtils.OnPostExecuteListener listener = new UiUtils.OnPostExecuteListener() {
            @Override
            public void
            onPostExecute(Err result, Object user) {
                if (Err.NO_ERR != result)
                    return;

                Iterator<YTPlayer.OnDBUpdatedListener> iter = mDbUpdatedListenerl.iterator();
                while (iter.hasNext())
                    iter.next().onDbUpdated(DBUpdateType.PLAYLIST);
            }
        };

        if (null != vid) {
            UiUtils.addVideosTo(mVActivity,
                                null,
                                listener,
                                UiUtils.PLID_INVALID,
                                new long[] { vid },
                                false);
        } else {
            UiUtils.OnPlaylistSelected action = new UiUtils.OnPlaylistSelected() {
                @Override
                public void
                onPlaylist(long plid, Object user) {
                    pvMoreControlAddToWithYtid(listener, user, plid, video);
                }

                @Override
                public void
                onUserMenu(int pos, Object user) {}
            };

            // exclude current playlist
            UiUtils.buildSelectPlaylistDialog(DB.get(),
                                              mVActivity,
                                              R.string.add_to,
                                              null,
                                              action,
                                              UiUtils.PLID_INVALID,
                                              null)
                   .show();
        }
    }

    private void
    pvMoreControlDelete(final Long vid, final String ytvid) {
        final UiUtils.OnPostExecuteListener listener = new UiUtils.OnPostExecuteListener() {
            @Override
            public void
            onPostExecute(Err result, Object user) {
                if (Err.NO_ERR != result)
                    return;

                mMp.removeVideo(ytvid);
                if (null != vid) {
                    Iterator<YTPlayer.OnDBUpdatedListener> iter = mDbUpdatedListenerl.iterator();
                    while (iter.hasNext())
                        iter.next().onDbUpdated(DBUpdateType.PLAYLIST);
                }
            }
        };

        if (null == vid) {
            UiUtils.ConfirmAction action = new UiUtils.ConfirmAction() {
                @Override
                public void
                onOk(Dialog dialog) {
                    listener.onPostExecute(Err.NO_ERR, null);
                }

                @Override
                public void
                onCancel(Dialog dialog) { }
            };
            UiUtils.buildConfirmDialog(mVActivity,
                                       R.string.delete,
                                       R.string.msg_delete_musics_completely,
                                       action)
                   .show();

        } else {
            UiUtils.deleteVideos(mVActivity,
                                 null,
                                 listener,
                                 UiUtils.PLID_UNKNOWN,
                                 new long[] { vid });
        }
    }

    private void
    pvOnMoreButtonClicked(@SuppressWarnings("unused") View v) {
        final YTPlayer.Video video  = mMp.getActiveVideo();

        if (null == video)
            return; // This line can be reached because of race-condition...

        final int[] opts;
        final Long vid = (Long)mDb.getVideoInfo(video.v.ytvid, ColVideo.ID);
        if (null != vid)
            opts = new int[] { R.string.detail_info,
                               R.string.set_bookmark,
                               R.string.bookmarks,
                               R.string.add_to,
                               R.string.volume,
                               R.string.delete };
        else
            opts = new int[] { R.string.add_to,
                               R.string.volume };

        final CharSequence[] items = new CharSequence[opts.length];
        for (int i = 0; i < opts.length; i++)
            items[i] = Utils.getResString(opts[i]);

        AlertDialog.Builder builder = new AlertDialog.Builder(mVActivity);
        builder.setTitle(R.string.player_extra_control_title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void
            onClick(DialogInterface dialog, int item) {
                switch (opts[item]) {
                case R.string.detail_info:
                    // vid is NOT null
                    //noinspection ConstantConditions
                    pvMoreControlDetailInfo(vid);
                    break;

                case R.string.set_bookmark:
                    // vid is NOT null
                    //noinspection ConstantConditions
                    pvMoreControlSetBookmark(vid);
                    break;

                case R.string.bookmarks:
                    UiUtils.showBookmarkDialog(mVActivity, video.v.ytvid, video.v.title);
                    break;

                case R.string.add_to:
                    pvMoreControlAddTo(vid, video);
                    break;

                case R.string.volume:
                    changeVideoVolume(video.v.title, video.v.ytvid);
                    break;

                case R.string.delete:
                    pvMoreControlDelete(vid, video.v.ytvid);
                    break;

                default:
                    eAssert(false);
                }
            }
        });
        builder.create().show();
    }

    private void
    pvSetupControlButton(final ViewGroup playerv) {
        ImageView btn = (ImageView)playerv.findViewById(R.id.mplayer_btnplay);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                // See pvConfigControl() for details.
                PlayBtnState st = (PlayBtnState)v.getTag();
                if (null == st)
                    return; // Nothing to do.

                switch (st) {
                case START:
                    mMp.playerStart();
                    break;

                case PAUSE:
                    v.setTag(PlayBtnState.USER_PAUSE);
                    mMp.playerPause();
                    break;

                case STOP:
                    // This doesn't means "Stop only this video",
                    //   but means stop playing vidoes - previous user request.
                    mMp.stopVideos();
                    break;

                default:
                    // do nothing.
                }
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnprev);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                mMp.startPrevVideo();
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnnext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                mMp.startNextVideo();
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btnmore);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                pvOnMoreButtonClicked(v);
            }
        });

        btn = (ImageView)playerv.findViewById(R.id.mplayer_btntool);
        if (null == mToolBtn)
            btn.setVisibility(View.INVISIBLE);
        else {
            btn.setImageResource(mToolBtn.drawable);
            btn.setOnClickListener(mToolBtn.onClick);
        }
    }

    private void
    pvSetupStatusBar(@SuppressWarnings("unused") ViewGroup playerv) {
        updateStatusAutoStopSet(mMp.isAutoStopSet(), mMp.getAutoStopTime());
        onSharedPreferenceChanged(Utils.getSharedPreference(), Utils.getResString(R.string.csquality));
        onSharedPreferenceChanged(Utils.getSharedPreference(), Utils.getResString(R.string.csrepeat));
        onSharedPreferenceChanged(Utils.getSharedPreference(), Utils.getResString(R.string.csshuffle));
    }

    private void
    pvInit(ViewGroup playerv,
           ViewGroup playerLDrawer,
           @SuppressWarnings("unused") SurfaceView surfacev) {
        mMp.addPlayerStateListener(this, new YTPlayer.PlayerStateListener() {
            @Override
            public void
            onStateChanged(YTPlayer.MPState from, int fromFlag,
                           YTPlayer.MPState to,   int toFlag) {
                pvConfigureAll(mPlayerv, mPlayerLDrawer, from, fromFlag, to, toFlag);
                notiConfigure(from, fromFlag, to, toFlag);
            }

            @Override
            public void
            onBufferingChanged(int percent) {
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
                public void
                onStopped(YTPlayer.StopState state) {
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
                        NotiManager.get().putPlayerNotification(NotiManager.NotiType.ALERT, (String)msg);
                }

                @Override
                public void
                onStarted() {
                    if (null != mPlayerv)
                        pvEnableLDrawer(mPlayerLDrawer);
                }

                @Override
                public void
                onChanged() {
                    // Control button should be re-checked due to 'next' and 'prev' button.
                    if (null != mPlayerv)
                        pvConfigureControl((ViewGroup)mPlayerv.findViewById(R.id.mplayer_control),
                                           YTPlayer.MPState.INVALID, YTPlayer.MPSTATE_FLAG_IDLE,
                                           mMp.playerGetState(), mMp.playerGetStateFlag());

                    if (null == mPlayerLDrawer)
                        return;

                    if (mMp.hasActiveVideo()) {
                        ListView lv = (ListView)mPlayerLDrawer.findViewById(R.id.mplayer_list);
                        YTPlayerVideoListAdapter adapter = (YTPlayerVideoListAdapter)lv.getAdapter();
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
                    if (!mMp.hasActiveVideo()
                        || null == mPlayerLDrawer)
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

        pvSetupStatusBar(playerv);
        pvSetupControlButton(playerv);

        // Set progress state to right position.
        mUpdateProg.update(mMp.playerGetDuration(), mMp.playerGetPosition());

        pvConfigureAll(playerv, playerLDrawer,
                       YTPlayer.MPState.INVALID, YTPlayer.MPSTATE_FLAG_IDLE,
                       mMp.playerGetState(), mMp.playerGetStateFlag());
    }

    // ============================================================================
    //
    // Overrides
    //
    // ============================================================================
    @Override
    public void
    onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(Utils.getResString(R.string.csshuffle))) {
            updateStatusShuffle(Utils.isPrefSuffle());
        } else if (key.equals(Utils.getResString(R.string.csrepeat))) {
            updateStatusRepeat(Utils.isPrefRepeat());
        } else if (key.equals(Utils.getResString(R.string.csquality))) {
            updateStatusQuality(Utils.getPrefQuality());
        }
    }

    // ============================================================================
    //
    // Update status icons
    //
    // ============================================================================
    void
    updateStatusAutoStopSet(boolean set, long timeMillis) {
        if (null == mPlayerv)
            return;

        ImageView iv = (ImageView)mPlayerv.findViewById(R.id.mplayer_status_autostop);
        TextView tv = (TextView)mPlayerv.findViewById(R.id.mplayer_status_autostop_time);

        if (set) {
            long tmLeft = timeMillis - System.currentTimeMillis();
            if (tmLeft < 0)
                tmLeft = 0;
            String tmText = Utils.millisToHourMinText(tmLeft) + Utils.getResString(R.string.minutes);
            iv.setImageResource(R.drawable.status_autostop_on);
            tv.setVisibility(View.VISIBLE);
            tv.setText(tmText);
        } else {
            iv.setImageResource(R.drawable.status_autostop_off);
            tv.setVisibility(View.GONE);
        }
    }

    private void
    updateStatusQuality(Utils.PrefQuality quality) {
        if (null == mPlayerv)
            return;

        TextView tv = (TextView)mPlayerv.findViewById(R.id.mplayer_status_quality);
        CharSequence text = "?";
        switch (quality) {
        case VERYHIGH:
            text = "VH";
            break;
        case HIGH:
            text = "H";
            break;
        case NORMAL:
            text = "M";
            break;
        case MIDLOW:
            text = "ML";
            break;
        case LOW:
            text = "L";
            break;
        }
        tv.setText("[" + text + "]");
    }

    private void
    updateStatusShuffle(boolean set) {
        if (null == mPlayerv)
            return;

        ImageView iv = (ImageView)mPlayerv.findViewById(R.id.mplayer_status_shuffle);
        iv.setImageResource(set? R.drawable.status_shuffle_on: R.drawable.status_shuffle_off);
    }

    private void
    updateStatusRepeat(boolean set) {
        if (null == mPlayerv)
            return;

        ImageView iv = (ImageView)mPlayerv.findViewById(R.id.mplayer_status_repeat);
        iv.setImageResource(set? R.drawable.status_repeat_on: R.drawable.status_repeat_off);
    }

    // ============================================================================
    //
    // Package interfaces (for YTPlayer)
    //
    // ============================================================================
    YTPlayerUI(YTPlayer ytplayer) {
        mMp = ytplayer;
        if (null == sInstance)
            sInstance = this;
        else
            eAssert(false); // This SHOULD BE SINGLETON (Only YTPlayer has this!)
    }

    void
    addOnDbUpdatedListener(Object key, YTPlayer.OnDBUpdatedListener listener) {
        mDbUpdatedListenerl.add(key, listener);
    }

    void
    removeOnDbUpdatedListener(Object key) {
        mDbUpdatedListenerl.remove(key);
    }

    void
    setController(Activity  activity,
                  ViewGroup playerv,
                  ViewGroup playerLDrawer,
                  SurfaceView surfacev,
                  YTPlayer.ToolButton toolBtn) {
        if (null != mVActivity
            && activity != mVActivity)
            unsetController(mVActivity);

        Utils.getSharedPreference().registerOnSharedPreferenceChangeListener(this);


        // update notification by force
        notiConfigure(YTPlayer.MPState.INVALID, YTPlayer.MPSTATE_FLAG_IDLE,
                      mMp.playerGetState(), mMp.playerGetStateFlag());

        if (activity == mVActivity && mPlayerv == playerv)
            // controller is already set for this context.
            // So, nothing to do. just return!
            return;

        mVActivity = activity;
        mDbUpdatedListenerl.clear();
        mPlayerv = (LinearLayout)playerv;
        mPlayerLDrawer = (LinearLayout)playerLDrawer;
        mSurfacev = surfacev;
        mToolBtn = toolBtn;

        if (null == mPlayerv) {
            eAssert(null == mPlayerLDrawer);
            return;
        }

        eAssert(null != mPlayerv.findViewById(R.id.mplayer_layout_magic_id));
        registerTimeTickReceiver();
        pvInit(playerv, playerLDrawer, surfacev);
    }

    void
    unsetController(Activity activity) {
        if (activity == mVActivity) {
            Utils.getSharedPreference().unregisterOnSharedPreferenceChangeListener(this);
            unregisterTimeTickReceiver();
            mPlayerv = null;
            mVActivity = null;
            mDbUpdatedListenerl.clear();
            mPlayerLDrawer = null;
            mSurfacev = null;
        }
    }

    Activity
    getActivity() {
        return mVActivity;
    }

    @SuppressWarnings("unused")
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
    setPlayerVisibility(int visibility) {
        if (null != mPlayerv)
            mPlayerv.setVisibility(visibility);
    }

    void
    updateLDrawerList() {
        if (null == mPlayerLDrawer)
            return;

        ListView lv = (ListView)mPlayerLDrawer.findViewById(R.id.mplayer_list);
        YTPlayer.Video[] vs =  mMp.getVideoList();
        if (null == vs)
            vs = new YTPlayer.Video[0];
        YTPlayerVideoListAdapter adapter = new YTPlayerVideoListAdapter(mVActivity, vs);
        adapter.setActiveItem(mMp.getActiveVideoIndex());
        lv.setAdapter(adapter);
    }

    void
    changeVideoVolume(final String title, final String ytvid) {
        if (null == mVActivity)
            return;

        final boolean runningVideo;
        // Retrieve current volume
        int curvol = Policy.DEFAULT_VIDEO_VOLUME;
        if (mMp.isVideoPlaying()
            && mMp.getActiveVideo().v.ytvid.equals(ytvid)) {
            runningVideo = true;
            curvol = mMp.playerGetVolume();
        } else {
            runningVideo = false;
            Long i = (Long)mDb.getVideoInfo(ytvid, ColVideo.VOLUME);
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
                mDb.updateVideoVolume(ytvid, newVolume);
            }
        });
        aDiag.show();
    }

}
