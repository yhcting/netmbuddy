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
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(BookmarkListAdapter.class);

    private final Context mContext;
    private final OnItemAction mOnItemAction;

    private DB.Bookmark[] mBms;

    interface OnItemAction {
        void onDelete(BookmarkListAdapter adapter, int pos, DB.Bookmark bm);
    }

    BookmarkListAdapter(Context context,
                        DB.Bookmark[] bms,
                        OnItemAction onItemAction) {
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
