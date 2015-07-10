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

package free.yhc.netmbuddy.core;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.os.Handler;
import free.yhc.netmbuddy.utils.Utils;

// [ Naming Convention ]
// Runnable => Job
// Thread   => Task
public class MultiThreadRunner {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(MultiThreadRunner.class);

    private final Handler mOwner;
    private final Object mQLock = new Object();
    private final LinkedList<Job<?>> mReadyQ = new LinkedList<>();
    private final LinkedList<Task<?>> mRunQ = new LinkedList<>();
    private final int mMaxConcur;
    private final AtomicBoolean mCancelled  = new AtomicBoolean(false);
    private final AtomicFloat mProgress = new AtomicFloat(0);
    private final AtomicReference<OnProgressListener> mProgListener
        = new AtomicReference<>(null);
    private final AtomicReference<OnDoneListener> mDoneListener
        = new AtomicReference<>(null);

    // For debugging purpose.
    @SuppressWarnings("unused")
    private int  mSeqN = 0;

    public interface OnProgressListener {
        /**
         * @param prog accumulated value of each Job's progress weight.
         */
        void onProgress(float prog);
    }

    public interface OnDoneListener {
        void onDone(MultiThreadRunner mtrunner, boolean cancelled);
    }

    public static abstract class Job<R> {
        private final boolean _mInterruptOnCancel;
        private final float _mProgWeight;
        private final int _mTaskPriority = -1; // not used yet.

        private Handler _mOwner = null;
        private OnProgressListener _mProgListener = null;

        public Job(@SuppressWarnings("unused") boolean interruptOnCancel,
                   float progWeight) {
            _mProgWeight = progWeight;
            _mInterruptOnCancel = true; // default is true.
        }

        public Job(float progWeight) {
            this(true, progWeight);
        }

        @SuppressWarnings("unused")
        public Job() {
            this(0);
        }

        final void
        setOwner(Handler owner) {
            // Setting only ONCE is allowed to avoid synch. issue.
            eAssert(null == _mOwner);
            _mOwner = owner;
        }

        final void
        setProgListener(OnProgressListener listener) {
            // Setting only ONCE is allowed to avoid synch. issue.
            eAssert(null == _mProgListener);
            _mProgListener = listener;
        }

        final boolean
        getInterruptOnCancel() {
            return _mInterruptOnCancel;
        }

        @SuppressWarnings("unused")
        final int
        getTaskPriority() {
            return _mTaskPriority;
        }

        final float
        getProgWeight() {
            return _mProgWeight;
        }

        @SuppressWarnings("unused")
        protected final void
        publishProgress(float prog) {
            // NOTE
            // _mProgListener and _mOwner can be set only once.
            // So, synch. issue can be ignored here.
            if (null != _mProgListener) {
                final float overallProg = prog * _mProgWeight;
                _mOwner.post(new Runnable() {
                    @Override
                    public void
                    run() {
                        _mProgListener.onProgress(overallProg);
                    }
                });
            }
        }

        public void
        onPreRun() { }

        abstract public R
        doJob();

        public void
        cancel() { }

        public void
        onCancelled() { }

        public void
        onPostRun(@SuppressWarnings("unused") R result) { }

        @SuppressWarnings("unused")
        public void
        onProgress(int prog) { }
    }

    private static class Task<R> extends BGTask<R> {
        private final MultiThreadRunner _mMtrunner;
        private final Job<R> _mJob;

        // NOTE
        // To workaround Android GB Framework bug regarding AsyncTask.
        // On GB Framework, it is NOT guaranteed that onCancelled() is called after returning from doInBackground().
        // This is based on experimental result on Moto Bionic.
        @SuppressWarnings("unused")
        private final boolean _mJobDone = false;

        private boolean
        isOwnerThread() {
            return _mMtrunner.getOwner().getLooper().getThread() == Thread.currentThread();
        }

        Task(MultiThreadRunner mtrunner,
             Job<R> job,
             Handler owner) {
            super(owner);
            _mMtrunner = mtrunner;
            _mJob = job;
        }

        Job<R>
        getJob() {
            return _mJob;
        }

        public void
        cancel() {
            eAssert(isOwnerThread());
            _mJob.cancel();
            super.cancel(_mJob.getInterruptOnCancel());
        }

        @Override
        protected void
        onPreRun() {
            eAssert(isOwnerThread());
            _mJob.onPreRun();
        }

