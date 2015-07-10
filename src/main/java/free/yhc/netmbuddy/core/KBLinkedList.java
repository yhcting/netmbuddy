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

package free.yhc.netmbuddy.core;

import java.util.Iterator;
import java.util.LinkedList;

import free.yhc.netmbuddy.utils.Utils;

public class KBLinkedList<T> {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(KBLinkedList.class);

    private LinkedList<Elem> mL = new LinkedList<>();

    private static class Elem {
        final Object key;
        final Object item;
        Elem(Object aKey, Object aItem) {
            key = aKey;
            item = aItem;
        }
    }

    private class Iter implements Iterator<T> {
        Iterator<Elem> itr = mL.iterator();

        @Override
        public boolean
        hasNext() {
            return itr.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T
        next() {
            return (T)itr.next().item;
        }

        @Override
        public void
        remove() {
            itr.remove();
        }
    }

    public KBLinkedList() {
    }

    public boolean
    add(Object key, T item) {
        return mL.add(new Elem(key, item));
    }

    @SuppressWarnings("unused")
    public void
    addFirst(Object key, T item) {
        mL.addFirst(new Elem(key, item));
    }

    @SuppressWarnings("unused")
    public void
    addLast(Object key, T item) {
        mL.addLast(new Elem(key, item));
    }

    public void
    remove(Object key) {
        Iterator<Elem> itr = mL.iterator();
        while (itr.hasNext()) {
            Elem e = itr.next();
            if (e.key == key)
                itr.remove();
        }
    }

    public boolean
    remove(Object key, T item) {
        Iterator<Elem> itr = mL.iterator();
        while (itr.hasNext()) {
            Elem e = itr.next();
            if (e.key == key && e.item == item) {
                itr.remove();
                return true;
            }
        }
        return false;
    }

    public void
    clear() {
        mL.clear();
    }

    public Iterator<T>
    iterator() {
        return new Iter();
    }

    @SuppressWarnings("unused")
    public T[]
    toArray(T[] a) {
        // NOT TESTED enough yet!
        @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
        Elem[] es = mL.toArray(new Elem[0]);
        if (a.length < es.length)
            //noinspection unchecked
            a = (T[])Utils.newArray(a.getClass().getComponentType(), es.length);
        for (int i = 0; i < es.length; i++)
            //noinspection unchecked
            a[i] = (T)es[i].item;
        return a;
    }
}
