package free.yhc.netmbuddy.core;

import android.graphics.Bitmap;

import java.util.Date;
import java.util.Objects;

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
    private static final boolean DBG = false;
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

    public static class YTApiException extends java.lang.Exception {
        static final long serialVersionUID = 0; // to make compiler be happy

        private final Err _mErr;

        public YTApiException(Err err) {
            _mErr = err;
        }

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
        public Object nextPageToken = null;
        public Object prevPageToken = null;
        public PageInfo() { }
        public PageInfo(int totalResults, Object nextPageToken, Object prevPageToken) {
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
        public String title        = null;
        public String thumbnailUrl = null;
        public Date   uploadedTime = null;
        public String id           = null; // youtube video id
        public long   playTimeSec  = -1;   // seconds
        public Video() { }
        public Video(String title, String thumbnailUrl, Date uploadedTime, String id, long playTimeSec) {
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
            this.uploadedTime = uploadedTime;
            this.id = id;
            this.playTimeSec = playTimeSec;
        }
    }

    /**
     * Data to request video search
     */
    public static class VideoListReq {
        public enum Type {
            KEYWORD,
            AUTHOR,
            PLAYLIST
        }
        public Type type        = null;
        public String hint      = null;
        public Object pageToken = null;
        public int pageSize     = -1; // # of max items in one page.
        public VideoListReq() { }
        public VideoListReq(Type type, String hint, Object pageToken, int pageSize) {
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
        public Video[]  vids = null;
        public VideoListResp() { }
        public VideoListResp(PageInfo page, Video[] vids) {
            this.page = page;
            this.vids = vids;
        }
    }
}
