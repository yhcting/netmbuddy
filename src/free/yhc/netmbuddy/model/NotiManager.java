/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
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

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.utils.Utils;

public class NotiManager {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(NotiManager.class);

    private static final String NOTI_INTENT_DELETE
        = "ytmplayer.intent.action.NOTIFICATION_DELETE";
    private static final String NOTI_INTENT_ACTION
        = "ytmplayer.intent.action.NOTIFICATION_ACTION";

    private static NotiManager sInstance = null;

    // active notification set.
    private final NotificationManager mNm = (NotificationManager)Utils.getAppContext()
                                                                      .getSystemService(Context.NOTIFICATION_SERVICE);
    private Notification mLastPlayerNotification = null;

    // NOTE
    public static enum NotiType {
        // All player type notification uses same notification id because
        //   these notification SHOULD NOT be multiple-displayed.
        BASE    (R.drawable.noti_base,  getPlayerNotificationId()),
        START   (R.drawable.noti_start, getPlayerNotificationId()),
        PAUSE   (R.drawable.noti_pause, getPlayerNotificationId()),
        STOP    (R.drawable.noti_stop,  getPlayerNotificationId()),
        ALERT   (R.drawable.noti_alert, getPlayerNotificationId()),

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

        private final int   _mIcon;
        private final int   _mId;

        NotiType(int icon, int id) {
            _mIcon   = icon;
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
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            NotiManager nm = NotiManager.get();
            if (NOTI_INTENT_DELETE.equals(action))
                nm.removePlayerNotification();
            else if (NOTI_INTENT_ACTION.equals(action)) {
                YTPlayer ytp = YTPlayer.get();
                String typeName = intent.getStringExtra("type");
                eAssert(null != typeName);
                NotiType nt = NotiType.valueOf(typeName);
                eAssert(null != nt);
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
                    Utils.resumeApp();
                    break;

                default:
                    eAssert(false);
                }
            }
        }
    }

    private NotiManager() {
    }

    Notification
    buildNotification(NotiType ntype, CharSequence title, CharSequence desc) {
        Intent intent = new Intent(Utils.getAppContext(), NotiManager.NotiIntentReceiver.class);
        intent.setAction(NOTI_INTENT_DELETE);
        PendingIntent piDelete = PendingIntent.getBroadcast(Utils.getAppContext(), 0, intent,
                                                            PendingIntent.FLAG_UPDATE_CURRENT);

        intent = new Intent(Utils.getAppContext(), NotiManager.NotiIntentReceiver.class);
        intent.setAction(NOTI_INTENT_ACTION);
        intent.putExtra("type", ntype.name());
        PendingIntent piContent = PendingIntent.getBroadcast(Utils.getAppContext(), 0, intent,
                                                             PendingIntent.FLAG_UPDATE_CURRENT);

        /*
         * NOTE
         * Below way is deprecated but works well better than using recommended way - notification builder.
         * (See commends below about using builder)
         */
        Notification n = new Notification(ntype.getIcon(), null, System.currentTimeMillis());
        n.setLatestEventInfo(Utils.getAppContext(), title, desc, piContent);
        n.deleteIntent = piDelete;
        n.flags = 0;

        return n;
        /* Below code generates "java.lang.NoClassDefFoundError : android.support.v4.app.NotificationCompat$Builder"
         * So, comment out!
         * (Damn Android!
         *
        NotificationCompat.Builder nbldr = new NotificationCompat.Builder(Utils.getAppContext());
        nbldr.setSmallIcon(ntype.getIcon())
             .setTicker(null)
             .setContentTitle(title)
             .setContentText(desc)
             .setAutoCancel(true)
             .setContentIntent(piContent)
             .setDeleteIntent(piDelete);
        return nbldr.build();
        */
    }

    public static NotiManager
    get() {
        if (null == sInstance)
            sInstance = new NotiManager();
        return sInstance;
    }

    public static final int
    getPlayerNotificationId() {
        return R.drawable.noti_base;
    }

    public void
    removeNotification(NotiType type) {
        // To avoid unexpected race-condition.
        eAssert(Utils.isUiThread());
        // Player notification shares same notification id.
        mNm.cancel(type.getId());
    }

    public void
    removePlayerNotification() {
        removeNotification(NotiType.BASE);
        mLastPlayerNotification = null;
    }

    public void
    putNotification(NotiType type, String title, String description) {
        // To avoid unexpected race-condition.
        eAssert(Utils.isUiThread());
        // Set event time.
        Notification n = buildNotification(type,
                                           title,
                                           description);
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
        }
    }

    public void
    putPlayerNotification(NotiType type, String videoTitle) {
        putNotification(type, Utils.getResText(R.string.app_name), videoTitle);
    }

    public Notification
    getLastPlayerNotification() {
        return mLastPlayerNotification;
    }
}
