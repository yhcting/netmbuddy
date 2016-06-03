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

package free.yhc.netmbuddy.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;

import free.yhc.baselib.Logger;
import free.yhc.abaselib.util.AUtil;
import free.yhc.baselib.util.FileUtil;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.core.PolicyConstant;

public class ReportUtil {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(ReportUtil.class, Logger.LOGLV_DEFAULT);

    private static final File sErrLogFile = new File(PolicyConstant.APPDATA_ERRLOG);

    private static String
    getSubjectPrefix() {
        return "[ " + AUtil.getResString(R.string.app_name) + " ] ";
    }

    private static void
    cleanReportFile(File f) {
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private static void
    storeReport(File f, String report) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
            bw.write(report);
            bw.flush();
            bw.close();
        } catch (IOException ignored) { }
    }

    /**
     * Send stored report - crash, improvement etc - to developer as E-mail.
     */
    private static void
    sendReportMail(Context context, File reportf, String subject) {
        if (!Util.isNetworkAvailable())
            return;

        if (!reportf.exists())
            return; // nothing to do
        String report;
        try {
            report = FileUtil.readTextFile(reportf) + "\n\n";
        } catch (IOException e) {
            // ignore htis report
            return;
        }

        // we successfully read all log files.
        // let's clean it.
        cleanReportFile(reportf);
        Util.sendMail(context,
                      PolicyConstant.REPORT_RECEIVER,
                      subject,
                      report,
                      null);
    }

    /**
     * Overwrite
     */
    public static void
    storeErrReport(String report) {
        if (!Util.isPrefErrReport())
            return;
        storeReport(new File(PolicyConstant.APPDATA_ERRLOG), report);
    }

    public static void
    storeYtPage(String page) {
        storeReport(new File(PolicyConstant.APPDATA_PARSELOG), page);
    }

    /**
     * Send stored report - crash, improvement etc - to developer as E-mail.
     */
    public static void
    sendErrReport(Context context) {
        if (!Util.isPrefErrReport())
            return;
        sendReportMail(context,
                       sErrLogFile,
                       getSubjectPrefix() + context.getResources().getText(R.string.pref_err_report));
    }

    public static void
    sendFeedback(Context context) {
        Util.sendMail(context,
                      PolicyConstant.REPORT_RECEIVER,
                      getSubjectPrefix() + context.getResources().getText(R.string.feedback),
                      "",
                      null);
    }
}
