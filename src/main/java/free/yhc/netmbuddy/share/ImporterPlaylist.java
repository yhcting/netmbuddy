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

import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Bitmap;

import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.MultiThreadRunner;
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.share.Share.Err;
import free.yhc.netmbuddy.share.Share.ImportPrepareResult;
import free.yhc.netmbuddy.share.Share.ImportResult;
import free.yhc.netmbuddy.share.Share.ImporterI;
import free.yhc.netmbuddy.share.Share.LocalException;
import free.yhc.netmbuddy.share.Share.OnProgressListener;
import free.yhc.netmbuddy.share.Share.Type;
import free.yhc.netmbuddy.utils.ImageUtils;
import free.yhc.netmbuddy.utils.Utils;
import free.yhc.netmbuddy.utils.YTUtils;

class ImporterPlaylist implements ImporterI {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(ImporterPlaylist.class);

    private final DataModel.Playlist mPl;
    private final MultiThreadRunner mMtrunner;

    static class ImportVideoJob extends MultiThreadRunner.Job<Err> {
        private final long _mPlid; // playlist id to import to
        private final DataModel.Video _mV;  // Video JSON Object.
        private final AtomicInteger _mSuccess;
        private final AtomicInteger _mFail;

        ImportVideoJob(float progWeight,
                       DataModel.Video video,
                       long plid,
                       AtomicInteger success,
                       AtomicInteger fail) {
            super(true, progWeight);
            _mPlid = plid;
            _mV = video;
            _mSuccess = success;
            _mFail = fail;
        }

        @Override
        public Err
        doJob() {
            Boolean success = false;
            try {
                if (!YTUtils.verifyYoutubeVideoId(_mV.ytvid))
                    return Err.INVALID_SHARE;

                DMVideo v = new DMVideo();
                v.ytvid = _mV.ytvid;
                if (!YTUtils.fillYtDataAndThumbnail(v))
                    return Err.IO_NET;
                v.setPreferenceData(_mV.volume, _mV.bookmarks);
                v.setPreferenceDataIfNotSet(Policy.DEFAULT_VIDEO_VOLUME, "");
                if (DB.Err.NO_ERR != DB.get().insertVideoToPlaylist(_mPlid, v))
                    return Err.DB_UNKNOWN;
                success = true;
                return Err.NO_ERR;
            } finally {
                if (success)
                    _mSuccess.incrementAndGet();
                else
                    _mFail.incrementAndGet();
            }
        }
    }

    // =======================================================================
    //
    //
    //
    // =======================================================================
    /**
     * Root of type dependent JSONObject
     */
    ImporterPlaylist(DataModel.Playlist pl) {
        mPl = pl;
        mMtrunner = new MultiThreadRunner(Utils.getUiHandler(),
                                          Policy.YTIMPORT_MAX_LOAD_THUMBNAIL_THREAD);

    }
    private static String
    getUniqueSharePlaylistTitle(String shareTitle) {
        DB db = DB.get();
        String baseTitle = shareTitle + "- share";
        int i = 0;
        //noinspection StatementWithEmptyBody
        while (db.containsPlaylist(baseTitle + i++));
        return baseTitle + --i;
    }

    @Override
    public ImportPrepareResult
    prepare() {
        ImportPrepareResult r = new ImportPrepareResult();
        r.type = Type.PLAYLIST;
        r.message = mPl.title;
        r.err = Err.NO_ERR;
        return r;
    }

    @Override
    public ImportResult
    execute(Object user, final OnProgressListener listener) {
        ImportResult ir = new ImportResult();

        mMtrunner.setOnProgressListener(new MultiThreadRunner.OnProgressListener() {
            @Override
            public void onProgress(float prog) {
                listener.onProgress((int)prog);
            }
        });

        listener.onPreProgress(mPl.videos.length);
        listener.onProgress(0);
        try {
            final DB db = DB.get();
            String title = getUniqueSharePlaylistTitle(mPl.title);
            ir.message = title;
            final long plid = db.insertPlaylist(title);
            if (plid < 0)
                throw new LocalException(Err.DB_UNKNOWN);

            // DO NOT count playlist-thumbnail as progress.
            // User doesn't think this task as 'import' task.
            float jobWeight = 0f;
            // Append job to load/insert playlist thumbnail.
            mMtrunner.appendJob(new MultiThreadRunner.Job<Err>(true, jobWeight) {
                @Override
                public Err
                doJob() {
                    if (!Utils.isValidValue(mPl.thumbnail_ytvid))
                        return Err.NO_ERR; // ignore for invalid thumbnail ytvid.

                    Bitmap bm = YTUtils.getYtThumbnail(mPl.thumbnail_ytvid);
                    if (null == bm)
                        return Err.NO_ERR;
                    byte[] bmdata = ImageUtils.compressBitmap(bm);
                    db.updatePlaylist(plid,
                                      new ColPlaylist[] { ColPlaylist.THUMBNAIL,
                                                          ColPlaylist.THUMBNAIL_YTVID },
                                      new Object[] { bmdata,
                                                     mPl.thumbnail_ytvid });
                    bm.recycle();
                    // Ignore if fail to load thumbnail - it's very minor for usecase.
                    return Err.NO_ERR;
                }
            });

            jobWeight = 1.0f;
            // Append jobs to load/add videos
            for (DataModel.Video v : mPl.videos) {
                mMtrunner.appendJob(new ImportVideoJob(jobWeight,
                                                       v,
                                                       plid,
                                                       ir.success,
                                                       ir.fail));
            }

            mMtrunner.waitAllDone();
            ir.err = Err.NO_ERR;
        } catch (IllegalArgumentException e) {
            ir.err = Err.INVALID_SHARE;
        } catch (InterruptedException e) {
            ir.err = Err.INTERRUPTED;
        } catch (LocalException e) {
            ir.err = e.error();
        }
        return ir;
    }

    @Override
    public void
    cancel() {
        mMtrunner.cancel();
    }


}
