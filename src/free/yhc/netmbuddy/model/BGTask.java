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

// NOTE
// Why this class is implemented even if AsyncTask is available at Android?
// AsyncTask has critical issue at Gingerbread and below.
// Issue is, canceling task by calling 'cancel(boolean xx)' stops background task
//   in the middle of running. That is, sometimes 'doInBackground()' function doens't return
//   when task is cancelled.
// Even worse, the moment of 'onCancelled()' is called, is NOT predictable.
// It may be called even if background task is still running (before terminating)
//
// All above issues are fixed at ICS.
// But, still GB is most-popular platform at Android world.
// That's the reason that new class is introduced.
//
// Additional advantage is, I can control it even at Java thread level :).
//
// [ WARNING ]
// -----------
// DO NOT make this CLASS DIRTY!
// That is, DO NOT ADD more FUNTIONALITIES.
// Main feature of this class is "calling back at mostly-demanded-moment (preRun, postRun, onCancelled etc)"
// If more complicated feature is required,
//   INHERITE THIS CLASS AND MAKE NEW CLASS FOR IT!
//   ----------------------------------------------
//

package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.os.Handler;
import free.yhc.netmbuddy.utils.Utils;

public abstract class BGTask<R> {
    public static final int PRIORITY_MIN    = Thread.MIN_PRIORITY;  // 1
    public static final int PRIORITY_MAX    = Thread.MAX_PRIORITY;  // 10
    public static final int PRIORITY_NORM   = Thread.NORM_PRIORITY; // 5
    public static final int PRIORITY_MIDLOW = 3;
    public static final int PRIORITY_MIDHIGH= 7;

    private static final String DEFAULT_THREAD_NAME = "BGTask";

    private final Thread            mThread;
    private final Handler           mOwner;
    private final AtomicBoolean     mCancelled = new AtomicBoolean(false);
    private final AtomicReference<State>    mState  = new AtomicReference<State>(State.READY);

    public enum State {
        // before background job is running
        READY,
        // background job is running
        RUNNING,
        // background job is done, but post processing - onCancelled() or onPostRun() - is not done yet.
        DONE,
        // background job and according post processing is completely finished.
        TERMINATED
    }

    private boolean
    isCurrentOwnerThread() {
        return Thread.currentThread() == mOwner.getLooper().getThread();
    }

    private void
    bgRun() {
        if (mCancelled.get())
            return;

        mState.set(State.RUNNING);
        R r = null;
        try {
            r = doAsyncTask();
        } finally {
            mState.set(State.DONE);

            if (mCancelled.get())
                mOwner.post(new Runnable() {
                    @Override
                    public void run() {
                        onCancelled();
                        mState.set(State.TERMINATED);
                    }
                });
            else {
                final R result = r;
                mOwner.post(new Runnable() {
                    @Override
                    public void run() {
                        onPostRun(result);
                        mState.set(State.TERMINATED);
                    }
                });
            }
        }
    }

    // ========================================================================
    // Action functions.
    // ========================================================================
    protected void
    onPreRun() { }

    protected void
    onPostRun(R result) { }

    protected void
    onCancel() { }

    protected void
    onCancelled() { }

    protected void
    onProgress(int prog) { }

    protected abstract R
    doAsyncTask();

    // ========================================================================
    //
    // ========================================================================
    /**
     *
     * @param name
     *   Thread name
     * @param owner
     *   Owner handler. All other action functions except for 'doAsyncTask' will be run on
     *     given owner handler's context. If null, UI context is used as default.
     * @param priority
     *   Java thread priority LOW[1, 10]High
     */
    public BGTask(String name,
                  Handler owner,
                  int priority) {
        mThread = new Thread(name) {
            @Override
            public void
            run() {
                bgRun();
            }
        };
        mThread.setPriority(priority);
        mOwner = owner;
    }

    public BGTask(String name, int priority) {
        this(name, Utils.getUiHandler(), priority);
    }

    public BGTask(int priority) {
        this(DEFAULT_THREAD_NAME, Utils.getUiHandler(), priority);
    }

    public BGTask(Handler owner) {
        this(DEFAULT_THREAD_NAME, owner, PRIORITY_MIDLOW);
    }

    public BGTask() {
        this(DEFAULT_THREAD_NAME, Utils.getUiHandler(), PRIORITY_MIDLOW);
    }

    public State
    getState() {
        return mState.get();
    }

    public final boolean
    isCancelled() {
        return mCancelled.get();
    }

    public final boolean
    isInterrupted() {
        return mThread.isInterrupted();
    }

    public void
    publishProgress(final int prog) {
        mOwner.post(new Runnable() {
            @Override
            public void run() {
                onProgress(prog);
            }
        });
    }

    public final boolean
    cancel(final boolean interrupt) {
        if (mCancelled.getAndSet(true)
            || State.READY == mState.get()
            || State.TERMINATED == mState.get())
            return false;

        mOwner.post(new Runnable() {
            @Override
            public void run() {
                onCancel();
                if (interrupt)
                    mThread.interrupt();
            }
        });
        return true;
    }

    public void
    run() {
        eAssert(State.READY == mState.get());
        mOwner.post(new Runnable() {
            @Override
            public void run() {
                onPreRun();
                mThread.start();
            }
        });
    }
}
