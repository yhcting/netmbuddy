package free.yhc.netmbuddy.db;

import android.database.sqlite.SQLiteDatabase;
import free.yhc.netmbuddy.db.DB.Col;

class DBUpgrader {
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
