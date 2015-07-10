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

public enum ColPlaylist implements DB.Col {
    TITLE           ("title",           "text",     null,   "not null"),
    // DESCRIPTION : Not used yet - reserved for future use.
    DESCRIPTION     ("description",     "text",     null,   "not null"),
    THUMBNAIL       ("thumbnail",       "blob",     null,   "not null"),

    // # of videos in this playlist.
    SIZE            ("size",            "integer",  null,   "not null"),

    // --------------------------------------------------------------------
    // newly added at DB version 2
    // --------------------------------------------------------------------
    // Youtube video id for thumbnail.
    THUMBNAIL_YTVID ("thumbnail_vid",   "text",     "\"\"", ""),

    // --------------------------------------------------------------------
    // Changes at DB version 4
    // --------------------------------------------------------------------
    // [ Removed ]
    // reservedN (See DBUpgrader)
    //


    ID              (BaseColumns._ID,   "integer",  null,   "primary key autoincrement");

    private final String _mName;
    private final String _mType;
    private final String _mConstraint;
    private final String _mDefault;

    static ContentValues
    createContentValuesForInsert(String title) {
        eAssert(null != title);
        ContentValues cvs = new ContentValues();
        cvs.put(TITLE.getName(), title);
        cvs.put(DESCRIPTION.getName(), "");
        cvs.put(THUMBNAIL.getName(), new byte[0]);
        cvs.put(THUMBNAIL_YTVID.getName(), "");
        cvs.put(SIZE.getName(), 0);

        // --------------------------------------------------------------------
        // newly added at DB version 2 (Those have default values.);
        // So, we don't need to describe values explicitly here.
        // --------------------------------------------------------------------
        return cvs;
    }

    ColPlaylist(String name, String type, String defaultv, String constraint) {
        _mName = name;
        _mType = type;
        _mConstraint = constraint;
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
