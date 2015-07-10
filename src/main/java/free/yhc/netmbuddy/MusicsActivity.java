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
import android.app.Activity;
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
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class MusicsActivity extends Activity implements
UnexpectedExceptionHandler.Evidence {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(MusicsActivity.class);

    public static final String MAP_KEY_PLAYLIST_ID  = "playlistid";
    public static final String MAP_KEY_TITLE = "title";
    public static final String MAP_KEY_KEYWORD = "keyword";
    public static final String MAP_KEY_THUMBNAIL = "thumbnail";

    private final DB mDb = DB.get();
    private final YTPlayer mMp = YTPlayer.get();

    private final MusicsAdapter.CheckStateListener mCheckListener
        = new MusicsAdapter.CheckStateListener() {
            @Override
            public void
            onStateChanged(int nrChecked, int pos, boolean checked) {
                // do nothing.
            }
        };
    private final OnPlayerUpdateDBListener mOnPlayerUpdateDbListener
        = new OnPlayerUpdateDBListener();

    private long mPlid = UiUtils.PLID_INVALID;
    private ListView mListv = null;

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

    private MusicsAdapter
    getAdapter() {
        return (MusicsAdapter)mListv.getAdapter();
    }

    private void
    startVideos(YTPlayer.Video[] vs) {
        if (!Utils.isNetworkAvailable()) {
            UiUtils.showTextToast(this, Err.IO_NET.getMessage());
            return;
        }
        mMp.startVideos(vs);
    }

    private void
    appendToPlayQ(YTPlayer.Video[] vids) {
        mMp.appendToPlayQ(vids);
    }

    private void
    addTo(final int[] poss, final boolean move) {
        MusicsAdapter adpr = getAdapter();
        final long[] mids = new long[poss.length];
        for (int i = 0; i < mids.length; i++)
            mids[i] = adpr.getItemId(poss[i]);

        UiUtils.OnPostExecuteListener listener = new UiUtils.OnPostExecuteListener() {
            @Override
            public void
            onPostExecute(Err result, Object user) {
                if (Err.NO_ERR != result)
                    UiUtils.showTextToast(MusicsActivity.this, result.getMessage());

                if (move)
                    getAdapter().reloadCursorAsync();
                else
                    getAdapter().cleanChecked();
            }
        };

        UiUtils.addVideosTo(this, null, listener, mPlid, mids, move);
    }

    private void
    deleteMusics(final long[] mids) {
        UiUtils.OnPostExecuteListener listener = new UiUtils.OnPostExecuteListener() {
            @Override
            public void
            onPostExecute(Err result, Object user) {
                if (Err.NO_ERR == result)
                    getAdapter().reloadCursorAsync();
            }
        };
        UiUtils.deleteVideos(this, null, listener, mPlid, mids);
    }

    private void
    setToPlaylistThumbnail(@SuppressWarnings("unused") long mid,
                           int pos) {
        eAssert(UiUtils.isUserPlaylist(mPlid));
        byte[] data = getAdapter().getMusicThumbnail(pos);
        mDb.updatePlaylist(mPlid,
                           new ColPlaylist[] { ColPlaylist.THUMBNAIL,
                                               ColPlaylist.THUMBNAIL_YTVID },
                           new Object[] { data,
                                          getAdapter().getMusicYtid(pos) });
        UiUtils.setThumbnailImageView(((ImageView)findViewById(R.id.thumbnail)), data);
    }

    private void
    onListItemClick(@SuppressWarnings("unused") View view,
                    int pos,
                    @SuppressWarnings("unused") long id) {
        YTPlayer.Video vid = getAdapter().getYTPlayerVideo(pos);
        startVideos(new YTPlayer.Video[] { vid });
    }

    private void
    onToolPlay(@SuppressWarnings("unused") View anchor) {
        MusicsAdapter adpr = getAdapter();
        int[] poss = adpr.getCheckedMusicsSortedByTime();
        if (0 == poss.length) {
            UiUtils.showTextToast(this, R.string.msg_no_items_selected);
            return;
        }

        YTPlayer.Video[] vs = new YTPlayer.Video[poss.length];
        for (int i = 0; i < poss.length; i++)
            vs[i] = adpr.getYTPlayerVideo(poss[i]);
        startVideos(vs);
        adpr.cleanChecked();
    }

    private void
    onToolAppendPlayQ(@SuppressWarnings("unused") View anchor) {
        MusicsAdapter adpr = getAdapter();
        int[] poss = adpr.getCheckedMusicsSortedByTime();
        if (0 == poss.length) {
            UiUtils.showTextToast(this, R.string.msg_no_items_selected);
            return;
        }

        YTPlayer.Video[] vids = new YTPlayer.Video[poss.length];
        int j = 0;
        for (int i : poss)
            vids[j++] = adpr.getYTPlayerVideo(i);
        appendToPlayQ(vids);

        adpr.cleanChecked();

        UiUtils.showTextToast(this, R.string.msg_appended_to_playq);
    }

    private void
    onToolCopy(@SuppressWarnings("unused") View anchor) {
        MusicsAdapter adpr = getAdapter();
        int[] poss = adpr.getCheckedMusics();
        if (0 == poss.length) {
            UiUtils.showTextToast(this, R.string.msg_no_items_selected);
            return;
        }

        addTo(poss, false);
    }

    private void
    onToolMove(@SuppressWarnings("unused") View anchor) {
        MusicsAdapter adpr = getAdapter();
        int[] poss = adpr.getCheckedMusics();
        if (0 == poss.length) {
            UiUtils.showTextToast(this, R.string.msg_no_items_selected);
            return;
        }

        addTo(poss, true);
    }

    private void
    onToolDelete(@SuppressWarnings("unused") View anchor) {
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
            public void
            onClick(View v) {
                onToolPlay(v);
            }
        });

        iv = (ImageView)findViewById(R.id.append_playq);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                onToolAppendPlayQ(v);
            }
        });

        iv = (ImageView)findViewById(R.id.copy);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                onToolCopy(v);
            }
        });

        iv = (ImageView)findViewById(R.id.move);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                onToolMove(v);
            }
        });

        iv = (ImageView)findViewById(R.id.delete);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                onToolDelete(v);
            }
        });
    }

    private void
    onContextMenuVolume(@SuppressWarnings("unused") final long id,
                        final int pos) {
        mMp.changeVideoVolume(getAdapter().getMusicTitle(pos),
                              getAdapter().getMusicYtid(pos));
    }

    private void
    onContextMenuAppendToPlayQ(@SuppressWarnings("unused") final long id,
                               final int pos) {
        YTPlayer.Video vid = getAdapter().getYTPlayerVideo(pos);
        appendToPlayQ(new YTPlayer.Video[] { vid });
    }

    private void
    onContextMenuRename(final long id, final int pos) {
        UiUtils.EditTextAction action = new UiUtils.EditTextAction() {
            @Override
            public void
            prepare(Dialog dialog, EditText edit) { }

            @Override
            public void
            onOk(Dialog dialog, EditText edit) {
                mDb.updateVideoTitle(id, edit.getText().toString());
                getAdapter().reloadCursorAsync();
            }
        };

        UiUtils.buildOneLineEditTextDialog(this,
                                           R.string.rename,
                                           getAdapter().getMusicTitle(pos),
                                           action)
               .show();

    }

    private void
    onContextMenuPlayVideo(@SuppressWarnings("unused") final long id,
                           final int pos) {
        UiUtils.playAsVideo(this, getAdapter().getMusicYtid(pos));
    }

    private void
    onContextMenuDetailInfo(final long id,
                            @SuppressWarnings("unused") final int pos) {
        UiUtils.showVideoDetailInfo(this, id);
    }

    private void
    onContextMenuBookmarks(@SuppressWarnings("unused") final long id,
                           final int pos) {

        UiUtils.showBookmarkDialog(this,
                                   getAdapter().getMusicYtid(pos),
                                   getAdapter().getMusicTitle(pos));
    }

    private void
    onContextMenuVideosOfThisChannel(@SuppressWarnings("unused") final long id,
                                     final int pos) {
        Intent i = new Intent(this, YTVideoSearchChannelActivity.class);
        i.putExtra(YTSearchActivity.KEY_TEXT, getAdapter().getMusicChannelId(pos));
        startActivity(i);
    }

    private void
    onContextMenuSearchSimilarTitles(@SuppressWarnings("unused") final long id,
                                     final int pos) {
        UiUtils.showSimilarTitlesDialog(this, getAdapter().getMusicTitle(pos));
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
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

        case R.id.detail_info:
            onContextMenuDetailInfo(info.id, info.position);
            return true;

        case R.id.bookmarks:
            onContextMenuBookmarks(info.id, info.position);
            return true;

        case R.id.videos_of_same_channel:
            onContextMenuVideosOfThisChannel(info.id, info.position);
            return true;

        case R.id.search_similar_titles:
            onContextMenuSearchSimilarTitles(info.id, info.position);
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
        AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;

        boolean visible = UiUtils.isUserPlaylist(mPlid);
        menu.findItem(R.id.plthumbnail).setVisible(visible);

        visible = Utils.isValidValue(getAdapter().getMusicChannel(mInfo.position));
        menu.findItem(R.id.videos_of_same_channel).setVisible(visible);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UnexpectedExceptionHandler.get().registerModule(this);
        setContentView(R.layout.musics);

        String searchWord = null;
        mPlid = getIntent().getLongExtra(MAP_KEY_PLAYLIST_ID, UiUtils.PLID_INVALID);
        eAssert(UiUtils.PLID_INVALID != mPlid);

        if (UiUtils.isUserPlaylist(mPlid)) {
            String title = getIntent().getStringExtra(MAP_KEY_TITLE);
            ((TextView)findViewById(R.id.title)).setText(title);

            byte[] imgdata = getIntent().getByteArrayExtra(MAP_KEY_THUMBNAIL);
            UiUtils.setThumbnailImageView(((ImageView)findViewById(R.id.thumbnail)), imgdata);
        } else if (UiUtils.PLID_RECENT_PLAYED == mPlid) {
            ((TextView)findViewById(R.id.title)).setText(R.string.recently_played);
            ((ImageView)findViewById(R.id.thumbnail)).setImageResource(R.drawable.ic_recently_played_up);
        } else if (UiUtils.PLID_SEARCHED == mPlid) {
            String word = getIntent().getStringExtra(MAP_KEY_KEYWORD);
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
            onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                onListItemClick(view, pos, id);
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
    }

    @Override
    protected void
    onPause() {
        mMp.removeOnDbUpdatedListener(this);
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
        UnexpectedExceptionHandler.get().unregisterModule(this);
        super.onDestroy();
    }

    @Override
    protected void
    onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK != resultCode)
            return;

        // TODO : NOT implemented yet
        eAssert(false);
        //noinspection StatementWithEmptyBody
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
