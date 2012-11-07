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

import static free.yhc.netmbuddy.model.Utils.eAssert;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.os.AsyncTask;
import android.os.Handler;

// [ Naming Convention ]
// Runnable => Job
// Thread   => Task
public class MultiThreadRunner {
    private static final String TAG = MultiThreadRunner.class.getName();

    private final Handler               mOwner;
    private final Object                mQLock      = new Object();
    private final LinkedList<Job<?>>    mReadyQ     = new LinkedList<Job<?>>();
    private final LinkedList<Task<?>>   mRunQ       = new LinkedList<Task<?>>();
    private final int                   mMaxConcur;
    private final AtomicBoolean         mCancelled  = new AtomicBoolean(false);
    private final AtomicFloat           mProgress   = new AtomicFloat(0);
    private final AtomicReference<OnProgressListener> mProgListener
        = new AtomicReference<OnProgressListener>(null);
    private final AtomicReference<OnDoneListener> mDoneListener
        = new AtomicReference<OnDoneListener>(null);

    // For debugging purpose.
    private int  mSeqN   = 0;

    public interface OnProgressListener {
        /**
         *
         * @param prog
         *   accumulated value of each Job's progress weight.
         */
        void onProgress(float prog);
    }

    public interface OnDoneListener {
        void onDone(MultiThreadRunner mtrunner, boolean cancelled);
    }

    public static abstract class Job<R> {
        private final boolean _mInterruptOnCancel;
        private final float _mProgWeight;
        private final int   _mTaskPriority = -1; // not used yet.

        private Handler _mOwner     = null;
        private OnProgressListener _mProgListener = null;

        public Job(boolean interruptOnCancel, float progWeight) {
            _mProgWeight = progWeight;
            _mInterruptOnCancel = true; // default is true.
        }

        public Job(float progWeight) {
            this(true, progWeight);
        }

        /**
         * @hide
         */
        final void
        setOwner(Handler owner) {
            // Setting only ONCE is allowed to avoid synch. issue.
            eAssert(null == _mOwner);
            _mOwner = owner;
        }

        /**
         * @hide
         */
        final void
        setProgListener(OnProgressListener listener) {
            // Setting only ONCE is allowed to avoid synch. issue.
            eAssert(null == _mProgListener);
            _mProgListener = listener;
        }

        /**
         * @hide
         */
        final boolean
        getInterruptOnCancel() {
            return _mInterruptOnCancel;
        }

        final int
        getTaskPriority() {
            return _mTaskPriority;
        }

        final float
        getProgWeight() {
            return _mProgWeight;
        }

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
        onPostRun(R result) { }

        public void
        onProgress(int prog) { }
    }

    private static class Task<R> extends AsyncTask<Void, Integer, R> {
        private final MultiThreadRunner _mMtrunner;
        private final Job<R>    _mJob;
        private final boolean   _mUiOwned; // pre-calculated value.

        private String          _mName  = "MultiThreadRunner.Task"; // For debugging

        private boolean
        isOwnerThread() {
            return _mMtrunner.getOwner().getLooper().getThread() == Thread.currentThread();
        }

        Task(MultiThreadRunner mtrunner, Job<R> job) {
            super();
            _mMtrunner = mtrunner;
            _mJob = job;
            _mUiOwned = Utils.isUiThread(_mMtrunner.getOwner().getLooper().getThread());
        }

        void
        setName(String name) {
            _mName = name;
        }

        String
        getName() {
            return _mName;
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
        onPreExecute() {
            if (_mUiOwned)
                _mJob.onCancelled();
            else
                _mMtrunner.getOwner().post(new Runnable() {
                    @Override
                    public void
                    run() {
                        _mJob.onCancelled();
                    }
                });
        }

        @Override
        protected void
        onCancelled() {
            if (_mUiOwned) {
                _mJob.onCancelled();
                _mMtrunner.onTaskDone(this, true);
            } else
                _mMtrunner.getOwner().post(new Runnable() {
                    @Override
                    public void
                    run() {
                        _mJob.onCancelled();
                        _mMtrunner.onTaskDone(Task.this, true);
                    }
                });
        }

        @Override
        protected void
        onPostExecute(final R r) {
            if (_mUiOwned) {
                _mJob.onPostRun(r);
                _mMtrunner.onTaskDone(this, false);
            } else
                _mMtrunner.getOwner().post(new Runnable() {
                    @Override
                    public void
                    run() {
                        _mJob.onPostRun(r);
                        _mMtrunner.onTaskDone(Task.this, false);
                    }
                });
        }

        @Override
        protected R
        doInBackground(Void... p) {
            return _mJob.doJob();
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
     * @return
     */
    private boolean
    isAllJobsDoneLocked() {
        return mReadyQ.isEmpty() && mRunQ.isEmpty();
    }

    /**
     * mQLock should be held.
     * @param job
     */
    private void
    runJobLocked(Job<?> job) {
        Task<?> t = new Task(this, job);
        t.setName("MultiThreadRunner-Task-" + mSeqN++);
        mRunQ.addLast(t);
        t.execute();
    }

    private void
    onTaskDone(final Task<?> task, final boolean cancelled) {
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
            else
                mReadyQ.addLast(job);
        }
    }

    public void
    clearCancelledState() {
        mCancelled.set(false);
    }

    public void
    setProgress(float v) {
        mProgress.set(v);
    }

    public void
    cancel() {
        mCancelled.set(true);
        synchronized (mQLock) {
            mReadyQ.clear();
            Iterator<Task<?>> iter = mRunQ.iterator();
            while (iter.hasNext())
                iter.next().cancel();
        }
    }

    public void
    waitAllDone() throws InterruptedException {
        synchronized (mQLock) {
            mQLock.wait();
        }
    }
}
