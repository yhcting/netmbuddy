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

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.model.MultiThreadRunner;
import free.yhc.netmbuddy.model.MultiThreadRunner.Job;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTPlaylistFeed;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.model.YTVideoFeed;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;
import free.yhc.netmbuddy.utils.YTUtils;

public class YTPlaylistSearchActivity extends YTSearchActivity {

    private static class ImportYtPlaylistResult {
        AtomicInteger nrIgnored = new AtomicInteger(0);
        AtomicInteger nrDone    = new AtomicInteger(0);
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
    onListItemClick(View view, int position, long itemId) {
        Intent i = new Intent(this, YTVideoSearchPlaylistActivity.class);
        i.putExtra(MAP_KEY_SEARCH_TEXT,
                   getAdapter().getItemPlaylistId(position));
        i.putExtra(MAP_KEY_SEARCH_TITLE,
                   getAdapter().getItemTitle(position));
        startActivity(i);
    }

    private boolean
    insertVideoToPlaylist(long plid, YTVideoFeed.Entry e) {
        int playtm = 0;
        try {
             playtm = Integer.parseInt(e.media.playTime);
        } catch (NumberFormatException ex) {
            return false;
        }

        return YTUtils.insertVideoToPlaylist(plid,
                                             e.media.videoId,
                                             e.media.title,
                                             e.author.name,
                                             playtm,
                                             Policy.DEFAULT_VIDEO_VOLUME);
    }

    private Err
    doBackgroundImportYtPlaylist(final ImportYtPlaylistResult mtpr,
                                 MultiThreadRunner mtrunner,
                                 final long plid,
                                 final String ytplid,
                                 final ProgressListener progl)
        throws InterruptedException {
        DB db = DB.get();
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

        // NOTE
        // Hash map with Youtube video id, should be used because,
        //   youtube playlist may contain more than one video - duplicated video.
        // But, NetMBuddy doesn't allow duplicated video in the playlist.
        // So, use hashmap instead of linked list.
        HashMap<String, YTVideoFeed.Entry> map = new HashMap<String, YTVideoFeed.Entry>();
        int lastPv = -1;
        // PV : progress value
        int pvBase = 0;
        int pvPortion  = 10;
        do {
            sarg.starti = YTSearchHelper.MAX_NR_RESULT_PER_PAGE * (curPage - 1) + 1;
            sr = YTSearchHelper.search(sarg);
            checkInterrupted();
            if (YTSearchHelper.Err.NO_ERR != sr.err)
                return Err.map(sr.err);

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
                    map.put(e.media.videoId, e);
            }

            int curPv = pvBase + (curPage * 100 / maxPage) * pvPortion / 100;
            if (lastPv < curPv && null != progl) {
                lastPv = curPv;
                progl.onProgress(curPv);
            }

        } while (++curPage <= maxPage);

        // Loading video feeds from playlist is done.
        // It's time to load thumbnail and insert to DB.
        // Update progress base and portion.
        pvBase += pvPortion;
        pvPortion = 100 - pvBase; // all remains.

        final int progressBase = pvBase;
        MultiThreadRunner.OnProgressListener progListener = new MultiThreadRunner.OnProgressListener() {
            @Override
            public void
            onProgress(float prog) {
                progl.onProgress((int)(progressBase + prog * 100));
            }
        };
        mtrunner.setOnProgressListener(progListener);

        Err err = Err.NO_ERR;
        Map.Entry<String, YTVideoFeed.Entry>[] mes = map.entrySet().toArray(new Map.Entry[0]);
        for (Map.Entry<String, YTVideoFeed.Entry> me : mes) {
            final YTVideoFeed.Entry e = me.getValue();
            mtrunner.appendJob(new Job<Integer>((float)pvPortion/ (float)100 / mes.length) {
                @Override
                public Integer
                doJob() {
                    if (insertVideoToPlaylist(plid, e))
                        mtpr.nrDone.incrementAndGet();
                    else
                        mtpr.nrIgnored.incrementAndGet();
                    return 0;
                }
            });
        }

