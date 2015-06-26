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

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.content.ContentValues;
import android.provider.BaseColumns;
import free.yhc.netmbuddy.core.Policy;

public enum ColVideo implements DB.Col {
        // --------------------------------------------------------------------
        // Youtube information
        // --------------------------------------------------------------------
        TITLE           ("title",           "text",     null,   "not null"),
        DESCRIPTION     ("description",     "text",     null,   "not null"), // Not used yet.
        VIDEOID         ("videoid",         "text",     null,   "not null"), // Youtube Video Id (11-characters)
        PLAYTIME        ("playtime",        "integer",  null,   "not null"), // Seconds (int)
        THUMBNAIL       ("thumbnail",       "blob",     null,   "not null"),

        // --------------------------------------------------------------------
        // Custom information
        // --------------------------------------------------------------------
        // Why volume is here?
        // Each Youtube video has it's own volume that is set at encoding step.
        // So, even if device volume setting is not changed, some video
        //   plays with loud sound but others are not.
        // To tune this variance between videos this field is required.
        VOLUME          ("volume",          "integer",  null,   "not null"),
        RATE            ("rate",            "integer",  null,   "not null"), // my rate of this Video - Not used yet
        TIME_ADD        ("time_add",        "integer",  null,   "not null"), // time video is added to DB.
        TIME_PLAYED     ("time_played",     "integer",  null,   "not_null"), // time last played

        // --------------------------------------------------------------------
        // Video information - Not used yet (reserved for future use)
        // --------------------------------------------------------------------
        GENRE           ("genre",           "text",     null,   "not null"), // Not used yet
        ARTIST          ("artist",          "text",     null,   "not null"), // Not used yet
        ALBUM           ("album",           "text",     null,   "not null"), // Not used yet

        // --------------------------------------------------------------------
        // Internal use for DB management
        // --------------------------------------------------------------------
        REFCOUNT        ("refcount",        "integer",  null,   "not null"), // reference count

        // --------------------------------------------------------------------
        // newly added at DB version 2
        // --------------------------------------------------------------------
        AUTHOR          ("author",          "text",     "\"\"", ""), // YTFeed.Author.name
        // Belows are not used yet.
        NRPLAYED        ("nrplayed",        "integer",  "0",    ""), // # of played
        REL_VIDEOS_FEED ("relvideosfeed",   "text",     "\"\"", ""), // feeds for relative videos.
        // Reserved fields for future use
        // ENUM name can be changed without affecting DB
        RESERVED0       ("reserved0",       "text",     "\"\"", ""),
        RESERVED1       ("reserved1",       "text",     "\"\"", ""),
        RESERVED2       ("reserved2",       "text",     "\"\"", ""),
        RESERVED3       ("reserved3",       "integer",  "0",    ""),
        RESERVED4       ("reserved4",       "integer",  "0",    ""),
        RESERVED5       ("reserved5",       "integer",  "0",    ""),
        RESERVED6       ("reserved6",       "blob",     "\"\"", ""),

        // --------------------------------------------------------------------
        // newly added at DB version 3
        // --------------------------------------------------------------------
        // Reserved column is NOT used here...
        // Because, it's very difficult to maintain DB itself.
        // Adding column is not expensive operation.
        // In consequence, "Adding reserved fields" was BIG MISTAKE :-(
        //
        // Delimiter between <time> and <bookmark name> : /
        // Delimiter between bookmarks : @
        // [ Format ]
        // bookmark : <time(ms)>/<bookmark name>
        // bookmarks : <bookmark>@<bookmark>@...
        BOOKMARKS       ("bookmarks",       "text",     "\"\"", ""),

        ID              (BaseColumns._ID,   "integer",  null,   "primary key autoincrement");

        private final String _mName;
        private final String _mType;
        private final String _mConstraint;
        private final String _mDefault;

        static ContentValues
        createContentValuesForInsert(String title, String videoId,
                                     int playtime, String author,
                                     byte[] thumbnail, int volume,
                                     String bookmarks) {
            eAssert(null != title && null != videoId);
            if (null == thumbnail)
                thumbnail = new byte[0];

            ContentValues cvs = new ContentValues();
            cvs.put(TITLE.getName(), title);
            cvs.put(DESCRIPTION.getName(), ""); // not used yet.
            cvs.put(VIDEOID.getName(), videoId);
            cvs.put(PLAYTIME.getName(), playtime);
            cvs.put(THUMBNAIL.getName(), thumbnail);
            cvs.put(BOOKMARKS.getName(), bookmarks);

            if (DB.INVALID_VOLUME == volume)
                volume = Policy.DEFAULT_VIDEO_VOLUME;
            cvs.put(VOLUME.getName(), volume);

            cvs.put(RATE.getName(), 0);
            cvs.put(TIME_ADD.getName(), System.currentTimeMillis());
            // Set to oldest value when first added because it is never played yet.
            cvs.put(TIME_PLAYED.getName(), 0);

            cvs.put(GENRE.getName(), "");  // not used yet.
            cvs.put(ARTIST.getName(), ""); // not used yet.
            cvs.put(ALBUM.getName(), "");  // not used yet.

            cvs.put(REFCOUNT.getName(), 0);

            // --------------------------------------------------------------------
            // newly added at DB version 2 (Those have default values.);
            // So, we don't need to describe values explicitly here.
            // --------------------------------------------------------------------
            cvs.put(AUTHOR.getName(), author);

            return cvs;
        }

        ColVideo(String aName, String aType, String defaultv, String aConstraint) {
            _mName = aName;
            _mType = aType;
            _mConstraint = aConstraint;
            _mDefault = defaultv;
        }
        @Override
        public String getName() { return _mName; }
        @Override
        public String getType() { return _mType; }
        @Override
        public String getConstraint() { return _mConstraint; }
        @Override
        public String getDefault() { return _mDefault; }

    }