        @Override
        protected void
        onCancelled() {
            eAssert(isOwnerThread());
            _mJob.onCancelled();
            _mMtrunner.onTaskDone(Task.this, true);
        }

        @Override
        protected void
        onPostRun(final R r) {
            eAssert(isOwnerThread());
            _mJob.onPostRun(r);
            _mMtrunner.onTaskDone(this, false);
        }

        @Override
        protected R
        doAsyncTask() {
            R r;
            r = _mJob.doJob();
            return r;
        }
    }

    private void
    mustRunOnOwnerThread() {
        eAssert(mOwner.getLooper().getThread() == Thread.currentThread());
    }

    private float
    updateProgress(float amountOfProgress) {
        float f = mProgress.get();
        mProgress.set(f + amountOfProgress);
        return mProgress.get();
    }

    private void
    publishProgress(float prog) {
        OnProgressListener listener = mProgListener.get();
        if (null != listener)
            listener.onProgress(prog);

    }

    private void
    publishDone(boolean cancelled) {
        OnDoneListener listener = mDoneListener.get();
        if (null != listener)
            listener.onDone(this, cancelled);
    }

    /**
     * mQLock should be held.
     */
    private boolean
    isAllJobsDoneLocked() {
        return mReadyQ.isEmpty() && mRunQ.isEmpty();
    }

    /**
     * mQLock should be held.
     */
    private void
    runJobLocked(Job<?> job) {
        // TODO : instantiate generic 'task'
        // Is there any to instantiate generic 'task' whose generic type is
        //   same with generic type of 'job' instead of raw-type?
        @SuppressWarnings("unchecked")
        Task<?> t = new Task(this, job, mOwner);
        mRunQ.addLast(t);
        t.run();
    }

    private void
    onTaskDone(final Task<?> task,
               @SuppressWarnings("unused") final boolean cancelled) {
        mustRunOnOwnerThread();
        //logD("Run TaskDone START : " + task.getName());

        mOwner.post(new Runnable() {
            @Override
            public void
            run() {
                if (!mCancelled.get())
                    publishProgress(updateProgress(task.getJob().getProgWeight()));
            }
        });

        synchronized (mQLock) {
            mRunQ.remove(task);
            eAssert(mRunQ.size() < mMaxConcur);

            if (!mReadyQ.isEmpty())
                runJobLocked(mReadyQ.removeFirst());

            if (isAllJobsDoneLocked()) {
                publishDone(mCancelled.get());
                mQLock.notifyAll();
            }
        }
        //logD("Run TaskDone END : " + task.getName());
    }

    public MultiThreadRunner(Handler ownerHandler,
                             int nrMaxConcurrent) {
        mOwner = ownerHandler;
        mMaxConcur = nrMaxConcurrent;
    }

    public Handler
    getOwner() {
        return mOwner;
    }

    @SuppressWarnings("unused")
    public void
    setOnDoneListener(OnDoneListener listener) {
        mDoneListener.set(listener);
    }

    public void
    setOnProgressListener(OnProgressListener listener) {
        mProgListener.set(listener);
    }

    public void
    appendJob(Job<?> job) {
        appendJob(job, false);
    }

    public void
    appendJob(Job<?> job, boolean toFirst) {
        job.setOwner(mOwner);
        job.setProgListener(new OnProgressListener() {
            @Override
            public void
            onProgress(float prog) {
                mustRunOnOwnerThread();
                // 'prog' value is calculated value based on Job's progressWeight.
                publishProgress(updateProgress(prog));
            }
        });

        synchronized (mQLock) {
            if (mRunQ.size() < mMaxConcur)
                runJobLocked(job);
            else {
                if (toFirst)
                    mReadyQ.addFirst(job);
                else
                    mReadyQ.addLast(job);
            }
        }
    }

    @SuppressWarnings("unused")
    public void
    clearCancelledState() {
        mCancelled.set(false);
    }

    @SuppressWarnings("unused")
    public void
    setProgress(float v) {
        mProgress.set(v);
    }

    public void
    cancel() {
        mCancelled.set(true);
        synchronized (mQLock) {
            mReadyQ.clear();
            for (Task<?> t : mRunQ)
                t.cancel();
        }
    }

    public void
    waitAllDone() throws InterruptedException {
        synchronized (mQLock) {
            if (!isAllJobsDoneLocked())
                mQLock.wait();
        }
    }
}
