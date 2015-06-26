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

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import free.yhc.netmbuddy.core.YTFeed;
import free.yhc.netmbuddy.core.YTSearchHelper;
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
