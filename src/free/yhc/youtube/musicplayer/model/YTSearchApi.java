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

package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logI;

import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import android.net.Uri;


// ============================================================================
//
// See Google's Youtube API documents for details.
//
// ============================================================================
//
// Notation
//     //P <= comment out for performance.
//
public class YTSearchApi {
    static final String sQueryProjection = "fields=openSearch:totalResults,openSearch:startIndex,openSearch:itemsPerPage,"
            + "entry(author(name),media:group(media:title,yt:videoid,media:thumbnail[@yt:name='default'](@url),yt:duration,yt:uploaded))";

    // For debugging
    private String mDbgQuery = "";
    private String mDbgXml   = "";

    public static class Result {
        public Header  header       = null;
        public Entry[] enties       = null;
    }

    public static class Header {
        public String  totalResults = "-1";
        public String  startIndex   = "-1";
        public String  itemsPerPage = "-1";
    }

    // Most of them are not used yet.
    // But for future use...
    // If performance is too low, removing unused value should be considered.
    public static class Entry {
        public long         uflag       = 0;    // reserved area for additional UX case.
        // This is NOT related with Youtube data!

        public boolean      available   = true; // available for this App.

        public Author       author      = new Author();
        public Media        media       = new Media();
        public Statistics   stat        = new Statistics();
        public GDRating     gdRating    = new GDRating();
        public YTRating     ytRating    = new YTRating();

        public class Author {
            public String   name            = "";
            public String   uri             = "";
            public String   ytUserId        = "";
        }

        public class Media {
            public String   title           = "";
            public String   description     = "";
            public String   thumbnailUrl    = ""; // smallest thumbnail url
            public String   uploadedTime    = "";
            public String   videoId         = "";
            public String   playTime        = "";
            public Credit   credit          = new Credit();

            public class Credit {
                public String role  = "";
                public String name  = "";
            }
        }

        public class Statistics {
            public String favoriteCount = "-1";
            public String viewCount     = "-1";
        }

        public class GDRating {
            public String min           = "-1";
            public String max           = "-1";
            public String average       = "-1";
            public String numRaters     = "-1";
        }

        public class YTRating {
            public String numLikes      = "-1";
            public String numDislikes   = "-1";
        }
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

    public YTSearchApi() {}

    public void
    setDebuggingInfo(String query, String xml) {
        mDbgQuery = query;
        mDbgXml = xml;
    }

    // For debugging.
    private static void
    printNexts(Node n) {
        String msg = "";
        while (null != n) {
            msg = msg + " / " + n.getNodeName();
            n = n.getNextSibling();
        }
        logI(msg + "\n");
    }

    private static Node
    findNodeByNameFromSiblings(Node n, String name) {
        while (null != n) {
            if (n.getNodeName().equalsIgnoreCase(name))
                return n;
            n = n.getNextSibling();
        }
        return null;
    }

    private static String
    getTextValue(Node n) {
        String text = "";
        Node t = findNodeByNameFromSiblings(n.getFirstChild(), "#text");
        if (null != t)
            text = t.getNodeValue();
        return text;
    }

    private static Err
    parseEntryAuthor(Node n, Entry.Author en) {
        n = n.getFirstChild();
        while (null != n) {
            //logI("        - " + n.getNodeName());
            if ("name".equals(n.getNodeName()))
                en.name = getTextValue(n);
            else if ("uri".equals(n.getNodeName()))
                en.uri = getTextValue(n);
            else if ("yt:userId".equals(n.getNodeName()))
                en.ytUserId = getTextValue(n);

            n = n.getNextSibling();
        }
        return Err.NO_ERR;
    }

    private static Err
    parseEntryStatistics(Node n, Entry.Statistics en) {
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("favoriteCount");
        if (null != nItem)
            en.favoriteCount = nItem.getNodeValue();

        nItem = nnm.getNamedItem("viewCount");
        if (null != nItem)
            en.viewCount = nItem.getNodeValue();

        return Err.NO_ERR;
    }

    private static Err
    parseEntryYTRating(Node n, Entry.YTRating en) {
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("numDislikes");
        if (null != nItem)
            en.numDislikes = nItem.getNodeValue();

        nItem = nnm.getNamedItem("numLikes");
        if (null != nItem)
            en.numLikes = nItem.getNodeValue();

        return Err.NO_ERR;
    }

