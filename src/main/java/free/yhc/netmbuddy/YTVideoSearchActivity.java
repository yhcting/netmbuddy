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

package free.yhc.netmbuddy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import static free.yhc.abaselib.util.UxUtil.showTextToast;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.async.Task;
import free.yhc.baselib.async.TmTask;
import free.yhc.abaselib.util.AUtil;
import free.yhc.abaselib.util.ImgUtil;
import free.yhc.abaselib.ux.DialogTask;
import free.yhc.netmbuddy.core.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.task.YTVideoListTask;
import free.yhc.netmbuddy.utils.UxUtil;
import free.yhc.netmbuddy.utils.Util;

public abstract class YTVideoSearchActivity extends YTSearchActivity implements
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTVideoSearchActivity.class, Logger.LOGLV_DEFAULT);

    private final DB mDb = DB.get();
    private final YTPlayer mMp = YTPlayer.get();

    private View.OnClickListener mToolBtnSearchAction;
    private DBCheckDupTask mDbCheckDupTask = null;

    private final DBCheckDupTask.EventListener<DBCheckDupTask, boolean[]> mDbCheckDupTaskListener
            = new DBCheckDupTask.EventListener<DBCheckDupTask, boolean[]>() {
        @Override
        public void
        onPostRun(@NonNull DBCheckDupTask task,
                  boolean[] result,
                  Exception ex) {
            Err err = Err.NO_ERR;
            if (null != ex)
                err = Err.DB_UNKNOWN;
            checkDupDone(task, task.getVideos(), result, err);
        }
    };

    private YTVideoSearchAdapter.CheckStateListener mAdapterCheckListener
        = new YTVideoSearchAdapter.CheckStateListener() {
        @Override
        public void
        onStateChanged(int nrChecked, int pos, boolean checked) {
            if (0 == nrChecked) {
                setupToolBtn1(getToolButtonSearchIcon(), mToolBtnSearchAction);
            } else {
                setupToolBtn1(R.drawable.ic_add, new View.OnClickListener() {
                    @Override
                    public void
                    onClick(View v) {
                        addCheckedMusicsTo();
                    }
                });
            }
        }
    };

    private final OnPlayerUpdateDBListener mOnPlayerUpdateDbListener
        = new OnPlayerUpdateDBListener();

    private class OnPlayerUpdateDBListener implements YTPlayer.OnDBUpdatedListener {
        @Override
        public void
        onDbUpdated(YTPlayer.DBUpdateType ty) {
            switch (ty) {
                case PLAYLIST:
                    enableContentLoading();
                    YTVideoSearchAdapter adapter = getAdapter();
                    if (null != adapter)
                        checkDupAsync(null, adapter.getItems());
            }
            // others are ignored.
        }
    }

    private class DBCheckDupTask extends TmTask<boolean[]> {
        private final YTDataAdapter.Video[] mVids;
        private final Object mOpaque;

        DBCheckDupTask(@NonNull YTDataAdapter.Video[] vids, Object opaque) {
            mVids = vids;
            mOpaque = opaque;
        }

        public YTDataAdapter.Video[]
        getVideos() {
            return mVids;
        }

        public Object
        getOpaque() {
            return mOpaque;
        }

        @Override
        @NonNull
        public boolean[]
        doAsync()
                throws InterruptedException {
            // TODO : Should I check "entries[i].available" flag???
            boolean[] r = new boolean[mVids.length];
            publishProgressInit(r.length);
            publishProgress(0);
            for (int i = 0; i < r.length; i++) {
                r[i] = DB.get().containsVideo(mVids[i].id);
                if (isCancel())
                    throw new InterruptedException("Task is cancelled");
                publishProgress(i + 1);
            }
            return r;
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    private YTVideoSearchAdapter
    getAdapter() {
        return (YTVideoSearchAdapter)mListv.getAdapter();
    }

    private void
    onContextMenuAddTo(final int position) {
        UxUtil.OnPlaylistSelected action = new UxUtil.OnPlaylistSelected() {
            @Override
            public void
            onPlaylist(long plid, Object user) {
                int pos = (Integer)user;
                int volume = getAdapter().getItemVolume(pos);
                int msg = addToPlaylist(getAdapter(), plid, pos, volume);
                if (0 != msg)
                    showTextToast(msg);
            }

            @Override
            public void
            onUserMenu(int pos, Object user) {}

        };

        UxUtil.buildSelectPlaylistDialog(mDb,
                                         this,
                                         R.string.add_to,
                                         null,
                                         action,
                                         DB.INVALID_PLAYLIST_ID,
                                         position)
               .show();
    }

    private void
    onContextMenuAppendToPlayQ(final int position) {
        YTPlayer.Video vid = getAdapter().getYTPlayerVideo(position);
        appendToPlayQ(new YTPlayer.Video[]{vid});

    }

    private void
    onContextMenuPlayVideo(final int position) {
        UxUtil.playAsVideo(this, getAdapter().getItemVideoId(position));
    }

    private void
    onContextMenuVideosOfSameChannel(final int position) {
        Intent i = new Intent(this, YTVideoSearchChannelActivity.class);
        i.putExtra(YTSearchActivity.KEY_TITLE, getAdapter().getItemChannelTitle(position));
        i.putExtra(YTSearchActivity.KEY_TEXT, getAdapter().getItemChannelId(position));
        startActivity(i);
    }

    private void
    onContextMenuSearchSimilarTitles(final int position) {
        UxUtil.showSimilarTitlesDialog(this, getAdapter().getItemTitle(position));
    }

    private void
    checkDupAsync(Object tag, YTDataAdapter.Video[] vids) {
        if (null != mDbCheckDupTask) {
            mDbCheckDupTask.removeEventListener(mDbCheckDupTaskListener);
            mTm.cancelTask(mDbCheckDupTask, null);
        }

        mDbCheckDupTask = new DBCheckDupTask(vids, tag);
        mDbCheckDupTask.addEventListener(AppEnv.getUiHandlerAdapter(), mDbCheckDupTaskListener);
        if (!mTm.addTask(
                mDbCheckDupTask,
                mDbCheckDupTask,
                this,
                null)) {
            P.bug(false); // This is UNEXPECTED!
        }
    }

    private void
    checkDupDoneNewEntries(YTDataAdapter.Video[] vids, boolean[] results) {
        // helper's event receiver is changed to adapter in adapter's constructor.
        YTVideoSearchAdapter adapter = new YTVideoSearchAdapter(this, vids);
        adapter.setCheckStateListener(getAdapterCheckStateListener());
        // First request is done!
        // Now we know total Results.
        // Let's build adapter and enable list.
        applyDupCheckResults(adapter, results);
        YTVideoSearchAdapter oldAdapter = getAdapter();
        mListv.setAdapter(adapter);

        // Cleanup before as soon as possible to secure memories.
        if (null != oldAdapter)
            oldAdapter.cleanup();
    }

    private void
    checkDupDone(DBCheckDupTask task, YTDataAdapter.Video[] vids,
                 boolean[] results, Err err) {
        if (task != mDbCheckDupTask)
            return; //new task is already started. Ignore this result.

        if (Err.NO_ERR != err
                || results.length != vids.length) {
            enableContentText(R.string.err_db_unknown);
            return;
        }

        enableContentList();
        if (null != getAdapter()
                && vids == getAdapter().getItems())
            // Entry is same with current adapter.
            // That means 'dup. checking is done for exsiting entries"
            applyDupCheckResults(getAdapter(), results);
        else
            checkDupDoneNewEntries(vids, results);
    }

    private void
    applyDupCheckResults(YTVideoSearchAdapter adapter, boolean[] results) {
        for (int i = 0; i < results.length; i++) {
            if (results[i])
                adapter.setToDup(i);
            else
                adapter.setToNew(i);
        }
    }

    private void
    addCheckedMusicsTo() {
        final int[] menuTextIds = new int[] { R.string.append_to_playq };

        final String[] userMenu = new String[menuTextIds.length];
        for (int i = 0; i < menuTextIds.length; i++)
            userMenu[i] = AUtil.getResString(menuTextIds[i]);

        UxUtil.OnPlaylistSelected action = new UxUtil.OnPlaylistSelected() {
            @Override
            public void
            onPlaylist(final long plid, Object user) {
                addCheckedMusicsToPlaylist(plid);
            }

            @Override
            public void
            onUserMenu(int pos, Object user) {
                P.bug(0 <= pos && pos < menuTextIds.length);
                switch (menuTextIds[pos]) {
                case R.string.append_to_playq:
                    appendCheckMusicsToPlayQ();
                    break;

                default:
                    P.bug(false);
                }
            }
        };

        UxUtil.buildSelectPlaylistDialog(mDb,
                                         this,
                                         R.string.add_to,
                                         userMenu,
                                         action,
                                         DB.INVALID_PLAYLIST_ID,
                                         null)
               .show();
    }

    private void
    addCheckedMusicsToPlaylist(final long plid) {
        // Scan to check all thumbnails are loaded.
        // And prepare data for background execution.
        final YTVideoSearchAdapter adpr = getAdapter();
        final int[] checkedItems = adpr.getCheckedItem();
        final int[] itemVolumes = new int[checkedItems.length];
        for (int i = 0; i < checkedItems.length; i++) {
            int pos = checkedItems[i];
            if (null == adpr.getItemThumbnail(pos)) {
                showTextToast(R.string.msg_no_all_thumbnail);
                return;
            }
            itemVolumes[i] = adpr.getItemVolume(pos);
        }

        Task<Void> t = new Task<Void>() {
            @Override
            protected Void
            doAsync() {
                int failedCnt = 0;
                mDb.beginTransaction();
                try {
                    for (int i = 0; i < checkedItems.length; i++) {
                        int pos = checkedItems[i];
                        int r = addToPlaylist(getAdapter(), plid, pos, itemVolumes[i]);
                        if (0 != r && R.string.msg_existing_muisc != r)
                            failedCnt++;
                    }
                    mDb.setTransactionSuccessful();
                } finally {
                    mDb.endTransaction();
                }

                final int failedCnt_ = failedCnt;
                AppEnv.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        adpr.cleanChecked();
                        if (failedCnt_ > 0) {
                            CharSequence msg = getResources().getText(R.string.msg_fails_to_add);
                            showTextToast(msg + " : " + failedCnt_);
                        }
                    }
                });
                return null;
            }
        };

        DialogTask.Builder<DialogTask.Builder> b
                = new DialogTask.Builder<>(this, t);
        b.setMessage(R.string.adding);
        if (!b.create().start())
            P.bug();
    }

    private void
    appendCheckMusicsToPlayQ() {
        // # of adapter items are at most Policy.YTSEARCH_MAX_RESULTS
        // So, just do it at main UI thread!
        YTVideoSearchAdapter adpr = getAdapter();
        int[] checkedItems = adpr.getCheckItemSortedByTime();
        YTPlayer.Video[] vids = new YTPlayer.Video[checkedItems.length];
        int j = 0;
        for (int i : checkedItems)
            vids[j++] = adpr.getYTPlayerVideo(i);
        appendToPlayQ(vids);
        adpr.cleanChecked();
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    protected void
    onCreateInternal(String title, String text) {
      mToolBtnSearchAction = new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                doNewSearch();
            }
        };

        setupBottomBar(getToolButtonSearchIcon(), mToolBtnSearchAction,
        0, null);

        if (null != text)
            startNewSearch(title, text);
        else
            doNewSearch();
    }

    /**
     * Override it to enabel tool button for search.
     */
    protected int
    getToolButtonSearchIcon() {
        return 0;
    }

    public YTVideoSearchAdapter.CheckStateListener
    getAdapterCheckStateListener() {
        return mAdapterCheckListener;
    }

    public void
    appendToPlayQ(YTPlayer.Video[] vids) {
        mMp.appendToPlayQ(vids);
    }

    /**
     * @return
     *   0 for success, otherwise error message id.
     */
    public int
    addToPlaylist(final YTVideoSearchAdapter adapter,
                  final long plid, final int pos, final int volume) {
        // NOTE
        // This function is designed to be able to run in background.
        // But, getting volume is related with YTPlayer instance.
        // And lots of functions of YTPlayer instance, requires running on UI Context
        //   to avoid synchronization issue.
        // So, volume should be gotten out of this function.
        P.bug(plid >= 0);

        Bitmap bm = adapter.getItemThumbnail(pos);
        if (null == bm) {
            return R.string.msg_no_thumbnail;
        }

        final YTDataAdapter.Video ytv = (YTDataAdapter.Video)adapter.getItem(pos);
        DMVideo v = new DMVideo();
        v.setYtData(ytv);
        v.setThumbnail(ImgUtil.compressToJpeg(bm));
        v.setPreferenceData(volume, "");
        DB.Err err = mDb.insertVideoToPlaylist(plid, v);
        if (DB.Err.NO_ERR != err) {
            if (DB.Err.DUPLICATED == err)
                return R.string.msg_existing_muisc;
            else
                return Err.map(err).getMessage();
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void
            run() {
                adapter.setToDup(pos);
            }
        });

        return 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void
    onListItemClick(View view, int position, long itemId) {
        if (!Util.isNetworkAvailable()) {
            showTextToast(Err.IO_NET.getMessage());
            return;
        }

        YTPlayer.Video v = getAdapter().getYTPlayerVideo(position);
        mMp.startVideos(new YTPlayer.Video[] { v });
    }

    @Override
    protected void
    onSearchResponse(@NonNull YTVideoListTask ytvl,
                     @NonNull YTDataAdapter.VideoListReq req,
                     @NonNull YTDataAdapter.VideoListResp resp,
                     @NonNull Err err) {
        checkDupAsync(req, resp.vids);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void
    onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ytvideosearch_context, menu);
        AdapterView.AdapterContextMenuInfo mInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;

        // menu 'videos of same channel' is useless if we are already in the same-type-search
        boolean visible = Util.isValidValue(getAdapter().getItemChannelTitle(mInfo.position))
                          && YTDataAdapter.ReqType.VID_CHANNEL != getSearchType();
        menu.findItem(R.id.videos_of_same_channel).setVisible(visible);
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
            case R.id.add_to:
                onContextMenuAddTo(info.position);
                return true;

            case R.id.append_to_playq:
                onContextMenuAppendToPlayQ(info.position);
                return true;

            case R.id.play_video:
                onContextMenuPlayVideo(info.position);
                return true;

            case R.id.videos_of_same_channel:
                onContextMenuVideosOfSameChannel(info.position);
                return true;

            case R.id.search_similar_titles:
                onContextMenuSearchSimilarTitles(info.position);
                return true;
        }
        return false;
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UnexpectedExceptionHandler.get().registerModule(this);
    }

    @Override
    public void
    onResume() {
        super.onResume();

        mMp.addOnDbUpdatedListener(mOnPlayerUpdateDbListener);
        if (mDb.isRegisteredToVideoTableWatcher(this)) {
            if (mDb.isVideoTableUpdated(this)
            && null != getAdapter()) {
                enableContentLoading();
                checkDupAsync(null, getAdapter().getItems());
            }
            mDb.unregisterToVideoTableWatcher(this);
        }
    }

    @Override
    public void
    onPause() {
        mMp.removeOnDbUpdatedListener(mOnPlayerUpdateDbListener);
        mDb.registerToVideoTableWatcher(this);
        super.onPause();
    }

    @Override
    protected void
    onDestroy() {
        mDb.unregisterToVideoTableWatcher(this);
        UnexpectedExceptionHandler.get().unregisterModule(this);
        super.onDestroy();
    }
}
