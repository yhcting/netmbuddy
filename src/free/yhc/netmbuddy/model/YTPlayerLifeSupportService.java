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

package free.yhc.netmbuddy.model;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import free.yhc.netmbuddy.model.YTPlayer.StopState;
import free.yhc.netmbuddy.utils.Utils;

public class YTPlayerLifeSupportService extends Service implements
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTPlayerLifeSupportService.class);

    public static final String ACTION_START = "ytmplayer.intent.action.START_LIFE_SUPPORT";

    private static void
    start() {
        if (DBG) P.v("Enter");
        Intent i = new Intent(Utils.getAppContext(),
                              YTPlayerLifeSupportService.class);
        i.setAction(ACTION_START);
        Utils.getAppContext().startService(i);
    }

    private static void
    stop() {
        if (DBG) P.v("Enter");
        Intent i = new Intent(Utils.getAppContext(),
                              YTPlayerLifeSupportService.class);
        Utils.getAppContext().stopService(i);
    }

    public static void
    init() {
        final YTPlayer  mp = YTPlayer.get();
        // This callback will be never removed.
        mp.addVideosStateListener(new Object(), new YTPlayer.VideosStateListener() {
            @Override
            public void
            onStopped(StopState state) {
                YTPlayerLifeSupportService.stop();
            }

            @Override
            public void
            onStarted() {
                YTPlayerLifeSupportService.stop();
                YTPlayerLifeSupportService.start();
            }

            @Override
            public void
            onChanged() { }
        });
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    @Override
    public void
    onCreate() {
        if (DBG) P.v("Enter");
        super.onCreate();
        UnexpectedExceptionHandler.get().registerModule(this);
    }

    @Override
    public int
    onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.getAction().equals(ACTION_START)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        NotiManager nm = NotiManager.get();
        Notification noti = nm.getLastPlayerNotification();
        // I'm not sure that what usecase can lead to following state.
        // But, bug report says that "null == noti".
        // So, let's ignore this exceptional case until root-cause is revealed
        if (null != noti)
            startForeground(NotiManager.getPlayerNotificationId(), noti);
        return START_NOT_STICKY;
    }

    @Override
    public void
    onDestroy() {
        if (DBG) P.v("Enter");
        UnexpectedExceptionHandler.get().unregisterModule(this);
        super.onDestroy();
    }

    @Override
    public IBinder
    onBind(Intent intent) {
        return null;
    }
}
