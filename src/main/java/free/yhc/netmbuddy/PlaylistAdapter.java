/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015, 2016
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

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.async.Task;
import free.yhc.abaselib.ux.DialogTask;
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.utils.UxUtil;

public class PlaylistAdapter extends ResourceCursorAdapter {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(PlaylistAdapter.class, Logger.LOGLV_DEFAULT);

    private static final int LAYOUT = R.layout.playlist_row;

    private static final ColPlaylist[] sDBProjection = {
        ColPlaylist.ID,
        ColPlaylist.TITLE,
        ColPlaylist.SIZE
    };
    private static final int COLI_ID        = 0;
    private static final int COLI_TITLE     = 1;
    private static final int COLI_SIZE      = 2;

    private final Context mContext;
    private final OnItemButtonClickListener mOnItemBtnClick;
    private final View.OnClickListener mDetailListOnClick = new View.OnClickListener() {
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
        return DB.get().queryPlaylist(sDBProjection);
    }

    public PlaylistAdapter(Context context,
                           OnItemButtonClickListener listener) {
        super(context, LAYOUT, null, true);
        mContext        = context;
        mOnItemBtnClick = listener;
    }

    public void
    reloadCursor() {
        changeCursor(createCursor());
    }

    public void
    reloadCursorAsync() {
        Task<Void> t = new Task<Void>() {
            @Override
            protected Void
            doAsync() {
                final Cursor c = createCursor();
                AppEnv.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        changeCursor(c);
                    }
                });
                return null;
            }
        };

        DialogTask.Builder<DialogTask.Builder> b
                = new DialogTask.Builder<>(mContext, t);
        b.setMessage(R.string.loading);
        if (!b.create().start())
            P.bug();
    }

    public String
    getItemTitle(int pos) {
        Cursor c = getCursor();
        if (c.moveToPosition(pos))
            return c.getString(COLI_TITLE);
        P.bug(false);
        return null;
    }

    public byte[]
    getItemThumbnail(int pos) {
        Cursor c = getCursor();
        if (c.moveToPosition(pos))
            return (byte[])DB.get().getPlaylistInfo(c.getLong(COLI_ID), ColPlaylist.THUMBNAIL);
        P.bug(false);
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
        UxUtil.setThumbnailImageView(thumbnailv, thumbnailData);
    }
}
