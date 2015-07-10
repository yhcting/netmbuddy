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

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.HashMap;

import android.support.v4.util.LruCache;
import free.yhc.netmbuddy.utils.Utils;

public class RTState implements
UnexpectedExceptionHandler.Evidence {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(RTState.class);

    private static RTState sInstance = null;

    // TODO Proxy string should be changed if user changes proxy setting.
    private String mProxy = "";
    private HashMap<String, MapValue> mOverridingPref = new HashMap<>();
    private LruCache<String, YTHacker> mHackerCache = new LruCache<>(Policy.YTHACK_CACHE_SIZE);

    private static class MapValue {
        Object owner;
        Object value;
        MapValue(Object aOwner, Object aValue) {
            owner = aOwner;
            value = aValue;
        }
    }

    private RTState() {
        UnexpectedExceptionHandler.get().registerModule(this);
        mProxy = System.getenv("http_proxy");
        if (null == mProxy)
            mProxy = "";
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    public static RTState
    get() {
        if (null == sInstance)
            sInstance = new RTState();
        return sInstance;
    }

    @SuppressWarnings("unused")
    public String
    getProxy() {
        return mProxy;
    }

    /**
     *
     * @param hacker should be successfully hacked object.
     */
    public void
    cachingYtHacker(YTHacker hacker) {
        eAssert(hacker.hasHackedResult());
        mHackerCache.put(hacker.getYtvid(), hacker);
    }

    public YTHacker
    getCachedYtHacker(String ytvid) {
        return mHackerCache.get(ytvid);
    }

    @SuppressWarnings("unused")
    public void
    setOverridingPreference(String key, Object owner, Object value) {
        mOverridingPref.put(key, new MapValue(owner, value));
    }

    @SuppressWarnings("unused")
    public boolean
    unsetOverridingPreference(String key, Object owner) {
        MapValue v = mOverridingPref.get(key);
        if (null != v && owner != v.owner)
            return false;
        mOverridingPref.remove(key);
        return true;
    }

    public Object
    getOverridingPreference(String key) {
        MapValue v = mOverridingPref.get(key);
        return null == v? null: v.value;
    }

    /**
     *
     * @param key key string of preference
     * @return null if overriding value DOESN'T EXIST.
     */
    @SuppressWarnings("unused")
    public Object
    getOverridingPreferenceOwner(String key) {
        MapValue v = mOverridingPref.get(key);
        return null == v? null: v.owner;
    }
}
