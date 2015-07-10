/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015
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

import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static free.yhc.netmbuddy.utils.JsonUtils.jGetString;
//import static free.yhc.netmbuddy.utils.JsonUtils.jGetStrings;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetInt;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetLong;
//import static free.yhc.netmbuddy.utils.JsonUtils.jGetDouble;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetObject;
import static free.yhc.netmbuddy.utils.JsonUtils.jGetObjects;
//import static free.yhc.netmbuddy.utils.JsonUtils.jGetBoolean;
//import static free.yhc.netmbuddy.utils.Utils.eAssert;

import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.utils.JsonUtils.JsonModel;
import free.yhc.netmbuddy.utils.Utils;

public class DataModel {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(DataModel.class);

    static final int DATAMODEL_VERSION = 2; // current share data version.

    static final int INVALID_VERSION = -1;
    static final long INVALID_TIME = -1;

    // All decendents of 'JsonModel' is set as 'public' to be used via reflection.
    // ---------------------------------------------------------------------------

    public static class Meta extends JsonModel {
        static final String MAGIC = "!*&@$$#$%~NetMBuddy!@@@@!%^^*#$$##";

        static final String FVERSION = "version";
        static final String FMAGIC = "magic";
        static final String FTYPE = "type";
        static final String FTIME = "time";

        String magic = MAGIC;
        Share.Type type = null;
        int version = DATAMODEL_VERSION;
        long time = System.currentTimeMillis();

        boolean
        verify() {
            return null != magic
                   && magic.equals(Meta.MAGIC)
                   && null != type
                   && version != INVALID_VERSION
                   && version <= DATAMODEL_VERSION;
        }

        @Override
        public void
        set(JSONObject jo) {
            magic = jGetString(jo, FMAGIC);
            String tystr = jGetString(jo, FTYPE);
            if (null != tystr)
                type = Share.Type.valueOf(tystr);
            version = jGetInt(jo, FVERSION, INVALID_VERSION);
            time = jGetLong(jo, FTIME, 0);
        }

        @Override
        public JSONObject
        toJson() {
            JSONObject jo = new JSONObject();
            try {
                if (null != magic)
                    jo.put(FMAGIC, MAGIC);
                if (INVALID_VERSION != version)
                    jo.put(FVERSION, version);
                if (null != type)
                    jo.put(FTYPE, type.name());
                if (INVALID_TIME != time)
                    jo.put(FTIME, time);
            } catch (JSONException e) {
                return null;
            }
            return jo;
        }
    }

    public static class Video extends JsonModel {
        // Projection between Jason and DB
        static final ColVideo[] sDBProjection = new ColVideo[] {
            ColVideo.VIDEOID, ColVideo.BOOKMARKS, ColVideo.VOLUME };

        String ytvid = null;
        int volume = DB.INVALID_VOLUME;
        String bookmarks = null;

        @Override
        public void
        set(JSONObject jo) {
            ytvid = jGetString(jo, ColVideo.VIDEOID.getName());
            volume = jGetInt(jo, ColVideo.VOLUME.getName(), DB.INVALID_VOLUME);
            bookmarks = jGetString(jo, ColVideo.BOOKMARKS.getName());
        }

        @Override
        public JSONObject
        toJson() {
            JSONObject jo = new JSONObject();
            try {
                if (null != ytvid)
                    jo.put(ColVideo.VIDEOID.getName(), ytvid);
                if (DB.INVALID_VOLUME != volume)
                    jo.put(ColVideo.VOLUME.getName(), volume);
                if (null != bookmarks)
                    jo.put(ColVideo.BOOKMARKS.getName(), bookmarks);
            } catch (JSONException e) {
                return null;
            }
            return jo;
        }

        /**
         * @param c cursor created by using Video.sDBProjection
         */
        public void
        set(Cursor c) {
            ytvid = c.getString(c.getColumnIndex(ColVideo.VIDEOID.getName()));
            volume = c.getInt(c.getColumnIndex(ColVideo.VOLUME.getName()));
            bookmarks = c.getString(c.getColumnIndex(ColVideo.BOOKMARKS.getName()));
        }
    }

    public static class Playlist extends JsonModel {
        static final String FVIDEOS = "videos";

        static final ColPlaylist[] sDBProjection = new ColPlaylist[] {
            ColPlaylist.TITLE, ColPlaylist.THUMBNAIL_YTVID };

        String title = null;
        String thumbnail_ytvid = null;
        Video[] videos = null;

        boolean
        verify() {
            return null != title
                   && null != videos;
        }

        @Override
        public void
        set(JSONObject jo) {
            title = jGetString(jo, ColPlaylist.TITLE.getName());
            thumbnail_ytvid = jGetString(jo, ColPlaylist.THUMBNAIL_YTVID.getName());
            videos = jGetObjects(jo, FVIDEOS, Video.class);
        }

        @Override
        public JSONObject
        toJson() {
            JSONObject jo = new JSONObject();
            try {
                if (null != title)
                    jo.put(ColPlaylist.TITLE.getName(), title);
                if (null != thumbnail_ytvid)
                    jo.put(ColPlaylist.THUMBNAIL_YTVID.getName(), thumbnail_ytvid);
                if (null != videos) {
                    JSONArray ja = new JSONArray();
                    for (Video v : videos) {
                        ja.put(v.toJson());
                    }
                    jo.put(FVIDEOS, ja);
                }
            } catch (JSONException e) {
                return null;
            }
            return jo;
        }

        /**
         * @param c cursor created by using Video.sDBProjection
         */
        public void
        set(Cursor c) {
            title = c.getString(c.getColumnIndex(ColPlaylist.TITLE.getName()));
            // thumbnail_ytvid is optional field.
            thumbnail_ytvid = c.getString(c.getColumnIndex(ColPlaylist.THUMBNAIL_YTVID.getName()));
            if (!Utils.isValidValue(thumbnail_ytvid))
                thumbnail_ytvid = null;
        }

    }

    public static class Root extends JsonModel {
        static final String FMETA = "meta";
        static final String FPLAYLIST = "playlist";

        Meta meta = null;
        Playlist pl = null;

        boolean
        verify() {
            return null != meta
                   && null != pl
                   && meta.verify()
                   && pl.verify();
        }

        @Override
        public void
        set(JSONObject jo) {
            meta = jGetObject(jo, FMETA, Meta.class);
            pl = jGetObject(jo, FPLAYLIST, Playlist.class);
        }

        @Override
        public JSONObject
        toJson() {
            JSONObject jo = new JSONObject();
            try {
                if (null != meta)
                    jo.put(FMETA, meta.toJson());
                if (null != pl)
                    jo.put(FPLAYLIST, pl.toJson());
            } catch (JSONException e) {
                return null;
            }
            return jo;
        }
    }
}
