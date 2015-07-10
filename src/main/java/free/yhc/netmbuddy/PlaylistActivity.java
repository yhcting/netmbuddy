/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
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
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.core.SearchSuggestionProvider;
import free.yhc.netmbuddy.core.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.share.Share;
import free.yhc.netmbuddy.utils.ReportUtils;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class PlaylistActivity extends Activity implements
UnexpectedExceptionHandler.Evidence {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(PlaylistActivity.class);

    private final DB mDb = DB.get();
    private final YTPlayer mMp = YTPlayer.get();

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
        mMp.startVideos(c, Utils.isPrefSuffle());

    }

    private void
    searchMusics(@SuppressWarnings("unused") View anchor) {
        startSearch(null, false, null, false);
    }

    private void
    playAllMusics(@SuppressWarnings("unused") View anchor) {
        playMusics(mDb.queryVideos(YTPlayer.sVideoProjectionToPlay, null, false));
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
                String msg  = Utils.getResString(R.string.done) + " : " + _mSCnt + "\n"
                              + Utils.getResString(R.string.duplication) + " : " + _mDupCnt + "\n"
                              + Utils.getResString(R.string.error) + " : " + _mFCnt;
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
        eAssert(!Utils.isUiThread());
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

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (uiWait) {
            try {
                uiWait.wait();
            } catch (InterruptedException ignored) { }
        }

        // Wait for sometime that existing DB operation is completed.
        // This is not safe enough.
        // But, waiting 2 seconds is fair enough.
        //noinspection EmptyCatchBlock
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) { }
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
        //noinspection ResultOfMethodCallIgnored
        new File(exDbf.getAbsoluteFile().getParent()).mkdirs();
        return Err.map(mDb.exportDatabase(exDbf));
    }

    // ------------------------------------------------------------------------
    //
    //
    //
    // ------------------------------------------------------------------------
    private void
    onMenuMoreAppInfo(@SuppressWarnings("unused") View anchor) {
        View v = UiUtils.inflateLayout(this, R.layout.info_dialog);
        ((ImageView)v.findViewById(R.id.image)).setImageResource(R.drawable.appinfo_pic);
        AlertDialog.Builder bldr = new AlertDialog.Builder(this);
        bldr.setView(v);
        AlertDialog aDiag = bldr.create();
        aDiag.show();
    }

    private void
    onMenuMoreLicense(@SuppressWarnings("unused") View anchor) {
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
    onMenuMoreClearSearchHistory(@SuppressWarnings("unused") View anchor) {
        SearchSuggestionProvider.clearHistory();
        UiUtils.showTextToast(this, R.string.msg_search_history_cleared);
    }

    private void
    onMenuMoreDbImport(@SuppressWarnings("unused") View anchor) {
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
    onMenuMoreDbMerge(@SuppressWarnings("unused") View anchor) {
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
    onMenuMoreDbExport(@SuppressWarnings("unused") View anchor) {
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
            onCancel(Dialog dialog) {
            }
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
    onMenuMoreYtSearchChannel(@SuppressWarnings("unused") final View anchor) {
        startActivity(new Intent(this, YTVideoSearchChannelActivity.class));
    }

    private void
    onMenuMoreYtSearch(final View anchor) {
        final int[] menus = {
                R.string.channel_videos };

        UiUtils.OnMenuSelected action = new UiUtils.OnMenuSelected() {
            @Override
            public void
            onSelected(int pos, int menuTitle) {
                switch (menuTitle) {
                case R.string.channel_videos:
                    onMenuMoreYtSearchChannel(anchor);
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
    onMenuMoreSendOpinion(@SuppressWarnings("unused") View anchor) {
        if (!Utils.isNetworkAvailable()) {
            UiUtils.showTextToast(this, R.string.err_network_unavailable);
            return;
        }
        ReportUtils.sendFeedback(this);
    }

    private void
    onMenuMoreAutoStop(@SuppressWarnings("unused") View anchor) {
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
                case R.string.time1h:   timems = 60 * 60 * 1000;        break;
                case R.string.time2h:   timems = 2 * 60 * 60 * 1000;    break;
                default:
                    eAssert(false);
                }

                if (0 < timems)
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
        (findViewById(R.id.playall)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                playAllMusics(v);
            }
        });

        (findViewById(R.id.recently_played)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                Intent i = new Intent(PlaylistActivity.this, MusicsActivity.class);
                i.putExtra(MusicsActivity.MAP_KEY_PLAYLIST_ID, UiUtils.PLID_RECENT_PLAYED);
                startActivity(i);
            }
        });

        (findViewById(R.id.dbsearch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                searchMusics(v);
            }
        });

        (findViewById(R.id.ytsearch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                startActivity(new Intent(PlaylistActivity.this, YTVideoSearchKeywordActivity.class));
            }
        });

        (findViewById(R.id.preferences)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                Intent i = new Intent(PlaylistActivity.this, YTMPPreferenceActivity.class);
                startActivity(i);
            }
        });

        (findViewById(R.id.more)).setOnClickListener(new View.OnClickListener() {
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
            fTmp = File.createTempFile(Utils.getResString(R.string.share_pl_attachment),
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
                               Utils.getResString(R.string.share_pl_email_subject) + ":" + plTitle,
                               Utils.getResString(R.string.share_pl_email_text),
                               fTmp);
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                Share.Err err = exporter.execute();
                if (Share.Err.NO_ERR != err) {
                    //noinspection ResultOfMethodCallIgnored
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
    onListItemClick(@SuppressWarnings("unused") View view,
                    @SuppressWarnings("unused") int position,
                    long itemId) {
        playMusics(mDb.queryVideos(itemId, YTPlayer.sVideoProjectionToPlay, null, false));
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
