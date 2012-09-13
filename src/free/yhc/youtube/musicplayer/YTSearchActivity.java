package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.DBHelper;
import free.yhc.youtube.musicplayer.model.DBHelper.CheckExistArg;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.Policy;
import free.yhc.youtube.musicplayer.model.RTState;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;
import free.yhc.youtube.musicplayer.model.YTPlayer;
import free.yhc.youtube.musicplayer.model.YTSearchApi;
import free.yhc.youtube.musicplayer.model.YTSearchHelper;

public class YTSearchActivity extends Activity implements
YTSearchHelper.SearchDoneReceiver,
DBHelper.CheckExistDoneReceiver {
    private static final int NR_ENTRY_PER_PAGE = Policy.YTSEARCH_MAX_RESULTS;

    private final DB            mDb = DB.get();
    private final YTPlayer      mMp = YTPlayer.get();

    private YTSearchHelper  mSearchHelper;
    private DBHelper        mDbHelper;
    private ListView        mListv;     // viewHolder for ListView

    // Variable to store current activity state.
    private String  mCurSearchWord = "";
    private int     mCurPage       = -1; // current page number
    private int     mTotalResults  = -1;

    private Button[]  mPageBtnHolder;
    private LinearLayout.LayoutParams mPageBtnLPHolder;
    private View.OnClickListener mPageOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int page = (Integer)v.getTag();
            loadPage(mCurSearchWord, page);
        }
    };

    private int
    getStarti(int pageNum) {
        int starti = (pageNum - 1) * NR_ENTRY_PER_PAGE + 1;
        return starti < 1? 1: starti;
    }

    private int
    getLastPage() {
        int page = (mTotalResults - 1) / NR_ENTRY_PER_PAGE + 1;
        return page < 1? 1: page;
    }

    private YTSearchAdapter
    getAdapter() {
        return (YTSearchAdapter)mListv.getAdapter();
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

    private void
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

    private void
    adjustPageUserAction() {
        int lastPage = getLastPage();
        eAssert(mCurPage >= 1 && mCurPage <= lastPage);

        View barv = findViewById(R.id.bottombar);
        ImageView nextBtn = (ImageView)barv.findViewById(R.id.next);
        ImageView prevBtn = (ImageView)barv.findViewById(R.id.prev);

        prevBtn.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);

        if (1 == mCurPage)
            prevBtn.setVisibility(View.INVISIBLE);

        if (lastPage == mCurPage)
            nextBtn.setVisibility(View.INVISIBLE);

        // Setup index buttons.
        LinearLayout ll = (LinearLayout)findViewById(R.id.indexgroup);
        ll.removeAllViews();
        int nrPages = mTotalResults / NR_ENTRY_PER_PAGE + 1;
        int mini = mCurPage - (Policy.YTSEARCH_NR_PAGE_INDEX / 2);
        if (mini < 1)
            mini = 1;

        int maxi = mini + Policy.YTSEARCH_NR_PAGE_INDEX - 1;
        if (maxi > nrPages)
            maxi = nrPages;

        for (int i = mini; i <= maxi; i++) {
            int bi = i - mini;
            mPageBtnHolder[bi].setText("" + i);
            mPageBtnHolder[bi].setTag(i);
            mPageBtnHolder[bi].setBackgroundResource(R.drawable.btnbg_normal);
            ll.addView(mPageBtnHolder[bi], mPageBtnLPHolder);
        }
        mPageBtnHolder[mCurPage - mini].setBackgroundResource(R.drawable.btnbg_focused);
    }

    /**
     *
     * @param pagei
     *   1-based page number
     */
    private void
    loadPage(String word, int pageNumber) {
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
                                           word,
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
        loadPage(mCurSearchWord, mCurPage + 1);
    }

    private void
    loadPrev() {
        eAssert(mCurPage > 1);
        loadPage(mCurSearchWord, mCurPage - 1);
    }

    private void
    doNewSearch() {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }
            @Override
            public void onOk(Dialog dialog, EditText edit) {
                String word = edit.getText().toString();
                RTState.get().setLastSearchWord(edit.getText().toString());
                loadPage(word, 1);
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              R.string.enter_search_word,
                                                              RTState.get().getLastSearchWord(),
                                                              action);
        diag.show();
    }

    private void
    setupTopBar() {
        //View barv = findViewById(R.id.topbar);
    }

    private void
    setupBottomBar() {
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
        iv = (ImageView)barv.findViewById(R.id.search);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doNewSearch();
            }
        });
    }

    private void
    addToPlaylist(long plid, int position) {
        eAssert(plid >= 0);
        Bitmap bm = getAdapter().getItemThumbnail(position);
        if (null == bm) {
            UiUtils.showTextToast(this, R.string.msg_no_thumbnail);
            return;
        }
        final YTSearchApi.Entry entry = (YTSearchApi.Entry)getAdapter().getItem(position);
        int playtm = 0;
        try {
             playtm = Integer.parseInt(entry.media.playTime);
        } catch (NumberFormatException ex) {
            UiUtils.showTextToast(this, R.string.msg_unknown_format);
            return;
        }

        int volume = DB.INVALID_VOLUME;
        YTPlayer ytp = YTPlayer.get();
        String runningYtVid = ytp.getPlayVideoYtId();
        if (null != runningYtVid
            && runningYtVid.equals(getAdapter().getItemVideoId(position)))
            volume = ytp.getVideoVolume();

        if (DB.INVALID_VOLUME == volume)
            volume = Policy.DEFAULT_VIDEO_VOLUME;

        Err err = mDb.insertVideoToPlaylist(plid,
                                            entry.media.title, entry.media.description,
                                            entry.media.videoId, playtm,
                                            Utils.compressBitmap(bm), volume);
        if (Err.NO_ERR != err) {
            if (Err.DB_DUPLICATED == err)
                UiUtils.showTextToast(this, R.string.msg_existing_muisc);
            else
                UiUtils.showTextToast(this, err.getMessage());
            return;
        }

        getAdapter().markEntryExist(position);
    }

    private void
    addToNewPlaylist(final int position) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }

            @Override
            public void onOk(Dialog dialog, EditText edit) {
                String title = edit.getText().toString();
                if (mDb.doesPlaylistExist(title)) {
                    UiUtils.showTextToast(YTSearchActivity.this, R.string.msg_existing_playlist);
                    return;
                }

                long plid = mDb.insertPlaylist(title, "");
                if (plid < 0) {
                    UiUtils.showTextToast(YTSearchActivity.this, R.string.err_db_unknown);
                } else {
                    addToPlaylist(plid, position);
                }
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this, R.string.enter_playlist_title, action);
        diag.show();
    }

    private void
    onAddToPlaylist(final int position) {
        UiUtils.OnPlaylistSelectedListener action = new UiUtils.OnPlaylistSelectedListener() {
            @Override
            public void onPlaylist(long plid, Object user) {
                addToPlaylist(plid, (Integer)user);
            }
            @Override
            public void onNewPlaylist(Object user) {
                addToNewPlaylist((Integer)user);
            }
        };

        UiUtils.buildSelectPlaylistDialog(mDb, this, action, DB.INVALID_PLAYLIST_ID, position).show();
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);

        int playtime = 0;
        try {
            playtime = Integer.parseInt(getAdapter().getItemPlaytime(position));
        } catch (NumberFormatException e) { }

        YTPlayer.Video v = new YTPlayer.Video(
                getAdapter().getItemVideoId(position),
                getAdapter().getItemTitle(position),
                Policy.DEFAULT_VIDEO_VOLUME,
                playtime);
        mMp.setController(this, playerv);
        mMp.startVideos(new YTPlayer.Video[] { v });
    }

    @Override
    public void
    checkExistDone(DBHelper helper, CheckExistArg arg,
                   boolean[] results, Err err) {
        stopLoadingLookAndFeel();
        if (Err.NO_ERR != err || results.length != arg.ents.length) {
            UiUtils.showTextToast(this, R.string.err_db_unknown);
            return;
        }

        mCurSearchWord = (String)arg.tag;
        String titleText = getResources().getText(R.string.search_word) + " : " + mCurSearchWord;
        ((TextView)findViewById(R.id.title)).setText(titleText);

        // First request is done!
        // Now we know total Results.
        // Let's build adapter and enable list.
        for (int i = 0; i < results.length; i++)
            arg.ents[i].uflag = results[i]?
                                YTSearchAdapter.FENT_EXIST_DUP:
                                YTSearchAdapter.FENT_EXIST_NEW;

        // helper's event receiver is changed to adapter in adapter's constructor.
        YTSearchAdapter adapter = new YTSearchAdapter(this,
                                                      mSearchHelper,
                                                      arg.ents);
        mListv.setAdapter(adapter);
        adjustPageUserAction();
    }

    @Override
    public void
    searchDone(YTSearchHelper helper, YTSearchHelper.SearchArg arg,
               YTSearchApi.Result result, Err err) {
        Err r = Err.NO_ERR;
        do {
            if (Err.NO_ERR != err) {
                r = err;
                break;
            }

            mCurPage = (Integer)arg.tag;

            if (result.enties.length <= 0
                && 1 == mCurPage) {
                r = Err.NO_MATCH;
                break;
            }

            try {
                mTotalResults = Integer.parseInt(result.header.totalResults);
            } catch (NumberFormatException e) {
                r = Err.YTSEARCH;
                break;
            }
        } while (false);

        if (Err.NO_ERR != r) {
            stopLoadingLookAndFeel();
            UiUtils.showTextToast(this, r.getMessage());
            return;
        }

        mDbHelper.checkExistAsync(new DBHelper.CheckExistArg(arg.word, result.enties));
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.add_to_playlist:
            onAddToPlaylist(info.position);
            return true;

        }
        return false;
    }

    @Override
    public void
    onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ytsearch_context, menu);
        // AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ytsearch);
        mListv = (ListView)findViewById(R.id.list);
        mListv.setEmptyView(UiUtils.inflateLayout(this, R.layout.ytsearch_empty_list));
        registerForContextMenu(mListv);
        mListv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void
            onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                onListItemClick(view, position, itemId);
            }
        });

        mSearchHelper = new YTSearchHelper();
        mSearchHelper.setSearchDoneRecevier(this);

        mDbHelper = new DBHelper();
        mDbHelper.setCheckExistDoneReceiver(this);
        mDbHelper.open();

        preparePageButtons();
        setupTopBar();
        setupBottomBar();
        doNewSearch();
    }

    @Override
    protected void
    onResume() {
        super.onResume();
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        mMp.setController(this, playerv);
        if (mMp.isVideoPlaying())
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
        mDbHelper.close();
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
