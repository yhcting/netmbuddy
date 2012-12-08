package free.yhc.netmbuddy.db;

/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
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
