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

package free.yhc.netmbuddy.model;

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

public class YTPlayerVideoListAdapter  extends BaseAdapter{
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTPlayerVideoListAdapter.class);

    private final int mActiveTextColor;
    private final int mInactiveTextColor;
    private final Context mContext;
    private final HashMap<View, Integer>    mView2PosMap = new HashMap<View, Integer>();

    private YTPlayer.Video[]    mVs;
    private int                 mActivePos = -1;

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
        tv.setText(((YTPlayer.Video)getItem(position)).title);

        if (mActivePos >=0
            && position == mActivePos)
            setToActive(v);
        else
            setToInactive(v);

        return tv;
    }
}
