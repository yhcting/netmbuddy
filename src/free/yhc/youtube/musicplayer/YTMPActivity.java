package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import free.yhc.youtube.musicplayer.PlayListAdapter.ItemButton;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.DB.ColMusic;
import free.yhc.youtube.musicplayer.model.UiUtils;

public class YTMPActivity extends Activity {
    private static final int REQC_YTSEARCH  = 0;
    private static final int REQC_MUSICS    = 1;

    private final DB          mDb = DB.get();
    private final MusicPlayer mMp = MusicPlayer.get();
    private ListView mListv;

    private PlayListAdapter
    getAdapter() {
        return (PlayListAdapter)mListv.getAdapter();
    }

    private void
    startSearchingYoutube(View anchor) {
        Intent i = new Intent(this, YTSearchActivity.class);
        startActivityForResult(i, REQC_YTSEARCH);
    }

    private void
    setupToolButtons() {
        ((ImageView)findViewById(R.id.btn_ytsearch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearchingYoutube(v);
            }
        });
    }

    private void
    onContextRename(final AdapterContextMenuInfo info) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }
            @Override
            public void onOk(Dialog dialog, EditText edit) {
                String word = edit.getText().toString();
                mDb.updatePlayListName(info.id, word);
                getAdapter().reloadCursor();
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              R.string.enter_new_name,
                                                              action);
        diag.show();
    }

    private void
    onContextDelete(AdapterContextMenuInfo info) {
        mDb.deletePlayList(info.id);
        getAdapter().reloadCursor();
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        Cursor c = mDb.queryMusics(itemId, new ColMusic[] { ColMusic.URL,
                                                            ColMusic.TITLE });
        if (!c.moveToFirst()) {
            UiUtils.showTextToast(this, R.string.msg_empty_playlist);
            c.close();
            return;
        }

        MusicPlayer.Music[] ms = new MusicPlayer.Music[c.getCount()];
        int i = 0;
        do {
            ms[i++] = new MusicPlayer.Music(Uri.parse(c.getString(0)),
                                          c.getString(1));
        } while (c.moveToNext());
        c.close();

        View playerv = findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);
        mMp.setController(this, playerv);
        mMp.startMusicsAsync(ms);
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.rename:
            onContextRename(info);
            return true;

        case R.id.delete:
            onContextDelete(info);
            return true;
        }
        return false;
    }

    @Override
    public void
    onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.playlist_context, menu);
        // AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mListv = (ListView)findViewById(R.id.list);
        PlayListAdapter adapter = new PlayListAdapter(this, new PlayListAdapter.OnItemButtonClickListener() {
            @Override
            public void onClick(int pos, ItemButton button) {
                eAssert(PlayListAdapter.ItemButton.LIST == button);
                Intent i = new Intent(YTMPActivity.this, MusicsActivity.class);
                PlayListAdapter adapter = getAdapter();
                i.putExtra("plid", adapter.getItemId(pos));
                i.putExtra("title", adapter.getItemTitle(pos));
                i.putExtra("thumbnail", adapter.getItemThumbnail(pos));
                startActivityForResult(i, REQC_MUSICS);
            }
        });
        mListv.setAdapter(adapter);
        registerForContextMenu(mListv);
        mListv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void
            onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                onListItemClick(view, position, itemId);
            }
        });

        setupToolButtons();
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
        case REQC_YTSEARCH:
            boolean plChanged = data.getBooleanExtra(YTSearchActivity.KEY_PLCHANGED, false);
            if (plChanged)
                getAdapter().reloadCursor();
            break;
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