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

class SCElemChar implements SCElemI {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(SCElemChar.class);

    private final char mC;
    private final boolean mIc; // ignore case

    SCElemChar(char c, boolean ic) {
        super();
        mC = c;
        mIc = ic;
    }

    @Override
    public float
    similarity(SCElemI e) {
        if (!(e instanceof SCElemChar))
            throw new IllegalArgumentException();
        char c0 = mC;
        char c1 = ((SCElemChar)e).mC;
        if (mIc) {
            c0 = Character.toLowerCase(c0);
            c1 = Character.toLowerCase(c1);
        }
        return c0 == c1? 1.0f: 0.0f;
    }
}