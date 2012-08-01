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

// Search example with query
// "http://gdata.youtube.com/feeds/mobile/videos?q=%EA%B9%80%EA%B4%91%EC%84%9D&start-index=1&max-results=50&client=ytapi-youtube-search&format=6&v=2"
//
//  <id>tag:youtube.com,2008:videos</id>
//  <updated>2012-07-24T06:30:59.527Z</updated>
//  <category scheme='http://schemas.google.com/g/2005#kind'
//            term='http://gdata.youtube.com/schemas/2007#video'/>
//  <title>YouTube Videos matching query: 김광석</title>
//  <logo>http://www.youtube.com/img/pic_youtubelogo_123x63.gif</logo>
//  <link rel='alternate'
//        type='text/html'
//        href='http://www.youtube.com'/>
//  <link rel='http://schemas.google.com/g/2005#feed'
//        type='application/atom+xml'
//        href='http://gdata.youtube.com/feeds/mobile/videos?client=ytapi-youtube-search&amp;v=2'/>
//  <link rel='http://schemas.google.com/g/2005#batch'
//        type='application/atom+xml'
//        href='http://gdata.youtube.com/feeds/mobile/videos/batch?client=ytapi-youtube-search&amp;v=2'/>
//  <link rel='self'
//        type='application/atom+xml'
//        href='http://gdata.youtube.com/feeds/mobile/videos?q=%EA%B9%80%EA%B4%91%EC%84%9D&amp;start-index=1&amp;max-results=50&amp;client=ytapi-youtube-search&amp;format=6&amp;v=2'/>
//  <link rel='service' type='application/atomsvc+xml'
//        href='http://gdata.youtube.com/feeds/mobile/videos?alt=atom-service&amp;v=2'/>
//  <link rel='next' type='application/atom+xml'
//        href='http://gdata.youtube.com/feeds/mobile/videos?q=%EA%B9%80%EA%B4%91%EC%84%9D&amp;start-index=51&amp;max-results=50&amp;client=ytapi-youtube-search&amp;format=6&amp;v=2'/>
//  <author>
//      <name>YouTube</name>
//      <uri>http://www.youtube.com/</uri>
//  </author>
//  <generator version='2.1'
//             uri='http://gdata.youtube.com'>
//      YouTube data API
//  </generator>
//  <openSearch:totalResults>
//      1634
//  </openSearch:totalResults>
//  <openSearch:startIndex>
//      1
//  </openSearch:startIndex>
//  <openSearch:itemsPerPage>
//      50
//  </openSearch:itemsPerPage>
//
//  <entry gd:etag='W/&quot;CUYAQn47eCp7I2A9WhJQEEo.&quot;'>
//      <id>tag:youtube.com,2008:video:IwZtD0XB7JQ</id>
//      <published>2010-09-08T00:28:34.000Z</published>
//      <updated>2012-07-23T19:52:23.000Z</updated>
//      <category scheme='http://schemas.google.com/g/2005#kind'
//                term='http://gdata.youtube.com/schemas/2007#video'/>
//      <title>김광석-너무 아픈사랑은 사랑이 아니었음을</title>
//      <link rel='alternate' type='text/html'
//            href='http://www.youtube.com/watch?v=IwZtD0XB7JQ&amp;feature=youtube_gdata'/>
//      <link rel='http://gdata.youtube.com/schemas/2007#video.responses'
//            type='application/atom+xml'
//            href='http://gdata.youtube.com/feeds/mobile/videos/IwZtD0XB7JQ/responses?client=ytapi-youtube-search&amp;v=2'/>
//      <link rel='http://gdata.youtube.com/schemas/2007#video.related'
//            type='application/atom+xml'
//            href='http://gdata.youtube.com/feeds/mobile/videos/IwZtD0XB7JQ/related?client=ytapi-youtube-search&amp;v=2'/>
//      <link rel='http://gdata.youtube.com/schemas/2007#mobile'
//            type='text/html'
//            href='http://m.youtube.com/details?v=IwZtD0XB7JQ'/>
//      <link rel='http://gdata.youtube.com/schemas/2007#uploader'
//            type='application/atom+xml'
//            href='http://gdata.youtube.com/feeds/mobile/users/tk7vwplRl0X-V7coWRt1EA?client=ytapi-youtube-search&amp;v=2'/>
//      <link rel='self'
//            type='application/atom+xml'
//            href='http://gdata.youtube.com/feeds/mobile/videos/IwZtD0XB7JQ?client=ytapi-youtube-search&amp;v=2'/>
//      <author>
//          <name>thekeishin79</name>
//          <uri>http://gdata.youtube.com/feeds/mobile/users/thekeishin79</uri>
//          <yt:userId>tk7vwplRl0X-V7coWRt1EA</yt:userId>
//      </author>
//      <yt:accessControl action='comment'
//                        permission='allowed'/>
//      <yt:accessControl action='commentVote'
//                        permission='allowed'/>
//      <yt:accessControl action='videoRespond'
//                        permission='moderated'/>
//      <yt:accessControl action='rate'
//                        permission='allowed'/>
//      <yt:accessControl action='embed'
//                        permission='allowed'/>
//      <yt:accessControl action='list'
//                        permission='allowed'/>
//      <yt:accessControl action='autoPlay'
//                        permission='allowed'/>
//      <yt:accessControl action='syndicate'
//                        permission='allowed'/>
//      <gd:comments>
//          <gd:feedLink rel='http://gdata.youtube.com/schemas/2007#comments'
//                       href='http://gdata.youtube.com/feeds/mobile/videos/IwZtD0XB7JQ/comments?client=ytapi-youtube-search&amp;v=2'
//                       countHint='171'/>
//      </gd:comments>
//      <media:group>
//          <media:category label='Entertainment'
//                          scheme='http://gdata.youtube.com/schemas/2007/categories.cat'>
//              Entertainment
//          </media:category>
//          <media:content url='rtsp://v5.cache5.c.youtube.com/CjgLENy73wIaLwmU7MFFD20GIxMYDSANFEIUeXRhcGkteW91dHViZS1zZWFyY2hIBlIGdmlkZW9zDA==/0/0/0/video.3gp'
//                         type='video/3gpp'
//                         medium='video'
//                         isDefault='true'
//                         expression='full'
//                         duration='323'
//                         yt:format='1'/>
//          <media:content url='rtsp://v6.cache2.c.youtube.com/CjgLENy73wIaLwmU7MFFD20GIxMYESARFEIUeXRhcGkteW91dHViZS1zZWFyY2hIBlIGdmlkZW9zDA==/0/0/0/video.3gp'
//                         type='video/3gpp'
//                         medium='video'
//                         expression='full'
//                         duration='323'
//                         yt:format='6'/>
//          <media:credit role='uploader'
//                        scheme='urn:youtube'
//                        yt:display='thekeishin79'>
//              thekeishin79
//          </media:credit>
//          <media:description type='plain'/>
//          <media:keywords>KKS, Super, Concert</media:keywords>
//          <media:license type='text/html'
//                 href='http://www.youtube.com/t/terms'>
//              youtube
//          </media:license>
//          <media:player url='http://www.youtube.com/watch?v=IwZtD0XB7JQ&amp;feature=youtube_gdata_player'/>
//          <media:thumbnail url='http://i.ytimg.com/vi/IwZtD0XB7JQ/default.jpg'
//                           height='90'
//                           width='120'
//                           time='00:02:41.500'
//                           yt:name='default'/>
//          <media:thumbnail url='http://i.ytimg.com/vi/IwZtD0XB7JQ/mqdefault.jpg'
//                           height='180'
//                           width='320'
//                           yt:name='mqdefault'/>
//          <media:thumbnail url='http://i.ytimg.com/vi/IwZtD0XB7JQ/hqdefault.jpg'
//                           height='360' width='480'
//                           yt:name='hqdefault'/>
//          <media:thumbnail url='http://i.ytimg.com/vi/IwZtD0XB7JQ/1.jpg'
//                           height='90'
//                           width='120'
//                           time='00:01:20.750'
//                           yt:name='start'/>
//          <media:thumbnail url='http://i.ytimg.com/vi/IwZtD0XB7JQ/2.jpg'
//                           height='90'
//                           width='120'
//                           time='00:02:41.500'
//                           yt:name='middle'/>
//          <media:thumbnail url='http://i.ytimg.com/vi/IwZtD0XB7JQ/3.jpg'
//                           height='90'
//                           width='120'
//                           time='00:04:02.250'
//                           yt:name='end'/>
//          <media:title type='plain'>김광석-너무 아픈사랑은 사랑이 아니었음을</media:title>
//          <yt:duration seconds='323'/>
//          <yt:uploaded>2010-09-08T00:28:34.000Z</yt:uploaded>
//          <yt:uploaderId>tk7vwplRl0X-V7coWRt1EA</yt:uploaderId>
//          <yt:videoid>IwZtD0XB7JQ</yt:videoid>
//      </media:group>
//      <gd:rating average='4.9372935'
//                 max='5'
//                 min='1'
//                 numRaters='1212'
//                 rel='http://schemas.google.com/g/2005#overall'/>
//      <yt:statistics favoriteCount='1266'
//                     viewCount='803963'/>
//      <yt:rating numDislikes='19'
//                 numLikes='1193'/>
//  </entry>

