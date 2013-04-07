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

    // FieldNTypes
    // To avoid creating duplicated object
    // ----------------------------------------------------------------------------------------------------------------
    // Version 1
    // ----------------------------------------------------------------------------------------------------------------
    private static FieldNType sFnT_idI =            new FieldNType("_id",             "integer");
    private static FieldNType sFnTTitleT =          new FieldNType("title",           "text");
    private static FieldNType sFnTDescriptionT =    new FieldNType("description",     "text");
    private static FieldNType sFnTThumbnailB =      new FieldNType("thumbnail",       "blob");
    private static FieldNType sFnTSizeI =           new FieldNType("size",            "integer");
    private static FieldNType sFnTVideoidT =        new FieldNType("videoid",         "text");
    private static FieldNType sFnTGenreT =          new FieldNType("genre",           "text");
    private static FieldNType sFnTArtistT =         new FieldNType("artist",          "text");
    private static FieldNType sFnTAlbumT =          new FieldNType("album",           "text");
    private static FieldNType sFnTPlaytimeI =       new FieldNType("playtime",        "integer");
    private static FieldNType sFnTVolumeI =         new FieldNType("volume",          "integer");
    private static FieldNType sFnTRateI =           new FieldNType("rate",            "integer");
    private static FieldNType sFnTTime_addI =       new FieldNType("time_add",        "integer");
    private static FieldNType sFnTTime_playedI =    new FieldNType("time_played",     "integer");
    private static FieldNType sFnTRefcountI =       new FieldNType("refcount",        "integer");

    // ----------------------------------------------------------------------------------------------------------------
    // Newly added at version 2
    // ----------------------------------------------------------------------------------------------------------------
    private static FieldNType sFnTThumbnail_vidT =  new FieldNType("thumbnail_vid",   "text");
    private static FieldNType sFnTAuthorT =         new FieldNType("author",          "text");
    private static FieldNType sFnTNrplayedI =       new FieldNType("nrplayed",        "integer");
    private static FieldNType sFnTRelvideosfeedT =  new FieldNType("relvideosfeed",   "text");
    // Adding reserved fields was my BIG MISTAKE :-(
    private static FieldNType sFnTReserved0T =      new FieldNType("reserved0",       "text");
    private static FieldNType sFnTReserved1T =      new FieldNType("reserved1",       "text");
    private static FieldNType sFnTReserved2I =      new FieldNType("reserved2",       "integer");
    private static FieldNType sFnTReserved2T =      new FieldNType("reserved2",       "text");
    private static FieldNType sFnTReserved3I =      new FieldNType("reserved3",       "integer");
    private static FieldNType sFnTReserved4B =      new FieldNType("reserved4",       "blob");
    private static FieldNType sFnTReserved4I =      new FieldNType("reserved4",       "integer");
    private static FieldNType sFnTReserved5I =      new FieldNType("reserved5",       "integer");
    private static FieldNType sFnTReserved6B =      new FieldNType("reserved6",       "blob");

    // ----------------------------------------------------------------------------------------------------------------
    // Newly added at version 3
    // ----------------------------------------------------------------------------------------------------------------
    private static FieldNType sFnTBookmarksT =      new FieldNType("bookmarks",       "text");


    // [3Dim][2Dim][1Dim]
    // 1st dimension : FieldNType lists
    // 2nd dimension : table type : Order should match sTables.
    // 3rd dimension : version of DB.
    static final FieldNType[][][] sFieldNType = {
        // DB version 1
        {
            // Playlist table
            {
                sFnTTitleT,
                sFnTDescriptionT,
                sFnTThumbnailB,
                sFnTSizeI,
                sFnT_idI,
            },

            // Video table
            {
                sFnTTitleT,
                sFnTDescriptionT,
                sFnTVideoidT,
                sFnTGenreT,
                sFnTArtistT,
                sFnTAlbumT,
                sFnTThumbnailB,
                sFnTPlaytimeI,
                sFnTVolumeI,
                sFnTRateI,
                sFnTTime_addI,
                sFnTTime_playedI,
                sFnTRefcountI,
                sFnT_idI,
            }
        },

        // DB version 2
        {
            // Playlist table
            {
                sFnTTitleT,
                sFnTDescriptionT,
                sFnTThumbnailB,
                sFnTSizeI,
                sFnT_idI,


                // Below fields are newly added.
                sFnTThumbnail_vidT,
                sFnTReserved0T,
                sFnTReserved1T,
                sFnTReserved2I,
                sFnTReserved3I,
                sFnTReserved4B,
            },

            // Video table
            {
                sFnTTitleT,
                sFnTDescriptionT,
                sFnTVideoidT,
                sFnTGenreT,
                sFnTArtistT,
                sFnTAlbumT,
                sFnTThumbnailB,
                sFnTPlaytimeI,
                sFnTVolumeI,
                sFnTRateI,
                sFnTTime_addI,
                sFnTTime_playedI,
                sFnTRefcountI,
                sFnT_idI,

                // Below fields are newly added.
                sFnTAuthorT,
                sFnTNrplayedI,
                sFnTRelvideosfeedT,
                sFnTReserved0T,
                sFnTReserved1T,
                sFnTReserved2T,
                sFnTReserved3I,
                sFnTReserved4I,
                sFnTReserved5I,
                sFnTReserved6B,
            }
        },

        // DB version 3
        {
            // Playlist table
            {
                sFnTTitleT,
                sFnTDescriptionT,
                sFnTThumbnailB,
                sFnTSizeI,
                sFnT_idI,
                sFnTThumbnail_vidT,
                sFnTReserved0T,
                sFnTReserved1T,
                sFnTReserved2I,
                sFnTReserved3I,
                sFnTReserved4B,
            },

            // Video table
            {
                sFnTTitleT,
                sFnTDescriptionT,
                sFnTVideoidT,
                sFnTGenreT,
                sFnTArtistT,
                sFnTAlbumT,
                sFnTThumbnailB,
                sFnTPlaytimeI,
                sFnTVolumeI,
                sFnTRateI,
                sFnTTime_addI,
                sFnTTime_playedI,
                sFnTRefcountI,
                sFnT_idI,
                sFnTAuthorT,
                sFnTNrplayedI,
                sFnTRelvideosfeedT,
                sFnTReserved0T,
                sFnTReserved1T,
                sFnTReserved2T,
                sFnTReserved3I,
                sFnTReserved4I,
                sFnTReserved5I,
                sFnTReserved6B,

                // Below field are newly added
                sFnTBookmarksT,
            }
        }
    };

}
