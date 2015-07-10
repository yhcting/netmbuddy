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

import free.yhc.netmbuddy.utils.Utils;

class DBHistory {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
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

    // ----------------------------------------------------------------------------------------------------------------
    // Newly added at version 4
    // ----------------------------------------------------------------------------------------------------------------
    private static FieldNType sFnTChannelIdT =      new FieldNType("channelid",       "text");
    private static FieldNType sFnTChannelTitleT =   new FieldNType("channeltitle",    "text");

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
        },

        // DB version 4
        {
            // Playlist table
            {
                sFnTTitleT,
                sFnTDescriptionT,
                sFnTThumbnailB,
                sFnTSizeI,
                sFnT_idI,
                sFnTThumbnail_vidT,
            },

            // Video table
            {
                sFnTTitleT,
                sFnTDescriptionT,
                sFnTVideoidT,
                sFnTPlaytimeI,
                sFnTThumbnailB,
                sFnTVolumeI,
                sFnTTime_addI,
                sFnTTime_playedI,
                sFnTRefcountI,
                sFnTNrplayedI,
                sFnTBookmarksT,
                sFnT_idI,
                // Below field are newly added
                sFnTChannelIdT,
                sFnTChannelTitleT
            }
        }
    };
}
