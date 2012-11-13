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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.utils.FileUtils;
import free.yhc.netmbuddy.utils.Utils;

//============================================================================
//[ Data File Format (JSON) ]
//  {
//  ----- Meta data -----
//  meta : {
//      magic : Magic string to confirm that this is NetMBuddy compatible.
//      type : Json Object의 type. 여러가지 형태를 제공하기 위해서..
//      version : sharing format version of this type.
//          : "playlist"
//      time : exporting된 시간.(milliseconds)
//  }
//
//  ----- Data -----
//  playlist : {
//      title : playlist title
//      videos : [
//          {
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
    private static final String FVERSION    = "version";
    private static final String FMETA       = "meta";
    private static final String FPLAYLIST   = "playlist";
    private static final String FMAGIC      = "magic";
    private static final String FTYPE       = "type";
    private static final String FTIME       = "time";
    private static final String FYTID       = "ytid";
    private static final String FVOLUME     = "volume";
    private static final String FTITLE      = "title";
    private static final String FVIDEOS     = "videos";

    public static enum Err {
        NO_ERR,
        IO_FILE,
        PARAMETER,
        INVALID_SHARE,
        UNSUPPORTED_VERSION,
        DB_UNKNOWN,
        UNKNOWN
    }

    public static class LocalException extends java.lang.Exception {
        static final long serialVersionUID = 0; // to make compiler be happy

        private final Err   _mErr;

        public LocalException(Err err) {
            _mErr = err;
        }

        public Err
        error() {
            return _mErr;
        }
    }

    // Sharing type
    public static enum Type {
        PLAYLIST (1);

        private final int _mVersion;
        Type(int version) {
            _mVersion = version;
        }

        int
        getVersion() {
            return _mVersion;
        }
    }

    public static class ImportResult {
        Err     err;        // result of import
        Type    type;       // type of importing data
        int     success;    // # of successfully imported
        int     fail;       // # of fails to import.
    }

    private static class ImportVideoJob extends MultiThreadRunner.Job<Err> {
        private final long          _mPlid; // playlist id to import to
        private final JSONObject    _mJov;  // Video JSON Object.

        ImportVideoJob(boolean interruptOnCancel,
                       float progWeight,
                       JSONObject jov,
                       long plid) {
            super(interruptOnCancel, progWeight);
            _mPlid = plid;
            _mJov = jov;
        }

        @Override
        public void
        onPreRun() { }

        @Override
        public Err
        doJob() {
            return Err.NO_ERR;
        }

        @Override
        public void
        cancel() { }

        @Override
        public void
        onCancelled() { }

        @Override
        public void
        onPostRun(Err result) { }

        @Override
        public void
        onProgress(int prog) { }
    }

    // ========================================================================
    //
    // Common Utilities
    //
    // ========================================================================
    /**
     * @param jo
     *   'meta' object
     * @return
     */
    private static boolean
    verify(JSONObject jo) {
        try {
            return jo.getString(FMAGIC).equals(FMAGIC);
        } catch (JSONException e) {
            return false;
        }
    }

    private static JSONObject
    createMetaJson(Type type) {
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

    private static JSONObject
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

        JSONObject jo = new JSONObject();
        try {
            jo.put(FYTID,     c.getString(COLI_VIDEOID));
            jo.put(FTITLE,    c.getString(COLI_TITLE));
            int vol = c.getInt(COLI_VOLUME);
            if (Policy.DEFAULT_VIDEO_VOLUME != vol)
                jo.put(FVOLUME,   vol);
        } catch (JSONException e) {
            jo = null;
        }
        c.close();

        return jo;
    }

    private static JSONObject
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

        JSONObject jo = new JSONObject();
        try {
            jo.put(FTITLE, DB.get().getPlaylistInfo(plid, DB.ColPlaylist.TITLE));
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

    private static String
    getUniqueSharePlaylistTitle(String shareTitle) {
        DB db = DB.get();
        String baseTitle = shareTitle + "- share";
        int i = 0;
        while (db.containsPlaylist(baseTitle + i++));
        return baseTitle + --i;
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    private static Err
    exportShareJson(ZipOutputStream zos, JSONObject jo, String shareName) {
        ByteArrayInputStream bais;
        try {
            bais = new ByteArrayInputStream(jo.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return Err.UNKNOWN;
        }

        try {
            FileUtils.zip(zos, bais, shareName);
        } catch (IOException e) {
            return Err.IO_FILE;
        } finally {
            try {
                bais.close();
            } catch (IOException ignored) { }
        }
        return Err.NO_ERR;
    }

    /**
     *
     * @param ir
     * @param jo
     *   Playlist JSON object.
     * @throws JSONException
     * @throws LocalException
     */
    private static void
    importSharePlaylist(ImportResult ir, JSONObject jo)
            throws JSONException, LocalException {
        DB db = DB.get();
        JSONArray jarr = jo.getJSONArray(FVIDEOS);

        String title = getUniqueSharePlaylistTitle(jo.getString(FTITLE));
        long plid = db.insertPlaylist(title, "");
        if (plid < 0)
            throw new LocalException(Err.DB_UNKNOWN);




    }

    // ========================================================================
    //
    // Interfaces
    //
    // ========================================================================
    public static ImportResult
    importShare(ZipInputStream zis) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImportResult ir = new ImportResult();
        try {
            FileUtils.unzip(baos, zis);
            JSONObject rootJo = new JSONObject(baos.toString("UTF-8"));
            // ---------------------------------------------
            // Handling Meta data
            // ---------------------------------------------
            JSONObject jo = rootJo.getJSONObject(FMETA);
            if (!verify(jo))
                throw new LocalException(Err.INVALID_SHARE);

            Type ty = Type.valueOf(jo.getString(FTYPE));
            switch (ty) {
            case PLAYLIST:
                importSharePlaylist(ir, rootJo.getJSONObject(FPLAYLIST));
            }

            jo = rootJo.getJSONObject(FPLAYLIST);

        } catch (IllegalArgumentException e) {
            ir.err = Err.INVALID_SHARE;
        } catch (JSONException e) {
            ir.err = Err.INVALID_SHARE;
        } catch (IOException e) {
            ir.err = Err.INVALID_SHARE;
        } catch (LocalException e) {
            ir.err = e.error();
        }
        return ir;
    }

    public static Err
    exportSharePlaylist(String file, long plid) {
        JSONObject jsonPl = playlistToJson(plid);
        JSONObject jsonMeta = createMetaJson(Type.PLAYLIST);
        JSONObject jo = new JSONObject();

        if (null == jsonPl)
            return Err.PARAMETER;

        eAssert(null != jsonMeta);
        try {
            jo.put(FMETA, jsonMeta);
            jo.put(FPLAYLIST, jsonPl);
        } catch (JSONException e) {
            return Err.UNKNOWN;
        }

        String shareName = "";
        try {
            shareName = FileUtils.pathNameEscapeString(Utils.getResText(R.string.playlist)
                                                       + "_"
                                                       + jo.getString(FTITLE)
                                                       + ".netmbuddy");
        } catch (JSONException e) {
            return Err.UNKNOWN;
        }

        ZipOutputStream zos;
        try {
            zos = new ZipOutputStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            return Err.PARAMETER;
        }

        Err err = exportShareJson(zos, jo, shareName);
        try {
            zos.close();
        } catch (IOException ignored) {
            err = Err.IO_FILE;
        }

        if (Err.NO_ERR != err)
            new File(file).delete();

        return Err.NO_ERR;
    }
}
