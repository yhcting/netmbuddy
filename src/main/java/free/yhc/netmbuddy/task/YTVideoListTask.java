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

import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.ConnectException;

import free.yhc.baselib.Logger;
import free.yhc.baselib.async.HelperHandler;
import free.yhc.baselib.async.ThreadEx;
import free.yhc.baselib.async.TmTask;
import free.yhc.baselib.exception.BadResponseException;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Util;
import free.yhc.netmbuddy.ytapiv3.YTApiFacade;

public class YTVideoListTask extends TmTask<YTDataAdapter.VideoListResp> {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(YTVideoListTask.class, Logger.LOGLV_DEFAULT);

    private final YTDataAdapter.VideoListReq mVLReq;
    private final Object mOpaque;

    private YTDataAdapter.VideoListResp mVLResp = null;

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    protected YTVideoListTask(
            @NonNull YTDataAdapter.VideoListReq vlreq,
            Object opaque) {
        super(YTVideoListTask.class.getSimpleName(),
              HelperHandler.get(),
              ThreadEx.TASK_PRIORITY_NORM,
              true);
        mVLReq = vlreq;
        mOpaque = opaque;
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static YTVideoListTask
    create(@NonNull YTDataAdapter.VideoListReq vlreq,
           Object opaque) {
        return new YTVideoListTask(vlreq, opaque);
    }

    public Object
    getOpaque() {
        return mOpaque;
    }

    public YTDataAdapter.VideoListReq
    getRequest() {
        return mVLReq;
    }

    public YTDataAdapter.VideoListResp
    getResponse() {
        return mVLResp;
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
    protected YTDataAdapter.VideoListResp
    doAsync() throws InterruptedException, IOException, BadResponseException {
        if (!Util.isNetworkAvailable())
            throw new ConnectException();
        switch (mVLReq.type) {
        case VID_KEYWORD:
        case VID_CHANNEL:
            mVLResp = YTApiFacade.requestVideoList(mVLReq);
            break;
        default:
            P.bug(false);
        }
        return mVLResp;
    }
}
