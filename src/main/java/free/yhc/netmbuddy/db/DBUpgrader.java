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

import android.database.sqlite.SQLiteDatabase;
import free.yhc.netmbuddy.db.DB.Col;
import free.yhc.netmbuddy.utils.Utils;

class DBUpgrader {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(DBUpgrader.class);

    private final SQLiteDatabase mDb;
    private final int mOldVersion;
    private final int mNewVersion;

    DBUpgrader(SQLiteDatabase db, int oldVersion, int newVersion) {
        mDb = db;
        mOldVersion = oldVersion;
        mNewVersion = newVersion;
    }

    private static String
    buildAddColumnSQL(String table, String col) {
        return "ALTER TABLE " + table + " ADD COLUMN " + col + ";";
    }

    private static String
    buildAddColumnSQL(String table, Col col) {
        return buildAddColumnSQL(table, DBUtils.buildColumnDef(col));
    }

    private static void
    execDropColumns(SQLiteDatabase db ,
                    String table, String tempTable,
                    Col[] dbCols,
                    @SuppressWarnings("unused") String[] unusedCols) {
        // SQLITE doesn't support 'DROP COLUMN'.
        // This is heavy workaround.

        // create temp table.
        db.execSQL(DBUtils.buildTableSQL(tempTable, dbCols));

        // copy columns that should be preserved.
        String[] dbColNames = new String[dbCols.length];
        for (int i = 0; i < dbCols.length; i++)
            dbColNames[i] = dbCols[i].getName();
        db.execSQL(DBUtils.buildCopyColumnsSQL(tempTable, table, dbColNames));

        // Drop current table.
        db.execSQL("DROP TABLE " + table + ";");

        // Rename newly generated temp table into real table name
        db.execSQL("ALTER TABLE " + tempTable + " RENAME TO " + table + ";");
    }


    private static void
    upgradeTo2(SQLiteDatabase db) {
        // Reserved column is useless.
        // Because, it's very difficult to maintain DB itself.
        // Adding column is not expensive operation.
        // In consequence, "Adding reserved fields" was BIG MISTAKE :-(
        // So, reserved columns and other unused columns are removed at DB version 4.

        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), ColPlaylist.THUMBNAIL_YTVID));

        // Playlist - Columes dropped at later version.(hardcoded-name is used instead of enum)
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), "reserved0 text \"\""));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), "reserved1 text \"\""));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), "reserved2 integer 0"));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), "reserved3 integer 0"));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), "reserved4 blob \"\""));

        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.NRPLAYED));

        // Video - Columes dropped at later version.(hardcoded-name is used instead of enum)
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "author text \"\""));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "relvideosfeed text \"\""));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "reserved0 text \"\""));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "reserved1 text \"\""));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "reserved2 text \"\""));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "reserved3 integer 0"));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "reserved4 integer 0"));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "reserved5 integer 0"));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), "reserved6 blob \"\""));
    }

    private static void
    upgradeTo3(SQLiteDatabase db) {
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.BOOKMARKS));
    }

    private static void
    upgradeTo4(SQLiteDatabase db) {
        final String tempTableName = "____TEMP____";
        final String[] playlistDeprecatedColumns
            = { "reserved0", "reserved1", "reserved2", "reserved3", "reserved4" };
        final String[] videoDeprecatedColumns
            = { "author", "relvideosfeed", "genre", "artist", "album", "rate",
                "reserved0", "reserved1", "reserved2", "reserved3",
                "reserved4", "reserved5", "reserved6" };

        // Add new columns
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.CHANNELID));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.CHANNELTITLE));

        execDropColumns(db, DB.getPlaylistTableName(), tempTableName,
                        ColPlaylist.values(),  playlistDeprecatedColumns);
        execDropColumns(db, DB.getVideoTableName(), tempTableName,
                        ColVideo.values(), videoDeprecatedColumns);

    }

    void
    upgrade() {
        int dbv = mOldVersion;
        mDb.beginTransaction();
        try {
            while (dbv < mNewVersion) {
                switch (dbv) {
                case 1: upgradeTo2(mDb); break;
                case 2: upgradeTo3(mDb); break;
                case 3: upgradeTo4(mDb); break;
                }
                dbv++;
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
    }
}
