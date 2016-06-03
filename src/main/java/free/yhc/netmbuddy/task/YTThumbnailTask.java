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

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;

import free.yhc.baselib.Logger;
import free.yhc.baselib.async.HelperHandler;
import free.yhc.baselib.async.ThreadEx;
import free.yhc.baselib.async.TmTask;
import free.yhc.baselib.exception.BadResponseException;
import free.yhc.baselib.net.NetReadTask;
import free.yhc.abaselib.util.ImgUtil;
import free.yhc.netmbuddy.utils.Util;

public class YTThumbnailTask extends TmTask<Bitmap> {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTThumbnailTask.class, Logger.LOGLV_DEFAULT);

    private final URL mUrl;
    private final int mW, mH;
    private final Object mOpaque;

    private Bitmap mThumbnail = null;

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////

    public YTThumbnailTask(
            @NonNull URL url,
            int width,
            int height,
            Object opaque) {
        super(tmId(url), HelperHandler.get(), ThreadEx.TASK_PRIORITY_MIN, true);
        mUrl = url;
        mW = width;
        mH = height;
        mOpaque = opaque;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static YTThumbnailTask
    create(@NonNull URL url,
           int width,
           int height,
           Object opaque) {
        return new YTThumbnailTask(url, width, height, opaque);
    }

    @NonNull
    public static String
    tmId(URL url) {
        return YTThumbnailTask.class.getSimpleName() + ":" + url.toString();
    }

    public String
    tmId() {
        return tmId(getURL());
    }

    public Object
    getOpaque() {
        return mOpaque;
    }

    public URL
    getURL() {
        return mUrl;
    }

    public Bitmap
    getThumbnail() {
        return mThumbnail;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @throws InterruptedException
     * @throws IOException (ConnectException ...)
     * @throws BadResponseException
     */
    @Override
    @NonNull
    protected Bitmap
    doAsync() throws InterruptedException, IOException, BadResponseException {
        if (!Util.isNetworkAvailable())
            throw new ConnectException();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        NetReadTask.Builder<NetReadTask.Builder> b
                = new NetReadTask.Builder<>(Util.createNetConn(mUrl), baos);
        try {
            b.create().startSync();
        } catch (IOException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Bitmap bm = ImgUtil.decodeBitmap(baos.toByteArray(), mW, mH);
        if (null == bm)
            throw new BadResponseException(); // data from network is NOT bitmap!
        mThumbnail = bm;
        return bm;
    }
}
