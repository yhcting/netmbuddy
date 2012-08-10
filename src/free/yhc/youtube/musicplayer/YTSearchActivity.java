package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.DBHelper;
import free.yhc.youtube.musicplayer.model.DBHelper.CheckExistArg;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;
import free.yhc.youtube.musicplayer.model.YTSearchApi;
import free.yhc.youtube.musicplayer.model.YTSearchHelper;

public class YTSearchActivity extends Activity implements
YTSearchHelper.SearchDoneReceiver,
DBHelper.CheckExistDoneReceiver {
    private static final int NR_ENTRY_PER_PAGE = YTSearchApi.NR_SEARCH_MAX;

    private final DB        mDb = DB.get();
    private final MusicPlayer mMp = MusicPlayer.get();

    private YTSearchHelper  mSearchHelper;
    private DBHelper        mDbHelper;
    private ListView        mListv;     // viewHolder for ListView

    private boolean         mPlayListChanged = false;

    // Variable to store current activity state.
    private String  mCurSearchWord = "";
    private int     mCurPage       = -1; // current page number
    private int     mTotalResults  = -1;


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

        prevBtn.setClickable(true);
        prevBtn.setImageResource(R.drawable.ic_prev);
        nextBtn.setClickable(true);
        nextBtn.setImageResource(R.drawable.ic_next);

        if (1 == mCurPage) {
            prevBtn.setClickable(false);
            prevBtn.setImageResource(R.drawable.ic_block);
        }

        if (lastPage == mCurPage) {
            nextBtn.setClickable(false);
            nextBtn.setImageResource(R.drawable.ic_block);
        }
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
        showLoadingLookAndFeel();
        mSearchHelper.searchAsync(arg);
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
                loadPage(word, 1);
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              R.string.enter_search_word,
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
        iv.setClickable(false);

        iv = (ImageView)barv.findViewById(R.id.prev);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrev();
            }
        });
        iv.setClickable(false);


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
    addToPlayList(long plid, int position) {
        eAssert(plid >= 0);
        Bitmap bm = getAdapter().getItemThumbnail(position);
        if (null == bm) {
            UiUtils.showTextToast(this, R.string.msg_no_thumbnail);
            return;
        }
        final YTSearchApi.Entry entry = (YTSearchApi.Entry)getAdapter().getItem(position);
        int playtm = 0;
        try {
             playtm = Integer.parseInt(entry.media.content.playTime);
        } catch (NumberFormatException ex) {
            UiUtils.showTextToast(this, R.string.msg_unknown_format);
            return;
        }

        Err err = mDb.insertMusicToPlayList(plid,
                                            entry.media.title, entry.media.description,
                                            entry.media.content.url, playtm,
                                            Utils.compressBitmap(bm));
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
    addToNewPlayList(final int position) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }

            @Override
            public void onOk(Dialog dialog, EditText edit) {
                String title = edit.getText().toString();
                if (mDb.doesPlayListExist(title)) {
                    UiUtils.showTextToast(YTSearchActivity.this, R.string.msg_existing_playlist);
                    return;
                }

                long plid = mDb.insertPlayList(title, "");
                if (plid < 0) {
                    UiUtils.showTextToast(YTSearchActivity.this, R.string.msg_unknown_dberror);
                } else {
                    mPlayListChanged = true;
                    addToPlayList(plid, position);
                }
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this, R.string.enter_playlist_title, action);
        diag.show();
    }

    private void
    onAddToPlayList(final int position) {
        // Create menu list
        final Cursor c = mDb.queryPlayList(new DB.ColPlayList[] { DB.ColPlayList.ID,
                                                                  DB.ColPlayList.TITLE });

        final int iTitle = c.getColumnIndex(DB.ColPlayList.TITLE.getName());
        final int iId    = c.getColumnIndex(DB.ColPlayList.ID.getName());

        final String[] menus = new String[c.getCount() + 1]; // + 1 for 'new playlist'
        final long[]   ids   = new long[menus.length];
        menus[0] = getResources().getText(R.string.new_playlist).toString();
        c.moveToFirst();
        for (int i = 1; i < menus.length; i++) {
            menus[i] = c.getString(iTitle);
            ids[i] = c.getLong(iId);
            c.moveToNext();
        }
        c.close();

        AlertDialog.Builder bldr = new AlertDialog.Builder(this);
        ArrayAdapter<String> adapter
            = new ArrayAdapter<String>(this,android.R.layout.select_dialog_item, menus);
        bldr.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eAssert(which >= 0);
                if (0 == which)
                    addToNewPlayList(position);
                else
                    addToPlayList(ids[which], position);

                dialog.cancel();
            }
        });
        bldr.create().show();
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        View playerv = findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);

        MusicPlayer.Music m = new MusicPlayer.Music(
                Uri.parse(getAdapter().getItemUrl(position)),
                getAdapter().getItemTitle(position));
        mMp.setController(this, playerv);
        mMp.startMusicsAsync(new MusicPlayer.Music[] { m });
    }

    @Override
    public void
    checkExistDone(DBHelper helper, CheckExistArg arg,
                   boolean[] results, Err err) {
        stopLoadingLookAndFeel();
        if (Err.NO_ERR != err || results.length != arg.ents.length) {
            UiUtils.showTextToast(this, R.string.msg_unknown_dberror);
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
            onAddToPlayList(info.position);
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

        setupTopBar();
        setupBottomBar();
        doNewSearch();
    }

    @Override
    protected void
    onResume() {
        super.onResume();
        View playerv = findViewById(R.id.player);
        if (mMp.isMusicPlaying()) {
            playerv.setVisibility(View.VISIBLE);
            mMp.setController(this, playerv);
        } else {
            playerv.setVisibility(View.GONE);
            mMp.setController(this, null);
        }
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
        if (mPlayListChanged) {
            Intent i = new Intent();
            i.putExtra(YTMPActivity.KEY_PLCHANGED, true);
            setResult(Activity.RESULT_OK, i);
        }
        mMp.unsetController(this);
        super.onBackPressed();
    }
}
