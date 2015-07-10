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

import org.json.JSONException;
import org.json.JSONObject;

import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Utils;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

public class YTRespVideos extends YTResp {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTRespVideos.class);

    static String
    getRequestUrl(String[] ytvids) {
        eAssert(ytvids.length > 0
                && ytvids.length <= YTApiFacade.MAX_RESULTS_PER_PAGE);

        String ids = ytvids[0];
        for (int i = 1; i < ytvids.length; i++)
            ids += "," + ytvids[i];
        return "https://www.googleapis.com/youtube/v3/videos"
                + "?key=" + Uri.encode(YTApiFacade.API_KEY)
                + "&id=" + ids
                + "&maxResults=" + ytvids.length
                + "&part=id,snippet,contentDetails"
                + "&fields=items("
                    + "id"
                    + ",snippet(publishedAt,channelId,title,channelTitle,thumbnails/default(url,width,height))"
                    + ",contentDetails/duration"
                + ")";
    }

    YTRespVideos() { }

    static YTResp.VideoListResponse
    parse(byte[] data) throws YTDataAdapter.YTApiException {
        JSONObject jo;
        try {
            jo = new JSONObject(new String(data));
        } catch (JSONException e) {
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.BAD_RESPONSE);
        }
        YTResp.VideoListResponse resp = new VideoListResponse();
        resp.set(jo);
        return resp;
    }

}
