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

package free.yhc.netmbuddy.db;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.BaseColumns;
import free.yhc.netmbuddy.db.DB.Col;
import free.yhc.netmbuddy.utils.Utils;

class DBUtils {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(DBUtils.class);

    /**
     * Convert Col[] to string[] of column's name
     * @param cols
     * @return
     */
    static String[]
    getColNames(Col[] cols) {
        String[] strs = new String[cols.length];
        for (int i = 0; i < cols.length; i++)
            strs[i] = cols[i].getName();
        return strs;
    }

    static ContentValues
    copyContent(Cursor c, DB.Col[] cols) {
        ContentValues cvs = new ContentValues();
        for (Col col : cols) {
            if (BaseColumns._ID.equals(col.getName()))
                    continue; // ID SHOULD NOT be copied.

            if ("text".equals(col.getType())) {
                cvs.put(col.getName(), c.getString(c.getColumnIndex(col.getName())));
            } else if ("integer".equals(col.getType())) {
                cvs.put(col.getName(), c.getLong(c.getColumnIndex(col.getName())));
            } else if ("blob".equals(col.getType())) {
                cvs.put(col.getName(), c.getBlob(c.getColumnIndex(col.getName())));
            } else
                eAssert(false);
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
     * @param table
     *   name of table
     * @param cols
     *   columns of table.
     * @return
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
     * @param plid
     * @param cols
     * @param field
     *   for "WHERE 'field' = 'value'"
     * @param value
     *   for "WHERE 'field' = 'value'"
     * @param colOrderBy
     * @param asc
     * @return
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

    static Object
    getCursorVal(Cursor c, Col col) {
        int i = c.getColumnIndex(col.getName());
        if ("text".equals(col.getType()))
            return c.getString(i);
        else if ("integer".equals(col.getType()))
            return c.getLong(i);
        else if ("blob".equals(col.getType()))
            return c.getBlob(i);
        else
            return null;
    }
}
