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

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.HashMap;

import android.support.v4.util.LruCache;
import free.yhc.netmbuddy.utils.Utils;

public class RTState implements
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(RTState.class);

    private static RTState sInstance = null;

    // TODO
    // Proxy string should be changed if user changes proxy setting.
    private String  mProxy          = "";
    private HashMap<String, MapValue>   mOverridingPref = new HashMap<String, MapValue>();
    private LruCache<String, YTHacker>  mHackerCache    = new LruCache(Policy.YTHACK_CACHE_SIZE);

    private static class MapValue {
        Object  owner;
        Object  value;
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

    public String
    getProxy() {
        return mProxy;
    }

    /**
     *
     * @param hacker
     *   should be successfully hacked object.
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

    public void
    setOverridingPreference(String key, Object owner, String value) {
        mOverridingPref.put(key, new MapValue(owner, value));
    }

    public boolean
    unsetOverridingPreference(String key, Object owner) {
        MapValue v = mOverridingPref.get(key);
        if (null != v && owner != v.owner)
            return false;
        mOverridingPref.remove(key);
        return true;
    }

    public String
    getOverridingPreference(String key) {
        MapValue v = mOverridingPref.get(key);
        return null == v? null: (String)v.value;
    }

    /**
     *
     * @param key
     * @return
     *   null if overriding value DOESN'T EXIST.
     */
    public Object
    getOverridingPreferenceOwner(String key) {
        MapValue v = mOverridingPref.get(key);
        return null == v? null: v.owner;
    }
}
