/******************************************************************************
 * Copyright (C) 2016
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

package free.yhc.netmbuddy.task;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import free.yhc.abaselib.util.AUtil;
import free.yhc.baselib.Logger;
import free.yhc.baselib.adapter.HandlerAdapter;
import free.yhc.baselib.async.HelperHandler;
import free.yhc.baselib.async.ThreadEx;
import free.yhc.baselib.async.TmTask;
import free.yhc.baselib.net.NetConnHttp;
import free.yhc.baselib.net.NetDownloadTask;
import free.yhc.netmbuddy.core.PolicyConstant;
import free.yhc.netmbuddy.utils.Util;

public class YTDownloadTask extends TmTask<Void> {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTDownloadTask.class, Logger.LOGLV_DEFAULT);

    private final String mYtvid;
    private final File mOutf;
    private final int mQScore;

    ///////////////////////////////////////////////////////////////////////////
    //
    // Builder
    //
    ///////////////////////////////////////////////////////////////////////////
    protected YTDownloadTask(
            @NonNull String name,
            @NonNull HandlerAdapter owner,
            @NonNull File outFile,
            int priority,
            boolean interruptOnCancel,
            @NonNull String ytvid,
            int qscore)
            throws IOException {
        super(name,
              owner,
              priority,
              interruptOnCancel);
        mYtvid = ytvid;
        mOutf = outFile;
        mQScore = qscore;
    }

    public static class Builder<B extends Builder>
            extends TmTask.Builder<B, YTDownloadTask> {
        protected final String mYtid;
        protected final int mQscore;
        protected final File mOutf;

        public Builder(
                @NonNull File outfile,
                @NonNull String ytvid,
                int qscore) {
            super();
            mName = tmId(outfile);
            mOwner = HelperHandler.get();
            mPriority = ThreadEx.TASK_PRIORITY_MIN;
            mInterruptOnCancel = true;
            mOutf = outfile;
            mYtid = ytvid;
            mQscore = qscore;
        }

        @Override
        @NonNull
        public YTDownloadTask
        create() {
            try {
                return new YTDownloadTask(mName,
                                          mOwner,
                                          mOutf,
                                          mPriority,
                                          mInterruptOnCancel,
                                          mYtid,
                                          mQscore);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static String
    tmId(@NonNull File outfile) {
        return YTDownloadTask.class.getSimpleName() + ":" + outfile.getAbsolutePath();
    }

    @NonNull
    public String
    tmId() {
        return tmId(mOutf);
    }

    @NonNull
    public String
    getYtvid() {
        return mYtvid;
    }

    public int
    getQScore() {
        return mQScore;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    @NonNull
    public Void
    doAsync() throws IOException, InterruptedException{
        YTHackTask.Builder<YTHackTask.Builder> hb = new YTHackTask.Builder<>(mYtvid);
        YTHackTask hack;
        try {
            hack = hb.create();
            hack.startSync();
        } catch (IOException | InterruptedException e) {
            throw e;
        } catch (ParseException e) {
            if (DBG) P.w("YTHack fails to parse!");
            throw new IOException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        YTHackTask.YtVideo vid = hack.getVideo(mQScore, false);
        /* We can assume that video page always gives correct result.
         * That is, we can think that, for following cases, Hacker doesn't parse html correctly!.
         *   - there is NO valid video
         *   - video URL is malformed.
         */
        if (null == vid) {
            if (DBG) P.w("YTHack fails to parse: id: " + mYtvid + ", qs: " + mQScore);
            throw new IOException();
        }

        URL url;
        try {
            url = new URL(vid.url);
        } catch (MalformedURLException e) {
            if (DBG) P.w("YTHack fails to parse: Invalid video url: " + vid.url);
            throw e;
        }
        // Ignore hack-time because dominant factor of process is, 'Download'.
        NetConnHttp conn = Util.createNetConnHttp(url, PolicyConstant.YTHACK_UASTRING);
        NetDownloadTask.Builder<NetDownloadTask.Builder> ndb
                = new NetDownloadTask.Builder<>(conn, AUtil.createTempFile());
        try {
            NetDownloadTask ndtask = ndb.create();
            ndtask.startSync();
            return null;
        } catch (IOException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
