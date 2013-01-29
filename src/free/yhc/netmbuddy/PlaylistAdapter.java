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

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class PlaylistAdapter extends ResourceCursorAdapter {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(PlaylistAdapter.class);

    private static final int LAYOUT = R.layout.playlist_row;

    private static final int COLI_ID        = 0;
    private static final int COLI_TITLE     = 1;
    private static final int COLI_SIZE      = 2;

    private final Context                     mContext;
    private final OnItemButtonClickListener   mOnItemBtnClick;
    private final View.OnClickListener        mDetailListOnClick = new View.OnClickListener() {
        @Override
        public void
        onClick(View v) {
            if (null != mOnItemBtnClick)
                mOnItemBtnClick.onClick((Integer)v.getTag(), ItemButton.LIST);
        }
    };

    public enum ItemButton {
        LIST,
    }

    public interface OnItemButtonClickListener {
        void onClick(int pos, ItemButton button);
    }

    private Cursor
    createCursor() {
        return DB.get().queryPlaylist(new ColPlaylist[] {
                ColPlaylist.ID,
                ColPlaylist.TITLE,
                ColPlaylist.SIZE
        });
    }

    public PlaylistAdapter(Context context,
                           OnItemButtonClickListener listener) {
        super(context, LAYOUT, null);
        mContext        = context;
        mOnItemBtnClick = listener;
    }

    public void
    reloadCursor() {
        changeCursor(createCursor());
    }

    public void
    reloadCursorAsync() {
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            private Cursor newCursor;
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                changeCursor(newCursor);
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                newCursor = createCursor();
                return Err.NO_ERR;
            }
        };
        new DiagAsyncTask(mContext,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          R.string.loading)
            .run();
    }

    public String
    getItemTitle(int pos) {
        Cursor c = getCursor();
        if (c.moveToPosition(pos))
            return c.getString(COLI_TITLE);
        eAssert(false);
        return null;
    }

    public byte[]
    getItemThumbnail(int pos) {
        Cursor c = getCursor();
        if (c.moveToPosition(pos))
            return (byte[])DB.get().getPlaylistInfo(c.getLong(COLI_ID), ColPlaylist.THUMBNAIL);
        eAssert(false);
        return null;
    }

    @Override
    public void
    bindView(View view, Context context, Cursor cur) {
        ImageView thumbnailv = (ImageView)view.findViewById(R.id.thumbnail);
        TextView  titlev     = (TextView) view.findViewById(R.id.title);
        TextView  nritemsv   = (TextView) view.findViewById(R.id.nritems);
        ImageView listbtn    = (ImageView)view.findViewById(R.id.detaillist);
        listbtn.setTag(cur.getPosition());
        listbtn.setOnClickListener(mDetailListOnClick);

        titlev.setText(cur.getString(COLI_TITLE));
        nritemsv.setText(cur.getLong(COLI_SIZE) + "");
        byte[] thumbnailData = (byte[])DB.get().getPlaylistInfo(cur.getLong(COLI_ID), ColPlaylist.THUMBNAIL);
        UiUtils.setThumbnailImageView(thumbnailv, thumbnailData);
    }
}
