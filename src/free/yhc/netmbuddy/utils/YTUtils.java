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
import free.yhc.netmbuddy.model.DB;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTSearchHelper;

public class YTUtils {
    public static boolean
    verifyYoutubeVideoId(String ytvid) {
        return 11 == ytvid.length();
    }


    /**
     * This function download thumbnail image through network synchronously.
     */
    public static boolean
    insertVideoToPlaylist(long      plid,
                          String    ytvid,
                          String    title,
                          String    description,
                          String    thumbnailUrl,
                          int       playtime,
                          int       volume) {
        YTSearchHelper.LoadThumbnailArg targ = new YTSearchHelper.LoadThumbnailArg(
                null,
                thumbnailUrl,
                Utils.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                Utils.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_height));
        YTSearchHelper.LoadThumbnailReturn tr;
        tr = YTSearchHelper.loadThumbnail(targ);
        if (YTSearchHelper.Err.NO_ERR != tr.err)
            return false;
        // Loading thumbnail is done.

        DB.Err err = DB.get().insertVideoToPlaylist(plid,
                                                    title,
                                                    description,
                                                    ytvid,
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
