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

public class SCmpPolicy {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(SCmpPolicy.class);

    static final float DEF_ORDER_COEFF = 3.0f;
    static final float DEF_CONT_ADV = 4.0f;
    // values whose similarity is smaller than "scmpThreshold", are ignored.
    // Larger value(close to 1.0f) leads to better performance.
    static final float DEF_SCMP_THRESHOLD = 0.7f;

    private final float mOrderCoeff;
    private final float mContAdv;
    private final float mCmpThreshold;

    public SCmpPolicy() {
        mOrderCoeff = DEF_ORDER_COEFF;
        mContAdv = DEF_CONT_ADV;
        mCmpThreshold = DEF_SCMP_THRESHOLD;
    }

    @SuppressWarnings("unused")
    public SCmpPolicy(float orderCoeff, float contAdv, float scmpThreshold) {
        mOrderCoeff = orderCoeff;
        mContAdv = contAdv;
        mCmpThreshold = scmpThreshold;
    }

    public float
    getOrderCoeff() {
        return mOrderCoeff;
    }

    public float
    getContAdv() {
        return mContAdv;
    }

    public float
    getCmpThreshold() {
        return mCmpThreshold;
    }
}