package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;

import java.io.File;

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
import free.yhc.youtube.musicplayer.PlaylistAdapter.ItemButton;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.DB.ColVideo;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.Policy;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class PlaylistActivity extends Activity {
    private final DB            mDb = DB.get();
    private final YTJSPlayer    mMp = YTJSPlayer.get();

    private ListView mListv;

    private PlaylistAdapter
    getAdapter() {
        return (PlaylistAdapter)mListv.getAdapter();
    }

    private void
    sendMail(CharSequence diagTitle, CharSequence subject, CharSequence text) {
        if (!Utils.isNetworkAvailable())
            return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { Policy.Constants.REPORT_RECEIVER });
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
        mMp.startVideos(c, 0, 1, 2, 3, Utils.isPrefSuffle());
    }

    private void
    searchMusics(View anchor) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void prepare(Dialog dialog, EditText edit) { }
            @Override
            public void onOk(Dialog dialog, EditText edit) {
                Intent i = new Intent(PlaylistActivity.this, MusicsActivity.class);
                i.putExtra("plid", MusicsActivity.PLID_SEARCHED);
                i.putExtra("word", edit.getText().toString());
                startActivity(i);
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
                                                    ColVideo.VOLUME,
                                                    ColVideo.PLAYTIME},
                                   null, false));
    }

    // ------------------------------------------------------------------------
    //
    // Importing/Exporting DB
    // Cautious! Extremely fatal/critical operation.
    //
    // ------------------------------------------------------------------------

    // CAUTIOUS!
    // This function MUST COVER ALL USE-CASE regarding DB ACCESS.
    private void
    stopDbAccess() {
        final Object uiWait = new Object();
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void run() {
                // NOTE & IMPORTANT
                // Stop/Pause all operations that might use DB before changing and reloading DB.
                // At this moment, playing video is only operation accessing DB
                // (Updating playtime)
                YTJSPlayer.get().stopVideos();
                synchronized (uiWait) {
                    uiWait.notifyAll();
                }
            }
        });

        synchronized (uiWait) {
            try {
                uiWait.wait();
            } catch (InterruptedException e) { }
        }

        // Wait for sometime that existing DB operation is completed.
        // This is not safe enough.
        // But, waiting 2 seconds is fair enough.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) { }
    }

    private Err
    importDbInBackground(File exDbf) {
        stopDbAccess();

        // Let's do real-import.
        return mDb.importDatabase(exDbf);
    }

    private Err
    mergeDbInBackground(File exDbf) {
        // Actually, in case of merging DB, we don't need to stop DB access.
        // But, just in case...
        stopDbAccess();

        return mDb.mergeDatabase(exDbf);
    }

    private Err
    exportDbInBackground(File exDbf) {
        stopDbAccess();

        // Make directories.
        new File(exDbf.getAbsoluteFile().getParent()).mkdirs();
        return mDb.exportDatabase(exDbf);
    }

    // ------------------------------------------------------------------------
    //
    //
    //
    // ------------------------------------------------------------------------
    private void
    onMenuMoreAppInfo(View anchor) {
        AlertDialog.Builder bldr = new AlertDialog.Builder(this);
        bldr.setView(UiUtils.inflateLayout(this, R.layout.appinfo));
        AlertDialog aDiag = bldr.create();
        aDiag.show();
    }

    private void
    onMenuMoreImportDb(View anchor) {
        final File exDbf = new File(Policy.Constants.EXTERNAL_DBFILE);
        // Actual import!
        CharSequence title = getResources().getText(R.string.importdb);
        CharSequence msg = getResources().getText(R.string.database) + " <= " + exDbf.getAbsolutePath();
        UiUtils.buildConfirmDialog(this, title, msg, new UiUtils.ConfirmAction() {
            @Override
            public void onOk(Dialog dialog) {
                // Check external DB file.
                if (!exDbf.canRead()) {
                    UiUtils.showTextToast(PlaylistActivity.this, R.string.msg_fail_access_exdb);
                    return;
                }

                SpinAsyncTask.Worker worker = new SpinAsyncTask.Worker() {
                    @Override
                    public void
                    onPostExecute(SpinAsyncTask task, Err result) {
                        if (Err.NO_ERR == result)
                            getAdapter().reloadCursorAsync();
                        else
                            UiUtils.showTextToast(PlaylistActivity.this, result.getMessage());
                    }

                    @Override
                    public void onCancel(SpinAsyncTask task) { }

                    @Override
                    public Err
                    doBackgroundWork(SpinAsyncTask task, Object... objs) {
                        return importDbInBackground(exDbf);
                    }
                };
                new SpinAsyncTask(PlaylistActivity.this, worker, R.string.importing_db, false).execute(exDbf);
            }
        }).show();
    }

    private void
    onMenuMoreMergeDb(View anchor) {
        final File exDbf = new File(Policy.Constants.EXTERNAL_DBFILE);
        // Actual import!
        CharSequence title = getResources().getText(R.string.mergedb);
        CharSequence msg = getResources().getText(R.string.database) + " <= " + exDbf.getAbsolutePath();
        UiUtils.buildConfirmDialog(this, title, msg, new UiUtils.ConfirmAction() {
            @Override
            public void onOk(Dialog dialog) {
                // Check external DB file.
                if (!exDbf.canRead()) {
                    UiUtils.showTextToast(PlaylistActivity.this, R.string.msg_fail_access_exdb);
                    return;
                }

                SpinAsyncTask.Worker worker = new SpinAsyncTask.Worker() {
                    @Override
                    public void
                    onPostExecute(SpinAsyncTask task, Err result) {
                        if (Err.NO_ERR == result)
                            getAdapter().reloadCursorAsync();
                        else
                            UiUtils.showTextToast(PlaylistActivity.this, result.getMessage());
                    }

                    @Override
                    public void onCancel(SpinAsyncTask task) { }

                    @Override
                    public Err
                    doBackgroundWork(SpinAsyncTask task, Object... objs) {
                        return mergeDbInBackground(exDbf);
                    }
                };
                new SpinAsyncTask(PlaylistActivity.this, worker, R.string.merging_db, false).execute(exDbf);
            }
        }).show();
    }

    private void
    onMenuMoreExportDb(View anchor) {
        final File exDbf = new File(Policy.Constants.EXTERNAL_DBFILE);
        // Actual import!
        CharSequence title = getResources().getText(R.string.exportdb);
        CharSequence msg = getResources().getText(R.string.database) + " => " + exDbf.getAbsolutePath();
        UiUtils.buildConfirmDialog(this, title, msg, new UiUtils.ConfirmAction() {
            @Override
            public void onOk(Dialog dialog) {
                // Check external DB file.
                if (exDbf.exists() && !exDbf.canWrite()) {
                    UiUtils.showTextToast(PlaylistActivity.this, R.string.msg_fail_access_exdb);
                    return;
                }

                SpinAsyncTask.Worker worker = new SpinAsyncTask.Worker() {
                    @Override
                    public void
                    onPostExecute(SpinAsyncTask task, Err result) {
                        if (Err.NO_ERR != result) {
                            UiUtils.showTextToast(PlaylistActivity.this, result.getMessage());
                        }
                    }

                    @Override
                    public void onCancel(SpinAsyncTask task) { }

                    @Override
                    public Err
                    doBackgroundWork(SpinAsyncTask task, Object... objs) {
                        return exportDbInBackground(exDbf);
                    }
                };
                new SpinAsyncTask(PlaylistActivity.this, worker, R.string.exporting_db, false).execute(exDbf);
            }
        }).show();
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

        ((ImageView)findViewById(R.id.recently_played)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(PlaylistActivity.this, MusicsActivity.class);
                i.putExtra("plid", MusicsActivity.PLID_RECENT_PLAYED);
                startActivity(i);
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
                Intent i = new Intent(PlaylistActivity.this, YTSearchActivity.class);
                startActivity(i);
            }
        });

        ((ImageView)findViewById(R.id.preferences)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(PlaylistActivity.this, YTMPPreferenceActivity.class);
                startActivity(i);
            }
        });

        ((ImageView)findViewById(R.id.more)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu popup = new PopupMenu(PlaylistActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.playlist_more_popup, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.feedback:
                            onMenuMoreSendOpinion(v);
                            break;

                        case R.id.exportdb:
                            onMenuMoreExportDb(v);
                            break;

                        case R.id.importdb:
                            onMenuMoreImportDb(v);
                            break;

                        case R.id.mergedb:
                            onMenuMoreMergeDb(v);
                            break;

                        case R.id.app_info:
                            onMenuMoreAppInfo(v);
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
                mDb.updatePlaylist(info.id, DB.ColPlaylist.TITLE, word);
                getAdapter().reloadCursorAsync();
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              R.string.enter_new_name,
                                                              getAdapter().getItemTitle(info.position),
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
            public void onCancel(SpinAsyncTask task) { }
            @Override
            public Err doBackgroundWork(SpinAsyncTask task, Object... objs) {
                mDb.deletePlaylist((Long)objs[0]);
                return Err.NO_ERR;
            }
        };
        new SpinAsyncTask(this, worker, R.string.loading, false).execute(info.id);
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        playMusics(mDb.queryVideos(itemId,
                                   new ColVideo[] { ColVideo.VIDEOID,
                                                    ColVideo.TITLE,
                                                    ColVideo.VOLUME,
                                                    ColVideo.PLAYTIME},
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
        eAssert(false);
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

        PlaylistAdapter adapter = new PlaylistAdapter(this, new PlaylistAdapter.OnItemButtonClickListener() {
            @Override
            public void onClick(int pos, ItemButton button) {
                //eAssert(PlaylistAdapter.ItemButton.LIST == button);
                Intent i = new Intent(PlaylistActivity.this, MusicsActivity.class);
                PlaylistAdapter adapter = getAdapter();
                i.putExtra("plid", adapter.getItemId(pos));
                i.putExtra("title", adapter.getItemTitle(pos));
                i.putExtra("thumbnail", adapter.getItemThumbnail(pos));
                startActivity(i);
            }
        });
        mListv.setAdapter(adapter);
        mListv.setEmptyView(findViewById(R.id.empty_list));
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
        } else
            playerv.setVisibility(View.GONE);

        if (mDb.isRegisteredToPlaylistTableWatcher(this)
            && mDb.isPlaylistTableUpdated(this))
            getAdapter().reloadCursorAsync();
    }

    @Override
    protected void
    onPause() {
        super.onPause();
        mDb.registerToPlaylistTableWatcher(this);
    }

    @Override
    protected void
    onStop() {
        super.onStop();
    }

    @Override
    protected void
    onDestroy() {
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
