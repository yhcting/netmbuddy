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
import static free.yhc.netmbuddy.utils.Utils.logE;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

import android.R.style;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import free.yhc.netmbuddy.share.Share;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class ImportShareActivity extends Activity {
    private DiagAsyncTask   mDiagTask   = null;
    private AlertDialog     mDiag       = null;
    private ZipInputStream  mZis        = null;

    /**
     * Cancel is NOT ALLOWED.
     */
    private class Preparer extends DiagAsyncTask.Worker {
        private final Share.ImporterI   _mImporter;
        private Share.ImportPrepareResult   _mResult = null;

        Preparer(Share.ImporterI importer) {
            _mImporter = importer;
        }

        Share.ImportPrepareResult
        result() {
            return _mResult;
        }

        @Override
        public Err
        doBackgroundWork(final DiagAsyncTask task) {
            _mResult = _mImporter.prepare();
            return Err.map(_mResult.err);
        }

        @Override
        public void
        onPostExecute(DiagAsyncTask task, Err result) {
            if (Err.NO_ERR != result)
                UiUtils.showTextToast(ImportShareActivity.this, result.getMessage());
        }
    }

    private class Importer extends DiagAsyncTask.Worker {
        private final Share.ImporterI               _mImporter;
        private final Share.ImportPrepareResult     _mIpr;

        private Share.ImportResult      _mResult = null;

        Importer(Share.ImporterI importer, Share.ImportPrepareResult ipr) {
            _mIpr = ipr;
            _mImporter = importer;
        }

        private CharSequence
        getReportText(boolean cancelled) {
            int success = 0;
            int fail = 0;
            Share.ImportResult r = _mResult;
            if (null != r) {
                success = r.success.get();
                fail = r.fail.get();
            } else
                logE("ImportShareActivity : Unexpected Error (returned result is null!)\n"
                     + "   recovered");

            CharSequence title = " [ " + Utils.getResText(R.string.app_name) + " ]\n"
                                 + Utils.getResText(R.string.import_) + " : "
                                     + Utils.getResText(cancelled?
                                                        R.string.cancelled:
                                                        R.string.done)
                                     + "\n"
                                 + r.message;
            if (Share.Err.NO_ERR != r.err)
                title = title + "\n" + Utils.getResText(Err.map(r.err).getMessage());

            return title + "\n"
                   + "  " + Utils.getResText(R.string.done) + " : " + success + "\n"
                   + "  " + Utils.getResText(R.string.error) + " : " + fail;
        }

        private void
        onEnd(boolean cancelled) {
            UiUtils.showTextToast(ImportShareActivity.this, getReportText(cancelled), true);
        }

        @Override
        public void
        onPreExecute(DiagAsyncTask task) {
        }

        @Override
        public Err
        doBackgroundWork(final DiagAsyncTask task) {
            Share.OnProgressListener listener = new Share.OnProgressListener() {
                @Override
                public void
                onProgress(float prog) {
                    task.publishProgress((int)(prog * 100));
                }
            };
            _mResult = _mImporter.execute(null, listener);
            return Err.NO_ERR;
        }

        @Override
        public void
        onCancel(DiagAsyncTask task) {
            _mImporter.cancel();
        }

        @Override
        public void
        onPostExecute(DiagAsyncTask task, Err result) {
            onEnd(false);
        }

        @Override
        public void
        onCancelled(DiagAsyncTask task) {
            onEnd(true);
        }
    };

    private void
    prepareImport() {
        if (null == mZis) {
            UiUtils.showTextToast(this, R.string.msg_fail_to_access_data);
            finish();
            return;
        }

        final Share.ImporterI importer = Share.buildImporter(mZis);
        final Preparer preparer = new Preparer(importer);
        mDiagTask = new DiagAsyncTask(this,
                                      preparer,
                                      DiagAsyncTask.Style.SPIN,
                                      R.string.preparing,
                                      true);
        mDiagTask.setTitle(R.string.app_name);
        mDiagTask.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void
            onDismiss(DialogInterface dialog) {
                Share.ImportPrepareResult ipr = preparer.result();
                if (null == ipr
                    || Share.Err.NO_ERR != ipr.err)
                    finish();
                else
                    startImport(importer, ipr);
            }
        });
        mDiagTask.run();
    }

    private void
    startImport(final Share.ImporterI importer, final Share.ImportPrepareResult ipr) {
        CharSequence msg = getResources().getText(R.string.msg_confirm_import_share) + "\n";
        switch (ipr.type) {
        case PLAYLIST:
            msg = msg + "[ " + getResources().getText(R.string.playlist) + " ]\n";
            break;
        }
        msg = msg + ipr.message;

        final AtomicBoolean cancelled = new AtomicBoolean(true);
        UiUtils.ConfirmAction action = new UiUtils.ConfirmAction() {
            @Override
            public void
            onOk(Dialog dialog) {
                cancelled.set(false);
                doImport(importer, ipr);
            }

            @Override
            public void
            onCancel(Dialog dialog) {
                finish();
            }
        };

        mDiag = UiUtils.buildConfirmDialog(this,
                                           getResources().getText(R.string.app_name),
                                           msg,
                                           action);
        mDiag.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void
            onDismiss(DialogInterface dialog) {
                mDiag = null;
                if (cancelled.get())
                    finish();
            }
        });
        mDiag.show();
    }

    private void
    doImport(Share.ImporterI importer, Share.ImportPrepareResult ipr) {
        mDiagTask = new DiagAsyncTask(this,
                                      new Importer(importer, ipr),
                                      DiagAsyncTask.Style.PROGRESS,
                                      getResources().getText(R.string.importing_share) + "\n"
                                          + getResources().getText(R.string.msg_warn_background_not_allowed),
                                      true,
                                      // Importing SHOULD NOT be cancelled by INTERRUPT.
                                      // Canceling by interrupt causes early return before
                                      //   importing is 'really' finished.
                                      // In that case, ImportResult value is NOT CORRECT!
                                      false);
        mDiagTask.setTitle(R.string.app_name);
        mDiagTask.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void
            onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        mDiagTask.run();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        eAssert(null != uri);
        InputStream is = null;
        try {
            if ("file".equals(uri.getScheme()))
                is = new FileInputStream(uri.getPath());
            else if ("content".equals(uri.getScheme()))
                is = getContentResolver().openInputStream(uri);
            else
                eAssert(false);
        } catch (FileNotFoundException e) {
            UiUtils.showTextToast(this, R.string.msg_fail_to_access_data);
            finish();
            return;
        } catch (SecurityException e) {
            UiUtils.showTextToast(this, R.string.msg_fail_to_access_data);
            finish();
            return;
        }

        mZis = new ZipInputStream(is);
        prepareImport();
    }

    @Override
    public void
    onNewIntent(Intent intent) {

    }

    @Override
    protected void
    onResume() {
        super.onResume();
    }

    @Override
    protected void
    onPause() {
        super.onPause();
    }

    @Override
    protected void
    onStop() {
        if (null != mDiagTask)
            mDiagTask.userCancel();
        super.onStop();
    }

    @Override
    protected void
    onDestroy() {
        if (null != mDiag)
            mDiag.dismiss();

        if (null != mDiagTask)
            mDiagTask.forceDismissDialog();

        try {
            if (null != mZis)
                mZis.close();
        } catch (IOException ignored) { }
        super.onDestroy();
    }

    @Override
    protected void
    onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, resid, first);
        // no background panel is shown
        theme.applyStyle(style.Theme_Panel, true);
    }

    @Override
    public void
    onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Do nothing!
    }

    @Override
    public void
    onBackPressed() {
        super.onBackPressed();
    }
}
