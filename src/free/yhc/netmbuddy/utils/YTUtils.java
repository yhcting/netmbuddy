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

package free.yhc.netmbuddy.utils;

import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTConstants;
import free.yhc.netmbuddy.model.YTHacker;
import free.yhc.netmbuddy.model.YTSearchHelper;

public class YTUtils {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTUtils.class);

    public static boolean
    verifyYoutubeVideoId(String ytvid) {
        return 11 == ytvid.length();
    }

    public static int
    getAvailableTotalResults(int totalResults) {
        return totalResults < YTConstants.MAX_AVAILABLE_RESULTS_FOR_QUERY?
               totalResults:
               YTConstants.MAX_AVAILABLE_RESULTS_FOR_QUERY;
    }

    public static YTSearchHelper.LoadThumbnailReturn
    loadYtVideoThumbnail(String ytvid) {
        String thumbnailUrl = YTHacker.getYtVideoThumbnailUrl(ytvid);
        YTSearchHelper.LoadThumbnailArg targ = new YTSearchHelper.LoadThumbnailArg(
                null,
                thumbnailUrl,
                Utils.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                Utils.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_height));
        return YTSearchHelper.loadThumbnail(targ);
    }

    /**
     * This function download thumbnail image through network synchronously.
     */
    public static boolean
    insertVideoToPlaylist(long      plid,
                          String    ytvid,
                          String    title,
                          String    author,
                          int       playtime,
                          int       volume,
                          String    bookmarks) {
        // Loading thumbnail is done.
        YTSearchHelper.LoadThumbnailReturn tr = loadYtVideoThumbnail(ytvid);
        if (null == tr.bm)
            return false;

        DB.Err err = DB.get().insertVideoToPlaylist(plid,
                                                    ytvid,
                                                    title,
                                                    author,
                                                    playtime,
                                                    ImageUtils.compressBitmap(tr.bm),
                                                    Policy.DEFAULT_VIDEO_VOLUME,
                                                    bookmarks);
        tr.bm.recycle();

        if (DB.Err.NO_ERR != err)
            return false;

        return true;
    }

    public static boolean
    insertVideoToPlaylist(long      plid,
                          String    ytvid,
                          String    title,
                          String    author,
                          int       playtime,
                          int       volume) {
        return insertVideoToPlaylist(plid, ytvid, title, author, playtime, volume, "");
    }
}
