/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of Feeder.
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

package free.yhc.youtube.musicplayer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import free.yhc.youtube.musicplayer.model.Err;

public class SpinAsyncTask extends AsyncTask<Object, Integer, Err> implements
DialogInterface.OnDismissListener,
DialogInterface.OnCancelListener,
DialogInterface.OnClickListener
{
    private String          name         = ""; // for debugging
    private Context         context      = null;
    private ProgressDialog  dialog       = null;
    private int             msgid        = -1;
    private Worker          worker       = null;
    private boolean         userCancelled= false;
    private boolean         cancelable   = true;

    interface Worker {
        Err  doBackgroundWork(SpinAsyncTask task, Object... objs);
        void onPostExecute(SpinAsyncTask task, Err result);
        void onCancel(SpinAsyncTask task);
    }

    private void
    constructor(Context aContext, Worker aWorker, int aMsgid, boolean aCancelable) {
        context = aContext;
        worker  = aWorker;
        msgid   = aMsgid;
        cancelable = aCancelable;
    }

    SpinAsyncTask(Context context, Worker listener, int msgid) {
        super();
        constructor(context, listener, msgid, true);
    }

    SpinAsyncTask(Context context, Worker listener, int msgid, boolean cancelable) {
        super();
        constructor(context, listener, msgid, cancelable);
    }

    public void
    setName(String newName) {
        name = newName;
    }

    // return :
    @Override
    protected Err
    doInBackground(Object... objs) {
        //logI("* Start background Job : SpinSyncTask\n");
        Err ret = Err.NO_ERR;
        if (null != worker)
            ret = worker.doBackgroundWork(this, objs);
        return ret;
    }

    private boolean
    cancelWork() {
        if (!cancelable)
            return false;
        // See comments in BGTaskUpdateChannel.cancel()
        userCancelled = true;
        cancel(true);
        return true;
    }

    @Override
    public void
    onCancelled() {
        dialog.dismiss();
    }

    @Override
    public void
    onCancel(DialogInterface dialogI) {
        if (!cancelable)
            return;
        cancelWork();
        if (null != worker)
            worker.onCancel(this);
    }

    @Override
    public void
    onClick(DialogInterface dialogI, int which) {
        if (!cancelable)
            return;
        dialog.setMessage(context.getResources().getText(R.string.msg_wait_cancel));
        dialog.cancel();
    }

    @Override
    protected void
    onPostExecute(Err result) {
        //logI("* postExecuted : SpinSyncTask\n");
        dialog.dismiss();

        // In normal case, onPostExecute is not called in case of 'user-cancel'.
        // below code is for safety.
        if (userCancelled)
            return; // onPostExecute SHOULD NOT be called in case of user-cancel

        if (null != worker)
            worker.onPostExecute(this, result);
    }

    @Override
    public void
    onDismiss(DialogInterface dialog) {
    }

    @Override
    protected void
    onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getResources().getText(msgid));
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCanceledOnTouchOutside(false);

        if (cancelable) {
            dialog.setButton(context.getResources().getText(R.string.cancel), this);
            dialog.setOnCancelListener(this);
        } else
            dialog.setCancelable(false);

        dialog.setOnDismissListener(this);
        dialog.show();
    }

    @Override
    protected void
    finalize() throws Throwable {
        super.finalize();
    }
}