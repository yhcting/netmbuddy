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

    private static final String TABLE_MUSIC             = "music";
    private static final String TABLE_PLAYLIST          = "playlist";
    private static final String TABLE_MUSICREF_PREFIX   = "musicref_";

    private static DB instance = null;


    private SQLiteDatabase mDb = null;


    public interface Col {
        String getName();
        String getType();
        String getConstraint();
    }

    public static enum ColPlayList implements Col {
        TITLE           ("title",           "text",     "not null"),
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
    // This is just music list
    // To get detail and sorted table, 'Joining Table' should be used.
    public static enum ColMusicRef implements Col {
        MUSICID         ("musicid",         "integer",  ""),
        ID              (BaseColumns._ID,   "integer",  "primary key autoincrement, "
                + "FOREIGN KEY(musicid) REFERENCES " + TABLE_MUSIC + "(" + ColMusic.ID.getName() + ")");

        private final String name;
        private final String type;
        private final String constraint;

        ColMusicRef(String aName, String aType, String aConstraint) {
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

    public static enum ColMusic implements Col {
        // Youtube information
        TITLE           ("title",           "text",     "not null"),
        DESCRIPTION     ("description",     "text",     "not null"),
        URL             ("url",             "text",     "not null"),
        PLAYTIME        ("playtime",        "integer",  "not null"),
        THUMBNAIL       ("thumbnail",       "blob",     "not null"),

        // Custom information
        RATE            ("rate",            "integer",  "not null"), // my rate of this Music
        TIME_ADD        ("time_add",        "integer",  "not null"),
        TIME_PLAYED     ("time_played",     "integer",  "not_null"), // time last played

        // Music information
        GENRE           ("genre",           "text",     "not null"),
        ARTIST          ("artist",          "text",     "not null"),
        ALBUM           ("album",           "text",     "not null"),

        // Internal use for DB management
        REFCOUNT        ("refcount",        "integer",  "not null"), // reference count

        ID              (BaseColumns._ID,   "integer",  "primary key autoincrement");

        private final String name;
        private final String type;
        private final String constraint;

        static ContentValues
        createContentValuesForInsert(String title, String desc,
                                     String url, int playtime,
                                     byte[] thumbnail) {
            eAssert(null != title && null != url);
            if (null == desc)
                desc = "";
            if (null == thumbnail)
                thumbnail = new byte[0];

            ContentValues cvs = new ContentValues();
            cvs.put(ColMusic.TITLE.getName(), title);
            cvs.put(ColMusic.DESCRIPTION.getName(), desc);
            cvs.put(ColMusic.URL.getName(), url);
            cvs.put(ColMusic.PLAYTIME.getName(), playtime);
            cvs.put(ColMusic.THUMBNAIL.getName(), thumbnail);

            cvs.put(ColMusic.RATE.getName(), 0);
            cvs.put(ColMusic.TIME_ADD.getName(), System.currentTimeMillis());
            cvs.put(ColMusic.TIME_PLAYED.getName(), System.currentTimeMillis());

            cvs.put(ColMusic.GENRE.getName(), "");
            cvs.put(ColMusic.ARTIST.getName(), "");
            cvs.put(ColMusic.ALBUM.getName(), "");

            cvs.put(ColMusic.REFCOUNT.getName(), 0);
            return cvs;
        }

        ColMusic(String aName, String aType, String aConstraint) {
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

    /**
     * Build SQL from joining music and music-ref tables
     * @param plid
     * @param cols
     * @return
     */
    private static String
    buildQueryMusicsSQL(long plid, ColMusic[] cols) {
        eAssert(cols.length > 0);

        String sql = "SELECT ";
        String sel = "";
        String tableMusicNS = TABLE_MUSIC + "."; // NS : NameSpace
        String[] cnames = getColNames(cols);
        for (int i = 0; i < cnames.length - 1; i++)
            sel += tableMusicNS + cnames[i] + ", ";
        sel += tableMusicNS + cnames[cnames.length - 1];

        String mrefTable = getMusicRefTableName(plid);
        sql += sel + " FROM " + TABLE_MUSIC + ", " + mrefTable
                + " WHERE " + mrefTable + "." + ColMusicRef.MUSICID.getName() + " = "
                + tableMusicNS + ColMusic.ID.getName()
                + " ORDER BY " + tableMusicNS + ColMusic.TITLE.getName() + ";";
        return sql;
    }

    private static String
    getMusicRefTableName(long playlistId) {
        return TABLE_MUSICREF_PREFIX + playlistId;
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
        db.execSQL(buildTableSQL(TABLE_MUSIC, ColMusic.values()));
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
    // For TABLE_MUSIC
    //
    // ----------------------------------------------------------------------
    private long
    insertMusic(String title, String desc,
                String url, int playtime,
                byte[] thumbnail) {
        ContentValues cvs = ColMusic.createContentValuesForInsert(title, desc,
                                                                  url, playtime,
                                                                  thumbnail);
        return mDb.insert(TABLE_MUSIC, null, cvs);
    }

    private Cursor
    queryMusic(ColMusic[] cols, ColMusic whCol, Object v) {
        return mDb.query(TABLE_MUSIC,
                         getColNames(cols),
                         whCol.getName() + " = " + DatabaseUtils.sqlEscapeString(v.toString()),
                         null, null, null, null);
    }

    private int
    deleteMusic(long id) {
        return mDb.delete(TABLE_MUSIC, ColMusic.ID.getName() + " = " + id, null);
    }

    private int
    updateMusic(long id, ColMusic[] cols, Object[] vs) {
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
        return mDb.update(TABLE_MUSIC, cvs, ColMusic.ID.getName() + " = " + id, null);
    }

    private void
    incMusicReference(long id) {
        long rcnt = getMusicInfoLong(id, ColMusic.REFCOUNT);
        eAssert(rcnt >= 0);
        rcnt++;
        updateMusic(id, new ColMusic[] { ColMusic.REFCOUNT }, new Long[] { rcnt });
    }

    private void
    decMusicReference(long id) {
        long rcnt = getMusicInfoLong(id, ColMusic.REFCOUNT);
        eAssert(rcnt >= 0);
        rcnt--;
        if (0 == rcnt)
            deleteMusic(id);
        else
            updateMusic(id, new ColMusic[] { ColMusic.REFCOUNT }, new Long[] { rcnt });
    }

    private long
    getMusicInfoLong(long id, ColMusic col) {
        Cursor c = queryMusic(new ColMusic[] { col }, ColMusic.ID, id);
        eAssert(c.getCount() > 0);
        c.moveToFirst();
        long r = c.getLong(0);
        c.close();
        return r;
    }

    // ----------------------------------------------------------------------
    //
    // For TABLE_MUSICREF_xxx
    //
    // ----------------------------------------------------------------------

    private long
    insertMusicRef(long plid, long mid) {
        ContentValues cvs = new ContentValues();
        cvs.put(ColMusicRef.MUSICID.getName(), mid);
        long r = -1;
        mDb.beginTransaction();
        try {
            r = mDb.insert(getMusicRefTableName(plid), null, cvs);
            if (r >= 0)
                incMusicReference(mid);
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return r;
    }

    private boolean
    doesMusicExist(long plid, long mid) {
        Cursor c = mDb.query(getMusicRefTableName(plid),
                             new String[] { ColMusicRef.ID.getName() },
                             ColMusicRef.MUSICID.getName() + " = " + mid,
                             null, null, null, null);
        boolean ret = c.getCount() > 0;
        c.close();
        return ret;
    }

    private long
    getMusicRefInfoMusicId(long plid, long id) {
        Cursor c = mDb.query(getMusicRefTableName(plid),
                             new String[] { ColMusicRef.MUSICID.getName() },
                             ColMusicRef.ID.getName() + " = " + id,
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
     *   NOTE : This is music id (NOT music reference's id - primary key.
     * @return
     */
    private int
    deleteMusicRef(long plid, long mid) {
        int r = 0;
        try {
            mDb.beginTransaction();
            r =  mDb.delete(getMusicRefTableName(plid),
                                ColMusicRef.MUSICID.getName() + " = " + mid,
                                null);
            eAssert(0 == r || 1 == r);
            if (r > 0)
                decMusicReference(mid);
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
                mDb.execSQL(buildTableSQL(getMusicRefTableName(id), ColMusicRef.values()));
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }

        return id;
    }

    public int
    deletePlayList(long id) {
        int r = -1;
        mDb.beginTransaction();
        try {
            r = mDb.delete(TABLE_PLAYLIST, ColPlayList.ID.getName() + " = " + id, null);
            eAssert(0 == r || 1 == r);
            if (r > 0)
                mDb.execSQL("DROP TABLE " + getMusicRefTableName(id) + ";");
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
    existMusic(String url) {
        Cursor c = queryMusic(new ColMusic[] { ColMusic.ID }, ColMusic.URL, url);
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
    insertMusicToPlayList(long plid,
                          String title, String desc,
                          String url, int playtime,
                          byte[] thumbnail) {
        Cursor c = queryMusic(new ColMusic[] { ColMusic.ID }, ColMusic.URL, url);
        eAssert(0 == c.getCount() || 1 == c.getCount());
        long mid;
        if (c.getCount() <= 0) {
            // This is new music
            c.close();
            mDb.beginTransaction();
            try {
                mid = insertMusic(title, desc, url, playtime, thumbnail);
                if (mid < 0)
                    return Err.DB_UNKNOWN;

                if (0 > insertMusicRef(plid, mid))
                    return Err.DB_UNKNOWN;

                mDb.setTransactionSuccessful();
            } finally {
                mDb.endTransaction();
            }
        } else {
            c.moveToFirst();
            mid = c.getLong(0);
            c.close();
            if (doesMusicExist(plid, mid))
                return Err.DB_DUPLICATED;

            if (0 > insertMusicRef(plid, mid))
                return Err.DB_UNKNOWN;
        }
        return Err.NO_ERR;
    }

    /**
     *
     * @param plid
     * @param mid
     *   NOTE : Music id (NOT Music reference id).
     * @return
     */
    public int
    deleteMusicFromPlayList(long plid, long mid) {
        return deleteMusicRef(plid, mid);
    }

    /**
     * Joined table is used.
     * So, DO NOT find column index with column name!
     * @param plid
     * @param cols
     * @return
     */
    public Cursor
    queryMusics(long plid, ColMusic[] cols) {
        eAssert(cols.length > 0);
        return mDb.rawQuery(buildQueryMusicsSQL(plid, cols), null);
    }

    public Cursor
    queryMusic(long mid, ColMusic[] cols) {
        eAssert(cols.length > 0);
        return mDb.query(TABLE_MUSIC,
                         getColNames(cols),
                         ColMusic.ID.getName() + " = " + mid,
                         null, null, null, null);
    }
}
