/*****************************************************************************
 *    Copyright (C) 2012, 2013 Younghyung Cho. <yhcting77@gmail.com>
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

//
// Not used yet
// For future use.
//

package free.yhc.netmbuddy.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Iterator;
import java.util.LinkedList;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import free.yhc.netmbuddy.utils.ReportUtils;
import free.yhc.netmbuddy.utils.Utils;

public class UnexpectedExceptionHandler implements
UncaughtExceptionHandler {
    private static final boolean DBG = false;
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
    private final Thread.UncaughtExceptionHandler   mOldHandler = Thread.getDefaultUncaughtExceptionHandler();
    private final LinkedList<Evidence>         mMods = new LinkedList<Evidence>();
    private final PackageReport mPr = new PackageReport();
    private final BuildReport   mBr = new BuildReport();

    public enum DumpLevel {
        FULL
    }

    public interface Evidence {
        String dump(DumpLevel lvl);
    }

    private class PackageReport {
        String packageName          = UNKNOWN;
        String versionName          = UNKNOWN;
        String filesDir             = UNKNOWN;
    }
    // Useful Informations
    private class BuildReport {
        String androidVersion       = UNKNOWN;
        String board                = UNKNOWN;
        String brand                = UNKNOWN;
        String device               = UNKNOWN;
        String display              = UNKNOWN;
        String fingerPrint          = UNKNOWN;
        String host                 = UNKNOWN;
        String id                   = UNKNOWN;
        String manufacturer         = UNKNOWN;
        String model                = UNKNOWN;
        String product              = UNKNOWN;
        String tags                 = UNKNOWN;
        long   time                 = 0;
        String type                 = UNKNOWN;
        String user                 = UNKNOWN;
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
        } catch (NameNotFoundException e) {
            ; // ignore
        }
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
     * @param m
     * @return
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
        Iterator<Evidence> iter = mMods.iterator();
        while (iter.hasNext()) {
            Evidence tm = iter.next();
            report.append(tm.dump(DumpLevel.FULL)).append("\n\n");
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        report.append(sw.toString());
        pw.close();

        ReportUtils.storeErrReport(report.toString());
        mOldHandler.uncaughtException(thread, ex);
    }
}
