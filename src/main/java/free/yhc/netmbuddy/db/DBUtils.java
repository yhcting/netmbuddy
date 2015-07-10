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
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import free.yhc.netmbuddy.db.DB.Col;
import free.yhc.netmbuddy.utils.Utils;

public class DBUtils {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(DBUtils.class);

    /**
     * Convert Col[] to string[] of column's name
     */
    static String[]
    getColNames(Col[] cols) {
        String[] strs = new String[cols.length];
        for (int i = 0; i < cols.length; i++)
            strs[i] = cols[i].getName();
        return strs;
    }

    static void
    putCvsValue(ContentValues cvs, Col col, Object value) {
        switch (col.getType()) {
        case "text":
            cvs.put(col.getName(), (String)value);
            break;
        case "integer":
            cvs.put(col.getName(), (Long)value);
            break;
        case "blob":
            cvs.put(col.getName(), (byte[])value);
            break;
        default:
            eAssert(false);
        }
    }

    static ContentValues
    copyContent(Cursor c, DB.Col[] cols) {
        ContentValues cvs = new ContentValues();
        for (Col col : cols) {
            if (BaseColumns._ID.equals(col.getName()))
                    continue; // ID SHOULD NOT be copied.
            putCvsValue(cvs, col, c.getColumnIndex(col.getName()));
        }
        return cvs;
    }

    // ========================================================================
    //
    //
    //
    // ========================================================================
    static String
    buildColumnDef(DB.Col col) {
        String defaultv = col.getDefault();
        if (null == defaultv)
            defaultv = "";
        else
            defaultv = " DEFAULT " + defaultv;

        String constraint = col.getConstraint();
        if (null == constraint)
            constraint = "";
        return col.getName() + " "
               + col.getType() + " "
               + defaultv + " "
               + constraint;
    }

    /**
     * Get SQL statement for creating table
     * @param table name of table
     * @param cols columns of table.
     */
    static String
    buildTableSQL(String table, DB.Col[] cols) {
        String sql = "CREATE TABLE " + table + " (";
        for (Col col : cols)
            sql += buildColumnDef(col) + ", ";
        sql += ");";
        sql = sql.replace(", );", ");");
        return sql;
    }

    static String
    buildSQLOrderBy(boolean withStatement, DB.Col col, boolean asc) {
        if (null == col)
            return null;
        return (withStatement? "ORDER BY ": "") + col.getName() + " " + (asc? "ASC": "DESC");
    }

    /**
     * Build SQL from joining video and video-ref tables
     * @param field for "WHERE 'field' = 'value'"
     * @param value for "WHERE 'field' = 'value'"
     * @param asc true for ascending order
     */
    static String
    buildQueryVideosSQL(long plid, ColVideo[] cols,
                        ColVideo field, Object value,
                        ColVideo colOrderBy, boolean asc) {
        eAssert(cols.length > 0);

        String sql = "SELECT ";
        String sel = "";
        String tableVideoNS = DB.getVideoTableName() + "."; // NS : NameSpace
        String[] cnames = getColNames(cols);
        for (int i = 0; i < cnames.length - 1; i++)
            sel += tableVideoNS + cnames[i] + ", ";
        sel += tableVideoNS + cnames[cnames.length - 1];

        String where = "";
        if (null != field && null != value)
            where = " AND "
                    + tableVideoNS + field.getName() + " = "
                    + DatabaseUtils.sqlEscapeString(value.toString());

        String orderBy = buildSQLOrderBy(true, colOrderBy, asc);
        // NOTE
        // There is NO USE CASE requiring sorted cursor for videos.
        // result of querying videos don't need to be sorted cursor.
        String mrefTable = DB.getVideoRefTableName(plid);
        sql += sel + " FROM " + DB.getVideoTableName() + ", " + mrefTable
                + " WHERE " + mrefTable + "." + ColVideoRef.VIDEOID.getName()
                            + " = " + tableVideoNS + ColVideo.ID.getName()
                + where
                + " " + (null != orderBy? orderBy: "")
                + ";";
        return sql;
    }

