package free.yhc.netmbuddy.db;


class DBHistory {
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
