package free.yhc.netmbuddy;

import java.util.concurrent.atomic.AtomicInteger;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public abstract class FragmentPagerAdapterEx extends PagerAdapter {
    private static final AtomicInteger sId = new AtomicInteger(0);

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
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

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

    @Override
    public Parcelable
    saveState() {
        return null;
    }

    @Override
    public void
    restoreState(Parcelable state, ClassLoader loader) { }
}
