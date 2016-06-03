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

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.IOException;

import free.yhc.abaselib.ABaselib;
import free.yhc.abaselib.AppEnv;
import free.yhc.abaselib.util.AUtil;
import free.yhc.baselib.Logger;
import free.yhc.netmbuddy.core.PolicyConstant;
import free.yhc.netmbuddy.core.SearchSuggestionProvider;
import free.yhc.netmbuddy.core.TaskManager;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.NotiManager;
import free.yhc.netmbuddy.core.RTState;
import free.yhc.netmbuddy.core.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.core.YTPlayerLifeSupportService;
import free.yhc.netmbuddy.utils.Util;

public class YTMPApp extends Application {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static Logger P = null;

    @SuppressWarnings("FieldCanBeLocal")
    private static String PREF_KEY_APP_VERSION = "app_version";

    private static final String[] ESSENTIAL_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

    ///////////////////////////////////////////////////////////////////////////
    //
    // App version upgrade handling
    //
    ///////////////////////////////////////////////////////////////////////////
    private void
    convertOnOffPreferenceToBoolean(SharedPreferences prefs,
                                    SharedPreferences.Editor prefEd,
                                    Resources res,
                                    int keyId) {
        String onoff = prefs.getString(res.getString(keyId), null);
        if (null != onoff) {
            if (res.getString(R.string.cson).equals(onoff))
                prefEd.putBoolean(res.getString(keyId), true);
            else
                prefEd.putBoolean(res.getString(keyId), false);
        }
    }

    // Following preference is changed from list preference to checkbox preference.
    private void
    onUpgradeTo31(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEd = prefs.edit();
        Resources res = context.getResources();
        convertOnOffPreferenceToBoolean(prefs, prefEd, res, R.string.csshuffle);
        convertOnOffPreferenceToBoolean(prefs, prefEd, res, R.string.csrepeat);
        convertOnOffPreferenceToBoolean(prefs, prefEd, res, R.string.csuse_wifi_only);
        convertOnOffPreferenceToBoolean(prefs, prefEd, res, R.string.csstop_on_back);
        convertOnOffPreferenceToBoolean(prefs, prefEd, res, R.string.cslockscreen);
        convertOnOffPreferenceToBoolean(prefs, prefEd, res, R.string.cserr_report);
        prefEd.apply();
    }

    private void
    onAppUpgrade(Context context, int from,
                 @SuppressWarnings("unused") int to) {
        if (from < 31)
            onUpgradeTo31(context);
    }

    private void
    checkAppUpgrade(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), -1);
            int prevVer = prefs.getInt(PREF_KEY_APP_VERSION, pi.versionCode);
            if (pi.versionCode > prevVer) {
                onAppUpgrade(context, prevVer, pi.versionCode);
                SharedPreferences.Editor prefEd = prefs.edit();
                prefEd.putInt(PREF_KEY_APP_VERSION, pi.versionCode);
                prefEd.apply();
            }
        } catch (NameNotFoundException ignore) { }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    public static boolean
    hasEssentialPermissions() {
        // App requires few dangerous permissions.
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                AppEnv.getAppContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static String[]
    getEssentialPermissions() {
        return ESSENTIAL_PERMISSIONS;
    }

    public static void
    initApplicationPostEssentialPermissions() {
        try {
            ABaselib.initLibraryWithExternalStoragePermission(PolicyConstant.APPDATA_TMPDIR);
            //noinspection ResultOfMethodCallIgnored
            new File(PolicyConstant.APPDATA_LOGDIR).mkdirs();
            Util.initPostEssentialPermissions();
        } catch (IOException e) {
            //UxUtil.showTextToast(R.string.err_io_file);
            throw new AssertionError("Unexpected");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void
    onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        ABaselib.initLibrary(
                context,
                new Handler(),
                null);
        P = Logger.create(YTMPApp.class, Logger.LOGLV_DEFAULT);

        SearchSuggestionProvider.init();
        // Set preference to default values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        checkAppUpgrade(context);

        Util.init(context);
        TaskManager.get();
        // register default customized uncaught exception handler for error collecting.
        Thread.setDefaultUncaughtExceptionHandler(UnexpectedExceptionHandler.get());

        DB.get().open();
        RTState.get();
        NotiManager.get();
        YTPlayer.get();
        LockScreenActivity.ScreenMonitor.init();
        YTPlayerLifeSupportService.init();
    }

    @Override
    public void
    onLowMemory() {
        super.onLowMemory();
    }
}
