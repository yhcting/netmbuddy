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

package free.yhc.netmbuddy.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.RTState;

public class Utils {
    private static final boolean DBG    = false;
    private static final boolean LOGF   = false;
    private static final String  TAG    = "[YoutubeMusicPlayer]";

    // This is only for debugging.
    private static boolean  sInitialized = false;

    // For debugging
    private static PrintWriter  sLogWriter  = null;
    private static DateFormat   sLogTimeDateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                           DateFormat.MEDIUM,
                                           Locale.ENGLISH);

    // Even if these two variables are not 'final', those should be handled like 'final'
    //   because those are set only at init() function, and SHOULD NOT be changed.
    private static Context  sAppContext  = null;
    private static Handler  sUiHandler   = null;

    private static SharedPreferences sPrefs = null;


    private static final String[] sDateFormats = new String[] {
            // To support W3CDTF
        "yyyy-MM-d'T'HH:mm:ss.SSSZ",
        "yyyy-MM-d'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-d'T'HH:mm:ssZ",
        "yyyy-MM-d'T'HH:mm:ss'Z'",
    };

    public static enum PrefQuality {
        LOW     (R.string.low),
        NORMAL  (R.string.normal),
        HIGH    (R.string.high);

        private int text;
        private
        PrefQuality(int aText) {
            text = aText;
        }

        public int
        getText() {
            return text;
        }

        public static PrefQuality
        getMatchingQuality(int text) {
            for (PrefQuality q : PrefQuality.values()) {
                if (q.getText() == text)
                    return q;
            }
            return null;
        }
    }

    // ========================================================================
    //
    // Initialization
    //
    // ========================================================================

    public static void
    init(Context aAppContext) {
        // This is called first for module initialization.
        // So, ANY DEPENDENCY to other module is NOT allowed
        eAssert(!sInitialized);
        if (!sInitialized)
            sInitialized = true;

        new File(Policy.APPDATA_DIR).mkdirs();
        new File(Policy.APPDATA_VIDDIR).mkdirs();

        // Clear/Create cache directory!
        File cacheF = new File(Policy.APPDATA_CACHEDIR);
        FileUtils.removeFileRecursive(cacheF, cacheF);
        cacheF.mkdirs();

        // Clear/Make temp directory!
        File tempF = new File(Policy.APPDATA_TMPDIR);
        FileUtils.removeFileRecursive(tempF, tempF);
        tempF.mkdirs();

        if (LOGF) {
            new File(Policy.APPDATA_LOGDIR).mkdirs();
            String dateText = DateFormat
                                .getDateTimeInstance(DateFormat.MEDIUM,
                                                     DateFormat.MEDIUM,
                                                     Locale.ENGLISH)
                                .format(new Date(System.currentTimeMillis()));
            dateText = dateText.replace(' ', '_');
            File logF = new File(Policy.APPDATA_LOGDIR + dateText + ".log");
            try {
                sLogWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logF)));
            } catch (FileNotFoundException e) {
                eAssert(false);
            }
        }

        sAppContext = aAppContext;
        sUiHandler = new Handler();
        sPrefs = PreferenceManager.getDefaultSharedPreferences(getAppContext());

    }

    // ========================================================================
    //
    // Fundamentals
    //
    // ========================================================================
    private static enum LogLV{
        V ("[V]", 6),
        D ("[D]", 5),
        I ("[I]", 4),
        W ("[W]", 3),
        E ("[E]", 2),
        F ("[F]", 1);

        private String pref; // prefix string
        private int    pri;  // priority
        LogLV(String aPref, int aPri) {
            pref = aPref;
            pri = aPri;
        }

        String pref() {
            return pref;
        }

        int pri() {
            return pri;
        }
    }

    private static void
    log(LogLV lv, String msg) {
        if (!DBG || null == msg)
            return;

        if (!LOGF) {
            switch(lv) {
            case V: Log.v(TAG, msg); break;
            case D: Log.d(TAG, msg); break;
            case I: Log.i(TAG, msg); break;
            case W: Log.w(TAG, msg); break;
            case E: Log.e(TAG, msg); break;
            case F: Log.wtf(TAG, msg); break;
            }
        } else {
            long timems = System.currentTimeMillis();
            sLogWriter.printf("<%s:%03d> [%s] %s\n",
                              sLogTimeDateFormat.format(new Date(timems)),
                              timems % 1000,
                              lv.name(),
                              msg);
            sLogWriter.flush();
        }
    }

    // Assert
    public static void
    eAssert(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    // For logging
    public static void logV(String msg) { log(LogLV.V, msg); }
    public static void logD(String msg) { log(LogLV.D, msg); }
    public static void logI(String msg) { log(LogLV.I, msg); }
    public static void logW(String msg) { log(LogLV.W, msg); }
    public static void logE(String msg) { log(LogLV.E, msg); }
    public static void logF(String msg) { log(LogLV.F, msg); }

    public static Context
    getAppContext() {
        return sAppContext;
    }

    public static Resources
    getResources() {
        return getAppContext().getResources();
    }

    public static String
    getResText(int id) {
        return (String)getResources().getText(id);
    }

    public static Handler
    getUiHandler() {
        return sUiHandler;
    }

    public static boolean
    isUiThread(Thread thread) {
        return thread == sUiHandler.getLooper().getThread();
    }

    public static boolean
    isUiThread() {
        return isUiThread(Thread.currentThread());
    }
    // ========================================================================
    //
    //
    //
    // ========================================================================

    /**
     * Is is valid string?
     * Valid means "Not NULL and Not empty".
     * @param v
     * @return
     */
    public static boolean
    isValidValue(CharSequence v) {
        return !(null == v || v.length() <= 0);
    }

    /**
     * Is any available active network at this device?
     * @return
     */
    public static boolean
    isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileNi = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if ((null != wifiNi && wifiNi.isConnected())
            || (null != mobileNi && mobileNi.isConnected()))
            return true;
        else
            return false;
    }

    public static String
    getCurrentTopActivity() {
        ActivityManager am = (ActivityManager)getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        ActivityManager.RunningTaskInfo ar = tasks.get(0);
        return ar.topActivity.getClassName().toString();
    }

    public static boolean
    isAppForeground() {
        return getCurrentTopActivity().startsWith(getAppContext().getPackageName()+".");
    }

    // ------------------------------------------------------------------------
    //
    //
    //
    // ------------------------------------------------------------------------
    public static long[]
    convertArrayLongTolong(Long[] L) {
        long[] l = new long[L.length];
        for (int i = 0; i < L.length; i++)
            l[i] = L[i];
        return l;
    }

    public static int[]
    convertArrayIntegerToint(Integer[] I) {
        int[] i = new int[I.length];
        for (int j = 0; j < I.length; j++)
            i[j] = I[j];
        return i;
    }

    // ------------------------------------------------------------------------
    //
    // Date
    //
    // ------------------------------------------------------------------------
    public static String
    removeLeadingTrailingWhiteSpace(String s) {
        s = s.replaceFirst("^\\s+", "");
        return s.replaceFirst("\\s+$", "");
    }

    public static Date
    parseDateString(String dateString) {
        dateString = removeLeadingTrailingWhiteSpace(dateString);
        Date date = null;
        try {
            date = DateUtils.parseDate(dateString, sDateFormats);
        } catch (DateParseException e) { }
        return date;
    }

    // ------------------------------------------------------
    // To handle generic array
    // ------------------------------------------------------
    public static <T> T[]
    toArray(List<T> list, T[] a) {
        if (a.length < list.size())
            a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), list.size());
        return list.toArray(a);
    }

    public static <T> T[]
    toArray(List<T> list, Class<T> k) {
        return list.toArray((T[])java.lang.reflect.Array.newInstance(k, list.size()));
    }

    public static <T> T[]
    newArray(Class<T> k, int size) {
        return (T[])java.lang.reflect.Array.newInstance(k, size);
    }

    // ------------------------------------------------------------------------
    //
    // Accessing preference
    //
    // ------------------------------------------------------------------------
    private static String
    getPreference(String key, String defvalue) {
        String value = RTState.get().getOverridingPreference(key);
        if (null == value)
            value =  sPrefs.getString(key, defvalue);
        return value;
    }

    public static boolean
    isPrefSuffle() {
        String v = getPreference(getResText(R.string.csshuffle),
                                 getResText(R.string.csoff));
        return v.equals(getResText(R.string.cson));
    }

    public static boolean
    isPrefRepeat() {
        String v = getPreference(getResText(R.string.csrepeat),
                                 getResText(R.string.csoff));
        return v.equals(getResText(R.string.cson));
    }

    public static PrefQuality
    getPrefQuality() {
        String v = getPreference(getResText(R.string.csquality),
                                 getResText(R.string.csNORMAL));
        for (PrefQuality q : PrefQuality.values()) {
            if (q.name().equals(v))
                return q;
        }
        eAssert(false);
        return null;
    }

    public static boolean
    isPrefLockScreen() {
        String v = getPreference(getResText(R.string.cslockscreen),
                                            getResText(R.string.csoff));
        return v.equals(getResText(R.string.cson));
    }

    // ------------------------------------------------------------------------
    //
    // Bit mask handling
    //
    // ------------------------------------------------------------------------
    public static long
    bitClear(long flag, long mask) {
        return flag & ~mask;
    }

    public static long
    bitSet(long flag, long value, long mask) {
        flag = bitClear(flag, mask);
        return flag | (value & mask);
    }

    public static boolean
    bitCompare(long flag, long value, long mask) {
        return value == (flag & mask);
    }

    public static boolean
    bitIsSet(long flag, long mask) {
        return bitCompare(flag, mask, mask);
    }

    public static boolean
    bitIsClear(long flag, long mask) {
        return bitCompare(flag, 0, mask);
    }

    public static int
    bitClear(int flag, int mask) {
        return flag & ~mask;
    }

    public static int
    bitSet(int flag, int value, int mask) {
        flag = bitClear(flag, mask);
        return flag | (value & mask);
    }

    public static boolean
    bitCompare(int flag, int value, int mask) {
        return value == (flag & mask);
    }

    public static boolean
    bitIsSet(int flag, int mask) {
        return bitCompare(flag, mask, mask);
    }

    public static boolean
    bitIsClear(int flag, int mask) {
        return bitCompare(flag, 0, mask);
    }

    // ------------------------------------------------------------------------
    //
    // Misc
    //
    // ------------------------------------------------------------------------
    public static String
    secsToTimeText(int secs) {
        int h = secs / 60 / 60;
        secs -= h * 60 * 60;
        int m = secs / 60;
        secs -= m * 60;
        return String.format("%02d:%02d:%02d", h, m, secs);
    }

    public static String
    secsToMinSecText(int secs) {
        int m = secs / 60;
        secs -= m * 60;
        return String.format("%02d:%02d", m, secs);
    }

    public static void
    copy(OutputStream os, InputStream is) throws IOException, InterruptedException {
        byte buf[]=new byte[1024 * 16];
        int len;
        while((len = is.read(buf)) > 0) {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            os.write(buf, 0, len);
        }
    }

    public static <K,V> K
    findKey(HashMap<K, V> map, V value) {
        Iterator<K> iter = map.keySet().iterator();
        while(iter.hasNext()) {
            K key = iter.next();
            if (map.get(key).equals(value))
                return key;
        }
        return null;
    }

    // ------------------------------------------------------------------------
    //
    // Other android specifics.
    //
    // ------------------------------------------------------------------------
    public static boolean
    copyAssetFile(String dest, String assetFile) {
        try {
            InputStream is = Utils.getAppContext().getAssets().open(assetFile);
            FileOutputStream os = new FileOutputStream(new File(dest));
            copy(os, is);
            is.close();
            os.close();
            return true;
        } catch (InterruptedException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Only available when status bar is showing.
     * @param activity
     * @return
     */
    public static int
    getStatusBarHeight(Activity activity) {
        Rect rect= new Rect();
        Window window= activity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        // Below is for future reference.
        // int StatusBarHeight = rect.top;
        // int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        // int TitleBarHeight= contentViewTop - StatusBarHeight;
        return rect.top;
    }

    public static Rect
    getVisibleFrame(Activity activity) {
        Rect rect= new Rect();
        Window window= activity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        return rect;
    }
}
