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

// STok : Similarity Token
//      : returns similarity.
class SCElemSTok extends SCElem {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(SCElemSTok.class);

    SCElemSTok(String tok) {
        super();
        SCElemChar[] ec = new SCElemChar[tok.length()];
        for (int i = 0; i < tok.length(); i++)
            ec[i] = new SCElemChar(tok.charAt(i), true);
        setElements(ec);
    }
}