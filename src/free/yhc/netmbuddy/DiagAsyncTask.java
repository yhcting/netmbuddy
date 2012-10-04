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
import android.os.AsyncTask;
import free.yhc.netmbuddy.model.Err;

public class DiagAsyncTask extends AsyncTask<Object, Integer, Err> implements
DialogInterface.OnDismissListener,
DialogInterface.OnCancelListener,
DialogInterface.OnClickListener
{
    private String          mName           = ""; // for debugging
    private Context         mContext        = null;
    private ProgressDialog  mDialog         = null;
    private int             mMsgid          = -1;
    private Style           mStyle          = Style.SPIN;
    private Worker          mWorker         = null;
    private boolean         mUserCancelled  = false;
    private boolean         mCancelable     = true;

    public interface Worker {
        Err  doBackgroundWork(DiagAsyncTask task, Object... objs);
        void onPostExecute(DiagAsyncTask task, Err result);
        void onCancel(DiagAsyncTask task);
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

    private void
    constructor(Context context, Worker worker, Style style, int msgid, boolean cancelable) {
        mContext= context;
        mWorker = worker;
        mMsgid  = msgid;
        mStyle  = style;
        mCancelable = cancelable;
    }

    DiagAsyncTask(Context context, Worker listener, Style style, int msgid) {
        super();
        constructor(context, listener, style, msgid, true);
    }

    DiagAsyncTask(Context context, Worker listener,
                  Style style, int msgid, boolean cancelable) {
        super();
        constructor(context, listener, style, msgid, cancelable);
    }

    public void
    setName(String newName) {
        mName = newName;
    }

    public void
    publishProgress(int percent) {
        if (null == mDialog)
            return;

        mDialog.setProgress(percent);
    }

    // return :
    @Override
    protected Err
    doInBackground(Object... objs) {
        //logI("* Start background Job : SpinSyncTask\n");
        Err ret = Err.NO_ERR;
        if (null != mWorker)
            ret = mWorker.doBackgroundWork(this, objs);
        return ret;
    }

    private boolean
    cancelWork() {
        if (!mCancelable)
            return false;
        // See comments in BGTaskUpdateChannel.cancel()
        mUserCancelled = true;
        cancel(true);
        return true;
    }

    @Override
    public void
    onCancelled() {
        mDialog.dismiss();
    }

    @Override
    public void
    onCancel(DialogInterface dialogI) {
        if (!mCancelable)
            return;
        cancelWork();
        if (null != mWorker)
            mWorker.onCancel(this);
    }

    @Override
    public void
    onClick(DialogInterface dialogI, int which) {
        if (!mCancelable)
            return;
        mDialog.setMessage(mContext.getResources().getText(R.string.msg_wait_cancel));
        mDialog.cancel();
    }

    @Override
    protected void
    onPostExecute(Err result) {
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
    onPreExecute() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage(mContext.getResources().getText(mMsgid));
        mDialog.setProgressStyle(mStyle.getStyle());
        mDialog.setMax(100); // percent
        mDialog.setCanceledOnTouchOutside(false);

        if (mCancelable) {
            mDialog.setButton(mContext.getResources().getText(R.string.cancel), this);
            mDialog.setOnCancelListener(this);
        } else
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