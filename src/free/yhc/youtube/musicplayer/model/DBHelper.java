package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

public class DBHelper {
    private static final int MSG_WHAT_CHECK_EXIST  = 0;

    private final DB    mDb = DB.get();

    private BGHandler               mBgHandler  = null;
    private CheckExistDoneReceiver  mDupRcvr    = null;

    public interface CheckExistDoneReceiver {
        void checkExistDone(DBHelper helper, CheckExistArg arg,
                          boolean[] results, Err err);
    }

    public static class CheckExistArg {
        public final Object                 tag;
        public final YTSearchApi.Entry[]    ents;
        public CheckExistArg(Object aTag, YTSearchApi.Entry[] aEnts) {
            tag = aTag;
            ents = aEnts;
        }
    }

    private class BGThread extends HandlerThread {
        BGThread() {
            super("YTSearchHelper.BGThread",Process.THREAD_PRIORITY_BACKGROUND);
        }

        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }
    }

    private class BGHandler extends Handler {
        BGHandler(Looper looper) {
            super(looper);
        }

        private boolean[]
        checkExist(YTSearchApi.Entry[] entries) throws YTMPException {
            // TODO
            // Should I check "entries[i].available" flag???
            boolean[] r = new boolean[entries.length];
            for (int i = 0; i < r.length; i++)
                r[i] = mDb.existMusic(entries[i].media.videoId);
            return r;
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

    private void
    sendCheckExistDone(final CheckExistArg arg, final boolean[] results, final Err err) {
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void
            run() {
                mDupRcvr.checkExistDone(DBHelper.this, arg, results, err);
            }
        });
        return;
    }

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
        mBgHandler = new BGHandler(hThread.getLooper());
    }

    public void
    close() {
        // TODO
        // Stop running thread!
        // Need to check that below code works as expected perfectly.
        // "interrupting thread" is quite annoying and unpredictable job!
        if (null != mBgHandler) {
            mBgHandler.getLooper().getThread().interrupt();
            mBgHandler.removeMessages(MSG_WHAT_CHECK_EXIST);
        }
    }
}
