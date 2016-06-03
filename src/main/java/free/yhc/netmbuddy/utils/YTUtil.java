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

package free.yhc.netmbuddy.utils;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.exception.BadResponseException;
import free.yhc.baselib.net.NetReadTask;
import free.yhc.abaselib.util.ImgUtil;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.task.YTHackTask;
import free.yhc.netmbuddy.task.YTThumbnailTask;
import free.yhc.netmbuddy.ytapiv3.YTApiFacade;

public class YTUtil {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTUtil.class, Logger.LOGLV_DEFAULT);

    /**
     * @throws InterruptedException
     * @throws IOException (MalformedURLException ...)
     */
    @NonNull
    public static byte[]
    loadYtDataUrl(String urlstr)
            throws InterruptedException, IOException {
        URL url = new URL(urlstr);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(4096)){
            NetReadTask.Builder<NetReadTask.Builder> nrb
                    = new NetReadTask.Builder<>(Util.createNetConn(url), baos);
            try {
                nrb.create().startSync();
            } catch (IOException | InterruptedException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return baos.toByteArray();
        }
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
        } catch (InterruptedException | IOException | BadResponseException e) {
            return null;
        }
    }

    @Nullable
    public static Bitmap
    getYtThumbnail(String ytvid) {
        try {
            YTThumbnailTask t = YTThumbnailTask.create(
                    new URL(YTHackTask.getYtVideoThumbnailUrl(ytvid)),
                    AppEnv.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                    AppEnv.getAppContext().getResources().getDimensionPixelSize(R.dimen.thumbnail_height),
                    null);
            try {
                return t.startSync();
            } catch (InterruptedException | IOException | BadResponseException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException | BadResponseException | IOException e) {
            return null;
        }
    }

    public static boolean
    fillYtDataAndThumbnail(DMVideo v) {
        P.bug(verifyYoutubeVideoId(v.ytvid));
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
            // see comments of 'YTHackTask.getYtVideoThumbnailUrl()' for details.
            Bitmap bm = getYtThumbnail(v.ytvid);
            if (null == bm)
                return false;
            v.setThumbnail(ImgUtil.compressToJpeg(bm));
            bm.recycle();
        }
        return true;
    }
}
