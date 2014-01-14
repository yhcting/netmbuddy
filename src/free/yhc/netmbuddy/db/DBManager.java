/******************************************************************************
 *    Copyright (C) 2012, 2013, 2014 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of NetMBuddy.
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
 *    along with this program.	If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy.db;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.DB.Err;
import free.yhc.netmbuddy.db.DBHistory.FieldNType;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.utils.Utils;

class DBManager {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(DBManager.class);

    private static final String sTableAndroidMetadata = "android_metadata";
    private static final String sStmtAndroidMetadata = "CREATE TABLE android_metadata (locale TEXT)";

    private static final Pattern sPFieldDef     = Pattern.compile("[^(]+\\((.+)\\)\\s*");
    private static final Pattern sPTokenGroup   = Pattern.compile("(\\s*([^,]+)).*");
    private static final Pattern sPFieldNType   = Pattern.compile("([^\\s]+)\\s+([^\\s]+).*");

    private static final String[] sFieldNameNotAllowed = new String[] {
        "foreign",
        "key",
        "select",
        "from",
    };


    private static HashMap<String, String>
    extractFieldAndType(String schemaString) {
        HashMap<String, String> map = new HashMap<String, String>();

        Matcher m = sPFieldDef.matcher(schemaString);
        String str;
        if (m.matches())
            str = m.group(1);
        else
            return map;

        while (true) {
            m = sPTokenGroup.matcher(str);
            if (!m.matches())
                break;

            String group = m.group(1);
            String token = m.group(2);

            m = sPFieldNType.matcher(token);
            if (!m.matches())
                break;

            if (2 != m.groupCount())
                break;

            // group(1) is field, group(2) is type.
            String field = m.group(1);
            String type = m.group(2);

            boolean allowed = true;
            for (String s : sFieldNameNotAllowed) {
                if (s.equalsIgnoreCase(field)) {
                    allowed = false;
                    break;
                }
            }

            if (allowed)
                // ignore field that is not allowed.
                map.put(field, type);

            // update 'str' to move to next token group.
            str = str.replace(group, "");
            if (!str.startsWith(","))
                break; // end of token.
            str = str.substring(1); // remove leading ','
        }
        return map;
    }

    private static Err
    copy(File fDst, File fSrc) {
        Err err = Err.NO_ERR;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(fSrc);
            fos = new FileOutputStream(fDst);
            Utils.copy(fos, fis);
        } catch (FileNotFoundException e) {
            err = Err.IO_FILE;
        } catch (InterruptedException e) {
            // Unexpected interrupt!!
            err = Err.INTERRUPTED;
        } catch (IOException e) {
            err = Err.IO_FILE;
        } finally {
            try {
                if (null != fis)
                    fis.close();
                if (null != fos)
                    fos.close();
            } catch (IOException ignored) { }
        }
        return err;
    }

    // NOTE
    // [ Algorithm ]
    // Condition to pass
    // - android_metadata, playlist and video table exist at DB
    // - fields and it's types for playlist and video table, matches exactly with DB history (see above)
    //
    // I think this is enough to verify DB...
    private static Err
    verifyDB(SQLiteDatabase db) {
        if (db.getVersion() < 0
            || DB.getVersion() < db.getVersion())
            return Err.INVALID_DB; // trivial case

        Cursor c = db.query("sqlite_master",
                            new String[] {"name", "sql"},
                            "type = 'table'",
                            null, null, null, null);

        HashMap<String, String> map = new HashMap<String, String>();
        if (c.moveToFirst()) {
            do {
                // Key : table name, Value : sql text
                map.put(c.getString(0), c.getString(1));
            } while (c.moveToNext());
        }
        c.close();

        // Verify
        // Check android metadata.
        String stmt = map.get(sTableAndroidMetadata);
        if (null == stmt || !stmt.equals(sStmtAndroidMetadata))
            return Err.INVALID_DB;

        // field and type map.
        // DB version starts from 1. And array index starts from 0.
        int dbVersion = db.getVersion();
        FieldNType[][] ftHistory = DBHistory.sFieldNType[dbVersion - 1];
        eAssert(DBHistory.sTables.length == ftHistory.length);
        for (int i = 0; i < DBHistory.sTables.length; i++) {
            stmt = map.get(DBHistory.sTables[i]);
            if (null == stmt)
                return Err.INVALID_DB;

            HashMap<String, String> ftmap = extractFieldAndType(stmt);
            if (ftmap.size() != ftHistory[i].length)
                return Err.INVALID_DB;

            for (FieldNType ft : ftHistory[i]) {
                String type = ftmap.get(ft.field);
                if (null == type
                    || !type.equalsIgnoreCase(ft.type))
                    return Err.INVALID_DB;
            }
        }
        return Err.NO_ERR;
    }


    private static Err
    verifyExternalDBFile(File exDbf) {
        Err err = Err.INVALID_DB;
        try {
            if (!exDbf.canRead())
                return Err.IO_FILE;

            SQLiteDatabase exDb = null;
            try {
                exDb = SQLiteDatabase.openDatabase(exDbf.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            } catch (SQLiteException e) {
                return Err.INVALID_DB;
            }

            err = verifyDB(exDb);
            exDb.close();
        } catch (Exception e) {
            err = Err.INVALID_DB;
        }
        return err;
    }

    private static Err
    copyAndUpgrade(File tempExDb, File exDbf) {
        Err err = copy(tempExDb, exDbf);
        if (Err.NO_ERR != err) {
            tempExDb.delete();
            return err;
        }

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(tempExDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            if (!new DBUpgrader(db, db.getVersion(), DB.getVersion()).upgrade())
                return Err.INVALID_DB;
        } catch (SQLiteException e) {
            return Err.INVALID_DB;
        } finally {
            if (null != db)
                db.close();
        }

        return Err.NO_ERR;
    }

    /**
     * Extremely critical function.
     * PREREQUISITE
     *   All operations that might access DB, SHOULD BE STOPPED
     *     before importing DB.
     *   And that operation should be resumed after importing DB.
     * @param exDbf
     */
    static Err
    importDatabase(File exDbf) {
        Err err = verifyExternalDBFile(exDbf);
        if (err != Err.NO_ERR)
            return err;

        // External DB is verified.
        // Let's do real importing.
        DB.get().close();
        try {
            File inDbf = Utils.getAppContext().getDatabasePath(DB.getName());
            File inDbfBackup = new File(inDbf.getAbsolutePath() + "____backup");

            if (!inDbf.renameTo(inDbfBackup)) {
                DB.get().open();
                return Err.IO_FILE;
            }

            err = copy(inDbf, exDbf);
            if (Err.NO_ERR != err) {
                // Restore it
                inDbf.delete();
                inDbfBackup.renameTo(inDbf);
            } else {
                inDbfBackup.delete();
            }
        } finally {
            // Open imported new DB
            DB.get().open();
        }
        return err;
    }


    private static Err
    doMergeDatabase(SQLiteDatabase exDb) {
        DB db = DB.get();
        Cursor excPl = exDb.query(DB.getPlaylistTableName(),
                                  DBUtils.getColNames(ColPlaylist.values()),
                                  null, null, null, null, null);
        if (!excPl.moveToFirst()) {
            // Empty DB.
            // So, nothing to merge!
            excPl.close();
            return Err.NO_ERR;
        }

        final int plColiTitle = excPl.getColumnIndex(ColPlaylist.TITLE.getName());
        final int plColiId = excPl.getColumnIndex(ColPlaylist.ID.getName());
        do {
            int i = 0;
            String plTitle = excPl.getString(plColiTitle);
            while (db.containsPlaylist(plTitle)) {
                i++;
                plTitle = excPl.getString(plColiTitle)+ "_" + Utils.getResText(R.string.merge) + i;
            }

            // Playlist title is chosen.
            ContentValues cvs = DBUtils.copyContent(excPl, ColPlaylist.values());
            cvs.put(ColPlaylist.TITLE.getName(), plTitle);
            cvs.put(ColPlaylist.SIZE.getName(), 0);
            long inPlid = db.insertPlaylist(cvs);

            // Scan all video references belongs to this playlist
            Cursor excVref = exDb.query(DB.getVideoRefTableName(excPl.getLong(plColiId)),
                    DBUtils.getColNames(new ColVideoRef[] { ColVideoRef.VIDEOID }),
                          null, null, null, null, null);

            if (!excVref.moveToFirst()) {
                // Empty playlist! Let's move to next.
                excVref.close();
                continue;
            }

            do {
                // get Youtube video id string of external database.
                Cursor excV = exDb.query(DB.getVideoTableName(),
                                         DBUtils.getColNames(ColVideo.values()),
                                         ColVideo.ID.getName() + " = " + excVref.getLong(0),
                                         null, null, null, null);
                if (!excV.moveToFirst())
                    eAssert(false);

                final int vColiVid = excV.getColumnIndex(ColVideo.VIDEOID.getName());

                Long vid = (Long)db.getVideoInfo(excV.getString(vColiVid), ColVideo.ID);
                if (null == vid) {
                    // This is new video!
                    cvs = DBUtils.copyContent(excV, ColVideo.values());
                    cvs.put(ColVideo.REFCOUNT.getName(), 0);
                    vid = db.insertVideo(cvs);
                }
                db.insertVideoRef(inPlid, vid);

            } while (excVref.moveToNext());
            excVref.close();
        } while (excPl.moveToNext());
        excPl.close();

        return Err.NO_ERR;
    }

    /**
     * Extremely critical function.
     * PREREQUISITE
     *   All operations that might access DB, SHOULD BE STOPPED
     *     before importing DB.
     *   And that operation should be resumed after importing DB.
     * @param exDbf
     */
    static Err
    mergeDatabase(File exDbf) {
        Err err = verifyExternalDBFile(exDbf);
        if (err != Err.NO_ERR)
            return err;

        File fTmp = null;
        try {
            fTmp = File.createTempFile("mergeDBTempFile", null, new File(Policy.APPDATA_TMPDIR));
            err = copyAndUpgrade(fTmp, exDbf);
            exDbf = fTmp;
        } catch (IOException e) {
            err = Err.IO_FILE;
        } finally {
            if (Err.NO_ERR != err) {
                if (null != fTmp)
                    fTmp.delete();
                return err;
            }
        }

        // Now exDbf is temporally-created-file.
        if (null == exDbf)
            return Err.IO_FILE;

        try {
            SQLiteDatabase exDb = null;
            try {
                exDb = SQLiteDatabase.openDatabase(exDbf.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            } catch (SQLiteException e) {
                return Err.INVALID_DB;
            }

            DB db = DB.get();
            // Merging Algorithm
            // -----------------
            //
            // * Merge playlist and reference tables
            //   : [if] there is duplicated playlist
            //     retry again and again with modified name - ex. title_#_
            //
            // * For each merged playlist, merge reference tables
            //   : scan vidoes
            //     - [if] there is duplicated video - based on Youtube video ID - in the DB
            //         => reference to the video's DB ID (reference count of this video should be increased.)
            //       [else] add the video to the current DB and reference to it.

            // Merging SHOULD BE ONE-TRANSACTION!
            db.beginTransaction();
            try {
                err = doMergeDatabase(exDb);
                if (Err.NO_ERR != err)
                    return err;

                db.setTransactionSuccessful();
            } finally {
                exDb.close();
                db.endTransaction();
            }
        } finally {
            exDbf.delete();
        }

        return Err.NO_ERR;
    }

    static Err
    exportDatabase(File exDbf) {
        Err err = Err.NO_ERR;

        DB.get().close();

        File inDbf = Utils.getAppContext().getDatabasePath(DB.getName());
        try {
            FileInputStream fis = new FileInputStream(inDbf);
            FileOutputStream fos = new FileOutputStream(exDbf);
            Utils.copy(fos, fis);
            fis.close();
            fos.close();
        } catch (FileNotFoundException e) {
            err = Err.IO_FILE;
        } catch (InterruptedException e) {
            // Unexpected interrupt!!
            err = Err.UNKNOWN;
        } catch (IOException e) {
            err = Err.IO_FILE;
        } finally {
            if (Err.NO_ERR != err) {
                exDbf.delete();
                return err;
            }
        }

        DB.get().open(); // open again.
        return Err.NO_ERR;
    }
}
