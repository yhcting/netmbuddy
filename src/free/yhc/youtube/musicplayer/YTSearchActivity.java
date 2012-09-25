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
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.Policy;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.YTFeed;
import free.yhc.youtube.musicplayer.model.YTPlayer;
import free.yhc.youtube.musicplayer.model.YTSearchHelper;

public abstract class YTSearchActivity extends Activity implements
YTSearchHelper.SearchDoneReceiver {
    private static final int NR_ENTRY_PER_PAGE = Policy.YTSEARCH_MAX_RESULTS;

    protected   final YTPlayer  mMp = YTPlayer.get();

    protected   YTSearchHelper  mSearchHelper;
    protected   ListView        mListv;     // viewHolder for ListView

    // Variable to store current activity state.
    private     YtSearchState   mSearchSt   = new YtSearchState();

    private     Button[]        mPageBtnHolder;
    private     LinearLayout.LayoutParams mPageBtnLPHolder;
    private     View.OnClickListener mPageOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int page = (Integer)v.getTag();
            loadPage(mSearchSt.type, mSearchSt.text, mSearchSt.title, page);
        }
    };

    private static class YtSearchState {
        YTSearchHelper.SearchType   type            = YTSearchHelper.SearchType.VID_KEYWORD;
        String                      text            = "";
        String                      title           = "";
        int                         curPage         = -1;
        int                         totalResults    = -1;
    }


    // ========================================================================
    //
    //
    //
    // ========================================================================
    private int
    getStarti(int pageNum) {
        int starti = (pageNum - 1) * NR_ENTRY_PER_PAGE + 1;
        return starti < 1? 1: starti;
    }

    private int
    getLastPage() {
        int page = (mSearchSt.totalResults - 1) / NR_ENTRY_PER_PAGE + 1;
        return page < 1? 1: page;
    }

    private void
    preparePageButtons() {
        mPageBtnHolder = new Button[Policy.YTSEARCH_NR_PAGE_INDEX];
        mPageBtnLPHolder =
                new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.ytsearch_idxbtn_width),
                                              getResources().getDimensionPixelSize(R.dimen.ytsearch_idxbtn_height));
        mPageBtnLPHolder.gravity = Gravity.CENTER_VERTICAL;
        for (int i = 0; i < mPageBtnHolder.length; i++) {
            mPageBtnHolder[i] = new Button(this);
            mPageBtnHolder[i].setTag(i);
            mPageBtnHolder[i].setOnClickListener(mPageOnClick);
        }
    }

    private void
    adjustPageUserAction() {
        int lastPage = getLastPage();
        eAssert(mSearchSt.curPage >= 1 && mSearchSt.curPage <= lastPage);

        View barv = findViewById(R.id.bottombar);
        ImageView nextBtn = (ImageView)barv.findViewById(R.id.next);
        ImageView prevBtn = (ImageView)barv.findViewById(R.id.prev);

        prevBtn.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);

        if (1 == mSearchSt.curPage)
            prevBtn.setVisibility(View.INVISIBLE);

        if (lastPage == mSearchSt.curPage)
            nextBtn.setVisibility(View.INVISIBLE);

        // Setup index buttons.
        LinearLayout ll = (LinearLayout)findViewById(R.id.indexgroup);
        ll.removeAllViews();
        int nrPages = mSearchSt.totalResults / NR_ENTRY_PER_PAGE + 1;
        int mini = mSearchSt.curPage - (Policy.YTSEARCH_NR_PAGE_INDEX / 2);
        if (mini < 1)
            mini = 1;

        int maxi = mini + Policy.YTSEARCH_NR_PAGE_INDEX - 1;
        if (maxi > nrPages) {
            maxi = nrPages;
            mini = maxi - Policy.YTSEARCH_NR_PAGE_INDEX + 1;
            if (mini < 1)
                mini = 1;
        }

        for (int i = mini; i <= maxi; i++) {
            int bi = i - mini;
            mPageBtnHolder[bi].setText("" + i);
            mPageBtnHolder[bi].setTag(i);
            mPageBtnHolder[bi].setBackgroundResource(R.drawable.btnbg_normal);
            ll.addView(mPageBtnHolder[bi], mPageBtnLPHolder);
        }
        mPageBtnHolder[mSearchSt.curPage - mini].setBackgroundResource(R.drawable.btnbg_focused);
    }

    /**
     *
     * @param pagei
     *   1-based page number
     */
    private void
    loadPage(YTSearchHelper.SearchType type, String text, String title, int pageNumber) {
        if (pageNumber < 1
            || pageNumber > getLastPage()) {
            UiUtils.showTextToast(this, R.string.err_ytsearch);
            return;
        }

        // close helper to cancel all existing work.
        mSearchHelper.close();

        // open again to support new search.
        mSearchHelper.open();
        YTSearchHelper.SearchArg arg
            = new YTSearchHelper.SearchArg(pageNumber,
                                           type,
                                           text,
                                           title,
                                           getStarti(pageNumber),
                                           NR_ENTRY_PER_PAGE);
        Err err = mSearchHelper.searchAsync(arg);
        if (Err.NO_ERR == err)
            showLoadingLookAndFeel();
        else
            UiUtils.showTextToast(this, err.getMessage());
    }

    private void
    loadNext() {
        loadPage(mSearchSt.type, mSearchSt.text, mSearchSt.title, mSearchSt.curPage + 1);
    }

    private void
    loadPrev() {
        eAssert(mSearchSt.curPage > 1);
        loadPage(mSearchSt.type, mSearchSt.text, mSearchSt.title, mSearchSt.curPage - 1);
    }


    // ========================================================================
    //
    //
    //
    // ========================================================================
    protected YTSearchHelper.SearchType
    getSearchType() {
        return mSearchSt.type;
    }

    protected void
    saveSearchArg(YTSearchHelper.SearchType type, String text, String title) {
        mSearchSt.type = type;
        mSearchSt.text = text;
        mSearchSt.title = title;
    }

    protected void
    setupToolBtn(int toolBtnDrawable,
                 View.OnClickListener onYtSearch) {
        View barv = findViewById(R.id.bottombar);
        ImageView iv = (ImageView)barv.findViewById(R.id.next);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNext();
            }
        });
        iv.setVisibility(View.INVISIBLE);

        iv = (ImageView)barv.findViewById(R.id.prev);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrev();
            }
        });
        iv.setVisibility(View.INVISIBLE);


        barv = barv.findViewById(R.id.toolbar);
        iv = (ImageView)barv.findViewById(R.id.toolbtn);
        if (toolBtnDrawable <= 0 || null == onYtSearch) {
            iv.setVisibility(View.GONE);
            iv.setOnClickListener(null);
        } else {
            iv.setImageResource(toolBtnDrawable);
            iv.setVisibility(View.VISIBLE);
            iv.setOnClickListener(onYtSearch);
        }
    }

    protected void
    showLoadingLookAndFeel() {
        View contentv = findViewById(R.id.content);
        View infov = findViewById(R.id.infolayout);

        ImageView iv = (ImageView)infov.findViewById(R.id.infoimg);
        TextView  tv = (TextView)infov.findViewById(R.id.infomsg);
        tv.setText(R.string.loading);
        infov.setVisibility(View.VISIBLE);
        contentv.setVisibility(View.GONE);
        iv.startAnimation(AnimationUtils.loadAnimation(YTSearchActivity.this, R.anim.rotate));
    }

    protected void
    stopLoadingLookAndFeel() {
        View contentv = findViewById(R.id.content);
        View infov = findViewById(R.id.infolayout);

        ImageView iv = (ImageView)infov.findViewById(R.id.infoimg);
        if (null != iv.getAnimation()) {
            iv.getAnimation().cancel();
            iv.getAnimation().reset();
        }
        infov.setVisibility(View.GONE);
        contentv.setVisibility(View.VISIBLE);
    }

    /**
     *
     * @param helper
     * @param arg
     * @param result
     * @param err
     * @return
     *   false : there is error in search, otherwise true.
     */
    protected boolean
    handleSearchResult(YTSearchHelper helper, YTSearchHelper.SearchArg arg,
                       YTFeed.Result result, Err err) {
        Err r = Err.NO_ERR;
        do {
            if (Err.NO_ERR != err) {
                r = err;
                break;
            }

            mSearchSt.curPage = (Integer)arg.tag;

            if (result.entries.length <= 0
                && 1 == mSearchSt.curPage) {
                r = Err.NO_MATCH;
                break;
            }

            try {
                mSearchSt.totalResults = Integer.parseInt(result.header.totalResults);
            } catch (NumberFormatException e) {
                r = Err.YTSEARCH;
                break;
            }
        } while (false);

        if (Err.NO_ERR != r) {
            stopLoadingLookAndFeel();
            UiUtils.showTextToast(this, r.getMessage());
            return false;
        }

        adjustPageUserAction();
        return true;
    }

    protected void
    loadFirstPage(YTSearchHelper.SearchType type, String text, String title) {
        loadPage(type, text, title, 1);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ytsearch);
        mListv = (ListView)findViewById(R.id.list);
        mListv.setEmptyView(UiUtils.inflateLayout(this, R.layout.ytsearch_empty_list));
        registerForContextMenu(mListv);
        mSearchHelper = new YTSearchHelper();
        mSearchHelper.setSearchDoneRecevier(this);

        preparePageButtons();
    }

    @Override
    protected void
    onResume() {
        super.onResume();
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        mMp.setController(this, playerv);
        if (mMp.hasActiveVideo())
            playerv.setVisibility(View.VISIBLE);
        else
            playerv.setVisibility(View.GONE);
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
        mSearchHelper.close();
        mMp.unsetController(this);
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
