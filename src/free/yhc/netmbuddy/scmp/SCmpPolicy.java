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

public class SCmpPolicy {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(SCmpPolicy.class);

    static final float DEF_ORDER_COEFF  = 3.0f;
    static final float DEF_CONT_ADV     = 4.0f;
    // values whose similarity is smaller than "scmpThreshold", are ignored.
    // Larger value(close to 1.0f) leads to better performance.
    static final float DEF_SCMP_THRESHOLD   = 0.7f;

    private final float mOrderCoeff;
    private final float mContAdv;
    private final float mCmpThreshold;

    public SCmpPolicy() {
        mOrderCoeff = DEF_ORDER_COEFF;
        mContAdv = DEF_CONT_ADV;
        mCmpThreshold = DEF_SCMP_THRESHOLD;
    }

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