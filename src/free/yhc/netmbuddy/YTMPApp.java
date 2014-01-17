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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.model.NotiManager;
import free.yhc.netmbuddy.model.RTState;
import free.yhc.netmbuddy.model.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.model.YTPlayerLifeSupportService;
import free.yhc.netmbuddy.utils.Utils;

public class YTMPApp extends Application {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTMPApp.class);

    private static String PREF_KEY_APP_VERSION = "app_version";

    // ========================================================================
    //
    // App version upgrade handling
    //
    // ========================================================================
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
    onAppUpgrade(Context context, int from, int to) {
        if (from < 31)
            onUpgradeTo31(context);
    }

    private void
    checkAppUpgrade(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int prevVer = prefs.getInt(PREF_KEY_APP_VERSION, -1);
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), -1);
            if (pi.versionCode > prevVer) {
                onAppUpgrade(context, prevVer, pi.versionCode);
                SharedPreferences.Editor prefEd = prefs.edit();
                prefEd.putInt(PREF_KEY_APP_VERSION, pi.versionCode);
                prefEd.apply();
            }
        } catch (NameNotFoundException ignore) { }
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
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
        checkAppUpgrade(context);

        Utils.init(context);

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
