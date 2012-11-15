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

// ============================================================================
//
// Main Importer.
// Main entry point of import.
// Has common types/functions regarding import.
//
// ============================================================================
class Importer implements ImporterI {
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
