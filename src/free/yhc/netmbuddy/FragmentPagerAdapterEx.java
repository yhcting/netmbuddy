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
