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
import android.provider.BaseColumns;
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.utils.Utils;

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
        TIME_ADD        ("time_add",        "integer",  null,   "not null"), // time video is added to DB.
        TIME_PLAYED     ("time_played",     "integer",  null,   "not_null"), // time last played

        // --------------------------------------------------------------------
        // Internal use for DB management
        // --------------------------------------------------------------------
        REFCOUNT        ("refcount",        "integer",  null,   "not null"), // reference count

        // --------------------------------------------------------------------
        // newly added at DB version 2
        // --------------------------------------------------------------------
        // Belows are not used yet.
        NRPLAYED        ("nrplayed",        "integer",  "0",    ""), // # of played

        // --------------------------------------------------------------------
        // newly added at DB version 3
        // --------------------------------------------------------------------
        // Delimiter between <time> and <bookmark name> : /
        // Delimiter between bookmarks : @
        // [ Format ]
        // bookmark : <time(ms)>/<bookmark name>
        // bookmarks : <bookmark>@<bookmark>@...
        BOOKMARKS       ("bookmarks",       "text",     "\"\"", ""),

        // --------------------------------------------------------------------
        // Changes at DB version 4
        // --------------------------------------------------------------------
        // [ Removed ]
        // author, relvideosfeed, reservedN (See DBUpgrader)
        //
        // [ Added ]
        // channelId, channelTitle
        CHANNELID       ("channelid",       "text",     "\"\"", ""),
        CHANNELTITLE    ("channeltitle",    "text",     "\"\"", ""),

        ID              (BaseColumns._ID,   "integer",  null,   "primary key autoincrement");

        private final String _mName;
        private final String _mType;
        private final String _mConstraint;
        private final String _mDefault;

        static ContentValues
        createContentValuesForInsert(DMVideo v) {
            eAssert(Utils.isValidValue(v.ytvid)
                    && Utils.isValidValue(v.title));
            byte[] thumbnail = v.thumbnail;
            if (null == thumbnail)
                thumbnail = new byte[0];
            long volume = v.volume;
            if (DB.INVALID_VOLUME == volume)
                volume = Policy.DEFAULT_VIDEO_VOLUME;

            // Invalid bookmark string.
            // This is definitely unexpected, but it's not fatal error.
            // So, just ignore invalid bookmark!
            String bookmarks = v.bookmarks;
            if (!DBUtils.isValidBookmarksString(v.bookmarks))
                bookmarks = "";

            ContentValues cvs = new ContentValues();
            cvs.put(TITLE.getName(), v.title);
            cvs.put(DESCRIPTION.getName(), ""); // not used yet.
            cvs.put(VIDEOID.getName(), v.ytvid);
            cvs.put(PLAYTIME.getName(), v.playtime);

            // non-core data
            cvs.put(THUMBNAIL.getName(), thumbnail);
            cvs.put(BOOKMARKS.getName(), bookmarks);
            cvs.put(VOLUME.getName(), volume);

            // Additional info.
            cvs.put(TIME_ADD.getName(), System.currentTimeMillis());
            // Set to oldest value when first added because it is never played yet.
            cvs.put(TIME_PLAYED.getName(), 0);

            cvs.put(REFCOUNT.getName(), 0);

            // --------------------------------------------------------------------
            // newly added at DB version 4 (Those have default values.);
            // So, we don't need to describe values explicitly here.
            // --------------------------------------------------------------------
            cvs.put(CHANNELID.getName(), v.channelId);
            cvs.put(CHANNELTITLE.getName(), v.channelTitle);

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
