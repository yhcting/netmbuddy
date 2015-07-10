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

package free.yhc.netmbuddy.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.db.DMVideo;

class SimilarTitlesListAdapter extends BaseAdapter {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(SimilarTitlesListAdapter.class);

    private final DB mDb;
    private Context mContext;
    private long[] mVids;

    SimilarTitlesListAdapter(Context context,
                             long[] vids) {
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

        CheckBox checkv = (CheckBox)v.findViewById(R.id.checkbtn);
        ImageView thumbnailv = (ImageView)v.findViewById(R.id.thumbnail);
        TextView titlev = (TextView)v.findViewById(R.id.title);
        TextView channelv = (TextView)v.findViewById(R.id.channel);
        TextView playtmv = (TextView)v.findViewById(R.id.playtime);
        TextView uploadtmv = (TextView)v.findViewById(R.id.uploadedtime);

        checkv.setVisibility(View.GONE);
        // TODO Caching video info is helpful to reduce IO bottleneck.
        // NOTE: To reduce cursor's window size, thumbnail is excluded from main adapter cursor.
        DMVideo dbv = mDb.getVideoInfo(mVids[position], DMVideo.sDBProjectionWithoutThumbnail);
        titlev.setText(dbv.title);
        if (Utils.isValidValue(dbv.channelTitle)) {
            channelv.setVisibility(View.VISIBLE);
            channelv.setText(dbv.channelTitle);
        } else
            channelv.setVisibility(View.GONE);
        uploadtmv.setVisibility(View.GONE);
        playtmv.setText(Utils.secsToMinSecText((int)dbv.playtime));

        // NOTE: Load thumbnail separately from main adapter cursor
        byte[] thumbnailData = (byte[])DB.get().getVideoInfo(dbv.id, ColVideo.THUMBNAIL);
        UiUtils.setThumbnailImageView(thumbnailv, thumbnailData);
        return v;
    }
}
