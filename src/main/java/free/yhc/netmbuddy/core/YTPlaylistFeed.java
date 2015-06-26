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


//<entry gd:etag='W/&quot;DkYEQ347eCp7I2A9WhJbEU8.&quot;'>
//  <id>tag:youtube.com,2008:user:hanitv:playlist:PLxrXSNAnHXeL1puk5TwZnVzU9c-8WTWVQ</id>
//  <published>2012-09-20T06:54:16.000Z</published>
//  <updated>2012-09-20T06:55:02.000Z</updated>
//  <category scheme='http://schemas.google.com/g/2005#kind' term='http://gdata.youtube.com/schemas/2007#playlistLink'/>
//  <title>사회통합 UCC 공모전</title>
//  <summary>한겨레사회정책연구소와 대통령소속 사회통합위원회가 공동 개최한 '차별을 녹이는 따뜻한 시선, 제3회 사회통합 공모전' 수상 작품입니다. 우리사회의 통합을 가로막는 차별과 폭력을 돌아보고 이를 극복하자는 주제를 담았습니다.</summary>
//  <content type='application/atom+xml;type=feed' src='https://gdata.youtube.com/feeds/api/playlists/PLxrXSNAnHXeL1puk5TwZnVzU9c-8WTWVQ?v=2'/>
//  <link rel='related' type='application/atom+xml' href='https://gdata.youtube.com/feeds/api/users/hanitv?v=2'/>
//  <link rel='alternate' type='text/html' href='https://www.youtube.com/playlist?list=PLxrXSNAnHXeL1puk5TwZnVzU9c-8WTWVQ'/>
//  <link rel='self' type='application/atom+xml' href='https://gdata.youtube.com/feeds/api/users/hanitv/playlists/PLxrXSNAnHXeL1puk5TwZnVzU9c-8WTWVQ?v=2'/>
//  <author>
//    <name>hanitv</name>
//    <uri>https://gdata.youtube.com/feeds/api/users/hanitv</uri>
//    <yt:userId>ugbqfMO94F9guLEb6Olb2A</yt:userId>
//  </author>
//  <yt:countHint>2</yt:countHint>
//  <media:group>
//    <media:thumbnail url='http://i.ytimg.com/vi/s5GBOpuxzII/default.jpg' height='90' width='120' yt:name='default'/>
//    <media:thumbnail url='http://i.ytimg.com/vi/s5GBOpuxzII/mqdefault.jpg' height='180' width='320' yt:name='mqdefault'/>
//    <media:thumbnail url='http://i.ytimg.com/vi/s5GBOpuxzII/hqdefault.jpg' height='360' width='480' yt:name='hqdefault'/>
//    <yt:duration seconds='0'/>
//  </media:group>
//  <yt:playlistId>PLxrXSNAnHXeL1puk5TwZnVzU9c-8WTWVQ</yt:playlistId>
//</entry>
public class YTPlaylistFeed extends YTFeed {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTPlaylistFeed.class);

    // Most of them are not used yet.
    // But for future use...
    public static class Entry extends YTFeed.Entry {
        public String   title       = "";
        public String   summary     = "";
        public String   playlistId  = "";
    }

    public static String
    getFeedUrlByUser(String user, int start, int maxCount) {
        return "https://gdata.youtube.com/feeds/api/users/"
               + Uri.encode(user, null)
               + "/playlists?v=2"
               + "&start-index=" + start
               + "&max-results=" + maxCount;

    }

    /**
     * Is this valid entry that can be handled by YoutubeMusicPlayer?
     * @param en
     * @return
     */
    private static boolean
    verifyEntry(Entry en) {
        return Utils.isValidValue(en.title)
               && Utils.isValidValue(en.playlistId);
    }

    private static Err
    parseEntry(Node n, LinkedList<Entry> entryl) {
        Entry en = new Entry();
        n = n.getFirstChild();
        while (null != n) {
            //logI("    - " + n.getNodeName());
            if ("title".equals(n.getNodeName()))
                en.title = getTextValue(n);
            else if ("summary".equals(n.getNodeName()))
                en.summary = getTextValue(n);
            else if ("media:group".equals(n.getNodeName()))
                parseMedia(n, en.media);
            else if ("yt:playlistId".equals(n.getNodeName()))
                en.playlistId = getTextValue(n);

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
