package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import free.yhc.youtube.musicplayer.PlayListAdapter.ItemButton;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.DB.ColVideo;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class PlayListActivity extends Activity {
    public static final String KEY_PLCHANGED    = "playListChanged";

    private static final String REPORT_RECEIVER = "yhcting77@gmail.com";
    private static final int REQC_YTSEARCH  = 0;
    private static final int REQC_MUSICS    = 1;

    private final DB            mDb = DB.get();
    private final YTJSPlayer    mMp = YTJSPlayer.get();

    private ListView mListv;

    private PlayListAdapter
    getAdapter() {
        return (PlayListAdapter)mListv.getAdapter();
    }

    private void
    sendMail(CharSequence diagTitle, CharSequence subject, CharSequence text) {
        if (!Utils.isNetworkAvailable())
            return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { REPORT_RECEIVER });
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.setType("message/rfc822");
        intent = Intent.createChooser(intent, diagTitle);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            UiUtils.showTextToast(this, R.string.msg_fail_find_app);
        }
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
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);
        mMp.setController(this, playerv);
        mMp.startVideos(c, 0, 1, 2, Utils.isPrefSuffle());
    }

    private void
    searchMusics(View anchor) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }
            @Override
            public void onOk(Dialog dialog, EditText edit) {
                Intent i = new Intent(PlayListActivity.this, MusicsActivity.class);
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
        playMusics(mDb.queryVideos(new ColVideo[] { ColVideo.VIDEOID,
                                                    ColVideo.TITLE,
                                                    ColVideo.VOLUME },
                                   null, false));
    }

    private void
    onMenuMoreDbManagement(View anchor) {
        UiUtils.showTextToast(this, R.string.msg_not_implemented);
    }

    private void
    onMenuMoreSendOpinion(View anchor) {
        if (!Utils.isNetworkAvailable()) {
            UiUtils.showTextToast(this, R.string.msg_network_unavailable);
            return;
        }
        sendMail(getResources().getText(R.string.choose_app),
                 getResources().getText(R.string.report_opinion_title),
                 "");
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
                Intent i = new Intent(PlayListActivity.this, MusicsActivity.class);
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
                Intent i = new Intent(PlayListActivity.this, YTSearchActivity.class);
                startActivityForResult(i, REQC_YTSEARCH);
            }
        });

        ((ImageView)findViewById(R.id.preferences)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(PlayListActivity.this, YTMPPreferenceActivity.class);
                startActivity(i);
            }
        });

        ((ImageView)findViewById(R.id.more)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu popup = new PopupMenu(PlayListActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.playlist_more_popup, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.db_management:
                            onMenuMoreDbManagement(v);
                            break;

                        case R.id.send_opinion:
                            onMenuMoreSendOpinion(v);
                            break;

                        default:
                            eAssert(false);
                        }
                        return true;
                    }
                });
                popup.show();

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
                getAdapter().reloadCursorAsync();
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              R.string.enter_new_name,
                                                              action);
        diag.show();
    }

    private void
    onContextDelete(AdapterContextMenuInfo info) {
        SpinAsyncTask.Worker worker = new SpinAsyncTask.Worker() {
            @Override
            public void onPostExecute(SpinAsyncTask task, Err result) {
                getAdapter().reloadCursor();
            }
            @Override
            public void onCancel(SpinAsyncTask task) {
                // TODO Auto-generated method stub

            }
            @Override
            public Err doBackgroundWork(SpinAsyncTask task, Object... objs) {
                mDb.deletePlayList((Long)objs[0]);
                return Err.NO_ERR;
            }
        };
        new SpinAsyncTask(this, worker, R.string.loading, false).execute(info.id);
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        playMusics(mDb.queryVideos(itemId,
                                   new ColVideo[] { ColVideo.VIDEOID, ColVideo.TITLE },
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

        setContentView(R.layout.playlist);
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
                //eAssert(PlayListAdapter.ItemButton.LIST == button);
                Intent i = new Intent(PlayListActivity.this, MusicsActivity.class);
                PlayListAdapter adapter = getAdapter();
                i.putExtra("plid", adapter.getItemId(pos));
                i.putExtra("title", adapter.getItemTitle(pos));
                i.putExtra("thumbnail", adapter.getItemThumbnail(pos));
                startActivityForResult(i, REQC_MUSICS);
            }
        });
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

        // Check common result.
        boolean plChanged = data.getBooleanExtra(KEY_PLCHANGED, false);
        if (plChanged)
            getAdapter().reloadCursorAsync();

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