        mtrunner.waitAllDone();
        progl.onProgress(100);
        return err;
    }

    private void
    onContextMenuImportYtPlaylist(final long plid, final Object user) {
        final ImportYtPlaylistResult mtpr = new ImportYtPlaylistResult();
        final MultiThreadRunner mtrunner = new MultiThreadRunner(
                Utils.getUiHandler(),
                Policy.YTSEARCH_MAX_LOAD_THUMBNAIL_THREAD);

        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            private CharSequence
            getReportText(Err result) {
                CharSequence title = "";
                if (Err.NO_ERR != result)
                    title = Utils.getResText(result.getMessage());

                return title + "\n"
                       + "  " + Utils.getResText(R.string.done) + " : " + mtpr.nrDone.get() + "\n"
                       + "  " + Utils.getResText(R.string.error) + " : " + mtpr.nrIgnored.get();
            }

            private void
            onEnd(Err result) {
                UiUtils.showTextToast(YTPlaylistSearchActivity.this, getReportText(result));
            }

            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                onEnd(result);
            }

            @Override
            public void
            onCancel(DiagAsyncTask task) {
                mtrunner.cancel();
            }

            @Override
            public void
            onCancelled(DiagAsyncTask task) {
                onEnd(Err.CANCELLED);
            }

            @Override
            public Err
            doBackgroundWork(final DiagAsyncTask task) {
                ProgressListener prog = new ProgressListener() {
                    private int _mLastPercent = -1;
                    @Override
                    public void
                    onProgress(int percent) {
                        if (_mLastPercent < percent) {
                            task.publishProgress(percent);
                            _mLastPercent = percent;
                        }
                    }
                };

                Err err = Err.NO_ERR;
                try {
                    err = doBackgroundImportYtPlaylist(mtpr,
                                                       mtrunner,
                                                       plid,
                                                       getAdapter().getItemPlaylistId((Integer)user),
                                                       prog);
                } catch (InterruptedException e) {
                    err = Err.INTERRUPTED;
                }
                return err;
            }
        };
        new DiagAsyncTask(YTPlaylistSearchActivity.this,
                          worker,
                          DiagAsyncTask.Style.PROGRESS,
                          R.string.merging_playlist,
                          true,
                          false)
            .run();
    }

    private void
    onContextMenuImport(int pos) {
        UiUtils.OnPlaylistSelectedListener action = new UiUtils.OnPlaylistSelectedListener() {
            @Override
            public void
            onPlaylist(final long plid, final Object user) {
                onContextMenuImportYtPlaylist(plid, user);
            }

            @Override
            public void
            onUserMenu(int pos, Object user) { }
        };

        UiUtils.buildSelectPlaylistDialog(DB.get(),
                                          this,
                                          R.string.import_,
                                          null,
                                          action,
                                          DB.INVALID_PLAYLIST_ID,
                                          pos)
               .show();
    }

    @Override
    protected YTSearchHelper.SearchType
    getSearchType() {
        return YTSearchHelper.SearchType.PL_USER;
    }

    @Override
    public void
    searchDone(YTSearchHelper helper, YTSearchHelper.SearchArg arg,
               YTFeed.Result result, YTSearchHelper.Err err) {
        if (!handleSearchResult(helper, arg, result, err))
            return; // There is an error in search

        stopLoadingLookAndFeel();
        saveSearchArg(arg.text, arg.title);

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
        case R.id.import_:
            onContextMenuImport(info.position);
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

        setupBottomBar(R.drawable.ic_ytsearch,
                       new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doNewSearch();
            }
        },
                       0, null);

        String stext = getIntent().getStringExtra(MAP_KEY_SEARCH_TEXT);
        if (null != stext)
            loadFirstPage(getSearchType(), stext, stext);
        else
            doNewSearch();
    }
}
