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

package free.yhc.netmbuddy.core;

import java.util.Date;

import free.yhc.netmbuddy.utils.Utils;

/**
 * Youtube data adapter between data API and NetMBuddy.
 * NetMBuddy should have dependency on this data structure only.
 * This decouples Youtube data API and NetMBuddy code.
 *
 * Youtube data API integration (in terms of data)
 * (Interfaces can be used anywhere in 'Client World')
 *
 *  +----------+  +----------+
 *  | Clients0 |  | Clients0 | ...
 *  +----+-----+  +----+-----+
 *       |             |
 *       +-----------+-+------ ...
 *                   |
 *          +--------+--------+
 *          |  YTDataAdapter  |
 *          +--------+--------+  Client World
 * ==================|=================
 *          +--------+--------+  YouTube DataAPI World.
 *          |   YTApiFacade   |
 *          +--------+--------+
 *                   |
 *          +--------+--------+
 *          | <'ytapi' world> |
 *          +-----------------+
 *
 */
public class YTDataAdapter {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTDataAdapter.class);

    public enum Err {
        NO_ERR,
        IO_NET,        // Something wrong in network.
        INTERRUPTED,
        NETWORK_UNAVAILABLE,
        INVALID_PARAM, // parameter is invalid
        BAD_RESPONSE,  // response from server is unexpected
        BAD_REQUEST,   // server complains that request is BAD.
        UNKNOWN
    }

    public enum ReqType {
        VID_KEYWORD,
        VID_CHANNEL,
    }


    public static class YTApiException extends java.lang.Exception {
        static final long serialVersionUID = 0; // to make compiler be happy

        private final Err _mErr;

        public YTApiException(Err err) {
            _mErr = err;
        }

        @SuppressWarnings("unused")
        public YTApiException(Err err, String msg) {
            super(msg);
            _mErr = err;
        }

        public Err
        error() {
            return _mErr;
        }
    }
    // =======================================================================
    //
    //
    //
    // =======================================================================
    public static class PageInfo {
        public int    totalResults = -1;
        public String nextPageToken = null;
        public String prevPageToken = null;
        public PageInfo() { }
        @SuppressWarnings("unused")
        public PageInfo(int totalResults, String nextPageToken, String prevPageToken) {
            this.totalResults = totalResults;
            this.nextPageToken = nextPageToken;
            this.prevPageToken = prevPageToken;
        }
    }

    // =======================================================================
    //
    //
    //
    // =======================================================================
    public static class Video {
        public String title = null;
        public String thumbnailUrl = null;
        public Date uploadedTime = null;
        public String id = null; // youtube video id
        public String channelId = null;
        public String channelTitle = null;
        public int playTimeSec  = -1;   // seconds
        public Video() { }
        public Video(String title, String thumbnailUrl, Date uploadedTime, String id,
                     String channelId, String channelTitle, int playTimeSec) {
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
            this.uploadedTime = uploadedTime;
            this.channelId = channelId;
            this.channelTitle = channelTitle;
            this.id = id;
            this.playTimeSec = playTimeSec;
        }
    }

    /**
     * Data to request video search
     */
    public static class VideoListReq {
        public ReqType type = null;
        public String hint = null;
        public String pageToken = null;
        public int pageSize = -1; // # of max items in one page.
        @SuppressWarnings("unused")
        public VideoListReq() { }
        public VideoListReq(ReqType type, String hint, String pageToken, int pageSize) {
            this.type = type;
            this.hint = hint;
            this.pageToken = pageToken;
            this.pageSize = pageSize;
        }
    }

    /**
     * Response for video search request
     */
    public static class VideoListResp {
        public PageInfo page = null;
        public Video[] vids = null;
        public VideoListResp() { }
        @SuppressWarnings("unused")
        public VideoListResp(PageInfo page, Video[] vids) {
            this.page = page;
            this.vids = vids;
        }
    }
}
