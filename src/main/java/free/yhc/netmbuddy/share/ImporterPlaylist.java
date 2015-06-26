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

import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.MultiThreadRunner;
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.core.YTSearchHelper;
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
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(ImporterPlaylist.class);

    private final JSONObject            mJo;
    private final MultiThreadRunner     mMtrunner;

    static class ImportVideoJob extends MultiThreadRunner.Job<Err> {
        private final long          _mPlid; // playlist id to import to
        private final JSONObject    _mJov;  // Video JSON Object.
        private final AtomicInteger _mSuccess;
        private final AtomicInteger _mFail;

        ImportVideoJob(float progWeight,
                       JSONObject jov,
                       long plid,
                       AtomicInteger success,
                       AtomicInteger fail) {
            super(true, progWeight);
            _mPlid = plid;
            _mJov = jov;
            _mSuccess = success;
            _mFail = fail;
        }

        @Override
        public Err
        doJob() {
            Err err = Err.UNKNOWN;
            try {
                String title = _mJov.getString(Json.FTITLE);
                String ytvid = _mJov.getString(Json.FYTID);
                if (!YTUtils.verifyYoutubeVideoId(ytvid))
                    return Err.INVALID_SHARE;

                // NOTE
                // Values that may not be in shared data
                //   - newly added field after Database version 1.
                // Use default values for them.

                // AUTHOR is newly added at Database version 2.
                String author = "";
                if (_mJov.has(Json.FAUTHOR))
                    author = _mJov.getString(Json.FAUTHOR);

                int playtm = _mJov.getInt(Json.FPLAYTIME);
                int volume = Policy.DEFAULT_VIDEO_VOLUME;
                if (_mJov.has(Json.FVOLUME))
                    volume = _mJov.getInt(Json.FVOLUME);
                String bookmarks = "";
                if (_mJov.has(Json.FBOOKMARKS))
                    bookmarks = _mJov.getString(Json.FBOOKMARKS);

                // NOTE
                // Getting thumbnail URL from youtube video id requires downloanding and parsing.
                // It takes too much time.
                // So, a kind of HACK is used to get thumbnail URL from youtube video id.
                // see comments of 'YTHacker.getYtVideoThumbnailUrl()' for details.
                if (YTUtils.insertVideoToPlaylist(_mPlid,
                                                  ytvid,
                                                  title,
                                                  author,
                                                  playtm,
                                                  volume,
                                                  bookmarks))
                    err = Err.NO_ERR;
            } catch (JSONException e) {
                return Err.INVALID_SHARE;
            } finally {
                if (Err.NO_ERR == err)
                    _mSuccess.incrementAndGet();
                else
                    _mFail.incrementAndGet();
            }
            return err;
        }
    }

    /**
     * Root of type dependent JSONObject
     * @param jo
     */
    ImporterPlaylist(JSONObject jo) {
        mJo = jo;
        mMtrunner =  new MultiThreadRunner(Utils.getUiHandler(),
                                           Policy.YTIMPORT_MAX_LOAD_THUMBNAIL_THREAD);

    }

    private static String
    getUniqueSharePlaylistTitle(String shareTitle) {
        DB db = DB.get();
        String baseTitle = shareTitle + "- share";
        int i = 0;
        while (db.containsPlaylist(baseTitle + i++));
        return baseTitle + --i;
    }

    @Override
    public ImportPrepareResult
    prepare() {
        ImportPrepareResult r = new ImportPrepareResult();
        r.type = Type.PLAYLIST;
        try {
            r.message = mJo.getString(Json.FTITLE);
            r.err = Err.NO_ERR;
        } catch (JSONException e) {
            r.err = Err.INVALID_SHARE;
        }
        return r;
    }

    @Override
    public ImportResult
    execute(Object user, final OnProgressListener listener) {
        ImportResult ir = new ImportResult();
        mMtrunner.setOnProgressListener(new MultiThreadRunner.OnProgressListener() {
            @Override
            public void onProgress(float prog) {
                listener.onProgress(prog);
            }
        });

        try {
            final DB db = DB.get();
            JSONArray jarr = mJo.getJSONArray(Json.FVIDEOS);

            String title = getUniqueSharePlaylistTitle(mJo.getString(Json.FTITLE));

            // THUMBNAIL_YTVID is newly added at Database version 2.
            // So, we cannot sure that DB always has valid value for this field.
            final String thumbnailYtvid;
            if (mJo.has(Json.FTHUMBNAIL_YTVID))
                thumbnailYtvid = mJo.getString(Json.FTHUMBNAIL_YTVID);
            else
                thumbnailYtvid = "";

            ir.message = title;
            final long plid = db.insertPlaylist(title);
            if (plid < 0)
                throw new LocalException(Err.DB_UNKNOWN);

            float jobWeight = 1.0f / (jarr.length() + 1); // + 1 for loading playlist thumbnail.
            // Append job to load/insert playlist thumbnail.
            mMtrunner.appendJob(new MultiThreadRunner.Job<Err>(true, jobWeight) {
                @Override
                public Err
                doJob() {
                    if (!Utils.isValidValue(thumbnailYtvid))
                        return Err.NO_ERR; // ignore for invalid thumbnail ytvid.

                    YTSearchHelper.LoadThumbnailReturn ltr
                        = YTUtils.loadYtVideoThumbnail(thumbnailYtvid);
                    if (YTSearchHelper.Err.NO_ERR == ltr.err) {
                        byte[] data = ImageUtils.compressBitmap(ltr.bm);
                        db.updatePlaylist(plid,
                                          new ColPlaylist[] { ColPlaylist.THUMBNAIL,
                                                              ColPlaylist.THUMBNAIL_YTVID },
                                          new Object[] { data,
                                                         thumbnailYtvid });
                        ltr.bm.recycle();
                    }
                    // Ignore if fail to load thumbnail - it's very minor for usecase.
                    return Err.NO_ERR;
                }
            });

            // Append jobs to load/add videos
            for (int i = 0; i < jarr.length(); i++) {
                mMtrunner.appendJob(new ImportVideoJob(jobWeight,
                                                       jarr.getJSONObject(i),
                                                       plid,
                                                       ir.success,
                                                       ir.fail));
            }

            mMtrunner.waitAllDone();
            ir.err = Err.NO_ERR;
        } catch (IllegalArgumentException e) {
            ir.err = Err.INVALID_SHARE;
        } catch (JSONException e) {
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
