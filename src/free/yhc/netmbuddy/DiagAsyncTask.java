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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import free.yhc.netmbuddy.model.BGTask;
import free.yhc.netmbuddy.utils.Utils;

public class DiagAsyncTask extends BGTask<Err> implements
DialogInterface.OnDismissListener,
DialogInterface.OnClickListener
{
    private Context         mContext        = null;
    private ProgressDialog  mDialog         = null;
    private int             mMsgid          = -1;
    private Style           mStyle          = Style.SPIN;
    private Worker          mWorker         = null;
    private boolean         mUserCancelled  = false;
    private boolean         mCancelable     = true;
    private boolean         mInterruptOnCancel = true;

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

    DiagAsyncTask(Context context,
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

    DiagAsyncTask(Context context,
            Worker listener,
            Style style,
            int msgid,
            boolean cancelable) {
        this(context, listener, style, msgid, cancelable, true);
    }

    DiagAsyncTask(Context context,
                  Worker listener,
                  Style style,
                  int msgid) {
        this(context, listener, style, msgid, true, true);
    }

    public void
    updateMessage(final String message) {
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
        mDialog.dismiss();
        if (null != mWorker)
            mWorker.onCancelled(this);
    }

    @Override
    public void
    onClick(DialogInterface dialogI, int which) {
        if (!mCancelable)
            return;
        mUserCancelled = true;
        mDialog.setMessage(mContext.getResources().getText(R.string.msg_wait_cancel));
        if (null != mWorker)
            mWorker.onCancel(this);
        super.cancel(mInterruptOnCancel);
    }

    @Override
    protected void
    onPostRun(Err result) {
        //logI("* postExecuted : SpinSyncTask\n");
        mDialog.dismiss();

        // In normal case, onPostExecute is not called in case of 'user-cancel'.
        // below code is for safety.
        if (mUserCancelled)
            return; // onPostExecute SHOULD NOT be called in case of user-cancel

        if (null != mWorker)
            mWorker.onPostExecute(this, result);
    }

    @Override
    public void
    onDismiss(DialogInterface dialog) {
    }

    @Override
    protected void
    onPreRun() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage(mContext.getResources().getText(mMsgid));
        mDialog.setProgressStyle(mStyle.getStyle());
        mDialog.setMax(100); // percent
        mDialog.setCanceledOnTouchOutside(false);

        if (mCancelable)
            mDialog.setButton(mContext.getResources().getText(R.string.cancel), this);
        else
            mDialog.setCancelable(false);

        mDialog.setOnDismissListener(this);
        mDialog.show();
    }

    @Override
    protected void
    finalize() throws Throwable {
        super.finalize();
    }
}