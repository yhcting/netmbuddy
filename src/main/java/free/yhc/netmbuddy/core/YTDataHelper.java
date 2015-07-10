/******************************************************************************
 * Copyright (C) 2015
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

package free.yhc.netmbuddy.core;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import free.yhc.netmbuddy.utils.ImageUtils;
import free.yhc.netmbuddy.utils.Utils;
import free.yhc.netmbuddy.ytapiv3.YTApiFacade;


public class YTDataHelper {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(YTDataHelper.class);

    private static final int MSG_WHAT_OPEN       = 0;
    private static final int MSG_WHAT_CLOSE      = 1;
    private static final int MSG_WHAT_VIDEO_LIST = 2;
    private static final int MSG_WHAT_THUMBNAIL  = 3;

    private BGHandler mBgHandler = null;
    private VideoListRespReceiver mVidListRcvr = null;
    private ThumbnailRespReceiver mThumbnailRcvr = null;

    public interface VideoListRespReceiver {
        void onResponse(YTDataHelper helper, VideoListReq req,
                        VideoListResp resp);
    }

    public interface ThumbnailRespReceiver {
        void onResponse(YTDataHelper helper, ThumbnailReq req,
                        ThumbnailResp resp);
    }

    public static class VideoListReq {
        public Object opaque = null;
        public YTDataAdapter.VideoListReq yt = null;
        @SuppressWarnings("unused")
        public VideoListReq() { }
        public VideoListReq(Object opaque, YTDataAdapter.VideoListReq yt) {
            this.opaque = opaque;
            this.yt = yt;
        }
    }

    public static class VideoListResp {
        public YTDataAdapter.Err err = null;
        public Object opaque = null;
        public YTDataAdapter.VideoListResp yt = null;
        @SuppressWarnings("unused")
        public VideoListResp() { }
        public VideoListResp(YTDataAdapter.Err err,
                             Object opaque,
                             YTDataAdapter.VideoListResp yt) {
            this.err = err;
            this.opaque = opaque;
            this.yt = yt;
        }
    }

    public static class ThumbnailReq {
        public Object opaque = null;
        public String url = null;
        public int width = -1;
        public int height = -1;
        @SuppressWarnings("unused")
        public ThumbnailReq() { }
        public ThumbnailReq(Object opaque,
                            String url,
                            int width,
                            int height) {
            this.opaque = opaque;
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }

    public static class ThumbnailResp {
        public YTDataAdapter.Err err;
        public Object opaque = null;
        public Bitmap bm = null;
        @SuppressWarnings("unused")
        public ThumbnailResp() { }
        public ThumbnailResp(YTDataAdapter.Err err,
                             Object opaque,
                             Bitmap bm) {
            this.err = err;
            this.opaque = opaque;
            this.bm = bm;
        }
    }


    private static class BGThread extends HandlerThread {
        private BGHandler mHandler = null;
        BGThread() {
            super("YTDataHelper.BGThread", Process.THREAD_PRIORITY_BACKGROUND);
        }

        void
        setBgHandler(BGHandler handler) {
            mHandler = handler;
        }

        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }

        @Override
        public void
        run() {
            try {
                super.run();
            } catch (Exception ignored) {
            } finally {
                if (null != mHandler)
                    mHandler.cleanupPostClose();
            }
        }
    }

    private static class BGHandler extends Handler {
        private final YTDataHelper _mHelper;

        private MultiThreadRunner _mMtrunner = null;
        private VideoListRespReceiver _mVidListRcvr = null;
        private ThumbnailRespReceiver _mThumbnailRcvr = null;
        private boolean _mClosed = false;

        BGHandler(Looper looper, YTDataHelper helper) {
            super(looper);
            _mHelper = helper;
        }

        private void
        sendVideoListResp(final VideoListReq req,
                          final VideoListResp resp) {
            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void
                run() {
                    if (!_mClosed && null != _mVidListRcvr)
                        _mVidListRcvr.onResponse(_mHelper, req, resp);
                }
            });
        }

        private void
        sendThumbnailResp(final ThumbnailReq req,
                          final ThumbnailResp resp) {
            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void
                run() {
                    if (!_mClosed && null != _mThumbnailRcvr)
                        _mThumbnailRcvr.onResponse(_mHelper, req, resp);
                }
            });
        }

        /**
         * This function will be called end of 'run()' function in HandlerThread.
         */
        void
        cleanupPostClose() {
            if (null != _mMtrunner) {
                _mMtrunner.cancel();
                _mMtrunner = null;
            }
        }

        void
        close(boolean interrupt) {
            if (_mClosed)
                return; // already closed.

            removeMessages(MSG_WHAT_OPEN);
            removeMessages(MSG_WHAT_VIDEO_LIST);
            removeMessages(MSG_WHAT_THUMBNAIL);
            _mVidListRcvr = null;
            _mThumbnailRcvr = null;
            if (interrupt) {
                _mClosed = true;
                getLooper().getThread().interrupt();
                ((HandlerThread)getLooper().getThread()).quit();
            } else
                sendEmptyMessage(MSG_WHAT_CLOSE);
        }

        void
        setVideoListRespRecevier(VideoListRespReceiver receiver) {
            _mVidListRcvr = receiver;
        }

        void
        setThumbnailRespRecevier(ThumbnailRespReceiver receiver) {
            _mThumbnailRcvr = receiver;
        }

        @Override
        public void
        handleMessage(final Message msg) {
            if (_mClosed)
                return;

            switch (msg.what) {
                case MSG_WHAT_OPEN: {
                    eAssert(null == _mMtrunner);
                    _mMtrunner = new MultiThreadRunner(this,
                            Policy.YTSEARCH_MAX_LOAD_THUMBNAIL_THREAD);
                } break;

                case MSG_WHAT_CLOSE: {
                    _mClosed = true;
                    ((HandlerThread)getLooper().getThread()).quit();
                } break;

                case MSG_WHAT_VIDEO_LIST: {
                    final VideoListReq req = (VideoListReq)msg.obj;
                    MultiThreadRunner.Job<Integer> job
                        = new MultiThreadRunner.Job<Integer>(true, 0) {
                        @Override
                        public Integer
                        doJob() {
                            VideoListResp r = null;
                            try {
                                r = requestVideoList(req);
                            } catch (YTDataAdapter.YTApiException e) {
                                r = new VideoListResp(e.error(), req.opaque, null);
                            } finally {
                                // "r == null" means that something unexpected exception issued.
                                if (null == r)
                                    r = new VideoListResp(YTDataAdapter.Err.UNKNOWN, req.opaque, null);
                                sendVideoListResp(req, r);
                            }
                            return 0;
                        }
                    };
                    _mMtrunner.appendJob(job, true);
                } break;

                case MSG_WHAT_THUMBNAIL: {
                    final ThumbnailReq req = (ThumbnailReq)msg.obj;
                    MultiThreadRunner.Job<Integer> job
                            = new MultiThreadRunner.Job<Integer>(true, 0) {
                        @Override
                        public Integer
                        doJob() {
                            ThumbnailResp r = null;
                            try {
                                r = requestThumbnail(req);
                            } catch (YTDataAdapter.YTApiException e) {
                                r = new ThumbnailResp(e.error(), req.opaque, null);
                            } finally {
                                if (null == r)
                                    r = new ThumbnailResp(YTDataAdapter.Err.UNKNOWN, req.opaque, null);
                                sendThumbnailResp(req, r);
                            }
                            return 0;
                        }
                    };
                    _mMtrunner.appendJob(job);
                } break;
            }
        }
    }

    @SuppressWarnings("unused")
    private static YTDataAdapter.Err
    map(NetLoader.Err err, Object extra) {
        switch (err) {
            case IO_NET:
                return YTDataAdapter.Err.IO_NET;
            case HTTPGET:
                if (!(extra instanceof Integer))
                    return YTDataAdapter.Err.IO_NET;

                int stcode = (Integer)extra;
                switch (stcode) {
                    case HttpUtils.SC_BAD_REQUEST:
                        return YTDataAdapter.Err.BAD_REQUEST;

                    case HttpUtils.SC_NOT_FOUND:
                        return YTDataAdapter.Err.INVALID_PARAM;

                    default:
                        return YTDataAdapter.Err.IO_NET;
                }

            case INTERRUPTED:
                return YTDataAdapter.Err.INTERRUPTED;

            case NO_ERR:
                eAssert(false);

            default:
                return YTDataAdapter.Err.UNKNOWN;
        }
    }

    private static byte[]
    loadUrl(String urlStr) throws NetLoader.LocalException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Uri uri = Uri.parse(urlStr);
        NetLoader loader = new NetLoader().open(null);
        loader.readHttpData(baos, uri);
        loader.close();

        byte[] data = baos.toByteArray();
        baos.close();
        return data;
    }

    private static YTDataAdapter.VideoListResp
    doRequestVideoList(YTDataAdapter.VideoListReq req)
        throws YTDataAdapter.YTApiException {
        switch (req.type) {
            case VID_KEYWORD:
            case VID_CHANNEL:
                return YTApiFacade.requestVideoList(req);
            default:
                eAssert(false);
        }
        return null;
    }

    /**
     * Thread safe
     */
    public static VideoListResp
    requestVideoList(VideoListReq req)
        throws YTDataAdapter.YTApiException {
        if (!Utils.isNetworkAvailable())
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.NETWORK_UNAVAILABLE);
        YTDataAdapter.VideoListResp resp = doRequestVideoList(req.yt);
        return new VideoListResp(YTDataAdapter.Err.NO_ERR, req.opaque, resp);
    }

    /**
     * Thread safe
     */
    public static ThumbnailResp
    requestThumbnail(ThumbnailReq req)
        throws YTDataAdapter.YTApiException {
        if (null == req
            || null == req.url)
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.INVALID_PARAM);
        if (!Utils.isNetworkAvailable())
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.NETWORK_UNAVAILABLE);
        Bitmap bm;
        try {
            bm = ImageUtils.decodeImage(loadUrl(req.url),
                                        req.width,
                                        req.height);

            return new ThumbnailResp(YTDataAdapter.Err.NO_ERR, req.opaque, bm);
        } catch (IOException | NetLoader.LocalException e) {
            // TODO : Error should be handled in details
            // Ex. Quota exceeded, Bad request etc...
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.IO_NET);
        }
    }

    public YTDataHelper() {
    }

    public void
    setVideoListRespRecevier(VideoListRespReceiver receiver) {
        mVidListRcvr = receiver;
        if (null != mBgHandler)
            mBgHandler.setVideoListRespRecevier(receiver);
    }

    public void
    setThumbnailRespRecevier(ThumbnailRespReceiver receiver) {
        mThumbnailRcvr = receiver;
        if (null != mBgHandler)
            mBgHandler.setThumbnailRespRecevier(receiver);
    }

    public YTDataAdapter.Err
    requestThumbnailAsync(ThumbnailReq req) {
        if (null == mBgHandler)
            return YTDataAdapter.Err.NO_ERR;
        if (!Utils.isNetworkAvailable())
            return YTDataAdapter.Err.NETWORK_UNAVAILABLE;

        Message msg = mBgHandler.obtainMessage(MSG_WHAT_THUMBNAIL, req);
        mBgHandler.sendMessage(msg);
        return YTDataAdapter.Err.NO_ERR;
    }

    public YTDataAdapter.Err
    requestVideoListAsync(VideoListReq req) {
        eAssert(0 < req.yt.pageSize
                && req.yt.pageSize <= Policy.YTSEARCH_MAX_RESULTS);
        if (!Utils.isNetworkAvailable())
            return YTDataAdapter.Err.NETWORK_UNAVAILABLE;
        Message msg = mBgHandler.obtainMessage(MSG_WHAT_VIDEO_LIST, req);
        mBgHandler.sendMessage(msg);
        return YTDataAdapter.Err.NO_ERR;
    }

    public void
    open() {
        BGThread hThread = new BGThread();
        hThread.start();
        mBgHandler = new BGHandler(hThread.getLooper(), this);
        hThread.setBgHandler(mBgHandler);
        mBgHandler.sendEmptyMessage(MSG_WHAT_OPEN);
        mBgHandler.setVideoListRespRecevier(mVidListRcvr);
        mBgHandler.setThumbnailRespRecevier(mThumbnailRcvr);
    }

    public void
    close(boolean interrupt) {
        // TODO checking that below code works as expected perfectly, is required.
        // Stop running thread!
        // "interrupting thread" is quite annoying and unpredictable job!
        if (null != mBgHandler) {
            mBgHandler.close(interrupt);
            mBgHandler = null;
        }
    }
}