//
// Notation
//     //P <= comment out for performance.
//
public class YTSearchApi {
    public static final int NR_SEARCH_MAX = 50; // See google API document for this value.

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

        public Media        media       = new Media();
        public Statistics   stat        = new Statistics();
        public GDRating     gdRating    = new GDRating();
        public YTRating     ytRating    = new YTRating();

        public class Media {
            public String   title           = "";
            public String   description     = "";
            public String   thumbnailUrl    = ""; // smallest thumbnail url
            public String   uploadedTime    = "";
            public Content  content         = new Content();
            public Credit   credit          = new Credit();

            public class Content {
                // content for yt:format='6'
                public String url       = "";
                public String playTime  = "";  // seconds
            }

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
    getFeedUrl(String word, int start, int maxCount) {
        eAssert(0 < start && 0 < maxCount && maxCount <= NR_SEARCH_MAX);
        // http://gdata.youtube.com/feeds/mobile/videos?q=김광석&start-index=1&max-results=50&client=ytapi-youtube-search&format=6&v=2
        word = word.replaceAll("\\s+", "+");
        return "http://gdata.youtube.com/feeds/mobile/videos?q="
                + Uri.encode(word, "+")
                + "&start-index=" + start
                + "&max-results=" + maxCount
                + "&client=ytapi-youtube-search&format=6&v=2";
    }

