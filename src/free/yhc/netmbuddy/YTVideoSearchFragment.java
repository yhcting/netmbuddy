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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.db.DBHelper;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.model.YTVideoFeed;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class YTVideoSearchFragment extends YTSearchFragment implements
YTSearchHelper.SearchDoneReceiver,
DBHelper.CheckDupDoneReceiver {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTVideoSearchFragment.class);

    private final DB        mDb = DB.get();
    private final YTPlayer  mMp = YTPlayer.get();

    private DBHelper        mDbHelper;

    private final OnPlayerUpdateDBListener mOnPlayerUpdateDbListener
        = new OnPlayerUpdateDBListener();

    private class OnPlayerUpdateDBListener implements YTPlayer.OnDBUpdatedListener {
        @Override
        public void
        onDbUpdated(YTPlayer.DBUpdateType ty) {
            switch (ty) {
            case PLAYLIST:
                showLoadingLookAndFeel();
                YTVideoSearchAdapter adapter = getAdapter();
                if (null != adapter)
                    checkDupAsync(null, (YTVideoFeed.Entry[])adapter.getEntries());
            }
            // others are ignored.
        }
    }

    private YTVideoSearchActivity
    getMyActivity() {
        return (YTVideoSearchActivity)super.getActivity();
    }

    private YTVideoSearchAdapter
    getAdapter() {
        return (YTVideoSearchAdapter)mListv.getAdapter();
    }

    private void
    onContextMenuAddTo(final int position) {
        UiUtils.OnPlaylistSelected action = new UiUtils.OnPlaylistSelected() {
            @Override
            public void
            onPlaylist(long plid, Object user) {
                int pos = (Integer)user;
                int volume = getAdapter().getItemVolume(pos);
                int msg = getMyActivity().addToPlaylist(getAdapter(), plid, pos, volume);
                if (0 != msg)
                    UiUtils.showTextToast(getMyActivity(), msg);
            }

            @Override
            public void
            onUserMenu(int pos, Object user) {}

        };

        UiUtils.buildSelectPlaylistDialog(mDb,
                                          getMyActivity(),
                                          R.string.add_to,
                                          null,
                                          action,
                                          DB.INVALID_PLAYLIST_ID,
                                          position)
               .show();
    }

    private void
    onContextMenuAppendToPlayQ(final int position) {
        YTPlayer.Video vid = getAdapter().getYTPlayerVideo(position);
        getMyActivity().appendToPlayQ(new YTPlayer.Video[] { vid });

    }

    private void
    onContextMenuPlayVideo(final int position) {
        UiUtils.playAsVideo(getMyActivity(), getAdapter().getItemVideoId(position));
    }

    private void
    onContextMenuVideosOfThisAuthor(final int position) {
        Intent i = new Intent(getActivity(), YTVideoSearchAuthorActivity.class);
        i.putExtra(YTSearchActivity.MAP_KEY_SEARCH_TEXT, getAdapter().getItemAuthor(position));
        startActivity(i);
    }

    private void
    onContextMenuPlaylistsOfThisAuthor(final int position) {
        Intent i = new Intent(getActivity(), YTPlaylistSearchActivity.class);
        i.putExtra(YTSearchActivity.MAP_KEY_SEARCH_TEXT, getAdapter().getItemAuthor(position));
        startActivity(i);
    }

    private void
    checkDupAsync(Object tag, YTVideoFeed.Entry[] entries) {
        mDbHelper.close();

        // Create new instance whenever it used to know owner of each callback.
        mDbHelper = new DBHelper();
        mDbHelper.setCheckDupDoneReceiver(this);
        mDbHelper.open();
        mDbHelper.checkDupAsync(new DBHelper.CheckDupArg(tag, entries));
    }

    private void
    checkDupDoneNewEntries(DBHelper.CheckDupArg arg, boolean[] results) {
        YTSearchHelper.SearchArg sarg = (YTSearchHelper.SearchArg)arg.tag;

        // helper's event receiver is changed to adapter in adapter's constructor.
        YTVideoSearchAdapter adapter = new YTVideoSearchAdapter(getMyActivity(),
                                                                mSearchHelper,
                                                                arg.ents);
        // First request is done!
        // Now we know total Results.
        // Let's build adapter and enable list.
        applyDupCheckResults(adapter, results);
        YTVideoSearchAdapter oldAdapter = getAdapter();
        mListv.setAdapter(adapter);

        // initialize adapter
        onSetToPrimary(isPrimary());

        // Cleanup before as soon as possible to secure memories.
        if (null != oldAdapter)
            oldAdapter.cleanup();
    }

    private void
    applyDupCheckResults(YTVideoSearchAdapter adapter, boolean[] results) {
        for (int i = 0; i < results.length; i++) {
            if (results[i])
                adapter.setToDup(i);
            else
                adapter.setToNew(i);
        }
    }

    @Override
    protected void
    onListItemClick(View view, int position, long itemId) {
        YTPlayer.Video v = getAdapter().getYTPlayerVideo(position);
        mMp.startVideos(new YTPlayer.Video[] { v });
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    public
    YTVideoSearchFragment() {
        super();
    }

    public YTVideoSearchAdapter
    getListAdapter() {
        if (null != mListv)
            return (YTVideoSearchAdapter)mListv.getAdapter();
        return null;
    }

    @Override
    public void
    searchDone(YTSearchHelper helper, YTSearchHelper.SearchArg arg,
               YTFeed.Result result, YTSearchHelper.Err err) {
        if (!handleSearchResult(helper, arg, result, err))
            return; // There is an error in search
        checkDupAsync(arg, (YTVideoFeed.Entry[])result.entries);
    }

    @Override
    public void
    checkDupDone(DBHelper helper, DBHelper.CheckDupArg arg,
                 boolean[] results, DBHelper.Err err) {
        if (null == mDbHelper
            || helper != mDbHelper
            || null == getActivity()) {
            helper.close();
            return; // invalid callback.
        }

        stopLoadingLookAndFeel();

        if (DBHelper.Err.NO_ERR != err
            || results.length != arg.ents.length) {
            showErrorMessage(R.string.err_db_unknown);
            return;
        }

        if (null != getAdapter()
            && arg.ents == getAdapter().getEntries())
            // Entry is same with current adapter.
            // That means 'dup. checking is done for exsiting entries"
            applyDupCheckResults(getAdapter(), results);
        else
            checkDupDoneNewEntries(arg, results);

    }
    @Override
    public void
    onSetToPrimary(boolean primary) {
       if (null != getListAdapter()) {
           if (primary)
               getListAdapter().setCheckStateListener(getMyActivity().getAdapterCheckStateListener());
           else {
               getListAdapter().cleanChecked();
               getListAdapter().unsetCheckStateListener();
           }
       }
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        if (!isPrimary()
            || !getMyActivity().isContextMenuOwner(this))
            return false;

        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.add_to:
            onContextMenuAddTo(info.position);
            return true;

        case R.id.append_to_playq:
            onContextMenuAppendToPlayQ(info.position);
            return true;

        case R.id.play_video:
            onContextMenuPlayVideo(info.position);
            return true;

        case R.id.videos_of_this_author:
            onContextMenuVideosOfThisAuthor(info.position);
            return true;

        case R.id.playlists_of_this_author:
            onContextMenuPlaylistsOfThisAuthor(info.position);
            return true;
        }
        return false;
    }

    @Override
    public void
    onCreateContextMenu2(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu2(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.ytvideosearch_context, menu);
        // AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;
        boolean visible = (YTSearchHelper.SearchType.VID_AUTHOR == getType())? false: true;
        menu.findItem(R.id.videos_of_this_author).setVisible(visible);
    }

    @Override
    public void
    onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHelper = new DBHelper();
    }

    @Override
    public View
    onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        return v;
    }

    @Override
    public void
    onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void
    onStart() {
        super.onStart();
    }

    @Override
    public void
    onResume() {
        super.onResume();
        mMp.addOnDbUpdatedListener(this, mOnPlayerUpdateDbListener);
        if (mDb.isRegisteredToVideoTableWatcher(this)) {
            if (mDb.isVideoTableUpdated(this)
                && null != getAdapter()) {
                showLoadingLookAndFeel();
                checkDupAsync(null, (YTVideoFeed.Entry[])getAdapter().getEntries());
            }
            mDb.unregisterToVideoTableWatcher(this);
        }
    }

    @Override
    public void
    onPause() {
        mMp.removeOnDbUpdatedListener(this);
        mDb.registerToVideoTableWatcher(this);
        super.onPause();
    }

    @Override
    public void
    onStop() {
        super.onStop();
    }

    @Override
    public void
    onEarlyDestroy() {
        onDestroyInternal();
        super.onEarlyDestroy();
    }

    @Override
    public void
    onDestroyView() {
        super.onDestroyView();
    }

    private void
    onDestroyInternal() {
        mDbHelper.close();
        mDb.unregisterToVideoTableWatcher(this);
    }

    @Override
    public void
    onDestroy() {
        onDestroyInternal();
        super.onDestroy();
    }

    @Override
    public void
    onDetach() {
        super.onDetach();
    }
}
