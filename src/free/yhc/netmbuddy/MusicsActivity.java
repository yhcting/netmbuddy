/*****************************************************************************
 *    Copyright (C) 2012, 2013 Younghyung Cho. <yhcting77@gmail.com>
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
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class MusicsActivity extends Activity {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(MusicsActivity.class);

    public static final String MAP_KEY_PLAYLIST_ID  = "playlistid";
    public static final String MAP_KEY_TITLE        = "title";
    public static final String MAP_KEY_KEYWORD      = "keyword";
    public static final String MAP_KEY_THUMBNAIL    = "thumbnail";

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
    private final OnPlayerUpdateDBListener mOnPlayerUpdateDbListener
        = new OnPlayerUpdateDBListener();

    private long        mPlid   = UiUtils.PLID_INVALID;
    private ListView    mListv  = null;

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
    setToPlaylistThumbnail(long mid, int pos) {
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
    onListItemClick(View view, int pos, long id) {
        YTPlayer.Video vid = getAdapter().getYTPlayerVideo(pos);
        startVideos(new YTPlayer.Video[] { vid });
    }

    private void
    onToolPlay(View anchor) {
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
    onToolAppendPlayQ(View anchor) {
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
    onContextMenuVolume(final long id, final int pos) {
        mMp.changeVideoVolume(getAdapter().getMusicTitle(pos),
                              getAdapter().getMusicYtid(pos));
    }

    private void
    onContextMenuAppendToPlayQ(final long id, final int pos) {
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
                mDb.updateVideo(ColVideo.ID, id,
                                ColVideo.TITLE, edit.getText().toString());
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
    onContextMenuPlayVideo(final long id, final int pos) {
        UiUtils.playAsVideo(this, getAdapter().getMusicYtid(pos));
    }

    private void
    onContextMenuDetailInfo(final long id, final int pos) {
        UiUtils.showVideoDetailInfo(this, id);
    }

    private void
    onContextMenuVideosOfThisAuthor(final long id, final int pos) {
        Intent i = new Intent(this, YTVideoSearchAuthorActivity.class);
        i.putExtra(YTSearchActivity.MAP_KEY_SEARCH_TEXT, getAdapter().getMusicAuthor(pos));
        startActivity(i);
    }

    private void
    onContextMenuPlaylistsOfThisAuthor(final long id, final int pos) {
        Intent i = new Intent(this, YTPlaylistSearchActivity.class);
        i.putExtra(YTSearchActivity.MAP_KEY_SEARCH_TEXT, getAdapter().getMusicAuthor(pos));
        startActivity(i);
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

        case R.id.videos_of_this_author:
            onContextMenuVideosOfThisAuthor(info.id, info.position);
            return true;

        case R.id.playlists_of_this_author:
            onContextMenuPlaylistsOfThisAuthor(info.id, info.position);
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

        boolean visible = UiUtils.isUserPlaylist(mPlid)? true: false;
        menu.findItem(R.id.plthumbnail).setVisible(visible);

        visible = Utils.isValidValue(getAdapter().getMusicAuthor(mInfo.position));
        menu.findItem(R.id.videos_of_this_author).setVisible(visible);
        menu.findItem(R.id.playlists_of_this_author).setVisible(visible);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
