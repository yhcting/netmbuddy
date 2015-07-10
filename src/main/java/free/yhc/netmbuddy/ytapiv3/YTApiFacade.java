/******************************************************************************
 * Copyright (C) 2015
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

import android.net.Uri;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import free.yhc.netmbuddy.core.NetLoader;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Utils;

public class YTApiFacade {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTApiFacade.class);

    public static final int MAX_RESULTS_PER_PAGE = 50;
    public static final int MAX_AVAILABLE_RESULTS_FOR_QUERY = 1000000;

    // Application specific
    // This is NetMBuddy default(temporal) key.
    // Please use your own "CHROME APP" Youtube api key (NOT Android App API key!)
    static final String API_KEY = "AIzaSyD2upYAFQK4WhZ_FjoeJpCsiKgRoN3OKq4";
    // UA String matching above API_KEY
    static final String API_KEY_UASTRING
        = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.168 Chrome/18.0.1025.168 Safari/535.19";


    private static byte[]
    loadUrl(String urlStr) throws NetLoader.LocalException  {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Uri uri = Uri.parse(urlStr);
        NetLoader loader = new NetLoader().open(API_KEY_UASTRING);
        try {
            loader.readHttpData(baos, uri);
        } finally {
            loader.close();
        }

        byte[] data = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException ignored) { }
        return data;
    }

    // =======================================================================
    //
    // Facade APIs
    //
    // =======================================================================

    /**
     * This function uses network. So, this function SHOULD NOT be used at main UI thread.
     */
    public static YTDataAdapter.VideoListResp
    requestVideoList(YTDataAdapter.VideoListReq req) throws YTDataAdapter.YTApiException {
        String requrl;
        switch (req.type) {
        case VID_KEYWORD:
            requrl = YTRespSearch.getVideoSearchRequestUrl("", req.hint, req.pageToken, req.pageSize);
            break;
        case VID_CHANNEL:
            requrl = YTRespSearch.getVideoSearchRequestUrl(req.hint, "", req.pageToken, req.pageSize);
            break;
        default:
            eAssert(false);
            return null;
        }

        byte[] data;
        try {
            data = loadUrl(requrl);
        } catch (NetLoader.LocalException e) {
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.IO_NET);
        }
        YTResp.SearchListResponse slresp = YTRespSearch.parse(data);
        if (null == slresp.items
            || slresp.items.length > slresp.pageInfo.totalResults)
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.BAD_RESPONSE);

        YTDataAdapter.VideoListResp ytvl;
        if (slresp.items.length > 0) {
            String[] ytvids = new String[slresp.items.length];
            for (int i = 0; i < ytvids.length; i++)
                ytvids[i] = slresp.items[i].id.videoId;
            try {
                data = loadUrl(YTRespVideos.getRequestUrl(ytvids));
            } catch (NetLoader.LocalException e) {
                throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.IO_NET);
            }
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
     * @throws YTDataAdapter.YTApiException
     */
    @Nullable
    public static YTDataAdapter.Video
    requestVideoInfo(String ytvid) throws YTDataAdapter.YTApiException {
        byte[] data;
        try {
            data = loadUrl(YTRespVideos.getRequestUrl(new String[] { ytvid }));
        } catch (NetLoader.LocalException e) {
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.IO_NET);
        }
        YTResp.VideoListResponse vlresp = YTRespVideos.parse(data);
        YTDataAdapter.VideoListResp resp = vlresp.makeAdapterData();
        if (0 == resp.vids.length)
            // This is invalid video id
            return null;
        return resp.vids[0];
    }
}
