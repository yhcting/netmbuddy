/******************************************************************************
 * Copyright (C) 2015
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

package free.yhc.netmbuddy.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

public class JsonUtils {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(JsonUtils.class);

    public static abstract class JsonModel {
        public void
        set(JSONObject jo) {
            eAssert(false); // Not implemented yet.
        }

        public JSONObject
        toJson() {
            eAssert(false); // Not implemented yet.
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static JSONObject
    jGetJObject(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getJSONObject(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static JSONArray
    jGetJArray(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getJSONArray(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static Object
    jGet(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.get(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static String
    jGetString(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getString(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static Integer
    jGetInt(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getInt(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static int
    jGetInt(JSONObject jo, String key, int defv) {
        Integer i = jGetInt(jo, key);
        return null == i ? defv : i;
    }

    public static Long
    jGetLong(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getLong(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static long
    jGetLong(JSONObject jo, String key, long defv) {
        Long l = jGetLong(jo, key);
        return null == l ? defv : l;
    }

    public static Boolean
    jGetBoolean(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getBoolean(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static Double
    jGetDouble(JSONObject jo, String key) {
        if (null == jo)
            return null;
        try {
            return jo.getDouble(key);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static String[]
    jGetStrings(JSONObject jo, String key) {
        JSONArray ja = jGetJArray(jo, key);
        if (null == ja)
            return null;
        String[] r = new String[ja.length()];
        try {
            for (int i = 0; i < r.length; i++)
                r[i] = ja.getString(i);
            return r;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <K extends JsonModel> K
    jGetObject(JSONObject jo, String key, Class<K> cls) {
        if (null == jo)
            return null;
        try {
            JSONObject o = jo.getJSONObject(key);
            K r;
            try {
                r = cls.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            r.set(o);
            return r;
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static <K extends JsonModel> K[]
    jGetObjects(JSONObject jo, String key, Class<K> cls) {
        JSONArray ja = jGetJArray(jo, key);
        if (null == ja)
            return null;
        @SuppressWarnings("unchecked")
        K[] r = (K[]) Array.newInstance(cls, ja.length());
        try {
            for (int i = 0; i < r.length; i++) {
                try {
                    r[i] = cls.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    eAssert(false);
                }
                r[i].set(ja.getJSONObject(i));
            }
            return r;
        } catch (JSONException e) {
            eAssert(false);
        }
        return null;
    }

    /**
     * @return true: success,
     *         false: error(ex. newkey already exists. Unknown json error)
     */
    public static boolean
    jReplaceKey(JSONObject jo, String oldkey, String newkey) {
        Object o;
        if (oldkey.equals(newkey))
            return true; // nothing to do.

        if (jo.has(newkey))
            return false;

        try {
            o = jo.get(oldkey);
        } catch (JSONException e) {
            return true; // There is no such mapping. Nothing to do. Let's say success.
        }

        try {
            jo.put(newkey, o);
        } catch (JSONException e) {
            return false; // error in json.
        }

        jo.remove(oldkey);
        return true;
    }
}
