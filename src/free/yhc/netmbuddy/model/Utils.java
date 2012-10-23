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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class Utils {
    private static final boolean DBG    = true;
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
        LOW,
        NORMAL,
        HIGH
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
        removeFileRecursive(cacheF, cacheF);
        cacheF.mkdirs();

        // Clear/Make temp directory!
        File tempF = new File(Policy.APPDATA_TMPDIR);
        removeFileRecursive(tempF, tempF);
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
        LogLV(String pref, int pri) {
            this.pref = pref;
            this.pri = pri;
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

    public static Handler
    getUiHandler() {
        return sUiHandler;
    }

    public static boolean
    isUiThread() {
        return Thread.currentThread() == sUiHandler.getLooper().getThread();
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
    // Image Handling
    //
    // ------------------------------------------------------------------------

    /**
     * Decode image from file path(String) or raw data (byte[]).
     * @param image
     *   Two types are supported.
     *   String for file path / byte[] for raw image data.
     * @param opt
     * @return
     */
    private static Bitmap
    decodeImageRaw(Object image, BitmapFactory.Options opt) {
        if (image instanceof String) {
            return BitmapFactory.decodeFile((String) image, opt);
        } else if (image instanceof byte[]) {
            byte[] data = (byte[]) image;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opt);
        }
        eAssert(false);
        return null;
    }


    /**
     * Get size(width, height) of given image.
     * @param image
     *   'image file path' or 'byte[]' image data
     * @param out
     *   out[0] : width of image / out[1] : height of image
     * @return
     *   false if image cannot be decode. true if success
     */
    public static boolean
    imageSize(Object image, int[] out) {
        eAssert(null != image);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        decodeImageRaw(image, opt);
        if (opt.outWidth <= 0 || opt.outHeight <= 0 || null == opt.outMimeType) {
            return false;
        }
        out[0] = opt.outWidth;
        out[1] = opt.outHeight;
        return true;
    }

    /**
     * Calculate rectangle(out[]). This is got by shrinking rectangle(width,height) to
     *   bound rectangle(boundW, boundH) with fixed ratio.
     * If input rectangle is included in bound, then input rectangle itself will be
     *   returned. (we don't need to adjust)
     * @param boundW
     *   width of bound rect
     * @param boundH
     *   height of bound rect
     * @param width
     *   width of rect to be shrunk
     * @param height
     *   height of rect to be shrunk
     * @param out
     *   calculated value [ out[0](width) out[1](height) ]
     * @return
     *   false(not shrunk) / true(shrunk)
     */
    public static boolean
    shrinkFixedRatio(int boundW, int boundH, int width, int height, int[] out) {
        boolean ret;
        // Check size of picture..
        float rw = (float) boundW / (float) width; // width ratio
        float rh = (float) boundH / (float) height; // height ratio

        // check whether shrinking is needed or not.
        if (rw >= 1.0f && rh >= 1.0f) {
            // we don't need to shrink
            out[0] = width;
            out[1] = height;
            ret = false;
        } else {
            // shrinking is essential.
            float ratio = (rw > rh) ? rh : rw; // choose minimum
            // integer-type-casting(rounding down) guarantees that value cannot
            // be greater than bound!!
            out[0] = (int) (ratio * width);
            out[1] = (int) (ratio * height);
            ret = true;
        }
        return ret;
    }

    /**
     * Calculate rectangle(out[]). This is got by fitting  rectangle(width,height) to
     *   bound rectangle(boundW, boundH) with fixed ratio - preserving width-height-ratio.
     * If input rectangle is included in bound, then input rectangle itself will be
     *   returned. (we don't need to adjust)
     * @param boundW
     *   width of bound rect
     * @param boundH
     *   height of bound rect
     * @param width
     *   width of rect to be shrunk
     * @param height
     *   height of rect to be shrunk
     * @param out
     *   calculated value [ out[0](width) out[1](height) ]
     * @return
     *   false(not shrunk) / true(shrunk)
     */
    public static void
    fitFixedRatio(int boundW, int boundH, int width, int height, int[] out) {
        boolean ret;
        // Check size of picture..
        float rw = (float) boundW / (float) width; // width ratio
        float rh = (float) boundH / (float) height; // height ratio

        float ratio = (rw > rh) ? rh : rw; // choose minimum
        // integer-type-casting(rounding down) guarantees that value cannot
        // be greater than bound!!
        out[0] = (int) (ratio * width);
        out[1] = (int) (ratio * height);
    }

    /**
     * Make fixed-ration-bounded-bitmap with file.
     * If (0 >= boundW || 0 >= boundH), original-size-bitmap is trying to be created.
     * @param image
     *   image file path (absolute path) or raw data (byte[])
     * @param boundW
     *   bound width
     * @param boundH
     *   bound height
     * @return
     *   null if fails
     */
    public static Bitmap
    decodeImage(Object image, int boundW, int boundH) {
        eAssert(null != image);

        BitmapFactory.Options opt = null;
        if (0 < boundW && 0 < boundH) {
            int[] imgsz = new int[2]; // image size : [0]=width / [1] = height
            if (false == imageSize(image, imgsz)) {
                // This is not proper image data
                return null;
            }

            int[] bsz = new int[2]; // adjusted bitmap size
            boolean bShrink = shrinkFixedRatio(boundW, boundH, imgsz[0], imgsz[1], bsz);

            opt = new BitmapFactory.Options();
            opt.inDither = false;
            if (bShrink) {
                // To save memory we need to control sampling rate. (based on
                // width!)
                // for performance reason, we use power of 2.
                if (0 >= bsz[0])
                    return null;

                int sampleSize = 1;
                while (1 < imgsz[0] / (bsz[0] * sampleSize))
                    sampleSize *= 2;

                // shrinking based on width ratio!!
                // NOTE : width-based-shrinking may make 1-pixel error in height
                // side!
                // (This is not Math!! And we are using integer!!! we cannot
                // make it exactly!!!)
                opt.inScaled = true;
                opt.inSampleSize = sampleSize;
                opt.inDensity = imgsz[0] / sampleSize;
                opt.inTargetDensity = bsz[0];
            }
        }
        return decodeImageRaw(image, opt);
    }

    /**
     * Compress give bitmap to JPEG formatted image data.
     * @param bm
     * @return
     */
    public static byte[]
    compressBitmap(Bitmap bm) {
        long time = System.currentTimeMillis();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        logI("TIME: Compress Image : " + (System.currentTimeMillis() - time));
        return baos.toByteArray();
    }



    // ------------------------------------------------------------------------
    //
    // Files
    //
    // ------------------------------------------------------------------------

    public static boolean unzip(String file, String outDir) {
        final int BUFSZ = 1024;
        try {
            File fSrc = new File(file);
            ZipFile zipFile = new ZipFile(fSrc);
            Enumeration<?> e = zipFile.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)e.nextElement();
                File destinationFilePath = new File(outDir, entry.getName());
                //create directories if required.
                destinationFilePath.getParentFile().mkdirs();

                //if the entry is directory, leave it. Otherwise extract it.
                if (entry.isDirectory())
                    continue;
                else {
                    BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                    int b;
                    byte buffer[] = new byte[BUFSZ];
                    FileOutputStream fos = new FileOutputStream(destinationFilePath);
                    BufferedOutputStream bos = new BufferedOutputStream(fos, BUFSZ);
                    while ((b = bis.read(buffer, 0, 1024)) != -1)
                        bos.write(buffer, 0, b);

                    bos.flush();
                    bos.close();
                    bis.close();
                }
            }
        } catch(IOException e) {
            return false;
        }
        return true;
    }

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

    public static boolean
    removeFileRecursive(File f, HashSet<String> skips) {
        boolean ret = true;
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                if (!removeFileRecursive(c, skips))
                    ret = false;
        }

        if (ret && !skips.contains(f.getAbsolutePath()))
            return f.delete();
        return ret;
    }

    public static boolean
    removeFileRecursive(File f, File[] skips) {
        HashSet<String> skipSets = new HashSet<String>();
        for (File skf : skips)
            skipSets.add(skf.getAbsolutePath());

        return removeFileRecursive(f, skipSets);
    }

    public static boolean
    removeFileRecursive(File f, File skip) {
        return removeFileRecursive(f, new File[] { skip });
    }

    public static boolean
    removeFileRecursive(File f) {
        return removeFileRecursive(f, new File[0]);
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
    // Strings
    //
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    //
    // Accessing preference
    //
    // ------------------------------------------------------------------------
    public static boolean
    isPrefSuffle() {
        return sPrefs.getString("shuffle", "off").equals("on");
    }

    public static boolean
    isPrefRepeat() {
        return sPrefs.getString("repeat", "off").equals("on");
    }

    public static PrefQuality
    getPrefQuality() {
        String qstr = sPrefs.getString("quality", PrefQuality.NORMAL.name());
        for (PrefQuality q : PrefQuality.values()) {
            if (q.name().equals(qstr))
                return q;
        }
        eAssert(false);
        return null;
    }
    // ------------------------------------------------------------------------
    //
    // Misc
    //
    // ------------------------------------------------------------------------
    // Bit mask handling
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
    bitIsSet(long flag, long value, long mask) {
        return value == (flag & mask);
    }

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
}
