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

package free.yhc.netmbuddy.scmp;

import java.util.Iterator;
import java.util.LinkedList;

import free.yhc.netmbuddy.utils.Utils;

class SCElem implements SCElemI {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(SCElem.class);

    private static final SCmpPolicy sDefaultPolicy = new SCmpPolicy();

    private SCElemI[] mElems = null;
    private SCmpPolicy mPolicy = sDefaultPolicy;
    private float mMaxW = -1.0f; // maximum weight
    private boolean mEnableLengthDiff = false; // by default...

    private enum State {
        I, // idle
        F, // Found
        C, // Continuous
        N, // Non-continuous
    }

    // Similarity CoMPare RESult
    private static class ScanRes {
        int i;
        float s; // similarity
        ScanRes(int aI, float aS) {
            i = aI;
            s = aS;
        }
    }

    void
    setEnableLengthDiff(boolean enable) {
        mEnableLengthDiff = enable;
    }

    boolean
    getEnableLengthDiff() {
        return mEnableLengthDiff;
    }

    void
    setPolicy(SCmpPolicy policy) {
        mPolicy = policy;
    }

    SCmpPolicy
    getPolicy() {
        return mPolicy;
    }

    protected void
    setElements(SCElemI[] elems) {
        mElems = elems;
    }

    SCElemI[]
    getElements() {
        return mElems;
    }

    private float
    getMaximumSimilarity(SCmpPolicy policy, SCElemI[] es) {
        // NOTE
        // es.length > 0

        // equal => full-continuous-match
        float w = (es.length > 1)? 1.0f: 0.0f;
        for (int i = 0; i < es.length - 1; i++)
            w *= policy.getOrderCoeff();
        w *= policy.getContAdv();
        w += 1.0f; // weight for 'found' state.
        return w;
    }

    private float
    getLengthDiffDeduction(@SuppressWarnings("unused") SCmpPolicy policy,
                           SCElemI[] es0,
                           SCElemI[] es1) {
        if (!getEnableLengthDiff())
            return 1.0f; // by default ignore length-diff-factor.

        int min, max;
        if (es0.length < es1.length) {
            min = es0.length;
            max = es1.length;
        } else {
            min = es1.length;
            max = es0.length;
        }

        float ldff = (max - min) / min;
        if (ldff > 1.0f)
            ldff = 1.0f;
        // Manual
        if (0 == ldff)
            return 1.0f;
        else if (0.2 > ldff) // 0 ~ 20% : 90%
            return 0.9f;
        else if (0.4 > ldff) // 20% ~ 40%
            return 0.7f;
        else if (0.6 > ldff) // 40% ~ 60%
            return 0.4f;
        // if length-difference is more than 30%,
        //   these two are regarded as totally different.
        return 0.0f;
    }

    private ScanRes[][]
    scanSimiarElems(SCmpPolicy policy, SCElemI[] es0, SCElemI[] es1) {
        LinkedList<LinkedList<ScanRes>> lres = new LinkedList<>();
        for (SCElemI e0 : es0) {
            int i = 0;
            boolean bFirst = true;
            LinkedList<ScanRes> ll = null;
            for (SCElemI e1 : es1) {
                ++i;
                float similarity = e0.similarity(e1);
                if (similarity >= policy.getCmpThreshold()) {
                    if (bFirst) {
                        ll = new LinkedList<>();
                        lres.addLast(ll);
                        bFirst = false;
                    }
                    ll.addLast(new ScanRes(i, similarity));
                }
            }
        }

        // Converting result to two-dimensional array
        ScanRes[][] res = new ScanRes[lres.size()][];
        int i = 0;
        for (LinkedList<ScanRes> l : lres) {
            Iterator<ScanRes> itr2 = l.iterator();
            res[i] = new ScanRes[l.size()];
            int j = 0;
            while (itr2.hasNext())
                res[i][j++] = itr2.next();
            ++i;
        }

        return res;
    }

    private boolean
    dnlInterate(DNLoop dnl, Object[] oVals) {
        SCmpPolicy policy = (SCmpPolicy)dnl.getUser();
        int lasti = -1;
        State st = State.I;
        State prevst = State.I;
        float w = 0.0f; // weight of this iteration
        float sw = 0.0f; // session weight
        for (Object o : oVals) {
            ScanRes r = (ScanRes)o;
            if (State.I == st) {
                // starting of new weight calculation session
                st = State.F;
            } else if (r.i == (lasti + 1)) {
                st = State.C;
            } else if (r.i > lasti) {
                st = State.N;
            } else {
                st = State.F;
            }
            if (DBG) P.v("State Changed : " + prevst.name() + " -> " + st.name() + " (" + r.s + ")");
            if (prevst != st
                || State.F == prevst) {
                if (State.C == prevst)
                    sw *= policy.getContAdv();
                if (DBG) P.v("Session Weight " + sw + " is added");
                w += sw;
                sw = 1.0f;
            }

            if (State.F == st)
                sw = 1.0f;
            else if (State.N == st || State.C == st)
                sw *= policy.getOrderCoeff() * r.s;

            prevst = st;
            lasti = r.i;
        }

        if (State.I != st) {
            if (State.C == st)
                sw *= policy.getContAdv();

            if (DBG) P.v("Session Weight " + sw + " is added");
            w += sw;
        }

        if (DBG) P.v("Current Weight : " + w);
        if (DBG) P.v("-------------------------------------------");
        if (mMaxW < w)
            mMaxW = w;

        return true;
    }

    private void
    dnlEndLoop(@SuppressWarnings("unused") DNLoop dnl,
               int depth) {
        if (DBG) P.v("- End loop : " + depth);
    }

    @Override
    public float
    similarity(SCElemI ei) {
        if (!(ei instanceof SCElem))
            throw new IllegalArgumentException();
        SCElem e = (SCElem)ei;

        if (null == e.getElements()
            || null == getElements())
            throw new IllegalArgumentException();

        ScanRes[][] r = scanSimiarElems(getPolicy(), getElements(), e.getElements());
        mMaxW = -1.0f;
        float maxS = getMaximumSimilarity(getPolicy(), getElements());
        float lenDiffDeduction = getLengthDiffDeduction(getPolicy(), getElements(), e.getElements());
        new DNLoop(r,
                   new DNLoop.IteratorListener() {
                       @Override
                       public boolean
                       iter(DNLoop dnl, Object[] oVals) {
                           return SCElem.this.dnlInterate(dnl, oVals);
                       }
                       @Override
                       public void
                       end(DNLoop dnl, int depth) {
                           SCElem.this.dnlEndLoop(dnl, depth);
                       }
                   }).start(getPolicy());

        if (DBG) P.v("mMaxW : " + mMaxW
                     + " / maxS : " + maxS
                     + " / deduc : " + lenDiffDeduction
                     + " => " + (mMaxW / maxS * lenDiffDeduction));
        return mMaxW / maxS * lenDiffDeduction;
    }
}
