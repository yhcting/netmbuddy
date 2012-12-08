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

package free.yhc.netmbuddy.utils;

import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTHacker;
import free.yhc.netmbuddy.model.YTSearchHelper;

public class YTUtils {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTUtils.class);

    public static boolean
    verifyYoutubeVideoId(String ytvid) {
        return 11 == ytvid.length();
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
                          int       volume) {
        // Loading thumbnail is done.
        YTSearchHelper.LoadThumbnailReturn tr = loadYtVideoThumbnail(ytvid);
        DB.Err err = DB.get().insertVideoToPlaylist(plid,
                                                    ytvid,
                                                    title,
                                                    author,
                                                    playtime,
                                                    ImageUtils.compressBitmap(tr.bm),
                                                    Policy.DEFAULT_VIDEO_VOLUME);
        if (null != tr.bm)
            tr.bm.recycle();

        if (DB.Err.NO_ERR != err)
            return false;

        return true;
    }

}
