/******************************************************************************
 * Copyright (C) 2012, 2013, 2014
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

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import android.net.Uri;
import free.yhc.netmbuddy.utils.Utils;


// This class is deprecated.
// This is used to support deprecated API-v2.
// And it is NOT USED anymore.
// It is left only for history.
public class YTVideoFeed extends YTFeed {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTVideoFeed.class);

    static final String sQueryProjection = "fields=openSearch:totalResults,openSearch:startIndex,openSearch:itemsPerPage,"
            + "entry(author(name),media:group(media:title,yt:videoid,media:thumbnail[@yt:name='default'](@url),yt:duration,yt:uploaded))";

    // For debugging
    private String mDbgQuery = "";
    private String mDbgXml   = "";

    // Most of them are not used yet.
    // But for future use...
    public static class Entry extends YTFeed.Entry {
        public Author       author      = new Author();
        public Statistics   stat        = new Statistics();
        public GdRating     gdRating    = new GdRating();
        public YtRating     ytRating    = new YtRating();
    }

    public static enum ParameterOrderBy {
        PUBLISHED   ("published"),
        RELEVANCE   ("relevance"),
        VIEWCOUNT   ("viewCount"),
        RATING      ("rating");

        private final String value;

        ParameterOrderBy(String aValue) {
            value = aValue;
        }

        public String
        getParamter() {
            return "orderby";
        }

        public String
        getValue() {
            return value;
        }
    }

    public static String
    getFeedUrlByKeyword(String word, int start, int maxCount) {
        eAssert(0 < start && 0 < maxCount && maxCount <= Policy.YTSEARCH_MAX_RESULTS);
        // http://gdata.youtube.com/feeds/api/videos?q=김광석&start-index=1&max-results=50&client=ytapi-youtube-search&autoplay=1&format=5&v=2
        word = word.replaceAll("\\s+", "+");
        return "http://gdata.youtube.com/feeds/api/videos?q="
                + Uri.encode(word, "+")
                + "&start-index=" + start
                + "&max-results=" + maxCount
                + "&client=ytapi-youtube-search&format=5&v=2&"
                + sQueryProjection;
    }

    public static String
    getFeedUrlByAuthor(String author, int start, int maxCount) {
        return "http://gdata.youtube.com/feeds/api/users/"
                + Uri.encode(author, null)
                + "/uploads?format=5&v=2"
                + "&start-index=" + start
                + "&max-results=" + maxCount
                + "&client=ytapi-youtube-search&"
                + sQueryProjection;
    }

    public static String
    getFeedUrlByPlaylist(String playlistId, int start, int maxCount) {
        return "http://gdata.youtube.com/feeds/api/playlists/"
                + Uri.encode(playlistId, null)
                + "?v=2"
                + "&start-index=" + start
                + "&max-results=" + maxCount
                + "&client=ytapi-youtube-search&"
                + sQueryProjection;
    }

    /**
     * Is this valid entry that can be handled by YoutubeMusicPlayer?
     * @param en
     * @return
     */
    private static boolean
    verifyEntry(Entry en) {
        return Utils.isValidValue(en.media.videoId)
               && Utils.isValidValue(en.media.title);
    }

    public YTVideoFeed() {}

    public void
    setDebuggingInfo(String query, String xml) {
        mDbgQuery = query;
        mDbgXml = xml;
    }


    private static Err
    parseEntry(Node n, LinkedList<Entry> entryl) {
        Entry en = new Entry();
        n = n.getFirstChild();
        while (null != n) {
            //logI("    - " + n.getNodeName());
            if ("media:group".equals(n.getNodeName()))
                parseMedia(n, en.media);
            else if ("author".equals(n.getNodeName()))
                parseAuthor(n, en.author);
            else if ("gd:rating".equals(n.getNodeName()))
                parseGdRating(n, en.gdRating);
            else if ("yt:rating".equals(n.getNodeName()))
                parseYtRating(n, en.ytRating);
            else if ("yt:statistics".equals(n.getNodeName()))
                parseStatistics(n, en.stat);

            n = n.getNextSibling();
        }
        en.available = verifyEntry(en);
        entryl.addLast(en);
        return Err.NO_ERR;
    }

    /**
     * @param dom
     * @return
     */
    public static Result
    parseFeed(Document dom) {
        Element root = dom.getDocumentElement();
        LinkedList<Entry> entryl = new LinkedList<Entry>();
        Result res = new Result();
        res.header = new Header();
        Node n = root.getFirstChild();
        while (null != n) {
            //logI("- " + n.getNodeName());
            if ("entry".equals(n.getNodeName()))
                parseEntry(n, entryl);
            else if (!parseHeader(res.header, n))
                ;// do nothing - ignore it.

            n = n.getNextSibling();
        }
        res.entries = entryl.toArray(new Entry[0]);
        return res;
    }
}