    /**
     * Is this valid entry that can be handled by YoutubeMusicPlayer?
     * @param en
     * @return
     */
    private static boolean
    verifyEntry(Entry en) {
        return Utils.isValidValue(en.media.content.url)
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
        if (null == nItem || !"default".equals(nItem.getNodeValue()))
            return Err.NO_ERR; // ignore other thumbnails.

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
    parseEntryMediaContent(Node n, Entry.Media.Content en) {
        NamedNodeMap nnm = n.getAttributes();
        Node nItem = nnm.getNamedItem("yt:format");
        if (null == nItem || !"6".equals(nItem.getNodeValue()))
            return Err.NO_ERR;

        nItem = nnm.getNamedItem("url");
        if (null == nItem)
            return Err.NO_ERR;
        en.url = nItem.getNodeValue();

        nItem = nnm.getNamedItem("duration");
        if (null != nItem)
            en.playTime = nItem.getNodeValue();

        return Err.NO_ERR;
    }

    private static Err
    parseEntryMedia(Node n, Entry.Media en) {
        n = n.getFirstChild();
        while (null != n) {
            if ("media:content".equals(n.getNodeName()))
                parseEntryMediaContent(n, en.content);
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
            if ("media:group".equals(n.getNodeName()))
                parseEntryMedia(n, en.media);
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
