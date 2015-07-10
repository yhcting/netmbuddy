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

//import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipOutputStream;

import org.json.JSONObject;

import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.share.Share.Err;
import free.yhc.netmbuddy.share.Share.ExporterI;
import free.yhc.netmbuddy.share.Share.Type;
import free.yhc.netmbuddy.utils.FileUtils;
import free.yhc.netmbuddy.utils.Utils;

// Format
// JSON-ROOT
//     - Json.FMETA <JSONObject>
//         * meta fields
//     - Json.FPLAYLIST <JSONObject>
//         * Json.sPlaylistProjectionForShare fields
//         * Json.FVIDEOS <JSONArray>
//             + Json.sVideoProjectionForShare fields
class ExporterPlaylist implements ExporterI {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(ExporterPlaylist.class);

    private final File _mFout;
    private final long _mPlid;

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
    public Err
    execute() {
        DB db = DB.get();

        // Meta data
        // -------------
        DataModel.Meta dmmeta = new DataModel.Meta();
        dmmeta.type = Type.PLAYLIST;

        // Playlist data
        // -------------
        DataModel.Playlist dmpl = new DataModel.Playlist();
        Cursor c = null;
        try {
            c = db.queryPlaylist(_mPlid, DataModel.Playlist.sDBProjection);
            if (!c.moveToFirst())
                return Err.PARAMETER;
            dmpl.set(c);
        } finally {
            if (null != c)
                c.close();
        }

        // Video data in this playlist
        // ---------------------------
        c = db.queryVideos(_mPlid,
                           DataModel.Video.sDBProjection,
                           null,
                           false);
        if (!c.moveToFirst()) {
            c.close();
            return Err.PARAMETER;
        }

        DataModel.Video[] dmvs = new DataModel.Video[c.getCount()];
        int i = 0;
        do {
            dmvs[i] = new DataModel.Video();
            dmvs[i].set(c);
        } while (c.moveToNext());
        c.close();


        DataModel.Root dr = new DataModel.Root();
        dr.meta = dmmeta;
        dr.pl = dmpl;
        dr.pl.videos = dmvs;

        JSONObject jo = dr.toJson();
        if (null == jo)
            return Err.UNKNOWN;

        String shareName
            = FileUtils.pathNameEscapeString(Utils.getResString(R.string.playlist)
                                             + "_"
                                             + dr.pl.title
                                             + "."
                                             + Policy.SHARE_FILE_EXTENTION);
        ZipOutputStream zos;
        try {
            zos = new ZipOutputStream(new FileOutputStream(_mFout));
        } catch (FileNotFoundException e) {
            return Err.IO_FILE;
        }

        Err err = exportShareJson(zos, jo, shareName);
        try {
            zos.close();
        } catch (IOException e) {
            return Err.IO_FILE;
        }

        return err;
    }
}
