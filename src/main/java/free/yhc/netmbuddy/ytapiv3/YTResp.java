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

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static free.yhc.netmbuddy.utils.JsonUtils.jGetString;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetStrings;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetInt;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetLong;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetDouble;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetObject;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetObjects;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetBoolean;

import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Utils;
import free.yhc.netmbuddy.utils.JsonUtils.JsonModel;

// To support Youtube Data API v3
class YTResp {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTResp.class);

    static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'", Locale.US);


    // =======================================================================
    //
    //
    //
    // =======================================================================
    private static void
    setSnippetData(YTDataAdapter.Video v, Snippet snippet) {
        if (null != snippet) {
            v.title = snippet.title;
            v.channelId = snippet.channelId;
            v.channelTitle = snippet.channelTitle;
            if (null != snippet.thumbnails
                && null != snippet.thumbnails.default_) {
                v.thumbnailUrl = snippet.thumbnails.default_.url;
            }
            try {
                v.uploadedTime = SDF.parse(snippet.publishedAt);
            } catch (ParseException ignore) { }
        }
    }

    /**
     *
     * @return seconds >=0. <0 if fails
     */
    @SuppressWarnings("ConstantConditions")
    private static long
    parseYTDuration(String durstr) {
        int Y = 0, M = 0, W = 0, D = 0, H = 0, m = 0, S = 0;
        if (null == durstr)
            return -1;

        Pattern p = Pattern.compile(
            "^P"
            + "(?:([0-9]+)Y)?"
            + "(?:([0-9]+)M)?"
            + "(?:([0-9]+)W)?"
            + "(?:([0-9]+)D)?"
            + "T"
            + "(?:([0-9]+)H)?"
            + "(?:([0-9]+)M)?"
            + "(?:([0-9]+)S)?"
            + "$");

        Matcher mr = p.matcher(durstr);
        if (!mr.matches())
            return -1; // unknown format

        String s;
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
    // All decendents of 'JsonModel' is set as 'public' to be used via reflection.
    //
    // =======================================================================
    public static class Id extends JsonModel {
        String kind = null;
        String videoId = null;
        String channelId = null;
        String playlistId = null;

        @Override
        public void
        set(JSONObject jo) {
            kind = jGetString(jo, "kind");
            videoId = jGetString(jo, "videoId");
            channelId = jGetString(jo, "channelId");
            playlistId = jGetString(jo, "playlistId");
        }
    }

    public static class PageInfo extends JsonModel {
        Integer totalResults = null;
        Integer resultsPerPage = null;

        @Override
        public void
        set(JSONObject jo) {
            totalResults = jGetInt(jo, "totalResults");
            resultsPerPage = jGetInt(jo, "resultsPerPage");
        }
    }

    public static class Thumbnail extends JsonModel {
        String url = null;
        Integer width = null;
        Integer height = null;

        @Override
        public void
        set(JSONObject jo) {
            url = jGetString(jo, "url");
            width = jGetInt(jo, "width");
            height = jGetInt(jo, "height");
        }
    }

    public static class Thumbnails extends JsonModel {
        Thumbnail default_ = null;
        Thumbnail medium = null;
        Thumbnail high = null;

        @Override
        public void
        set(JSONObject jo) {
            default_ = jGetObject(jo, "default", Thumbnail.class);
            medium = jGetObject(jo, "medium", Thumbnail.class);
            high = jGetObject(jo, "high", Thumbnail.class);
        }
    }

    public static class Snippet extends JsonModel {
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
        public void
        set(JSONObject jo) {
            publishedAt = jGetString(jo, "publishedAt");
            channelId = jGetString(jo, "channelId");
            title = jGetString(jo, "title");
            description = jGetString(jo, "description");
            thumbnails = jGetObject(jo, "thumbnails", Thumbnails.class);
            channelTitle = jGetString(jo, "channelTitle");
            tags = jGetStrings(jo, "tags");
            categoryId = jGetString(jo, "categoryId");
            liveBroadcastContent = jGetString(jo, "liveBroadcastContent");
        }
    }

    public static class RegionRestriction extends JsonModel {
        String[] allowed = null;
        String[] blocked = null;

        @Override
        public void
        set(JSONObject jo) {
            allowed = jGetStrings(jo, "allowed");
            blocked = jGetStrings(jo, "blocked");
        }
    }

    public static class ContentRating extends JsonModel {
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
        public void
        set(JSONObject jo) {
            acbRating = jGetString(jo, "acbRating");
            agcomRating = jGetString(jo, "agcomRating");
            anatelRating = jGetString(jo, "anatelRating");
            bbfcRating = jGetString(jo, "bbfcRating");
            bfvcRating = jGetString(jo, "bfvcRating");
            bmukkRating = jGetString(jo, "bmukkRating");
            catvRating = jGetString(jo, "catvRating");
            catvfrRating = jGetString(jo, "catvfrRating");
            cbfcRating = jGetString(jo, "cbfcRating");
            cccRating = jGetString(jo, "cccRating");
            cceRating = jGetString(jo, "cceRating");
            chfilmRating = jGetString(jo, "chfilmRating");
            chvrsRating = jGetString(jo, "chvrsRating");
            cicfRating = jGetString(jo, "cicfRating");
            cnaRating = jGetString(jo, "cnaRating");
            csaRating = jGetString(jo, "csaRating");
            cscfRating = jGetString(jo, "cscfRating");
            czfilmRating = jGetString(jo, "czfilmRating");
            djctqRating = jGetString(jo, "djctqRating");
            djctqRatingReasons = jGetStrings(jo, "djctqRatingReasons");
            eefilmRating = jGetString(jo, "eefilmRating");
            egfilmRating = jGetString(jo, "egfilmRating");
            eirinRating = jGetString(jo, "eirinRating");
            fcbmRating = jGetString(jo, "fcbmRating");
            fcoRating = jGetString(jo, "fcoRating");
            fmocRating = jGetString(jo, "fmocRating");
            fpbRating = jGetString(jo, "fpbRating");
            fskRating = jGetString(jo, "fskRating");
            grfilmRating = jGetString(jo, "grfilmRating");
            icaaRating = jGetString(jo, "icaaRating");
            ifcoRating = jGetString(jo, "ifcoRating");
            ilfilmRating = jGetString(jo, "ilfilmRating");
            incaaRating = jGetString(jo, "incaaRating");
            kfcbRating = jGetString(jo, "kfcbRating");
            kijkwijzerRating = jGetString(jo, "kijkwijzerRating");
            kmrbRating = jGetString(jo, "kmrbRating");
            lsfRating = jGetString(jo, "lsfRating");
            mccaaRating = jGetString(jo, "mccaaRating");
            mccypRating = jGetString(jo, "mccypRating");
            mdaRating = jGetString(jo, "mdaRating");
            medietilsynetRating = jGetString(jo, "medietilsynetRating");
            mekuRating = jGetString(jo, "mekuRating");
            mibacRating = jGetString(jo, "mibacRating");
            mocRating = jGetString(jo, "mocRating");
            moctwRating = jGetString(jo, "moctwRating");
            mpaaRating = jGetString(jo, "mpaaRating");
            mtrcbRating = jGetString(jo, "mtrcbRating");
            nbcRating = jGetString(jo, "nbcRating");
            nbcplRating = jGetString(jo, "nbcplRating");
            nfrcRating = jGetString(jo, "nfrcRating");
            nfvcbRating = jGetString(jo, "nfvcbRating");
            nkclvRating = jGetString(jo, "nkclvRating");
            oflcRating = jGetString(jo, "oflcRating");
            pefilmRating = jGetString(jo, "pefilmRating");
            rcnofRating = jGetString(jo, "rcnofRating");
            resorteviolenciaRating = jGetString(jo, "resorteviolenciaRating");
            rtcRating = jGetString(jo, "rtcRating");
            rteRating = jGetString(jo, "rteRating");
            russiaRating = jGetString(jo, "russiaRating");
            skfilmRating = jGetString(jo, "skfilmRating");
            smaisRating = jGetString(jo, "smaisRating");
            smsaRating = jGetString(jo, "smsaRating");
            tvpgRating = jGetString(jo, "tvpgRating");
            ytRating = jGetString(jo, "ytRating");
        }
    }

    public static class Status extends JsonModel {
        String uploadStatus = null;
        String failureReason = null;
        String rejectionReason = null;
        String privacyStatus = null;
        String publishAt = null;
        String license = null;
        String embeddable = null;
        String publicStatsViewable = null;

        @Override
        public void
        set(JSONObject jo) {
            uploadStatus = jGetString(jo, "uploadStatus");
            failureReason = jGetString(jo, "failureReason");
            rejectionReason = jGetString(jo, "rejectionReason");
            privacyStatus = jGetString(jo, "privacyStatus");
            publishAt = jGetString(jo, "publishAt");
            license = jGetString(jo, "license");
            embeddable = jGetString(jo, "embeddable");
            publicStatsViewable = jGetString(jo, "publicStatsViewable");

        }
    }

    public static class Statistics extends JsonModel {
        Long viewCount = null;
        Long likeCount = null;
        Long dislikeCount = null;
        Long favoriteCount = null;
        Long commentCount = null;

        @Override
        public void
        set(JSONObject jo) {
            viewCount = jGetLong(jo, "viewCount");
            likeCount = jGetLong(jo, "likeCount");
            dislikeCount = jGetLong(jo, "dislikeCount");
            favoriteCount = jGetLong(jo, "favoriteCount");
            commentCount = jGetLong(jo, "commentCount");
        }
    }

    public static class Player extends JsonModel {
        String embedHtml = null;

        @Override
        public void
        set(JSONObject jo) {
            embedHtml = jGetString(jo, "embedHtml");
        }
    }

    public static class TopicDetails extends JsonModel {
        String[] topicIds = null;
        String[] relevantTopicIds = null;

        @Override
        public void
        set(JSONObject jo) {
            topicIds = jGetStrings(jo, "topicIds");
            relevantTopicIds = jGetStrings(jo, "relevantTopicIds");
        }
    }

    public static class Location extends JsonModel {
        Double latitude = null;
        Double longitude = null;
        Double altitude = null;

        @Override
        public void
        set(JSONObject jo) {
            latitude = jGetDouble(jo, "latitude");
            longitude = jGetDouble(jo, "longitude");
            altitude = jGetDouble(jo, "altitude");
        }
    }

    public static class RecordingDetails extends JsonModel {
        String locationDescription = null;
        Location location = null;
        String recordingDate = null;

        @Override
        public void
        set(JSONObject jo) {
            locationDescription = jGetString(jo, "locationDescription");
            location = jGetObject(jo, "location", Location.class);
            recordingDate = jGetString(jo, "recordingDate");
        }
    }

    public static class VideoStream extends JsonModel {
        Integer widthPixels = null;
        Integer heightPixels = null;
        Double frameRateFps = null;
        Double aspectRatio = null;
        String codec = null;
        Long bitrateBps = null;
        String rotation = null;
        String vendor = null;

        @Override
        public void
        set(JSONObject jo) {
            widthPixels = jGetInt(jo, "widthPixels");
            heightPixels = jGetInt(jo, "heightPixels");
            frameRateFps = jGetDouble(jo, "frameRateFps");
            aspectRatio = jGetDouble(jo, "aspectRatio");
            codec = jGetString(jo, "codec");
            bitrateBps = jGetLong(jo, "bitrateBps");
            rotation = jGetString(jo, "rotation");
            vendor = jGetString(jo, "vendor");
        }
    }

    public static class AudioStream extends JsonModel {
        Integer channelCount = null;
        String codec = null;
        Long bitrateBps = null;
        String vendor = null;

        @Override
        public void
        set(JSONObject jo) {
            channelCount = jGetInt(jo, "channelCount");
            codec = jGetString(jo, "codec");
            bitrateBps = jGetLong(jo, "bitrateBps");
            vendor = jGetString(jo, "vendor");
        }
    }

    public static class FileDetails extends JsonModel {
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
        public void
        set(JSONObject jo) {
            fileName = jGetString(jo, "fileName");
            fileSize = jGetLong(jo, "fileSize");
            fileType = jGetString(jo, "fileType");
            container = jGetString(jo, "container");
            videoStreams = jGetObjects(jo, "videoStreams", VideoStream.class);
            audioStreams = jGetObjects(jo, "audioStreams", AudioStream.class);
            durationMs = jGetLong(jo, "durationMs");
            bitrateBps = jGetLong(jo, "bitrateBps");
            recordingLocation = jGetObject(jo, "recordingLocation", Location.class);
            creationTime = jGetString(jo, "creationTime");
        }
    }

    public static class ProcessingProgress extends JsonModel {
        Long partsTotal = null;
        Long partsProcessed = null;
        Long timeLeftMs = null;

        @Override
        public void
        set(JSONObject jo) {
            partsTotal = jGetLong(jo, "partsTotal");
            partsProcessed = jGetLong(jo, "partsProcessed");
            timeLeftMs = jGetLong(jo, "timeLeftMs");
        }
    }

    public static class ProcessingDetails extends JsonModel {
        String processingStatus = null;
        ProcessingProgress processingProgress = null;
        String processingFailureReason = null;
        String fileDetailsAvailability = null;
        String processingIssuesAvailability = null;
        String tagSuggestionsAvailability = null;
        String editorSuggestionsAvailability = null;
        String thumbnailsAvailability = null;

        @Override
        public void
        set(JSONObject jo) {
            processingStatus = jGetString(jo, "processingStatus");
            processingProgress = jGetObject(jo, "processingProgress", ProcessingProgress.class);
            processingFailureReason = jGetString(jo, "processingFailureReason");
            fileDetailsAvailability = jGetString(jo, "fileDetailsAvailability");
            processingIssuesAvailability = jGetString(jo, "processingIssuesAvailability");
            tagSuggestionsAvailability = jGetString(jo, "tagSuggestionsAvailability");
            editorSuggestionsAvailability = jGetString(jo, "editorSuggestionsAvailability");
            thumbnailsAvailability = jGetString(jo, "thumbnailsAvailability");
        }
    }

    public static class TagSuggestion extends JsonModel {
        String tag = null;
        String[] categoryRestricts = null;

        @Override
        public void
        set(JSONObject jo) {
            tag = jGetString(jo, "tag");
            categoryRestricts = jGetStrings(jo, "categoryRestricts");
        }
    }

    public static class Suggestions extends JsonModel {
        String[] processingErrors = null;
        String[] processingWarnings = null;
        String[] processingHints = null;
        TagSuggestion[] tagSuggestions = null;
        String[] editorSuggestions = null;

        @Override
        public void
        set(JSONObject jo) {
            processingErrors = jGetStrings(jo, "processingErrors");
            processingWarnings = jGetStrings(jo, "processingWarnings");
            processingHints = jGetStrings(jo, "processingHints");
            tagSuggestions = jGetObjects(jo, "tagSuggestions", TagSuggestion.class);
            editorSuggestions = jGetStrings(jo, "editorSuggestions");
        }
    }


    public static class LiveStreamingDetails extends JsonModel {
        String actualStartTime = null;
        String actualEndTime = null;
        String scheduledStartTime = null;
        String scheduledEndTime = null;
        Long concurrentViewers = null;

        @Override
        public void
        set(JSONObject jo) {
            actualStartTime = jGetString(jo, "actualStartTime");
            actualEndTime = jGetString(jo, "actualEndTime");
            scheduledStartTime = jGetString(jo, "scheduledStartTime");
            scheduledEndTime = jGetString(jo, "scheduledEndTime");
            concurrentViewers = jGetLong(jo, "concurrentViewers");
        }
    }

    public static class ContentDetails extends JsonModel {
        String duration = null;
        String dimension = null;
        String definition = null;
        String caption = null;
        Boolean licensedContent = null;
        RegionRestriction regionRestriction = null;
        ContentRating contentRating = null;

        @Override
        public void
        set(JSONObject jo) {
            duration = jGetString(jo, "duration");
            dimension = jGetString(jo, "dimension");
            definition = jGetString(jo, "definition");
            caption = jGetString(jo, "caption");
            licensedContent = jGetBoolean(jo, "licensedContent");
            regionRestriction = jGetObject(jo, "regionRestriction", RegionRestriction.class);
            contentRating = jGetObject(jo, "contentRating", ContentRating.class);
        }
    }

    public static class VideoRes extends JsonModel {
        @SuppressWarnings("unused")
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
        public void
        set(JSONObject jo) {
            kind = jGetString(jo, "kind");
            etag = jGetString(jo, "etag");
            id = jGetString(jo, "id");
            snippet = jGetObject(jo, "snippet", Snippet.class);
            contentDetails = jGetObject(jo, "contentDetails", ContentDetails.class);
            status = jGetObject(jo, "status", Status.class);
            statistics = jGetObject(jo, "statistics", Statistics.class);
            player = jGetObject(jo, "player", Player.class);
            topicDetails = jGetObject(jo, "topicDetails", TopicDetails.class);
            recordingDetails = jGetObject(jo, "recordingDetails", RecordingDetails.class);
            fileDetails = jGetObject(jo, "fileDetails", FileDetails.class);
            processingDetails = jGetObject(jo, "processingDetails", ProcessingDetails.class);
            suggestions = jGetObject(jo, "suggestions", Suggestions.class);
            liveStreamingDetails = jGetObject(jo, "liveStreamingDetails", LiveStreamingDetails.class);
        }

        /**
         * Generate corresponding data structure of facade client side.
         */
        YTDataAdapter.Video
        makeAdapterData() {
            YTDataAdapter.Video v = new YTDataAdapter.Video();
            v.id = id;
            setSnippetData(v, snippet);
            if (null != contentDetails)
                v.playTimeSec = (int)parseYTDuration(contentDetails.duration);
            return v;
        }
    }

    public static class SearchRes extends JsonModel {
        @SuppressWarnings("unused")
        static final String KIND = "youtube#searchResult";
        String kind = null;
        String etag = null;
        Id id = null;
        Snippet snippet = null;

        @Override
        public void
        set(JSONObject jo) {
            kind = jGetString(jo, "kind");
            etag = jGetString(jo, "etag");
            id = jGetObject(jo, "id", Id.class);
            snippet = jGetObject(jo, "snippet", Snippet.class);
        }

        YTDataAdapter.Video
        makeAdapterData() {
            YTDataAdapter.Video v = new YTDataAdapter.Video();
            v.id = id.videoId;
            setSnippetData(v, snippet);
            return v;
        }
    }

    public static class SearchListResponse extends JsonModel {
        @SuppressWarnings("unused")
        static final String KIND = "youtube#searchListResponse";
        String kind = null;
        String etag = null;
        String nextPageToken = null;
        String prevPageToken = null;
        PageInfo pageInfo = null;
        SearchRes[] items = null;

        @Override
        public void
        set(JSONObject jo) {
            kind = jGetString(jo, "kind");
            etag = jGetString(jo, "etag");
            nextPageToken = jGetString(jo, "nextPageToken");
            prevPageToken = jGetString(jo, "prevPageToken");
            pageInfo = jGetObject(jo, "pageInfo", PageInfo.class);
            items = jGetObjects(jo, "items", SearchRes.class);
        }

        /**
         * Generate corresponding data structure of facade client side.
         */
        @SuppressWarnings("unused")
        YTDataAdapter.VideoListResp
        makeAdapterData() {
            YTDataAdapter.VideoListResp r = new YTDataAdapter.VideoListResp();
            r.page = new YTDataAdapter.PageInfo();
            if (null != pageInfo
                && null != pageInfo.totalResults)
                r.page.totalResults = pageInfo.totalResults;
            r.page.nextPageToken = nextPageToken;
            r.page.prevPageToken = prevPageToken;
            r.vids = new YTDataAdapter.Video[items.length];
            for (int i = 0; i < r.vids.length; i++)
                r.vids[i] = items[i].makeAdapterData();
            return r;
        }
    }

    public static class VideoListResponse extends JsonModel {
        @SuppressWarnings("unused")
        static final String KIND = "youtube#videoListResponse";
        String kind = null;
        String etag = null;
        String nextPageToken = null;
        String prevPageToken = null;
        PageInfo pageInfo = null;
        VideoRes[] items = null;

        @Override
        public void
        set(JSONObject jo) {
            kind = jGetString(jo, "kind");
            etag = jGetString(jo, "etag");
            nextPageToken = jGetString(jo, "nextPageToken");
            prevPageToken = jGetString(jo, "prevPageToken");
            pageInfo = jGetObject(jo, "pageInfo", PageInfo.class);
            items = jGetObjects(jo, "items", VideoRes.class);
        }

        /**
         * Generate corresponding data structure of facade client side.
         */
        YTDataAdapter.VideoListResp
        makeAdapterData() {
            YTDataAdapter.VideoListResp r = new YTDataAdapter.VideoListResp();
            r.page = new YTDataAdapter.PageInfo();
            if (null != pageInfo
                && null != pageInfo.totalResults)
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

}
