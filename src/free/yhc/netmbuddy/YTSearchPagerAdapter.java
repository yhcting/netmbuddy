package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import static free.yhc.netmbuddy.utils.Utils.logI;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.utils.Utils;

public class YTSearchPagerAdapter extends FragmentPagerAdapterEx {
    private final Class<? extends YTSearchFragment> mFragmentClass;
    private final YTSearchHelper.SearchType mSearchType;
    private final String                    mSearchText;
    private final String                    mSearchTitle;

    private final YTSearchFragment.OnSearchDoneListener mFirstSearchDone = new OnFirstSearchDone();


    private boolean mInitialized    = false;
    private int     mTotalResults   = -1;
    private int     mNrPages        = 1; // at least one page is required even if it's empty.
    private OnInitializedListener    mOnInitializedListener = null;

    private int     mPrimaryPos     = -1;


    interface OnInitializedListener {
        void onInitialized(YTSearchPagerAdapter adapter);
    }

    private class OnFirstSearchDone implements YTSearchFragment.OnSearchDoneListener {
        @Override
        public void
        onSearchDone(YTSearchFragment fragment,
                     Err result,
                     int totalResults) {
            if (Err.NO_MATCH == result)
                totalResults = 0;
            else if (Err.NO_ERR != result)
                return; // nothing to do.

            initialize(totalResults);
        }
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
            fragment.setOnSearchDoneListener(mFirstSearchDone);

        return fragment;
    }

    @Override
    public long
    getItemId(int position) {
        return position;
    }

    @Override
    public Parcelable
    saveState() {
        logI("PagerAdapter : saveState()");
        return super.saveState();
    }

    @Override
    public void
    restoreState(Parcelable state, ClassLoader loader) {
        logI("PagerAdapter : restoreState");
        super.restoreState(state, loader);
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
        logI("PagerAdapter : instantiateItem : " + position);
        return super.instantiateItem(container, position);
    }

    @Override
    public void
    destroyItem(ViewGroup container, int position, Object object) {
        logI("PagerAdapter : destroyItem : " + position);
        YTSearchFragment fragment = (YTSearchFragment)getFragmentManager()
                                        .findFragmentByTag(getFragmentName(position));
        super.destroyItem(container, position, object);
        if (null != fragment)
            fragment.onEarlyDestroy();
    }
}
