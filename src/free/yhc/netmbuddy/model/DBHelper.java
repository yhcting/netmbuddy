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

package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.model.Utils.eAssert;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

public class DBHelper {
    private static final int MSG_WHAT_CHECK_EXIST  = 0;

    private BGHandler               mBgHandler  = null;
    private CheckExistDoneReceiver  mDupRcvr    = null;

    public interface CheckExistDoneReceiver {
        void checkExistDone(DBHelper helper, CheckExistArg arg,
                          boolean[] results, Err err);
    }

    public static class CheckExistArg {
        public final Object                 tag;
        public final YTVideoFeed.Entry[]    ents;
        public CheckExistArg(Object aTag, YTVideoFeed.Entry[] aEnts) {
            tag = aTag;
            ents = aEnts;
        }
    }

    private static class BGThread extends HandlerThread {
        BGThread() {
            super("YTSearchHelper.BGThread",Process.THREAD_PRIORITY_BACKGROUND);
        }

        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }
    }

    private static class BGHandler extends Handler {
        private final DBHelper  helper;
        BGHandler(Looper    looper,
                  DBHelper  aHelper) {
            super(looper);
            helper = aHelper;
        }

        private boolean[]
        checkExist(YTVideoFeed.Entry[] entries) throws YTMPException {
            // TODO
            // Should I check "entries[i].available" flag???
            boolean[] r = new boolean[entries.length];
            for (int i = 0; i < r.length; i++)
                r[i] = DB.get().containsVideo(entries[i].media.videoId);
            return r;
        }

        private void
        sendCheckExistDone(final CheckExistArg arg, final boolean[] results, final Err err) {
            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void
                run() {
                    CheckExistDoneReceiver rcvr = helper.getCheckExistDoneReceiver();
                    if (null != rcvr)
                        rcvr.checkExistDone(helper, arg, results, err);
                }
            });
            return;
        }

        private void
        handleCheckExist(CheckExistArg arg) {
            boolean[] r;
            try {
                r = checkExist(arg.ents);
            } catch (YTMPException e) {
                eAssert(Err.NO_ERR != e.getError());
                sendCheckExistDone(arg, null, e.getError());
                return;
            }
            sendCheckExistDone(arg, r, Err.NO_ERR);
        }

        void
        close() {
            removeMessages(MSG_WHAT_CHECK_EXIST);
            ((HandlerThread)getLooper().getThread()).quit();
        }

        @Override
        public void
        handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_WHAT_CHECK_EXIST:
                handleCheckExist((CheckExistArg)msg.obj);
                break;
            }
        }
    }

    CheckExistDoneReceiver
    getCheckExistDoneReceiver() {
        return mDupRcvr;
    }

    // ======================================================================
    //
    //
    //
    // ======================================================================
    public void
    setCheckExistDoneReceiver(CheckExistDoneReceiver rcvr) {
        mDupRcvr = rcvr;
    }

    public void
    checkExistAsync(CheckExistArg arg) {
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
        if (null != mBgHandler)
            mBgHandler.close();
    }
}
