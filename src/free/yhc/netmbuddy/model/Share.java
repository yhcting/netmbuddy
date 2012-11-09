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

import static free.yhc.netmbuddy.model.Utils.eAssert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;

//============================================================================
//[ Data File Format (JSON) ]
//  {
//  ----- Meta data -----
//  meta : {
//      magic : Magic string to confirm that this is NetMBuddy compatible.
//      version : meta data format version
//      type : Json Object의 type. 여러가지 형태를 제공하기 위해서..
//          : "playlist"
//      time : exporting된 시간.(milliseconds)
//  }
//
//  ----- Data -----
//  playlist : {
//      version : playlist format version
//      title : playlist title
//      ??? thumbnal : playlist thumbnail data <= for future use... not used yet.
//      videos : [
//          {
//              version : video format version
//              ytid: 11-character youtube video id
//              title : title of the video (Exporter may change the video title.)
//              volume: volume
//          }
//          ...
//      ]
//  }
//============================================================================
public class Share {
    // Field name constants.
    private static final String MAGIC   = "!*&@$$#$%~NetMBuddy!@@@@!%^^*#$$##";

    public static enum Err {
        NO_ERR,
        INVALID_FILE,
        UNSUPPORTED_VERSION
    }

    public static class Meta {
        static final int    VERSION    = 1;

        public enum Type {
            PLAYLIST,
        }

        String  magic   = "";
        int     version = 0;
        Type    type    = Type.PLAYLIST;
        long    time    = 0;
    }

    public static class Video {
        static final int    VERSION    = 1; // Data version of video object

        String  ytid    = "";
        String  title   = "";
        int     volume  = Policy.DEFAULT_VIDEO_VOLUME;
    }

    public static class Playlist {
        static final int    VERSION    = 1; // Data version of playlist type.

        String  title   = "";
        Video[] videos  = new Video[0];
    }

    private static class Json {
        private static final String FVERSION    = "version";
        private static final String FMETA       = "meta";
        private static final String FMAGIC      = "magic";
        private static final String FTYPE       = "type";
        private static final String FTIME       = "time";
        private static final String FYTID       = "ytid";
        private static final String FVOLUME     = "volume";
        private static final String FTITLE      = "title";
        private static final String FVIDEOS     = "videos";

        // --------------------------------------------------------------------
        // JSON Values
        // --------------------------------------------------------------------
        static boolean
        verify(JSONObject jobj) {
            try {
                JSONObject meta = jobj.getJSONObject(FMETA);
                return meta.getString(FMAGIC).equals(FMAGIC);
            } catch (JSONException e) {
                return false;
            }
        }

        static JSONObject
        createMeta(Meta.Type type) {
            JSONObject jobj = new JSONObject();
            try {
                jobj.put(FMAGIC, MAGIC);
                jobj.put(FVERSION, Meta.VERSION);
                jobj.put(FTYPE, type.name());
                jobj.put(FTIME, System.currentTimeMillis());
            } catch (JSONException e) {
                jobj = null;
            }
            return jobj;
        }

        static JSONObject
        videoToJson(long vid) {
            final int COLI_VIDEOID  = 0;
            final int COLI_TITLE    = 1;
            final int COLI_VOLUME   = 2;
            Cursor c = DB.get().queryVideo(vid,
                                           new DB.ColVideo[] {
                    DB.ColVideo.VIDEOID,
                    DB.ColVideo.TITLE,
                    DB.ColVideo.VOLUME
            });

            if (!c.moveToFirst()) {
                c.close();
                return null;
            }

            JSONObject jobj = new JSONObject();
            try {
                jobj.put(FVERSION,  Video.VERSION);
                jobj.put(FYTID,     c.getString(COLI_VIDEOID));
                jobj.put(FTITLE,    c.getString(COLI_TITLE));
                jobj.put(FVOLUME,   c.getString(COLI_VOLUME));
            } catch (JSONException e) {
                jobj = null;
            }
            c.close();

            return jobj;
        }

        static Video
        JsonToVideo(JSONObject jobj) {
            try {
                Video v = new Video();
                eAssert(Video.VERSION == jobj.getInt(FVERSION));
                v.title     = jobj.getString(FTITLE);
                v.ytid      = jobj.getString(FYTID);
                v.volume    = jobj.getInt(FVOLUME);
                return v;
            } catch (JSONException e) {
                return null;
            }
        }

        static JSONObject
        playlistToJson(long plid) {
            final int COLI_ID   = 0;
            Cursor c = DB.get().queryVideos(plid,
                                            new DB.ColVideo[] { DB.ColVideo.ID },
                                            null,
                                            false);
            if (!c.moveToFirst()) {
                c.close();
                return null;
            }

            JSONObject jobj = new JSONObject();
            try {
                jobj.put(FVERSION, Playlist.VERSION);
                jobj.put(FTITLE, DB.get().getPlaylistInfo(plid, DB.ColPlaylist.TITLE));
                JSONArray  jarr = new JSONArray();
                do {
                    JSONObject jo = videoToJson(c.getLong(COLI_ID));
                    eAssert(null != jo);
                    jarr.put(jo);
                } while (c.moveToNext());
                jobj.put(FVIDEOS, jarr);
            } catch (JSONException e) {
                jobj = null;
            }
            c.close();

            return jobj;
        }

        static Playlist
        JsonToPlaylist(JSONObject jobj) {
            try {
                Playlist pl = new Playlist();
                eAssert(Playlist.VERSION == jobj.getInt(FVERSION));
                pl.title = jobj.getString(FTITLE);
                JSONArray jarr = jobj.getJSONArray(FVIDEOS);
                pl.videos = new Video[jarr.length()];
                for (int i = 0; i < jarr.length(); i++)
                    pl.videos[i] = JsonToVideo(jarr.getJSONObject(i));
                return pl;
            } catch (JSONException e) {
                return null;
            }
        }

        /*
        static Err
        parseJsonPlaylist(Playlist out, JSONObject jobj) {
            verify(jobj);
        }
        */
    } // Json
}
