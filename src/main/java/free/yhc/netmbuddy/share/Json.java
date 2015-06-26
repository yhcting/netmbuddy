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

package free.yhc.netmbuddy.share;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.utils.Utils;

//============================================================================
//[ Data File Format (JSON) ]
//{
//----- Meta data -----
//meta : {
//    magic : Magic string to confirm that this is NetMBuddy compatible.
//    type : Json Object의 type. 여러가지 형태를 제공하기 위해서..
//    version : sharing format version of this type.
//        : "playlist"
//    time : exporting된 시간.(milliseconds)
//}
//
//----- Data -----
//playlist : {
//    title : playlist title
//    videos : [
//        {
//            ytid: 11-character youtube video id
//            title : title of the video (Exporter may change the video title.)
//            playtime:
//            volume: volume
//            bookmarks : bookmarks
//        }
//        ...
//    ]
//}
//============================================================================
class Json {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(Json.class);

    // Field name constants.
    static final String MAGIC       = "!*&@$$#$%~NetMBuddy!@@@@!%^^*#$$##";
    static final String FVERSION    = "version";
    static final String FMETA       = "meta";
    static final String FPLAYLIST   = "playlist";
    static final String FMAGIC      = "magic";
    static final String FTYPE       = "type";
    static final String FTHUMBNAIL_YTVID = "thumbnail_ytvid";
    static final String FTIME       = "time";
    static final String FYTID       = "ytid";
    static final String FAUTHOR     = "author";
    static final String FVOLUME     = "volume";
    static final String FPLAYTIME   = "playtime";
    static final String FBOOKMARKS  = "bookmarks";
    static final String FTITLE      = "title";
    static final String FVIDEOS     = "videos";

    /**
     * @param jo
     *   'meta' object
     * @return
     */
    static boolean
    verify(JSONObject jo) {
        try {
            return MAGIC.equals(jo.getString(FMAGIC));
        } catch (JSONException e) {
            return false;
        }
    }

    static JSONObject
    createMetaJson(Share.Type type) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(FMAGIC, MAGIC);
            jo.put(FVERSION, type.getVersion());
            jo.put(FTYPE, type.name());
            jo.put(FTIME, System.currentTimeMillis());
        } catch (JSONException e) {
            jo = null;
        }
        return jo;
    }

    static JSONObject
    videoToJson(long vid) {
        final int COLI_VIDEOID  = 0;
        final int COLI_TITLE    = 1;
        final int COLI_AUTHOR   = 2;
        final int COLI_VOLUME   = 3;
        final int COLI_PLAYTIME = 4;
        final int COLI_BOOKMARKS= 5;
        Cursor c = DB.get().queryVideo(vid,
                                       new ColVideo[] {
                ColVideo.VIDEOID,
                ColVideo.TITLE,
                ColVideo.AUTHOR,
                ColVideo.VOLUME,
                ColVideo.PLAYTIME,
                ColVideo.BOOKMARKS
        });

        if (!c.moveToFirst()) {
            c.close();
            return null;
        }

        JSONObject jo = new JSONObject();
        try {
            jo.put(FYTID,     c.getString(COLI_VIDEOID));
            jo.put(FTITLE,    c.getString(COLI_TITLE));

            // NOTE
            // Below field - author - is newly added at Database version 2
            // So, we cannot sure that DB includes valid field value for them.
            if (Utils.isValidValue(c.getString(COLI_AUTHOR)))
                jo.put(FAUTHOR,   c.getString(COLI_AUTHOR));

            jo.put(FPLAYTIME, c.getInt(COLI_PLAYTIME));
            jo.put(FBOOKMARKS, c.getString(COLI_BOOKMARKS));
            int vol = c.getInt(COLI_VOLUME);
            if (Policy.DEFAULT_VIDEO_VOLUME != vol)
                jo.put(FVOLUME,   vol);
        } catch (JSONException e) {
            jo = null;
        }
        c.close();

        return jo;
    }

    static JSONObject
    playlistToJson(long plid) {
        final int COLI_ID   = 0;
        Cursor c = DB.get().queryVideos(plid,
                                        new ColVideo[] { ColVideo.ID },
                                        null,
                                        false);
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }

        JSONObject jo = new JSONObject();
        try {
            jo.put(FTITLE, DB.get().getPlaylistInfo(plid, ColPlaylist.TITLE));
            String thumbnailYtvid = (String)DB.get().getPlaylistInfo(plid, ColPlaylist.THUMBNAIL_YTVID);
            if (Utils.isValidValue(thumbnailYtvid))
                jo.put(FTHUMBNAIL_YTVID, thumbnailYtvid);

            JSONArray  jarr = new JSONArray();
            do {
                JSONObject jov = videoToJson(c.getLong(COLI_ID));
                eAssert(null != jov);
                jarr.put(jov);
            } while (c.moveToNext());
            jo.put(FVIDEOS, jarr);
        } catch (JSONException e) {
            jo = null;
        }
        c.close();

        return jo;
    }

}
