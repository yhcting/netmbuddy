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

import free.yhc.netmbuddy.utils.Utils;

class DBHistory {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(DBHistory.class);

    static final String[] sTables = {
        DB.getPlaylistTableName(),  // playlist table at index 0
        DB.getVideoTableName()      // video table at index 1
    };

    static class FieldNType {
        String field;
        String type;
        FieldNType(String aField, String aType) {
            field = aField;
            type = aType;
        }
    }

    // [3Dim][2Dim][1Dim]
    // 1st dimension : FieldNType lists
    // 2nd dimension : table type : Order should match sMandatoryTables.
    // 3rd dimension : version of DB.
    static final FieldNType[][][] sFieldNType = {
        // DB version 1
        {
            // Playlist table
            {
                new FieldNType("title",           "text"),
                new FieldNType("description",     "text"),
                new FieldNType("thumbnail",       "blob"),
                new FieldNType("size",            "integer"),
                new FieldNType("_id",             "integer"),
            },

            // Video table
            {
                new FieldNType("title",           "text"),
                new FieldNType("description",     "text"),
                new FieldNType("videoid",         "text"),
                new FieldNType("genre",           "text"),
                new FieldNType("artist",          "text"),
                new FieldNType("album",           "text"),
                new FieldNType("thumbnail",       "blob"),
                new FieldNType("playtime",        "integer"),
                new FieldNType("volume",          "integer"),
                new FieldNType("rate",            "integer"),
                new FieldNType("time_add",        "integer"),
                new FieldNType("time_played",     "integer"),
                new FieldNType("refcount",        "integer"),
                new FieldNType("_id",             "integer"),
            }
        },

        // DB version 2
        {
            // Playlist table
            {
                new FieldNType("title",           "text"),
                new FieldNType("description",     "text"),
                new FieldNType("thumbnail",       "blob"),
                new FieldNType("size",            "integer"),
                new FieldNType("_id",             "integer"),


                // Below fields are newly added.
                new FieldNType("thumbnail_vid",   "text"),
                new FieldNType("reserved0",       "text"),
                new FieldNType("reserved1",       "text"),
                new FieldNType("reserved2",       "integer"),
                new FieldNType("reserved3",       "integer"),
                new FieldNType("reserved4",       "blob"),
            },

            // Video table
            {
                new FieldNType("title",           "text"),
                new FieldNType("description",     "text"),
                new FieldNType("videoid",         "text"),
                new FieldNType("genre",           "text"),
                new FieldNType("artist",          "text"),
                new FieldNType("album",           "text"),
                new FieldNType("thumbnail",       "blob"),
                new FieldNType("playtime",        "integer"),
                new FieldNType("volume",          "integer"),
                new FieldNType("rate",            "integer"),
                new FieldNType("time_add",        "integer"),
                new FieldNType("time_played",     "integer"),
                new FieldNType("refcount",        "integer"),
                new FieldNType("_id",             "integer"),


                // Below fields are newly added.
                new FieldNType("author",          "text"),
                new FieldNType("nrplayed",        "integer"),
                new FieldNType("relvideosfeed",   "text"),
                new FieldNType("reserved0",       "text"),
                new FieldNType("reserved1",       "text"),
                new FieldNType("reserved2",       "text"),
                new FieldNType("reserved3",       "integer"),
                new FieldNType("reserved4",       "integer"),
                new FieldNType("reserved5",       "integer"),
                new FieldNType("reserved6",       "blob"),
            }
        }
    };

}
