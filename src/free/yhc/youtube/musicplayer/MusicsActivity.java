package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;
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
    setToPlayListThumbnail(long musicId, int itemPos) {
        eAssert(isUserPlayList(mPlid));
        byte[] data = getAdapter().getMusicThumbnail(itemPos);
        mDb.updatePlayListThumbnail(mPlid, data);
        mPlayListChanged = true;
        // update current screen's thumbnail too.
        UiUtils.setThumbnailImageView(((ImageView)findViewById(R.id.thumbnail)), data);
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        YTJSPlayer.Video vid = new YTJSPlayer.Video(getAdapter().getMusicYtid(position),
                                                    getAdapter().getMusicTitle(position),
                                                    getAdapter().getMusicVolume(position));
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);
        mMp.setController(this, playerv);
        mMp.startVideos(new YTJSPlayer.Video[] { vid });
    }

    private void
    onContextMenuVolume(final long itemId, final int itemPos) {
        final int oldVolume = getAdapter().getMusicVolume(itemPos);

        ViewGroup diagv = (ViewGroup)UiUtils.inflateLayout(this, R.layout.set_volume_dialog);
        AlertDialog.Builder bldr = new AlertDialog.Builder(this);
        bldr.setView(diagv);
        bldr.setTitle(R.string.volume);
        final AlertDialog aDiag = bldr.create();

        final SeekBar sbar = (SeekBar)diagv.findViewById(R.id.seekbar);
        sbar.setMax(100);
        sbar.setProgress(getAdapter().getMusicVolume(itemPos));
        sbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void
            onStopTrackingTouch(SeekBar seekBar) { }
            @Override
            public void
            onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void
            onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mMp.setVideoVolume(progress);
            }
        });

        aDiag.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void
            onDismiss(DialogInterface dialog) {
                int newVolume = sbar.getProgress();
                if (oldVolume == newVolume)
                    return;
                // Save to database and update adapter
                // NOTE
                // Should I consider about performance?
                // Not yet. do something when performance is issued.
                mDb.updateVideo(DB.ColVideo.ID, itemId, DB.ColVideo.VOLUME, sbar.getProgress());
                getAdapter().reloadCursor();
            }
        });

        aDiag.show();
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.delete:
            eAssert(isUserPlayList(mPlid));
            mDb.deleteVideoFromPlayList(mPlid, info.id);
            getAdapter().reloadCursorAsync();
            return true;

        case R.id.plthumbnail:
            setToPlayListThumbnail(info.id, info.position);
            return true;

        case R.id.volume:
            onContextMenuVolume(info.id, info.position);
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

        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        if (mMp.isVideoPlaying()) {
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
            i.putExtra(PlayListActivity.KEY_PLCHANGED, true);
            setResult(RESULT_OK, i);
        }
        super.onBackPressed();
    }
}
