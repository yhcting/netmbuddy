/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015, 2016
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

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.net.MalformedURLException;
import java.net.URL;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.async.TmTask;
import free.yhc.abaselib.util.AUtil;
import free.yhc.netmbuddy.core.TaskManager;
import free.yhc.netmbuddy.task.YTThumbnailTask;
import free.yhc.netmbuddy.utils.UxUtil;

public abstract class YTSearchAdapter<T> extends BaseAdapter {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTSearchAdapter.class, Logger.LOGLV_DEFAULT);

    // So, assign one of them as view tag's key value.
    protected static final int VTAGKEY_VALID = R.id.content;

    protected final Context mCxt;
    // View holder for each item
    protected View[] mItemViews;
    protected T[] mItems;

    private Bitmap[] mThumbnails;
    private final YTThumbnailTask.EventListener<YTThumbnailTask, Bitmap> mThumbnailTaskEventListener
            = new YTThumbnailTask.EventListener<YTThumbnailTask, Bitmap>() {
        @Override
        public void
        onPostRun(@NonNull YTThumbnailTask task,
                  Bitmap result,
                  Exception ex) {
            if (null == result)
                // set to thumbthing else?
                return;
            // View is NOT reused here.
            // So, I don't need to worry about issues comes from reusing view in the list.
            int i = (Integer)task.getOpaque();
            ImageView iv = (ImageView)mItemViews[i].findViewById(R.id.thumbnail);
            mThumbnails[i] = result;
            iv.setImageBitmap(result);
        }
    };

    YTSearchAdapter(Context context,
                    int rowLayout,
                    T[] items) {
        super();
        mCxt = context;

        mItems = items;
        mItemViews = new View[mItems.length];
        for (int i = 0; i < mItemViews.length; i++)
            mItemViews[i] = AUtil.inflateLayout(rowLayout);

        TaskManager tm = TaskManager.get();
        mThumbnails = new Bitmap[mItems.length];
        for (int i = 0; i < mItemViews.length; i++) {
            // NOTE!
            // IMPORTANT! : DO NOT put R.drawable.ic_unknown_image at layout!
            // Because of 'memory optimization' for thumbnail bitmap,
            //   putting drawable at Layout may lead to "Exception : try to used recycled bitmap ...".
            // See comments at UxUtil.setThumbnailImageView() for details.
            // Initialize thumbnail to ic_unknown_image
            mThumbnails[i] = null;
            if (null == getThumnailUrl(mItems[i]))
                continue;

            UxUtil.setThumbnailImageView((ImageView)mItemViews[i].findViewById(R.id.thumbnail), null);
            setViewInvalid(mItemViews[i]);
            URL url;
            try {
                url = new URL(getThumnailUrl(mItems[i]));
            } catch (MalformedURLException e) {
                if (DBG) P.w("MalformedURL?: " + getThumnailUrl(mItems[i]));
                continue;
            }
            YTThumbnailTask t = YTThumbnailTask.create(
                    url,
                    AUtil.getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                    AUtil.getResources().getDimensionPixelSize(R.dimen.thumbnail_height),
                    i);
            t.addEventListener(AppEnv.getUiHandlerAdapter(),mThumbnailTaskEventListener);
            if (!tm.addTask(t,
                            t,
                            this,
                            null)) {
                if (DBG) P.w("Fail to add thumbnail download task");
            }
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
        P.bug(AUtil.isUiThread());
        for (int i = 0; i < mThumbnails.length; i++) {
            if (null != mThumbnails[i]) {
                mThumbnails[i].recycle();
                mThumbnails[i] = null;
            }
        }
        TaskManager tm = TaskManager.get();
        for (TmTask t : tm.getTasks(this))
            tm.cancelTask(t);
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
