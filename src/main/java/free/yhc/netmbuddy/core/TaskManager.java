package free.yhc.netmbuddy.core;

import android.os.Handler;
import android.support.annotation.NonNull;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.adapter.android.AHandlerAdapter;

public class TaskManager extends free.yhc.baselib.async.TaskManager {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(TaskManager.class, Logger.LOGLV_DEFAULT);

    private static final int AVAILABLE_PROCESSOR = Runtime.getRuntime().availableProcessors();

    private static TaskManager sInstance = null;

    public static class TaskTag {}

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    protected TaskManager(
            @NonNull Handler owner,
            int maxJob,
            int maxWatched,
            TaskWatchFilter watchFilter) {
        super(new AHandlerAdapter(owner), maxJob, maxWatched, watchFilter);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static TaskManager
    get() {
        if (null == sInstance)
            sInstance = new TaskManager(
                    AppEnv.getUiHandler(),
                    2 < AVAILABLE_PROCESSOR? AVAILABLE_PROCESSOR: 2,
                    0,
                    null);
        return sInstance;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
}
