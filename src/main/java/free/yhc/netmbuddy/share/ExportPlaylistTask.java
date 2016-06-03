/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015, 2016
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
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import free.yhc.baselib.Logger;
import free.yhc.baselib.async.TmTask;
import free.yhc.abaselib.util.AUtil;
import free.yhc.baselib.util.FileUtil;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.core.PolicyConstant;
import free.yhc.netmbuddy.db.DB;


// Format
// JSON-ROOT
//     - Json.FMETA <JSONObject>
//         * meta fields
//     - Json.FPLAYLIST <JSONObject>
//         * Json.sPlaylistProjectionForShare fields
//         * Json.FVIDEOS <JSONArray>
//             + Json.sVideoProjectionForShare fields
public class ExportPlaylistTask extends TmTask<Void> {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(ExportPlaylistTask.class, Logger.LOGLV_DEFAULT);

    private final File mFout;
    private final long mPlid;

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    protected ExportPlaylistTask(@NonNull File fout, long plid) {
        mFout = fout;
        mPlid = plid;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static ExportPlaylistTask
    create(@NonNull File fout, long plid) {
        return new ExportPlaylistTask(fout, plid);
    }

    /**
     * @throws IOException (FileNotFoundException)
     */
    @Override
    @NonNull
    protected Void
    doAsync() throws IOException {
        DB db = DB.get();

        // Meta data
        // -------------
        DataModel.Meta dmmeta = new DataModel.Meta();
        dmmeta.type = Share.Type.PLAYLIST;

        // Playlist data
        // -------------
        DataModel.Playlist dmpl = new DataModel.Playlist();
        try (Cursor c = db.queryPlaylist(mPlid, DataModel.Playlist.sDBProjection)) {
            if (!c.moveToFirst())
                throw new IllegalArgumentException();
            dmpl.set(c);
        }

        // Video data in this playlist
        // ---------------------------
        DataModel.Video[] dmvs;
        try (Cursor c = db.queryVideos(
                mPlid,
                DataModel.Video.sDBProjection,
                null,
                false)) {
            if (!c.moveToFirst())
                throw new IllegalArgumentException();
            dmvs = new DataModel.Video[c.getCount()];
            int i = 0;
            do {
                dmvs[i] = new DataModel.Video();
                dmvs[i].set(c);
            } while (c.moveToNext());
        }

        DataModel.Root dr = new DataModel.Root();
        dr.meta = dmmeta;
        dr.pl = dmpl;
        dr.pl.videos = dmvs;

        JSONObject jo = dr.toJson();
        if (null == jo)
            throw new AssertionError();

        String shareName = FileUtil.pathNameEscapeString(
                AUtil.getResString(R.string.playlist)
                        + "_"
                        + dr.pl.title
                        + "."
                        + PolicyConstant.SHARE_FILE_EXTENTION);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(mFout));
             ByteArrayInputStream bais = new ByteArrayInputStream(jo.toString().getBytes("UTF-8"))) {
            FileUtil.zip(zos, bais, shareName);
        }
        return null;
    }

}
