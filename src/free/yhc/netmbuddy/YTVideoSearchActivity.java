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

import static free.yhc.netmbuddy.model.Utils.eAssert;

import java.util.Iterator;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.TextView;
import free.yhc.netmbuddy.model.DB;
import free.yhc.netmbuddy.model.DBHelper;
import free.yhc.netmbuddy.model.DBHelper.CheckExistArg;
import free.yhc.netmbuddy.model.Err;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.RTState;
import free.yhc.netmbuddy.model.UiUtils;
import free.yhc.netmbuddy.model.Utils;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.model.YTVideoFeed;

public class YTVideoSearchActivity extends YTSearchActivity implements
YTSearchHelper.SearchDoneReceiver,
DBHelper.CheckExistDoneReceiver {
    public static final String  INTENT_KEY_SEARCH_TYPE  = "searchtype";
    public static final String  INTENT_KEY_SEARCH_TEXT  = "searchtext";
    public static final String  INTENT_KEY_SEARCH_TITLE = "searchtitle";

    private final DB        mDb = DB.get();

    private DBHelper        mDbHelper;
    private int             mToolBtnSearchIcon = 0;
    private View.OnClickListener mToolBtnSearchAction;

    private YTVideoSearchAdapter.CheckStateListener mAdapterCheckListener
        = new YTVideoSearchAdapter.CheckStateListener() {
        @Override
        public void
        onStateChanged(int nrChecked, int pos, boolean checked) {
            if (0 == nrChecked) {
                setupToolBtn1(mToolBtnSearchIcon, mToolBtnSearchAction);
            } else {
                setupToolBtn1(R.drawable.ic_add, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addCheckedMusicsToPlaylist();
                    }
                });
            }
        }
    };

    private YTVideoSearchAdapter
    getAdapter() {
        return (YTVideoSearchAdapter)mListv.getAdapter();
    }

    private void
    doNewSearch(final YTSearchHelper.SearchType type, int title) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }
            @Override
            public void onOk(Dialog dialog, EditText edit) {
                String word = edit.getText().toString();
                RTState.get().setLastSearchWord(edit.getText().toString());
                loadFirstPage(type, word, word);
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              title,
                                                              RTState.get().getLastSearchWord(),
                                                              action);
        diag.show();
    }


    /**
     * @return
     *   0 for success, otherwise error message id.
     */
    private int
    addToPlaylist(final long plid, final int pos, final int volume) {
        // NOTE
        // This function is designed to be able to run in background.
        // But, getting volume is related with YTPlayer instance.
        // And lots of functions of YTPlayer instance, requires running on UI Context
        //   to avoid synchronization issue.
        // So, volume should be gotten out of this function.
        eAssert(plid >= 0);

        Bitmap bm = getAdapter().getItemThumbnail(pos);
        if (null == bm) {
            return R.string.msg_no_thumbnail;
        }

        final YTVideoFeed.Entry entry = (YTVideoFeed.Entry)getAdapter().getItem(pos);
        int playtm = 0;
        try {
             playtm = Integer.parseInt(entry.media.playTime);
        } catch (NumberFormatException ex) {
            return R.string.msg_unknown_format;
        }

        Err err = mDb.insertVideoToPlaylist(plid,
                                            entry.media.title, entry.media.description,
                                            entry.media.videoId, playtm,
                                            Utils.compressBitmap(bm), volume);
        if (Err.NO_ERR != err) {
            if (Err.DB_DUPLICATED == err)
                return R.string.msg_existing_muisc;
            else
                return err.getMessage();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void
            run() {
                getAdapter().markEntryExist(pos);
            }
        });

        return 0;
    }

    private void
    addCheckedMusicsToPlaylist() {
        // Scan to check all thumbnails are loaded.
        // And prepare data for background execution.
        YTVideoSearchAdapter adpr = getAdapter();
        final LinkedList<Integer> iteml = new LinkedList<Integer>();
        final LinkedList<Integer> volumel = new LinkedList<Integer>();
        for (int i = 0; i < adpr.getCount(); i++) {
            if (adpr.getItemMarkState(i)) {
                if (null == adpr.getItemThumbnail(i)) {
                    UiUtils.showTextToast(this, R.string.msg_no_all_thumbnail);
                    return;
                }
                iteml.add(i);
                volumel.add(adpr.getItemVolume(i));
            }
        }

        UiUtils.OnPlaylistSelectedListener action = new UiUtils.OnPlaylistSelectedListener() {
            private int failedCnt = 0;

            @Override
            public void onPlaylist(final long plid, Object user) {
                DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
                    @Override
                    public void
                    onPostExecute(DiagAsyncTask task, Err result) {
                        Iterator<Integer> iIter = iteml.iterator();
                        while (iIter.hasNext())
                            getAdapter().unmarkItemCheck(iIter.next());

                        if (failedCnt > 0) {
                            CharSequence msg = YTVideoSearchActivity.this.getResources().getText(R.string.msg_fails_to_add);
                            UiUtils.showTextToast(YTVideoSearchActivity.this,
                                                  msg + " : " + failedCnt);
                        }
                    }
                    @Override
                    public void
                    onCancel(DiagAsyncTask task) {
                    }
                    @Override
                    public Err
                    doBackgroundWork(DiagAsyncTask task, Object... objs) {
                        mDb.beginTransaction();
                        try {
                            Iterator<Integer> iIter = iteml.iterator();
                            Iterator<Integer> vIter = volumel.iterator();
                            while (iIter.hasNext()) {
                                if (0 != addToPlaylist(plid, iIter.next(), vIter.next()))
                                    failedCnt++;
                            }

                            mDb.setTransactionSuccessful();
                        } finally {
                            mDb.endTransaction();
                        }
                        return Err.NO_ERR;
                    }
                };

                new DiagAsyncTask(YTVideoSearchActivity.this, worker,
                                  DiagAsyncTask.Style.SPIN,
                                  R.string.adding, false).execute();

            }
        };

        UiUtils.buildSelectPlaylistDialog(mDb,
                                          this,
                                          R.string.add_to,
                                          action,
                                          DB.INVALID_PLAYLIST_ID,
                                          null)
               .show();
    }

    private void
    onContextMenuAddTo(final int position) {
        UiUtils.OnPlaylistSelectedListener action = new UiUtils.OnPlaylistSelectedListener() {
            @Override
            public void onPlaylist(long plid, Object user) {
                int pos = (Integer)user;
                int volume = getAdapter().getItemVolume(pos);
                int msg = addToPlaylist(plid, pos, volume);
                if (0 != msg)
                    UiUtils.showTextToast(YTVideoSearchActivity.this, msg);
            }
        };

        UiUtils.buildSelectPlaylistDialog(mDb,
                                          this,
                                          R.string.add_to,
                                          action,
                                          DB.INVALID_PLAYLIST_ID,
                                          position)
               .show();
    }

    private void
    onContextMenuAppendToPlayQ(final int position) {
        int playtime;
        try {
            playtime = Integer.parseInt(getAdapter().getItemPlaytime(position));
        } catch (NumberFormatException e) {
            UiUtils.showTextToast(this, R.string.err_unknown);
            return;
        }

        YTPlayer.Video vid = new YTPlayer.Video(getAdapter().getItemVideoId(position),
                                                getAdapter().getItemTitle(position),
                                                Policy.DEFAULT_VIDEO_VOLUME,
                                                playtime);

        if (!YTPlayer.get().appendToCurrentPlayQ(vid))
            UiUtils.showTextToast(this, R.string.err_unknown);
    }

    private void
    onContextMenuPlayVideo(final int position) {
        UiUtils.playAsVideo(this, getAdapter().getItemVideoId(position));
    }

    private void
    onContextMenuVideosOfThisAuthor(final int position) {
        loadFirstPage(YTSearchHelper.SearchType.VID_AUTHOR,
                      getAdapter().getItemAuthor(position),
                      getAdapter().getItemAuthor(position));
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);

        int playtime = 0;
        try {
            playtime = Integer.parseInt(getAdapter().getItemPlaytime(position));
        } catch (NumberFormatException e) { }

        YTPlayer.Video v = new YTPlayer.Video(
                getAdapter().getItemVideoId(position),
                getAdapter().getItemTitle(position),
                Policy.DEFAULT_VIDEO_VOLUME,
                playtime);
        mMp.setController(this, playerv, (ViewGroup)findViewById(R.id.list_drawer), null);
        mMp.startVideos(new YTPlayer.Video[] { v });
    }

    @Override
    public void
    searchDone(YTSearchHelper helper, YTSearchHelper.SearchArg arg,
               YTFeed.Result result, Err err) {
        if (!handleSearchResult(helper, arg, result, err))
            return; // There is an error in search

        mDbHelper.checkExistAsync(new DBHelper.CheckExistArg(arg, (YTVideoFeed.Entry[])result.entries));
    }

    @Override
    public void
    checkExistDone(DBHelper helper, CheckExistArg arg,
                   boolean[] results, Err err) {
        stopLoadingLookAndFeel();
        if (Err.NO_ERR != err || results.length != arg.ents.length) {
            UiUtils.showTextToast(this, R.string.err_db_unknown);
            return;
        }

        YTSearchHelper.SearchArg sarg = (YTSearchHelper.SearchArg)arg.tag;
        saveSearchArg(sarg.type, sarg.text, sarg.title);
        String titleText = "";
        switch (sarg.type) {
        case VID_KEYWORD:
            titleText += getResources().getText(R.string.keyword);
            break;

        case VID_AUTHOR:
            titleText += getResources().getText(R.string.author);
            break;

        case VID_PLAYLIST:
            titleText += getResources().getText(R.string.playlist);
            break;

        default:
            eAssert(false);
        }
        titleText += " : " + sarg.title;
        ((TextView)findViewById(R.id.title)).setText(titleText);

        // First request is done!
        // Now we know total Results.
        // Let's build adapter and enable list.
        for (int i = 0; i < results.length; i++)
            arg.ents[i].uflag = results[i]?
                                YTVideoSearchAdapter.FENT_EXIST_DUP:
                                YTVideoSearchAdapter.FENT_EXIST_NEW;

        // helper's event receiver is changed to adapter in adapter's constructor.
        YTVideoSearchAdapter adapter = new YTVideoSearchAdapter(this,
                                                               mSearchHelper,
                                                               mAdapterCheckListener,
                                                               arg.ents);
        YTVideoSearchAdapter oldAdapter = getAdapter();
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
        case R.id.add_to:
            onContextMenuAddTo(info.position);
            return true;

        case R.id.append_to_playq:
            onContextMenuAppendToPlayQ(info.position);
            return true;

        case R.id.play_video:
            onContextMenuPlayVideo(info.position);
            return true;

        case R.id.videos_of_this_author:
            onContextMenuVideosOfThisAuthor(info.position);
            return true;
        }
        return false;
    }

    @Override
    public void
    onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ytvideosearch_context, menu);
        // AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;
        boolean visible = (YTSearchHelper.SearchType.VID_AUTHOR == getSearchType())? false: true;
        menu.findItem(R.id.videos_of_this_author).setVisible(visible);

        visible = YTPlayer.get().hasActiveVideo();
        menu.findItem(R.id.append_to_playq).setVisible(visible);
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

        mDbHelper = new DBHelper();
        mDbHelper.setCheckExistDoneReceiver(this);
        mDbHelper.open();

        String stypeStr = getIntent().getStringExtra(INTENT_KEY_SEARCH_TYPE);
        final String stext = getIntent().getStringExtra(INTENT_KEY_SEARCH_TEXT);
        final String stitle = getIntent().getStringExtra(INTENT_KEY_SEARCH_TITLE);

        YTSearchHelper.SearchType stype = YTSearchHelper.SearchType.VID_KEYWORD;
        if (null != stypeStr) {
            for (YTSearchHelper.SearchType st : YTSearchHelper.SearchType.values()) {
                if (st.name().equals(stypeStr))
                    stype = st;
            }
        }

        final int diagTitle;
        switch (stype) {
        case VID_KEYWORD:
            mToolBtnSearchIcon = R.drawable.ic_ytsearch;
            diagTitle = R.string.enter_keyword;
            break;

        case VID_AUTHOR:
            mToolBtnSearchIcon = R.drawable.ic_ytsearch;
            diagTitle = R.string.enter_user_name;
            break;

        case VID_PLAYLIST:
            diagTitle = 0;
            break;

        default:
            diagTitle = 0;
            eAssert(false);
        }

        final YTSearchHelper.SearchType searchType = stype;
        mToolBtnSearchAction = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doNewSearch(searchType, diagTitle);
            }
        };
        setupBottomBar(mToolBtnSearchIcon, mToolBtnSearchAction,
                       0, null);


        if (null == stext)
            doNewSearch(searchType, R.string.enter_keyword);
        else
            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    String title = (null == stitle)? stext: stitle;
                    loadFirstPage(searchType, stext, title);
                }
            });
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
        mDbHelper.close();
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
