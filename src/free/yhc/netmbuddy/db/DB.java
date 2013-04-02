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

package free.yhc.netmbuddy.db;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import free.yhc.netmbuddy.model.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.utils.Utils;

public class DB implements
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(DB.class);

    public static final long    INVALID_PLAYLIST_ID = -1;
    public static final int     INVALID_VOLUME      = -1;

    // ytmp : YouTubeMusicPlayer
    private static final String NAME            = "ytmp.db";
    private static final int    VERSION         = 2;

    private static final String TABLE_VIDEO             = "video";
    private static final String TABLE_PLAYLIST          = "playlist";
    private static final String TABLE_VIDEOREF_PREFIX   = "videoref_";

    private static DB instance = null;

    private DBOpenHelper        mDbOpenHelper = null;
    private SQLiteDatabase      mDb = null;

    // mPlTblWM : PLaylist TaBLe Watcher Map
    // Watcher for playlist table is changed
    private final HashMap<Object, Boolean> mPlTblWM     = new HashMap<Object, Boolean>();
    // Video table Watcher Map
    private final HashMap<Object, Boolean> mVidTblWM    = new HashMap<Object, Boolean>();

    public static enum Err {
        NO_ERR,
        VERSION_MISMATCH,
        IO_FILE,
        INVALID_DB,
        DUPLICATED,
        INTERRUPTED,
        UNKNOWN,   // err inside module
    }

    public interface Col {
        String getName();
        String getType();
        String getConstraint();
        String getDefault();
    }

    private class DBOpenHelper extends SQLiteOpenHelper {
        DBOpenHelper() {
            super(Utils.getAppContext(), NAME, null, getVersion());
        }

        @Override
        public void
        onCreate(SQLiteDatabase db) {
            db.execSQL(DBUtils.buildTableSQL(TABLE_VIDEO, ColVideo.values()));
            db.execSQL(DBUtils.buildTableSQL(TABLE_PLAYLIST, ColPlaylist.values()));
        }

        @Override
        public void
        onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            new DBUpgrader(db, oldVersion, newVersion).upgrade();
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
    }

    static String
    getName() {
        return NAME;
    }

    static int
    getVersion() {
        return VERSION;
    }

    static String
    getPlaylistTableName() {
        return TABLE_PLAYLIST;
    }

    static String
    getVideoTableName() {
        return TABLE_VIDEO;
    }

    static String
    getVideoRefTableName(long playlistId) {
        return TABLE_VIDEOREF_PREFIX + playlistId;
    }



    // ======================================================================
    //
    // Creation / Upgrade
    //
    // ======================================================================
    private DB() {
        UnexpectedExceptionHandler.get().registerModule(this);
    }

    public static DB
    get() {
        if (null == instance)
            instance = new DB();
        return instance;
    }

    public void
    open() {
        eAssert(null == mDb && null == mDbOpenHelper);
        mDbOpenHelper = new DBOpenHelper();
        mDb = mDbOpenHelper.getWritableDatabase();
    }

    // package private.
    void
    close() {
        mDb.close();
        mDb = null;
        mDbOpenHelper.close();
        mDbOpenHelper = null;
    }


    // ======================================================================
    //
    // Operations (Private)
    //
    // ======================================================================

    // ----------------------------------------------------------------------
    //
    // For watchers
    //
    // ----------------------------------------------------------------------
    private void
    markBooleanWatcherChanged(HashMap<Object, Boolean> hm) {
        synchronized (hm) {
            Iterator<Object> itr = hm.keySet().iterator();
            while (itr.hasNext())
                hm.put(itr.next(), true);
        }
    }

    private void
    registerToBooleanWatcher(HashMap<Object, Boolean> hm, Object key) {
        synchronized (hm) {
            hm.put(key, false);
        }
    }

    private boolean
    isRegisteredToBooleanWatcher(HashMap<Object, Boolean> hm, Object key) {
        synchronized (hm) {
            return (null != hm.get(key));
        }
    }

    private void
    unregisterToBooleanWatcher(HashMap<Object, Boolean> hm, Object key) {
        synchronized (hm) {
            hm.remove(key);
        }
    }

    private boolean
    isBooleanWatcherUpdated(HashMap<Object, Boolean> hm, Object key) {
        synchronized (hm) {
            return hm.get(key);
        }
    }

    // ----------------------------------------------------------------------
    //
    // For TABLE_VIDEO
    //
    // ----------------------------------------------------------------------
    private Cursor
    queryVideos(ColVideo[] cols, ColVideo whCol, Object v) {
        return mDb.query(TABLE_VIDEO,
                         DBUtils.getColNames(cols),
                         whCol.getName() + " = " + DatabaseUtils.sqlEscapeString(v.toString()),
                         null, null, null, null);
    }

    private int
    deleteVideo(long id) {
        int r = mDb.delete(TABLE_VIDEO, ColVideo.ID.getName() + " = " + id, null);
        if (r > 0)
            markBooleanWatcherChanged(mVidTblWM);
        return r;
    }

    /**
     * Update video value
     * @param where
     *   field of where clause
     * @param wherev
     *   field value of where clause
     * @param fields
     *   fields to update
     * @param vs
     *   new field values
     * @return
     *   number of rows that are updated.
     */
    private int
    updateVideo(ColVideo where, Object wherev,
                ColVideo[] fields, Object[] vs) {
        eAssert(fields.length == vs.length);
        ContentValues cvs = new ContentValues();
        for (int i = 0; i < fields.length; i++) {
            try {
                Method m = cvs.getClass().getMethod("put", String.class, vs[i].getClass());
                m.invoke(cvs, fields[i].getName(), vs[i]);
            } catch (Exception e) {
                eAssert(false);
            }
        }

        int r = mDb.update(TABLE_VIDEO,
                           cvs,
                           where.getName() + " = " + DatabaseUtils.sqlEscapeString(wherev.toString()),
                           null);
        if (r > 0)
            markBooleanWatcherChanged(mVidTblWM);
        return r;
    }

    /**
     * Update video value
     * @param where
     *   field of where clause
     * @param wherev
     *   field value of where clause
     * @param field
     *   field to update
     * @param v
     *   new field value
     * @return
     *   number of rows that are updated.
     */
    private int
    updateVideo(ColVideo where, Object wherev,
                ColVideo field, Object v) {
        return updateVideo(where,
                           wherev,
                           new ColVideo[] { field },
                           new Object[] { v });
    }

    private int
    updateVideo(long id, ColVideo[] cols, Object[] vs) {
        return updateVideo(ColVideo.ID, id, cols, vs);
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
    private boolean
    containsVideo(long plid, long vid) {
        Cursor c = mDb.query(getVideoRefTableName(plid),
                             new String[] { ColVideoRef.ID.getName() },
                             ColVideoRef.VIDEOID.getName() + " = " + vid,
                             null, null, null, null);
        boolean ret = c.getCount() > 0;
        c.close();
        return ret;
    }

    /**
     *
     * @param plid
     * @param vid
     *   NOTE : This is video id (NOT video reference's id - primary key.
     * @return
     */
    private int
    deleteVideoRef(long plid, long vid) {
        int r = 0;
        try {
            mDb.beginTransaction();
            r =  mDb.delete(getVideoRefTableName(plid),
                            ColVideoRef.VIDEOID.getName() + " = " + vid,
                            null);

            // NOTE
            // "eAssert(0 == r || 1 == r);" is expected.
            // But, who knows? (may from unknown bug...)
            // To increase code tolerance, case that "r > 1" is also handled here.
            while (r-- > 0) {
                decVideoReference(vid);
                decPlaylistSize(plid);
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return r;
    }

    // ----------------------------------------------------------------------
    //
    // For TABLE_PLAYLIST
    //
    // ----------------------------------------------------------------------


    // Private
    // ---------------
    private Cursor
    queryPlaylist(long plid, ColPlaylist col) {
        return mDb.query(TABLE_PLAYLIST,
                         new String[] { col.getName() },
                         ColPlaylist.ID.getName() + " = " + plid,
                         null, null, null, null);
    }

    private int
    updatePlaylistSize(long plid, long size) {
        ContentValues cvs = new ContentValues();
        cvs.put(ColPlaylist.SIZE.getName(), size);
        int r = mDb.update(TABLE_PLAYLIST, cvs, ColPlaylist.ID.getName() + " = " + plid, null);
        if (r > 0)
            markBooleanWatcherChanged(mPlTblWM);
        return r;
    }

    private void
    incPlaylistSize(long plid) {
        long sz = (Long)getPlaylistInfo(plid, ColPlaylist.SIZE);
        eAssert(sz >= 0);
        sz++;
        updatePlaylistSize(plid, sz);
    }

    private void
    decPlaylistSize(long plid) {
        long sz = (Long)getPlaylistInfo(plid, ColPlaylist.SIZE);
        eAssert(sz > 0);
        sz--;
        updatePlaylistSize(plid, sz);
    }

    // ======================================================================
    //
    // Package Privates
    //   - used by DBManager.
    //
    // ======================================================================
    long
    insertVideo(ContentValues cvs) {
        long r = mDb.insert(TABLE_VIDEO, null, cvs);
        if (r >= 0)
            markBooleanWatcherChanged(mVidTblWM);
        return r;
    }

    long
    insertVideo(String title, String url,
                int playtime, String author,
                byte[] thumbnail, int volume) {
        ContentValues cvs = ColVideo.createContentValuesForInsert(title, url,
                                                                  playtime, author,
                                                                  thumbnail, volume);
        return insertVideo(cvs);
    }

    long
    insertPlaylist(ContentValues cvs) {
        long id = -1;
        mDb.beginTransaction();
        try {
            id = mDb.insert(TABLE_PLAYLIST, null, cvs);
            if (id >= 0) {
                mDb.execSQL(DBUtils.buildTableSQL(getVideoRefTableName(id), ColVideoRef.values()));
                markBooleanWatcherChanged(mPlTblWM);
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }

        return id;
    }

    long
    insertVideoRef(long plid, long vid) {
        ContentValues cvs = new ContentValues();
        cvs.put(ColVideoRef.VIDEOID.getName(), vid);
        long r = -1;
        mDb.beginTransaction();
        try {
            r = mDb.insert(getVideoRefTableName(plid), null, cvs);
            if (r >= 0) {
                incVideoReference(vid);
                incPlaylistSize(plid);
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return r;
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    // ======================================================================
    //
    // Importing/Exporting DB
    //
    // ======================================================================
    /**
     * Extremely critical function.
     * PREREQUISITE
     *   All operations that might access DB, SHOULD BE STOPPED
     *     before importing DB.
     *   And that operation should be resumed after importing DB.
     * @param exDbf
     */

    public Err
    importDatabase(File exDbf) {
        Err err = DBManager.importDatabase(exDbf);
        if (Err.NO_ERR == err) {
            // DB is successfully imported!
            // Mark that playlist table is changed.
            markBooleanWatcherChanged(mPlTblWM);
            markBooleanWatcherChanged(mVidTblWM);
        }
        return err;
    }

    public Err
    mergeDatabase(File exDbf) {
        Err err = DBManager.mergeDatabase(exDbf);
        if (Err.NO_ERR == err) {
            markBooleanWatcherChanged(mPlTblWM);
            markBooleanWatcherChanged(mVidTblWM);
        }
        return err;
    }

    public Err
    exportDatabase(File exDbf) {
        return DBManager.exportDatabase(exDbf);
    }

    // ======================================================================
    //
    // Transaction
    //
    // ======================================================================
    public void
    beginTransaction() {
        mDb.beginTransaction();
    }

    public void
    setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }

    public void
    endTransaction() {
        mDb.endTransaction();
    }

    // ======================================================================
    //
    // Operations
    //
    // ======================================================================
    public boolean
    containsPlaylist(String title) {
        boolean r;
        Cursor c = mDb.query(TABLE_PLAYLIST,
                             new String[] { ColPlaylist.ID.getName() },
                             ColPlaylist.TITLE.getName() + " = " + DatabaseUtils.sqlEscapeString(title),
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
    insertPlaylist(String title) {
        return insertPlaylist(ColPlaylist.createContentValuesForInsert(title));
    }

    public int
    updatePlaylist(long plid, ColPlaylist[] fields, Object[] vs) {
        eAssert(fields.length == vs.length);
        ContentValues cvs = new ContentValues();
        try {
            for (int i = 0; i < fields.length; i++) {
                Method m = cvs.getClass().getMethod("put", String.class, vs[i].getClass());
                m.invoke(cvs, fields[i].getName(), vs[i]);
            }
        } catch (Exception e) {
            eAssert(false);
        }
        int r = mDb.update(TABLE_PLAYLIST,
                           cvs,
                           ColPlaylist.ID.getName() + " = " + plid,
                           null);
        if (r > 0)
            markBooleanWatcherChanged(mPlTblWM);

        return r;
    }

    public int
    updatePlaylist(long plid,
                   ColPlaylist field, Object v) {
        return updatePlaylist(plid, new ColPlaylist[] { field }, new Object[] { v });
    }

    public int
    deletePlaylist(long id) {
        int r = -1;
        mDb.beginTransaction();
        try {
            r = mDb.delete(TABLE_PLAYLIST, ColPlaylist.ID.getName() + " = " + id, null);
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
                markBooleanWatcherChanged(mPlTblWM);
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return r;
    }

    public Cursor
    queryPlaylist(ColPlaylist[] cols) {
        return mDb.query(TABLE_PLAYLIST,
                DBUtils.getColNames(cols),
                null, null, null, null,
                ColPlaylist.TITLE.getName());
    }

    /**
     * Returned object can be type-casted to one of follows
     *  - Long
     *  - String
     *  - byte[]
     * @param plid
     * @param col
     * @return
     */
    public Object
    getPlaylistInfo(long plid, ColPlaylist col) {
        Cursor c = queryPlaylist(plid, col);
        try {
            if (c.moveToFirst())
                return DBUtils.getCursorVal(c, col);
            else
                return null;
        } finally {
            c.close();
        }
    }

    /**
     * Any of playlists contain given video?
     * @param ytvid
     * @return
     */
    public boolean
    containsVideo(String ytvid) {
        Cursor c = queryVideos(new ColVideo[] { ColVideo.ID }, ColVideo.VIDEOID, ytvid);
        boolean r = c.getCount() > 0;
        c.close();
        return r;
    }

    /**
     * Does playlist contains given video?
     * @param plid
     * @param ytvid
     * @return
     */
    public boolean
    containsVideo(long plid, String ytvid) {
        Cursor c = mDb.rawQuery(DBUtils.buildQueryVideosSQL(
                                    plid,
                                    new ColVideo[] { ColVideo.ID },
                                    ColVideo.VIDEOID,
                                    ytvid,
                                    null,
                                    true),
                                null);
        boolean r = c.getCount() > 0;
        c.close();
        return r;
    }

    /**
     * Insert video.
     * @param plid
     * @param vid
     * @return
     *   Err.DB_DUPLICATED if video is duplicated one.
     */
    public Err
    insertVideoToPlaylist(long plid, long vid) {
        if (containsVideo(plid, vid))
            return Err.DUPLICATED;

        if (0 > insertVideoRef(plid, vid))
            return Err.UNKNOWN;

        return Err.NO_ERR;
    }

    /**
     * This is NOT THREAD SAFE
     * checking for duplication and inserting to DB is not an atomic operation in this function.
     * @param plid
     *   Playlist DB id
     * @param title
     * @param desc
     * @param url
     * @param playtime
     * @param thumbnail
     * @param volume
     * @return
     *   -1 for error (ex. already exist)
     */
    public Err
    insertVideoToPlaylist(long plid,
                          String videoId, String title,
                          String author, int playtime,
                          byte[] thumbnail, int volume) {
        Cursor c = queryVideos(new ColVideo[] { ColVideo.ID }, ColVideo.VIDEOID, videoId);
        eAssert(0 == c.getCount() || 1 == c.getCount());
        long vid;
        if (c.getCount() <= 0) {
            // This is new video
            c.close();
            mDb.beginTransaction();
            try {
                vid = insertVideo(title, videoId,
                                  playtime, author,
                                  thumbnail, volume);
                if (vid < 0)
                    return Err.UNKNOWN;

                if (0 > insertVideoRef(plid, vid))
                    return Err.UNKNOWN;

                mDb.setTransactionSuccessful();
            } finally {
                mDb.endTransaction();
            }
        } else {
            c.moveToFirst();
            vid = c.getLong(0);
            c.close();
            if (containsVideo(plid, vid))
                return Err.DUPLICATED;

            if (0 > insertVideoRef(plid, vid))
                return Err.UNKNOWN;
        }
        return Err.NO_ERR;
    }

    public int
    updateVideoTitle(long vid, String title) {
        eAssert(null != title
                && !title.isEmpty());
        return updateVideo(ColVideo.ID, vid, ColVideo.TITLE, title);
    }

    public int
    updateVideoTimePlayed(String ytvid, long time) {
        return updateVideo(ColVideo.VIDEOID, ytvid, ColVideo.TIME_PLAYED, time);
    }

    public int
    updateVideoVolume(String ytvid, int volume) {
        return updateVideo(ColVideo.VIDEOID, ytvid, ColVideo.VOLUME, volume);
    }

    /**
     * Delete video from given playlist.
     * @param plid
     * @param vid
     *   NOTE : Video id (NOT Video reference id).
     * @return
     */
    public int
    deleteVideoFrom(long plid, long vid) {
        return deleteVideoRef(plid, vid);
    }

    /**
     * Delete video from all playlists except for given playlist.
     * @param plid
     * @param vid
     * @return
     */
    public int
    deleteVideoExcept(long plid, long vid) {
        Cursor c = queryPlaylist(new ColPlaylist[] { ColPlaylist.ID });
        if (!c.moveToFirst()) {
            c.close();
            return 0;
        }
        // NOTE
        // "deleteVideoFromPlaylist()" is very expensive operation.
        // So, calling "deleteVideoFromPlaylist()" for all playlist table is very expensive.
        int cnt = 0;
        do {
            if (c.getLong(0) != plid)
                cnt += deleteVideoFrom(c.getLong(0), vid);
        } while (c.moveToNext());
        c.close();
        return cnt;
    }

    /**
     * Delete video from all playlists
     * @param vid
     * @return
     */
    public int
    deleteVideoFromAll(long vid) {
        return deleteVideoExcept(-1, vid);
    }

    public Cursor
    queryVideos(ColVideo[] cols, ColVideo colOrderBy, boolean asc) {
        return mDb.query(TABLE_VIDEO,
                         DBUtils.getColNames(cols),
                         null, null, null, null, DBUtils.buildSQLOrderBy(false, colOrderBy, asc));
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
        return mDb.rawQuery(DBUtils.buildQueryVideosSQL(plid, cols, null, null, colOrderBy, asc), null);
    }

    // NOTE
    // Usually, number of videos in the playlist at most 10,000;
    // And user usually expects so-called "sub string search" (Not token search).
    // That's the reason why 'LIKE' is used instead of FTS3/FTS4.
    // If performance is critical, using FTS3/FTS4 should be considered seriously.
    /**
     *
     * @param cols
     * @param titleLikes
     *   sub strings to search(Not token). So, search with 'ab' may find '123abcd'.
     * @return
     */
    public Cursor
    queryVideosSearchTitle(ColVideo[] cols, String[] titleLikes) {
        String selection;
        if (null == titleLikes || 0 == titleLikes.length)
            selection = null;
        else {
            String lhv = ColVideo.TITLE.getName() + " LIKE ";
            selection = lhv + DatabaseUtils.sqlEscapeString("%" + titleLikes[0] + "%");
            for (int i = 1; i < titleLikes.length; i++)
                selection += " AND " + lhv + DatabaseUtils.sqlEscapeString("%" + titleLikes[i] + "%");
        }
        return mDb.query(TABLE_VIDEO,
                         DBUtils.getColNames(cols),
                         selection,
                         null, null, null, DBUtils.buildSQLOrderBy(false, ColVideo.TITLE, true));
    }

    public Cursor
    queryVideo(long vid, ColVideo[] cols) {
        eAssert(cols.length > 0);
        return mDb.query(TABLE_VIDEO,
                         DBUtils.getColNames(cols),
                         ColVideo.ID.getName() + " = " + vid,
                         null, null, null, null);
    }

    /**
     * Returned value can be type-casted to one of follows
     *  - Long
     *  - String
     *  - byte[]
     * @param ytvid
     * @param col
     * @return
     */
    public Object
    getVideoInfo(String ytvid, ColVideo col) {
        Cursor c = mDb.query(TABLE_VIDEO,
                             DBUtils.getColNames(new ColVideo[] { col }),
                             ColVideo.VIDEOID + " = " + DatabaseUtils.sqlEscapeString(ytvid),
                             null, null, null, null);
        eAssert(0 == c.getCount() || 1 == c.getCount());
        try {
            if (c.moveToFirst())
                return DBUtils.getCursorVal(c, col);
            else
                return null;
        } finally {
            c.close();
        }
    }

    /**
     * Get playlist's DB-ids which contains given video.
     * @param vid
     *   DB-id of video in Video Table(TABLE_VIDEO).
     * @return
     */
    public long[]
    getPlaylistsContainVideo(long vid) {
        Cursor plc = queryPlaylist(new ColPlaylist[] { ColPlaylist.ID });
        if (!plc.moveToFirst()) {
            plc.close();
            return new long[0];
        }

        ArrayList<Long> pls = new ArrayList<Long>();
        do {
            long plid = plc.getLong(0);
            if (containsVideo(plid, vid))
                pls.add(plid);
        } while (plc.moveToNext());
        plc.close();

        return Utils.convertArrayLongTolong(pls.toArray(new Long[0]));
    }


    // ----------------------------------------------------------------------
    //
    // For watchers
    //
    // ----------------------------------------------------------------------
    /**
     * Playlist watcher is to tell whether playlist table is changed or not.
     * Not only inserting/deleting, but also updating values of fields.
     * @param key
     *   owner key of this wathcer.
     */
    public void
    registerToPlaylistTableWatcher(Object key) {
        registerToBooleanWatcher(mPlTblWM, key);
    }

    public boolean
    isRegisteredToPlaylistTableWatcher(Object key) {
        return isRegisteredToBooleanWatcher(mPlTblWM, key);
    }

    public void
    unregisterToPlaylistTableWatcher(Object key) {
        unregisterToBooleanWatcher(mPlTblWM, key);
    }

    public boolean
    isPlaylistTableUpdated(Object key) {
        return isBooleanWatcherUpdated(mPlTblWM, key);
    }

    // ------------------------------------------------------------------------

    public void
    registerToVideoTableWatcher(Object key) {
        registerToBooleanWatcher(mVidTblWM, key);
    }

    public boolean
    isRegisteredToVideoTableWatcher(Object key) {
        return isRegisteredToBooleanWatcher(mVidTblWM, key);
    }

    public void
    unregisterToVideoTableWatcher(Object key) {
        unregisterToBooleanWatcher(mVidTblWM, key);
    }

    public boolean
    isVideoTableUpdated(Object key) {
        return isBooleanWatcherUpdated(mVidTblWM, key);
    }
}
