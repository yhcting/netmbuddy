/******************************************************************************
 *    Copyright (C) 2012, 2013, 2014 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of NetMBuddy.
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
 *    along with this program.	If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy.scmp;

import free.yhc.netmbuddy.utils.Utils;

public class SCmp {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(SCmp.class);

    private SCElemI mBaseElem;

    public void
    setCmpParameter(String baseStr, boolean enableLenDiff, SCmpPolicy policy) {
        SCElemSStr es = SCElemSStr.create(SCElemSStr.Type.EXACT_WORD, baseStr, null);
        es.setEnableLengthDiff(enableLenDiff);
        if (null != policy)
            es.setPolicy(policy);
        mBaseElem = es;
    }

    public float
    similarity(String s) {
        SCElemSStr es = SCElemSStr.create(SCElemSStr.Type.EXACT_WORD, s, null);
        // Performance drop but better accuracy
        float s0 =  mBaseElem.similarity(es);
        float s1 = es.similarity(mBaseElem);
        return Utils.max(s0, s1);
    }

    public static float
    similarity(String s0, String s1, boolean enableLenDiff, SCmpPolicy policy) {
        SCElemSStr estr0 = SCElemSStr.create(SCElemSStr.Type.EXACT_WORD, s0, null);
        SCElemSStr estr1 = SCElemSStr.create(SCElemSStr.Type.EXACT_WORD, s1, null);
        if (null != policy) {
            estr0.setPolicy(policy);
            estr1.setPolicy(policy);
        }
        float sim0 = estr0.similarity(estr1);
        float sim1 = estr1.similarity(estr0);
        return Utils.max(sim0, sim1);
    }
}