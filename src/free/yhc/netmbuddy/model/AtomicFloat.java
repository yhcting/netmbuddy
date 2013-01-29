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

package free.yhc.netmbuddy.model;

import java.util.concurrent.atomic.AtomicInteger;

import free.yhc.netmbuddy.utils.Utils;

public class AtomicFloat extends Number {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(AtomicFloat.class);

    private AtomicInteger mBits;

    public AtomicFloat() {
        this(0f);
    }

    public AtomicFloat(float initialValue) {
        mBits = new AtomicInteger(Float.floatToIntBits(initialValue));
    }

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

    public final float
    getAndSet(float newValue) {
        return Float.intBitsToFloat(mBits.getAndSet(Float.floatToIntBits(newValue)));
    }

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