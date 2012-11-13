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

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import free.yhc.netmbuddy.model.BGTask;
import free.yhc.netmbuddy.utils.Utils;

public class DiagAsyncTask extends BGTask<Err> implements
DialogInterface.OnDismissListener,
DialogInterface.OnClickListener {
    private Context         mContext        = null;
    private ProgressDialog  mDialog         = null;
    private int             mMsgid          = -1;
    private Style           mStyle          = Style.SPIN;
    private Worker          mWorker         = null;
    private boolean         mUserCancelled  = false;
    private boolean         mCancelable     = true;
    private boolean         mInterruptOnCancel = true;
    private DialogInterface.OnDismissListener mOnDismissListener = null;

    public static abstract class Worker {
        public abstract Err
        doBackgroundWork(DiagAsyncTask task);

        public void
        onPostExecute(DiagAsyncTask task, Err result) { }

        public void
        onCancel(DiagAsyncTask task) { }

        public void
        onCancelled(DiagAsyncTask task) { }
    }

    public static enum Style {
        SPIN        (ProgressDialog.STYLE_SPINNER),
        PROGRESS    (ProgressDialog.STYLE_HORIZONTAL);

        private int style;

        Style(int aStyle) {
            style = aStyle;
        }

        public int
        getStyle() {
            return style;
        }
    }

    public DiagAsyncTask(Context context,
                         Worker listener,
                         Style style,
                         int msgid,
                         boolean cancelable,
                         boolean interruptOnCancel) {
        super();
        mContext= context;
        mWorker = listener;
        mMsgid  = msgid;
        mStyle  = style;
        mCancelable = cancelable;
        mInterruptOnCancel = interruptOnCancel;
    }

    public DiagAsyncTask(Context context,
                         Worker listener,
                         Style style,
                         int msgid,
                         boolean cancelable) {
        this(context, listener, style, msgid, cancelable, true);
    }

    public DiagAsyncTask(Context context,
                         Worker listener,
                         Style style,
                         int msgid) {
        this(context, listener, style, msgid, false, true);
    }

    public void
    setOnDismissListener(DialogInterface.OnDismissListener listener) {
        eAssert(Utils.isUiThread());
        mOnDismissListener = listener;
    }

    public void
    publishMessage(final String message) {
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void
            run() {
                mDialog.setMessage(message);
            }
        });
    }

    @Override
    protected Err
    doAsyncTask() {
        //logI("* Start background Job : SpinSyncTask\n");
        Err ret = Err.NO_ERR;
        if (null != mWorker)
            ret = mWorker.doBackgroundWork(this);
        return ret;
    }

    @Override
    public void
    onProgress(int percent) {
        if (null == mDialog)
            return;

        mDialog.setProgress(percent);
    }

    @Override
    public void
    onCancelled() {
        if (null != mWorker)
            mWorker.onCancelled(this);

        mDialog.dismiss();
    }

    @Override
    protected void
    onPostRun(Err result) {
        // In normal case, onPostExecute is not called in case of 'user-cancel'.
        // below code is for safety.
        if (!mUserCancelled && null != mWorker)
            mWorker.onPostExecute(this, result);

        mDialog.dismiss();
    }

    @Override
    protected void
    onPreRun() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage(mContext.getResources().getText(mMsgid));
        mDialog.setProgressStyle(mStyle.getStyle());
        mDialog.setMax(100); // percent
        // To prevent dialog is dismissed unexpectedly by back-key
        mDialog.setCancelable(false);

        // To prevent dialog is dismissed unexpectedly by search-key (in Gingerbread)
        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean
            onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_SEARCH == keyCode
                    && 0 == event.getRepeatCount()) {
                    return true;
                }
                return false;
            }
        });

        if (mCancelable)
            mDialog.setButton(mContext.getResources().getText(R.string.cancel), this);

        mDialog.setOnDismissListener(this);
        mDialog.show();
    }

    @Override
    public void
    onClick(DialogInterface dialogI, int which) {
        eAssert(mCancelable);
        mUserCancelled = true;
        mDialog.setMessage(mContext.getResources().getText(R.string.msg_wait_cancel));
        if (null != mWorker)
            mWorker.onCancel(this);
        super.cancel(mInterruptOnCancel);
    }

    @Override
    public void
    onDismiss(DialogInterface dialogI) {
        if (null != mOnDismissListener)
            mOnDismissListener.onDismiss(dialogI);
    }

    @Override
    protected void
    finalize() throws Throwable {
        super.finalize();
    }
}