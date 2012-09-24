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

import static free.yhc.youtube.musicplayer.model.Utils.logI;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public abstract class YTFeed {
    public static class Result {
        public Header   header          = null;
        public Entry[]  entries         = null;
    }

    public static class Header {
        public String   totalResults    = "-1";
        public String   startIndex      = "-1";
        public String   itemsPerPage    = "-1";
    }

    public static class Entry {
        public long     uflag       = 0;    // reserved area for additional UX case.
        public boolean  available   = true; // available for this App.

        public Media    media       = new Media();
    }

    public static class Author {
        public String   name            = "";
        public String   uri             = "";
        public String   ytUserId        = "";
    }

    public static class Media {
        public String   title           = "";
        public String   description     = "";
        public String   thumbnailUrl    = ""; // smallest thumbnail url
        public String   uploadedTime    = "";
        public String   videoId         = "";
        public String   playTime        = "";
        public Credit   credit          = new Credit();

        public static class Credit {
            public String role  = "";
            public String name  = "";
        }
    }

    public static class Statistics {
        public String favoriteCount = "-1";
        public String viewCount     = "-1";
    }

    public static class GdRating {
        public String min           = "-1";
        public String max           = "-1";
        public String average       = "-1";
        public String numRaters     = "-1";
    }

    public static class YtRating {
        public String numLikes      = "-1";
        public String numDislikes   = "-1";
    }

    protected static final Node
    findNodeByNameFromSiblings(Node n, String name) {
        while (null != n) {
            if (n.getNodeName().equalsIgnoreCase(name))
                return n;
            n = n.getNextSibling();
        }
        return null;
    }

    protected static final String
    getTextValue(Node n) {
        String text = "";
        Node t = findNodeByNameFromSiblings(n.getFirstChild(), "#text");
        if (null != t)
            text = t.getNodeValue();
        return text;
    }

    // For debugging.
    protected static final void
    printNexts(Node n) {
        String msg = "";
        while (null != n) {
            msg = msg + " / " + n.getNodeName();
            n = n.getNextSibling();
        }
        logI(msg + "\n");
    }


    protected static final Err
    parseAuthor(Node n, Author en) {
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



    protected static final Err
    parseStatistics(Node n, Statistics en) {
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("favoriteCount");
        if (null != nItem)
            en.favoriteCount = nItem.getNodeValue();

        nItem = nnm.getNamedItem("viewCount");
        if (null != nItem)
            en.viewCount = nItem.getNodeValue();

        return Err.NO_ERR;
    }

    protected static final Err
    parseYtRating(Node n, YtRating en) {
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("numDislikes");
        if (null != nItem)
            en.numDislikes = nItem.getNodeValue();

        nItem = nnm.getNamedItem("numLikes");
        if (null != nItem)
            en.numLikes = nItem.getNodeValue();

        return Err.NO_ERR;
    }

    protected static final Err
    parseGdRating(Node n, GdRating en) {
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

    protected static final Err
    parseMediaThumbnail(Node n, Media en) {
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

    protected static final Err
    parseMediaCredit(Node n, Media.Credit en) {
        // Overwriting if there are multiple "credit" nodes.
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("role");
        if (null != nItem)
            en.role = nItem.getNodeValue();
        en.name = getTextValue(n);
        return Err.NO_ERR;
    }

    protected static final Err
    parseMediaDuration(Node n, Media en) {
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("seconds");
        if (null != nItem)
            en.playTime = nItem.getNodeValue();
        return Err.NO_ERR;
    }

    protected static final Err
    parseMedia(Node n, Media en) {
        n = n.getFirstChild();
        while (null != n) {
            //logI("        - " + n.getNodeName());
            if ("yt:videoid".equals(n.getNodeName()))
                en.videoId = getTextValue(n);
            else if ("yt:duration".equals(n.getNodeName()))
                parseMediaDuration(n, en);
            else if ("media:credit".equals(n.getNodeName()))
                parseMediaCredit(n, en.credit);
            else if ("media:description".equals(n.getNodeName()))
                en.description = getTextValue(n);
            else if ("media:thumbnail".equals(n.getNodeName()))
                parseMediaThumbnail(n, en);
            else if ("media:title".equals(n.getNodeName()))
                en.title = getTextValue(n);
            else if ("yt:uploaded".equals(n.getNodeName()))
                en.uploadedTime = getTextValue(n);

            n = n.getNextSibling();
        }
        return Err.NO_ERR;
    }

    /**
     *
     * @param n
     * @return
     *   true : handled otherwise false
     */
    protected static final boolean
    parseHeader(Header hdr, // inout
                Node n) {
        boolean handled = true;
        if ("openSearch:totalResults".equals(n.getNodeName()))
            hdr.totalResults = getTextValue(n);
        else if ("openSearch:startIndex".equals(n.getNodeName()))
            hdr.startIndex = getTextValue(n);
        else if ("openSearch:itemsPerPage".equals(n.getNodeName()))
            hdr.itemsPerPage = getTextValue(n);
        else
            handled =false;

        return handled;
    }
}
