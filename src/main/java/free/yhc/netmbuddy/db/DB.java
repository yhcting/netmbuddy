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
package free.yhc.netmbuddy.db;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import free.yhc.netmbuddy.core.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.utils.Utils;

public class DB implements
UnexpectedExceptionHandler.Evidence {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(DB.class);

    public static final long INVALID_PLAYLIST_ID = -1;
    public static final int INVALID_VOLUME = -1;
    public static final char BOOKMARK_DELIMITER = '@';

    // ----------------------------------------------------------------------------------------------------------------
    // Package Privates
    // ----------------------------------------------------------------------------------------------------------------
    // See ColVideo.java : BOOKMARKS field for details
    static final char BOOKMARK_NAME_DELIMIETER = '/';

    // ----------------------------------------------------------------------------------------------------------------
    // Privates
    // ----------------------------------------------------------------------------------------------------------------
    // ytmp : YouTubeMusicPlayer
    private static final String NAME = "ytmp.db";
    private static final int VERSION = 4;

    private static final String TABLE_VIDEO = "video";
    private static final String TABLE_PLAYLIST = "playlist";
    private static final String TABLE_VIDEOREF_PREFIX = "videoref_";

    private static DB instance = null;

    private DBOpenHelper mDbOpenHelper = null;
    private SQLiteDatabase mDb = null;

    // mPlTblWM : PLaylist TaBLe Watcher Map
    // Watcher for playlist table is changed
    private final HashMap<Object, Boolean> mPlTblWM = new HashMap<>();
    // Video table Watcher Map
    private final HashMap<Object, Boolean> mVidTblWM = new HashMap<>();

    public enum Err {
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

    public static class Bookmark {
        public int pos;  // ms
        public String name; // Bookmark name.

        public static Bookmark[]
        decode(String bookmarkString) {
            return DBUtils.decodeBookmarks(bookmarkString);
        }

        @SuppressWarnings("unused")
        public Bookmark() { }
        public Bookmark(String aName, int aPos) {
            name = aName;
            pos = aPos;
        }

        public boolean
        equal(Bookmark bm) {
            return name.equals(bm.name) && pos == bm.pos;
        }
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
    private static boolean
    isValidVideoData(DMVideo v) {
         return (Utils.isValidValue(v.ytvid)
                 && Utils.isValidValue(v.title));
    }

    // ----------------------------------------------------------------------
    //
    // For watchers
    //
    // ----------------------------------------------------------------------
    private void
    markBooleanWatcherChanged(HashMap<Object, Boolean> hm) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (hm) {
            for (Object o : hm.keySet())
                hm.put(o, true);
        }
    }

    private void
    registerToBooleanWatcher(HashMap<Object, Boolean> hm, Object key) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (hm) {
            hm.put(key, false);
        }
    }

    private boolean
    isRegisteredToBooleanWatcher(HashMap<Object, Boolean> hm, Object key) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (hm) {
            return (null != hm.get(key));
        }
    }

    private void
    unregisterToBooleanWatcher(HashMap<Object, Boolean> hm, Object key) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (hm) {
            hm.remove(key);
        }
    }

    private boolean
    isBooleanWatcherUpdated(HashMap<Object, Boolean> hm, Object key) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
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
     * @param where field of where clause
     * @param wherev field value of where clause
     * @param fields fields to update
     * @param vs new field values
     * @return number of rows that are updated.
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
     * @param where field of where clause
     * @param wherev field value of where clause
     * @param field field to update
     * @param v new field value
     * @return number of rows that are updated.
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
     * @param plid Playlist DB id
     * @param vid This is video id (NOT video reference's id - primary key.
     * @return number of deleted item
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
        int r = mDb.update(TABLE_PLAYLIST,
                           cvs,
                           ColPlaylist.ID.getName() + " = " + plid,
                           null);
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
    insertVideo(DMVideo v) {
        if (!isValidVideoData(v))
            return -1;
        ContentValues cvs = ColVideo.createContentValuesForInsert(v);
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
     * @param exDbf external database file.
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
     * @param title playlist title
     * @return playlist DB id newly added.
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
        Cursor c = null;
        try {
            r = mDb.delete(TABLE_PLAYLIST,
                           ColPlaylist.ID.getName() + " = " + id,
                           null);
            eAssert(0 == r || 1 == r);
            if (r > 0) {
                c = mDb.query(getVideoRefTableName(id),
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
            if (null != c)
                c.close();
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

    public  Cursor
    queryPlaylist(long plid, ColPlaylist[] cols) {
        return mDb.query(TABLE_PLAYLIST,
                         DBUtils.getColNames(cols),
                         ColPlaylist.ID.getName() + " = " + plid,
                         null, null, null, null);
    }

    /**
     * Returned object can be type-casted to one of follows
     *  - Long
     *  - String
     *  - byte[]
     * @param plid playlist DB id
     * @param col column
     * @return value of column
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
     */
    public boolean
    containsVideo(String ytvid) {
        Cursor c = queryVideos(new ColVideo[]{ColVideo.ID}, ColVideo.VIDEOID, ytvid);
        boolean r = c.getCount() > 0;
        c.close();
        return r;
    }

    /**
     * Does playlist contains given video?
     */
    @SuppressWarnings("unused")
    public boolean
    containsVideo(long plid, String ytvid) {
        Cursor c = mDb.rawQuery(DBUtils.buildQueryVideosSQL(
        plid,
        new ColVideo[]{ColVideo.ID},
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
     * @param plid playlist DB id
     * @param vid video DB id
     * @return Err.DB_DUPLICATED if video is duplicated one.
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
     * @param plid Playlist DB id
     */
    public Err
    insertVideoToPlaylist(long plid, DMVideo v) {
        if (!isValidVideoData(v))
            return Err.UNKNOWN;

        Cursor c = queryVideos(new ColVideo[] { ColVideo.ID }, ColVideo.VIDEOID, v.ytvid);
        eAssert(0 == c.getCount() || 1 == c.getCount());
        long vid;
        if (c.getCount() <= 0) {
            c.close();
            // This is new video
            mDb.beginTransaction();
            try {
                vid = insertVideo(v);
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

    // ----------------------------------------------------------------------
    // For bookmarks
    // ----------------------------------------------------------------------
    /**
     * @param vid Video DB id
     * @param name bookmark name
     * @param position milliseconds.
     * @return 1 for success otherwise unexpected error.
     */
    public int
    addBookmark(long vid, String name, int position) {
        String bmsstr = (String)getVideoInfo(vid, ColVideo.BOOKMARKS);
        bmsstr = DBUtils.addBookmark(bmsstr, new Bookmark(name, position));
        return updateVideo(ColVideo.ID, vid, ColVideo.BOOKMARKS, bmsstr);
    }

    @SuppressWarnings("unused")
    public int
    deleteBookmark(long vid, String name, int position) {
        String bmsstr = (String)getVideoInfo(vid, ColVideo.BOOKMARKS);
        DBUtils.deleteBookmark(bmsstr, new Bookmark(name, position));
        return updateVideo(ColVideo.ID, vid, ColVideo.BOOKMARKS, bmsstr);
    }

    public int
    deleteBookmark(String ytvid, String name, int position) {
        String bmsstr = (String)getVideoInfo(ytvid, ColVideo.BOOKMARKS);
        bmsstr = DBUtils.deleteBookmark(bmsstr, new Bookmark(name, position));
        return updateVideo(ColVideo.VIDEOID, ytvid, ColVideo.BOOKMARKS, bmsstr);
    }

    @SuppressWarnings("unused")
    public Bookmark[]
    getBookmarks(long vid) {
        String bmsstr = (String)getVideoInfo(vid, ColVideo.BOOKMARKS);
        return DBUtils.decodeBookmarks(bmsstr);
    }

    public Bookmark[]
    getBookmarks(String ytvid) {
        String bmsstr = (String)getVideoInfo(ytvid, ColVideo.BOOKMARKS);
        return DBUtils.decodeBookmarks(bmsstr);
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------
    /**
     * Delete video from given playlist.
     * @param plid playlist
     * @param vid Video id (NOT Video reference id).
     * @return number of deleted item
     */
    public int
    deleteVideoFrom(long plid, long vid) {
        return deleteVideoRef(plid, vid);
    }

    /**
     * Delete video from all playlists except for given playlist.
     * @param plid exceptional playlist
     * @param vid video DB id.
     * @return number of deleted item
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
     * @return number of times deleted.
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
     * @param titleLikes sub strings to search(Not token). So, search with 'ab' may find '123abcd'.
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

    @SuppressWarnings("unused")
    public Cursor
    queryVideo(long vid, ColVideo[] cols) {
        eAssert(cols.length > 0);
        return mDb.query(TABLE_VIDEO,
                         DBUtils.getColNames(cols),
                         ColVideo.ID.getName() + " = " + vid,
                         null, null, null, null);
    }

    public DMVideo
    getVideoInfo(long vid, ColVideo[] cols) {
        Cursor c = mDb.query(TABLE_VIDEO,
                             DBUtils.getColNames(cols),
                             ColVideo.ID.getName() + " = " + vid,
                             null, null, null, null);
        if (!c.moveToFirst())
            eAssert(false);
        DMVideo v = new DMVideo();
        v.setData(cols, c);
        c.close();
        return v;
    }

    /**
     * Returned value can be type-casted to one of follows
     *  - Long
     *  - String
     *  - byte[]
     */
    public Object
    getVideoInfo(String ytvid, ColVideo col) {
        Cursor c = mDb.query(TABLE_VIDEO,
                             DBUtils.getColNames(new ColVideo[] { col }),
                             ColVideo.VIDEOID.getName() + " = " + DatabaseUtils.sqlEscapeString(ytvid),
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

    public Object
    getVideoInfo(long vid, ColVideo col) {
        Cursor c = mDb.query(TABLE_VIDEO,
                             DBUtils.getColNames(new ColVideo[] { col }),
                             ColVideo.ID.getName() + " = " + vid,
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
     * @param vid DB-id of video in Video Table(TABLE_VIDEO).
     */
    public long[]
    getPlaylistsContainVideo(long vid) {
        Cursor plc = queryPlaylist(new ColPlaylist[] { ColPlaylist.ID });
        if (!plc.moveToFirst()) {
            plc.close();
            return new long[0];
        }

        ArrayList<Long> pls = new ArrayList<>();
        do {
            long plid = plc.getLong(0);
            if (containsVideo(plid, vid))
                pls.add(plid);
        } while (plc.moveToNext());
        plc.close();

        return Utils.convertArrayLongTolong(pls.toArray(new Long[pls.size()]));
    }

    // ----------------------------------------------------------------------
    //
    // For watchers
    //
    // ----------------------------------------------------------------------
    /**
     * Playlist watcher is to tell whether playlist table is changed or not.
     * Not only inserting/deleting, but also updating values of fields.
     * @param key owner key of this wathcer.
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
