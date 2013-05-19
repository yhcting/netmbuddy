/*****************************************************************************
 *    Copyright (C) 2013 Younghyung Cho. <yhcting77@gmail.com>
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

package free.yhc.netmbuddy.scmp;

import free.yhc.netmbuddy.utils.Utils;

/**
 * Package Private
 */
class DNLoop {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(DNLoop.class);

    private final Object[][]    mLoopVals;
    private final IteratorListener  mIterL;

    private int[]   mLoopI;
    private Object  mUser;

    interface IteratorListener {
        /**
         *
         * @param dnl
         * @param oVals
         *   iteration values
         * @return
         *     true : keep going.
         *     false: stop iteration.
         */
        boolean iter(DNLoop dnl, Object[] oVals);
        /**
         *
         * @param dnl
         * @param depth
         *   0 : inner-most loop
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
     * @param loopVals
     * @param iterL
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
                    bDone = false;
                    if (null != mIterL
                        && !mIterL.iter(this, oVals))
                        bDone = true;

                    break; // exit from for-loop.
                }
            }
        }
    }

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
                    s += (String)o + " ";
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
