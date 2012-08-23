package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.DB.ColMusic;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class MusicsActivity extends Activity {
    public static final long PLID_INVALID       = -100000;
    public static final long PLID_RECENT_PLAYED = -1;
    public static final long PLID_SEARCHED      = -2;

    private final DB            mDb = DB.get();
    private final YTJSPlayer    mMp = YTJSPlayer.get();

    private boolean     mPlayListChanged = false;

    private long        mPlid   = PLID_INVALID;
    private ListView    mListv  = null;


    private static boolean
    isUserPlayList(long plid) {
        return plid >= 0;
    }

    private MusicsAdapter
    getAdapter() {
        return (MusicsAdapter)mListv.getAdapter();
    }

    private void
    setToPlayListThumbnail(long musicid) {
        eAssert(isUserPlayList(mPlid));

        Cursor c = mDb.queryMusic(musicid, new ColMusic[] { ColMusic.THUMBNAIL });
        if (!c.moveToFirst()) {
            UiUtils.showTextToast(this, R.string.err_db_unknown);
            c.close();
            return;
        }

        byte[] data = c.getBlob(0);
        eAssert(null != data);
        c.close();

        mDb.updatePlayListThumbnail(mPlid, data);
        mPlayListChanged = true;
        // update current screen's thumbnail too.
        UiUtils.setThumbnailImageView(((ImageView)findViewById(R.id.thumbnail)), data);
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        Cursor c = mDb.queryMusic(itemId, new ColMusic[] { ColMusic.URL,
                                                           ColMusic.TITLE });
        if (!c.moveToFirst()) {
            UiUtils.showTextToast(this, R.string.err_unknown);
            c.close();
            return;
        }

        View playerv = findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);
        mMp.setController(this, playerv);
        mMp.startMusicsAsync(c, 0, 1, Utils.isPrefSuffle());
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.delete:
            eAssert(isUserPlayList(mPlid));
            mDb.deleteMusicFromPlayList(mPlid, info.id);
            getAdapter().reloadCursorAsync();
            return true;

        case R.id.plthumbnail:
            setToPlayListThumbnail(info.id);
            return true;
        }
        return false;
    }

    @Override
    public void
    onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.musics_context, menu);
        //AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;

        if (isUserPlayList(mPlid))
            menu.findItem(R.id.plthumbnail).setVisible(true);
        else
            menu.findItem(R.id.plthumbnail).setVisible(false);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.musics);

        String searchWord = null;
        mPlid = getIntent().getLongExtra("plid", PLID_INVALID);
        eAssert(PLID_INVALID != mPlid);

        if (isUserPlayList(mPlid)) {
            String title = getIntent().getStringExtra("title");
            ((TextView)findViewById(R.id.title)).setText(title);

            byte[] imgdata = getIntent().getByteArrayExtra("thumbnail");
            UiUtils.setThumbnailImageView(((ImageView)findViewById(R.id.thumbnail)), imgdata);
        } else if (PLID_RECENT_PLAYED == mPlid) {
            ((TextView)findViewById(R.id.title)).setText(R.string.recent_played);
            ((ImageView)findViewById(R.id.thumbnail)).setImageResource(R.drawable.ic_recent_played_up);
        } else if (PLID_SEARCHED == mPlid) {
            String word = getIntent().getStringExtra("word");
            searchWord = (null == word)? "": word;
            String title = Utils.getAppContext().getResources().getText(R.string.search_word) + " : " + word;
            ((TextView)findViewById(R.id.title)).setText(title);
            ((ImageView)findViewById(R.id.thumbnail)).setImageResource(R.drawable.ic_search_list_up);
        }

        mListv = (ListView)findViewById(R.id.list);
        //mListv.setEmptyView(UiUtils.inflateLayout(this, R.layout.ytsearch_empty_list));
        registerForContextMenu(mListv);
        mListv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void
            onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                onListItemClick(view, position, itemId);
            }
        });
        MusicsAdapter adapter = new MusicsAdapter(this, new MusicsAdapter.CursorArg(mPlid, searchWord));
        mListv.setAdapter(adapter);
        adapter.reloadCursorAsync();
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
        super.onDestroy();

    }

    @Override
    protected void
    onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK != resultCode)
            return;

        switch (requestCode) {

        }
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
        mMp.unsetController(this);
        if (mPlayListChanged) {
            Intent i = new Intent();
            i.putExtra(YTMPActivity.KEY_PLCHANGED, true);
            setResult(RESULT_OK, i);
        }
        super.onBackPressed();
    }
}
