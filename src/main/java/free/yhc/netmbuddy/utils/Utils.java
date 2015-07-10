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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Window;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.YTMPActivity;
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.core.RTState;

public class Utils {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Logger(Utils.class);

    //private static final boolean DBG    = false;
    private static final boolean LOGF = false;
    @SuppressWarnings("unused")
    private static final String TAG = "[NetMBuddy]";

    // Value SHOULD match xml preference value for title tts.
    private static final int TITLE_TTS_HEAD = 0x02; // tts at the beginning
    private static final int TITLE_TTS_TAIL = 0x01; // tts at the end

    // This is only for debugging.
    private static boolean sInitialized = false;

    // For debugging
    private static PrintWriter sLogWriter = null;
    private static DateFormat sLogTimeDateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                           DateFormat.MEDIUM,
                                           Locale.ENGLISH);

    // Even if these two variables are not 'final', those should be handled like 'final'
    //   because those are set only at init() function, and SHOULD NOT be changed.
    private static Context sAppContext = null;
    private static Handler sUiHandler = null;

    private static SharedPreferences sPrefs = null;
    private static TimeElemComparator sTimeElemComparator = new TimeElemComparator();

    private static final String[] sDateFormats = new String[] {
            // To support W3CDTF
        "yyyy-MM-d'T'HH:mm:ss.SSSZ",
        "yyyy-MM-d'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-d'T'HH:mm:ssZ",
        "yyyy-MM-d'T'HH:mm:ss'Z'",
    };

    public enum PrefLevel {
        LOW,
        NORMAL,
        HIGH
    }

    public enum PrefQuality {
        LOW     (R.string.low),
        MIDLOW  (R.string.midlow),
        NORMAL  (R.string.normal),
        HIGH    (R.string.high),
        VERYHIGH(R.string.veryhigh);

        private int text;

        PrefQuality(int aText) {
            text = aText;
        }

        public int
        getText() {
            return text;
        }

        @Nullable
        public static PrefQuality
        getMatchingQuality(int text) {
            for (PrefQuality q : PrefQuality.values()) {
                if (q.getText() == text)
                    return q;
            }
            return null;
        }
    }

    public enum PrefTitleSimilarityThreshold {
        VERYLOW (Policy.SIMILARITY_THRESHOLD_VERYLOW),
        LOW     (Policy.SIMILARITY_THRESHOLD_LOW),
        NORMAL  (Policy.SIMILARITY_THRESHOLD_NORMAL),
        HIGH    (Policy.SIMILARITY_THRESHOLD_HIGH),
        VERYHIGH(Policy.SIMILARITY_THRESHOLD_VERYHIGH);

        private float v;

        PrefTitleSimilarityThreshold(float aV) {
            v = aV;
        }

        public float
        getValue() {
            return v;
        }
    }

    private static class TimeElem {
        public Object v;
        public long time;

        public
        TimeElem(Object aV, long aTime) {
            v = aV;
            time = aTime;
        }
    }

    private static class TimeElemComparator implements Comparator<TimeElem> {
        @Override
        public int
        compare(TimeElem a0, TimeElem a1) {
            if (a0.time < a1.time)
                return -1;
            else if (a0.time > a1.time)
                return 1;
            else
                return 0;
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

        //noinspection ResultOfMethodCallIgnored
        new File(Policy.APPDATA_DIR).mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(Policy.APPDATA_VIDDIR).mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(Policy.APPDATA_LOGDIR).mkdirs();

        // Clear/Create cache directory!
        File cacheF = new File(Policy.APPDATA_CACHEDIR);
        FileUtils.removeFileRecursive(cacheF, cacheF);
        //noinspection ResultOfMethodCallIgnored
        cacheF.mkdirs();

        // Clear/Make temp directory!
        File tempF = new File(Policy.APPDATA_TMPDIR);
        FileUtils.removeFileRecursive(tempF, tempF);
        //noinspection ResultOfMethodCallIgnored
        tempF.mkdirs();

        if (LOGF) {
            //noinspection ResultOfMethodCallIgnored
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
    private enum LogLV{
        V ("[V]", 6),
        D ("[D]", 5),
        I ("[I]", 4),
        W ("[W]", 3),
        E ("[E]", 2),
        F ("[F]", 1);

        private String pref; // prefix string
        private int pri;  // priority
        LogLV(String aPref, int aPri) {
            pref = aPref;
            pri = aPri;
        }

        @SuppressWarnings("unused")
        String pref() {
            return pref;
        }

        @SuppressWarnings("unused")
        int pri() {
            return pri;
        }
    }

    public static class Logger {
        private final Class<?> _mCls;
        public Logger(Class<?> cls) {
            _mCls = cls;
        }
        // For logging
        public void v(String msg) { log(_mCls, LogLV.V, msg); }
        @SuppressWarnings("unused")
        public void d(String msg) { log(_mCls, LogLV.D, msg); }
        public void i(String msg) { log(_mCls, LogLV.I, msg); }
        public void w(String msg) { log(_mCls, LogLV.W, msg); }
        public void e(String msg) { log(_mCls, LogLV.E, msg); }
        @SuppressWarnings("unused")
        public void f(String msg) { log(_mCls, LogLV.F, msg); }
    }

    private static void
    log(Class<?> cls, LogLV lv, String msg) {
        if (null == msg)
            return;

        StackTraceElement ste = Thread.currentThread().getStackTrace()[5];
        msg = ste.getClassName() + "/" + ste.getMethodName() + "(" + ste.getLineNumber() + ") : " + msg;

        //noinspection PointlessBooleanExpression
        if (!LOGF) {
            switch(lv) {
            case V: Log.v(cls.getSimpleName(), msg); break;
            case D: Log.d(cls.getSimpleName(), msg); break;
            case I: Log.i(cls.getSimpleName(), msg); break;
            case W: Log.w(cls.getSimpleName(), msg); break;
            case E: Log.e(cls.getSimpleName(), msg); break;
            case F: Log.wtf(cls.getSimpleName(), msg); break;
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

    public static Context
    getAppContext() {
        return sAppContext;
    }

    public static Resources
    getResources() {
        return getAppContext().getResources();
    }

    public static String
    getResString(int id) {
        return getResources().getString(id);
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
     */
    public static boolean
    isValidValue(CharSequence v) {
        return !(null == v || v.length() <= 0);
    }

    /**
     * Is any available active network at this device?
     */
    public static boolean
    isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni;

        if (isPrefUseWifiOnly())
            ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        else
            ni = cm.getActiveNetworkInfo();

        return null != ni
               && ni.isConnectedOrConnecting();
    }

    public static String
    getCurrentTopActivity() {
        ActivityManager am = (ActivityManager)getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        ActivityManager.RunningTaskInfo ar = tasks.get(0);
        return ar.topActivity.getClassName();
    }

    public static boolean
    isAppForeground() {
        return getCurrentTopActivity().startsWith(getAppContext().getPackageName() + ".");
    }

    public static void
    resumeApp() {
        Intent intent = new Intent(Utils.getAppContext(), YTMPActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Utils.getAppContext().startActivity(intent);
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

    @SuppressWarnings("unused")
    public static Date
    parseDateString(String dateString) {
        dateString = removeLeadingTrailingWhiteSpace(dateString);
        Date date = null;
        try {
            date = DateUtils.parseDate(dateString, sDateFormats);
        } catch (DateParseException ignored) { }
        return date;
    }

    // ------------------------------------------------------
    // To handle generic array
    // ------------------------------------------------------
    @SuppressWarnings("unused")
    public static <T> T[]
    toArray(List<T> list, T[] a) {
        if (a.length < list.size())
            //noinspection unchecked
            a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), list.size());
        return list.toArray(a);
    }

    @SuppressWarnings("unused")
    public static <T> T[]
    toArray(List<T> list, Class<T> k) {
        //noinspection unchecked
        return list.toArray((T[])java.lang.reflect.Array.newInstance(k, list.size()));
    }

    public static <T> T[]
    newArray(Class<T> k, int size) {
        //noinspection unchecked
        return (T[])java.lang.reflect.Array.newInstance(k, size);
    }

    // ------------------------------------------------------------------------
    //
    // Accessing preference
    //
    // ------------------------------------------------------------------------
    private static String
    getStringPreference(String key, String defvalue) {
        String value = (String)RTState.get().getOverridingPreference(key);
        if (null == value)
            value = sPrefs.getString(key, defvalue);
        return value;
    }

    @SuppressWarnings("unused")
    private static int
    getIntPreference(String key, int defvalue) {
        Integer value = (Integer)RTState.get().getOverridingPreference(key);
        if (null == value)
            value = sPrefs.getInt(key, defvalue);
        return value;
    }

    private static boolean
    getBooleanPreference(String key, boolean defvalue) {
        Boolean value = (Boolean)RTState.get().getOverridingPreference(key);
        if (null == value)
            value = sPrefs.getBoolean(key, defvalue);
        return value;
    }

    public static SharedPreferences
    getSharedPreference() {
        return sPrefs;
    }

    public static boolean
    isPrefSuffle() {
        return getBooleanPreference(getResString(R.string.csshuffle), false);
    }

    public static boolean
    isPrefRepeat() {
        return getBooleanPreference(getResString(R.string.csrepeat), false);
    }

    @NonNull
    public static PrefQuality
    getPrefQuality() {
        String v = getStringPreference(getResString(R.string.csquality),
                                       getResString(R.string.csNORMAL));
        for (PrefQuality q : PrefQuality.values()) {
            if (q.name().equals(v))
                return q;
        }
        eAssert(false);
        return null;
    }

    public static float
    getPrefTitleSimilarityThreshold() {
        String v = getStringPreference(getResString(R.string.cstitle_similarity_threshold),
                                       getResString(R.string.csNORMAL));
        for (PrefTitleSimilarityThreshold q : PrefTitleSimilarityThreshold.values()) {
            if (q.name().equals(v))
                return q.getValue();
        }
        eAssert(false);
        return Policy.SIMILARITY_THRESHOLD_NORMAL;
    }

    @SuppressWarnings("unused")
    public static PrefLevel
    getPrefMemConsumption() {
        // See preference.xml for meaning of each number value.
        String lv = sPrefs.getString(getResString(R.string.csmem_consumption),
                getResString(R.string.csNORMAL));
        if (getResString(R.string.csLOW).equals(lv))
            return PrefLevel.LOW;
        else if (getResString(R.string.csNORMAL).equals(lv))
            return PrefLevel.NORMAL;
        else if (getResString(R.string.csHIGH).equals(lv))
            return PrefLevel.HIGH;
        else {
            eAssert(false);
            return PrefLevel.NORMAL;
        }
    }

    public static boolean
    isPrefLockScreen() {
        return getBooleanPreference(getResString(R.string.cslockscreen), false);
    }

    public static boolean
    isPrefStopOnBack() {
        return getBooleanPreference(getResString(R.string.csstop_on_back), true);
    }

    public static boolean
    isPrefErrReport() {
        return getBooleanPreference(getResString(R.string.cserr_report), true);
    }

    private static boolean
    isPrefUseWifiOnly() {
        return getBooleanPreference(getResString(R.string.csuse_wifi_only), true);
    }

    public static int
    getPrefTtsValue() {
        int value = 0;
        try {
            value = Integer.parseInt(sPrefs.getString(getResString(R.string.cstitle_tts), "0"));
        } catch (NumberFormatException ignored) { }
        return value;
    }

    public static boolean
    isPrefTtsEnabled() {
        return 0 != getPrefTtsValue();
    }

    public static boolean
    isPrefHeadTts() {
        return (0 != (TITLE_TTS_HEAD & getPrefTtsValue()));
    }

    public static boolean
    isPrefTailTts() {
        return (0 != (TITLE_TTS_TAIL & getPrefTtsValue()));
    }
    // ------------------------------------------------------------------------
    //
    // Min Max
    //
    // ------------------------------------------------------------------------
    public static float
    max(float f0, float f1) {
        return (f0 < f1)? f1: f0;
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

    @SuppressWarnings("unused")
    public static long
    bitSet(long flag, long value, long mask) {
        flag = bitClear(flag, mask);
        return flag | (value & mask);
    }

    public static boolean
    bitCompare(long flag, long value, long mask) {
        return value == (flag & mask);
    }

    @SuppressWarnings("unused")
    public static boolean
    bitIsSet(long flag, long mask) {
        return bitCompare(flag, mask, mask);
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public static boolean
    bitIsClear(int flag, int mask) {
        return bitCompare(flag, 0, mask);
    }

    // ------------------------------------------------------------------------
    //
    // Misc
    //
    // ------------------------------------------------------------------------
    @SuppressWarnings("unused")
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

    public static String
    millisToHourMinText(long millies) {
        int s = (int)(millies / 1000);
        int m = s / 60;
        int h = m / 60;
        m -= h * 60;
        return String.format("%02d:%02d", h, m);
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
        for (K key : map.keySet()) {
            if (map.get(key).equals(value))
                return key;
        }
        return null;
    }

    public static Object[]
    getSortedKeyOfTimeMap (HashMap<?, Long> timeMap) {
        TimeElem[] te = new TimeElem[timeMap.size()];
        Object[] vs = timeMap.keySet().toArray();
        for (int i = 0; i < vs.length; i++)
            te[i] = new TimeElem(vs[i], timeMap.get(vs[i]));
        Arrays.sort(te, sTimeElemComparator);
        Object[] sorted = new Object[vs.length];
        for (int i = 0; i < sorted.length; i++)
            sorted[i] = te[i].v;

        return sorted;
    }

    // ------------------------------------------------------------------------
    //
    // Other android specifics.
    //
    // ------------------------------------------------------------------------
    @SuppressWarnings("unused")
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

    public static void
    sendMail(Context context,
             String receiver,
             String subject,
             String text,
             File attachment) {
        if (!Utils.isNetworkAvailable())
            return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        if (null != receiver)
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] { receiver });
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (null != attachment)
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(attachment));
        intent.setType("message/rfc822");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            UiUtils.showTextToast(context, R.string.msg_fail_find_app);
        }
    }

    /**
     * Only available when status bar is showing.
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

    @NonNull
    public static Rect
    getVisibleFrame(Activity activity) {
        Rect rect= new Rect();
        Window window= activity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        return rect;
    }
}
