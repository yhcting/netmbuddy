/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of YTMPlayer.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy.db;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import free.yhc.netmbuddy.model.YTVideoFeed;
import free.yhc.netmbuddy.utils.Utils;

public class DBHelper {
    private static final int MSG_WHAT_CLOSE         = 0;
    private static final int MSG_WHAT_CHECK_EXIST   = 1;

    private BGHandler               mBgHandler  = null;
    private CheckDupDoneReceiver    mDupRcvr    = null;

    public interface CheckDupDoneReceiver {
        void checkDupDone(DBHelper helper, CheckDupArg arg,
                          boolean[] results, Err err);
    }

    public static enum Err {
        NO_ERR
    }

    public static class CheckDupArg {
        public final Object                 tag;
        public final YTVideoFeed.Entry[]    ents;
        public CheckDupArg(Object aTag, YTVideoFeed.Entry[] aEnts) {
            tag = aTag;
            ents = aEnts;
        }
    }

    private static class BGThread extends HandlerThread {
        BGThread() {
            super("DBHelper.BGThread",Process.THREAD_PRIORITY_BACKGROUND);
        }

        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }
    }

    private static class BGHandler extends Handler {
        private final DBHelper  _mHelper;

        private boolean         _mClosed  = false;

        BGHandler(Looper    looper,
                  DBHelper  helper) {
            super(looper);
            _mHelper = helper;
        }

        private boolean[]
        checkDup(YTVideoFeed.Entry[] entries) {
            // TODO
            // Should I check "entries[i].available" flag???
            boolean[] r = new boolean[entries.length];
            for (int i = 0; i < r.length; i++)
                r[i] = DB.get().containsVideo(entries[i].media.videoId);
            return r;
        }

        private void
        sendCheckDupDone(final CheckDupArg arg, final boolean[] results, final Err err) {
            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void
                run() {
                    CheckDupDoneReceiver rcvr = _mHelper.getCheckDupDoneReceiver();
                    if (!_mClosed && null != rcvr)
                        rcvr.checkDupDone(_mHelper, arg, results, err);
                }
            });
            return;
        }

        private void
        handleCheckDup(CheckDupArg arg) {
            sendCheckDupDone(arg, checkDup(arg.ents), Err.NO_ERR);
        }

        void
        close() {
            removeMessages(MSG_WHAT_CHECK_EXIST);
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
                ((HandlerThread)getLooper().getThread()).quit();
                break;

            case MSG_WHAT_CHECK_EXIST:
                handleCheckDup((CheckDupArg)msg.obj);
                break;
            }
        }
    }

    CheckDupDoneReceiver
    getCheckDupDoneReceiver() {
        return mDupRcvr;
    }

    // ======================================================================
    //
    //
    //
    // ======================================================================
    public void
    setCheckDupDoneReceiver(CheckDupDoneReceiver rcvr) {
        mDupRcvr = rcvr;
    }

    public void
    checkDupAsync(CheckDupArg arg) {
        Message msg = mBgHandler.obtainMessage(MSG_WHAT_CHECK_EXIST, arg);
        mBgHandler.sendMessage(msg);
    }

    public DBHelper() {
    }

    public void
    open() {
        HandlerThread hThread = new BGThread();
        hThread.start();
        mBgHandler = new BGHandler(hThread.getLooper(), this);
    }

    public void
    close() {
        // TODO
        // Stop running thread!
        // Need to check that below code works as expected perfectly.
        // "interrupting thread" is quite annoying and unpredictable job!
        if (null != mBgHandler) {
            mBgHandler.close();
            mBgHandler = null;
        }
    }
}
