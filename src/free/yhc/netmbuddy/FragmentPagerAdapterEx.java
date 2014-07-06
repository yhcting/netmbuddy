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

import java.util.concurrent.atomic.AtomicInteger;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import free.yhc.netmbuddy.utils.Utils;

public abstract class FragmentPagerAdapterEx extends PagerAdapter {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(FragmentPagerAdapterEx.class);

    private static final AtomicInteger sId = new AtomicInteger(0);

    private static final String KEY_PARENT_STATE = "FragmentPagerAdapterEx:parent_state";

    private final int mId;
    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;
    private Fragment mCurrentPrimaryItem = null;

    public FragmentPagerAdapterEx(FragmentManager fm) {
        mId = sId.incrementAndGet();
        mFragmentManager = fm;
    }

    protected final String
    getFragmentName(int position) {
        return "netmbuddy:FragmentPagerAdapterEx:" + mId + ":" + getItemId(position);
    }

    protected final FragmentManager
    getFragmentManager() {
        return mFragmentManager;
    }

    protected final Fragment
    getCurrentPrimaryFragment() {
        return mCurrentPrimaryItem;
    }

    /**
     * Return the Fragment associated with a specified position.
     */
    public abstract Fragment
    getItem(int position);

    public long
    getItemId(int position) {
        return position;
    }

    @Override
    public void
    startUpdate(ViewGroup container) { }

    @Override
    public Object
    instantiateItem(ViewGroup container, int position) {
        if (mCurTransaction == null)
            mCurTransaction = mFragmentManager.beginTransaction();

        // Do we already have this fragment?
        String name = getFragmentName(position);
        Fragment fragment = mFragmentManager.findFragmentByTag(name);
        if (fragment != null)
            mCurTransaction.attach(fragment);
        else {
            fragment = getItem(position);
            mCurTransaction.add(container.getId(),
                                fragment,
                                getFragmentName(position));
        }

        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
            fragment.setUserVisibleHint(false);
        }

        return fragment;
    }

    @Override
    public void
    destroyItem(ViewGroup container, int position, Object object) {
        if (mCurTransaction == null)
            mCurTransaction = mFragmentManager.beginTransaction();
        mCurTransaction.detach((Fragment)object);
        mCurTransaction.remove((Fragment)object);
    }

    @Override
    public void
    setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void
    finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean
    isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    protected void
    onSaveInstanceState(Bundle outState) { }

    protected void
    onRestoreInstanceState(Bundle inState) { }

    @Override
    public final Parcelable
    saveState() {
        Bundle data = new Bundle();
        data.putParcelable(KEY_PARENT_STATE, super.saveState());
        onSaveInstanceState(data);
        return data;
    }

    @Override
    public final void
    restoreState(Parcelable state, ClassLoader loader) {
        Bundle data = (Bundle)state;
        super.restoreState((null == data)? null: data.getParcelable(KEY_PARENT_STATE), loader);
        onRestoreInstanceState(data);
    }
}
