/*****************************************************************************
 *    Copyright (C) 2012, 2013 Younghyung Cho. <yhcting77@gmail.com>
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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.model.YTVideoFeed;
import free.yhc.netmbuddy.utils.ImageUtils;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public abstract class YTVideoSearchActivity extends YTSearchActivity {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTVideoSearchActivity.class);

    private final DB        mDb = DB.get();
    private final YTPlayer  mMp = YTPlayer.get();

    private View.OnClickListener mToolBtnSearchAction;

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

    private YTVideoSearchAdapter
    getPrimaryListAdapter() {
        return ((YTVideoSearchFragment)getPagerAdapter().getPrimaryFragment()).getListAdapter();
    }

    private void
    addCheckedMusicsTo() {
        final int[] menuTextIds = new int[] { R.string.append_to_playq };

        final String[] userMenu = new String[menuTextIds.length];
        for (int i = 0; i < menuTextIds.length; i++)
            userMenu[i] = Utils.getResText(menuTextIds[i]);

        UiUtils.OnPlaylistSelected action = new UiUtils.OnPlaylistSelected() {
            @Override
            public void
            onPlaylist(final long plid, Object user) {
                addCheckedMusicsToPlaylist(plid);
            }

            @Override
            public void
            onUserMenu(int pos, Object user) {
                eAssert(0 <= pos && pos < menuTextIds.length);
                switch (menuTextIds[pos]) {
                case R.string.append_to_playq:
                    appendCheckMusicsToPlayQ();
                    break;

                default:
                    eAssert(false);
                }
            }
        };

        UiUtils.buildSelectPlaylistDialog(mDb,
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
        final YTVideoSearchAdapter adpr = getPrimaryListAdapter();
        final int[] checkedItems = adpr.getCheckedItem();
        final int[] itemVolumes = new int[checkedItems.length];
        for (int i = 0; i < checkedItems.length; i++) {
            int pos = checkedItems[i];
            if (null == adpr.getItemThumbnail(pos)) {
                UiUtils.showTextToast(this, R.string.msg_no_all_thumbnail);
                return;
            }
            itemVolumes[i] = adpr.getItemVolume(pos);
        }

        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            private int failedCnt = 0;

            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                adpr.cleanChecked();
                if (failedCnt > 0) {
                    CharSequence msg = getResources().getText(R.string.msg_fails_to_add);
                    UiUtils.showTextToast(YTVideoSearchActivity.this, msg + " : " + failedCnt);
                }
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                mDb.beginTransaction();
                try {
                    for (int i = 0; i < checkedItems.length; i++) {
                        int pos = checkedItems[i];
                        int r = addToPlaylist(getPrimaryListAdapter(), plid, pos, itemVolumes[i]);
                        if (0 != r && R.string.msg_existing_muisc != r)
                            failedCnt++;
                    }
                    mDb.setTransactionSuccessful();
                } finally {
                    mDb.endTransaction();
                }
                return Err.NO_ERR;
            }
        };

        new DiagAsyncTask(this,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          R.string.adding)
            .run();

    }

    private void
    appendCheckMusicsToPlayQ() {
        // # of adapter items are at most Policy.YTSEARCH_MAX_RESULTS
        // So, just do it at main UI thread!
        YTVideoSearchAdapter adpr = getPrimaryListAdapter();
        int[] checkedItems = adpr.getCheckItemSortedByTime();
        YTPlayer.Video[] vids = new YTPlayer.Video[checkedItems.length];
        int j = 0;
        for (int i : checkedItems) {
            vids[j++] = adpr.getYTPlayerVideo(i);
        }
        appendToPlayQ(vids);
        adpr.cleanChecked();
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    @Override
    protected Class<? extends YTSearchFragment>
    getFragmentClass() {
        return YTVideoSearchFragment.class;
    }

    @Override
    protected void
    onSearchMetaInformationReady(String text, String title, int totalResults) {
        String titleText = getTitlePrefix() + " : " + title;
        ((TextView)findViewById(R.id.title)).setText(titleText);
    }

    protected void
    onCreateInternal(String stext, String stitle) {
      mToolBtnSearchAction = new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                doNewSearch();
            }
        };

        setupBottomBar(getToolButtonSearchIcon(), mToolBtnSearchAction,
                       0, null);

        if (null != stext)
            startNewSearch(stext, stitle);
        else
            doNewSearch();
    }


    /**
     * Override it to enabel tool button for search.
     * @return
     */
    protected int
    getToolButtonSearchIcon() {
        return 0;
    }

    /**
     * Override it.
     * @return
     */
    protected String
    getTitlePrefix() {
        return "";
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
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
        eAssert(plid >= 0);

        Bitmap bm = adapter.getItemThumbnail(pos);
        if (null == bm) {
            return R.string.msg_no_thumbnail;
        }

        final YTVideoFeed.Entry entry = (YTVideoFeed.Entry)adapter.getItem(pos);
        int playtm = 0;
        try {
             playtm = Integer.parseInt(entry.media.playTime);
        } catch (NumberFormatException ex) {
            return R.string.msg_unknown_format;
        }

        DB.Err err = mDb.insertVideoToPlaylist(plid,
                                               entry.media.videoId,
                                               entry.media.title,
                                               entry.author.name,
                                               playtm,
                                               ImageUtils.compressBitmap(bm),
                                               volume);
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

    // ========================================================================
    //
    //
    //
    // ========================================================================
    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void
    onDestroy() {
        super.onDestroy();
    }
}
