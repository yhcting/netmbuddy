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

package free.yhc.netmbuddy.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.model.Policy;

public class ReportUtils {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(ReportUtils.class);

    private static final File   sErrLogFile     = new File(Policy.APPDATA_ERRLOG);

    private static String
    getSubjectPrefix() {
        return "[ " + Utils.getResString(R.string.app_name) + " ] ";
    }

    private static void
    cleanReportFile(File f) {
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
     * @param context
     */
    private static void
    sendReportMail(Context context, File reportf, String subject) {
        if (!Utils.isNetworkAvailable())
            return;

        if (!reportf.exists())
            return; // nothing to do

        StringBuilder sbr = new StringBuilder();
        sbr.append(FileUtils.readTextFile(reportf)).append("\n\n");
        // we successfully read all log files.
        // let's clean it.
        cleanReportFile(reportf);

        Utils.sendMail(context,
                       Policy.REPORT_RECEIVER,
                       subject,
                       sbr.toString(),
                       null);
    }

    /**
     * Overwrite
     * @param report
     */
    public static void
    storeErrReport(String report) {
        if (!Utils.isPrefErrReport())
            return;
        storeReport(new File(Policy.APPDATA_ERRLOG), report);
    }

    /**
     * Send stored report - crash, improvement etc - to developer as E-mail.
     * @param context
     */
    public static void
    sendErrReport(Context context) {
        if (!Utils.isPrefErrReport())
            return;
        sendReportMail(context,
                       sErrLogFile,
                       getSubjectPrefix() + context.getResources().getText(R.string.pref_err_report));
    }

    public static void
    sendFeedback(Context context) {
        Utils.sendMail(context,
                       Policy.REPORT_RECEIVER,
                       getSubjectPrefix() + context.getResources().getText(R.string.feedback),
                       "",
                       null);
    }
}
