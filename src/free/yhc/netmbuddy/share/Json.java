package free.yhc.netmbuddy.share;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import free.yhc.netmbuddy.model.DB;
import free.yhc.netmbuddy.model.Policy;


//============================================================================
//[ Data File Format (JSON) ]
//{
//----- Meta data -----
//meta : {
//    magic : Magic string to confirm that this is NetMBuddy compatible.
//    type : Json Object의 type. 여러가지 형태를 제공하기 위해서..
//    version : sharing format version of this type.
//        : "playlist"
//    time : exporting된 시간.(milliseconds)
//}
//
//----- Data -----
//playlist : {
//    title : playlist title
//    videos : [
//        {
//            ytid: 11-character youtube video id
//            title : title of the video (Exporter may change the video title.)
//            playtime:
//            volume: volume
//        }
//        ...
//    ]
//}
//============================================================================
class Json {
    // Field name constants.
    static final String MAGIC       = "!*&@$$#$%~NetMBuddy!@@@@!%^^*#$$##";
    static final String FVERSION    = "version";
    static final String FMETA       = "meta";
    static final String FPLAYLIST   = "playlist";
    static final String FMAGIC      = "magic";
    static final String FTYPE       = "type";
    static final String FTIME       = "time";
    static final String FYTID       = "ytid";
    static final String FVOLUME     = "volume";
    static final String FPLAYTIME   = "playtime";
    static final String FTITLE      = "title";
    static final String FVIDEOS     = "videos";

    /**
     * @param jo
     *   'meta' object
     * @return
     */
    static boolean
    verify(JSONObject jo) {
        try {
            return MAGIC.equals(jo.getString(FMAGIC));
        } catch (JSONException e) {
            return false;
        }
    }

    static JSONObject
    createMetaJson(Share.Type type) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(FMAGIC, MAGIC);
            jo.put(FVERSION, type.getVersion());
            jo.put(FTYPE, type.name());
            jo.put(FTIME, System.currentTimeMillis());
        } catch (JSONException e) {
            jo = null;
        }
        return jo;
    }

    static JSONObject
    videoToJson(long vid) {
        final int COLI_VIDEOID  = 0;
        final int COLI_TITLE    = 1;
        final int COLI_VOLUME   = 2;
        final int COLI_PLAYTIME = 3;
        Cursor c = DB.get().queryVideo(vid,
                                       new DB.ColVideo[] {
                DB.ColVideo.VIDEOID,
                DB.ColVideo.TITLE,
                DB.ColVideo.VOLUME,
                DB.ColVideo.PLAYTIME
        });

        if (!c.moveToFirst()) {
            c.close();
            return null;
        }

        JSONObject jo = new JSONObject();
        try {
            jo.put(FYTID,     c.getString(COLI_VIDEOID));
            jo.put(FTITLE,    c.getString(COLI_TITLE));
            jo.put(FPLAYTIME, c.getInt(COLI_PLAYTIME));
            int vol = c.getInt(COLI_VOLUME);
            if (Policy.DEFAULT_VIDEO_VOLUME != vol)
                jo.put(FVOLUME,   vol);
        } catch (JSONException e) {
            jo = null;
        }
        c.close();

        return jo;
    }

    static JSONObject
    playlistToJson(long plid) {
        final int COLI_ID   = 0;
        Cursor c = DB.get().queryVideos(plid,
                                        new DB.ColVideo[] { DB.ColVideo.ID },
                                        null,
                                        false);
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }

        JSONObject jo = new JSONObject();
        try {
            jo.put(FTITLE, DB.get().getPlaylistInfo(plid, DB.ColPlaylist.TITLE));
            JSONArray  jarr = new JSONArray();
            do {
                JSONObject jov = videoToJson(c.getLong(COLI_ID));
                eAssert(null != jov);
                jarr.put(jov);
            } while (c.moveToNext());
            jo.put(FVIDEOS, jarr);
        } catch (JSONException e) {
            jo = null;
        }
        c.close();

        return jo;
    }

}
