/******************************************************************************
 * Copyright (C) 2012, 2013, 2014
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
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(DBUpgrader.class);

    private final SQLiteDatabase    mDb;
    private final int               mOldVersion;
    private final int               mNewVersion;

    DBUpgrader(SQLiteDatabase db, int oldVersion, int newVersion) {
        mDb = db;
        mOldVersion = oldVersion;
        mNewVersion = newVersion;
    }

    private static String
    buildAddColumnSQL(String table, Col col) {
        return "ALTER TABLE " + table + " ADD COLUMN "
               + DBUtils.buildColumnDef(col)
               + ";";
    }

    private static void
    upgradeTo2(SQLiteDatabase db) {
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), ColPlaylist.THUMBNAIL_YTVID));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), ColPlaylist.RESERVED0));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), ColPlaylist.RESERVED1));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), ColPlaylist.RESERVED2));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), ColPlaylist.RESERVED3));
        db.execSQL(buildAddColumnSQL(DB.getPlaylistTableName(), ColPlaylist.RESERVED4));

        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.AUTHOR));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.NRPLAYED));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.REL_VIDEOS_FEED));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.RESERVED0));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.RESERVED1));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.RESERVED2));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.RESERVED3));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.RESERVED4));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.RESERVED5));
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.RESERVED6));
    }

    private static void
    upgradeTo3(SQLiteDatabase db) {
        db.execSQL(buildAddColumnSQL(DB.getVideoTableName(), ColVideo.BOOKMARKS));
    }

    boolean
    upgrade() {
        boolean success = true;
        int dbv = mOldVersion;
        mDb.beginTransaction();
        try {
            while (dbv < mNewVersion) {
                switch (dbv) {
                case 1:
                    upgradeTo2(mDb);
                    break;

                case 2:
                    upgradeTo3(mDb);
                    break;
                }
                dbv++;
            }
            mDb.setTransactionSuccessful();
        } catch (Exception e) {
            success = false;
        } finally {
            mDb.endTransaction();
        }
        return success;
    }
}
