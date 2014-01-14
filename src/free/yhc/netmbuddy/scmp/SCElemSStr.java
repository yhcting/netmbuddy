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

import java.util.LinkedList;

import free.yhc.netmbuddy.utils.Utils;

class SCElemSStr extends SCElem {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(SCElemSStr.class);

    private static final String DEF_DELIMITER_REGEX
        = "(\\s|~|\\(|\\)|\\.|\\,|\\-|\\\\|\\*|\\&|\\||\\^|\\%|\\$|\\#|\\@|\\!|\\`|\\'|\\\"|\\?|\\/|\\>|\\<|\\_|\\+|\\=|\\[|\\]|\\{|\\})+";

    enum Type {
        NORMAL,
        EXACT_WORD,
    };

    static SCElemSStr
    create(Type ty, String str, String delimiterRegex) {
        if (null == delimiterRegex)
            delimiterRegex = DEF_DELIMITER_REGEX;

        String[] toks = str.split(delimiterRegex);
        LinkedList<String> l = new LinkedList<String>();
        for (String s : toks) {
            if (!s.isEmpty())
                l.addLast(s);
        }
        return new SCElemSStr(l.toArray(new String[0]), ty);
    }

    private SCElemSStr(String[] toks, Type ty) {
        super();
        SCElemI[] es = new SCElemI[toks.length];
        for (int i = 0; i < toks.length; i++) {
            switch (ty) {
            case NORMAL:
                es[i] = new SCElemSTok(toks[i]);
                break;

            case EXACT_WORD:
                es[i] = new SCElemTok(toks[i], true);
                break;
            }
        }
        setElements(es);
    }
}