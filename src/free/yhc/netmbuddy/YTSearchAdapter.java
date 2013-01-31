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

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public abstract class YTSearchAdapter extends BaseAdapter implements
YTSearchHelper.LoadThumbnailDoneReceiver {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTSearchAdapter.class);

    // So, assign one of them as view tag's key value.
    protected static final int VTAGKEY_VALID      = R.id.content;

    protected final Context         mCxt;
    // View holder for each item
    protected View[]                mItemViews;
    protected YTFeed.Entry[]        mEntries;

    private Bitmap[]                mThumbnails;
    private YTSearchHelper          mHelper;

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
            // NOTE!
            // IMPORTANT! : DO NOT put R.drawable.ic_unknown_image at layout!
            // Because of 'memory optimization' for thumbnail bitmap,
            //   putting drawable at Layout may lead to "Exception : try to used recycled bitmap ...".
            // See comments at UiUtils.setThumbnailImageView() for details.
            // Initialize thumbnail to ic_unknown_image
            UiUtils.setThumbnailImageView((ImageView)mItemViews[i].findViewById(R.id.thumbnail), null);
            setViewInvalid(mItemViews[i]);
            final YTSearchHelper.LoadThumbnailArg arg
                = new YTSearchHelper.LoadThumbnailArg(i,
                                                      mEntries[i].media.thumbnailUrl,
                                                      mCxt.getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                                                      mCxt.getResources().getDimensionPixelSize(R.dimen.thumbnail_height));
            mThumbnails[i] = null;
            if (null != mHelper)
                mHelper.loadThumbnailAsync(arg);
        }
    }


    protected int
    pos2index(int pos) {
        return pos + 1;
    }

    protected void
    setViewInvalid(View v) {
        v.setTag(VTAGKEY_VALID, false);
    }

    protected void
    setViewInvalid(int pos) {
        setViewInvalid(mItemViews[pos]);
    }

    protected void
    setViewValid(View v) {
        v.setTag(VTAGKEY_VALID, true);
    }

    protected boolean
    isViewValid(int pos) {
        return (Boolean)mItemViews[pos].getTag(VTAGKEY_VALID);
    }

    protected abstract void
    setItemView(int position, View v, YTFeed.Entry e);

    /**
     * This should be called when adapter is no more used.
     * Adapter caching each music icons.
     * So, it occupies lots of memory.
     * To free those memories before GC, calling cleanup might be useful.
     */
    public void
    cleanup() {
        eAssert(Utils.isUiThread());
        for (int i = 0; i < mThumbnails.length; i++) {
            if (null != mThumbnails[i]) {
                mThumbnails[i].recycle();
                mThumbnails[i] = null;
            }
        }

        mHelper = null;
    }

    public YTFeed.Entry[]
    getEntries() {
        return mEntries;
    }

    public Bitmap
    getItemThumbnail(int pos) {
        return mThumbnails[pos];
    }


    @Override
    public void
    loadThumbnailDone(YTSearchHelper helper, YTSearchHelper.LoadThumbnailArg arg,
                      Bitmap bm, YTSearchHelper.Err err) {
        if (null == mHelper || mHelper != helper) {
            helper.close(true);
            return; // invalid callback.
        }

        if (YTSearchHelper.Err.NO_ERR != err) {
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
        setItemView(position, v, e);
        return v;
    }
}
