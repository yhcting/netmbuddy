/*****************************************************************************
 *    Copyright (C) 2013 Younghyung Cho. <yhcting77@gmail.com>
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
