/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of YTMPlayer.
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
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.model.Utils.eAssert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import free.yhc.netmbuddy.model.DB;
import free.yhc.netmbuddy.model.Err;
import free.yhc.netmbuddy.model.UiUtils;
import free.yhc.netmbuddy.model.Utils;
import free.yhc.netmbuddy.model.YTPlayer;

public class MusicsActivity extends Activity {
    public static final long PLID_INVALID       = DB.INVALID_PLAYLIST_ID;
    public static final long PLID_RECENT_PLAYED = PLID_INVALID - 1;
    public static final long PLID_SEARCHED      = PLID_INVALID - 2;

    private final DB            mDb = DB.get();
    private final YTPlayer      mMp = YTPlayer.get();

    private final MusicsAdapter.CheckStateListener mCheckListener
        = new MusicsAdapter.CheckStateListener() {
            @Override
            public void
            onStateChanged(int nrChecked, int pos, boolean checked) {
                // do nothing.
            }
        };


    private long        mPlid   = PLID_INVALID;
    private ListView    mListv  = null;


    private static boolean
    isUserPlaylist(long plid) {
        return plid >= 0;
    }

    private MusicsAdapter
    getAdapter() {
        return (MusicsAdapter)mListv.getAdapter();
    }

    private void
    startVideos(YTPlayer.Video[] vs) {
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        playerv.setVisibility(View.VISIBLE);
        mMp.startVideos(vs);
    }

    private void
    doAddTo(final long plid, final long[] mids, final boolean move) {
        eAssert(isUserPlaylist(plid));
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                if (Err.NO_ERR != result)
                    UiUtils.showTextToast(MusicsActivity.this, result.getMessage());
                else
                    getAdapter().reloadCursorAsync();
            }

