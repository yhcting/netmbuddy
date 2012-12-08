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

package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import android.net.Uri;
import free.yhc.netmbuddy.utils.Utils;

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
