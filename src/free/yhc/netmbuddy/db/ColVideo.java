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
import android.provider.BaseColumns;
import free.yhc.netmbuddy.model.Policy;

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

        ID              (BaseColumns._ID,   "integer",  null,   "primary key autoincrement");

        private final String _mName;
        private final String _mType;
        private final String _mConstraint;
        private final String _mDefault;

        static ContentValues
        createContentValuesForInsert(String title, String videoId,
                                     int playtime, String author,
                                     byte[] thumbnail, int volume) {
            eAssert(null != title && null != videoId);
            if (null == thumbnail)
                thumbnail = new byte[0];

            ContentValues cvs = new ContentValues();
            cvs.put(TITLE.getName(), title);
            cvs.put(DESCRIPTION.getName(), ""); // not used yet.
            cvs.put(VIDEOID.getName(), videoId);
            cvs.put(PLAYTIME.getName(), playtime);
            cvs.put(THUMBNAIL.getName(), thumbnail);

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
