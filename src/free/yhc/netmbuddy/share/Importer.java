/*****************************************************************************
 *    Copyright (C) 2012, 2013 Younghyung Cho. <yhcting77@gmail.com>
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

package free.yhc.netmbuddy.share;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import org.json.JSONException;
import org.json.JSONObject;

import free.yhc.netmbuddy.share.Share.Err;
import free.yhc.netmbuddy.share.Share.ImportPrepareResult;
import free.yhc.netmbuddy.share.Share.ImportResult;
import free.yhc.netmbuddy.share.Share.ImporterI;
import free.yhc.netmbuddy.share.Share.LocalException;
import free.yhc.netmbuddy.share.Share.OnProgressListener;
import free.yhc.netmbuddy.share.Share.Type;
import free.yhc.netmbuddy.utils.FileUtils;
import free.yhc.netmbuddy.utils.Utils;

// ============================================================================
//
// Main Importer.
// Main entry point of import.
// Has common types/functions regarding import.
//
// ============================================================================
class Importer implements ImporterI {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(Importer.class);

    private final ZipInputStream    mZis;
    // concrete importer instance (depends on share type)
    private ImporterI               mImporter = null;

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
            // Handling Meta data
            // ---------------------------------------------
            JSONObject jo = rootJo.getJSONObject(Json.FMETA);
            if (!Json.verify(jo))
                throw new LocalException(Err.INVALID_SHARE);

            Type type = Type.valueOf(jo.getString(Json.FTYPE));
            switch (type) {
            case PLAYLIST:
                mImporter = new ImporterPlaylist(rootJo.getJSONObject(Json.FPLAYLIST));
                break;
            }
            ipr = mImporter.prepare();
        } catch (IllegalArgumentException e) {
            ipr.err = Err.INVALID_SHARE;
        } catch (JSONException e) {
            ipr.err = Err.INVALID_SHARE;
        } catch (IOException e) {
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
