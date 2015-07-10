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

package free.yhc.netmbuddy.core;

import java.util.concurrent.atomic.AtomicInteger;

import free.yhc.netmbuddy.utils.Utils;

public class AtomicFloat extends Number {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(AtomicFloat.class);

    private AtomicInteger mBits;

    @SuppressWarnings("unused")
    public AtomicFloat() {
        this(0f);
    }

    public AtomicFloat(float initialValue) {
        mBits = new AtomicInteger(Float.floatToIntBits(initialValue));
    }

    @SuppressWarnings("unused")
    public final boolean
    compareAndSet(float expect, float update) {
        return mBits.compareAndSet(Float.floatToIntBits(expect),
                                   Float.floatToIntBits(update));
    }

    public final void
    set(float newValue) {
        mBits.set(Float.floatToIntBits(newValue));
    }

    public final float
    get() {
        return Float.intBitsToFloat(mBits.get());
    }

    @SuppressWarnings("unused")
    public final float
    getAndSet(float newValue) {
        return Float.intBitsToFloat(mBits.getAndSet(Float.floatToIntBits(newValue)));
    }

    @SuppressWarnings("unused")
    public final boolean
    weakCompareAndSet(float expect, float update) {
        return mBits.weakCompareAndSet(Float.floatToIntBits(expect),
                                       Float.floatToIntBits(update));
    }

    @Override
    public final float
    floatValue() {
        return get();
    }

    @Override
    public final double
    doubleValue() {
        return floatValue();
    }

    @Override
    public final int
    intValue() {
        return (int)get();
    }

    @Override
    public final long
    longValue() {
        return (long)get();
    }

}