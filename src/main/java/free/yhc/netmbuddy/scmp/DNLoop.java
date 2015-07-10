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

import free.yhc.netmbuddy.utils.Utils;

/**
 * Package Private
 */
class DNLoop {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(DNLoop.class);

    private final Object[][] mLoopVals;
    private final IteratorListener mIterL;

    private int[] mLoopI;
    private Object mUser;

    interface IteratorListener {
        /**
         *
         * @param dnl DNLoop object
         * @param oVals iteration values
         * @return true : keep going.
         *         false: stop iteration.
         */
        boolean iter(DNLoop dnl, Object[] oVals);
        /**
         *
         * @param dnl DNLoop object
         * @param depth 0 : inner-most loop
         */
        void end(DNLoop dnl, int depth);
    }

    /**
     * index '0' is 'inner-most loop'.
     * That is, 'loopVals[0][]' is loop-value-array of inner-most loop.
     * [ Example ]
     *     loop <5>
     *         loop <4>
     *             loop <2> <= loopVals[0][2]
     */
    DNLoop(Object[][] loopVals,
           IteratorListener iterL) {
        mLoopVals = loopVals;
        mIterL = iterL;

        mLoopI = new int[mLoopVals.length];
    }

    Object
    getUser() {
        return mUser;
    }

    void
    setUser(Object user) {
        mUser = user;
    }

    void
    start(Object user) {
        mUser = user;
        // initialize loop index.
        for (int i = 0; i < mLoopI.length; i++)
            mLoopI[i] = 0;

        Object[] oVals = new Object[mLoopI.length];
        boolean bDone = false;
        while (!bDone) {
            bDone = true;
            for (int i = 0; i < mLoopI.length; i++) {
                int lc = mLoopVals[i].length; // lc : Loop Count
                // is end of this loop?
                if (mLoopI[i] >= lc) {
                    // end of this loop.
                    if (null != mIterL)
                        mIterL.end(this, i);
                    if (i < (mLoopI.length -1)) {
                        mLoopI[i] = 0;
                        ++mLoopI[i + 1];
                    }
                } else {
                    for (int j = 0; j < mLoopI.length; j++)
                        oVals[j] = mLoopVals[j][mLoopI[j]];

                    ++mLoopI[0];
                    bDone = null != mIterL && !mIterL.iter(this, oVals);
                    break; // exit from for-loop.
                }
            }
        }
    }

    @SuppressWarnings("unused")
    static void
    test() {
        String[][] oVals = new String[][] {
            new String[] {"a", "b"},
            new String[] {"A", "B", "C"},
            new String[] {"1", "2", "3", "4"},
        };

        IteratorListener iter = new IteratorListener() {
            @Override
            public boolean
            iter(DNLoop dnl, Object[] oVals) {
                String s = "";
                for (Object o : oVals)
                    s += o + " ";
                if (DBG) P.v("- " + s);
                return true;
            }
            @Override
            public void
            end(DNLoop dnl, int depth) {
                if (DBG) P.v("End : " + depth);
            }
        };

        DNLoop dnl = new DNLoop(oVals, iter);
        dnl.start(null);
    }
}