            @Override
            public void
            onCancel(DiagAsyncTask task) {
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task, Object... objs) {
                mDb.beginTransaction();
                try {
                    for (long mid : mids) {
                        Err err = mDb.insertVideoToPlaylist(plid, mid);
                        if (Err.NO_ERR != err
                            && (1 == mids.length || Err.DB_DUPLICATED != err)) {
                            return err;
                        } else if (move && isUserPlaylist(mPlid))
                            mDb.deleteVideoFromPlaylist(mPlid, mid);
                    }
                    mDb.setTransactionSuccessful();
                } finally {
                    mDb.endTransaction();
                }
                return Err.NO_ERR;
            }
        };
        new DiagAsyncTask(this,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          move? R.string.moving: R.string.adding,
                          false)
            .execute();
    }

    private void
    addTo(final int[] poss, final boolean move) {
        long plid = isUserPlaylist(mPlid)? mPlid: DB.INVALID_PLAYLIST_ID;
        MusicsAdapter adpr = getAdapter();
        final long[] mids = new long[poss.length];
        for (int i = 0; i < mids.length; i++)
            mids[i] = adpr.getItemId(poss[i]);

        UiUtils.OnPlaylistSelectedListener action = new UiUtils.OnPlaylistSelectedListener() {
            @Override
            public void
            onPlaylist(long plid, Object user) {
                doAddTo(plid, mids, move);
            }
        };

        // exclude current playlist
        UiUtils.buildSelectPlaylistDialog(mDb,
                                          this,
                                          move? R.string.move_to: R.string.add_to,
                                          action,
                                          plid,
                                          null)
               .show();
    }

    private void
    doDeleteMusics(final long plid, final long[] mids) {
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                getAdapter().reloadCursorAsync();
            }
            @Override
            public void
            onCancel(DiagAsyncTask task) {
            }
            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task, Object... objs) {
                mDb.beginTransaction();
                try {
                    for (long mid : mids) {
                        if (isUserPlaylist(plid))
                            mDb.deleteVideoFromPlaylist(plid, mid);
                        else
                            mDb.deleteVideoAndRefsCompletely(mid);
                    }
                    mDb.setTransactionSuccessful();
                } finally {
                    mDb.endTransaction();
                }
                return Err.NO_ERR;
            }
        };
        new DiagAsyncTask(this,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          R.string.deleting,
                          false)
            .execute();
    }

    private void
    deleteMusics(final long[] mids) {
        UiUtils.ConfirmAction action = new UiUtils.ConfirmAction() {
            @Override
            public void
            onOk(Dialog dialog) {
                doDeleteMusics(mPlid, mids);
            }
        };
        UiUtils.buildConfirmDialog(this,
                                   R.string.delete,
                                   isUserPlaylist(mPlid)? R.string.msg_delete_musics
                                                        : R.string.msg_delete_musics_completely,
                                   action)
               .show();
    }

    private void
    setToPlaylistThumbnail(long musicId, int itemPos) {
        eAssert(isUserPlaylist(mPlid));
        byte[] data = getAdapter().getMusicThumbnail(itemPos);
        mDb.updatePlaylist(mPlid, DB.ColPlaylist.THUMBNAIL, data);
        // update current screen's thumbnail too.
        UiUtils.setThumbnailImageView(((ImageView)findViewById(R.id.thumbnail)), data);
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        YTPlayer.Video vid = new YTPlayer.Video(getAdapter().getMusicYtid(position),
                                                getAdapter().getMusicTitle(position),
                                                getAdapter().getMusicVolume(position),
                                                getAdapter().getMusicPlaytime(position));
        startVideos(new YTPlayer.Video[] { vid });
    }

    private void
    onToolPlay(View anchor) {
        MusicsAdapter adpr = getAdapter();
        int[] poss = adpr.getCheckedMusics();
        if (0 == poss.length) {
            UiUtils.showTextToast(this, R.string.msg_no_items_selected);
            return;
        }

        YTPlayer.Video[] vs = new YTPlayer.Video[poss.length];
        for (int i = 0; i < poss.length; i++)
            vs[i] = new YTPlayer.Video(adpr.getMusicYtid(poss[i]),
                                       adpr.getMusicTitle(poss[i]),
                                       adpr.getMusicVolume(poss[i]),
                                       adpr.getMusicPlaytime(poss[i]));
        startVideos(vs);
        adpr.clearCheckState();
        adpr.notifyDataSetChanged();
    }

    private void
    onToolCopy(View anchor) {
        MusicsAdapter adpr = getAdapter();
        int[] poss = adpr.getCheckedMusics();
        if (0 == poss.length) {
            UiUtils.showTextToast(this, R.string.msg_no_items_selected);
            return;
        }

        addTo(poss, false);
    }

    private void
    onToolMove(View anchor) {
        MusicsAdapter adpr = getAdapter();
        int[] poss = adpr.getCheckedMusics();
        if (0 == poss.length) {
            UiUtils.showTextToast(this, R.string.msg_no_items_selected);
            return;
        }

        addTo(poss, true);
    }

    private void
    onToolDelete(View anchor) {
        MusicsAdapter adpr = getAdapter();
        int[] poss = adpr.getCheckedMusics();
        if (0 == poss.length) {
            UiUtils.showTextToast(this, R.string.msg_no_items_selected);
            return;
        }

        long[] mids = new long[poss.length];
        for (int i = 0; i < poss.length; i++)
            mids[i] = adpr.getItemId(poss[i]);

        deleteMusics(mids);
    }

    private void
    setupToolButtons() {
        ImageView iv = (ImageView)findViewById(R.id.play);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToolPlay(v);
            }
        });

        iv = (ImageView)findViewById(R.id.copy);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToolCopy(v);
            }
        });

        iv = (ImageView)findViewById(R.id.move);
        if (isUserPlaylist(mPlid)) {
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onToolMove(v);
                }
            });
        } else
            iv.setVisibility(View.INVISIBLE);

        iv = (ImageView)findViewById(R.id.delete);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToolDelete(v);
            }
        });
    }

    private void
    onContextMenuVolume(final long itemId, final int itemPos) {
        YTPlayer.get().changeVideoVolume(getAdapter().getMusicTitle(itemPos),
                                         getAdapter().getMusicYtid(itemPos));
    }

    private void
    onContextMenuAppendToPlayQ(final long itemId, final int itemPos) {
        YTPlayer.Video vid = new YTPlayer.Video(getAdapter().getMusicYtid(itemPos),
                                                getAdapter().getMusicTitle(itemPos),
                                                getAdapter().getMusicVolume(itemPos),
                                                getAdapter().getMusicPlaytime(itemPos));
        if (!YTPlayer.get().appendToCurrentPlayQ(vid))
            UiUtils.showTextToast(this, R.string.err_unknown);
    }

    private void
    onContextMenuRename(final long itemId, final int itemPos) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void
            prepare(Dialog dialog, EditText edit) { }

            @Override
            public void
            onOk(Dialog dialog, EditText edit) {
                mDb.updateVideo(DB.ColVideo.ID, itemId,
                                DB.ColVideo.TITLE, edit.getText().toString());
                getAdapter().reloadCursorAsync();
            }
        };
        AlertDialog diag = UiUtils.buildOneLineEditTextDialog(this,
                                                              R.string.rename,
                                                              getAdapter().getMusicTitle(itemPos),
                                                              action);
        diag.show();

    }

    private void
    onContextMenuPlayVideo(final long itemId, final int itemPos) {
        UiUtils.playAsVideo(this, getAdapter().getMusicYtid(itemPos));
    }

    // ========================================================================
    //
    // Overriding Activity Member Functions
    //
    // ========================================================================
    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.add_to:
            addTo(new int[] { info.position }, false);
            return true;

        case R.id.move_to:
            addTo(new int[] { info.position }, true);
            return true;

        case R.id.volume:
            onContextMenuVolume(info.id, info.position);
            return true;

        case R.id.append_to_playq:
            onContextMenuAppendToPlayQ(info.id, info.position);
            return true;

        case R.id.plthumbnail:
            setToPlaylistThumbnail(info.id, info.position);
            return true;

        case R.id.rename:
            onContextMenuRename(info.id, info.position);
            return true;

        case R.id.play_video:
            onContextMenuPlayVideo(info.id, info.position);
            return true;

        case R.id.delete:
            deleteMusics(new long[] { info.id });
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
        inflater.inflate(R.menu.musics_context, menu);
        //AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;

        // NOTE
        // Html5 Youtube player doens't support setVolume / mute functionality.
        // (Only stream volume is available.)
        // But SWF player supports this.
        // So, enable volume menu by default.
        boolean visible = isUserPlaylist(mPlid)? true: false;
        menu.findItem(R.id.move_to).setVisible(visible);
        menu.findItem(R.id.plthumbnail).setVisible(visible);

        visible = YTPlayer.get().hasActiveVideo();
        menu.findItem(R.id.append_to_playq).setVisible(visible);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.musics);

        String searchWord = null;
        mPlid = getIntent().getLongExtra("plid", PLID_INVALID);
        eAssert(PLID_INVALID != mPlid);

        if (isUserPlaylist(mPlid)) {
            String title = getIntent().getStringExtra("title");
            ((TextView)findViewById(R.id.title)).setText(title);

            byte[] imgdata = getIntent().getByteArrayExtra("thumbnail");
            UiUtils.setThumbnailImageView(((ImageView)findViewById(R.id.thumbnail)), imgdata);
        } else if (PLID_RECENT_PLAYED == mPlid) {
            ((TextView)findViewById(R.id.title)).setText(R.string.recently_played);
            ((ImageView)findViewById(R.id.thumbnail)).setImageResource(R.drawable.ic_recently_played_up);
        } else if (PLID_SEARCHED == mPlid) {
            String word = getIntent().getStringExtra("word");
            searchWord = (null == word)? "": word;
            String title = Utils.getAppContext().getResources().getText(R.string.keyword) + " : " + word;
            ((TextView)findViewById(R.id.title)).setText(title);
            ((ImageView)findViewById(R.id.thumbnail)).setImageResource(R.drawable.ic_search_list_up);
        }

        setupToolButtons();

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
        MusicsAdapter adapter = new MusicsAdapter(this,
                                                  new MusicsAdapter.CursorArg(mPlid, searchWord),
                                                  mCheckListener);
        mListv.setAdapter(adapter);
        adapter.reloadCursorAsync();
    }

    @Override
    protected void
    onResume() {
        super.onResume();
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        mMp.setController(this, playerv, (ViewGroup)findViewById(R.id.list_drawer), null);
        if (mMp.hasActiveVideo())
            playerv.setVisibility(View.VISIBLE);
        else
            playerv.setVisibility(View.GONE);
    }

    @Override
    protected void
    onPause() {
        mMp.unsetController(this);
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
        super.onBackPressed();
    }
}
