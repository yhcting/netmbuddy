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
