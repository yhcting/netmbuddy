/******************************************************************************
 * Copyright (C) 2012, 2013, 2014
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

package free.yhc.netmbuddy.utils;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;

class SimilarTitlesListAdapter extends BaseAdapter {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(SimilarTitlesListAdapter.class);

    // Below value SHOULD match queries of 'createCursor()'
    private static final int COLI_VIDEOID       = 0;
    private static final int COLI_TITLE         = 1;
    private static final int COLI_AUTHOR        = 2;
    private static final int COLI_PLAYTIME      = 3;

    private static final ColVideo[] sQueryCols
        = new ColVideo[] { ColVideo.VIDEOID,
                           ColVideo.TITLE,
                           ColVideo.AUTHOR,
                           ColVideo.PLAYTIME,
                           };

    private final DB    mDb;
    private Context     mContext;
    private long[]      mVids;;

    SimilarTitlesListAdapter(Context    context,
                             long[]     vids) {
        super();
        mDb = DB.get();
        mContext = context;
        mVids = vids;
    }


    @Override
    public int
    getCount() {
        return mVids.length;
    }

    @Override
    public Object
    getItem(int position) {
        return mVids[position];
    }

    @Override
    public long
    getItemId(int position) {
        return mVids[position];
    }

    @Override
    public View
    getView(final int position, View convertView, ViewGroup parent) {
        View v;
        if (null != convertView)
            v = convertView;
        else
            v = UiUtils.inflateLayout(mContext, R.layout.similar_titles_row);

        CheckBox  checkv     = (CheckBox)v.findViewById(R.id.checkbtn);
        ImageView thumbnailv = (ImageView)v.findViewById(R.id.thumbnail);
        TextView  titlev     = (TextView)v.findViewById(R.id.title);
        TextView  authorv    = (TextView)v.findViewById(R.id.author);
        TextView  playtmv    = (TextView)v.findViewById(R.id.playtime);
        TextView  uploadtmv  = (TextView)v.findViewById(R.id.uploadedtime);

        checkv.setVisibility(View.GONE);
        Cursor c = mDb.queryVideo(mVids[position], sQueryCols);
        c.moveToFirst(); // this SHOULD always succeed.

        titlev.setText(c.getString(COLI_TITLE));
        String author = c.getString(COLI_AUTHOR);
        if (Utils.isValidValue(author)) {
            authorv.setVisibility(View.VISIBLE);
            authorv.setText(author);
        } else
            authorv.setVisibility(View.GONE);
        uploadtmv.setVisibility(View.GONE);
        playtmv.setText(Utils.secsToMinSecText(c.getInt(COLI_PLAYTIME)));
        byte[] thumbnailData = (byte[])DB.get().getVideoInfo(c.getString(COLI_VIDEOID), ColVideo.THUMBNAIL);
        UiUtils.setThumbnailImageView(thumbnailv, thumbnailData);

        c.close();

        return v;
    }
}
