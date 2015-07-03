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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Utils;

// To support Youtube Data API v3
class YTResp {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTResp.class);

    static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");

    // =======================================================================
    //
    //
    //
    // =======================================================================
    static abstract class JSONModel {
        abstract void set(JSONObject jo);
    }

    static class Id extends JSONModel {
        String kind = null;
        String videoId = null;
        String channelId = null;
        String playlistId = null;

        @Override
        void
        set(JSONObject jo) {
            kind = getJString(jo, "kind");
            videoId = getJString(jo, "videoId");
            channelId = getJString(jo, "channelId");
            playlistId = getJString(jo, "playlistId");
        }
    }

    static class PageInfo extends JSONModel {
        Integer totalResults = null;
        Integer resultsPerPage = null;

        @Override
        void
        set(JSONObject jo) {
            totalResults = getJInt(jo, "totalResults");
            resultsPerPage = getJInt(jo, "resultsPerPage");
        }
    }

    static class Thumbnail extends JSONModel {
        String url = null;
        Integer width = null;
        Integer height = null;

        @Override
        void
        set(JSONObject jo) {
            url = getJString(jo, "url");
            width = getJInt(jo, "width");
            height = getJInt(jo, "height");
        }
    }

    static class Thumbnails extends JSONModel {
        Thumbnail default_ = null;
        Thumbnail medium = null;
        Thumbnail high = null;

        @Override
        void
        set(JSONObject jo) {
            default_ = getJObject(jo, "default", Thumbnail.class);
            medium = getJObject(jo, "medium", Thumbnail.class);
            high = getJObject(jo, "high", Thumbnail.class);
        }
    }

    static class Snippet extends JSONModel {
        String publishedAt = null;
        String channelId = null;
        String title = null;
        String description = null;
        Thumbnails thumbnails = null;
        String channelTitle = null;
        String[] tags = null;
        String categoryId = null;
        String liveBroadcastContent = null;

        @Override
        void
        set(JSONObject jo) {
            publishedAt = getJString(jo, "publishedAt");
            channelId = getJString(jo, "channelId");
            title = getJString(jo, "title");
            description = getJString(jo, "description");
            thumbnails = getJObject(jo, "thumbnails", Thumbnails.class);
            channelTitle = getJString(jo, "channelTitle");
            tags = getJStrings(jo, "tags");
            categoryId = getJString(jo, "categoryId");
            liveBroadcastContent = getJString(jo, "liveBroadcastContent");
        }
    }

    static class RegionRestriction extends JSONModel {
        String[] allowed = null;
        String[] blocked = null;

        @Override
        void
        set(JSONObject jo) {
            allowed = getJStrings(jo, "allowed");
            blocked = getJStrings(jo, "blocked");
        }
    }

    static class ContentRating extends JSONModel {
        String acbRating = null;
        String agcomRating = null;
        String anatelRating = null;
        String bbfcRating = null;
        String bfvcRating = null;
        String bmukkRating = null;
        String catvRating = null;
        String catvfrRating = null;
        String cbfcRating = null;
        String cccRating = null;
        String cceRating = null;
        String chfilmRating = null;
        String chvrsRating = null;
        String cicfRating = null;
        String cnaRating = null;
        String csaRating = null;
        String cscfRating = null;
        String czfilmRating = null;
        String djctqRating = null;
        String[] djctqRatingReasons = null;
        String eefilmRating = null;
        String egfilmRating = null;
        String eirinRating = null;
        String fcbmRating = null;
        String fcoRating = null;
        String fmocRating = null;
        String fpbRating = null;
        String fskRating = null;
        String grfilmRating = null;
        String icaaRating = null;
        String ifcoRating = null;
        String ilfilmRating = null;
        String incaaRating = null;
        String kfcbRating = null;
        String kijkwijzerRating = null;
        String kmrbRating = null;
        String lsfRating = null;
        String mccaaRating = null;
        String mccypRating = null;
        String mdaRating = null;
        String medietilsynetRating = null;
        String mekuRating = null;
        String mibacRating = null;
        String mocRating = null;
        String moctwRating = null;
        String mpaaRating = null;
        String mtrcbRating = null;
        String nbcRating = null;
        String nbcplRating = null;
        String nfrcRating = null;
        String nfvcbRating = null;
        String nkclvRating = null;
        String oflcRating = null;
        String pefilmRating = null;
        String rcnofRating = null;
        String resorteviolenciaRating = null;
        String rtcRating = null;
        String rteRating = null;
        String russiaRating = null;
        String skfilmRating = null;
        String smaisRating = null;
        String smsaRating = null;
        String tvpgRating = null;
        String ytRating = null;

        @Override
        void
        set(JSONObject jo) {
            acbRating = getJString(jo, "acbRating");
            agcomRating = getJString(jo, "agcomRating");
            anatelRating = getJString(jo, "anatelRating");
            bbfcRating = getJString(jo, "bbfcRating");
            bfvcRating = getJString(jo, "bfvcRating");
            bmukkRating = getJString(jo, "bmukkRating");
            catvRating = getJString(jo, "catvRating");
            catvfrRating = getJString(jo, "catvfrRating");
            cbfcRating = getJString(jo, "cbfcRating");
            cccRating = getJString(jo, "cccRating");
            cceRating = getJString(jo, "cceRating");
            chfilmRating = getJString(jo, "chfilmRating");
            chvrsRating = getJString(jo, "chvrsRating");
            cicfRating = getJString(jo, "cicfRating");
            cnaRating = getJString(jo, "cnaRating");
            csaRating = getJString(jo, "csaRating");
            cscfRating = getJString(jo, "cscfRating");
            czfilmRating = getJString(jo, "czfilmRating");
            djctqRating = getJString(jo, "djctqRating");
            djctqRatingReasons = getJStrings(jo, "djctqRatingReasons");
            eefilmRating = getJString(jo, "eefilmRating");
            egfilmRating = getJString(jo, "egfilmRating");
            eirinRating = getJString(jo, "eirinRating");
            fcbmRating = getJString(jo, "fcbmRating");
            fcoRating = getJString(jo, "fcoRating");
            fmocRating = getJString(jo, "fmocRating");
            fpbRating = getJString(jo, "fpbRating");
            fskRating = getJString(jo, "fskRating");
            grfilmRating = getJString(jo, "grfilmRating");
            icaaRating = getJString(jo, "icaaRating");
            ifcoRating = getJString(jo, "ifcoRating");
            ilfilmRating = getJString(jo, "ilfilmRating");
            incaaRating = getJString(jo, "incaaRating");
            kfcbRating = getJString(jo, "kfcbRating");
            kijkwijzerRating = getJString(jo, "kijkwijzerRating");
            kmrbRating = getJString(jo, "kmrbRating");
            lsfRating = getJString(jo, "lsfRating");
            mccaaRating = getJString(jo, "mccaaRating");
            mccypRating = getJString(jo, "mccypRating");
            mdaRating = getJString(jo, "mdaRating");
            medietilsynetRating = getJString(jo, "medietilsynetRating");
            mekuRating = getJString(jo, "mekuRating");
            mibacRating = getJString(jo, "mibacRating");
            mocRating = getJString(jo, "mocRating");
            moctwRating = getJString(jo, "moctwRating");
            mpaaRating = getJString(jo, "mpaaRating");
            mtrcbRating = getJString(jo, "mtrcbRating");
            nbcRating = getJString(jo, "nbcRating");
            nbcplRating = getJString(jo, "nbcplRating");
            nfrcRating = getJString(jo, "nfrcRating");
            nfvcbRating = getJString(jo, "nfvcbRating");
            nkclvRating = getJString(jo, "nkclvRating");
            oflcRating = getJString(jo, "oflcRating");
            pefilmRating = getJString(jo, "pefilmRating");
            rcnofRating = getJString(jo, "rcnofRating");
            resorteviolenciaRating = getJString(jo, "resorteviolenciaRating");
            rtcRating = getJString(jo, "rtcRating");
            rteRating = getJString(jo, "rteRating");
            russiaRating = getJString(jo, "russiaRating");
            skfilmRating = getJString(jo, "skfilmRating");
            smaisRating = getJString(jo, "smaisRating");
            smsaRating = getJString(jo, "smsaRating");
            tvpgRating = getJString(jo, "tvpgRating");
            ytRating = getJString(jo, "ytRating");
        }
    }

    static class Status extends JSONModel {
        String uploadStatus = null;
        String failureReason = null;
        String rejectionReason = null;
        String privacyStatus = null;
        String publishAt = null;
        String license = null;
        String embeddable = null;
        String publicStatsViewable = null;

        @Override
        void
        set(JSONObject jo) {
            uploadStatus = getJString(jo, "uploadStatus");
            failureReason = getJString(jo, "failureReason");
            rejectionReason = getJString(jo, "rejectionReason");
            privacyStatus = getJString(jo, "privacyStatus");
            publishAt = getJString(jo, "publishAt");
            license = getJString(jo, "license");
            embeddable = getJString(jo, "embeddable");
            publicStatsViewable = getJString(jo, "publicStatsViewable");

        }
    }

    static class Statistics extends JSONModel {
        Long viewCount = null;
        Long likeCount = null;
        Long dislikeCount = null;
        Long favoriteCount = null;
        Long commentCount = null;

        @Override
        void
        set(JSONObject jo) {
            viewCount = getJLong(jo, "viewCount");
            likeCount = getJLong(jo, "likeCount");
            dislikeCount = getJLong(jo, "dislikeCount");
            favoriteCount = getJLong(jo, "favoriteCount");
            commentCount = getJLong(jo, "commentCount");
        }
    }

    static class Player extends JSONModel {
        String embedHtml = null;

        @Override
        void
        set(JSONObject jo) {
            embedHtml = getJString(jo, "embedHtml");
        }
    }

    static class TopicDetails extends JSONModel {
        String[] topicIds = null;
        String[] relevantTopicIds = null;

        @Override
        void
        set(JSONObject jo) {
            topicIds = getJStrings(jo, "topicIds");
            relevantTopicIds = getJStrings(jo, "relevantTopicIds");
        }
    }

    static class Location extends JSONModel {
        Double latitude = null;
        Double longitude = null;
        Double altitude = null;

        @Override
        void
        set(JSONObject jo) {
            latitude = getJDouble(jo, "latitude");
            longitude = getJDouble(jo, "longitude");
            altitude = getJDouble(jo, "altitude");
        }
    }

    static class RecordingDetails extends JSONModel {
        String locationDescription = null;
        Location location = null;
        String recordingDate = null;

        @Override
        void
        set(JSONObject jo) {
            locationDescription = getJString(jo, "locationDescription");
            location = getJObject(jo, "location", Location.class);
            recordingDate = getJString(jo, "recordingDate");
        }
    }

    static class VideoStream extends JSONModel {
        Integer widthPixels = null;
        Integer heightPixels = null;
        Double frameRateFps = null;
        Double aspectRatio = null;
        String codec = null;
        Long bitrateBps = null;
        String rotation = null;
        String vendor = null;

        @Override
        void
        set(JSONObject jo) {
            widthPixels = getJInt(jo, "widthPixels");
            heightPixels = getJInt(jo, "heightPixels");
            frameRateFps = getJDouble(jo, "frameRateFps");
            aspectRatio = getJDouble(jo, "aspectRatio");
            codec = getJString(jo, "codec");
            bitrateBps = getJLong(jo, "bitrateBps");
            rotation = getJString(jo, "rotation");
            vendor = getJString(jo, "vendor");
        }
    }

    static class AudioStream extends JSONModel {
        Integer channelCount = null;
        String codec = null;
        Long bitrateBps = null;
        String vendor = null;

        @Override
        void
        set(JSONObject jo) {
            channelCount = getJInt(jo, "channelCount");
            codec = getJString(jo, "codec");
            bitrateBps = getJLong(jo, "bitrateBps");
            vendor = getJString(jo, "vendor");
        }
    }

    static class FileDetails extends JSONModel {
        String fileName = null;
        Long fileSize = null;
        String fileType = null;
        String container = null;
        VideoStream[] videoStreams = null;
        AudioStream[] audioStreams = null;
        Long durationMs = null;
        Long bitrateBps = null;
        Location recordingLocation = null;
        String creationTime = null;

        @Override
        void
        set(JSONObject jo) {
            fileName = getJString(jo, "fileName");
            fileSize = getJLong(jo, "fileSize");
            fileType = getJString(jo, "fileType");
            container = getJString(jo, "container");
            videoStreams = getJObjects(jo, "videoStreams", VideoStream.class);
            audioStreams = getJObjects(jo, "audioStreams", AudioStream.class);
            durationMs = getJLong(jo, "durationMs");
            bitrateBps = getJLong(jo, "bitrateBps");
            recordingLocation = getJObject(jo, "recordingLocation", Location.class);
            creationTime = getJString(jo, "creationTime");
        }
    }

    static class ProcessingProgress extends JSONModel {
        Long partsTotal = null;
        Long partsProcessed = null;
        Long timeLeftMs = null;

        @Override
        void
        set(JSONObject jo) {
            partsTotal = getJLong(jo, "partsTotal");
            partsProcessed = getJLong(jo, "partsProcessed");
            timeLeftMs = getJLong(jo, "timeLeftMs");
        }
    }


    static class ProcessingDetails extends JSONModel {
        String processingStatus = null;
        ProcessingProgress processingProgress = null;
        String processingFailureReason = null;
        String fileDetailsAvailability = null;
        String processingIssuesAvailability = null;
        String tagSuggestionsAvailability = null;
        String editorSuggestionsAvailability = null;
        String thumbnailsAvailability = null;

        @Override
        void
        set(JSONObject jo) {
            processingStatus = getJString(jo, "processingStatus");
            processingProgress = getJObject(jo, "processingProgress", ProcessingProgress.class);
            processingFailureReason = getJString(jo, "processingFailureReason");
            fileDetailsAvailability = getJString(jo, "fileDetailsAvailability");
            processingIssuesAvailability = getJString(jo, "processingIssuesAvailability");
            tagSuggestionsAvailability = getJString(jo, "tagSuggestionsAvailability");
            editorSuggestionsAvailability = getJString(jo, "editorSuggestionsAvailability");
            thumbnailsAvailability = getJString(jo, "thumbnailsAvailability");
        }
    }

    static class TagSuggestion extends JSONModel {
        String tag = null;
        String[] categoryRestricts = null;

        @Override
        void
        set(JSONObject jo) {
            tag = getJString(jo, "tag");
            categoryRestricts = getJStrings(jo, "categoryRestricts");
        }
    }

    static class Suggestions extends JSONModel {
        String[] processingErrors = null;
        String[] processingWarnings = null;
        String[] processingHints = null;
        TagSuggestion[] tagSuggestions = null;
        String[] editorSuggestions = null;

        @Override
        void
        set(JSONObject jo) {
            processingErrors = getJStrings(jo, "processingErrors");
            processingWarnings = getJStrings(jo, "processingWarnings");
            processingHints = getJStrings(jo, "processingHints");
            tagSuggestions = getJObjects(jo, "tagSuggestions", TagSuggestion.class);
            editorSuggestions = getJStrings(jo, "editorSuggestions");
        }
    }


    static class LiveStreamingDetails extends JSONModel {
        String actualStartTime = null;
        String actualEndTime = null;
        String scheduledStartTime = null;
        String scheduledEndTime = null;
        Long concurrentViewers = null;

        @Override
        void
        set(JSONObject jo) {
            actualStartTime = getJString(jo, "actualStartTime");
            actualEndTime = getJString(jo, "actualEndTime");
            scheduledStartTime = getJString(jo, "scheduledStartTime");
            scheduledEndTime = getJString(jo, "scheduledEndTime");
            concurrentViewers = getJLong(jo, "concurrentViewers");
        }
    }

    static class ContentDetails extends JSONModel {
        String duration = null;
        String dimension = null;
        String definition = null;
        String caption = null;
        Boolean licensedContent = null;
        RegionRestriction regionRestriction = null;
        ContentRating contentRating = null;

        @Override
        void
        set(JSONObject jo) {
            duration = getJString(jo, "duration");
            dimension = getJString(jo, "dimension");
            definition = getJString(jo, "definition");
            caption = getJString(jo, "caption");
            licensedContent = getJBoolean(jo, "licensedContent");
            regionRestriction = getJObject(jo, "regionRestriction", RegionRestriction.class);
            contentRating = getJObject(jo, "contentRating", ContentRating.class);
        }
    }

    static class VideoRes extends JSONModel {
        static final String KIND = "youtube#video";
        String kind = null;
        String etag = null;
        String id = null; // youtube video id
        Snippet snippet = null;
        ContentDetails contentDetails = null;
        Status status = null;
        Statistics statistics = null;
        Player player = null;
        TopicDetails topicDetails = null;
        RecordingDetails recordingDetails = null;
        FileDetails fileDetails = null;
        ProcessingDetails processingDetails = null;
        Suggestions suggestions = null;
        LiveStreamingDetails liveStreamingDetails = null;

        @Override
        void
        set(JSONObject jo) {
            kind = getJString(jo, "kind");
            etag = getJString(jo, "etag");
            id = getJString(jo, "id");
            snippet = getJObject(jo, "snippet", Snippet.class);
            contentDetails = getJObject(jo, "contentDetails", ContentDetails.class);
            status = getJObject(jo, "status", Status.class);
            statistics = getJObject(jo, "statistics", Statistics.class);
            player = getJObject(jo, "player", Player.class);
            topicDetails = getJObject(jo, "topicDetails", TopicDetails.class);
            recordingDetails = getJObject(jo, "recordingDetails", RecordingDetails.class);
            fileDetails = getJObject(jo, "fileDetails", FileDetails.class);
            processingDetails = getJObject(jo, "processingDetails", ProcessingDetails.class);
            suggestions = getJObject(jo, "suggestions", Suggestions.class);
            liveStreamingDetails = getJObject(jo, "liveStreamingDetails", LiveStreamingDetails.class);
        }

        /**
         * Generate corresponding data structure of facade client side.
         */
        YTDataAdapter.Video
        makeAdapterData() {
            YTDataAdapter.Video v = new YTDataAdapter.Video();
            v.id = id;
            if (null != snippet) {
                v.title = snippet.title;
                if (null != snippet.thumbnails
                    && null != snippet.thumbnails.default_) {
                    v.thumbnailUrl = snippet.thumbnails.default_.url;
                }
                try {
                    v.uploadedTime = SDF.parse(snippet.publishedAt);
                } catch (ParseException ignore) { }
            }
            if (null != contentDetails)
                v.playTimeSec = parseYTDuration(contentDetails.duration);
            return v;
        }
    }

    static class SearchRes extends JSONModel {
        static final String KIND = "youtube#searchResult";
        String kind = null;
        String etag = null;
        Id id = null;
        Snippet snippet = null;

        @Override
        void
        set(JSONObject jo) {
            kind = getJString(jo, "kind");
            etag = getJString(jo, "etag");
            id = getJObject(jo, "id", Id.class);
            snippet = getJObject(jo, "snippet", Snippet.class);
        }

        YTDataAdapter.Video
        makeAdapterData() {
            YTDataAdapter.Video v = new YTDataAdapter.Video();
            v.id = id.videoId;
            v.title = snippet.title;
            v.thumbnailUrl = snippet.thumbnails.default_.url;
            try {
                v.uploadedTime = SDF.parse(snippet.publishedAt);
            } catch (ParseException ignore) { }
            return v;
        }
    }

    static class SearchListResponse extends JSONModel {
        static final String KIND = "youtube#searchListResponse";
        String kind = null;
        String etag = null;
        String nextPageToken = null;
        String prevPageToken = null;
        PageInfo pageInfo = null;
        SearchRes[] items = null;

        @Override
        void
        set(JSONObject jo) {
            kind = getJString(jo, "kind");
            etag = getJString(jo, "etag");
            nextPageToken = getJString(jo, "nextPageToken");
            prevPageToken = getJString(jo, "prevPageToken");
            pageInfo = getJObject(jo, "pageInfo", PageInfo.class);
            items = getJObjects(jo, "items", SearchRes.class);
        }

        /**
         * Generate corresponding data structure of facade client side.
         */
        YTDataAdapter.VideoListResp
        makeAdapterData() {
            YTDataAdapter.VideoListResp r = new YTDataAdapter.VideoListResp();
            r.page = new YTDataAdapter.PageInfo();
            if (null != pageInfo)
                r.page.totalResults = pageInfo.totalResults;
            r.page.nextPageToken = nextPageToken;
            r.page.prevPageToken = prevPageToken;
            r.vids = new YTDataAdapter.Video[items.length];
            for (int i = 0; i < r.vids.length; i++)
                r.vids[i] = items[i].makeAdapterData();
            return r;
        }
    }

    static class VideoListResponse extends JSONModel {
        static final String KIND = "youtube#videoListResponse";
        String kind = null;
        String etag = null;
        String nextPageToken = null;
        String prevPageToken = null;
        PageInfo pageInfo = null;
        VideoRes[] items = null;

        @Override
        void
        set(JSONObject jo) {
            kind = getJString(jo, "kind");
            etag = getJString(jo, "etag");
            nextPageToken = getJString(jo, "nextPageToken");
            prevPageToken = getJString(jo, "prevPageToken");
            pageInfo = getJObject(jo, "pageInfo", PageInfo.class);
            items = getJObjects(jo, "items", VideoRes.class);
        }

        /**
         * Generate corresponding data structure of facade client side.
         */
        YTDataAdapter.VideoListResp
        makeAdapterData() {
            YTDataAdapter.VideoListResp r = new YTDataAdapter.VideoListResp();
            r.page = new YTDataAdapter.PageInfo();
            if (null != pageInfo)
                r.page.totalResults = pageInfo.totalResults;
            r.page.nextPageToken = nextPageToken;
            r.page.prevPageToken = prevPageToken;
            r.vids = new YTDataAdapter.Video[items.length];
            for (int i = 0; i < r.vids.length; i++)
                r.vids[i] = items[i].makeAdapterData();
            return r;
        }
    }

    // =======================================================================
    //
    //
    //
    // =======================================================================

    // return seconds
    private static long
    parseYTDuration(String durstr) {
        String s = null;
        int Y = 0, M = 0, W = 0, D = 0, H = 0, m = 0, S = 0;
        Pattern p = Pattern.compile(
              "^P"
            + "(?:([0-9]+)Y)?"
            + "(?:([0-9]+)M)?"
            + "(?:([0-9]+)W)?"
            + "(?:([0-9]+)D)?"
            + "T"
            + "(?:([0-9]+)H)?"
            + "(?:([0-9]+)M)?"
            + "(?:([0-9]+)S)"
            + "$");
        Matcher mr = p.matcher(durstr);
        if (!mr.matches())
            return -1; // unknown format

        if (null != (s = mr.group(1)))
            Y = Integer.parseInt(s);
        if (null != (s = mr.group(2)))
            M = Integer.parseInt(s);
        if (null != (s = mr.group(3)))
            W = Integer.parseInt(s);
        if (null != (s = mr.group(4)))
            D = Integer.parseInt(s);
        if (null != (s = mr.group(5)))
            H = Integer.parseInt(s);
        if (null != (s = mr.group(6)))
            m = Integer.parseInt(s);
        if (null != (s = mr.group(7)))
            S = Integer.parseInt(s);

        final int unitm = 60; // 1 min == 60 sec
        final int unitH = 60 * unitm;
        final int unitD = 24 * unitH;
        if (0 < W
                || 0 < M
                || 0 < Y)
            return -1; // too long
        return D * unitD + H * unitH + m * unitm + S;
    }

    // =======================================================================
    //
    //
    //
    // =======================================================================
    private static JSONObject
    getJJObject(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getJSONObject(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static JSONArray
    getJJArray(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getJSONArray(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static String
    getJString(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getString(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static Integer
    getJInt(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getInt(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static Long
    getJLong(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getLong(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static Boolean
    getJBoolean(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getBoolean(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static Double
    getJDouble(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getDouble(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static String[]
    getJStrings(JSONObject jo, String key) {
        JSONArray ja = getJJArray(jo, key);
        if (null == ja)
            return null;
        String[] r = new String[ja.length()];
        try {
            for (int i = 0; i < r.length; i++)
                r[i] = ja.getString(i);
            return r;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static <K extends JSONModel> K
    getJObject(JSONObject jo, String key, Class<K> cls) {
        if (null == jo)
            return null;
        try {
            JSONObject o = jo.getJSONObject(key);
            K r = null;
            try {
                r = cls.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            r.set(o);
            return r;
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static <K extends JSONModel> K[]
    getJObjects(JSONObject jo, String key, Class<K> cls) {
        JSONArray ja = getJJArray(jo, key);
        if (null == ja)
            return null;
        K[] r = (K[])Array.newInstance(cls, ja.length());
        try {
            for (int i = 0; i < r.length; i++) {
                try {
                    r[i] = cls.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                r[i].set(ja.getJSONObject(i));
            }
            return r;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}
