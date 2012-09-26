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

package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import free.yhc.youtube.musicplayer.R;

public class NotiManager {
    private static final String NOTI_INTENT_DELETE  = "ytmplayer.intent.action.NOTIFICATION_DELETE";
    private static final String NOTI_INTENT_ACTION  = "ytmplayer.intent.action.NOTIFICATION_ACTION";

    // Unique Identification Number for the Notification.
    // 'noti_base' doens't have any meaning except for random unique number.
    private static final int NOTI_ID     = R.drawable.noti_base;

    private static NotiManager sInstance = null;

    // active notification set.
    private final NotificationManager   nm = (NotificationManager)Utils.getAppContext()
                                                                       .getSystemService(Context.NOTIFICATION_SERVICE);

    public static enum NotiType {
        // NOTE
        // All uses same notification id because these notification SHOULD NOT be multiple-displayed.
        BASE    (R.drawable.noti_base),
        START   (R.drawable.noti_start),
        PAUSE   (R.drawable.noti_pause),
        STOP    (R.drawable.noti_stop),
        ALERT   (R.drawable.noti_alert);

        // true : for keep notification alive even if app. is killed.
        // false: notification should be removed when app. is killed.
        private final int           icon;

        static NotiType
        convert(String name) {
            for (NotiType n : NotiType.values()) {
                if (n.name().equals(name))
                    return n;
            }
            return null;
        }

        NotiType(int aIcon) {
            icon   = aIcon;
        }

        int getIcon() {
            return icon;
        }
    }


    public static class NotiIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            NotiManager nm = NotiManager.get();
            if (NOTI_INTENT_DELETE.equals(action))
                nm.removeNotification();
            else if (NOTI_INTENT_ACTION.equals(action)) {
                YTPlayer ytp = YTPlayer.get();
                String typeName = intent.getStringExtra("type");
                eAssert(null != typeName);
                NotiType nt = NotiType.convert(typeName);
                eAssert(null != nt);
                switch (nt) {
                case BASE:
                    // Ignore this action!!
                    break;

                case START:
                    ytp.startVideo();
                    break;

                case PAUSE:
                    ytp.pauseVideo();
                    break;

                case STOP:
                    ytp.stopVideos();
                    break;

                case ALERT:
                    // Just remove notification
                    nm.removeNotification();
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

    void
    putNotification(NotiType type, String videoTitle) {
        // To avoid unexpected race-condition.
        eAssert(Utils.isUiThread());
        // Set event time.
        Notification n = buildNotification(
                type,
                Utils.getAppContext().getResources().getText(R.string.app_name),
                videoTitle);
        n.when = System.currentTimeMillis();
        nm.notify(NOTI_ID, n);
    }

    void
    removeNotification() {
        // To avoid unexpected race-condition.
        eAssert(Utils.isUiThread());
        nm.cancel(NOTI_ID);
    }

    public static NotiManager
    get() {
        if (null == sInstance)
            sInstance = new NotiManager();
        return sInstance;
    }
}
