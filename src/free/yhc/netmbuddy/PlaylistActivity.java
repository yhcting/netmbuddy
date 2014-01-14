/******************************************************************************
 *    Copyright (C) 2012, 2013, 2014 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of NetMBuddy.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.	If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
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
import android.widget.TextView;
import free.yhc.netmbuddy.PlaylistAdapter.ItemButton;
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.SearchSuggestionProvider;
import free.yhc.netmbuddy.model.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.share.Share;
import free.yhc.netmbuddy.utils.ReportUtils;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class PlaylistActivity extends Activity implements
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(PlaylistActivity.class);

    private static final ColVideo[] sVideoProjectionToPlay
        = new ColVideo[] { ColVideo.VIDEOID,
                           ColVideo.TITLE,
                           ColVideo.AUTHOR,
                           ColVideo.VOLUME,
                           ColVideo.PLAYTIME};
    private static final int COLI_VID_YTVID     = 0;
    private static final int COLI_VID_TITLE     = 1;
    private static final int COLI_VID_AUTHOR    = 2;
    private static final int COLI_VID_VOLUME    = 3;
    private static final int COLI_VID_PLAYTIME  = 4;

    private final DB            mDb = DB.get();
    private final YTPlayer      mMp = YTPlayer.get();

    private final OnPlayerUpdateDBListener mOnPlayerUpdateDbListener
        = new OnPlayerUpdateDBListener();

    private ListView mListv;

    private class OnPlayerUpdateDBListener implements YTPlayer.OnDBUpdatedListener {
        @Override
        public void
        onDbUpdated(YTPlayer.DBUpdateType ty) {
            switch (ty) {
            case PLAYLIST:
                if (null != getAdapter())
                    getAdapter().reloadCursorAsync();
            }
            // others are ignored.
        }
    }

    private PlaylistAdapter
    getAdapter() {
        return (PlaylistAdapter)mListv.getAdapter();
    }

    /**
     * Closing cursor is this functions responsibility.
     * DO NOT close cursor by caller.
     * @param c
     */
    private void
    playMusics(Cursor c) {
        if (!Utils.isNetworkAvailable()) {
            UiUtils.showTextToast(this, Err.IO_NET.getMessage());
            c.close();
            return;
        }

        if (!c.moveToFirst()) {
            UiUtils.showTextToast(this, R.string.msg_empty_playlist);
            c.close();
            return;
        }

        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);
        mMp.startVideos(c,
                        COLI_VID_YTVID,
                        COLI_VID_TITLE,
                        COLI_VID_AUTHOR,
                        COLI_VID_VOLUME,
                        COLI_VID_PLAYTIME,
                        Utils.isPrefSuffle());

    }

    private void
    searchMusics(View anchor) {
        startSearch(null, false, null, false);
    }

    private void
    playAllMusics(View anchor) {
        playMusics(mDb.queryVideos(sVideoProjectionToPlay, null, false));
        UiUtils.showTextToast(this, R.string.msg_play_all_musics);
    }

    private void
    copyPlaylist(final long dstPlid, final long srcPlid) {
        // Below three variables are reserved for future use - user feedback.

        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            private int _mSCnt      = 0;    // inserted count
            private int _mDupCnt    = 0;    // duplicated cont
            private int _mFCnt      = 0;    // failure count

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                Cursor c = mDb.queryVideos(srcPlid,
                                           new ColVideo[] { ColVideo.ID },
                                           null,
                                           false);
                if (!c.moveToFirst()) {
                    c.close();
                    return Err.NO_ERR;
                }

                mDb.beginTransaction();
                try {
                    do {
                        switch (mDb.insertVideoToPlaylist(dstPlid, c.getLong(0))) {
                        case NO_ERR:        _mSCnt++;   break;
                        case DUPLICATED:    _mDupCnt++; break;
                        default:            _mFCnt++;
                        }
                    } while (c.moveToNext());
                    mDb.setTransactionSuccessful();
                } finally {
                    mDb.endTransaction();
                    c.close();
                }
                return Err.NO_ERR;
            }

            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                String msg  = Utils.getResText(R.string.done) + " : " + _mSCnt + "\n"
                              + Utils.getResText(R.string.duplication) + " : " + _mDupCnt + "\n"
                              + Utils.getResText(R.string.error) + " : " + _mFCnt;
                UiUtils.showTextToast(PlaylistActivity.this, msg);
                getAdapter().reloadCursorAsync();
            }
        };

        new DiagAsyncTask(this,
                worker,
                DiagAsyncTask.Style.SPIN,
                R.string.copying)
            .run();
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
            public void
            run() {
                // NOTE & IMPORTANT
                // Stop/Pause all operations that might use DB before changing and reloading DB.
                // At this moment, playing video is only operation accessing DB
                // (Updating playtime)
                YTPlayer.get().stopVideos();
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
        return Err.map(mDb.importDatabase(exDbf));
    }

    private Err
    mergeDbInBackground(File exDbf) {
        // Actually, in case of merging DB, we don't need to stop DB access.
        // But, just in case...
        stopDbAccess();

        return Err.map(mDb.mergeDatabase(exDbf));
    }

    private Err
    exportDbInBackground(File exDbf) {
        stopDbAccess();

        // Make directories.
        new File(exDbf.getAbsoluteFile().getParent()).mkdirs();
        return Err.map(mDb.exportDatabase(exDbf));
    }

    // ------------------------------------------------------------------------
    //
    //
    //
    // ------------------------------------------------------------------------
    private void
    onMenuMoreAppInfo(View anchor) {
        View v = UiUtils.inflateLayout(this, R.layout.info_dialog);
        ((ImageView)v.findViewById(R.id.image)).setImageResource(R.drawable.appinfo_pic);
        AlertDialog.Builder bldr = new AlertDialog.Builder(this);
        bldr.setView(v);
        AlertDialog aDiag = bldr.create();
        aDiag.show();
    }

    private void
    onMenuMoreLicense(View anchor) {
        View v = UiUtils.inflateLayout(this, R.layout.info_dialog);
        v.findViewById(R.id.image).setVisibility(View.GONE);
        TextView tv = ((TextView)v.findViewById(R.id.text));
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setText(R.string.license_desc);
        AlertDialog.Builder bldr = new AlertDialog.Builder(this);
        bldr.setView(v);
        AlertDialog aDiag = bldr.create();
        aDiag.show();
    }

    private void
    onMenuMoreClearSearchHistory(View anchor) {
        SearchSuggestionProvider.clearHistory();
        UiUtils.showTextToast(this, R.string.msg_search_history_cleared);
    }

    private void
    onMenuMoreDbImport(View anchor) {
        final File exDbf = new File(Policy.EXTERNAL_DBFILE);
        // Actual import!
        CharSequence title = getResources().getText(R.string.import_);
        CharSequence msg = getResources().getText(R.string.database) + " <= " + exDbf.getAbsolutePath();
        UiUtils.buildConfirmDialog(this, title, msg, new UiUtils.ConfirmAction() {
            @Override
            public void
            onOk(Dialog dialog) {
                // Check external DB file.
                if (!exDbf.canRead()) {
                    UiUtils.showTextToast(PlaylistActivity.this, R.string.msg_fail_access_exdb);
                    return;
                }

                DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
                    @Override
                    public void
                    onPostExecute(DiagAsyncTask task, Err result) {
                        if (Err.NO_ERR == result)
                            getAdapter().reloadCursorAsync();
                        else
                            UiUtils.showTextToast(PlaylistActivity.this, result.getMessage());
                    }

                    @Override
                    public Err
                    doBackgroundWork(DiagAsyncTask task) {
                        return importDbInBackground(exDbf);
                    }
                };
                new DiagAsyncTask(PlaylistActivity.this,
                                  worker,
                                  DiagAsyncTask.Style.SPIN,
                                  R.string.importing_db)
                    .run();
            }

            @Override
            public void
            onCancel(Dialog dialog) { }
        }).show();
    }

    private void
    onMenuMoreDbMerge(View anchor) {
        final File exDbf = new File(Policy.EXTERNAL_DBFILE);
        // Actual import!
        CharSequence title = getResources().getText(R.string.merge);
        CharSequence msg = getResources().getText(R.string.database) + " <= " + exDbf.getAbsolutePath();
        UiUtils.buildConfirmDialog(this, title, msg, new UiUtils.ConfirmAction() {
            @Override
            public void
            onOk(Dialog dialog) {
                // Check external DB file.
                if (!exDbf.canRead()) {
                    UiUtils.showTextToast(PlaylistActivity.this, R.string.msg_fail_access_exdb);
                    return;
                }

                DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
                    @Override
                    public void
                    onPostExecute(DiagAsyncTask task, Err result) {
                        if (Err.NO_ERR == result)
                            getAdapter().reloadCursorAsync();
                        else
                            UiUtils.showTextToast(PlaylistActivity.this, result.getMessage());
                    }

                    @Override
                    public Err
                    doBackgroundWork(DiagAsyncTask task) {
                        return mergeDbInBackground(exDbf);
                    }
                };
                new DiagAsyncTask(PlaylistActivity.this,
                                  worker,
                                  DiagAsyncTask.Style.SPIN,
                                  R.string.merging_db)
                    .run();
            }

            @Override
            public void
            onCancel(Dialog dialog) { }
        }).show();
    }

    private void
    onMenuMoreDbExport(View anchor) {
        final File exDbf = new File(Policy.EXTERNAL_DBFILE);
        // Actual import!
        CharSequence title = getResources().getText(R.string.export);
        CharSequence msg = getResources().getText(R.string.database) + " => " + exDbf.getAbsolutePath();
        UiUtils.buildConfirmDialog(this, title, msg, new UiUtils.ConfirmAction() {
            @Override
            public void
            onOk(Dialog dialog) {
                // Check external DB file.
                if (exDbf.exists() && !exDbf.canWrite()) {
                    UiUtils.showTextToast(PlaylistActivity.this, R.string.msg_fail_access_exdb);
                    return;
                }

                DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
                    @Override
                    public void
                    onPostExecute(DiagAsyncTask task, Err result) {
                        if (Err.NO_ERR != result) {
                            UiUtils.showTextToast(PlaylistActivity.this, result.getMessage());
                        }
                    }

                    @Override
                    public Err
                    doBackgroundWork(DiagAsyncTask task) {
                        return exportDbInBackground(exDbf);
                    }
                };
                new DiagAsyncTask(PlaylistActivity.this,
                                  worker,
                                  DiagAsyncTask.Style.SPIN,
                                  R.string.exporting_db)
                    .run();
            }

            @Override
            public void
            onCancel(Dialog dialog) { }
        }).show();
    }

    private void
    onMenuMoreDB(final View anchor) {
        final int[] menus = {
                R.string.export,
                R.string.import_,
                R.string.merge };

        UiUtils.OnMenuSelected action = new UiUtils.OnMenuSelected() {
            @Override
            public void
            onSelected(int pos, int menuTitle) {
                switch (menuTitle) {
                case R.string.export:
                    onMenuMoreDbExport(anchor);
                    break;

                case R.string.import_:
                    onMenuMoreDbImport(anchor);
                    break;

                case R.string.merge:
                    onMenuMoreDbMerge(anchor);
                    break;

                default:
                    eAssert(false);
                }
            }
        };

        UiUtils.buildPopupMenuDialog(this,
                                     action,
                                     R.string.database,
                                     menus)
               .show();
    }

    private void
    onMenuMoreYtSearchAuthor(final View anchor) {
        startActivity(new Intent(this, YTVideoSearchAuthorActivity.class));
    }

    private void
    onMenuMoreYtSearchPlaylist(final View anchor) {
        Intent i = new Intent(PlaylistActivity.this, YTPlaylistSearchActivity.class);
        startActivity(i);
    }

    private void
    onMenuMoreYtSearch(final View anchor) {
        final int[] menus = {
                R.string.videos_with_author,
                R.string.user_playlist };

        UiUtils.OnMenuSelected action = new UiUtils.OnMenuSelected() {
            @Override
            public void
            onSelected(int pos, int menuTitle) {
                switch (menuTitle) {
                case R.string.videos_with_author:
                    onMenuMoreYtSearchAuthor(anchor);
                    break;

                case R.string.user_playlist:
                    onMenuMoreYtSearchPlaylist(anchor);
                    break;
                default:
                    eAssert(false);
                }
            }
        };

        UiUtils.buildPopupMenuDialog(this,
                                     action,
                                     R.string.ytsearch,
                                     menus)
               .show();
    }

    private void
    onMenuMoreSendOpinion(View anchor) {
        if (!Utils.isNetworkAvailable()) {
            UiUtils.showTextToast(this, R.string.err_network_unavailable);
            return;
        }
        ReportUtils.sendFeedback(this);
    }

    private void
    onMenuMoreAutoStop(View anchor) {
        final int[] menus = {
                R.string.off,
                R.string.time10m,
                R.string.time20m,
                R.string.time30m,
                R.string.time1h,
                R.string.time2h };

        UiUtils.OnMenuSelected action = new UiUtils.OnMenuSelected() {
            @Override
            public void
            onSelected(int pos, int menuTitle) {
                long timems = 0;
                switch (menuTitle) {
                case R.string.off:      timems = 0;                     break;
                case R.string.time10m:  timems = 10 * 60 * 1000;        break;
                case R.string.time20m:  timems = 20 * 60 * 1000;        break;
                case R.string.time30m:  timems = 30 * 60 * 1000;        break;
                case R.string.time1h:   timems = 1 * 60 * 60 * 1000;    break;
                case R.string.time2h:   timems = 2 * 60 * 60 * 1000;    break;
                default:
                    eAssert(false);
                }

                if (0 <= timems)
                    mMp.setAutoStop(timems);
                else
                    mMp.unsetAutoStop();
            }
        };

        UiUtils.buildPopupMenuDialog(this,
                                     action,
                                     R.string.autostop,
                                     menus)
               .show();
    }

    private void
    onMenuMore(final View anchor) {
        final int[] menus = {
                R.string.app_info,
                R.string.license,
                R.string.clear_search_history,
                R.string.dbmore,
                R.string.ytsearchmore,
                R.string.feedback,
                R.string.autostop };

        UiUtils.OnMenuSelected action = new UiUtils.OnMenuSelected() {
            @Override
            public void
            onSelected(int pos, int menuTitle) {
                switch (menuTitle) {
                case R.string.autostop:
                    if (mMp.hasActiveVideo())
                        onMenuMoreAutoStop(anchor);
                    else
                        UiUtils.showTextToast(PlaylistActivity.this, R.string.msg_autostop_not_allowed);
                    break;

                case R.string.feedback:
                    onMenuMoreSendOpinion(anchor);
                    break;

                case R.string.dbmore:
                    onMenuMoreDB(anchor);
                    break;

                case R.string.ytsearchmore:
                    onMenuMoreYtSearch(anchor);
                    break;

                case R.string.clear_search_history:
                    onMenuMoreClearSearchHistory(anchor);
                    break;

                case R.string.app_info:
                    onMenuMoreAppInfo(anchor);
                    break;

                case R.string.license:
                    onMenuMoreLicense(anchor);
                    break;

                default:
                    eAssert(false);
                }
            }
        };

        UiUtils.buildPopupMenuDialog(this,
                                     action,
                                     -1,
                                     menus)
               .show();
    }

    private void
    setupToolButtons() {
        ((ImageView)findViewById(R.id.playall)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                playAllMusics(v);
            }
        });

        ((ImageView)findViewById(R.id.recently_played)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                Intent i = new Intent(PlaylistActivity.this, MusicsActivity.class);
                i.putExtra(MusicsActivity.MAP_KEY_PLAYLIST_ID, UiUtils.PLID_RECENT_PLAYED);
                startActivity(i);
            }
        });

        ((ImageView)findViewById(R.id.dbsearch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                searchMusics(v);
            }
        });

        ((ImageView)findViewById(R.id.ytsearch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                startActivity(new Intent(PlaylistActivity.this, YTVideoSearchKeywordActivity.class));
            }
        });

        ((ImageView)findViewById(R.id.preferences)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                Intent i = new Intent(PlaylistActivity.this, YTMPPreferenceActivity.class);
                startActivity(i);
            }
        });

        ((ImageView)findViewById(R.id.more)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(final View v) {
                onMenuMore(v);
            }
        });
    }

    private void
    onContextMenuRename(final AdapterContextMenuInfo info) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void
            prepare(Dialog dialog, EditText edit) { }
            @Override
            public void
            onOk(Dialog dialog, EditText edit) {
                String word = edit.getText().toString();
                mDb.updatePlaylist(info.id, ColPlaylist.TITLE, word);
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
    onContextMenuDeleteDo(final long plid) {
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                getAdapter().reloadCursor();
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                mDb.deletePlaylist(plid);
                return Err.NO_ERR;
            }
        };
        new DiagAsyncTask(this,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          R.string.deleting)
            .run();
    }

    private void
    onContextMenuDelete(final AdapterContextMenuInfo info) {
        UiUtils.ConfirmAction action = new UiUtils.ConfirmAction() {
            @Override
            public void
            onOk(Dialog dialog) {
                onContextMenuDeleteDo(info.id);
            }

            @Override
            public void
            onCancel(Dialog dialog) { }
        };
        UiUtils.buildConfirmDialog(this,
                                   R.string.delete,
                                   R.string.msg_delete_playlist,
                                   action)
               .show();
    }



    private void
    onContextMenuCopyTo(final AdapterContextMenuInfo info) {
        UiUtils.OnPlaylistSelected action = new UiUtils.OnPlaylistSelected() {
            @Override
            public void
            onPlaylist(final long plid, final Object user) {
                copyPlaylist(plid, info.id);
            }

            @Override
            public void
            onUserMenu(int pos, Object user) { }
        };

        UiUtils.buildSelectPlaylistDialog(DB.get(),
                                          this,
                                          R.string.copy_to,
                                          null,
                                          action,
                                          info.id,
                                          null)
               .show();
    }

    private void
    onContextMenuShare(final AdapterContextMenuInfo info) {
        if (0 >= (Long)mDb.getPlaylistInfo(info.id, ColPlaylist.SIZE)) {
            UiUtils.showTextToast(this, R.string.msg_empty_playlist);
            return;
        }

        final File fTmp;
        try {
            fTmp = File.createTempFile(Utils.getResText(R.string.share_pl_attachment),
                                       "." + Policy.SHARE_FILE_EXTENTION,
                                       new File(Policy.APPDATA_TMPDIR));
        } catch (IOException e) {
            UiUtils.showTextToast(this, R.string.err_io_file);
            return;
        }

        final Share.ExporterI exporter = Share.buildPlayerlistExporter(fTmp, info.id);

        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                if (Err.NO_ERR != result) {
                    UiUtils.showTextToast(PlaylistActivity.this, result.getMessage());
                    return;
                }

                String plTitle = (String)DB.get().getPlaylistInfo(info.id, ColPlaylist.TITLE);
                Utils.sendMail(PlaylistActivity.this,
                               null,
                               Utils.getResText(R.string.share_pl_email_subject) + ":" + plTitle,
                               Utils.getResText(R.string.share_pl_email_text),
                               fTmp);
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                Share.Err err = exporter.execute();
                if (Share.Err.NO_ERR != err) {
                    fTmp.delete();
                    return Err.map(err);
                }
                return Err.NO_ERR;
            }
        };

        new DiagAsyncTask(this,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          R.string.preparing)
            .run();
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        playMusics(mDb.queryVideos(itemId, sVideoProjectionToPlay, null, false));
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.rename:
            onContextMenuRename(info);
            return true;

        case R.id.delete:
            onContextMenuDelete(info);
            return true;

        case R.id.copy_to:
            onContextMenuCopyTo(info);
            return true;

        case R.id.share:
            onContextMenuShare(info);
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
        UnexpectedExceptionHandler.get().registerModule(this);

        ReportUtils.sendErrReport(this);
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
                i.putExtra(MusicsActivity.MAP_KEY_PLAYLIST_ID, adapter.getItemId(pos));
                i.putExtra(MusicsActivity.MAP_KEY_TITLE, adapter.getItemTitle(pos));
                i.putExtra(MusicsActivity.MAP_KEY_THUMBNAIL, adapter.getItemThumbnail(pos));
                startActivity(i);
            }
        });
        mListv.setAdapter(adapter);
        mListv.setEmptyView(findViewById(R.id.empty_list));
        adapter.reloadCursorAsync();

    }

    @Override
    protected void
    onNewIntent(Intent intent) {
        setIntent(intent);
        if (!Intent.ACTION_SEARCH.equals(intent.getAction()))
            return; // ignore unexpected intent

        String query = intent.getStringExtra(SearchManager.QUERY);
        SearchSuggestionProvider.saveRecentQuery(query);
        Intent i = new Intent(PlaylistActivity.this, MusicsActivity.class);
        i.putExtra(MusicsActivity.MAP_KEY_PLAYLIST_ID, UiUtils.PLID_SEARCHED);
        i.putExtra(MusicsActivity.MAP_KEY_KEYWORD, query);
        startActivity(i);
    }

    @Override
    protected void
    onResume() {
        super.onResume();
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        mMp.setController(this,
                          playerv,
                          (ViewGroup)findViewById(R.id.list_drawer),
                          null,
                          mMp.getVideoToolButton());
        mMp.addOnDbUpdatedListener(this, mOnPlayerUpdateDbListener);
        if (mMp.hasActiveVideo())
            playerv.setVisibility(View.VISIBLE);
        else
            playerv.setVisibility(View.GONE);

        if (mDb.isRegisteredToPlaylistTableWatcher(this)) {
            if (mDb.isPlaylistTableUpdated(this))
                getAdapter().reloadCursorAsync();
            mDb.unregisterToPlaylistTableWatcher(this);
        }
    }

    @Override
    protected void
    onPause() {
        mMp.removeOnDbUpdatedListener(this);
        mMp.unsetController(this);
        mDb.registerToPlaylistTableWatcher(this);
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
        mDb.unregisterToPlaylistTableWatcher(this);
        UnexpectedExceptionHandler.get().unregisterModule(this);
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
        if (Utils.isPrefStopOnBack())
            // stop playing if exit via back-key.
            mMp.stopVideos();
        super.onBackPressed();
    }
}
