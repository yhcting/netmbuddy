package free.yhc.netmbuddy.share;

import android.database.SQLException;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipInputStream;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.async.HelperHandler;
import free.yhc.baselib.async.Task;
import free.yhc.baselib.exception.UnsupportedFormatException;
import free.yhc.abaselib.util.AUtil;
import free.yhc.baselib.util.FileUtil;
import free.yhc.netmbuddy.Err;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.utils.JsonUtil;
import free.yhc.netmbuddy.utils.UxUtil;

public class ImportJob extends Task<ImportJob.Result> {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(ImportJob.class, Logger.LOGLV_DEFAULT);

    private final AtomicReference<Task<Result>> mTask = new AtomicReference<>(null);
    private final ZipInputStream mZis;

    ///////////////////////////////////////////////////////////////////////////
    //
    // Upgrade old-JSON-format to latest one
    //
    ///////////////////////////////////////////////////////////////////////////
    public static class Result {
        public String message = "";
        // # of successfully imported
        public AtomicInteger success = new AtomicInteger(0);
        // # of fails to import.
        public AtomicInteger fail = new AtomicInteger(0);
    }

    @NonNull
    private static JSONObject
    upgradeTo2(@NonNull JSONObject jo)
            throws JSONException, UnsupportedFormatException {
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

        JSONObject pl = jo.getJSONObject(DataModel.Root.FPLAYLIST);
        JSONArray varr = pl.getJSONArray(DataModel.Playlist.FVIDEOS);
        // upgrade to v2 format for videos
        for (int i = 0; i < varr.length(); i++) {
            JSONObject o = varr.getJSONObject(i);
            // replace keys.
            for (String[] m : video_v1Tov2)
                if (!JsonUtil.jReplaceKey(o, m[0], m[1]))
                    // something wrong during replacing key. JSON format is unrecognizable!
                    throw new UnsupportedFormatException();
        }

        // upgrade to v2 format for playlists
        for (String[] m : pl_v1Tov2) {
            if (!JsonUtil.jReplaceKey(pl, m[0], m[1]))
                // something wrong during replacing key. JSON format is unrecognizable!
                throw new UnsupportedFormatException();
        }
        return jo;
    }

    @NonNull
    private static JSONObject
    upgrade(@NonNull JSONObject jo) throws JSONException, UnsupportedFormatException {
        JSONObject jometa = jo.getJSONObject(DataModel.Root.FMETA);
        int ov = jometa.getInt(DataModel.Meta.FVERSION); // object version
        while (ov < DataModel.DATAMODEL_VERSION) {
            switch (ov) {
            case 1: jo = upgradeTo2(jo); break;
            }
            ov++;
        }
        return jo;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    private CharSequence
    getReportText(boolean cancelled, Result r) {
        int success;
        int fail;
        if (null != r) {
            success = r.success.get();
            fail = r.fail.get();
        } else {
            if (DBG) P.e("Unexpected Error (returned result is null!)\n" +
                                 "   recovered");
            return "<null> data";
        }

        CharSequence text = " [ " + AUtil.getResString(R.string.app_name) + " ]\n"
                + AUtil.getResString(R.string.import_) + " : "
                + AUtil.getResString(cancelled?
                                      R.string.cancelled:
                                      R.string.done)
                + "\n"
                + r.message;

        return text + "\n"
                + "  " + AUtil.getResString(R.string.done) + " : " + success + "\n"
                + "  " + AUtil.getResString(R.string.error) + " : " + fail;
    }


    /**
     */
    private Result
    doExecute() throws IOException, JSONException, UnsupportedFormatException, SQLException {
        JSONObject rootJo;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            FileUtil.unzip(baos, mZis);
            rootJo = new JSONObject(baos.toString("UTF-8"));
        }
        // ---------------------------------------------
        // Upgrade to current data format
        // ---------------------------------------------
        rootJo = upgrade(rootJo);
        DataModel.Root dr = new DataModel.Root();
        dr.set(rootJo);
        if (!dr.verify())
            throw new UnsupportedFormatException();
        // ---------------------------------------------
        // Handling Meta data
        // ---------------------------------------------
        Task<Result> t;
        switch (dr.meta.type) {
        case PLAYLIST:
            t = ImportPlaylistTask.create(dr.pl);
            break;
        default:
            throw new UnsupportedFormatException();
        }

        try {
            t.addEventListener(HelperHandler.get(), new Task.EventListener<Task, Result>(){
                public void
                onProgressInit(@NonNull Task task, long maxProgress) {
                    ImportJob.this.publishProgressInit(maxProgress);
                }
                public void
                onProgress(@NonNull Task task, long progress) {
                    ImportJob.this.publishProgress(progress);
                }
            });
            /* Order of below two lines are important.
             * 'onEarlyCancel' and 'isCancel' guarantee that
             * "below 'isCancel' is NOT executed after 'onEarlyCancel'".
             */
            mTask.set(t);
            if (isCancel())
                throw new InterruptedException();
            return t.startSync();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void
    onEarlyCancel(boolean started, Object param) {
        super.onEarlyCancel(started, param);
        Task<Result> t = mTask.get();
        if (null != t)
            t.cancel();
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    protected ImportJob(@NonNull ZipInputStream zis) {
        mZis = zis;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static ImportJob
    create(@NonNull ZipInputStream zis) {
        return new ImportJob(zis);
    }

    @Override
    public Result
    doAsync() {
        CharSequence text;
        try {
            Result r = doExecute();
            text = getReportText(isCancel(), r);
        } catch (SQLException e) {
            text = AUtil.getResText(Err.DB_UNKNOWN.getMessage());
        } catch (JSONException | UnsupportedFormatException e) {
            text = AUtil.getResText(Err.INVALID_DATA.getMessage());
        } catch (IOException e) {
            text = AUtil.getResText(Err.IO_FILE.getMessage());
        } catch (Exception e) {
            if (DBG) P.w(P.stackTrace(e));
            text = AUtil.getResText(Err.UNKNOWN.getMessage());
        }
        final CharSequence text_ = text;
        AppEnv.getUiHandler().post(new Runnable() {
            @Override
            public void run() {
                UxUtil.showTextToast(text_);
            }
        });
        return null;
    }
}
