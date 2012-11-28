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

