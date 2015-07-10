package free.yhc.netmbuddy.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;

import free.yhc.netmbuddy.Err;
import free.yhc.netmbuddy.db.DB;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

public class JsonUtils {
    private static final boolean DBG = false;
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
            K r = null;
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
        K[] r = (K[]) Array.newInstance(cls, ja.length());
        try {
            for (int i = 0; i < r.length; i++) {
                try {
                    r[i] = cls.newInstance();
                } catch (InstantiationException e) {
                    eAssert(false);
                } catch (IllegalAccessException e) {
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
