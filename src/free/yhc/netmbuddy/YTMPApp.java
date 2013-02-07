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

package free.yhc.netmbuddy;

import android.app.Application;
import android.content.res.Configuration;
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

    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void
    onCreate() {
        super.onCreate();
        Utils.init(getApplicationContext());

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
