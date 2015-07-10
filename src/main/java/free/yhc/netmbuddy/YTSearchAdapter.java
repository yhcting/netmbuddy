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

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.core.YTDataHelper;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public abstract class YTSearchAdapter<T> extends BaseAdapter implements
YTDataHelper.ThumbnailRespReceiver {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTSearchAdapter.class);

    // So, assign one of them as view tag's key value.
    protected static final int VTAGKEY_VALID = R.id.content;

    protected final Context mCxt;
    // View holder for each item
    protected View[] mItemViews;
    protected T[] mItems;

    private Bitmap[] mThumbnails;
    private YTDataHelper mHelper;

    YTSearchAdapter(Context context,
                    int rowLayout,
                    T[] items) {
        super();
        mCxt = context;

        mItems = items;
        mItemViews = new View[mItems.length];
        for (int i = 0; i < mItemViews.length; i++)
            mItemViews[i] = UiUtils.inflateLayout(Utils.getAppContext(), rowLayout);

        mThumbnails = new Bitmap[mItems.length];
        mHelper = new YTDataHelper();
        mHelper.setThumbnailRespRecevier(this);
        mHelper.open();
        for (int i = 0; i < mItemViews.length; i++) {
            // NOTE!
            // IMPORTANT! : DO NOT put R.drawable.ic_unknown_image at layout!
            // Because of 'memory optimization' for thumbnail bitmap,
            //   putting drawable at Layout may lead to "Exception : try to used recycled bitmap ...".
            // See comments at UiUtils.setThumbnailImageView() for details.
            // Initialize thumbnail to ic_unknown_image
            mThumbnails[i] = null;
            if (null == getThumnailUrl(mItems[i]))
                continue;

            UiUtils.setThumbnailImageView((ImageView) mItemViews[i].findViewById(R.id.thumbnail), null);
            setViewInvalid(mItemViews[i]);
            final YTDataHelper.ThumbnailReq req
                = new YTDataHelper.ThumbnailReq(i,
                                                getThumnailUrl(mItems[i]),
                                                mCxt.getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                                                mCxt.getResources().getDimensionPixelSize(R.dimen.thumbnail_height));
            mHelper.requestThumbnailAsync(req);
        }

    }

    protected abstract String
    getThumnailUrl(T item);

    protected abstract void
    setItemView(int position, View v, T item);

    protected int
    pos2index(int pos) {
        return pos + 1;
    }

    protected void
    setViewInvalid(View v) {
        v.setTag(VTAGKEY_VALID, false);
    }

    @SuppressWarnings("unused")
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
        if (null != mHelper)
            mHelper.close(true);
        mHelper = null;
    }

    public T[]
    getItems() {
        return mItems;
    }

    public Bitmap
    getItemThumbnail(int pos) {
        if (0 <= pos
            && mThumbnails.length > pos)
            return mThumbnails[pos];
        return null;
    }

    @Override
    public void
    onResponse(YTDataHelper helper, YTDataHelper.ThumbnailReq req, YTDataHelper.ThumbnailResp resp) {
        if (mHelper != helper)
            return; // invalid callback.

        //noinspection StatementWithEmptyBody
        if (YTDataAdapter.Err.NO_ERR != resp.err) {
            // TODO set to something else...
        } else {
            // View is NOT reused here.
            // So, I don't need to worry about issues comes from reusing view in the list.
            int i = (Integer)resp.opaque;
            ImageView iv = (ImageView)mItemViews[i].findViewById(R.id.thumbnail);
            mThumbnails[i] = resp.bm;
            iv.setImageBitmap(resp.bm);
        }
    }


    @Override
    public int
    getCount() {
        return mItems.length;
    }

    @Override
    public Object
    getItem(int position) {
        return mItems[position];
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

        T item = mItems[position];
        setItemView(position, v, item);
        return v;
    }
}
