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

package free.yhc.netmbuddy.core;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.HashMap;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class YTPlayerVideoListAdapter extends BaseAdapter {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTPlayerVideoListAdapter.class);

    private final int mActiveTextColor;
    private final int mInactiveTextColor;
    private final Context mContext;
    private final HashMap<View, Integer> mView2PosMap = new HashMap<>();

    private YTPlayer.Video[] mVs;
    private int mActivePos = -1;

    private void
    setToActive(View v) {
        TextView tv = (TextView)v;
        tv.setTextColor(mActiveTextColor);
    }

    private void
    setToInactive(View v) {
        TextView tv = (TextView)v;
        tv.setTextColor(mInactiveTextColor);
    }

    YTPlayerVideoListAdapter(Context context, YTPlayer.Video[] vs) {
        super();
        eAssert(null != vs);
        mContext = context;
        mVs = vs;
        mActiveTextColor = context.getResources().getColor(R.color.title_text_color_new);
        mInactiveTextColor = context.getResources().getColor(R.color.desc_text_color);
    }

    void
    setActiveItem(int pos) {
        if (pos == mActivePos)
            return;

        View v = Utils.findKey(mView2PosMap, mActivePos);
        if (null != v)
            setToInactive(v);
        v = Utils.findKey(mView2PosMap, pos);
        if (null != v)
            setToActive(v);

        mActivePos = pos;
    }

    int
    getActiveItemPos() {
        return mActivePos;
    }

    void
    setVidArray(YTPlayer.Video[] vs) {
        eAssert(null != vs);
        mVs = vs;
    }

    @Override
    public int
    getCount() {
        return mVs.length;
    }

    @Override
    public Object
    getItem(int position) {
        return mVs[position];
    }

    @Override
    public long
    getItemId(int position) {
        return position;
    }

    @Override
    public View
    getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (null != convertView)
            v = convertView;
        else
            v = UiUtils.inflateLayout(mContext, R.layout.mplayer_ldrawer_row);

        mView2PosMap.put(v, position);

        TextView tv = (TextView)v;
        tv.setText(((YTPlayer.Video)getItem(position)).v.title);

        if (mActivePos >=0
            && position == mActivePos)
            setToActive(v);
        else
            setToInactive(v);

        return tv;
    }
}
