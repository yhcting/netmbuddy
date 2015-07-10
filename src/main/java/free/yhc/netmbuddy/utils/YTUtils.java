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

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.core.YTDataHelper;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.ytapiv3.YTApiFacade;
import free.yhc.netmbuddy.core.YTHacker;

public class YTUtils {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTUtils.class);

    private static YTDataHelper.ThumbnailResp
    loadYtVideoThumbnail(String ytvid) throws YTDataAdapter.YTApiException {
        String thumbnailUrl = YTHacker.getYtVideoThumbnailUrl(ytvid);
        YTDataHelper.ThumbnailReq req = new YTDataHelper.ThumbnailReq(
            null,
            thumbnailUrl,
            Utils.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
            Utils.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_height));
        return YTDataHelper.requestThumbnail(req);
    }

    public static boolean
    verifyYoutubeVideoId(String ytvid) {
        return null != ytvid
               && 11 == ytvid.length();
    }

    @SuppressWarnings("unused")
    public static int
    getAvailableTotalResults(int totalResults) {
        return totalResults < YTApiFacade.MAX_AVAILABLE_RESULTS_FOR_QUERY?
               totalResults:
               YTApiFacade.MAX_AVAILABLE_RESULTS_FOR_QUERY;
    }

    /**
     * Fill Youtube data fil
     */
    @Nullable
    public static YTDataAdapter.Video
    getYtVideoData(String ytvid) {
        try {
            return YTApiFacade.requestVideoInfo(ytvid);
        } catch (YTDataAdapter.YTApiException e) {
            return null;
        }
    }

    @Nullable
    public static Bitmap
    getYtThumbnail(String ytvid) {
        YTDataHelper.ThumbnailResp tr;
        // Loading thumbnail is done.
        try {
            tr = loadYtVideoThumbnail(ytvid);
        } catch (YTDataAdapter.YTApiException e) {
            return null;
        }
        if (null == tr)
            return null;
        return tr.bm;
    }

    public static boolean
    fillYtDataAndThumbnail(DMVideo v) {
        eAssert(verifyYoutubeVideoId(v.ytvid));
        if (!v.isYtDataFilled()) {
            YTDataAdapter.Video ytv = getYtVideoData(v.ytvid);
            if (null == ytv)
                return false;
            v.setYtData(ytv);
        }
        if (!v.isThumbnailFilled()) {
            // NOTE
            // Getting thumbnail URL from youtube video id requires downloanding and parsing.
            // It takes too much time.
            // So, a kind of HACK is used to get thumbnail URL from youtube video id.
            // see comments of 'YTHacker.getYtVideoThumbnailUrl()' for details.
            Bitmap bm = getYtThumbnail(v.ytvid);
            if (null == bm)
                return false;
            v.setThumbnail(ImageUtils.compressBitmap(bm));
            bm.recycle();
        }
        return true;
    }
}
