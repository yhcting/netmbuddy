package free.yhc.youtube.musicplayer.model;

import free.yhc.youtube.musicplayer.R;

public enum Err {
    NO_ERR                      (R.string.err_no_err),
    UNKNOWN                     (R.string.err_unknown),
    INTERRUPTED                 (R.string.err_interrupted),
    CANCELLED                   (R.string.err_cancelled),
    MULTITHREADING              (R.string.err_unknown),
    NETWORK_UNAVAILABLE         (R.string.err_network_unavailable),
    IO_NET                      (R.string.err_io_net),
    IO_FILE                     (R.string.err_io_file),
    IO_UNKNOWN                  (R.string.err_io_unknown),
    DB_DUPLICATED               (R.string.err_db_duplicated),
    DB_VERSION_MISMATCH         (R.string.err_db_version_mismatch),
    DB_NODATA                   (R.string.err_db_unknown),
    DB_INVALID                  (R.string.err_db_invalid),
    DB_UNKNOWN                  (R.string.err_db_unknown),
    INVALID_URL                 (R.string.err_invalid_url),
    PARSER_UNEXPECTED_FORMAT    (R.string.err_parser_unknown),
    PARSER_UNKNOWN              (R.string.err_parser_unknown),
    NO_MATCH                    (R.string.err_no_match),
    YTSEARCH                    (R.string.err_ytsearch),
    YTHTTPGET                   (R.string.err_ytprotocol),
    END_OF_DATA                 (R.string.err_end_of_data);

    private final int mMsg;

    private Err(int msg) {
        mMsg = msg;
    }

    public int
    getMessage() {
        return mMsg;
    }
}
