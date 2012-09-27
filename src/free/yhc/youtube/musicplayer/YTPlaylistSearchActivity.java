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

package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;

import java.util.Iterator;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.Policy;
import free.yhc.youtube.musicplayer.model.RTState;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;
import free.yhc.youtube.musicplayer.model.YTFeed;
import free.yhc.youtube.musicplayer.model.YTPlaylistFeed;
import free.yhc.youtube.musicplayer.model.YTSearchHelper;
import free.yhc.youtube.musicplayer.model.YTVideoFeed;

public class YTPlaylistSearchActivity extends YTSearchActivity {

    private static class MergeToPlaylistResult {
        Err     err         = Err.NO_ERR;
        int     nrIgnored   = -1;
        int     nrDone      = -1;
        void init() {
            nrIgnored = nrDone = 0;
        }
    }

    private interface ProgressListener {
        void onProgress(int percent);
    }

    private YTPlaylistSearchAdapter
    getAdapter() {
        return (YTPlaylistSearchAdapter)mListv.getAdapter();
    }

    private void
    checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException();
    }

    private void
    doNewPlaylistSearch() {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }
            @Override
            public void onOk(Dialog dialog, EditText edit) {
                String user = edit.getText().toString();
                RTState.get().setLastSearchWord(edit.getText().toString());
                loadFirstPage(YTSearchHelper.SearchType.PL_USER, user, user);
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              R.string.enter_user_name,
                                                              RTState.get().getLastSearchWord(),
                                                              action);
        diag.show();
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        Intent i = new Intent(this, YTVideoSearchActivity.class);
        i.putExtra(YTVideoSearchActivity.INTENT_KEY_SEARCH_TYPE,
                   YTSearchHelper.SearchType.VID_PLAYLIST.name());
        i.putExtra(YTVideoSearchActivity.INTENT_KEY_SEARCH_TEXT,
                   getAdapter().getItemPlaylistId(position));
        i.putExtra(YTVideoSearchActivity.INTENT_KEY_SEARCH_TITLE,
                getAdapter().getItemTitle(position));
        startActivity(i);
    }

    private Err
    doBackgroundMergeToPlaylist(MergeToPlaylistResult mtpr,
                                final long plid,
                                final String ytplid,
                                final ProgressListener prog)
        throws InterruptedException {
        DB db = DB.get();
        YTSearchHelper ytsh = new YTSearchHelper();
        YTSearchHelper.SearchArg sarg = new YTSearchHelper.SearchArg(
                null,
                YTSearchHelper.SearchType.VID_PLAYLIST,
                ytplid,
                "",
                1,
                YTSearchHelper.MAX_NR_RESULT_PER_PAGE);
        YTSearchHelper.SearchReturn sr;
        int maxPage = -1;
        int curPage = 1;
        LinkedList<YTVideoFeed.Entry> plvl = new LinkedList<YTVideoFeed.Entry>();

        int lastPv = -1;
        // PV : progress value
        int pvBase = 0;
        int pvPortion  = 10;
        // NOTE
        // ytsh isn't open because, only synchronous interfaces are used here.
        do {
            sarg.starti = YTSearchHelper.MAX_NR_RESULT_PER_PAGE * (curPage - 1) + 1;
            sr = ytsh.search(sarg);
            checkInterrupted();
            if (Err.NO_ERR != sr.err)
                return sr.err;

            if (maxPage < 0) {
                int total;
                try {
                    total = Integer.parseInt(sr.r.header.totalResults);
                } catch (NumberFormatException e) {
                    return Err.YTSEARCH;
                }
                // Do only once.
                maxPage = total / YTSearchHelper.MAX_NR_RESULT_PER_PAGE + 1;
            }

            YTVideoFeed.Entry[] ents = (YTVideoFeed.Entry[])sr.r.entries;
            for (YTVideoFeed.Entry e : ents) {
                // Check that this video is in DB or not.
                // And add video only that is missed at selected local playlist
                if (!db.containsVideo(plid, e.media.videoId))
                    plvl.addLast(e);
            }

            int curPv = pvBase + (curPage * 100 / maxPage) * pvPortion / 100;
            if (lastPv < curPv && null != prog) {
                lastPv = curPv;
                prog.onProgress(curPv);
            }

        } while (++curPage <= maxPage);

        // Loading video feeds from playlist is done.
        // It's time to load thumbnail and insert to DB.
        Iterator<YTVideoFeed.Entry> itr = plvl.iterator();
        YTSearchHelper.LoadThumbnailArg targ = new YTSearchHelper.LoadThumbnailArg(
                null,
                "", // Not filled yet.
                Utils.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                Utils.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_height));

        mtpr.init();
        // Update progress base and portion.
        pvBase += pvPortion;
        pvPortion = 100 - pvBase; // all remains.
        while (itr.hasNext()) {
            int curPv = pvBase + (mtpr.nrDone + mtpr.nrIgnored) * 100 / plvl.size() * pvPortion / 100;
            if (lastPv < curPv && null != prog) {
                lastPv = curPv;
                prog.onProgress(curPv);
            }

            YTVideoFeed.Entry e = itr.next();

            int playtm = 0;
            try {
                 playtm = Integer.parseInt(e.media.playTime);
            } catch (NumberFormatException ex) {
                ++mtpr.nrIgnored;
                continue; // skip
            }

            YTSearchHelper.LoadThumbnailReturn tr;
            targ.url = e.media.thumbnailUrl;
            tr = ytsh.loadThumbnail(targ);
            checkInterrupted();
            if (Err.NO_ERR != tr.err) {
                ++mtpr.nrIgnored;
                continue; // skip this entry.
            }
            // Loading thumbnail is done.

            Err err = db.insertVideoToPlaylist(plid,
                                               e.media.title,
                                               e.media.description,
                                               e.media.videoId,
                                               playtm,
                                               Utils.compressBitmap(tr.bm),
                                               Policy.DEFAULT_VIDEO_VOLUME);
            if (null != tr.bm)
                tr.bm.recycle();

            if (Err.NO_ERR != err) {
                ++mtpr.nrIgnored;
                continue; // skip
            }

            ++mtpr.nrDone;
        }
        return Err.NO_ERR;
    }

    private void
    onContextMenuMergeTo(int pos) {
        UiUtils.OnPlaylistSelectedListener action = new UiUtils.OnPlaylistSelectedListener() {
            MergeToPlaylistResult mtpr = new MergeToPlaylistResult();

            @Override
            public void onPlaylist(final long plid, final Object user) {
                DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
                    @Override
                    public void onPostExecute(DiagAsyncTask task, Err result) {
                    }
                    @Override
                    public void onCancel(DiagAsyncTask task) {
                    }
                    @Override
                    public Err
                    doBackgroundWork(final DiagAsyncTask task, Object... objs) {
                        ProgressListener prog = new ProgressListener() {
                            @Override
                            public void onProgress(int percent) {
                                task.publishProgress(percent);
                            }
                        };

                        try {
                            return doBackgroundMergeToPlaylist(mtpr,
                                                               plid,
                                                               getAdapter().getItemPlaylistId((Integer)user),
                                                               prog);
                        } catch (InterruptedException e) {
                            return Err.INTERRUPTED;
                        }
                    }
                };
                new DiagAsyncTask(YTPlaylistSearchActivity.this,
                                  worker,
                                  DiagAsyncTask.Style.PROGRESS,
                                  R.string.merging_playlist,
                                  true)
                .execute();
            }
        };

        UiUtils.buildSelectPlaylistDialog(DB.get(), this, action, DB.INVALID_PLAYLIST_ID, pos).show();
    }

    @Override
    public void
    searchDone(YTSearchHelper helper, YTSearchHelper.SearchArg arg,
               YTFeed.Result result, Err err) {
        if (!handleSearchResult(helper, arg, result, err))
            return; // There is an error in search

        stopLoadingLookAndFeel();
        saveSearchArg(arg.type, arg.text, arg.title);

        String titleText = arg.text + getResources().getText(R.string.user_playlist_title_suffix);
        ((TextView)findViewById(R.id.title)).setText(titleText);


        // helper's event receiver is changed to adapter in adapter's constructor.
        YTPlaylistSearchAdapter adapter = new YTPlaylistSearchAdapter(this,
                                                                      mSearchHelper,
                                                                      (YTPlaylistFeed.Entry[])result.entries);
        YTPlaylistSearchAdapter oldAdapter = getAdapter();
        mListv.setAdapter(adapter);
        // Cleanup before as soon as possible to secure memories.
        if (null != oldAdapter)
            oldAdapter.cleanup();
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.merge_to:
            onContextMenuMergeTo(info.position);
            return true;

        }
        eAssert(false);
        return false;
    }

    @Override
    public void
    onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ytplaylistsearch_context, menu);
        // AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void
            onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                onListItemClick(view, position, itemId);
            }
        });

        setupToolBtn(R.drawable.ic_ytsearch,
                     new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doNewPlaylistSearch();
            }
        });
        doNewPlaylistSearch();
    }

    @Override
    protected void
    onResume() {
        super.onResume();
    }

    @Override
    protected void
    onPause() {
        super.onPause();
    }

    @Override
    protected void
    onStop() {
        super.onStop();
    }

    @Override
    protected void
    onDestroy() {
        super.onDestroy();
    }

    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Do nothing!
    }

    @Override
    public void
    onBackPressed() {
        super.onBackPressed();
    }
}
