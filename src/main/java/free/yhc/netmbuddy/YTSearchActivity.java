/******************************************************************************
 * Copyright (C) 2012, 2013, 2014
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

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.core.SearchSuggestionProvider;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.core.YTSearchHelper;
import free.yhc.netmbuddy.utils.Utils;

public abstract class YTSearchActivity extends FragmentActivity {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTSearchActivity.class);

    public static final String  MAP_KEY_SEARCH_TYPE     = "searchtype";
    public static final String  MAP_KEY_SEARCH_TEXT     = "searchtext";
    public static final String  MAP_KEY_SEARCH_TITLE    = "searchtitle";

    protected   final YTPlayer  mMp = YTPlayer.get();
    private     final ViewPager.OnPageChangeListener mPCListener = new OnPageViewChange();

    private     ViewPager       mPager  = null;
    private     YTSearchFragment    mContextMenuOwner = null;
    private     Button[]        mPageBtnHolder;
    private     LinearLayout.LayoutParams mPageBtnLPHolder;
    private     View.OnClickListener mPageOnClick = new View.OnClickListener() {
        @Override
        public void
        onClick(View v) {
            int page = (Integer)v.getTag();
            if (getPagerAdapter().getPrimaryPage() != page)
                mPager.setCurrentItem(getPagerAdapter().pageToPos(page));
            else
                // reload
                getPagerAdapter().getPrimaryFragment().reloadPage();
        }
    };

    private class OnPageViewChange implements ViewPager.OnPageChangeListener {
        @Override
        public void
        onPageSelected(int arg0) {
            if (DBG) P.v("arg : " + arg0);
            adjustPageUserAction(getPagerAdapter().posToPage(arg0));
        }

        @Override
        public void
        onPageScrolled(int arg0, float arg1, int arg2) {
            //logI("OnPageViewChange : onPageScrolled : " + arg0 + "/" + arg1 + "/" + arg2);
        }

        @Override
        public void
        onPageScrollStateChanged(int arg0) {
            if (DBG) P.v("arg : " + arg0);
            YTSearchActivity.this.closeContextMenu();
        }
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    private void
    preparePageButtons() {
        mPageBtnHolder = new Button[Policy.YTSEARCH_NR_PAGE_INDEX_BUTTONS];
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
    adjustPageUserAction(int curPage) {
        int nrPages = getPagerAdapter().getNrPages();
        eAssert(curPage >= 1 && curPage <= nrPages);

        // Setup index buttons.
        LinearLayout ll = (LinearLayout)findViewById(R.id.indexgroup);
        ll.removeAllViews();
        int mini = curPage - (Policy.YTSEARCH_NR_PAGE_INDEX_BUTTONS / 2);
        if (mini < 1)
            mini = 1;

        int maxi = mini + Policy.YTSEARCH_NR_PAGE_INDEX_BUTTONS - 1;
        if (maxi > nrPages) {
            maxi = nrPages;
            mini = maxi - Policy.YTSEARCH_NR_PAGE_INDEX_BUTTONS + 1;
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
        mPageBtnHolder[curPage - mini].setBackgroundResource(R.drawable.btnbg_focused);
    }

    private void
    setupButton(ImageView iv,
                int drawable,
                View.OnClickListener onClick) {
        if (drawable <= 0 || null == onClick) {
            iv.setVisibility(View.GONE);
            iv.setOnClickListener(null);
        } else {
            iv.setImageResource(drawable);
            iv.setVisibility(View.VISIBLE);
            iv.setOnClickListener(onClick);
        }
    }

    private void
    disablePageIndexBar() {
        findViewById(R.id.indexbar).setVisibility(View.INVISIBLE);
    }

    private void
    enablePageIndexBar() {
        findViewById(R.id.indexbar).setVisibility(View.VISIBLE);
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    protected abstract YTSearchHelper.SearchType
    getSearchType();

    protected abstract Class<? extends YTSearchFragment>
    getFragmentClass();

    protected abstract void
    onSearchMetaInformationReady(String text, String title, int totalResults);

    protected YTSearchPagerAdapter
    getPagerAdapter() {
        return (YTSearchPagerAdapter)mPager.getAdapter();
    }

    protected void
    setupToolBtn1(int drawable,
                  View.OnClickListener onClick) {
        setupButton((ImageView)findViewById(R.id.toolbtn1), drawable, onClick);
    }

    protected void
    setupToolBtn2(int drawable,
                  View.OnClickListener onClick) {
        setupButton((ImageView)findViewById(R.id.toolbtn2), drawable, onClick);
    }

    protected void
    setupBottomBar(int tool1Drawable,
                   View.OnClickListener tool1OnClick,
                   int tool2Drawable,
                   View.OnClickListener tool2OnClick) {
        setupToolBtn1(tool1Drawable, tool1OnClick);
        setupToolBtn2(tool2Drawable, tool2OnClick);
    }

    protected void
    doNewSearch() {
        onSearchRequested();
    }

    protected void
    startNewSearch(final String text, final String title) {
        // Creating adapter and attaching it will lead to start loading pages.
        YTSearchPagerAdapter adapter = new YTSearchPagerAdapter(
                getSupportFragmentManager(),
                getFragmentClass(),
                getSearchType(),
                text,
                title);
        adapter.setOnInitializedListener(new YTSearchPagerAdapter.OnInitializedListener() {
            @Override
            public void
            onInitialized(YTSearchPagerAdapter adapter) {
                adjustPageUserAction(1); // first page.
                enablePageIndexBar();
                onSearchMetaInformationReady(text, title, adapter.getTotalResults());
            }
        });
        mPager.setAdapter(adapter);
    }
    // ========================================================================
    //
    // Bridge between App and Fragment.
    //
    // NOTE
    // Fragment is destroyed and re-instantiated inside FragmentManager implicitly.
    // So, saving/restoring runtime state of Fragment is mandatory.
    // But, reference of class instance is NOT allowed to save!
    // That is, Fragment SHOULD NOT have any runtime data that cannot be stored
    //   as Parcelable data.
    // That's why this bridge is required.
    //
    // ========================================================================
    public boolean
    isContextMenuOwner(YTSearchFragment fragment) {
        return fragment == mContextMenuOwner;
    }

    public void
    onFragmentSearchDone(YTSearchFragment fragment,
                         Err result,
                         int totalResults) {
        if (null != getPagerAdapter())
            getPagerAdapter().onFragmentSearchDone(fragment, result, totalResults);
    }


    // ========================================================================
    //
    //
    //
    // ========================================================================
    @Override
    public void
    onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mContextMenuOwner = getPagerAdapter().getPrimaryFragment();
        mContextMenuOwner.onCreateContextMenu2(menu, v, menuInfo);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ytsearch);
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setOnPageChangeListener(mPCListener);
        preparePageButtons();
        disablePageIndexBar();
    }

    @Override
    protected void
    onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!Intent.ACTION_SEARCH.equals(intent.getAction()))
            return; // ignore unexpected intent

        final String query = intent.getStringExtra(SearchManager.QUERY);
        SearchSuggestionProvider.saveRecentQuery(query);
        disablePageIndexBar();
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void
            run() {
                startNewSearch(query, query);
            }
        });
    }

    @Override
    protected void
    onResume() {
        // onResume of each fragments SHOULD be called after 'setController'.
        // So, super.onResume() is located at the bottom of onResume().
        super.onResume();

        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        mMp.setController(this,
                          playerv,
                          (ViewGroup)findViewById(R.id.list_drawer),
                          null,
                          mMp.getVideoToolButton());
        if (mMp.hasActiveVideo())
            playerv.setVisibility(View.VISIBLE);
        else
            playerv.setVisibility(View.GONE);
    }

    @Override
    protected void
    onPause() {
        mMp.unsetController(this);
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
