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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.share.Share.Err;
import free.yhc.netmbuddy.share.Share.ImportPrepareResult;
import free.yhc.netmbuddy.share.Share.ImportResult;
import free.yhc.netmbuddy.share.Share.ImporterI;
import free.yhc.netmbuddy.share.Share.LocalException;
import free.yhc.netmbuddy.share.Share.OnProgressListener;
import free.yhc.netmbuddy.utils.FileUtils;
import free.yhc.netmbuddy.utils.JsonUtils;
import free.yhc.netmbuddy.utils.Utils;

// ============================================================================
//
// Main Importer.
// Main entry point of import.
// Has common types/functions regarding import.
//
// ============================================================================
class Importer implements ImporterI {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(Importer.class);

    private final ZipInputStream mZis;
    // concrete importer instance (depends on share type)
    private ImporterI mImporter = null;

    // =======================================================================
    //
    // JSONObject upgrader.
    //
    // =======================================================================
    private static JSONObject
    upgradeTo2(JSONObject jo) {
        // See DataModel.Video.sDBProjection
        final String[][] video_v1Tov2 = {
            { "ytid", ColVideo.VIDEOID.getName() },
            { "volume", ColVideo.VOLUME.getName() },
            { "bookmarks", ColVideo.BOOKMARKS.getName() },
        };

        // See DataModel.Playlist.sDBProjection
        final String[][] pl_v1Tov2 = {
            { "title", ColPlaylist.TITLE.getName() },
            { "thumbnail_ytvid", ColPlaylist.THUMBNAIL_YTVID.getName() },
        };

        try {
            JSONObject pl = jo.getJSONObject(DataModel.Root.FPLAYLIST);
            JSONArray varr = pl.getJSONArray(DataModel.Playlist.FVIDEOS);
            // upgrade to v2 format for videos
            for (int i = 0; i < varr.length(); i++) {
                JSONObject o = varr.getJSONObject(i);
                // replace keys.
                for (String[] m : video_v1Tov2)
                    if (!JsonUtils.jReplaceKey(o, m[0], m[1]))
                        return null; // something wrong during replacing key.
            }

            // upgrade to v2 format for playlists
            for (String[] m : pl_v1Tov2) {
                if (!JsonUtils.jReplaceKey(pl, m[0], m[1]))
                    return null; // something wrong during replacing key.
            }
            return jo;
        } catch (JSONException e) {
            return null;
        }
    }

    private static JSONObject
    upgrade(JSONObject jo) {
        int ov;
        try {
            JSONObject jometa = jo.getJSONObject(DataModel.Root.FMETA);
            ov = jometa.getInt(DataModel.Meta.FVERSION); // object version
        } catch (JSONException e) {
            return null; // Invalid jason object
        }

        while (null != jo
               && ov < DataModel.DATAMODEL_VERSION) {
            switch (ov) {
                case 1: jo = upgradeTo2(jo); break;
            }
            ov++;
        }
        return jo;
    }

    // =======================================================================
    //
    //
    //
    // =======================================================================

    Importer(ZipInputStream zis) {
        mZis = zis;
    }

    @Override
    public ImportPrepareResult
    prepare() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImportPrepareResult ipr = new ImportPrepareResult();
        try {
            FileUtils.unzip(baos, mZis);
            JSONObject rootJo = new JSONObject(baos.toString("UTF-8"));
            // ---------------------------------------------
            // Upgrade to current data format
            // ---------------------------------------------
            rootJo = upgrade(rootJo);
            if (null == rootJo)
                throw new LocalException(Err.INVALID_SHARE);

            DataModel.Root dr = new DataModel.Root();
            dr.set(rootJo);
            if (!dr.verify())
                throw new LocalException(Err.INVALID_SHARE);
            // ---------------------------------------------
            // Handling Meta data
            // ---------------------------------------------
            switch (dr.meta.type) {
            case PLAYLIST:
                mImporter = new ImporterPlaylist(dr.pl);
                break;
            default:
                throw new LocalException(Err.INVALID_SHARE);
            }
            ipr = mImporter.prepare();
        } catch (JSONException | IOException | IllegalArgumentException e) {
            ipr.err = Err.INVALID_SHARE;
        } catch (LocalException e) {
            ipr.err = e.error();
        }
        return ipr;
    }

    @Override
    public ImportResult
    execute(Object arg, OnProgressListener listener) {
        return mImporter.execute(arg, listener);
    }

    @Override
    public void
    cancel() {
        mImporter.cancel();
    }
}
