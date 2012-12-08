package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTSearchHelper;

public abstract class YTSearchFragment extends Fragment implements
YTSearchHelper.SearchDoneReceiver {
    private static final int INVALID_PAGE   = -1;
    private static final int NR_ENTRY_PER_PAGE = Policy.YTSEARCH_MAX_RESULTS;

    protected YTSearchHelper    mSearchHelper;
    protected ListView          mListv  = null;
    protected ViewGroup         mRootv  = null;
    protected OnPostHandleSearchResultListener mPostHandleListener = null;

    private boolean mPrimary = false;
    private OnSearchDoneListener mSearchDoneListener = null;
    private YTSearchHelper.SearchType   mType;
    // TODO
    // refactoing is required for below two variable... is it really required?
    private String                      mText;
    private String                      mTitle;
    private int                         mPage = INVALID_PAGE;

    interface OnSearchDoneListener {
        void onSearchDone(YTSearchFragment fragment,
                          Err result,
                          int totalResults);
    }

    interface OnPostHandleSearchResultListener {
        void onPostHandle(YTVideoSearchFragment fragment, Err result);

    }

    private int
    getStarti(int pageNum) {
        int starti = (pageNum - 1) * NR_ENTRY_PER_PAGE + 1;
        return starti < 1? 1: starti;
    }
    /**
     *
     * @param pagei
     *   1-based page number
     */
    private void
    loadPage(YTSearchHelper.SearchType type, String text, String title, int pageNumber) {
        // close helper to cancel all existing work.
        mSearchHelper.close(true);

        // Create new helper instance to know owner instance at callback from helper.
        mSearchHelper = new YTSearchHelper();
        mSearchHelper.setSearchDoneRecevier(this);
        // open again to support new search.
        mSearchHelper.open();
        YTSearchHelper.SearchArg arg
            = new YTSearchHelper.SearchArg(pageNumber,
                                           type,
                                           text,
                                           title,
                                           getStarti(pageNumber),
                                           NR_ENTRY_PER_PAGE);
        YTSearchHelper.Err err = mSearchHelper.searchAsync(arg);
        if (YTSearchHelper.Err.NO_ERR == err)
            showLoadingLookAndFeel();
        else
            showErrorMessage(Err.map(err).getMessage());
    }


    // ========================================================================
    //
    //
    //
    // ========================================================================
    abstract protected void
    onListItemClick(View view, int position, long itemId);

    protected final YTSearchHelper.SearchType
    getType() {
        return mType;
    }

    private final YTSearchAdapter
    getAdapter() {
        if (null != mListv)
            return (YTSearchAdapter)mListv.getAdapter();
        return null;
    }

    protected void
    showLoadingLookAndFeel() {
        View loadingv = mRootv.findViewById(R.id.loading);
        if (View.VISIBLE == loadingv.getVisibility()) {
            eAssert(View.VISIBLE != mListv.getVisibility());
            return;
        }

        ImageView iv = (ImageView)loadingv.findViewById(R.id.loading_img);
        TextView  tv = (TextView)loadingv.findViewById(R.id.loading_msg);
        tv.setText(R.string.loading);
        loadingv.setVisibility(View.VISIBLE);
        mRootv.findViewById(R.id.error_text).setVisibility(View.GONE);
        mListv.setVisibility(View.GONE);
        iv.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.rotate));
    }

    protected void
    stopLoadingLookAndFeel() {
        View loadingv = mRootv.findViewById(R.id.loading);
        if (View.VISIBLE != loadingv.getVisibility())
            return;

        ImageView iv = (ImageView)loadingv.findViewById(R.id.loading_img);
        if (null != iv.getAnimation()) {
            iv.getAnimation().cancel();
            iv.setAnimation(null);
        }
        mRootv.findViewById(R.id.error_text).setVisibility(View.GONE);
        loadingv.setVisibility(View.GONE);
        mListv.setVisibility(View.VISIBLE);
    }

    protected void
    showErrorMessage(int msg) {
        mListv.setVisibility(View.GONE);
        mRootv.findViewById(R.id.loading).setVisibility(View.GONE);
        TextView tv = (TextView)mRootv.findViewById(R.id.error_text);
        tv.setVisibility(View.VISIBLE);
        tv.setText(msg);
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
                       YTFeed.Result result, YTSearchHelper.Err err) {
        if (null == mSearchHelper
            || mSearchHelper != helper
            || null == getActivity())
            return false;

        Err r = Err.NO_ERR;
        int totalResults = 0;
        do {
            if (YTSearchHelper.Err.NO_ERR != err) {
                r = Err.map(err);
                break;
            }

            int curPage = (Integer)arg.tag;

            if (result.entries.length <= 0
                && 1 == curPage) {
                r = Err.NO_MATCH;
                break;
            }

            try {
                totalResults = Integer.parseInt(result.header.totalResults);
            } catch (NumberFormatException e) {
                r = Err.YTSEARCH;
                break;
            }
        } while (false);

        if (null != mSearchDoneListener)
            mSearchDoneListener.onSearchDone(this,
                                             r,
                                             totalResults);

        if (Err.NO_ERR != r) {
            stopLoadingLookAndFeel();
            showErrorMessage(r.getMessage());
            return false;
        }
        return true;
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    public YTSearchFragment() {
    }

    public final void
    setToPrimary(boolean primary) {
        mPrimary = primary;
        if (null != getActivity()
            && null != mListv) {
            if (primary)
                getActivity().registerForContextMenu(mListv);
            else
                getActivity().unregisterForContextMenu(mListv);
        }
        onSetToPrimary(primary);
    }

    protected void
    onSetToPrimary(boolean primary) { }

    public final boolean
    isPrimary() {
        return mPrimary;
    }

    public final int
    getPage() {
        return mPage;
    }

    public final void
    setOnSearchDoneListener(OnSearchDoneListener listener) {
        mSearchDoneListener = listener;
    }

    public final void
    setAttributes(YTSearchHelper.SearchType type, String text, String title, int page) {
        mType = type;
        mText = text;
        mTitle = title;
        mPage = page;
    }

    public void
    reloadPage() {
        loadPage(mType, mText, mTitle, mPage);
    }

    public void
    onCreateContextMenu2(ContextMenu menu, View v, ContextMenuInfo menuInfo) { }

    @Override
    public void
    onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchHelper = new YTSearchHelper(); // initialization.
    }

    @Override
    public View
    onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FrameLayout fl = (FrameLayout)inflater.inflate(R.layout.ytsearch_fragment, null);
        mListv = (ListView)fl.findViewById(R.id.list);
        mRootv = fl;
        mListv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void
            onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                onListItemClick(view, position, itemId);
            }
        });
        setToPrimary(isPrimary());
        return fl;
    }

    @Override
    public void
    onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadPage(mType, mText, mTitle, mPage);
    }

    @Override
    public void
    onStart() {
        super.onStart();
    }

    @Override
    public void
    onResume() {
        super.onResume();
    }

    @Override
    public void
    onPause() {
        super.onPause();
    }

    @Override
    public void
    onStop() {
        super.onStop();
    }

    /**
     * Pager adapter - YTSearchPagerAdapter - calls this function when the fragment is no more used.
     * In general, onDestroy() is the callback for cleanup.
     * But, in case that system is very busy, calling onDestroy() is delayed
     *   and this may make huge amount of garbage resource usage.
     * To reduce above issue 'onEarlyDestroy()' is introduced.
     */
    public void
    onEarlyDestroy() {
        // release resources that should be freed as soon as possible.
        onDestroyViewInternal();
        onDestroyInternal();
    }

    private void
    onDestroyViewInternal() {
        if (null != getAdapter())
            getAdapter().cleanup();
    }

    @Override
    public void
    onDestroyView() {
        onDestroyViewInternal();
        super.onDestroyView();
    }

    private void
    onDestroyInternal() {
        mSearchHelper.close(true);
    }

    @Override
    public void
    onDestroy() {
        onDestroyInternal();
        super.onDestroy();
    }

    @Override
    public void
    onDetach() {
        super.onDetach();
    }
}