    private static Err
    parseEntryGDRating(Node n, Entry.GDRating en) {
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("average");
        if (null != nItem)
            en.average = nItem.getNodeValue();

        nItem = nnm.getNamedItem("max");
        if (null != nItem)
            en.max = nItem.getNodeValue();

        nItem = nnm.getNamedItem("min");
        if (null != nItem)
            en.min = nItem.getNodeValue();

        nItem = nnm.getNamedItem("numRaters");
        if (null != nItem)
            en.numRaters = nItem.getNodeValue();

        return Err.NO_ERR;
    }

    private static Err
    parseEntryMediaThumbnail(Node n, Entry.Media en) {
        // Only "yt:name='default'" is used.
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("yt:name");
        if (null != nItem && !"default".equals(nItem.getNodeValue()))
            return Err.NO_ERR; // ignore other thumbnails.

        // If there is no 'yt:name' attribute, it is accepted.
        nItem = nnm.getNamedItem("url");
        if (null != nItem)
            en.thumbnailUrl = nItem.getNodeValue();

        return Err.NO_ERR;
    }

    private static Err
    parseEntryMediaCredit(Node n, Entry.Media.Credit en) {
        // Overwriting if there are multiple "credit" nodes.
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("role");
        if (null != nItem)
            en.role = nItem.getNodeValue();
        en.name = getTextValue(n);
        return Err.NO_ERR;
    }

    private static Err
    parseEntryMediaDuration(Node n, Entry.Media en) {
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("seconds");
        if (null != nItem)
            en.playTime = nItem.getNodeValue();
        return Err.NO_ERR;
    }

    private static Err
    parseEntryMedia(Node n, Entry.Media en) {
        n = n.getFirstChild();
        while (null != n) {
            //logI("        - " + n.getNodeName());
            if ("yt:videoid".equals(n.getNodeName()))
                en.videoId = getTextValue(n);
            else if ("yt:duration".equals(n.getNodeName()))
                parseEntryMediaDuration(n, en);
            else if ("media:credit".equals(n.getNodeName()))
                parseEntryMediaCredit(n, en.credit);
            else if ("media:description".equals(n.getNodeName()))
                en.description = getTextValue(n);
            else if ("media:thumbnail".equals(n.getNodeName()))
                parseEntryMediaThumbnail(n, en);
            else if ("media:title".equals(n.getNodeName()))
                en.title = getTextValue(n);
            else if ("yt:uploaded".equals(n.getNodeName()))
                en.uploadedTime = getTextValue(n);

            n = n.getNextSibling();
        }
        return Err.NO_ERR;
    }

    private static Err
    parseEntry(Node n, LinkedList<Entry> entryl) {
        Entry en = new Entry();
        n = n.getFirstChild();
        while (null != n) {
            //logI("    - " + n.getNodeName());
            if ("media:group".equals(n.getNodeName()))
                parseEntryMedia(n, en.media);
            else if ("author".equals(n.getNodeName()))
                parseEntryAuthor(n, en.author);
            else if ("gd:rating".equals(n.getNodeName()))
                parseEntryGDRating(n, en.gdRating);
            else if ("yt:rating".equals(n.getNodeName()))
                parseEntryYTRating(n, en.ytRating);
            else if ("yt:statistics".equals(n.getNodeName()))
                parseEntryStatistics(n, en.stat);

            n = n.getNextSibling();
        }
        en.available = verifyEntry(en);
        entryl.addLast(en);
        return Err.NO_ERR;
    }

    /**
     * WARNING
     * This parser assumes that query is issued with 'mobile' projection
     * @param dom
     * @return
     */
    public static Result
    parseSearchFeed(Document dom) throws YTMPException {
        Element root = dom.getDocumentElement();
        LinkedList<Entry> entryl = new LinkedList<Entry>();
        Result res = new Result();
        res.header = new Header();
        Node n = root.getFirstChild();
        while (null != n) {
            //logI("- " + n.getNodeName());
            if ("entry".equals(n.getNodeName()))
                parseEntry(n, entryl);
            else if ("openSearch:totalResults".equals(n.getNodeName()))
                res.header.totalResults = getTextValue(n);
            else if ("openSearch:startIndex".equals(n.getNodeName()))
                res.header.startIndex = getTextValue(n);
            else if ("openSearch:itemsPerPage".equals(n.getNodeName()))
                res.header.itemsPerPage = getTextValue(n);

            n = n.getNextSibling();
        }
        res.enties = entryl.toArray(new Entry[0]);
        return res;
    }
}
