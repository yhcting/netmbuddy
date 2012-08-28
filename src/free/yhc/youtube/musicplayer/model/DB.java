package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;

import java.lang.reflect.Method;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DB extends SQLiteOpenHelper {
    // ytmp : YouTubeMusicPlayer
    private static final String NAME            = "ytmp.db";
    private static final int    VERSION         = 1;

    private static final String TABLE_VIDEO             = "video";
    private static final String TABLE_PLAYLIST          = "playlist";
    private static final String TABLE_VIDEOREF_PREFIX   = "videoref_";

    private static DB instance = null;

    private SQLiteDatabase mDb = null;

    public interface Col {
        String getName();
        String getType();
        String getConstraint();
    }

    public static enum ColPlayList implements Col {
        TITLE           ("title",           "text",     "not null"),
        // DESCRIPTION : Not used yet - reserved for future use.
        DESCRIPTION     ("description",     "text",     "not null"),
        THUMBNAIL       ("thumbnail",       "blob",     "not null"),
        ID              (BaseColumns._ID,   "integer",  "primary key autoincrement");

        private final String name;
        private final String type;
        private final String constraint;

        static ContentValues
        createContentValuesForInsert(String title, String desc) {
            eAssert(null != title);
            if (null == desc)
                desc = "";

            ContentValues cvs = new ContentValues();
            cvs.put(ColPlayList.TITLE.getName(), title);
            cvs.put(ColPlayList.DESCRIPTION.getName(), desc);
            cvs.put(ColPlayList.THUMBNAIL.getName(), new byte[0]);
            return cvs;
        }

        ColPlayList(String aName, String aType, String aConstraint) {
            name = aName;
            type = aType;
            constraint = aConstraint;
        }
        @Override
        public String getName() { return name; }
        @Override
        public String getType() { return type; }
        @Override
        public String getConstraint() { return constraint; }
    }

    // NOTE
    // This is just video list
    // To get detail and sorted table, 'Joining Table' should be used.
    public static enum ColVideoRef implements Col {
        VIDEOID         ("videoid",         "integer",  ""),
        ID              (BaseColumns._ID,   "integer",  "primary key autoincrement, "
                + "FOREIGN KEY(videoid) REFERENCES " + TABLE_VIDEO + "(" + ColVideo.ID.getName() + ")");

        private final String name;
        private final String type;
        private final String constraint;

        ColVideoRef(String aName, String aType, String aConstraint) {
            name = aName;
            type = aType;
            constraint = aConstraint;
        }
        @Override
        public String getName() { return name; }
        @Override
        public String getType() { return type; }
        @Override
        public String getConstraint() { return constraint; }
    }

    public static enum ColVideo implements Col {
        // --------------------------------------------------------------------
        // Youtube information
        // --------------------------------------------------------------------
        TITLE           ("title",           "text",     "not null"),
        DESCRIPTION     ("description",     "text",     "not null"),
        VIDEOID         ("videoid",         "text",     "not null"),
        PLAYTIME        ("playtime",        "integer",  "not null"),
        THUMBNAIL       ("thumbnail",       "blob",     "not null"),

        // --------------------------------------------------------------------
        // Custom information
        // --------------------------------------------------------------------
        // Why volume is here?
        // Each Youtube video has it's own volume that is set at encoding step.
        // So, even if device volume setting is not changed, some video
        //   plays with loud sound but others are not.
        // To tune this variance between videos this field is required.
        VOLUME          ("volume",          "integer",  "not null"),
        RATE            ("rate",            "integer",  "not null"), // my rate of this Video
        TIME_ADD        ("time_add",        "integer",  "not null"),
        TIME_PLAYED     ("time_played",     "integer",  "not_null"), // time last played

        // --------------------------------------------------------------------
        // Video information - Not used yet (reserved for future use)
        // --------------------------------------------------------------------
        GENRE           ("genre",           "text",     "not null"),
        ARTIST          ("artist",          "text",     "not null"),
        ALBUM           ("album",           "text",     "not null"),

        // --------------------------------------------------------------------
        // Internal use for DB management
        // --------------------------------------------------------------------
        REFCOUNT        ("refcount",        "integer",  "not null"), // reference count

        ID              (BaseColumns._ID,   "integer",  "primary key autoincrement");

        private final String name;
        private final String type;
        private final String constraint;

        static ContentValues
        createContentValuesForInsert(String title, String desc,
                                     String videoId, int playtime,
                                     byte[] thumbnail) {
            eAssert(null != title && null != videoId);
            if (null == desc)
                desc = "";
            if (null == thumbnail)
                thumbnail = new byte[0];

            ContentValues cvs = new ContentValues();
            cvs.put(ColVideo.TITLE.getName(), title);
            cvs.put(ColVideo.DESCRIPTION.getName(), desc);
            cvs.put(ColVideo.VIDEOID.getName(), videoId);
            cvs.put(ColVideo.PLAYTIME.getName(), playtime);
            cvs.put(ColVideo.THUMBNAIL.getName(), thumbnail);

            cvs.put(ColVideo.VOLUME.getName(), Policy.DefaultConstants.VIDEO_VOLUME);
            cvs.put(ColVideo.RATE.getName(), 0);
            cvs.put(ColVideo.TIME_ADD.getName(), System.currentTimeMillis());
            cvs.put(ColVideo.TIME_PLAYED.getName(), System.currentTimeMillis());

            cvs.put(ColVideo.GENRE.getName(), "");
            cvs.put(ColVideo.ARTIST.getName(), "");
            cvs.put(ColVideo.ALBUM.getName(), "");

            cvs.put(ColVideo.REFCOUNT.getName(), 0);
            return cvs;
        }

        ColVideo(String aName, String aType, String aConstraint) {
            name = aName;
            type = aType;
            constraint = aConstraint;
        }
        @Override
        public String getName() { return name; }
        @Override
        public String getType() { return type; }
        @Override
        public String getConstraint() { return constraint; }
    }

    static int
    getVersion() {
        return VERSION;
    }

    /**
     * Get SQL statement for creating table
     * @param table
     *   name of table
     * @param cols
     *   columns of table.
     * @return
     */
    private static String
    buildTableSQL(String table, Col[] cols) {
        String sql = "CREATE TABLE " + table + " (";
        for (Col col : cols) {
            sql += col.getName() + " "
                    + col.getType() + " "
                    + col.getConstraint() + ", ";
        }
        sql += ");";
        sql = sql.replace(", );", ");");
        return sql;
    }

    private static String
    buildSQLOrderBy(boolean withStatement, Col col, boolean asc) {
        if (null == col)
            return null;
        return (withStatement? "ORDER BY ": "") + col.getName() + " " + (asc? "ASC": "DESC");
    }

    /**
     * Build SQL from joining video and video-ref tables
     * @param plid
     * @param cols
     * @return
     */
    private static String
    buildQueryVideosSQL(long plid, ColVideo[] cols, ColVideo colOrderBy, boolean asc) {
        eAssert(cols.length > 0);

        String sql = "SELECT ";
        String sel = "";
        String tableVideoNS = TABLE_VIDEO + "."; // NS : NameSpace
        String[] cnames = getColNames(cols);
        for (int i = 0; i < cnames.length - 1; i++)
            sel += tableVideoNS + cnames[i] + ", ";
        sel += tableVideoNS + cnames[cnames.length - 1];

        String orderBy = buildSQLOrderBy(true, colOrderBy, asc);
        // NOTE
        // There is NO USE CASE requiring sorted cursor for videos.
        // result of querying videos don't need to be sorted cursor.
        String mrefTable = getVideoRefTableName(plid);
        sql += sel + " FROM " + TABLE_VIDEO + ", " + mrefTable
                + " WHERE " + mrefTable + "." + ColVideoRef.VIDEOID.getName()
                            + " = " + tableVideoNS + ColVideo.ID.getName()
                + " " + (null != orderBy? orderBy: "")
                + ";";
        return sql;
    }

    private static String
    getVideoRefTableName(long playlistId) {
        return TABLE_VIDEOREF_PREFIX + playlistId;
    }

    /**
     * Convert Col[] to string[] of column's name
     * @param cols
     * @return
     */
    private static String[]
    getColNames(Col[] cols) {
        String[] strs = new String[cols.length];
        for (int i = 0; i < cols.length; i++)
            strs[i] = cols[i].getName();
        return strs;
    }

    // ======================================================================
    //
    // Creation / Upgrade
    //
    // ======================================================================
    private DB() {
        super(Utils.getAppContext(), NAME, null, getVersion());
    }

    public static DB
    get() {
        if (null == instance)
            instance = new DB();
        return instance;
    }

    public void
    open() {
        eAssert(null == mDb);
        mDb = getWritableDatabase();
    }

    @Override
    public void
    onCreate(SQLiteDatabase db) {
        db.execSQL(buildTableSQL(TABLE_VIDEO, ColVideo.values()));
        db.execSQL(buildTableSQL(TABLE_PLAYLIST, ColPlayList.values()));
    }

    @Override
    public void
    onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void
    close() {
        super.close();
        // Something to do???
    }

    @Override
    public void
    onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Something to do???
    }

    // ======================================================================
    //
    // Operations (Private)
    //
    // ======================================================================


    // ----------------------------------------------------------------------
    //
    // For TABLE_VIDEO
    //
    // ----------------------------------------------------------------------
    private long
    insertVideo(String title, String desc,
                String url, int playtime,
                byte[] thumbnail) {
        ContentValues cvs = ColVideo.createContentValuesForInsert(title, desc,
                                                                  url, playtime,
                                                                  thumbnail);
        return mDb.insert(TABLE_VIDEO, null, cvs);
    }

    private Cursor
    queryVideos(ColVideo[] cols, ColVideo whCol, Object v) {
        return mDb.query(TABLE_VIDEO,
                         getColNames(cols),
                         whCol.getName() + " = " + DatabaseUtils.sqlEscapeString(v.toString()),
                         null, null, null, null);
    }

    private int
    deleteVideo(long id) {
        return mDb.delete(TABLE_VIDEO, ColVideo.ID.getName() + " = " + id, null);
    }

    private int
    updateVideo(long id, ColVideo[] cols, Object[] vs) {
        eAssert(cols.length == vs.length);
        ContentValues cvs = new ContentValues();
        for (int i = 0; i < cols.length; i++) {
            try {
                Method m = cvs.getClass().getMethod("put", String.class, vs[i].getClass());
                m.invoke(cvs, cols[i].getName(), vs[i]);
            } catch (Exception e) {
                eAssert(false);
            }
        }
        return mDb.update(TABLE_VIDEO, cvs, ColVideo.ID.getName() + " = " + id, null);
    }

    private void
    incVideoReference(long id) {
        long rcnt = getVideoInfoLong(id, ColVideo.REFCOUNT);
        eAssert(rcnt >= 0);
        rcnt++;
        updateVideo(id, new ColVideo[] { ColVideo.REFCOUNT }, new Long[] { rcnt });
    }

    private void
    decVideoReference(long id) {
        long rcnt = getVideoInfoLong(id, ColVideo.REFCOUNT);
        eAssert(rcnt >= 0);
        rcnt--;
        if (0 == rcnt)
            deleteVideo(id);
        else
            updateVideo(id, new ColVideo[] { ColVideo.REFCOUNT }, new Long[] { rcnt });
    }

    private long
    getVideoInfoLong(long id, ColVideo col) {
        Cursor c = queryVideos(new ColVideo[] { col }, ColVideo.ID, id);
        eAssert(c.getCount() > 0);
        c.moveToFirst();
        long r = c.getLong(0);
        c.close();
        return r;
    }

    // ----------------------------------------------------------------------
    //
    // For TABLE_VIDEOREF_xxx
    //
    // ----------------------------------------------------------------------

    private long
    insertVideoRef(long plid, long mid) {
        ContentValues cvs = new ContentValues();
        cvs.put(ColVideoRef.VIDEOID.getName(), mid);
        long r = -1;
        mDb.beginTransaction();
        try {
            r = mDb.insert(getVideoRefTableName(plid), null, cvs);
            if (r >= 0)
                incVideoReference(mid);
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return r;
    }

    private boolean
    doesVideoExist(long plid, long mid) {
        Cursor c = mDb.query(getVideoRefTableName(plid),
                             new String[] { ColVideoRef.ID.getName() },
                             ColVideoRef.VIDEOID.getName() + " = " + mid,
                             null, null, null, null);
        boolean ret = c.getCount() > 0;
        c.close();
        return ret;
    }

    private long
    getVideoRefInfoVideoId(long plid, long id) {
        Cursor c = mDb.query(getVideoRefTableName(plid),
                             new String[] { ColVideoRef.VIDEOID.getName() },
                             ColVideoRef.ID.getName() + " = " + id,
                             null, null, null, null);
        eAssert(c.getCount() > 0);
        c.moveToFirst();
        long mid = c.getLong(0);
        c.close();
        return mid;
    }

    /**
     *
     * @param plid
     * @param mid
     *   NOTE : This is video id (NOT video reference's id - primary key.
     * @return
     */
    private int
    deleteVideoRef(long plid, long mid) {
        int r = 0;
        try {
            mDb.beginTransaction();
            r =  mDb.delete(getVideoRefTableName(plid),
                            ColVideoRef.VIDEOID.getName() + " = " + mid,
                            null);
            eAssert(0 == r || 1 == r);
            if (r > 0)
                decVideoReference(mid);
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return r;
    }

    // ======================================================================
    //
    // Operations (Public)
    //
    // ======================================================================
    public boolean
    doesPlayListExist(String title) {
        boolean r;
        Cursor c = mDb.query(TABLE_PLAYLIST,
                             new String[] { ColPlayList.ID.getName() },
                             ColPlayList.TITLE.getName() + " = " + DatabaseUtils.sqlEscapeString(title),
                             null, null, null, null);
        r = c.getCount() > 0;
        c.close();
        return r;
    }

    /**
     *
     * @param title
     * @param desc
     * @return
     */
    public long
    insertPlayList(String title, String desc) {
        // Inserting PlayList that has same 'title' is NOT allowed.
        ContentValues cvs = ColPlayList.createContentValuesForInsert(title, desc);
        long id = -1;
        mDb.beginTransaction();
        try {
            id = mDb.insert(TABLE_PLAYLIST, null, cvs);
            if (id >= 0)
                mDb.execSQL(buildTableSQL(getVideoRefTableName(id), ColVideoRef.values()));
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }

        return id;
    }

    public int
    updatePlayListName(long id, String name) {
        ContentValues cvs = new ContentValues();
        cvs.put(ColPlayList.TITLE.getName(), name);
        return mDb.update(TABLE_PLAYLIST, cvs, ColPlayList.ID.getName() + " = " + id, null);
    }

    public int
    updatePlayListThumbnail(long id, byte[] data) {
        eAssert(null != data);
        ContentValues cvs = new ContentValues();
        cvs.put(ColPlayList.THUMBNAIL.getName(), data);
        return mDb.update(TABLE_PLAYLIST, cvs, ColPlayList.ID.getName() + " = " + id, null);
    }

    public int
    deletePlayList(long id) {
        int r = -1;
        mDb.beginTransaction();
        try {
            r = mDb.delete(TABLE_PLAYLIST, ColPlayList.ID.getName() + " = " + id, null);
            eAssert(0 == r || 1 == r);
            if (r > 0) {
                Cursor c = mDb.query(getVideoRefTableName(id),
                                     new String[] { ColVideoRef.VIDEOID.getName() },
                                     null, null, null, null, null);
                if (c.moveToFirst()) {
                    do {
                        decVideoReference(c.getLong(0));
                    } while(c.moveToNext());
                }
                mDb.execSQL("DROP TABLE " + getVideoRefTableName(id) + ";");
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return r;
    }

    public Cursor
    queryPlayList(ColPlayList[] cols) {
        return mDb.query(TABLE_PLAYLIST,
                getColNames(cols),
                null, null, null, null,
                ColPlayList.TITLE.getName());
    }

    public boolean
    existVideo(String videoId) {
        Cursor c = queryVideos(new ColVideo[] { ColVideo.ID }, ColVideo.VIDEOID, videoId);
        boolean r = c.getCount() > 0;
        c.close();
        return r;
    }

    /**
     *
     * @param plid
     *   PlayList DB id
     * @param title
     * @param desc
     * @param url
     * @param playtime
     * @param thumbnail
     * @return
     *   -1 for error (ex. already exist)
     */
    public Err
    insertVideoToPlayList(long plid,
                          String title, String desc,
                          String videoId, int playtime,
                          byte[] thumbnail) {
        Cursor c = queryVideos(new ColVideo[] { ColVideo.ID }, ColVideo.VIDEOID, videoId);
        eAssert(0 == c.getCount() || 1 == c.getCount());
        long mid;
        if (c.getCount() <= 0) {
            // This is new video
            c.close();
            mDb.beginTransaction();
            try {
                mid = insertVideo(title, desc, videoId, playtime, thumbnail);
                if (mid < 0)
                    return Err.DB_UNKNOWN;

                if (0 > insertVideoRef(plid, mid))
                    return Err.DB_UNKNOWN;

                mDb.setTransactionSuccessful();
            } finally {
                mDb.endTransaction();
            }
        } else {
            c.moveToFirst();
            mid = c.getLong(0);
            c.close();
            if (doesVideoExist(plid, mid))
                return Err.DB_DUPLICATED;

            if (0 > insertVideoRef(plid, mid))
                return Err.DB_UNKNOWN;
        }
        return Err.NO_ERR;
    }

    public int
    updateVideo(ColVideo where, Object wherev,
                ColVideo field, Object v) {
        ContentValues cvs = new ContentValues();
        try {
            Method m = cvs.getClass().getMethod("put", String.class, v.getClass());
            m.invoke(cvs, field.getName(), v);
        } catch (Exception e) {
            eAssert(false);
        }
        return mDb.update(TABLE_VIDEO,
                          cvs,
                          where.getName() + " = " + DatabaseUtils.sqlEscapeString(wherev.toString()),
                          null);
    }

    /**
     *
     * @param plid
     * @param mid
     *   NOTE : Video id (NOT Video reference id).
     * @return
     */
    public int
    deleteVideoFromPlayList(long plid, long mid) {
        return deleteVideoRef(plid, mid);
    }

    public Cursor
    queryVideos(ColVideo[] cols, ColVideo colOrderBy, boolean asc) {
        return mDb.query(TABLE_VIDEO,
                         getColNames(cols),
                         null, null, null, null, buildSQLOrderBy(false, colOrderBy, asc));
    }

    /**
     * Joined table is used.
     * So, DO NOT find column index with column name!
     * @param plid
     * @param cols
     * @return
     */
    public Cursor
    queryVideos(long plid, ColVideo[] cols, ColVideo colOrderBy, boolean asc) {
        eAssert(cols.length > 0);
        return mDb.rawQuery(buildQueryVideosSQL(plid, cols, colOrderBy, asc), null);
    }

    public Cursor
    queryVideosSearchTitle(ColVideo[] cols, String titleLike) {
        return mDb.query(TABLE_VIDEO,
                         getColNames(cols),
                         ColVideo.TITLE.getName() + " LIKE " + DatabaseUtils.sqlEscapeString("%" + titleLike + "%"),
                         null, null, null, buildSQLOrderBy(false, ColVideo.TITLE, true));
    }

    public Cursor
    queryVideo(long mid, ColVideo[] cols) {
        eAssert(cols.length > 0);
        return mDb.query(TABLE_VIDEO,
                         getColNames(cols),
                         ColVideo.ID.getName() + " = " + mid,
                         null, null, null, null);
    }
}
