/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015, 2016
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.netmbuddy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import android.R.style;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import free.yhc.baselib.Logger;
import free.yhc.abaselib.ux.DialogTask;
import free.yhc.netmbuddy.core.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.share.ImportJob;
import free.yhc.netmbuddy.utils.UxUtil;

public class ImportShareActivity extends Activity implements
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(ImportShareActivity.class, Logger.LOGLV_DEFAULT);

    private DialogTask mDiagTask = null;
    private ZipInputStream mZis = null;

    private void
    importShare(final ZipInputStream zis) {
        if (null == mZis) {
            UxUtil.showTextToast(R.string.msg_fail_to_access_data);
            finish();
            return;
        }

        CharSequence msg = getResources().getText(R.string.msg_confirm_import_share) + "\n";
        UxUtil.ConfirmAction action = new UxUtil.ConfirmAction() {
            @Override
            public void
            onPositive(@NonNull Dialog dialog) {
                doImport(zis);
            }

            @Override
            public void
            onNegative(@NonNull Dialog dialog) {
                finish();
            }
        };
        UxUtil.buildConfirmDialog(this,
                                  getResources().getText(R.string.app_name),
                                  msg,
                                  action)
                .show();
    }

    private void
    doImport(final ZipInputStream zis) {
        DialogTask.Builder<DialogTask.Builder> b
                = new DialogTask.Builder<>(this, ImportJob.create(zis));
        b.setStyle(DialogTask.Style.PROGRESS);
        b.setTitle(R.string.app_name);
        b.setMessage(getResources().getText(R.string.importing_share) + "\n"
                     + getResources().getText(R.string.msg_warn_background_not_allowed));
        b.setCancelButtonText(R.string.cancel);
        b.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void
            onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        mDiagTask = b.create();
        if (!mDiagTask.start())
            P.bug();
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UnexpectedExceptionHandler.get().registerModule(this);
        Uri uri = getIntent().getData();
        P.bug(null != uri);
        assert null != uri;
        InputStream is = null;
        try {
            if ("file".equals(uri.getScheme()))
                is = new FileInputStream(uri.getPath());
            else if ("content".equals(uri.getScheme()))
                is = getContentResolver().openInputStream(uri);
            else
                P.bug(false);
        } catch (FileNotFoundException | SecurityException e) {
            UxUtil.showTextToast(R.string.msg_fail_to_access_data);
            finish();
            return;
        }

        mZis = new ZipInputStream(is);
        importShare(mZis);
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
            mDiagTask.cancel();
        super.onStop();
    }

    @Override
    protected void
    onDestroy() {
        if (null != mDiagTask)
            mDiagTask.forceDismissDialog();

        try {
            if (null != mZis)
                mZis.close();
        } catch (IOException ignored) { }
        UnexpectedExceptionHandler.get().unregisterModule(this);
        super.onDestroy();
    }

    @Override
    protected void
    onApplyThemeResource(@NonNull Resources.Theme theme, int resid, boolean first) {
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
