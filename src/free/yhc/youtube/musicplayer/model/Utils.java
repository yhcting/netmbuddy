package free.yhc.youtube.musicplayer.model;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class Utils {
    private static final boolean DBG = true;
    private static final String  TAG = "[YoutubeMusicPlayer]";

    // This is only for debugging.
    private static boolean  sInitialized = false;

    // Even if these two varaibles are not 'final', those should be handled like 'final'
    //   because those are set only at init() function, and SHOULD NOT be changed.
    private static Context  sAppContext  = null;
    private static Handler  sUiHandler   = null;

    public static void
    init(Context aAppContext) {
        // This is called first for module initialization.
        // So, ANY DEPENDENCY to other module is NOT allowed
        eAssert(!sInitialized);
        if (!sInitialized)
            sInitialized = true;

        sAppContext = aAppContext;
        sUiHandler = new Handler();
    }

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

        Log.w(TAG, msg);
        /*
        switch(lv) {
        case V: Log.v(TAG, msg); break;
        case D: Log.d(TAG, msg); break;
        case I: Log.i(TAG, msg); break;
        case W: Log.w(TAG, msg); break;
        case E: Log.e(TAG, msg); break;
        case F: Log.wtf(TAG, msg); break;
        }
        */
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

}
