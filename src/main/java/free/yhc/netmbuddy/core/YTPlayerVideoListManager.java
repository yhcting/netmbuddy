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

import java.util.ArrayList;

import free.yhc.netmbuddy.utils.Utils;

class YTPlayerVideoListManager {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTPlayerVideoListManager.class);

    private YTPlayer.Video[] mVs = null; // video array
    private int mVi = -1;   // video index
    private OnListChangedListener mListener = null;

    interface OnListChangedListener {
        void onChanged(YTPlayerVideoListManager vm);
    }

    YTPlayerVideoListManager(OnListChangedListener listener) {
        mListener = listener;
    }

    @SuppressWarnings("unused")
    void
    setOnListChangedListener(OnListChangedListener listener) {
        eAssert(Utils.isUiThread());
        mListener = listener;
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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

    void
    removeVideo(String ytvid) {
        eAssert(Utils.isUiThread());
        if (null == mVs)
            return;

        ArrayList<YTPlayer.Video> al = new ArrayList<>(mVs.length);
        int adjust = 0;
        for (int i = 0; i < mVs.length; i++) {
            if (!mVs[i].v.ytvid.equals(ytvid))
                al.add(mVs[i]);
            else if (i <= mVi)
                adjust++;
        }
        mVs = al.toArray(new YTPlayer.Video[al.size()]);
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
     * @return -1 if fail to find.
     */
    int
    findVideoExcept(int from, String ytvid) {
        eAssert(from >= 0 && from <= mVs.length);
        for (int i = from; i < mVs.length; i++) {
            if (!ytvid.equals(mVs[i].v.ytvid))
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
