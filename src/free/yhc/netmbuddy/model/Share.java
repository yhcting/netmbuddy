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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.utils.FileUtils;
import free.yhc.netmbuddy.utils.Utils;
import free.yhc.netmbuddy.utils.YTUtils;

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
//              playtime:
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
    private static final String FPLAYTIME   = "playtime";
    private static final String FTITLE      = "title";
    private static final String FVIDEOS     = "videos";


    public static interface OnProgressListener {
        void onProgress(float prog);
    }

    public static interface ImporterI {
        /**
         * Synchronous call.
         * @param mtrunner
         */
        void            run(OnProgressListener listener);
        void            cancel();
        ImportResult    result();
    }

    public static interface ExporterI {
        void            run();
        Err             result();
    }

    public static enum Err {
        NO_ERR,
        IO_FILE,
        PARAMETER,
        INTERRUPTED,
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
        public Err              err     = Err.UNKNOWN;        // result of import
        public Type             type    = Type.PLAYLIST;       // type of importing data
        // # of successfully imported
        public AtomicInteger    success = new AtomicInteger(0);
        // # of fails to import.
        public AtomicInteger    fail    = new AtomicInteger(0);
    }


    private static class ImportVideoJob extends MultiThreadRunner.Job<Err> {
        private final long          _mPlid; // playlist id to import to
        private final JSONObject    _mJov;  // Video JSON Object.
        private final AtomicInteger _mSuccess;
        private final AtomicInteger _mFail;

        ImportVideoJob(float progWeight,
                       JSONObject jov,
                       long plid,
                       AtomicInteger success,
                       AtomicInteger fail) {
            super(true, progWeight);
            _mPlid = plid;
            _mJov = jov;
            _mSuccess = success;
            _mFail = fail;
        }

        @Override
        public Err
        doJob() {
            Err err = Err.UNKNOWN;
            try {
                String title = _mJov.getString(FTITLE);
                String ytvid = _mJov.getString(FYTID);
                if (!YTUtils.verifyYoutubeVideoId(ytvid))
                    return Err.INVALID_SHARE;

                int playtm = _mJov.getInt(FPLAYTIME);
                int volume = Policy.DEFAULT_VIDEO_VOLUME;
                if (_mJov.has(FVOLUME))
                    volume = _mJov.getInt(FVOLUME);

                // NOTE
                // Getting thumbnail URL from youtube video id requires downloanding and parsing.
                // It takes too much time.
                // So, a kind of HACK is used to get thumbnail URL from youtube video id.
                // see comments of 'YTHacker.getYtVideoThumbnailUrl()' for details.
                if (YTUtils.insertVideoToPlaylist(_mPlid,
                                                  ytvid,
                                                  title,
                                                  "",
                                                  YTHacker.getYtVideoThumbnailUrl(ytvid),
                                                  playtm,
                                                  volume))
                    err = Err.NO_ERR;
            } catch (JSONException e) {
                return Err.INVALID_SHARE;
            } finally {
                if (Err.NO_ERR == err)
                    _mSuccess.incrementAndGet();
                else
                    _mFail.incrementAndGet();
            }
            return err;
        }
    }

    private static class Importer implements ImporterI {
        private final ZipInputStream    _mZis;
        private final MultiThreadRunner _mMtrunner;
        private final AtomicReference<ImportResult> _mIr
            = new AtomicReference<ImportResult>(new ImportResult());

        Importer(ZipInputStream zis) {
            _mZis = zis;
            _mMtrunner =  new MultiThreadRunner(Utils.getUiHandler(),
                                                Policy.YTSEARCH_MAX_LOAD_THUMBNAIL_THREAD);
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
        importSharePlaylist(ImportResult ir,
                            JSONObject jo,
                            MultiThreadRunner mtrunner)
                throws JSONException, LocalException, InterruptedException {
            DB db = DB.get();
            JSONArray jarr = jo.getJSONArray(FVIDEOS);

            String title = getUniqueSharePlaylistTitle(jo.getString(FTITLE));
            long plid = db.insertPlaylist(title, "");
            if (plid < 0)
                throw new LocalException(Err.DB_UNKNOWN);

            for (int i = 0; i < jarr.length(); i++) {
                mtrunner.appendJob(new ImportVideoJob(1.0f / jarr.length(),
                                                      jarr.getJSONObject(i),
                                                      plid,
                                                      ir.success,
                                                      ir.fail));
            }

            mtrunner.waitAllDone();
        }

        @Override
        public void
        run(final OnProgressListener listener) {
            _mMtrunner.setOnProgressListener(new MultiThreadRunner.OnProgressListener() {
                @Override
                public void onProgress(float prog) {
                    listener.onProgress(prog);
                }
            });

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImportResult ir = new ImportResult();
            try {
                FileUtils.unzip(baos, _mZis);
                JSONObject rootJo = new JSONObject(baos.toString("UTF-8"));
                // ---------------------------------------------
                // Handling Meta data
                // ---------------------------------------------
                JSONObject jo = rootJo.getJSONObject(FMETA);
                if (!verify(jo))
                    throw new LocalException(Err.INVALID_SHARE);

                ir.type = Type.valueOf(jo.getString(FTYPE));
                switch (ir.type) {
                case PLAYLIST:
                    importSharePlaylist(ir, rootJo.getJSONObject(FPLAYLIST), _mMtrunner);
                }

                ir.err = Err.NO_ERR;
            } catch (IllegalArgumentException e) {
                ir.err = Err.INVALID_SHARE;
            } catch (JSONException e) {
                ir.err = Err.INVALID_SHARE;
            } catch (IOException e) {
                ir.err = Err.INVALID_SHARE;
            } catch (InterruptedException e) {
                ir.err = Err.INTERRUPTED;
            } catch (LocalException e) {
                ir.err = e.error();
            }
            _mIr.set(ir);
        }

        @Override
        public void
        cancel() {
            _mMtrunner.cancel();
        }

        @Override
        public ImportResult
        result() {
            return _mIr.get();
        }
    }

    private static class ExporterPlaylist implements ExporterI {
        private final File  _mFout;
        private final long  _mPlid;
        private final AtomicReference<Err> _mErr = new AtomicReference<Err>(Err.UNKNOWN);

        ExporterPlaylist(File fout, long plid) {
            _mFout = fout;
            _mPlid = plid;
        }

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

        @Override
        public void
        run() {
            JSONObject jsonPl = playlistToJson(_mPlid);
            JSONObject jsonMeta = createMetaJson(Type.PLAYLIST);
            JSONObject jo = new JSONObject();

            if (null == jsonPl) {
                _mErr.set(Err.PARAMETER);
                return;
            }

            eAssert(null != jsonMeta);
            try {
                jo.put(FMETA, jsonMeta);
                jo.put(FPLAYLIST, jsonPl);
            } catch (JSONException e) {
                _mErr.set(Err.UNKNOWN);
                return;
            }

            String shareName = "";
            try {
                shareName = FileUtils.pathNameEscapeString(Utils.getResText(R.string.playlist)
                                                           + "_"
                                                           + jsonPl.getString(FTITLE)
                                                           + "."
                                                           + Policy.SHARE_FILE_EXTENTION);
            } catch (JSONException e) {
                _mErr.set(Err.UNKNOWN);
                return;
            }

            ZipOutputStream zos;
            try {
                zos = new ZipOutputStream(new FileOutputStream(_mFout));
            } catch (FileNotFoundException e) {
                _mErr.set(Err.IO_FILE);
                return;
            }

            Err err = exportShareJson(zos, jo, shareName);
            try {
                zos.close();
            } catch (IOException e) {
                _mErr.set(Err.IO_FILE);
                return;
            }

            _mErr.set(err);
        }

        @Override
        public Err
        result() {
            return _mErr.get();
        }
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
            return MAGIC.equals(jo.getString(FMAGIC));
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
        final int COLI_PLAYTIME = 3;
        Cursor c = DB.get().queryVideo(vid,
                                       new DB.ColVideo[] {
                DB.ColVideo.VIDEOID,
                DB.ColVideo.TITLE,
                DB.ColVideo.VOLUME,
                DB.ColVideo.PLAYTIME
        });

        if (!c.moveToFirst()) {
            c.close();
            return null;
        }

        JSONObject jo = new JSONObject();
        try {
            jo.put(FYTID,     c.getString(COLI_VIDEOID));
            jo.put(FTITLE,    c.getString(COLI_TITLE));
            jo.put(FPLAYTIME, c.getInt(COLI_PLAYTIME));
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


    // ========================================================================
    //
    // Interfaces
    //
    // ========================================================================
    public static ImporterI
    buildImporter(ZipInputStream zis) {
        return new Importer(zis);
    }

    public static ExporterI
    buildPlayerlistExporter(File file, long plid) {
        return new ExporterPlaylist(file, plid);
    }
}
