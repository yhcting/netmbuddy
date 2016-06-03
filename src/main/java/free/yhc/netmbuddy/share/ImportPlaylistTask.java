package free.yhc.netmbuddy.share;

import android.database.SQLException;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import org.json.JSONException;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import free.yhc.baselib.Logger;
import free.yhc.baselib.async.HelperHandler;
import free.yhc.baselib.async.Task;
import free.yhc.baselib.async.TmTask;
import free.yhc.baselib.async.TmTaskGroup;
import free.yhc.abaselib.util.ImgUtil;
import free.yhc.netmbuddy.core.PolicyConstant;
import free.yhc.netmbuddy.core.TaskManager;
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.utils.Util;
import free.yhc.netmbuddy.utils.YTUtil;

public class ImportPlaylistTask extends TmTask<ImportJob.Result> {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(ImportPlaylistTask.class, Logger.LOGLV_DEFAULT);

    private final DataModel.Playlist mPl;
    private final AtomicReference<TmTaskGroup> mTmtg = new AtomicReference<>(null);

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    static class ImportVideoTask extends TmTask<Void> {
        private final long _mPlid; // playlist id to import to
        private final DataModel.Video _mV;  // Video JSON Object.
        private final AtomicInteger _mSuccess;
        private final AtomicInteger _mFail;

        ImportVideoTask(DataModel.Video video,
                        long plid,
                        AtomicInteger success,
                        AtomicInteger fail) {
            _mPlid = plid;
            _mV = video;
            _mSuccess = success;
            _mFail = fail;
        }

        @Override
        protected Void
        doAsync() throws JSONException, IOException {
            Boolean success = false;
            try {
                if (!YTUtil.verifyYoutubeVideoId(_mV.ytvid))
                    throw new JSONException("");

                DMVideo v = new DMVideo();
                v.ytvid = _mV.ytvid;
                if (!YTUtil.fillYtDataAndThumbnail(v))
                    throw new ConnectException();
                v.setPreferenceData(_mV.volume, _mV.bookmarks);
                v.setPreferenceDataIfNotSet(PolicyConstant.DEFAULT_VIDEO_VOLUME, "");
                if (DB.Err.NO_ERR != DB.get().insertVideoToPlaylist(_mPlid, v))
                    throw new UnsupportedOperationException();
                success = true;
                return null;
            } finally {
                if (success)
                    _mSuccess.incrementAndGet();
                else
                    _mFail.incrementAndGet();
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    private static String
    getUniqueSharePlaylistTitle(String shareTitle) {
        DB db = DB.get();
        String baseTitle = shareTitle + "- share";
        int i = 0;
        //noinspection StatementWithEmptyBody
        while (db.containsPlaylist(baseTitle + i++));
        return baseTitle + --i;
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
        TmTaskGroup ttg = mTmtg.get();
        if (null != ttg)
            ttg.cancel();
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    protected ImportPlaylistTask(@NonNull DataModel.Playlist pl) {
        mPl = pl;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static ImportPlaylistTask
    create(@NonNull DataModel.Playlist pl) {
        return new ImportPlaylistTask(pl);
    }

    /**
     */
    @Override
    @NonNull
    protected ImportJob.Result
    doAsync() throws SQLException, InterruptedException {
        final DB db = DB.get();
        final ImportJob.Result ir = new ImportJob.Result();

        /* Create jobs to run in parallel.
         *********************************/
        String title = getUniqueSharePlaylistTitle(mPl.title);
        final long plid = db.insertPlaylist(title);
        if (plid < 0)
            throw new SQLException();

        TmTask[] tasks = new TmTask[mPl.videos.length + 1];
        int ti = 0;
        TmTask<Void> t = new TmTask<Void>() {
            @Override
            protected Void
            doAsync() throws JSONException, IOException {
                if (!Util.isValidValue(mPl.thumbnail_ytvid))
                    return null;

                Bitmap bm = YTUtil.getYtThumbnail(mPl.thumbnail_ytvid);
                if (null == bm)
                    return null;
                byte[] bmdata = ImgUtil.compressToJpeg(bm);
                db.updatePlaylist(plid,
                                  new ColPlaylist[] { ColPlaylist.THUMBNAIL,
                                          ColPlaylist.THUMBNAIL_YTVID },
                                  new Object[] { bmdata,
                                          mPl.thumbnail_ytvid });
                bm.recycle();
                // Ignore if fail to load thumbnail - it's very minor for usecase.
                return null;
            }
        };
        tasks[ti++] = t;
        for (DataModel.Video v : mPl.videos)
            tasks[ti++] = new ImportVideoTask(v, plid, ir.success, ir.fail);

        TmTaskGroup.Builder<TmTaskGroup.Builder> ttgb
                = new TmTaskGroup.Builder<>(TaskManager.get());
        ttgb.setTasks(tasks);
        TmTaskGroup ttg = ttgb.create();
        ttg.addEventListener(HelperHandler.get(), new TmTaskGroup.EventListener<TmTaskGroup, Task>() {
            @Override
            public void
            onProgressInit(@NonNull TmTaskGroup ttg, long maxProgress) {
                ImportPlaylistTask.this.publishProgressInit(maxProgress);
            }

            @Override
            public void
            onProgress(@NonNull TmTaskGroup ttg, long progress) {
                ImportPlaylistTask.this.publishProgress(progress);
            }
        });
        mTmtg.set(ttg);
        if (isCancel())
            throw new InterruptedException();
        // Below code is NOT run after 'onEarlyCancel()'.
        try {
            ttg.startSync();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            // Exception is NOT expected here!
            P.bug();
        }
        return ir;
    }
}
