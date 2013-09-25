/*****************************************************************************
 *    Copyright (C) 2012, 2013 Younghyung Cho. <yhcting77@gmail.com>
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

import java.util.concurrent.atomic.AtomicReference;

import android.os.Handler;
import free.yhc.netmbuddy.utils.Utils;

public abstract class BGTask<R> {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(BGTask.class);

    public static final int PRIORITY_MIN    = Thread.MIN_PRIORITY;  // 1
    public static final int PRIORITY_MAX    = Thread.MAX_PRIORITY;  // 10
    public static final int PRIORITY_NORM   = Thread.NORM_PRIORITY; // 5
    public static final int PRIORITY_MIDLOW = 3;
    public static final int PRIORITY_MIDHIGH= 7;

    private static final String DEFAULT_THREAD_NAME = "BGTask";

    private final Thread            mThread;
    private final Handler           mOwner;
    private final Object            mStateLock  = new Object();
    // NOTE
    // Why this special variable 'mUserCancel' is required?
    // Most of interface functions work asynchronously to keep order of State-event.
    // (ex. NEW->START->CANCELING->CANCELLED etc)
    // But, just after 'cancel' is called, this Task should be regarded as 'cancelled'
    //   even if canceling is still ongoing.(User already cancel the task!)
    // To resolve this issue, mUserCancel is introduced.
    private final AtomicReference<Boolean> mUserCancel = new AtomicReference<Boolean>(false);
    private State   mState  = State.READY;

    public enum State {
        // before background job is running
        READY,
        // background job is started but not running yet.
        STARTED,
        // background job is running
        RUNNING,
        // job is canceling
        CANCELING,
        // background job is done, but post processing - onPostRun() - is not done yet.
        DONE,
        // background job is done, but post processing - onCancelled() - is not done yet.
        CANCELLED,
        // background job and according post processing is completely finished.
        TERMINATED,
        TERMINATED_CANCELLED


    }

    private void
    setStateLocked(State st) {
        if (DBG) P.v("State Set to : " + mState.name());
        mState = st;
    }

    private State
    getStateLocked() {
        return mState;
    }

    private void
    postOnCancelled() {
        if (DBG) P.v("Enter");
        mOwner.post(new Runnable() {
            @Override
            public void
            run() {
                onCancelled();
                synchronized (mStateLock) {
                    setStateLocked(State.TERMINATED_CANCELLED);
                }
            }
        });
    }

    private void
    postOnPostRun(final R r) {
        if (DBG) P.v("Enter");
        mOwner.post(new Runnable() {
            @Override
            public void
            run() {
                onPostRun(r);
                synchronized (mStateLock) {
                    setStateLocked(State.TERMINATED);
                }
            }
        });
    }

    private void
    bgRun() {
        R r = null;
        try {
            synchronized (mStateLock) {
                switch (getStateLocked()) {
                case CANCELING:
                    return;

                case STARTED:
                    // Normal case
                    setStateLocked(State.RUNNING);
                    break;

                default:
                    if (DBG) P.w("Invalid state (" + getStateLocked().name() + ")");
                    return; // nothing to do
                }
            }

            r = doAsyncTask();
        } finally {
            synchronized (mStateLock) {
                switch (getStateLocked()) {
                case CANCELING:
                    setStateLocked(State.CANCELLED);
                    postOnCancelled();
                    break;

                case RUNNING:
                    setStateLocked(State.DONE);
                    postOnPostRun(r);
                    break;

                default:
                    ;// unexpected... just ignore it...
                }
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

    public final String
    getName() {
        return mThread.getName();
    }

    public final void
    setName(String name) {
        mThread.setName(name);
    }

    public final State
    getState() {
        synchronized (mStateLock) {
            return getStateLocked();
        }
    }

    public final Handler
    getOwner() {
        return mOwner;
    }

    public final boolean
    isOwnerThread(Thread thread) {
        return thread == mOwner.getLooper().getThread();
    }

    /**
     * Is task cancelled by user? (by calling 'cancel()')
     * This function returns 'true' even if task is under CANCELING state.
     * @return
     */
    public final boolean
    isCancelled() {
        return mUserCancel.get();
    }

    public final boolean
    isInterrupted() {
        return mThread.isInterrupted();
    }

    public final void
    publishProgress(final int prog) {
        mOwner.post(new Runnable() {
            @Override
            public void
            run() {
                onProgress(prog);
            }
        });
    }

    /**
     * @param interrupt
     * @return
     *   'false' if task is already cancelled - cancel() is called more than once!
     */
    public final boolean
    cancel(final boolean interrupt) {
        if (mUserCancel.getAndSet(true))
            return false;

        mOwner.post(new Runnable() {
            @Override
            public void
            run() {
                synchronized(mStateLock) {
                    switch (getStateLocked()) {
                    case STARTED:
                    case RUNNING:
                        setStateLocked(State.CANCELING);
                        if (DBG) P.v("before onCancel()");
                        // NOTE
                        // onCancel() SHOULD be HERE!
                        // The reason is that state of "BG job" should be in 'CANCELLING' while onCancel is called.
                        // Putting onCancel() outside of this critical section, breaks above rule because
                        //   background thread may change state into CANCELLED.
                        //
                        // Issue is, onCancel() may take lots of time.
                        // But, this blocks only background thread.
                        // This is NOT critical to user experience.
                        onCancel();
                        if (interrupt)
                            mThread.interrupt();
                        break;

                    default:
                        // ignored
                    }
                }
            }
        });
        return true;
    }

    public final void
    run() {
        mOwner.post(new Runnable() {
            @Override
            public void
            run() {
                boolean canRun = false;
                synchronized (mStateLock) {
                    if (State.READY == getStateLocked()) {
                        setStateLocked(State.STARTED);
                        canRun = true;
                    }
                }

                if (canRun) {
                    onPreRun();
                    mThread.start();
                }
            }
        });
    }
}
