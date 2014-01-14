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

package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.ArrayList;

import free.yhc.netmbuddy.utils.Utils;

class YTPlayerVideoListManager {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTPlayerVideoListManager.class);

    // NOTE!
    // mVs is accessed on by UIThread.
    // So, synchronization is not required.
    private final Object        mVsLock = new Object();
    private YTPlayer.Video[]    mVs = null; // video array
    private int                 mVi = -1;   // video index
    private OnListChangedListener   mListener = null;

    interface OnListChangedListener {
        void onChanged(YTPlayerVideoListManager vm);
    }

    YTPlayerVideoListManager(OnListChangedListener listener) {
        mListener = listener;
    }

    void
    setOnListChangedListener(OnListChangedListener listener) {
        eAssert(Utils.isUiThread());
        mListener = listener;
    }

    void
    clearOnListChangedListener() {
        eAssert(Utils.isUiThread());
        mListener = null;
    }

    void
    notifyToListChangedListener() {
        if (null != mListener)
            mListener.onChanged(this);
    }

    int
    size() {
        eAssert(Utils.isUiThread());
        return mVs.length;
    }

    boolean
    hasActiveVideo() {
        eAssert(Utils.isUiThread());
        return null != getActiveVideo();
    }

    boolean
    hasNextVideo() {
        eAssert(Utils.isUiThread());
        return hasActiveVideo()
               && mVi < (mVs.length - 1);
    }

    boolean
    hasPrevVideo() {
        eAssert(Utils.isUiThread());
        return hasActiveVideo() && 0 < mVi;
    }

    boolean
    isValidVideoIndex(int i) {
        return 0 <= i && i < mVs.length;
    }

    void
    reset() {
        eAssert(Utils.isUiThread());
        mVs = null;
        mVi = -1;
        notifyToListChangedListener();
    }

    void
    setVideoList(YTPlayer.Video[] vs) {
        eAssert(Utils.isUiThread());
        mVs = vs;
        if (null == mVs || 0 >= mVs.length)
            reset();
        else if(mVs.length > 0)
            mVi = 0;
        notifyToListChangedListener();
    }

    YTPlayer.Video[]
    getVideoList() {
        eAssert(Utils.isUiThread());
        return mVs;
    }

    void
    appendVideo(YTPlayer.Video vids[]) {
        eAssert(Utils.isUiThread());
        YTPlayer.Video[] newvs = new YTPlayer.Video[mVs.length + vids.length];
        System.arraycopy(mVs, 0, newvs, 0, mVs.length);
        System.arraycopy(vids, 0, newvs, mVs.length, vids.length);
        mVs = newvs;
        notifyToListChangedListener();
    }

    /**
     *
     * @param index
     * @return
     *   false if -1 == mVi after removing. Otherwise true.
     */
    void
    removeVideo(String ytvid) {
        eAssert(Utils.isUiThread());
        if (null == mVs)
            return;

        ArrayList<YTPlayer.Video> al = new ArrayList<YTPlayer.Video>(mVs.length);
        int adjust = 0;
        for (int i = 0; i < mVs.length; i++) {
            if (!mVs[i].ytvid.equals(ytvid))
                al.add(mVs[i]);
            else if (i <= mVi)
                adjust++;
        }
        mVs = al.toArray(new YTPlayer.Video[0]);
        mVi = mVi - adjust;
        eAssert(mVi >= 0 || mVi <= mVs.length);
        notifyToListChangedListener();
    }

    int
    getActiveVideoIndex() {
        return mVi;
    }

    /**
     * find video index that is NOT 'ytvid'
     * @param ytvid
     * @return
     *   -1 if fail to find.
     */
    int
    findVideoExcept(int from, String ytvid) {
        eAssert(from >= 0 && from <= mVs.length);
        for (int i = from; i < mVs.length; i++) {
            if (!ytvid.equals(mVs[i].ytvid))
                return i;
        }
        return -1;
    }

    YTPlayer.Video
    getActiveVideo() {
        eAssert(Utils.isUiThread());
        if (null != mVs && 0 <= mVi && mVi < mVs.length)
            return mVs[mVi];
        return null;
    }

    YTPlayer.Video
    getNextVideo() {
        eAssert(Utils.isUiThread());
        if (!hasNextVideo())
            return null;
        return mVs[mVi + 1];
    }

    boolean
    moveTo(int index) {
        eAssert(Utils.isUiThread());
        if (index < 0 || index >= mVs.length)
            return false;
        mVi = index;
        return true;
    }

    boolean
    moveToFist() {
        eAssert(Utils.isUiThread());
        return moveTo(0);
    }

    boolean
    moveToNext() {
        eAssert(Utils.isUiThread());
        return moveTo(mVi + 1);
    }

    boolean
    moveToPrev() {
        eAssert(Utils.isUiThread());
        return moveTo(mVi - 1);
    }
}
