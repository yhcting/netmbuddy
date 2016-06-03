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

package free.yhc.netmbuddy.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Window;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.net.NetConn;
import free.yhc.baselib.net.NetConnHttp;
import free.yhc.abaselib.util.AUtil;
import free.yhc.baselib.util.FileUtil;
import free.yhc.abaselib.util.UxUtil;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.YTMPActivity;
import free.yhc.netmbuddy.core.PolicyConstant;
import free.yhc.netmbuddy.core.RTState;

public class Util extends free.yhc.baselib.util.Util {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(free.yhc.netmbuddy.utils.Util.class, Logger.LOGLV_DEFAULT);

    @SuppressWarnings("unused")
    private static final String TAG = "[NetMBuddy]";

    // Value SHOULD match xml preference value for title tts.
    private static final int TITLE_TTS_HEAD = 0x02; // tts at the beginning
    private static final int TITLE_TTS_TAIL = 0x01; // tts at the end

    // This is only for debugging.
    private static boolean sInitialized = false;

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
        VERYLOW (PolicyConstant.SIMILARITY_THRESHOLD_VERYLOW),
        LOW     (PolicyConstant.SIMILARITY_THRESHOLD_LOW),
        NORMAL  (PolicyConstant.SIMILARITY_THRESHOLD_NORMAL),
        HIGH    (PolicyConstant.SIMILARITY_THRESHOLD_HIGH),
        VERYHIGH(PolicyConstant.SIMILARITY_THRESHOLD_VERYHIGH);

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
        P.bug(!sInitialized);
        sInitialized = true;
        // This is called first for module initialization.
        // So, ANY DEPENDENCY to other module is NOT allowed
        sPrefs = PreferenceManager.getDefaultSharedPreferences(AppEnv.getAppContext());
    }

    public static void
    initPostEssentialPermissions() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        new File(PolicyConstant.APPDATA_DIR).mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(PolicyConstant.APPDATA_VIDDIR).mkdirs();

        // Clear/Create cache directory!
        File cacheF = new File(PolicyConstant.APPDATA_CACHEDIR);
        FileUtil.removeFileRecursive(cacheF);
        //noinspection ResultOfMethodCallIgnored
        cacheF.mkdirs();
    }

    // ========================================================================
    //
    // Fundamentals
    //
    // ========================================================================
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

    public static String
    getCurrentTopActivity() {
        ActivityManager am = (ActivityManager)AppEnv.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        ActivityManager.RunningTaskInfo ar = tasks.get(0);
        return ar.topActivity.getClassName();
    }

    public static boolean
    isAppForeground() {
        return getCurrentTopActivity().startsWith(AppEnv.getAppContext().getPackageName() + ".");
    }

    public static void
    resumeApp() {
        Intent intent = new Intent(AppEnv.getAppContext(), YTMPActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        AppEnv.getAppContext().startActivity(intent);
    }
    // ------------------------------------------------------------------------
    //
    //
    //
    // ------------------------------------------------------------------------
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
        return getBooleanPreference(AUtil.getResString(R.string.csshuffle), false);
    }

    public static boolean
    isPrefRepeat() {
        return getBooleanPreference(AUtil.getResString(R.string.csrepeat), false);
    }

    @NonNull
    public static PrefQuality
    getPrefQuality() {
        String v = getStringPreference(AUtil.getResString(R.string.csquality),
                                       AUtil.getResString(R.string.csNORMAL));
        for (PrefQuality q : PrefQuality.values()) {
            if (q.name().equals(v))
                return q;
        }
        P.bug(false);
        return PrefQuality.LOW;
    }

    public static float
    getPrefTitleSimilarityThreshold() {
        String v = getStringPreference(AUtil.getResString(R.string.cstitle_similarity_threshold),
                                       AUtil.getResString(R.string.csNORMAL));
        for (PrefTitleSimilarityThreshold q : PrefTitleSimilarityThreshold.values()) {
            if (q.name().equals(v))
                return q.getValue();
        }
        P.bug(false);
        return PolicyConstant.SIMILARITY_THRESHOLD_NORMAL;
    }

    @SuppressWarnings("unused")
    public static PrefLevel
    getPrefMemConsumption() {
        // See preference.xml for meaning of each number value.
        String lv = sPrefs.getString(AUtil.getResString(R.string.csmem_consumption),
                                     AUtil.getResString(R.string.csNORMAL));
        if (AUtil.getResString(R.string.csLOW).equals(lv))
            return PrefLevel.LOW;
        else if (AUtil.getResString(R.string.csNORMAL).equals(lv))
            return PrefLevel.NORMAL;
        else if (AUtil.getResString(R.string.csHIGH).equals(lv))
            return PrefLevel.HIGH;
        else {
            P.bug(false);
            return PrefLevel.NORMAL;
        }
    }

    public static boolean
    isPrefLockScreen() {
        return getBooleanPreference(AUtil.getResString(R.string.cslockscreen), false);
    }

    public static boolean
    isPrefStopOnBack() {
        return getBooleanPreference(AUtil.getResString(R.string.csstop_on_back), true);
    }

    public static boolean
    isPrefErrReport() {
        return getBooleanPreference(AUtil.getResString(R.string.cserr_report), true);
    }

    private static boolean
    isPrefUseWifiOnly() {
        return getBooleanPreference(AUtil.getResString(R.string.csuse_wifi_only), false);
    }

    public static int
    getPrefTtsValue() {
        int value = 0;
        try {
            value = Integer.parseInt(sPrefs.getString(AUtil.getResString(R.string.cstitle_tts), "0"));
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
    //
    //
    // ------------------------------------------------------------------------
    public static float
    max(float f0, float f1) {
        return (f0 < f1)? f1: f0;
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
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, secs);
    }

    public static String
    secsToMinSecText(int secs) {
        int m = secs / 60;
        secs -= m * 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, secs);
    }

    public static String
    millisToHourMinText(long millies) {
        int s = (int)(millies / 1000);
        int m = s / 60;
        int h = m / 60;
        m -= h * 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
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
    public static boolean
    isNetworkAvailable() {
        return NetConn.isNetConnected(isPrefUseWifiOnly()? NetConn.TYPE_WIFI: NetConn.TYPE_ANY);
    }

    public static NetConnHttp
    createNetConnHttp(URL url, String uastring) throws IOException {
        NetConnHttp.Builder bldr = NetConnHttp.Builder.newBuilder(url);
        if (isPrefUseWifiOnly())
            bldr.setNetType(NetConn.TYPE_WIFI);
        if (null != uastring)
            bldr.setUastring(uastring);
        return bldr.create();
    }

    public static NetConn
    createNetConn(URL url) throws IOException {
        NetConn.Builder bldr = NetConn.Builder.newBuilder(url);
        if (isPrefUseWifiOnly())
            bldr.setNetType(NetConn.TYPE_WIFI);
        return bldr.create();
    }

    @SuppressWarnings("unused")
    public static boolean
    copyAssetFile(String dest, String assetFile) {
        try {
            InputStream is = AppEnv.getAppContext().getAssets().open(assetFile);
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
        if (!free.yhc.netmbuddy.utils.Util.isNetworkAvailable())
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
            UxUtil.showTextToast(R.string.msg_fail_find_app);
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
