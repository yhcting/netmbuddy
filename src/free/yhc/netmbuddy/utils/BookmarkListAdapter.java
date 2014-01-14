/******************************************************************************
 *    Copyright (C) 2012, 2013, 2014 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of NetMBuddy.
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
 *    along with this program.	If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy.utils;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.DB;

class BookmarkListAdapter extends BaseAdapter {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(BookmarkListAdapter.class);

    private final Context       mContext;
    private final OnItemAction  mOnItemAction;

    private DB.Bookmark[] mBms;

    interface OnItemAction {
        void onDelete(BookmarkListAdapter adapter, int pos, DB.Bookmark bm);
    }

    BookmarkListAdapter(Context          context,
                        DB.Bookmark[]    bms,
                        OnItemAction     onItemAction) {
        super();
        eAssert(null != bms
                && null != onItemAction);
        mContext = context;
        mBms = bms;
        mOnItemAction = onItemAction;
    }

    void
    removeItem(int pos) {
        eAssert(0 <= pos && pos < mBms.length);
        DB.Bookmark[] bms = new DB.Bookmark[mBms.length - 1];

        // Clone bookmark array except for item at 'pos'.
        int i = 0;
        while (i < pos) {
            bms[i] = mBms[i];
            i++;
        }
        while (i < bms.length) {
            bms[i] = mBms[i + 1];
            i++;
        }
        mBms = bms;
        notifyDataSetChanged();
    }

    @Override
    public int
    getCount() {
        return mBms.length;
    }

    @Override
    public Object
    getItem(int position) {
        return mBms[position];
    }

    @Override
    public long
    getItemId(int position) {
        return position;
    }

    @Override
    public View
    getView(final int position, View convertView, ViewGroup parent) {
        View v;
        if (null != convertView)
            v = convertView;
        else
            v = UiUtils.inflateLayout(mContext, R.layout.bookmark_row);

        DB.Bookmark bm = mBms[position];
        ImageView delv = (ImageView)v.findViewById(R.id.delete);
        TextView  tv   = (TextView)v.findViewById(R.id.name);

        delv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                mOnItemAction.onDelete(BookmarkListAdapter.this, position, mBms[position]);
            }
        });

        tv.setText("[" + Utils.secsToMinSecText(bm.pos / 1000) + "] " + bm.name);
        return v;
    }
}
