/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015
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

//
// Not used yet
// For future use.
//

package free.yhc.netmbuddy.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import free.yhc.netmbuddy.utils.ReportUtils;
import free.yhc.netmbuddy.utils.Utils;

public class UnexpectedExceptionHandler implements
UncaughtExceptionHandler {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(UnexpectedExceptionHandler.class);

    private static final String UNKNOWN = "unknown";

    private static UnexpectedExceptionHandler sInstance = null;

    // This module to capturing unexpected exception.
    // So this SHOULD have minimum set of code in constructor,
    //   because this module SHOULD be instantiate as early as possible
    //   before any other module is instantiated
    //
    // Dependency on only following modules are allowed
    // - Utils
    private final Thread.UncaughtExceptionHandler mOldHandler = Thread.getDefaultUncaughtExceptionHandler();
    private final LinkedList<Evidence> mMods = new LinkedList<>();
    private final PackageReport mPr = new PackageReport();
    private final BuildReport mBr = new BuildReport();

    public enum DumpLevel {
        FULL
    }

    public interface Evidence {
        String dump(DumpLevel lvl);
    }

    private class PackageReport {
        String packageName = UNKNOWN;
        String versionName = UNKNOWN;
        String filesDir    = UNKNOWN;
    }
    // Useful Informations
    private class BuildReport {
        String androidVersion = UNKNOWN;
        String board          = UNKNOWN;
        String brand          = UNKNOWN;
        String device         = UNKNOWN;
        String display        = UNKNOWN;
        String fingerPrint    = UNKNOWN;
        String host           = UNKNOWN;
        String id             = UNKNOWN;
        String manufacturer   = UNKNOWN;
        String model          = UNKNOWN;
        String product        = UNKNOWN;
        String tags           = UNKNOWN;
        long   time           = 0;
        String type           = UNKNOWN;
        String user           = UNKNOWN;
    }

    // ========================
    // Privates
    // ========================
    private void
    setEnvironmentInfo(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            mPr.versionName = pi.versionName;
            mPr.packageName = pi.packageName;
        } catch (NameNotFoundException ignore) { }

        mPr.filesDir        = context.getFilesDir().getAbsolutePath();
        mBr.model           = android.os.Build.MODEL;
        mBr.androidVersion  = android.os.Build.VERSION.RELEASE;
        mBr.board           = android.os.Build.BOARD;
        mBr.brand           = android.os.Build.BRAND;
        mBr.device          = android.os.Build.DEVICE;
        mBr.display         = android.os.Build.DISPLAY;
        mBr.fingerPrint     = android.os.Build.FINGERPRINT;
        mBr.host            = android.os.Build.HOST;
        mBr.id              = android.os.Build.ID;
        mBr.product         = android.os.Build.PRODUCT;
        mBr.tags            = android.os.Build.TAGS;
        mBr.time            = android.os.Build.TIME;
        mBr.type            = android.os.Build.TYPE;
        mBr.user            = android.os.Build.USER;
    }


    private void
    appendCommonReport(StringBuilder report) {
        //noinspection StringConcatenationInsideStringBufferAppend
        report.append("==================== Package Information ==================\n")
              .append("  - name        : " + mPr.packageName + "\n")
              .append("  - version     : " + mPr.versionName + "\n")
              .append("  - filesDir    : " + mPr.filesDir + "\n")
              .append("\n")
              .append("===================== Device Information ==================\n")
              .append("  - androidVer  : " + mBr.androidVersion + "\n")
              .append("  - board       : " + mBr.board + "\n")
              .append("  - brand       : " + mBr.brand + "\n")
              .append("  - device      : " + mBr.device + "\n")
              .append("  - display     : " + mBr.display + "\n")
              .append("  - fingerprint : " + mBr.fingerPrint + "\n")
              .append("  - host        : " + mBr.host + "\n")
              .append("  - id          : " + mBr.id + "\n")
              .append("  - manufactuere: " + mBr.manufacturer + "\n")
              .append("  - model       : " + mBr.model + "\n")
              .append("  - product     : " + mBr.product + "\n")
              .append("  - tags        : " + mBr.tags + "\n")
              .append("  - time        : " + mBr.time + "\n")
              .append("  - type        : " + mBr.type + "\n")
              .append("  - user        : " + mBr.user + "\n")
              .append("\n\n");
    }

    private UnexpectedExceptionHandler() {
        setEnvironmentInfo(Utils.getAppContext());
    }
    // ========================
    // Publics
    // ========================

    // Get singleton instance,.
    public static UnexpectedExceptionHandler
    get() {
        if (null == sInstance)
            sInstance = new UnexpectedExceptionHandler();
        return sInstance;
    }

    /**
     * register module that will be dumped when unexpected exception is issued.
     * @param m module providing evidences
     */
    public boolean
    registerModule(Evidence m) {
        if (null == m)
            return false;

        synchronized (mMods) {
            if (mMods.contains(m))
                return false;

            mMods.addLast(m);
            return true;
        }
    }

    public boolean
    unregisterModule(Evidence m) {
        synchronized (mMods) {
            return mMods.remove(m);
        }
    }

    @Override
    public void
    uncaughtException(Thread thread, Throwable ex) {
        StringBuilder report = new StringBuilder();
        appendCommonReport(report);

        // collect dump informations
        for (Evidence tm : mMods)
            report.append(tm.dump(DumpLevel.FULL)).append("\n\n");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        report.append(sw.toString());
        pw.close();

        ReportUtils.storeErrReport(report.toString());
        mOldHandler.uncaughtException(thread, ex);
    }
}
