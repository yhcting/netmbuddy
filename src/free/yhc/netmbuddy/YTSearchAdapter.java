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

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import free.yhc.netmbuddy.model.Err;
import free.yhc.netmbuddy.model.UiUtils;
import free.yhc.netmbuddy.model.Utils;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTSearchHelper;

public abstract class YTSearchAdapter extends BaseAdapter implements
YTSearchHelper.LoadThumbnailDoneReceiver {
    // So, assign one of them as view tag's key value.
    protected static final int VTAGKEY_POS        = R.id.title;
    protected static final int VTAGKEY_VALID      = R.id.content;

    protected final Context         mCxt;
    // View holder for each item
    protected View[]                mItemViews;
    protected YTFeed.Entry[]        mEntries;

    private Bitmap[]                mThumbnails;
    private final YTSearchHelper    mHelper;

    YTSearchAdapter(Context context,
                    YTSearchHelper helper,
                    int rowLayout,
                    YTFeed.Entry[] entries) {
        super();
        mCxt = context;
        mHelper = helper;

        mEntries = entries;
        mItemViews = new View[mEntries.length];
        mThumbnails = new Bitmap[mEntries.length];

        mHelper.setLoadThumbnailDoneRecevier(this);
        for (int i = 0; i < mItemViews.length; i++) {
            mItemViews[i] = UiUtils.inflateLayout(Utils.getAppContext(), rowLayout);
            markViewInvalid(i);
            YTSearchHelper.LoadThumbnailArg arg
                = new YTSearchHelper.LoadThumbnailArg(i,
                                                      mEntries[i].media.thumbnailUrl,
                                                      mCxt.getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                                                      mCxt.getResources().getDimensionPixelSize(R.dimen.thumbnail_height));
            mThumbnails[i] = null;
            mHelper.loadThumbnailAsync(arg);
        }
    }


    protected int
    pos2index(int pos) {
        return pos + 1;
    }

    protected void
    markViewInvalid(int pos) {
        mItemViews[pos].setTag(VTAGKEY_VALID, false);
    }

    protected boolean
    isViewValid(int pos) {
        return (Boolean)mItemViews[pos].getTag(VTAGKEY_VALID);
    }

    protected abstract void
    setItemView(View v, YTFeed.Entry e);

    /**
     * This should be called when adapter is no more used.
     * Adapter caching each music icons.
     * So, it occupies lots of memory.
     * To free those memories before GC, calling cleanup might be useful.
     */
    public void
    cleanup() {
        for (Bitmap bm : mThumbnails)
            if (null != bm)
                bm.recycle();
    }

    public Bitmap
    getItemThumbnail(int pos) {
        return mThumbnails[pos];
    }


    @Override
    public void
    loadThumbnailDone(YTSearchHelper helper, YTSearchHelper.LoadThumbnailArg arg,
                      Bitmap bm, Err err) {
        if (Err.NO_ERR != err) {
            ; // TODO set to something else...
        } else {
            // View is NOT reused here.
            // So, I don't need to worry about issues comes from reusing view in the list.
            int i = (Integer)arg.tag;
            ImageView iv = (ImageView)mItemViews[i].findViewById(R.id.thumbnail);
            mThumbnails[i] = bm;
            iv.setImageBitmap(bm);
        }
    }


    @Override
    public int
    getCount() {
        return mEntries.length;
    }

    @Override
    public Object
    getItem(int position) {
        return mEntries[position];
    }

    @Override
    public long
    getItemId(int position) {
        return pos2index(position);
    }

    @Override
    public View
    getView(int position, View convertView, ViewGroup parent) {
        View v = mItemViews[position];

        if (isViewValid(position))
            return v;

        YTFeed.Entry e = mEntries[position];
        setItemView(v, e);
        return v;
    }
}
