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

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.share.Share.Err;
import free.yhc.netmbuddy.share.Share.ExporterI;
import free.yhc.netmbuddy.share.Share.Type;
import free.yhc.netmbuddy.utils.FileUtils;
import free.yhc.netmbuddy.utils.Utils;

class ExporterPlaylist implements ExporterI {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(ExporterPlaylist.class);

    private final File  _mFout;
    private final long  _mPlid;

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
        JSONObject jsonPl = Json.playlistToJson(_mPlid);
        JSONObject jsonMeta = Json.createMetaJson(Type.PLAYLIST);
        JSONObject jo = new JSONObject();

        if (null == jsonPl)
            return Err.PARAMETER;

        eAssert(null != jsonMeta);
        try {
            jo.put(Json.FMETA, jsonMeta);
            jo.put(Json.FPLAYLIST, jsonPl);
        } catch (JSONException e) {
            return Err.UNKNOWN;
        }

        String shareName = "";
        try {
            shareName = FileUtils.pathNameEscapeString(Utils.getResText(R.string.playlist)
                                                       + "_"
                                                       + jsonPl.getString(Json.FTITLE)
                                                       + "."
                                                       + Policy.SHARE_FILE_EXTENTION);
        } catch (JSONException e) {
            return Err.UNKNOWN;
        }

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
