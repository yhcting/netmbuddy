/******************************************************************************
 * Copyright (C) 2015, 2016
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

package free.yhc.netmbuddy.ytapiv3;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;

import free.yhc.baselib.Logger;
import free.yhc.baselib.exception.BadResponseException;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.YTUtil;

public class YTApiFacade {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTApiFacade.class, Logger.LOGLV_DEFAULT);

    public static final int MAX_RESULTS_PER_PAGE = 50;
    public static final int MAX_AVAILABLE_RESULTS_FOR_QUERY = 1000000;

    // Application specific
    // This is NetMBuddy default(temporal) key.
    // Please use your own "CHROME APP" Youtube api key (NOT Android App API key!)
    static final String API_KEY = "AIzaSyD2upYAFQK4WhZ_FjoeJpCsiKgRoN3OKq4";

    // =======================================================================
    //
    // Facade APIs
    // These APIs communicates with Youtube server.
    // So, these functions SHOULD NOT be used at main UI thread.,
    //
    // =======================================================================
    public static boolean
    isValidYtVideo(String ytvid)
            throws InterruptedException, IOException, BadResponseException {
        return null != requestVideoInfo(ytvid);
    }

    @NonNull
    public static YTDataAdapter.VideoListResp
    requestVideoList(YTDataAdapter.VideoListReq req)
            throws InterruptedException, IOException, BadResponseException {
        String requrl = "";
        switch (req.type) {
        case VID_KEYWORD:
            requrl = YTRespSearch.getVideoSearchRequestUrl("", req.hint, req.pageToken, req.pageSize);
            break;
        case VID_CHANNEL:
            requrl = YTRespSearch.getVideoSearchRequestUrl(req.hint, "", req.pageToken, req.pageSize);
            break;
        default:
            P.bug(false);
        }

        YTResp.SearchListResponse slresp = YTRespSearch.parse(YTUtil.loadYtDataUrl(requrl));
        if (null == slresp.items
            || slresp.items.length > slresp.pageInfo.totalResults)
            throw new BadResponseException();

        YTDataAdapter.VideoListResp ytvl;
        if (slresp.items.length > 0) {
            String[] ytvids = new String[slresp.items.length];
            for (int i = 0; i < ytvids.length; i++)
                ytvids[i] = slresp.items[i].id.videoId;
            byte[] data = YTUtil.loadYtDataUrl(YTRespVideos.getRequestUrl(ytvids));
            YTResp.VideoListResponse vlresp = YTRespVideos.parse(data);
            ytvl = vlresp.makeAdapterData();
        } else {
            ytvl = new YTDataAdapter.VideoListResp();
            ytvl.vids = new YTDataAdapter.Video[0];
        }
        ytvl.page.totalResults = slresp.pageInfo.totalResults;
        ytvl.page.nextPageToken = slresp.nextPageToken;
        ytvl.page.prevPageToken = slresp.prevPageToken;
        return ytvl;
    }

    /**
     *
     * @param ytvid youtube video id
     * @return null if youtube video id is invalid(unavailable) one.
     * @throws InterruptedException
     * @throws IOException (MalformedURLException...)
     */
    @Nullable
    public static YTDataAdapter.Video
    requestVideoInfo(String ytvid)
            throws InterruptedException, IOException, BadResponseException {
        byte[] data;
        try {
            data = YTUtil.loadYtDataUrl(YTRespVideos.getRequestUrl(new String[]{ytvid}));
        } catch (MalformedURLException e) {
            // This is Unexpected!
            throw new AssertionError();
        }
        YTResp.VideoListResponse vlresp = YTRespVideos.parse(data);
        YTDataAdapter.VideoListResp resp = vlresp.makeAdapterData();
        if (null == resp.vids
            || 0 == resp.vids.length)
            // This is invalid video id
            return null;
        return resp.vids[0];
    }
}
