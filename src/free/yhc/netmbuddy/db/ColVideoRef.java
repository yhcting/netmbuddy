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

import android.provider.BaseColumns;

//NOTE
//This is just video list
//To get detail and sorted table, 'Joining Table' should be used.
public enum ColVideoRef implements DB.Col {
        // primary key - BaseColumns._ID of TABLE_VIDEO table
        VIDEOID         ("videoid",         "integer",  null,   ""),
        ID              (BaseColumns._ID,   "integer",  null,   "primary key autoincrement, "
                + "FOREIGN KEY(videoid) REFERENCES " + DB.getVideoTableName() + "(" + ColVideo.ID.getName() + ")");

        private final String _mName;
        private final String _mType;
        private final String _mConstraint;
        private final String _mDefault;

        ColVideoRef(String name, String type, String defaultv, String constraint) {
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

