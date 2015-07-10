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

package free.yhc.netmbuddy.core;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import free.yhc.netmbuddy.core.YTPlayer.StopState;
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
