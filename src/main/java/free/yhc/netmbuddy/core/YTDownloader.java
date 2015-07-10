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

package free.yhc.netmbuddy.core;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import free.yhc.netmbuddy.utils.Utils;

public class YTDownloader {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTDownloader.class);

    private static final int MSG_WHAT_CLOSE     = 0;
    private static final int MSG_WHAT_DOWNLOAD  = 1;

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private String mProxy = null;
    private DownloadDoneReceiver mDnDoneRcvr = null;
    private BGHandler mBgHandler  = null;
    private Object mUserTag = null; // user tag value

    public interface DownloadDoneReceiver {
        void downloadDone(YTDownloader downloader, DnArg arg, Err err);
    }

    public enum Err {
        NO_ERR,
        IO_NET,
        IO_FILE,
        PROTOCOL, // unexpected response from Youtube
        NETWORK_UNAVAILABLE,
        UNSUPPORTED_VIDFORMAT,
        INTERRUPTED,
        UNKNOWN,   // err inside module
    }

    public static class DnArg {
        String ytvid;
        File outf;
        int qscore;

        public DnArg(String aYtvid, File aOutf, int aQscore) {
            ytvid = aYtvid;
            outf = aOutf;
            qscore = aQscore;
        }
    }

    private static class BGThread extends HandlerThread {
        BGThread() {
            super("YTDownloader.BGThread",Process.THREAD_PRIORITY_BACKGROUND);
        }
        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }
    }

    private static class BGHandler extends Handler {
        private final YTDownloader _mYtDownloader;

        private NetLoader _mLoader = new NetLoader();
        private File _mTmpF = null;
        private volatile File _mCurOutF = null;
        private boolean _mClosed = false;

        BGHandler(Looper looper,
                  YTDownloader ytDownloader) {
            super(looper);
            _mYtDownloader = ytDownloader;
        }

        private void
        sendResult(final DnArg arg, final Err result) {
            if (null == _mYtDownloader.getDownloadDoneReceiver())
                return;

            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void
                run() {
                    DownloadDoneReceiver rcvr = _mYtDownloader.getDownloadDoneReceiver();
                    if (!_mClosed && null != rcvr)
                        rcvr.downloadDone(_mYtDownloader, arg, result);
                }
            });
        }

        // This is synchronous function.
        // That is, ONLY one file can be download in one YTDownloader instance at a time.
        private void
        handleDownload(DnArg arg) {
            // assigning object reference is atomic operation in JAVA
            _mCurOutF = arg.outf;

            if (null != _mTmpF)
                // We did out best. Return value is ignored intentionally.
                //noinspection ResultOfMethodCallIgnored
                _mTmpF.delete();

            if (DBG) P.v("Start Download : " + arg.ytvid + " => " + arg.outf.getAbsolutePath());
            YTHacker hack = new YTHacker(arg.ytvid, null, null);
            FileOutputStream fos = null;
            try {
                YTHacker.Err hkerr = hack.start();
                if (YTHacker.Err.NO_ERR != hkerr) {
                    sendResult(arg, map(hkerr));
                    return;
                }
                _mLoader = hack.getNetLoader();
                YTHacker.YtVideo vid = hack.getVideo(arg.qscore, false);
                if (null == vid) {
                    sendResult(arg, Err.UNSUPPORTED_VIDFORMAT);
                    return;
                }

                NetLoader.HttpRespContent content = _mLoader.getHttpContent(Uri.parse(vid.url));
                if (HttpUtils.SC_OK != content.stcode) {
                    sendResult(arg, Err.IO_NET);
                    return;
                }

                // Download to temp file.
                _mTmpF = File.createTempFile(arg.ytvid, null, new File(Policy.APPDATA_TMPDIR));
                fos = new FileOutputStream(_mTmpF);
                Utils.copy(fos, content.stream);
                fos.close();
                fos = null;
                // file returned by YTHacker is mpeg format!
                // We did out best. Return value is ignored intentionally.
                //noinspection ResultOfMethodCallIgnored
                _mTmpF.renameTo(arg.outf);
                sendResult(arg, Err.NO_ERR);
                if (DBG) P.v("Download Done : " + arg.ytvid);
            } catch (FileNotFoundException e) {
                sendResult(arg, Err.IO_FILE);
            } catch (InterruptedException e) {
                if (DBG) P.v("Download Interrupted!");
                sendResult(arg, Err.INTERRUPTED);
            } catch (IOException e) {
                if (DBG) P.v("Download IOException!");
                sendResult(arg, Err.IO_FILE);
            } catch (NetLoader.LocalException e) {
                if (DBG) P.v("NetLoader Exception!");
                sendResult(arg, map(e.error()));
            } finally {
                _mLoader.close();

                if (null != fos)
                    try {
                        fos.close();
                    } catch (IOException ignored) {}

                if (null != _mTmpF)
                    // We did out best. Return value is ignored intentionally.
                    //noinspection ResultOfMethodCallIgnored
                    _mTmpF.delete();

                // assigning object reference is atomic operation in JAVA
                _mCurOutF = null;
            }
        }

        String
        getCurrentTargetFile() {
            // NOTE.
            // curOutF is volatile and assigning object reference is atomic operation in JAVA.
            // So, synchronization is not required here.
            File outF = _mCurOutF;
            return (null == outF)? null: outF.getAbsolutePath();
        }

        void
        close() {
            removeMessages(MSG_WHAT_DOWNLOAD);
            sendEmptyMessage(MSG_WHAT_CLOSE);
        }

        @Override
        public void
        handleMessage(Message msg) {
            if (_mClosed)
                return;

            switch (msg.what) {
            case MSG_WHAT_CLOSE:
                _mClosed = true;
                if (null != _mLoader)
                    _mLoader.close();
                ((HandlerThread)getLooper().getThread()).quit();
                break;

            case MSG_WHAT_DOWNLOAD:
                handleDownload((DnArg)msg.obj);
                break;
            }
        }
    }

    private static Err
    map(YTHacker.Err err) {
        switch (err) {
        case NO_ERR:
            return Err.NO_ERR;
        case IO_NET:
            return Err.IO_NET;
        case NETWORK_UNAVAILABLE:
            return Err.NETWORK_UNAVAILABLE;
        case PARSE_HTML:
            return Err.PROTOCOL;
        case INTERRUPTED:
            return Err.INTERRUPTED;
        default:
            return Err.UNKNOWN;
        }
    }

    private static Err
    map(NetLoader.Err err) {
        switch (err) {
        case NO_ERR:
            return Err.NO_ERR;
        case HTTPGET:
        case IO_NET:
            return Err.IO_NET;
        case INTERRUPTED:
            return Err.INTERRUPTED;
        default:
            return Err.UNKNOWN;
        }
    }

    DownloadDoneReceiver
    getDownloadDoneReceiver() {
        return mDnDoneRcvr;
    }

    // ======================================================================
    //
    //
    //
    // ======================================================================
    public YTDownloader() {
    }

    public void
    setTag(Object tag) {
        mUserTag = tag;
    }

    public Object
    getTag() {
        return mUserTag;
    }

    public String
    getCurrentTargetFile() {
        return (null == mBgHandler)? null: mBgHandler.getCurrentTargetFile();
    }

    /**
     *
     * @param ytvid 11-character-long youtube video id
     * @param delay real-downloading will be started after 'delay' milliseconds.
     */
    public Err
    download(final String ytvid, final File outf, final int qscore, final long delay) {
        eAssert(Utils.isUiThread());

        if (outf.exists()) {
            if (null != mDnDoneRcvr) {
                // already downloaded.
                Utils.getUiHandler().post(new Runnable() {
                    @Override
                    public void
                    run() {
                        mDnDoneRcvr.downloadDone(YTDownloader.this,
                                                 new DnArg(ytvid, outf, qscore),
                                                 Err.NO_ERR);
                    }
                });
            }
            return Err.NO_ERR;
        }

        if (Utils.isNetworkAvailable()) {
            Message msg = mBgHandler.obtainMessage(MSG_WHAT_DOWNLOAD,
                                                   new DnArg(ytvid, outf, qscore));
            mBgHandler.sendMessageDelayed(msg, delay);
            return Err.NO_ERR;
        }
        return Err.NETWORK_UNAVAILABLE;
    }

    public void
    open(String proxy, DownloadDoneReceiver dnDoneRcvr) {
        mProxy = proxy;
        mDnDoneRcvr = dnDoneRcvr;

        HandlerThread hThread = new BGThread();
        hThread.start();
        mBgHandler = new BGHandler(hThread.getLooper(), this);
    }

    public void
    close() {
        // TODO : Checking that below code works as expected perfectly, is required.
        // Stop running thread!
        // "interrupting thread" is quite annoying and unpredictable job!
        if (null != mBgHandler)
            mBgHandler.close();
    }
}
