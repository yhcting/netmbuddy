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
