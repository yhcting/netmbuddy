/******************************************************************************
 *    Copyright (C) 2012, 2013, 2014 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of NetMBuddy.
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
 *    along with this program.	If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import free.yhc.netmbuddy.model.BGTask;
import free.yhc.netmbuddy.model.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.utils.Utils;

public class DiagAsyncTask extends BGTask<Err> implements
DialogInterface.OnDismissListener,
View.OnClickListener,
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(DiagAsyncTask.class);

    private Context         mContext        = null;
    private ProgressDialog  mDialog         = null;
    private CharSequence    mTitle          = null;
    private CharSequence    mMessage        = null;
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
        onPreExecute(DiagAsyncTask task) { }

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
                         CharSequence msg,
                         boolean cancelable,
                         boolean interruptOnCancel) {
        super();
        UnexpectedExceptionHandler.get().registerModule(this);
        mContext= context;
        mWorker = listener;
        mMessage = msg;
        mStyle  = style;
        mCancelable = cancelable;
        mInterruptOnCancel = interruptOnCancel;
    }

    public DiagAsyncTask(Context context,
            Worker listener,
            Style style,
            int msg,
            boolean cancelable,
            boolean interruptOnCancel) {
        this(context, listener, style, context.getResources().getText(msg), cancelable, interruptOnCancel);
    }

    public DiagAsyncTask(Context context,
                         Worker listener,
                         Style style,
                         CharSequence msg,
                         boolean cancelable) {
        this(context, listener, style, msg, cancelable, true);
    }

    public DiagAsyncTask(Context context,
            Worker listener,
            Style style,
            int msg,
            boolean cancelable) {
        this(context, listener, style, context.getResources().getText(msg), cancelable, true);
    }

    public DiagAsyncTask(Context context,
                         Worker listener,
                         Style style,
                         CharSequence msg) {
        this(context, listener, style, msg, false, true);
    }

    public DiagAsyncTask(Context context,
            Worker listener,
            Style style,
            int msg) {
        this(context, listener, style, context.getResources().getText(msg), false, true);
    }

    public void
    setOnDismissListener(DialogInterface.OnDismissListener listener) {
        eAssert(Utils.isUiThread());
        mOnDismissListener = listener;
    }

    public void
    setTitle(CharSequence title) {
        mTitle = title;
    }

    public void
    setTitle(int title) {
        setTitle(mContext.getResources().getText(title));
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

    public boolean
    userCancel() {
        if (!mCancelable || mUserCancelled)
            return false;

        mUserCancelled = true;
        mDialog.setMessage(mContext.getResources().getText(R.string.msg_wait_cancel));
        return super.cancel(mInterruptOnCancel);
    }

    /**
     * This is used usually to avoid "leaked window..." error.
     * But be careful to use it.
     * This function dismiss ONLY dialog and doens't cancel background job!
     */
    public void
    forceDismissDialog() {
        if (null != mDialog)
            mDialog.dismiss();
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    @Override
    protected Err
    doAsyncTask() {
        //logI("* Start background Job : SpinSyncTask\n");
        try {
            Err ret = Err.NO_ERR;
            if (null != mWorker)
                ret = mWorker.doBackgroundWork(this);
            return ret;
        } catch (Throwable e) {
            return Err.UNKNOWN;
        }
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

        // See comments in onPostRun
        try {
            mDialog.dismiss();
        } catch (IllegalArgumentException ignored) { }
    }

    @Override
    protected void
    onPostRun(Err result) {
        // In normal case, onPostExecute is not called in case of 'user-cancel'.
        // below code is for safety.
        if (!mUserCancelled && null != mWorker)
            mWorker.onPostExecute(this, result);

        // This may be called after context(ie. Activity) is destroyed.
        // In this case, dialog is no more attached windowManager and exception is issued.
        // we need to ignore this exception here with out concern.
        try {
            mDialog.dismiss();
        } catch (IllegalArgumentException ignored) { }
    }

    @Override
    protected void
    onCancel() {
        if (null != mWorker)
            mWorker.onCancel(this);
    }

    @Override
    protected void
    onPreRun() {
        mDialog = new ProgressDialog(mContext);
        if (null != mTitle)
            mDialog.setTitle(mTitle);
        if (null != mMessage)
            mDialog.setMessage(mMessage);
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

        mDialog.setOnDismissListener(this);

        // NOTE
        // See below codes.
        // In case of cancelable dialog, set dummy onClick is registered.
        // And then, REAL onClick is re-registered for the button directly.
        // This is to workaround Android Framework's ProgressDialog policy.
        // According to Android Framework, ProgressDialog dismissed as soon as button is clicked.
        // But, this is not what I expected.
        // Below code is a way of workaround this policy.
        // For details, See "https://groups.google.com/forum/?fromgroups=#!topic/android-developers/-1bIchuFASQ".
        if (mCancelable) {
            // Set dummy onClick listener.
            mDialog.setButton(Dialog.BUTTON_POSITIVE,
                              mContext.getResources().getText(R.string.cancel),
                              new DialogInterface.OnClickListener() {
                                  @Override
                                  public void
                                  onClick(DialogInterface dialog, int which) {
                                  }
                              });
        }
        mDialog.show();
        if (mCancelable)
            mDialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(this);

        if (null != mWorker)
            mWorker.onPreExecute(this);
    }

    /**
     * handle event - cancel button is clicked.
     */
    @Override
    public void
    onClick(View view) {
        userCancel();
    }

    @Override
    public void
    onDismiss(DialogInterface dialogI) {
        if (null != mOnDismissListener)
            mOnDismissListener.onDismiss(dialogI);
        UnexpectedExceptionHandler.get().unregisterModule(this);
    }
}