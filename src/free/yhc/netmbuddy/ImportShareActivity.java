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

import static free.yhc.netmbuddy.utils.Utils.logE;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import free.yhc.netmbuddy.model.Share;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class ImportShareActivity extends Activity {
    private DiagAsyncTask   mDiag   = null;
    private ZipInputStream  mZis    = null;

    private static class Importer extends DiagAsyncTask.Worker {
        private final Activity          _mActivity;
        private final ZipInputStream    _mIs;
        private final AtomicReference<Share.ImportResult>   _mResult
            = new AtomicReference<Share.ImportResult>(null);

        private
        Importer(Activity activity, ZipInputStream is) {
            _mActivity = activity;
            _mIs = is;
        }

        private CharSequence
        getReportText() {
            int success = 0;
            int fail = 0;
            Share.ImportResult r = _mResult.get();
            if (null != r) {
                success = r.success.get();
                fail = r.fail.get();
            } else
                logE("ImportShareActivity : Unexpected Error (returned result is null!)\n"
                     + "   recovered");

            CharSequence title = " [ " + Utils.getResText(R.string.app_name) + " ]\n";
            if (Share.Err.NO_ERR != r.err)
                title = Utils.getResText(Err.map(r.err).getMessage());

            return title + "\n"
                   + "  " + Utils.getResText(R.string.done) + " : " + success + "\n"
                   + "  " + Utils.getResText(R.string.error) + " : " + fail;
        }

        private void
        onEnd() {
            UiUtils.showTextToast(_mActivity, getReportText());
            Utils.resumeApp();
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
            _mResult.set(Share.importShare(_mIs, listener));
            return Err.NO_ERR;
        }

        @Override
        public void
        onPostExecute(DiagAsyncTask task, Err result) {
            onEnd();
        }

        @Override
        public void
        onCancelled(DiagAsyncTask task) {
            onEnd();
        }
    };

    /**
     * ImportShareActivity has responsibility regarding closing input stream.
     * @param savedInstanceState
     * @param is
     */
    protected void
    onCreateInternal(Bundle savedInstanceState, InputStream is) {
        if (null == is) {
            UiUtils.showTextToast(this, R.string.msg_fail_to_access_data);
            finish();
            return;
        }


        mZis = new ZipInputStream(is);
        mDiag = new DiagAsyncTask(this,
                                  new Importer(this, mZis),
                                  DiagAsyncTask.Style.PROGRESS,
                                  R.string.importing_share,
                                  true);
        mDiag.setTitle(R.string.app_name);
        mDiag.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void
            onDismiss(DialogInterface dialog) {
                finish();
            }
        });

        mDiag.run();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        super.onStop();
    }

    @Override
    protected void
    onDestroy() {
        try {
            if (null != mZis)
                mZis.close();
        } catch (IOException ignored) { }
        super.onDestroy();
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
