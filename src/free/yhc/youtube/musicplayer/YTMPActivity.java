package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import free.yhc.youtube.musicplayer.PlayListAdapter.ItemButton;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.DB.ColMusic;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class YTMPActivity extends Activity {
    public static final String KEY_PLCHANGED    = "playListChanged";

    private static final int REQC_YTSEARCH  = 0;
    private static final int REQC_MUSICS    = 1;

    private final DB          mDb = DB.get();
    private final MusicPlayer mMp = MusicPlayer.get();

    private ListView mListv;

    private PlayListAdapter
    getAdapter() {
        return (PlayListAdapter)mListv.getAdapter();
    }

    /**
     * Closing cursor is this functions responsibility.
     * DO NOT close cursor by caller.
     * @param c
     */
    private void
    playMusics(Cursor c) {
        if (!c.moveToFirst()) {
            UiUtils.showTextToast(this, R.string.msg_empty_playlist);
            c.close();
            return;
        }
        View playerv = findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);
        mMp.setController(this, playerv);
        mMp.startMusicsAsync(c, 0, 1, Utils.isPrefSuffle());
    }

    private void
    searchMusics(View anchor) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }
            @Override
            public void onOk(Dialog dialog, EditText edit) {
                Intent i = new Intent(YTMPActivity.this, MusicsActivity.class);
                i.putExtra("plid", MusicsActivity.PLID_SEARCHED);
                i.putExtra("word", edit.getText().toString());
                startActivityForResult(i, REQC_MUSICS);
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              R.string.enter_search_word,
                                                              action);
        diag.show();
    }

    private void
    playAllMusics(View anchor) {
        playMusics(mDb.queryMusics(new ColMusic[] { ColMusic.URL, ColMusic.TITLE },
                                   null, false));
    }

    private void
    setupToolButtons() {
        ((ImageView)findViewById(R.id.playall)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAllMusics(v);
            }
        });

        ((ImageView)findViewById(R.id.recent_played)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(YTMPActivity.this, MusicsActivity.class);
                i.putExtra("plid", MusicsActivity.PLID_RECENT_PLAYED);
                startActivityForResult(i, REQC_MUSICS);
            }
        });

        ((ImageView)findViewById(R.id.dbsearch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchMusics(v);
            }
        });

        ((ImageView)findViewById(R.id.ytsearch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(YTMPActivity.this, YTSearchActivity.class);
                startActivityForResult(i, REQC_YTSEARCH);
            }
        });

        ((ImageView)findViewById(R.id.preferences)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(YTMPActivity.this, YTMPPreferenceActivity.class);
                startActivity(i);
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
        playMusics(mDb.queryMusics(itemId,
                                   new ColMusic[] { ColMusic.URL, ColMusic.TITLE },
                                   null, false));
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
        registerForContextMenu(mListv);
        mListv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void
            onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                onListItemClick(view, position, itemId);
            }
        });
        setupToolButtons();

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
        adapter.reloadCursor();
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

        // Check common result.
        boolean plChanged = data.getBooleanExtra(KEY_PLCHANGED, false);
        if (plChanged)
            getAdapter().reloadCursor();

        switch (requestCode) {
        case REQC_YTSEARCH:
            break;
        case REQC_MUSICS:
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