    static String
    buildCopyColumnsSQL(String dstTable, String srcTable, String[] cols) {
        if (0 == cols.length)
            return ";"; // nothing to do
        String sql = "INSERT INTO " + dstTable + " SELECT ";
        for (int i = 0; i < cols.length - 1; i++)
            sql += cols[i] + ",";
        sql += cols[cols.length - 1];
        sql += " FROM " + srcTable + ";";
        return sql;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // For Bookmarks
    // ----------------------------------------------------------------------------------------------------------------
    /**
     * @param bmstr empty string is NOT allowed.
     * @return null for invalid bookmark string.
     */
    @Nullable
    private static DB.Bookmark
    decodeBookmark(String bmstr) {
        int i = bmstr.indexOf(DB.BOOKMARK_NAME_DELIMIETER);

        if (-1 == i)
            return null; // invalid bookmark string

        String posstr = bmstr.substring(0, i);
        String name = bmstr.substring(i + 1);

        // sanity check
        int pos;
        try {
            pos = Integer.parseInt(posstr);
        } catch (NumberFormatException e) {
            return null; // invalid bookmark string
        }

        if (name.isEmpty()
            || name.contains("" + DB.BOOKMARK_DELIMITER))
            return null; // invalid bookmark string

        return new DB.Bookmark(name, pos);
    }

    private static String
    encodeBookmark(DB.Bookmark bm) {
        // NOTE : Check strictly to keep DB safe!!!
        eAssert(bm.pos > 0
                && Utils.isValidValue(bm.name));
        return ((Integer)bm.pos).toString() // to avoid implicit casting to 'char' type,
                                            //   because following DB.BOOKMARK_NAME_DELIMIETER is 'char'.
               + DB.BOOKMARK_NAME_DELIMIETER
               + bm.name;
    }

    static boolean
    isValidBookmarksString(String bmsstr) {
        return null != bmsstr
               && null != decodeBookmarks(bmsstr);
    }
    /**
     *
     * @return null for invalid bookmarks string.
     */
    @Nullable
    static DB.Bookmark[]
    decodeBookmarks(String bmsstr) {
        if (null == bmsstr)
            return null;

        if (bmsstr.isEmpty())
            return new DB.Bookmark[0];
        String[] bmarr = bmsstr.split("" + DB.BOOKMARK_DELIMITER);
        DB.Bookmark[] bms = new DB.Bookmark[bmarr.length];
        for (int i = 0; i < bms.length; i++) {
            bms[i] = decodeBookmark(bmarr[i]);
            if (null == bms[i])
                return null; // error!
        }
        return bms;
    }

    static String
    encodeBookmarks(DB.Bookmark[] bms) {
        if (0 == bms.length)
            return "";

        String s = "";
        int i;
        for (i = 0; i < bms.length - 1; i++)
            s += encodeBookmark(bms[i]) + DB.BOOKMARK_DELIMITER;
        s += encodeBookmark(bms[i]);
        return s;
    }

    static String
    addBookmark(String bmsstr, DB.Bookmark bm) {
        if (!bmsstr.isEmpty())
            bmsstr += DB.BOOKMARK_DELIMITER;
        return bmsstr + encodeBookmark(bm);
    }

    /**
     * Delete first matching bookmark.
     * If there is more than one bookmark matching, only first one is deleted.
     */
    static String
    deleteBookmark(String bmsstr, DB.Bookmark bm) {
        DB.Bookmark[] bmarr = decodeBookmarks(bmsstr);
        if (null == bmarr
            || 0 == bmarr.length)
            return ""; // nothing to delete.
        DB.Bookmark[] newBmarr = new DB.Bookmark[bmarr.length - 1];
        // to avoid deleting more than one item.
        boolean deleted = false;
        int j = 0;
        for (DB.Bookmark b : bmarr) {
            if (deleted || !bm.equal(b))
                newBmarr[j++] = b;
            else
                deleted = true;
        }
        return encodeBookmarks(newBmarr);
    }

    // ========================================================================
    //
    // public
    //
    // ========================================================================
    @NonNull
    public static Object
    getCursorVal(Cursor c, Col col) {
        int i = c.getColumnIndex(col.getName());
        if ("text".equals(col.getType()))
            return c.getString(i);
        else if ("integer".equals(col.getType())) {
            return c.getLong(i);
        } else if ("blob".equals(col.getType()))
            return c.getBlob(i);
        else {
            eAssert(false);
            return null;
        }
    }

}
