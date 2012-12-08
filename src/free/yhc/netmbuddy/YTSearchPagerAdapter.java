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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.utils.Utils;

public class YTSearchPagerAdapter extends FragmentPagerAdapterEx {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTSearchPagerAdapter.class);

    private final Class<? extends YTSearchFragment> mFragmentClass;
    private final YTSearchHelper.SearchType mSearchType;
    private final String                    mSearchText;
    private final String                    mSearchTitle;

    private boolean mInitialized    = false;
    private int     mTotalResults   = -1;
    private int     mNrPages        = 1; // at least one page is required even if it's empty.
    private OnInitializedListener    mOnInitializedListener = null;

    private int     mPrimaryPos     = -1;


    interface OnInitializedListener {
        void onInitialized(YTSearchPagerAdapter adapter);
    }

    private int
    getLastPage(int totalResults) {
        int page = (totalResults - 1) / Policy.YTSEARCH_MAX_RESULTS + 1;
        return page < 1? 1: page;
    }

    private void
    initialize(int totalResults) {
        mInitialized = true;
        mTotalResults = totalResults;
        int lastPage = getLastPage(totalResults);
        mNrPages = lastPage;
        //notifyDataSetChanged();
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void
            run() {
                if (null != mOnInitializedListener)
                    mOnInitializedListener.onInitialized(YTSearchPagerAdapter.this);
            }
        });
    }

    public YTSearchPagerAdapter(FragmentManager fm,
                                Class<? extends YTSearchFragment> fragmentClass,
                                YTSearchHelper.SearchType searchType,
                                String searchText,
                                String searchTitle) {
        super(fm);
        mFragmentClass = fragmentClass;
        mSearchType = searchType;
        mSearchText = searchText;
        mSearchTitle = searchTitle;
    }

    public int
    pageToPos(int page) {
        return page - 1;
    }

    public int
    posToPage(int pos) {
        return pos + 1;
    }

    public void
    setOnInitializedListener(OnInitializedListener listener) {
        mOnInitializedListener = listener;
    }

    public int
    getTotalResults() {
        eAssert(mInitialized);
        return mTotalResults;
    }

    public int
    getNrPages() {
        return mNrPages;
    }

    public int
    getPrimaryPage() {
        return posToPage(mPrimaryPos);
    }

    public YTSearchFragment
    getPrimaryFragment() {
        return (YTSearchFragment) super.getCurrentPrimaryFragment();
    }

    public void
    onFragmentSearchDone(YTSearchFragment fragment,
                      Err result,
                      int totalResults) {
        if (mInitialized
            || 1 != fragment.getPage())
            return;

        if (Err.NO_MATCH == result)
            totalResults = 0;
        else if (Err.NO_ERR != result)
            return; // nothing to do.

        initialize(totalResults);
    }

    @Override
    public void
    setPrimaryItem(ViewGroup container, int position, Object object) {
        YTSearchFragment oldf = getPrimaryFragment();
        super.setPrimaryItem(container, position, object);
        YTSearchFragment newf = getPrimaryFragment();
        mPrimaryPos = position;
        if (oldf != newf) {
            if (null != oldf)
                oldf.setToPrimary(false);
            if (null != newf)
                newf.setToPrimary(true);
        }
    }

    @Override
    public int
    getCount() {
        return mNrPages;
    }

    @Override
    public Fragment
    getItem(int position) {
        YTSearchFragment fragment;
        try {
            fragment = mFragmentClass.newInstance();
        } catch (Exception e) {
            eAssert(false);
            return null;
        }

        // page : 1 based index / position : 0 based index.
        fragment.setAttributes(mSearchType, mSearchText, mSearchTitle, position + 1);
        if (0 == position
            && !mInitialized)
            // if this is first page of search.
            fragment.setSearchDoneResponseRequired(true);

        return fragment;
    }

    @Override
    public long
    getItemId(int position) {
        return position;
    }

    @Override
    public void
    startUpdate(ViewGroup container) {
        //logI("PagerAdapter : startUpdate");
        super.startUpdate(container);
    }

    @Override
    public void
    finishUpdate(ViewGroup container) {
        //logI("PagerAdapter : finishUpdate");
        super.finishUpdate(container);
    }

    @Override
    public Object
    instantiateItem(ViewGroup container, int position) {
        if (DBG) P.v("instantiateItem : " + position);
        return super.instantiateItem(container, position);
    }

    @Override
    public void
    destroyItem(ViewGroup container, int position, Object object) {
        if (DBG) P.v("destroyItem : " + position);
        YTSearchFragment fragment = (YTSearchFragment)getFragmentManager()
                                        .findFragmentByTag(getFragmentName(position));
        super.destroyItem(container, position, object);
        if (null != fragment)
            fragment.onEarlyDestroy();
    }

    @Override
    protected void
    onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void
    onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
    }
}
