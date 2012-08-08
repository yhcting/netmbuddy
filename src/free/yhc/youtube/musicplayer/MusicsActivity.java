package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
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
import free.yhc.youtube.musicplayer.model.MusicPlayer;
import free.yhc.youtube.musicplayer.model.UiUtils;

public class MusicsActivity extends Activity {

    private final DB            mDb = DB.get();
    private final MusicPlayer   mMp = MusicPlayer.get();

    private long        mPlid   = -1;
    private ListView    mListv  = null;


    private MusicsAdapter
    getAdapter() {
        return (MusicsAdapter)mListv.getAdapter();
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

        MusicPlayer.Music m = new MusicPlayer.Music(Uri.parse(c.getString(0)),
                                                    c.getString(1));
        c.close();

        View playerv = findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);
        mMp.setController(this, playerv);
        mMp.startMusicsAsync(new MusicPlayer.Music[] { m });
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.delete:
            mDb.deleteMusicFromPlayList(mPlid, info.id);
            getAdapter().reloadCursor(mPlid);
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
        // AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.musics);

        mPlid = getIntent().getLongExtra("plid", -1);
        eAssert(mPlid >= 0);

        String title = getIntent().getStringExtra("title");
        ((TextView)findViewById(R.id.title)).setText(title);

        byte[] imgdata = getIntent().getByteArrayExtra("thumbnail");
        UiUtils.setThumbnailImageView(((ImageView)findViewById(R.id.thumbnail)), imgdata);

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
        MusicsAdapter adapter = new MusicsAdapter(this, mPlid);
        mListv.setAdapter(adapter);
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

        super.onBackPressed();
    }
}
