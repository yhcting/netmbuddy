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

public class SCmp {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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