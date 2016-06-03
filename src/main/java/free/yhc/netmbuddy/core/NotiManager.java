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

package free.yhc.netmbuddy.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.abaselib.util.AUtil;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.YTMPActivity;
import free.yhc.netmbuddy.utils.Util;

public class NotiManager {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(NotiManager.class, Logger.LOGLV_DEFAULT);

    private static final String NOTI_INTENT_DELETE
        = "ytmplayer.intent.action.NOTIFICATION_DELETE";
    private static final String NOTI_INTENT_ACTION
        = "ytmplayer.intent.action.NOTIFICATION_ACTION";
    private static final String NOTI_INTENT_STOP_PLAYER
        = "ytmplayer.intent.action.NOTIFICATION_STOP_PLAYER";

    private static NotiManager sInstance = null;

    // active notification set.
    private final NotificationManager mNm
        = (NotificationManager)AppEnv.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
    private Notification mLastPlayerNotification = null;

    // NOTE
    public enum NotiType {
        // All player type notification uses same notification id because
        //   these notification SHOULD NOT be multiple-displayed.
        BASE  (R.drawable.noti_base,  getPlayerNotificationId()),
        START (R.drawable.noti_start, getPlayerNotificationId()),
        PAUSE (R.drawable.noti_pause, getPlayerNotificationId()),
        STOP  (R.drawable.noti_stop,  getPlayerNotificationId()),
        ALERT (R.drawable.noti_alert, getPlayerNotificationId()),

        // General notification - not used yet.
        // Why?
        // In case of GMail, ImportShareActivity seems to be started with NEW_TASK.
        // See below steps.
        //   - 1. After starting importing, ImportShareActivity is running foreground.
        //   - 2. Backing to launcher by touching 'Home' button.
        //   - 3a. Touch GMail icon again to go to GMail.
        //       => ImportShareActivity is not shown anymore!
        //   - 3b. Touch NetMBuddy to see ImportShareActivity
        //       => ImportShareActivity is not shown any more.
        // As described above, there is no way to back to ImportShareActivity
        //   after backing to launcher by touching 'Home' key.
        //
        // To workaround this, notification may be considered.
        // But, it doesn't help too.
        // Resuming app. or starting ImportShareActivity with singleTop
        //   always tries to start new Activity instance.
        // I didn't find any solution for this issue.
        // So, by any reasonable solution is found, this notification is left as 'UNUSED'.
        IMPORT  (R.drawable.noti_import,R.drawable.noti_import);

        private final int _mIcon;
        private final int _mId;

        NotiType(int icon, int id) {
            _mIcon = icon;
            _mId = id;
        }

        int getId() {
            return _mId;
        }

        int getIcon() {
            return _mIcon;
        }
    }

    public static class NotiIntentReceiver extends BroadcastReceiver {
        @Override
        public void
        onReceive(Context context, final Intent intent) {
            AppEnv.getUiHandler().post(new Runnable() {
                @Override
                public void
                run() {
                    handleNotificationIntent(intent);
                }
            });
        }
    }

    private NotiManager() {
    }

    private static void
    handleNotificationIntent(Intent intent) {
        String action = intent.getAction();
        NotiManager nm = NotiManager.get();
        YTPlayer ytp = YTPlayer.get();
        if (NOTI_INTENT_DELETE.equals(action))
            nm.removePlayerNotification();
        else if (NOTI_INTENT_STOP_PLAYER.equals(action))
            ytp.stopVideos();
        else if (NOTI_INTENT_ACTION.equals(action)) {
            String typeName = intent.getStringExtra("type");
            if (DBG) P.v("Intent action type : " + typeName);
            P.bug(null != typeName);
            NotiType nt = NotiType.valueOf(typeName);
            P.bug(null != nt);
            assert null != nt;
            switch (nt) {
            case BASE:
                // Ignore this action!!
                break;

            case START:
                ytp.playerStart();
                break;

            case PAUSE:
                ytp.playerPause();
                break;

            case STOP:
                ytp.stopVideos();
                break;

            case ALERT:
                // Just remove notification
                nm.removePlayerNotification();
                break;

            case IMPORT:
                Util.resumeApp();
                break;

            default:
                P.bug(false);
            }
        }
    }

    Notification
    buildNotification(NotiType ntype, CharSequence videoTitle) {
        RemoteViews rv = new RemoteViews(AppEnv.getAppContext().getPackageName(),
                                         R.layout.player_notification);
        NotificationCompat.Builder nbldr = new NotificationCompat.Builder(AppEnv.getAppContext());

        rv.setTextViewText(R.id.title, videoTitle);

        Intent intent = new Intent(AppEnv.getAppContext(), NotiManager.NotiIntentReceiver.class);
        intent.setAction(NOTI_INTENT_ACTION);
        intent.putExtra("type", ntype.name());
        PendingIntent pi = PendingIntent.getBroadcast(AppEnv.getAppContext(), 0, intent,
                                                      PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setImageViewResource(R.id.action, ntype.getIcon());
        rv.setOnClickPendingIntent(R.id.action, pi);

        intent = new Intent(AppEnv.getAppContext(), NotiManager.NotiIntentReceiver.class);
        intent.setAction(NOTI_INTENT_STOP_PLAYER);
        pi = PendingIntent.getBroadcast(AppEnv.getAppContext(), 0, intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setImageViewResource(R.id.stop, R.drawable.ic_media_stop);
        rv.setOnClickPendingIntent(R.id.stop, pi);

        intent = new Intent(AppEnv.getAppContext(), NotiManager.NotiIntentReceiver.class);
        intent.setAction(NOTI_INTENT_DELETE);
        PendingIntent piDelete = PendingIntent.getBroadcast(AppEnv.getAppContext(), 0, intent,
                                                            PendingIntent.FLAG_UPDATE_CURRENT);

        intent = new Intent(AppEnv.getAppContext(), YTMPActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent piContent = PendingIntent.getActivity(AppEnv.getAppContext(), 0, intent,
                                                            PendingIntent.FLAG_UPDATE_CURRENT);

        nbldr.setContent(rv)
             .setContentIntent(piContent)
             .setDeleteIntent(piDelete)
             .setWhen(System.currentTimeMillis())
             .setAutoCancel(true)
             .setSmallIcon(ntype.getIcon())
             .setTicker(null);
        return nbldr.build();
    }

    public static NotiManager
    get() {
        if (null == sInstance)
            sInstance = new NotiManager();
        return sInstance;
    }

    public static int
    getPlayerNotificationId() {
        return R.drawable.noti_base;
    }

    public void
    removeNotification(NotiType type) {
        // To avoid unexpected race-condition.
        P.bug(AUtil.isUiThread());
        // Player notification shares same notification id.
        mNm.cancel(type.getId());
    }

    public void
    removePlayerNotification() {
        removeNotification(NotiType.BASE);
        mLastPlayerNotification = null;
    }

    public void
    putNotification(NotiType type, String videoTitle) {
        // To avoid unexpected race-condition.
        P.bug(AUtil.isUiThread());
        // Set event time.
        Notification n = buildNotification(type, videoTitle);
        n.when = System.currentTimeMillis();
        mNm.notify(type.getId(), n);
        switch (type) {
        case BASE:
        case START:
        case PAUSE:
        case STOP:
        case ALERT:
            // In case of player notification
            mLastPlayerNotification = n;
        default:
            // ignored for other cases.
        }
    }

    public void
    putPlayerNotification(NotiType type, String videoTitle) {
        putNotification(type, videoTitle);
    }

    public Notification
    getLastPlayerNotification() {
        return mLastPlayerNotification;
    }
}
