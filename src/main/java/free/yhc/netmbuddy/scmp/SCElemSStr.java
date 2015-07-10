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

import java.util.LinkedList;

import free.yhc.netmbuddy.utils.Utils;

class SCElemSStr extends SCElem {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(SCElemSStr.class);

    private static final String DEF_DELIMITER_REGEX = "[\\Q ~().,-\\*&|^%$#@!`'\"?/><_+=[]{};:\\E]+";

    enum Type {
        NORMAL,
        EXACT_WORD,
    }

    static SCElemSStr
    create(Type ty, String str, String delimiterRegex) {
        if (null == delimiterRegex)
            delimiterRegex = DEF_DELIMITER_REGEX;

        String[] toks = str.split(delimiterRegex);
        LinkedList<String> l = new LinkedList<>();
        for (String s : toks) {
            if (!s.isEmpty())
                l.addLast(s);
        }
        return new SCElemSStr(l.toArray(new String[l.size()]), ty);
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