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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.utils.Utils;
import free.yhc.netmbuddy.utils.YTUtils;

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
        int avaiableTotalResults = YTUtils.getAvailableTotalResults(totalResults);
        int page = (avaiableTotalResults - 1) / Policy.YTSEARCH_MAX_RESULTS + 1;
        return page < 1? 1: page;
    }

    private void
    initialize(int totalResults) {
        mInitialized = true;
        mTotalResults = totalResults;
        int lastPage = getLastPage(totalResults);
        mNrPages = lastPage;
        notifyDataSetChanged();
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
        if (DBG) P.v("pos : " + position);
        return super.instantiateItem(container, position);
    }

    @Override
    public void
    destroyItem(ViewGroup container, int position, Object object) {
        if (DBG) P.v("pos : " + position);
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
